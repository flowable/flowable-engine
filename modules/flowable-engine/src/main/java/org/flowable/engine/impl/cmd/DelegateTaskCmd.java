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
import org.flowable.engine.impl.util.TaskHelper;
import org.flowable.task.api.DelegationState;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class DelegateTaskCmd extends NeedsActiveTaskCmd<Object> {

    private static final long serialVersionUID = 1L;
    protected String userId;

    public DelegateTaskCmd(String taskId, String userId) {
        super(taskId);
        this.userId = userId;
    }

    @Override
    protected Object execute(CommandContext commandContext, TaskEntity task) {
        task.setDelegationState(DelegationState.PENDING);
        if (task.getOwner() == null) {
            task.setOwner(task.getAssignee());
        }
        TaskHelper.changeTaskAssignee(task, userId);
        return null;
    }

    @Override
    protected String getSuspendedTaskExceptionPrefix() {
        return "Cannot delegate";
    }

}
