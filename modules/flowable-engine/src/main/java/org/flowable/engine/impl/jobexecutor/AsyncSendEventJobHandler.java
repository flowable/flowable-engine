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

import java.util.Collection;
import java.util.Objects;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SendEventServiceTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.EventInstanceBpmnUtil;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.runtime.EventInstanceImpl;
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

        EventModel eventModel = null;
        if (Objects.equals(ProcessEngineConfiguration.NO_TENANT_ID, job.getTenantId())) {
            eventModel = CommandContextUtil.getEventRepositoryService(commandContext).getEventModelByKey(sendEventServiceTask.getEventType());
        } else {
            eventModel = CommandContextUtil.getEventRepositoryService(commandContext).getEventModelByKey(sendEventServiceTask.getEventType(), job.getTenantId());
        }

        if (eventModel == null) {
            throw new FlowableException("No event model found for event key " + sendEventServiceTask.getEventType());
        }
        
        EventInstanceImpl eventInstance = new EventInstanceImpl();
        eventInstance.setEventModel(eventModel);

        Collection<EventPayloadInstance> eventPayloadInstances = EventInstanceBpmnUtil.createEventPayloadInstances(executionEntity, 
                        CommandContextUtil.getProcessEngineConfiguration().getExpressionManager(), sendEventServiceTask, eventModel);
        eventInstance.setPayloadInstances(eventPayloadInstances);

        CommandContextUtil.getEventRegistry(commandContext).sendEventOutbound(eventInstance);
        
        if (!sendEventServiceTask.isTriggerable()) {
            CommandContextUtil.getAgenda(commandContext).planTakeOutgoingSequenceFlowsOperation(executionEntity, true);
        }
    }

}
