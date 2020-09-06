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

import java.util.List;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.Signal;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.event.EventDefinitionExpressionUtil;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.SignalEventSubscriptionEntity;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class BoundarySignalEventActivityBehavior extends BoundaryEventActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected SignalEventDefinition signalEventDefinition;
    protected Signal signal;

    public BoundarySignalEventActivityBehavior(SignalEventDefinition signalEventDefinition, Signal signal, boolean interrupting) {
        super(interrupting);
        this.signalEventDefinition = signalEventDefinition;
        this.signal = signal;
    }

    @Override
    public void execute(DelegateExecution execution) {
        CommandContext commandContext = Context.getCommandContext();
        ExecutionEntity executionEntity = (ExecutionEntity) execution;

        String signalName = EventDefinitionExpressionUtil.determineSignalName(commandContext, signalEventDefinition,
                ProcessDefinitionUtil.getBpmnModel(execution.getProcessDefinitionId()), execution);

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        EventSubscriptionEntity eventSubscription = (EventSubscriptionEntity) processEngineConfiguration.getEventSubscriptionServiceConfiguration()
                .getEventSubscriptionService().createEventSubscriptionBuilder()
                    .eventType(SignalEventSubscriptionEntity.EVENT_TYPE)
                    .eventName(signalName)
                    .signal(signal)
                    .executionId(executionEntity.getId())
                    .processInstanceId(executionEntity.getProcessInstanceId())
                    .activityId(executionEntity.getCurrentActivityId())
                    .processDefinitionId(executionEntity.getProcessDefinitionId())
                    .tenantId(executionEntity.getTenantId())
                    .create();
        
        CountingEntityUtil.handleInsertEventSubscriptionEntityCount(eventSubscription);
        executionEntity.getEventSubscriptions().add(eventSubscription);

        FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createSignalEvent(FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING, executionEntity.getActivityId(), signalName,
                    null, executionEntity.getId(), executionEntity.getProcessInstanceId(), executionEntity.getProcessDefinitionId()),
                    processEngineConfiguration.getEngineCfgKey());
        }
    }

    @Override
    public void trigger(DelegateExecution execution, String triggerName, Object triggerData) {
        ExecutionEntity executionEntity = (ExecutionEntity) execution;
        BoundaryEvent boundaryEvent = (BoundaryEvent) execution.getCurrentFlowElement();
        
        if (boundaryEvent.isCancelActivity()) {
            CommandContext commandContext = Context.getCommandContext();
            String eventName = EventDefinitionExpressionUtil.determineSignalName(commandContext, signalEventDefinition,
                    ProcessDefinitionUtil.getBpmnModel(execution.getProcessDefinitionId()), execution);
            
            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
            EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
            List<EventSubscriptionEntity> eventSubscriptions = executionEntity.getEventSubscriptions();
            for (EventSubscriptionEntity eventSubscription : eventSubscriptions) {
                if (eventSubscription instanceof SignalEventSubscriptionEntity && eventSubscription.getEventName().equals(eventName)) {
                    eventSubscriptionService.deleteEventSubscription(eventSubscription);
                    CountingEntityUtil.handleDeleteEventSubscriptionEntityCount(eventSubscription);
                }
            }
        }

        super.trigger(executionEntity, triggerName, triggerData);
    }
}