package org.flowable.engine.impl.cmd;

import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskBuilder;

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
        return CommandContextUtil.getTaskService().createTask(this.taskBuilder);
    }
}
