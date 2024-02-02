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

public interface CmmnLoggingSessionConstants {
    
    String TYPE_CASE_STARTED = "caseStarted";
    String TYPE_CASE_COMPLETED = "caseCompleted";
    String TYPE_CASE_TERMINATED = "caseTerminated";
    
    String TYPE_PLAN_ITEM_CREATED = "planItemCreated";
    String TYPE_PLAN_ITEM_NEW_STATE = "planItemNewState";
    
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
    
    String TYPE_HUMAN_TASK_CREATE = "humanTaskCreate";
    String TYPE_HUMAN_TASK_SET_ASSIGNEE = "humanTaskSetAssignee";
    String TYPE_HUMAN_TASK_SET_OWNER = "humanTaskSetOwner";
    String TYPE_HUMAN_TASK_SET_GROUP_IDENTITY_LINKS = "humanTaskSetGroupIdentityLinks";
    String TYPE_HUMAN_TASK_SET_USER_IDENTITY_LINKS = "humanTaskSetUserIdentityLinks";
    String TYPE_HUMAN_TASK_COMPLETE = "humanTaskComplete";
    
    String TYPE_EVALUATE_SENTRY = "evaluateSentry";
    String TYPE_EVALUATE_SENTRY_FAILED = "evaluateSentryFailed";
    
    String TYPE_VARIABLE_CREATE = "variableCreate";
    String TYPE_VARIABLE_UPDATE = "variableUpdate";
    String TYPE_VARIABLE_DELETE = "variableDelete";
    
    String TYPE_COMMAND_CONTEXT_CLOSE = "commandContextClose";
    String TYPE_COMMAND_CONTEXT_CLOSE_FAILURE = "commandContextCloseFailure";
}
