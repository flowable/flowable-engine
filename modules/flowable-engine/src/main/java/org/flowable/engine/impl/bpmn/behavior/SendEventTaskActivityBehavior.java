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
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.SendEventServiceTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CorrelationUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.engine.impl.util.EventInstanceBpmnUtil;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.definition.EventDefinition;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.runtime.EventInstanceImpl;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;

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
        EventRegistry eventRegistry = CommandContextUtil.getProcessEngineConfiguration().getEventRegistry();
        EventDefinition eventDefinition = eventRegistry.getEventDefinition(sendEventServiceTask.getEventType());

        if (eventDefinition == null) {
            throw new FlowableException("No event definition found for event key " + sendEventServiceTask.getEventType());
        }

        EventInstanceImpl eventInstance = new EventInstanceImpl();
        eventInstance.setEventDefinition(eventDefinition);

        Collection<EventPayloadInstance> eventPayloadInstances = EventInstanceBpmnUtil.createEventPayloadInstances(execution, 
                        CommandContextUtil.getProcessEngineConfiguration().getExpressionManager(), sendEventServiceTask, eventDefinition);
        eventInstance.setPayloadInstances(eventPayloadInstances);

        // TODO: always async? Send event in post-commit? Triggerable?

        eventRegistry.sendEvent(eventInstance);
        
        if (!sendEventServiceTask.isTriggerable()) {
            leave(execution);
            
        } else {
            EventDefinition triggerEventDefinition = null;
            if (StringUtils.isNotEmpty(sendEventServiceTask.getTriggerEventType())) {
                triggerEventDefinition = eventRegistry.getEventDefinition(sendEventServiceTask.getTriggerEventType());
            } else {
                triggerEventDefinition = eventDefinition;
            }
            
            ExecutionEntity executionEntity = (ExecutionEntity) execution;
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
        }
    }
    
    @Override
    public void trigger(DelegateExecution execution, String signalName, Object signalData) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        
        if (sendEventServiceTask.isTriggerable()) {
            Object eventInstance = execution.getTransientVariables().get("eventInstance");
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
            EventRegistry eventRegistry = processEngineConfiguration.getEventRegistry();
            EventDefinition eventDefinition = eventRegistry.getEventDefinition(eventType);
            for (EventSubscriptionEntity eventSubscription : eventSubscriptions) {
                if (Objects.equals(eventDefinition.getKey(), eventSubscription.getEventType())) {
                    eventSubscriptionService.deleteEventSubscription(eventSubscription);
                }
            }
            
            leave(execution);
        }
    }
}
