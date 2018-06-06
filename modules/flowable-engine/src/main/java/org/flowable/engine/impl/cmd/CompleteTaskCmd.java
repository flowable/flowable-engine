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
package org.flowable.engine.impl.cmd;

import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.impl.util.TaskHelper;
import org.flowable.task.api.DelegationState;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Joram Barrez
 */
public class CompleteTaskCmd extends NeedsActiveTaskCmd<Void> {

    private static final long serialVersionUID = 1L;
    protected Map<String, Object> variables;
    protected Map<String, Object> transientVariables;
    protected boolean localScope;

    public CompleteTaskCmd(String taskId, Map<String, Object> variables) {
        super(taskId);
        this.variables = variables;
    }

    public CompleteTaskCmd(String taskId, Map<String, Object> variables, boolean localScope) {
        this(taskId, variables);
        this.localScope = localScope;
    }

    public CompleteTaskCmd(String taskId, Map<String, Object> variables, Map<String, Object> transientVariables) {
        this(taskId, variables);
        this.transientVariables = transientVariables;
    }

    @Override
    protected Void execute(CommandContext commandContext, TaskEntity task) {
        // Backwards compatibility
        if (task.getProcessDefinitionId() != null) {
            if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, task.getProcessDefinitionId())) {
                Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();

                if (transientVariables == null) {
                    compatibilityHandler.completeTask(task, variables, localScope);
                } else {
                    compatibilityHandler.completeTask(task, variables, transientVariables);
                }
                return null;
            }
        }

        completeTask(commandContext, this.variables, task);
        return null;
    }

    @Override
    protected String getSuspendedTaskException() {
        return "Cannot complete a suspended task";
    }

    protected void completeTask(CommandContext commandContext, Map<String, Object> variables, TaskEntity taskEntity) {

        // Task complete logic

        if (taskEntity.getDelegationState() != null && taskEntity.getDelegationState() == DelegationState.PENDING) {
            throw new FlowableException("A delegated task cannot be completed, but should be resolved instead.");
        }

        if (variables != null) {
            if (localScope) {
                taskEntity.setVariablesLocal(variables);

            } else if (taskEntity.getExecutionId() != null) {
                ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager().findById(taskEntity.getExecutionId());
                if (execution != null) {
                    execution.setVariables(variables);
                }

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

        CommandContextUtil.getProcessEngineConfiguration(commandContext).getListenerNotificationHelper()
            .executeTaskListeners(taskEntity, TaskListener.EVENTNAME_COMPLETE);
        CommandContextUtil.getInternalTaskAssignmentManager(commandContext).addUserIdentityLinkToParent(taskEntity, Authentication.getAuthenticatedUserId());

        FlowableEventDispatcher eventDispatcher = CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher();
        if (eventDispatcher.isEnabled()) {
            if (variables != null) {
                eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityWithVariablesEvent(
                    FlowableEngineEventType.TASK_COMPLETED, taskEntity, variables, localScope));
            } else {
                eventDispatcher.dispatchEvent(
                    FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_COMPLETED, taskEntity));
            }
        }

        TaskHelper.deleteTask(taskEntity, null, false, true, true);

        // Continue process (if not a standalone task)
        if (taskEntity.getExecutionId() != null) {
            ExecutionEntity executionEntity = CommandContextUtil.getExecutionEntityManager(commandContext).findById(taskEntity.getExecutionId());
            CommandContextUtil.getAgenda(commandContext).planTriggerExecutionOperation(executionEntity);
        }
    }

}
