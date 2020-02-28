/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.engine.impl.jobexecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SendEventServiceTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.EventInstanceBpmnUtil;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.runtime.EventInstanceImpl;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * 
 * @author Tijs Rademakers
 */
public class AsyncSendEventJobHandler implements JobHandler {

    public static final String TYPE = "async-send-event";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        ExecutionEntity executionEntity = (ExecutionEntity) variableScope;
        FlowElement flowElement = executionEntity.getCurrentFlowElement();
        
        if (!(flowElement instanceof SendEventServiceTask)) {
            throw new FlowableException(String.format("unexpected activity type found for job %s, at activity %s", job.getId(), flowElement.getId()));
        }
        SendEventServiceTask sendEventServiceTask = (SendEventServiceTask) flowElement;

        EventModel eventModel = getEventModel(job, commandContext, sendEventServiceTask);
        List<ChannelModel> channelModels = getChannelModels(commandContext, executionEntity);

        Collection<EventPayloadInstance> eventPayloadInstances = EventInstanceBpmnUtil.createEventPayloadInstances(executionEntity,
                        CommandContextUtil.getProcessEngineConfiguration().getExpressionManager(), sendEventServiceTask, eventModel);

        EventInstanceImpl eventInstance = new EventInstanceImpl(eventModel, channelModels, null, eventPayloadInstances);

        CommandContextUtil.getEventRegistry(commandContext).sendEventOutbound(eventInstance);
        
        if (!sendEventServiceTask.isTriggerable()) {
            CommandContextUtil.getAgenda(commandContext).planTakeOutgoingSequenceFlowsOperation(executionEntity, true);
        }
    }

    protected EventModel getEventModel(JobEntity job, CommandContext commandContext, SendEventServiceTask sendEventServiceTask) {
        EventModel eventModel = null;
        if (Objects.equals(ProcessEngineConfiguration.NO_TENANT_ID, job.getTenantId())) {
            eventModel = CommandContextUtil.getEventRepositoryService(commandContext).getEventModelByKey(sendEventServiceTask.getEventType());
        } else {
            eventModel = CommandContextUtil.getEventRepositoryService(commandContext).getEventModelByKey(sendEventServiceTask.getEventType(), job.getTenantId());
        }

        if (eventModel == null) {
            throw new FlowableException("No event model found for event key " + sendEventServiceTask.getEventType());
        }
        return eventModel;
    }

    protected List<ChannelModel> getChannelModels(CommandContext commandContext, DelegateExecution execution) {
        List<String> channelKeys = new ArrayList<>();

        Map<String, List<ExtensionElement>> extensionElements = execution.getCurrentFlowElement().getExtensionElements();
        if (extensionElements != null) {
            List<ExtensionElement> channelKeyElements = extensionElements.get("channelKey");
            if (channelKeyElements != null && !channelKeyElements.isEmpty()) {
                String channelKey = channelKeyElements.get(0).getElementText();
                if (StringUtils.isNotEmpty(channelKey)) {
                    ExpressionManager expressionManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getExpressionManager();
                    Expression expression = expressionManager.createExpression(channelKey);
                    Object resolvedChannelKey = expression.getValue(execution);
                    if (resolvedChannelKey instanceof Collection) {
                        for (Object next : (Collection) resolvedChannelKey) {
                            if (next instanceof String) {
                                String[] keys = ((String) next).split(",");
                                channelKeys.addAll(Arrays.asList(keys));

                            } else {
                                throw new FlowableIllegalArgumentException("Can only use a collection of String elements for referencing channel model key");

                            }
                        }

                    } else if (resolvedChannelKey instanceof String) {
                        String[] keys = ((String) resolvedChannelKey).split(",");
                        channelKeys.addAll(Arrays.asList(keys));

                    }
                }
            }
        }

        if (channelKeys.isEmpty()) {
            throw new FlowableException("No channel keys configured");
        }

        EventRepositoryService eventRepositoryService = CommandContextUtil.getEventRegistryEngineConfiguration(commandContext).getEventRepositoryService();
        List<ChannelModel> channelModels = new ArrayList<>(channelKeys.size());
        for (String channelKey : channelKeys) {
            if (Objects.equals(ProcessEngineConfiguration.NO_TENANT_ID, execution.getTenantId())) {
                channelModels.add(eventRepositoryService.getChannelModelByKey(channelKey));
            } else {
                channelModels.add(eventRepositoryService.getChannelModelByKey(channelKey, execution.getTenantId()));
            }
        }

        return channelModels;
    }

}
