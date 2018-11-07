package org.flowable.engine.impl.cmd;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskBuilder;
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
        Task task = CommandContextUtil.getTaskService().createTask(this.taskBuilder);
        if (CountingTaskUtil.isTaskRelatedEntityCountEnabledGlobally()) {
            if (StringUtils.isNotEmpty(task.getParentTaskId())) {
                TaskEntity parentTaskEntity = CommandContextUtil.getTaskService().getTask(task.getParentTaskId());
                if (CountingEntityUtil.isTaskRelatedEntityCountEnabled(parentTaskEntity)) {
                    CountingTaskEntity countingParentTaskEntity = (CountingTaskEntity) parentTaskEntity;
                    countingParentTaskEntity.setSubTaskCount(countingParentTaskEntity.getSubTaskCount() + 1);
                }
            }
        }
        return task;
    }
}
