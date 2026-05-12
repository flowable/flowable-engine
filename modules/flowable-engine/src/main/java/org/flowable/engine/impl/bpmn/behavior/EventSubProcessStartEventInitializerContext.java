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

import org.flowable.bpmn.model.StartEvent;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.eventsubscription.api.EventSubscriptionBuilder;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;

/**
 * Context passed to {@link EventSubProcessStartEventActivityBehavior#initializeEventSubProcessStart}
 * when a parent process or sub-process becomes active and its event-sub-process start events need
 * to register their subscriptions / timers / waiting executions.
 */
public class EventSubProcessStartEventInitializerContext {

    protected final ExecutionEntity parentExecution;
    protected final StartEvent startEvent;
    protected final ProcessEngineConfigurationImpl processEngineConfiguration;
    protected final CommandContext commandContext;
    protected final List<EventSubscriptionEntity> messageEventSubscriptions;
    protected final List<EventSubscriptionEntity> signalEventSubscriptions;

    public EventSubProcessStartEventInitializerContext(ExecutionEntity parentExecution, StartEvent startEvent,
            ProcessEngineConfigurationImpl processEngineConfiguration, CommandContext commandContext,
            List<EventSubscriptionEntity> messageEventSubscriptions, List<EventSubscriptionEntity> signalEventSubscriptions) {
        this.parentExecution = parentExecution;
        this.startEvent = startEvent;
        this.processEngineConfiguration = processEngineConfiguration;
        this.commandContext = commandContext;
        this.messageEventSubscriptions = messageEventSubscriptions;
        this.signalEventSubscriptions = signalEventSubscriptions;
    }

    public ExecutionEntity getParentExecution() {
        return parentExecution;
    }

    public StartEvent getStartEvent() {
        return startEvent;
    }

    public ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
        return processEngineConfiguration;
    }

    public CommandContext getCommandContext() {
        return commandContext;
    }

    /**
     * Returns an {@link EventSubscriptionBuilder} pre-filled with the execution / process-instance /
     * activity / process-definition / tenant ids of the supplied event-scope execution. Subscription-
     * registering initializers chain on it with the type-specific fields (eventType, eventName, etc.)
     * before calling {@code create()}.
     */
    public EventSubscriptionBuilder createEventSubscriptionBuilder(ExecutionEntity eventScopeExecution) {
        return processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService()
                .createEventSubscriptionBuilder()
                .executionId(eventScopeExecution.getId())
                .processInstanceId(eventScopeExecution.getProcessInstanceId())
                .activityId(eventScopeExecution.getCurrentActivityId())
                .processDefinitionId(eventScopeExecution.getProcessDefinitionId())
                .tenantId(eventScopeExecution.getTenantId());
    }

    /**
     * Creates a child execution of the parent that represents the waiting state for this event-sub-process
     * start event: pointed at the start event, marked as event scope, and inactive. This is the four-line
     * pattern repeated by every built-in event-sub-process start initializer.
     */
    public ExecutionEntity createEventScopeChildExecution() {
        ExecutionEntity execution = processEngineConfiguration.getExecutionEntityManager().createChildExecution(parentExecution);
        execution.setCurrentFlowElement(startEvent);
        execution.setEventScope(true);
        execution.setActive(false);
        return execution;
    }

    /**
     * Records a message event subscription so that {@code processEventSubProcess} can dispatch
     * {@code ACTIVITY_MESSAGE_WAITING} for it after every start event in the same sub-process has
     * registered. No-op if the caller didn't supply a collection (e.g. dynamic state migration paths).
     */
    public void recordWaitingMessageSubscription(EventSubscriptionEntity subscription) {
        if (messageEventSubscriptions != null) {
            messageEventSubscriptions.add(subscription);
        }
    }

    /**
     * Records a signal event subscription so that {@code processEventSubProcess} can dispatch
     * {@code ACTIVITY_SIGNAL_WAITING} for it after every start event in the same sub-process has
     * registered. No-op if the caller didn't supply a collection (e.g. dynamic state migration paths).
     */
    public void recordWaitingSignalSubscription(EventSubscriptionEntity subscription) {
        if (signalEventSubscriptions != null) {
            signalEventSubscriptions.add(subscription);
        }
    }
}
