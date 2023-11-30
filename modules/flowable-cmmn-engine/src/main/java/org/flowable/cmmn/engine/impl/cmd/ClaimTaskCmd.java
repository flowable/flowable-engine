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

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.task.TaskHelper;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableTaskAlreadyClaimedException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.task.api.Task;
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
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        if (userId != null) {
            Clock clock = cmmnEngineConfiguration.getClock();
            task.setClaimTime(clock.getCurrentTime());
            task.setClaimedBy(userId);
            task.setState(Task.CLAIMED);

            if (task.getAssignee() != null) {
                if (!task.getAssignee().equals(userId)) {
                    // When the task is already claimed by another user, throw
                    // exception. Otherwise, ignore this, post-conditions of method already met.
                    throw new FlowableTaskAlreadyClaimedException(task.getId(), task.getAssignee());
                }
                cmmnEngineConfiguration.getCmmnHistoryManager().recordTaskInfoChange(task, clock.getCurrentTime());
                
            } else {
                TaskHelper.changeTaskAssignee(task, userId, cmmnEngineConfiguration);
                
                if (cmmnEngineConfiguration.getHumanTaskStateInterceptor() != null) {
                    cmmnEngineConfiguration.getHumanTaskStateInterceptor().handleClaim(task, userId);
                }
            }
            
        } else {
            if (task.getAssignee() != null) {
                // Task claim time should be null
                task.setClaimTime(null);
                task.setClaimedBy(null);
                
                if (task.getInProgressStartTime() != null) {
                    task.setState(Task.IN_PROGRESS);
                } else {
                    task.setState(Task.CREATED);
                }
                
                // Task should be assigned to no one
                TaskHelper.changeTaskAssignee(task, null, cmmnEngineConfiguration);
                
                if (cmmnEngineConfiguration.getHumanTaskStateInterceptor() != null) {
                    cmmnEngineConfiguration.getHumanTaskStateInterceptor().handleUnclaim(task, userId);
                }
            }
        }

        return null;
    }

    @Override
    protected String getSuspendedTaskExceptionPrefix() {
        return "Cannot claim";
    }

}
