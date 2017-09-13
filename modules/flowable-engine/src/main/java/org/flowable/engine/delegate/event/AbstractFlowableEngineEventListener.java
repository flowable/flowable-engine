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
 *
 */
package org.flowable.engine.delegate.event;

import org.flowable.engine.common.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEngineEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.common.api.delegate.event.FlowableEntityEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEventListener;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.variable.service.event.FlowableVariableEvent;

import java.util.Set;

/**
 *  @author Robert Hafner
 */
public abstract class AbstractFlowableEngineEventListener implements FlowableEventListener {

    protected Set<FlowableEngineEventType> types;

    public AbstractFlowableEngineEventListener() {}

    public AbstractFlowableEngineEventListener(Set<FlowableEngineEventType> types) {
        this.types = types;
    }

    @Override
    public void onEvent(FlowableEvent flowableEvent) {
        if(flowableEvent instanceof FlowableEngineEvent) {
            FlowableEngineEvent flowableEngineEvent = (FlowableEngineEvent) flowableEvent;
            FlowableEngineEventType engineEventType = (FlowableEngineEventType) flowableEvent.getType();

            if(types == null || types.contains(engineEventType)) {
                switch (engineEventType) {
                    case ENTITY_CREATED:
                        entityCreated((FlowableEntityEvent) flowableEngineEvent);
                        break;
                    case ENTITY_INITIALIZED:
                        entityInitialized((FlowableEntityEvent) flowableEngineEvent);
                        break;
                    case ENTITY_UPDATED:
                        entityUpdated((FlowableEntityEvent) flowableEngineEvent);
                        break;
                    case ENTITY_DELETED:
                        entityDeleted((FlowableEntityEvent) flowableEngineEvent);
                        break;
                    case ENTITY_SUSPENDED:
                        entitySuspended((FlowableEntityEvent) flowableEngineEvent);
                        break;
                    case ENTITY_ACTIVATED:
                        entityActivated((FlowableEntityEvent) flowableEngineEvent);
                        break;
                    case TIMER_SCHEDULED:
                        timerScheduled((FlowableEntityEvent) flowableEngineEvent);
                        break;
                    case TIMER_FIRED:
                        timerFired((FlowableEntityEvent) flowableEngineEvent);
                        break;
                    case JOB_CANCELED:
                        jobCancelled((FlowableEntityEvent) flowableEngineEvent);
                        break;
                    case JOB_EXECUTION_SUCCESS:
                        jobExecutionSuccess((FlowableEntityEvent) flowableEngineEvent);
                        break;
                    case JOB_EXECUTION_FAILURE:
                        jobExecutionFailure((FlowableEntityEvent) flowableEngineEvent);
                        break;
                    case JOB_RETRIES_DECREMENTED:
                        jobRetriesDecremented((FlowableEntityEvent) flowableEngineEvent);
                        break;
                    case JOB_RESCHEDULED:
                        jobRescheduled((FlowableEntityEvent) flowableEngineEvent);
                        break;
                    case CUSTOM:
                        custom(flowableEngineEvent);
                        break;
                    case ENGINE_CREATED:
                        engineCreated((FlowableProcessEngineEvent) flowableEngineEvent);
                        break;
                    case ENGINE_CLOSED:
                        engineClosed((FlowableProcessEngineEvent) flowableEngineEvent);
                        break;
                    case ACTIVITY_STARTED:
                        activityStarted((FlowableActivityEvent) flowableEngineEvent);
                        break;
                    case ACTIVITY_COMPLETED:
                        activityCompleted((FlowableActivityEvent) flowableEngineEvent);
                        break;
                    case ACTIVITY_CANCELLED:
                        activityCancelled((FlowableActivityCancelledEvent) flowableEngineEvent);
                        break;
                    case ACTIVITY_SIGNAL_WAITING:
                        activitySignalWaiting((FlowableSignalEvent) flowableEngineEvent);
                        break;
                    case ACTIVITY_SIGNALED:
                        activitySignaled((FlowableSignalEvent) flowableEngineEvent);
                        break;
                    case ACTIVITY_COMPENSATE:
                        activityCompensate((FlowableActivityEvent) flowableEngineEvent);
                        break;
                    case ACTIVITY_MESSAGE_WAITING:
                        activityMessageWaiting((FlowableMessageEvent) flowableEngineEvent);
                        break;
                    case ACTIVITY_MESSAGE_RECEIVED:
                        activityMessageReceived((FlowableMessageEvent) flowableEngineEvent);
                        break;
                    case ACTIVITY_MESSAGE_CANCELLED:
                        activityMessageCancelled((FlowableMessageEvent) flowableEngineEvent);
                        break;
                    case ACTIVITY_ERROR_RECEIVED:
                        activityErrorReceived((FlowableErrorEvent) flowableEngineEvent);
                        break;
                    case HISTORIC_ACTIVITY_INSTANCE_CREATED:
                        historicActivityInstanceCreated((FlowableEntityEvent) flowableEngineEvent);
                        break;
                    case HISTORIC_ACTIVITY_INSTANCE_ENDED:
                        historicActivityInstanceEnded((FlowableEntityEvent) flowableEngineEvent);
                        break;
                    case SEQUENCEFLOW_TAKEN:
                        sequenceFlowTaken((FlowableSequenceFlowTakenEvent) flowableEngineEvent);
                        break;
                    case VARIABLE_CREATED:
                        variableCreated((FlowableVariableEvent) flowableEngineEvent);
                        break;
                    case VARIABLE_UPDATED:
                        variableUpdatedEvent((FlowableVariableEvent) flowableEngineEvent);
                        break;
                    case VARIABLE_DELETED:
                        variableDeletedEvent((FlowableVariableEvent) flowableEngineEvent);
                        break;
                    case TASK_CREATED:
                        taskCreated((FlowableEngineEntityEvent) flowableEngineEvent);
                        break;
                    case TASK_ASSIGNED:
                        taskAssigned((FlowableEntityEvent) flowableEngineEvent);
                        break;
                    case TASK_COMPLETED:
                        taskCompleted((FlowableEntityEvent) flowableEngineEvent);
                        break;
                    case PROCESS_CREATED:
                        processCreated((FlowableEntityEvent) flowableEngineEvent);
                        break;
                    case PROCESS_STARTED:
                        processStarted((FlowableProcessStartedEvent) flowableEngineEvent);
                        break;
                    case PROCESS_COMPLETED:
                        processCompleted((FlowableEngineEntityEvent) flowableEngineEvent);
                        break;
                    case PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT:
                        processCompletedWithTerminateEnd((FlowableEngineEntityEvent) flowableEngineEvent);
                        break;
                    case PROCESS_COMPLETED_WITH_ERROR_END_EVENT:
                        processCompletedWithErrorEnd((FlowableEntityEvent) flowableEngineEvent);
                        break;
                    case PROCESS_CANCELLED:
                        processCancelled((FlowableCancelledEvent) flowableEngineEvent);
                        break;
                    case HISTORIC_PROCESS_INSTANCE_CREATED:
                        historicProcessInstanceCreated((FlowableEntityEvent) flowableEngineEvent);
                        break;
                    case HISTORIC_PROCESS_INSTANCE_ENDED:
                        historicProcessInstanceEnded((FlowableEntityEvent) flowableEngineEvent);
                        break;
                }
            }
        }
    }

    @Override
    public boolean isFailOnException() {
        return true;
    }

    protected void entityCreated(FlowableEntityEvent event) {}

    protected void entityInitialized(FlowableEntityEvent event) {}

    protected void entityUpdated(FlowableEntityEvent event) {}

    protected void entityDeleted(FlowableEntityEvent event) {}

    protected void entitySuspended(FlowableEntityEvent event) {}

    protected void entityActivated(FlowableEntityEvent event) {}

    protected void timerScheduled(FlowableEntityEvent event) {}

    protected void timerFired(FlowableEntityEvent event) {}

    protected void jobCancelled(FlowableEntityEvent event) {}

    protected void jobExecutionSuccess(FlowableEntityEvent event) {}

    protected void jobExecutionFailure(FlowableEntityEvent event) {}

    protected void jobRetriesDecremented(FlowableEntityEvent event) {}

    protected void jobRescheduled(FlowableEntityEvent event) {}

    protected void custom(FlowableEngineEvent event) {}

    protected void engineCreated(FlowableProcessEngineEvent event) {}

    protected void engineClosed(FlowableProcessEngineEvent flowableEngineEvent) {}

    protected void activityStarted(FlowableActivityEvent event) {}

    protected void activityCompleted(FlowableActivityEvent event) {}

    protected void activityCancelled(FlowableActivityCancelledEvent event) {}

    protected void activitySignalWaiting(FlowableSignalEvent event) {}

    protected void activitySignaled(FlowableSignalEvent event) {}

    protected void activityCompensate(FlowableActivityEvent event) {}

    protected void activityMessageWaiting(FlowableMessageEvent event) {}

    protected void activityMessageReceived(FlowableMessageEvent event) {}

    protected void activityMessageCancelled(FlowableMessageEvent event) {}

    protected void activityErrorReceived(FlowableErrorEvent event) {}

    protected void historicActivityInstanceCreated(FlowableEntityEvent event) {}

    protected void historicActivityInstanceEnded(FlowableEntityEvent event) {}

    protected void sequenceFlowTaken(FlowableSequenceFlowTakenEvent event) {}

    protected void variableCreated(FlowableVariableEvent event) {}

    protected void variableUpdatedEvent(FlowableVariableEvent event) {}

    protected void variableDeletedEvent(FlowableVariableEvent event) {}

    protected void taskCreated(FlowableEngineEntityEvent event) {}

    protected void taskAssigned(FlowableEntityEvent event) {}

    protected void taskCompleted(FlowableEntityEvent event) {}

    protected void processCreated(FlowableEntityEvent event) {}

    protected void processStarted(FlowableProcessStartedEvent event) {}

    protected void processCompleted(FlowableEngineEntityEvent event) {}

    protected void processCompletedWithTerminateEnd(FlowableEngineEntityEvent event) {}

    protected void processCompletedWithErrorEnd(FlowableEntityEvent event) {}

    protected void processCancelled(FlowableCancelledEvent event) {}

    protected void historicProcessInstanceCreated(FlowableEntityEvent event) {}

    protected void historicProcessInstanceEnded(FlowableEntityEvent event) {}

    protected DelegateExecution getExecution(FlowableEngineEvent event) {
        String executionId = event.getExecutionId();

        if (executionId != null) {
            CommandContext commandContext = CommandContextUtil.getCommandContext();
            if (commandContext != null) {
                return CommandContextUtil.getExecutionEntityManager(commandContext).findById(executionId);
                }
            }
        return null;
    }
}
