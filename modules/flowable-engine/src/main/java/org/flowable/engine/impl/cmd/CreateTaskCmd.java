package org.flowable.engine.impl.cmd;

import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;

/**
 * Creates new task by {@link org.flowable.task.api.TaskBuilder}
 *
 * @author martin.grofcik
 */
public class CreateTaskCmd implements Command<Task> {
    protected TaskInfo taskTemplate;

    public CreateTaskCmd(TaskInfo taskTemplate) {
        this.taskTemplate = taskTemplate;
    }

    @Override
    public Task execute(CommandContext commandContext) {
        return CommandContextUtil.getTaskService().createTask(this.taskTemplate);
    }
}
