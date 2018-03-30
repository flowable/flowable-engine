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

import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Tijs Rademakers
 */
public class SetTaskPriorityCmd extends NeedsActiveTaskCmd<Void> {

    private static final long serialVersionUID = 1L;

    protected int priority;

    public SetTaskPriorityCmd(String taskId, int priority) {
        super(taskId);
        this.priority = priority;
    }

    @Override
    protected Void execute(CommandContext commandContext, TaskEntity task) {
        task.setPriority(priority);
        CommandContextUtil.getCmmnHistoryManager(commandContext).recordTaskInfoChange(task);
        CommandContextUtil.getTaskService().updateTask(task, true);
        return null;
    }

}
