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
package org.flowable.cmmn.engine.impl.task;

import java.util.List;

import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.task.api.Task;
import org.flowable.task.service.TaskService;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Joram Barrez
 */
public class TaskHelper {

    public static void deleteTask(TaskEntity task, String deleteReason, boolean cascade, boolean fireEvents) {
        if (!task.isDeleted()) {
            task.setDeleted(true);

            CommandContext commandContext = CommandContextUtil.getCommandContext();
            TaskService taskService = CommandContextUtil.getTaskService(commandContext);
            List<Task> subTasks = taskService.findTasksByParentTaskId(task.getId());
            for (Task subTask : subTasks) {
                deleteTask((TaskEntity) subTask, deleteReason, cascade, fireEvents);
            }

            CountingTaskEntity countingTaskEntity = (CountingTaskEntity) task;
            if (countingTaskEntity.isCountEnabled() && countingTaskEntity.getIdentityLinkCount() > 0) {    
                CommandContextUtil.getIdentityLinkService(commandContext).deleteIdentityLinksByTaskId(task.getId());
            }
            if (countingTaskEntity.isCountEnabled() && countingTaskEntity.getVariableCount() > 0) {
                CommandContextUtil.getVariableService(commandContext).deleteVariableInstanceMap(task.getVariableInstanceEntities());
            }
            CommandContextUtil.getCmmnHistoryManager(commandContext).recordTaskEnd(task, deleteReason);
            CommandContextUtil.getTaskService().deleteTask(task, fireEvents);
        }
    }
    
    
}
