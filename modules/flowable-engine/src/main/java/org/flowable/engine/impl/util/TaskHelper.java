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
package org.flowable.engine.impl.util;

import java.util.Map;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.delegate.event.FlowableEventDispatcher;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.identity.Authentication;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.TaskEntity;
import org.flowable.engine.task.DelegationState;
import org.flowable.engine.task.IdentityLinkType;

/**
 * @author Joram Barrez
 * @author Marcus Klimstra
 */
public class TaskHelper {

    public static void completeTask(TaskEntity taskEntity, Map<String, Object> variables,
            Map<String, Object> transientVariables, boolean localScope, CommandContext commandContext) {
        // Task complete logic

        if (taskEntity.getDelegationState() != null && taskEntity.getDelegationState() == DelegationState.PENDING) {
            throw new FlowableException("A delegated task cannot be completed, but should be resolved instead.");
        }

        if (variables != null) {
            if (localScope) {
                taskEntity.setVariablesLocal(variables);
            } else if (taskEntity.getExecutionId() != null) {
                taskEntity.setExecutionVariables(variables);
            } else {
                taskEntity.setVariables(variables);
            }
        }

        if (transientVariables != null) {
            if (localScope) {
                taskEntity.setTransientVariablesLocal(transientVariables);
            } else {
                taskEntity.setTransientVariables(transientVariables);
            }
        }

        CommandContextUtil.getProcessEngineConfiguration(commandContext).getListenerNotificationHelper().executeTaskListeners(taskEntity, TaskListener.EVENTNAME_COMPLETE);
        if (Authentication.getAuthenticatedUserId() != null && taskEntity.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceEntity = CommandContextUtil.getExecutionEntityManager(commandContext).findById(taskEntity.getProcessInstanceId());
            CommandContextUtil.getIdentityLinkEntityManager(commandContext).involveUser(processInstanceEntity, Authentication.getAuthenticatedUserId(), IdentityLinkType.PARTICIPANT);
        }

        FlowableEventDispatcher eventDispatcher = CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher();
        if (eventDispatcher.isEnabled()) {
            if (variables != null) {
                eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityWithVariablesEvent(FlowableEngineEventType.TASK_COMPLETED, taskEntity, variables, localScope));
            } else {
                eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_COMPLETED, taskEntity));
            }
        }

        CommandContextUtil.getTaskEntityManager(commandContext).deleteTask(taskEntity, null, false, true);

        // Continue process (if not a standalone task)
        if (taskEntity.getExecutionId() != null) {
            ExecutionEntity executionEntity = CommandContextUtil.getExecutionEntityManager(commandContext).findById(taskEntity.getExecutionId());
            CommandContextUtil.getAgenda(commandContext).planTriggerExecutionOperation(executionEntity);
        }
    }

}
