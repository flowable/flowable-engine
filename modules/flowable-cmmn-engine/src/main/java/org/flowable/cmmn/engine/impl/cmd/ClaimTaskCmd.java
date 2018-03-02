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
package org.flowable.cmmn.engine.impl.cmd;

import org.flowable.cmmn.api.exception.FlowableTaskAlreadyClaimedException;
import org.flowable.cmmn.engine.impl.task.TaskHelper;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.engine.common.impl.interceptor.CommandContext;
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
        if (userId != null) {
            task.setClaimTime(CommandContextUtil.getCmmnEngineConfiguration(commandContext).getClock().getCurrentTime());

            if (task.getAssignee() != null) {
                if (!task.getAssignee().equals(userId)) {
                    // When the task is already claimed by another user, throw
                    // exception. Otherwise, ignore this, post-conditions of method already met.
                    throw new FlowableTaskAlreadyClaimedException(task.getId(), task.getAssignee());
                }
                CommandContextUtil.getCmmnHistoryManager(commandContext).recordTaskInfoChange(task);
                
            } else {
                TaskHelper.changeTaskAssignee(task, userId);
            }
            
        } else {
            if (task.getAssignee() != null) {
                // Task claim time should be null
                task.setClaimTime(null);
                
                // Task should be assigned to no one
                TaskHelper.changeTaskAssignee(task, null);
            }
        }

        return null;
    }

    @Override
    protected String getSuspendedTaskException() {
        return "Cannot claim a suspended task";
    }

}
