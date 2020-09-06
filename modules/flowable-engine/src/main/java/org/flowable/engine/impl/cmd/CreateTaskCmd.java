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

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskBuilder;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.task.service.impl.util.CountingTaskUtil;

/**
 * Creates new task by {@link org.flowable.task.api.TaskBuilder}
 *
 * @author martin.grofcik
 */
public class CreateTaskCmd implements Command<Task> {
    protected TaskBuilder taskBuilder;

    public CreateTaskCmd(TaskBuilder taskBuilder) {
        this.taskBuilder = taskBuilder;
    }

    @Override
    public Task execute(CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        TaskServiceConfiguration taskServiceConfiguration = processEngineConfiguration.getTaskServiceConfiguration();
        Task task = taskServiceConfiguration.getTaskService().createTask(this.taskBuilder);
        if (CountingTaskUtil.isTaskRelatedEntityCountEnabledGlobally(taskServiceConfiguration)) {
            if (StringUtils.isNotEmpty(task.getParentTaskId())) {
                TaskEntity parentTaskEntity = taskServiceConfiguration.getTaskService().getTask(task.getParentTaskId());
                if (CountingEntityUtil.isTaskRelatedEntityCountEnabled(parentTaskEntity)) {
                    CountingTaskEntity countingParentTaskEntity = (CountingTaskEntity) parentTaskEntity;
                    countingParentTaskEntity.setSubTaskCount(countingParentTaskEntity.getSubTaskCount() + 1);
                }
            }
        }

        return task;
    }
}
