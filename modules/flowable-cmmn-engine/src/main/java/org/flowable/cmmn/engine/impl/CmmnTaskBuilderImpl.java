package org.flowable.cmmn.engine.impl;

import org.flowable.cmmn.engine.impl.cmd.CreateCmmnTaskCmd;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskBuilder;
import org.flowable.task.service.impl.BaseTaskBuilderImpl;

/**
 * {@link TaskBuilder} implementation
 */
public class CmmnTaskBuilderImpl extends BaseTaskBuilderImpl {

    CmmnTaskBuilderImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public Task create() {
        return commandExecutor.execute(new CreateCmmnTaskCmd(this));
    }

}
