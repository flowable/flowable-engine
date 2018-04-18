package org.flowable.engine.impl;

import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.cmd.CreateTaskCmd;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskBuilder;
import org.flowable.task.service.impl.BaseTaskBuilderImpl;

/**
 * {@link TaskBuilder} implementation
 */
public class TaskBuilderImpl extends BaseTaskBuilderImpl {
    TaskBuilderImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public Task create() {
        return commandExecutor.execute(new CreateTaskCmd(this));
    }

}
