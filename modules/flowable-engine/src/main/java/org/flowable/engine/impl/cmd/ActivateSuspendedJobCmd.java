package org.flowable.engine.impl.cmd;

import org.flowable.engine.impl.interceptor.Command;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.SuspendedJobEntity;

/**
 * This {@link org.flowable.engine.impl.interceptor.Command} activates suspended job
 */
public class ActivateSuspendedJobCmd implements Command<Void> {

    protected SuspendedJobEntity suspendedJob;

    public ActivateSuspendedJobCmd(SuspendedJobEntity suspendedJob) {
        this.suspendedJob = suspendedJob;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        commandContext.getJobManager().activateSuspendedJob(this.suspendedJob);
        return null;
    }
}
