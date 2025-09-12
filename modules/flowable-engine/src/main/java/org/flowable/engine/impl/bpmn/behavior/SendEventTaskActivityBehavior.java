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

package org.flowable.engine.impl.bpmn.behavior;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.SendEventServiceTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.helper.SkipExpressionUtil;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.jobexecutor.AsyncSendEventJobHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CorrelationUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.engine.impl.util.EventInstanceBpmnUtil;
import org.flowable.engine.impl.util.JobUtil;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.constant.EventConstants;
import org.flowable.eventregistry.impl.runtime.EventInstanceImpl;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;

/**
 * Sends an event to the event registry
 *
 * @author Tijs Rademakers
 */
public class SendEventTaskActivityBehavior extends AbstractBpmnActivityBehavior {

    private static final long serialVersionUID = 1L;
    
    protected SendEventServiceTask sendEventServiceTask;

    public SendEventTaskActivityBehavior(SendEventServiceTask sendEventServiceTask) {
        this.sendEventServiceTask = sendEventServiceTask;
    }

    @Override
    public void execute(DelegateExecution execution) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        
        boolean isSkipExpressionEnabled = SkipExpressionUtil.isSkipExpressionEnabled(sendEventServiceTask.getSkipExpression(), sendEventServiceTask.getId(), execution, commandContext);

        if (isSkipExpressionEnabled && SkipExpressionUtil.shouldSkipFlowElement(sendEventServiceTask.getSkipExpression(), sendEventServiceTask.getId(), execution, commandContext)) {
            leave(execution);
            return;
        }
        
        EventRegistry eventRegistry = CommandContextUtil.getEventRegistry(commandContext);

        EventModel eventModel = getEventModel(commandContext, execution);
        ExecutionEntity executionEntity = (ExecutionEntity) execution;

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        boolean executedAsAsyncJob = Boolean.TRUE.equals(commandContext.getAttribute(AsyncSendEventJobHandler.TYPE));
        boolean sendSynchronously = sendEventServiceTask.isSendSynchronously() || executedAsAsyncJob;
        if (!sendSynchronously) {
            JobService jobService = processEngineConfiguration.getJobServiceConfiguration().getJobService();
            
            JobEntity job = JobUtil.createJob(executionEntity, sendEventServiceTask, AsyncSendEventJobHandler.TYPE, processEngineConfiguration);

            jobService.createAsyncJob(job, true);
            jobService.scheduleAsyncJob(job);

        } else {
            commandContext.removeAttribute(AsyncSendEventJobHandler.TYPE);

            Collection<EventPayloadInstance> eventPayloadInstances = EventInstanceBpmnUtil.createEventPayloadInstances(executionEntity,
                    processEngineConfiguration.getExpressionManager(), execution.getCurrentFlowElement(), eventModel);

            boolean sendOnSystemChannel = isSendOnSystemChannel(execution);
            List<ChannelModel> channelModels = getChannelModels(commandContext, execution, sendOnSystemChannel);
            EventInstanceImpl eventInstance = new EventInstanceImpl(eventModel.getKey(), eventPayloadInstances, execution.getTenantId());
            if (!channelModels.isEmpty()) {
                eventRegistry.sendEventOutbound(eventInstance, channelModels);
            }

            if (sendOnSystemChannel) {
                eventRegistry.sendSystemEventOutbound(eventInstance);
            }

            if (executedAsAsyncJob) {
                commandContext.addAttribute(AsyncSendEventJobHandler.TYPE, true);
            }
        }

        if (sendEventServiceTask.isTriggerable() && !executedAsAsyncJob) {
            String triggerEventDefinitionKey;
            if (StringUtils.isNotEmpty(sendEventServiceTask.getTriggerEventType())) {
                triggerEventDefinitionKey = sendEventServiceTask.getTriggerEventType();

            } else {
                triggerEventDefinitionKey = eventModel.getKey();
            }
            
            EventSubscriptionEntity eventSubscription = (EventSubscriptionEntity) processEngineConfiguration.getEventSubscriptionServiceConfiguration()
                    .getEventSubscriptionService().createEventSubscriptionBuilder()
                        .eventType(triggerEventDefinitionKey)
                        .executionId(execution.getId())
                        .processInstanceId(execution.getProcessInstanceId())
                        .activityId(execution.getCurrentActivityId())
                        .processDefinitionId(execution.getProcessDefinitionId())
                        .scopeType(ScopeTypes.BPMN)
                        .tenantId(execution.getTenantId())
                        .configuration(CorrelationUtil.getCorrelationKey(BpmnXMLConstants.ELEMENT_TRIGGER_EVENT_CORRELATION_PARAMETER, commandContext, executionEntity))
                        .create();
            
            CountingEntityUtil.handleInsertEventSubscriptionEntityCount(eventSubscription);
            executionEntity.getEventSubscriptions().add(eventSubscription);

        } else if ( (sendSynchronously && !executedAsAsyncJob)
                || (!sendEventServiceTask.isTriggerable() && executedAsAsyncJob)) {
            // If this send task is specifically marked to send synchronously and is not triggerable then leave
            leave(execution);
        }
    }

    protected EventModel getEventModel(CommandContext commandContext, DelegateExecution execution) {
        EventModel eventModel = null;
        if (Objects.equals(ProcessEngineConfiguration.NO_TENANT_ID, execution.getTenantId())) {
            eventModel = CommandContextUtil.getEventRepositoryService(commandContext)
                .getEventModelByKey(sendEventServiceTask.getEventType());
        } else {
            eventModel = CommandContextUtil.getEventRepositoryService(commandContext)
                .getEventModelByKey(sendEventServiceTask.getEventType(), execution.getTenantId());
        }

        if (eventModel == null) {
            throw new FlowableException("No event definition found for event key " + sendEventServiceTask.getEventType() + " for " + execution);
        }
        return eventModel;
    }

    protected boolean isSendOnSystemChannel(DelegateExecution execution) {
        List<ExtensionElement> systemChannels = execution.getCurrentFlowElement().getExtensionElements().getOrDefault("systemChannel", Collections.emptyList());
        return !systemChannels.isEmpty();
    }

    protected List<ChannelModel> getChannelModels(CommandContext commandContext, DelegateExecution execution, boolean sendOnSystemChannel) {
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
            if (!sendOnSystemChannel) {
                // If the event is going to be send on the system channel then it is allowed to not define any other channels
                throw new FlowableException("No channel keys configured for " + execution);
            } else {
                return Collections.emptyList();
            }
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

    @Override
    public void trigger(DelegateExecution execution, String signalName, Object signalData) {
        if (sendEventServiceTask.isTriggerable()) {
            Object eventInstance = execution.getTransientVariables().get(EventConstants.EVENT_INSTANCE);
            if (eventInstance instanceof EventInstance) {
                EventInstanceBpmnUtil.handleEventInstanceOutParameters(execution, sendEventServiceTask, (EventInstance) eventInstance);
            }

            CommandContext commandContext = CommandContextUtil.getCommandContext();
            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
            EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
            ExecutionEntity executionEntity = (ExecutionEntity) execution;
            List<EventSubscriptionEntity> eventSubscriptions = executionEntity.getEventSubscriptions();

            String eventType = null;
            if (StringUtils.isNotEmpty(sendEventServiceTask.getTriggerEventType())) {
                eventType = sendEventServiceTask.getTriggerEventType();
            } else {
                eventType = sendEventServiceTask.getEventType();
            }
            
            EventModel eventModel = null;
            if (Objects.equals(ProcessEngineConfiguration.NO_TENANT_ID, execution.getTenantId())) {
                eventModel = CommandContextUtil.getEventRepositoryService(commandContext).getEventModelByKey(eventType);
            } else {
                eventModel = CommandContextUtil.getEventRepositoryService(commandContext).getEventModelByKey(eventType, execution.getTenantId());
            }

            for (EventSubscriptionEntity eventSubscription : eventSubscriptions) {
                if (Objects.equals(eventModel.getKey(), eventSubscription.getEventType())) {
                    eventSubscriptionService.deleteEventSubscription(eventSubscription);
                }
            }
            
            leave(execution);
        }
    }

}
