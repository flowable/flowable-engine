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
package org.flowable.common.engine.impl.logging;

public interface LoggingSessionConstants {
    
    String TYPE_PROCESS_STARTED = "processStarted";
    String TYPE_PROCESS_COMPLETED = "processCompleted";
    
    String TYPE_SERVICE_TASK_ENTER = "serviceTaskEnter";
    String TYPE_SERVICE_TASK_EXIT = "serviceTaskExit";
    String TYPE_SERVICE_TASK_BEFORE_TRIGGER = "serviceTaskBeforeTrigger";
    String TYPE_SERVICE_TASK_AFTER_TRIGGER = "serviceTaskAfterTrigger";
    String TYPE_SERVICE_TASK_WRONG_TRIGGER = "serviceTaskWrongTrigger";
    String TYPE_SERVICE_TASK_EXCEPTION = "serviceTaskException";
    String TYPE_SERVICE_TASK_ASYNC_JOB = "serviceTaskAsyncJob";
    String TYPE_SERVICE_TASK_EXECUTE_ASYNC_JOB = "serviceTaskExecuteAsyncJob";
    String TYPE_SERVICE_TASK_LOCK_JOB = "serviceTaskLockJob";
    String TYPE_SERVICE_TASK_UNLOCK_JOB = "serviceTaskUnlockJob";
    
    String TYPE_USER_TASK_CREATE = "userTaskCreate";
    String TYPE_USER_TASK_SET_ASSIGNEE = "userTaskSetAssignee";
    String TYPE_USER_TASK_SET_OWNER = "userTaskSetOwner";
    String TYPE_USER_TASK_SET_GROUP_IDENTITY_LINKS = "userTaskSetGroupIdentityLinks";
    String TYPE_USER_TASK_SET_USER_IDENTITY_LINKS = "userTaskSetUserIdentityLinks";
    String TYPE_USER_TASK_COMPLETE = "userTaskComplete";
    
    String TYPE_BOUNDARY_CANCEL_EVENT_CREATE = "boundaryCancelEventCreate";
    String TYPE_BOUNDARY_COMPENSATE_EVENT_CREATE = "boundaryCompensateEventCreate";
    String TYPE_BOUNDARY_CONDITIONAL_EVENT_CREATE = "boundaryConditionalEventCreate";
    String TYPE_BOUNDARY_ESCALATION_EVENT_CREATE = "boundaryEscalationEventCreate";
    String TYPE_BOUNDARY_MESSAGE_EVENT_CREATE = "boundaryMessageEventCreate";
    String TYPE_BOUNDARY_SIGNAL_EVENT_CREATE = "boundarySignalEventCreate";
    String TYPE_BOUNDARY_TIMER_EVENT_CREATE = "boundaryTimerEventCreate";
    String TYPE_BOUNDARY_EVENT_CREATE = "boundaryEventCreate";

    String TYPE_SKIP_TASK = "skipTask";
    
    String TYPE_ACTIVITY_BEHAVIOR_EXECUTE = "activityBehaviorExecute";
    String TYPE_SEQUENCE_FLOW_TAKE = "sequenceFlowTake";
    
    String TYPE_VARIABLE_CREATE = "variableCreate";
    String TYPE_VARIABLE_UPDATE = "variableUpdate";
    String TYPE_VARIABLE_DELETE = "variableDelete";
    
    String TYPE_COMMAND_CONTEXT_CLOSE = "commandContextClose";
    String TYPE_COMMAND_CONTEXT_CLOSE_FAILURE = "commandContextCloseFailure";
}
