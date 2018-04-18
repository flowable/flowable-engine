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

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.FlowableTaskAlreadyClaimedException;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.impl.util.TaskHelper;
import org.flowable.identitylink.service.IdentityLinkType;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Joram Barrez
 */
public class ClaimTaskCmd extends NeedsActiveTaskCmd<Void> {

    private static final long serialVersionUID = 1L;

    protected String userId;

    public ClaimTaskCmd(String taskId, String userId) {
        super(taskId);
        this.userId = userId;
    }

    @Override
    protected Void execute(CommandContext commandContext, TaskEntity task) {
        if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, task.getProcessDefinitionId())) {
            Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
            compatibilityHandler.claimTask(taskId, userId);
            return null;
        }

        if (userId != null) {
            task.setClaimTime(CommandContextUtil.getProcessEngineConfiguration(commandContext).getClock().getCurrentTime());

            if (task.getAssignee() != null) {
                if (!task.getAssignee().equals(userId)) {
                    // When the task is already claimed by another user, throw
                    // exception. Otherwise, ignore this, post-conditions of method already met.
                    throw new FlowableTaskAlreadyClaimedException(task.getId(), task.getAssignee());
                }
                CommandContextUtil.getHistoryManager(commandContext).recordTaskInfoChange(task);
                
            } else {
                TaskHelper.changeTaskAssignee(task, userId);
            }
            
            CommandContextUtil.getHistoryManager().createUserIdentityLinkComment(task, userId, IdentityLinkType.ASSIGNEE, true);
            
        } else {
            if (task.getAssignee() != null) {
                // Task claim time should be null
                task.setClaimTime(null);
                
                String oldAssigneeId = task.getAssignee();
    
                // Task should be assigned to no one
                TaskHelper.changeTaskAssignee(task, null);
                
                CommandContextUtil.getHistoryManager().createUserIdentityLinkComment(task, oldAssigneeId, IdentityLinkType.ASSIGNEE, true, true);
            }
        }

        return null;
    }

    @Override
    protected String getSuspendedTaskException() {
        return "Cannot claim a suspended task";
    }

}
