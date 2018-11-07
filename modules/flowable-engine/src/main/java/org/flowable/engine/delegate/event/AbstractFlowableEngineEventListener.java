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

import java.util.Set;

import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.variable.api.event.FlowableVariableEvent;

/**
 *  @author Robert Hafner
 */
public abstract class AbstractFlowableEngineEventListener extends AbstractFlowableEventListener {

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
                        entityCreated((FlowableEngineEntityEvent) flowableEngineEvent);
                        break;
                    case ENTITY_INITIALIZED:
                        entityInitialized((FlowableEngineEntityEvent) flowableEngineEvent);
                        break;
                    case ENTITY_UPDATED:
                        entityUpdated((FlowableEngineEntityEvent) flowableEngineEvent);
                        break;
                    case ENTITY_DELETED:
                        entityDeleted((FlowableEngineEntityEvent) flowableEngineEvent);
                        break;
                    case ENTITY_SUSPENDED:
                        entitySuspended((FlowableEngineEntityEvent) flowableEngineEvent);
                        break;
                    case ENTITY_ACTIVATED:
                        entityActivated((FlowableEngineEntityEvent) flowableEngineEvent);
                        break;
                    case TIMER_SCHEDULED:
                        timerScheduled((FlowableEngineEntityEvent) flowableEngineEvent);
                        break;
                    case TIMER_FIRED:
                        timerFired((FlowableEngineEntityEvent) flowableEngineEvent);
                        break;
                    case JOB_CANCELED:
                        jobCancelled((FlowableEngineEntityEvent) flowableEngineEvent);
                        break;
                    case JOB_EXECUTION_SUCCESS:
                        jobExecutionSuccess((FlowableEngineEntityEvent) flowableEngineEvent);
                        break;
                    case JOB_EXECUTION_FAILURE:
                        jobExecutionFailure((FlowableEngineEntityEvent) flowableEngineEvent);
                        break;
                    case JOB_RETRIES_DECREMENTED:
                        jobRetriesDecremented((FlowableEngineEntityEvent) flowableEngineEvent);
                        break;
                    case JOB_RESCHEDULED:
                        jobRescheduled((FlowableEngineEntityEvent) flowableEngineEvent);
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
                    case MULTI_INSTANCE_ACTIVITY_STARTED:
                        multiInstanceActivityStarted((FlowableMultiInstanceActivityEvent) flowableEngineEvent);
                        break;
                    case MULTI_INSTANCE_ACTIVITY_COMPLETED:
                        multiInstanceActivityCompleted((FlowableMultiInstanceActivityCompletedEvent) flowableEngineEvent);
                        break;
                    case MULTI_INSTANCE_ACTIVITY_COMPLETED_WITH_CONDITION:
                        multiInstanceActivityCompletedWithCondition((FlowableMultiInstanceActivityCompletedEvent) flowableEngineEvent);
                        break;
                    case MULTI_INSTANCE_ACTIVITY_CANCELLED:
                        multiInstanceActivityCancelled((FlowableMultiInstanceActivityCancelledEvent) flowableEngineEvent);
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
                        historicActivityInstanceCreated((FlowableEngineEntityEvent) flowableEngineEvent);
                        break;
                    case HISTORIC_ACTIVITY_INSTANCE_ENDED:
                        historicActivityInstanceEnded((FlowableEngineEntityEvent) flowableEngineEvent);
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
                        taskAssigned((FlowableEngineEntityEvent) flowableEngineEvent);
                        break;
                    case TASK_COMPLETED:
                        taskCompleted((FlowableEngineEntityEvent) flowableEngineEvent);
                        break;
                    case PROCESS_CREATED:
                        processCreated((FlowableEngineEntityEvent) flowableEngineEvent);
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
                        processCompletedWithErrorEnd((FlowableEngineEntityEvent) flowableEngineEvent);
                        break;
                    case PROCESS_CANCELLED:
                        processCancelled((FlowableCancelledEvent) flowableEngineEvent);
                        break;
                    case HISTORIC_PROCESS_INSTANCE_CREATED:
                        historicProcessInstanceCreated((FlowableEngineEntityEvent) flowableEngineEvent);
                        break;
                    case HISTORIC_PROCESS_INSTANCE_ENDED:
                        historicProcessInstanceEnded((FlowableEngineEntityEvent) flowableEngineEvent);
                        break;
                }
            }
        }
    }

    @Override
    public boolean isFailOnException() {
        return true;
    }

    protected void entityCreated(FlowableEngineEntityEvent event) {}

    protected void entityInitialized(FlowableEngineEntityEvent event) {}

    protected void entityUpdated(FlowableEngineEntityEvent event) {}

    protected void entityDeleted(FlowableEngineEntityEvent event) {}

    protected void entitySuspended(FlowableEngineEntityEvent event) {}

    protected void entityActivated(FlowableEngineEntityEvent event) {}

    protected void timerScheduled(FlowableEngineEntityEvent event) {}

    protected void timerFired(FlowableEngineEntityEvent event) {}

    protected void jobCancelled(FlowableEngineEntityEvent event) {}

    protected void jobExecutionSuccess(FlowableEngineEntityEvent event) {}

    protected void jobExecutionFailure(FlowableEngineEntityEvent event) {}

    protected void jobRetriesDecremented(FlowableEngineEntityEvent event) {}

    protected void jobRescheduled(FlowableEngineEntityEvent event) {}

    protected void custom(FlowableEngineEvent event) {}

    protected void engineCreated(FlowableProcessEngineEvent event) {}

    protected void engineClosed(FlowableProcessEngineEvent flowableEngineEvent) {}

    protected void activityStarted(FlowableActivityEvent event) {}

    protected void activityCompleted(FlowableActivityEvent event) {}

    protected void activityCancelled(FlowableActivityCancelledEvent event) {}

    protected void multiInstanceActivityStarted(FlowableMultiInstanceActivityEvent event) {}

    protected void multiInstanceActivityCompleted(FlowableMultiInstanceActivityCompletedEvent event) {}

    protected void multiInstanceActivityCompletedWithCondition(FlowableMultiInstanceActivityCompletedEvent event) {}

    protected void multiInstanceActivityCancelled(FlowableMultiInstanceActivityCancelledEvent event) {}

    protected void activitySignalWaiting(FlowableSignalEvent event) {}

    protected void activitySignaled(FlowableSignalEvent event) {}

    protected void activityCompensate(FlowableActivityEvent event) {}

    protected void activityMessageWaiting(FlowableMessageEvent event) {}

    protected void activityMessageReceived(FlowableMessageEvent event) {}

    protected void activityMessageCancelled(FlowableMessageEvent event) {}

    protected void activityErrorReceived(FlowableErrorEvent event) {}

    protected void historicActivityInstanceCreated(FlowableEngineEntityEvent event) {}

    protected void historicActivityInstanceEnded(FlowableEngineEntityEvent event) {}

    protected void sequenceFlowTaken(FlowableSequenceFlowTakenEvent event) {}

    protected void variableCreated(FlowableVariableEvent event) {}

    protected void variableUpdatedEvent(FlowableVariableEvent event) {}

    protected void variableDeletedEvent(FlowableVariableEvent event) {}

    protected void taskCreated(FlowableEngineEntityEvent event) {}

    protected void taskAssigned(FlowableEngineEntityEvent event) {}

    protected void taskCompleted(FlowableEngineEntityEvent event) {}

    protected void processCreated(FlowableEngineEntityEvent event) {}

    protected void processStarted(FlowableProcessStartedEvent event) {}

    protected void processCompleted(FlowableEngineEntityEvent event) {}

    protected void processCompletedWithTerminateEnd(FlowableEngineEntityEvent event) {}

    protected void processCompletedWithErrorEnd(FlowableEngineEntityEvent event) {}

    protected void processCancelled(FlowableCancelledEvent event) {}

    protected void historicProcessInstanceCreated(FlowableEngineEntityEvent event) {}

    protected void historicProcessInstanceEnded(FlowableEngineEntityEvent event) {}

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
