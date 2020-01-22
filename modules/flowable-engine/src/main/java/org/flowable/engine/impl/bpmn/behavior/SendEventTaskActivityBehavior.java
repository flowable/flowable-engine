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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.SendEventServiceTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.jobexecutor.AsyncSendEventJobHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CorrelationUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.engine.impl.util.EventInstanceBpmnUtil;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.constant.EventConstants;
import org.flowable.eventregistry.impl.runtime.EventInstanceImpl;
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
        EventRegistry eventRegistry = CommandContextUtil.getEventRegistry();

        EventModel eventDefinition = null;
        if (Objects.equals(ProcessEngineConfiguration.NO_TENANT_ID, execution.getTenantId())) {
            eventDefinition = CommandContextUtil.getEventRepositoryService().getEventModelByKey(sendEventServiceTask.getEventType());
        } else {
            eventDefinition = CommandContextUtil.getEventRepositoryService().getEventModelByKey(sendEventServiceTask.getEventType(), 
                            execution.getTenantId());
        }

        if (eventDefinition == null) {
            throw new FlowableException("No event definition found for event key " + sendEventServiceTask.getEventType());
        }
        ExecutionEntity executionEntity = (ExecutionEntity) execution;

        boolean sendSynchronously = sendEventServiceTask.isSendSynchronously();
        if (!sendSynchronously) {
            JobService jobService = CommandContextUtil.getJobService();

            JobEntity job = jobService.createJob();
            job.setExecutionId(execution.getId());
            job.setProcessInstanceId(execution.getProcessInstanceId());
            job.setProcessDefinitionId(execution.getProcessDefinitionId());
            job.setElementId(sendEventServiceTask.getId());
            job.setElementName(sendEventServiceTask.getName());
            job.setJobHandlerType(AsyncSendEventJobHandler.TYPE);

            // Inherit tenant id (if applicable)
            if (execution.getTenantId() != null) {
                job.setTenantId(execution.getTenantId());
            }

            executionEntity.getJobs().add(job);

            jobService.createAsyncJob(job, true);
            jobService.scheduleAsyncJob(job);
        } else {
            Collection<EventPayloadInstance> eventPayloadInstances = EventInstanceBpmnUtil.createEventPayloadInstances(executionEntity,
                CommandContextUtil.getProcessEngineConfiguration(CommandContextUtil.getCommandContext()).getExpressionManager(),
                execution.getCurrentFlowElement(),
                eventDefinition);
            EventInstanceImpl eventInstance = new EventInstanceImpl(eventDefinition, Collections.emptyList(), eventPayloadInstances);
            eventRegistry.sendEventOutbound(eventInstance);
        }

        if (sendEventServiceTask.isTriggerable()) {
            EventModel triggerEventDefinition = null;
            if (StringUtils.isNotEmpty(sendEventServiceTask.getTriggerEventType())) {

                if (Objects.equals(ProcessEngineConfiguration.NO_TENANT_ID, execution.getTenantId())) {
                    triggerEventDefinition = CommandContextUtil.getEventRepositoryService().getEventModelByKey(sendEventServiceTask.getTriggerEventType());
                } else {
                    triggerEventDefinition = CommandContextUtil.getEventRepositoryService().getEventModelByKey(sendEventServiceTask.getTriggerEventType(), 
                                    execution.getTenantId());
                }

            } else {
                triggerEventDefinition = eventDefinition;
            }
            
            EventSubscriptionEntity eventSubscription = (EventSubscriptionEntity) CommandContextUtil.getEventSubscriptionService().createEventSubscriptionBuilder()
                    .eventType(triggerEventDefinition.getKey())
                    .executionId(execution.getId())
                    .processInstanceId(execution.getProcessInstanceId())
                    .activityId(execution.getCurrentActivityId())
                    .processDefinitionId(execution.getProcessDefinitionId())
                    .scopeType(ScopeTypes.BPMN)
                    .tenantId(execution.getTenantId())
                    .configuration(CorrelationUtil.getCorrelationKey(BpmnXMLConstants.ELEMENT_TRIGGER_EVENT_CORRELATION_PARAMETER, 
                                    Context.getCommandContext(), executionEntity))
                    .create();
            
            CountingEntityUtil.handleInsertEventSubscriptionEntityCount(eventSubscription);
            executionEntity.getEventSubscriptions().add(eventSubscription);
        } else if (sendSynchronously) {
            // If ths send task is specifically marked to send synchronously and is not triggerable then leave
            leave(execution);
        }
    }
    
    @Override
    public void trigger(DelegateExecution execution, String signalName, Object signalData) {
        if (sendEventServiceTask.isTriggerable()) {
            Object eventInstance = execution.getTransientVariables().get(EventConstants.EVENT_INSTANCE);
            if (eventInstance instanceof EventInstance) {
                EventInstanceBpmnUtil.handleEventInstanceOutParameters(execution, sendEventServiceTask, (EventInstance) eventInstance);
            }

            EventSubscriptionService eventSubscriptionService = CommandContextUtil.getEventSubscriptionService();
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
                eventModel = CommandContextUtil.getEventRepositoryService().getEventModelByKey(eventType);
            } else {
                eventModel = CommandContextUtil.getEventRepositoryService().getEventModelByKey(eventType, execution.getTenantId());
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
