package org.flowable.engine.impl.jobexecutor;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.agenda.MonitorParallelMultiInstanceOperation;
import org.flowable.engine.impl.cmd.JobRetryCmd;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryOnlyParallelInstanceMonitoringJobsCmd extends JobRetryCmd {

    protected static final Logger LOGGER = LoggerFactory.getLogger(RetryOnlyParallelInstanceMonitoringJobsCmd.class);

    private static final int DEFAULT_MONITOR_JOB_RETRIES_COUNT = 10;

    public RetryOnlyParallelInstanceMonitoringJobsCmd(String jobId, Throwable exception) {
        super(jobId, exception);
    }

    @Override
    public Object execute(CommandContext commandContext) {
        JobEntity failedJob = findJob(commandContext);
        if (failedJob != null && failedJob.getJobHandlerType().equals(AsyncParallelMultiInstanceMonitorJobHandler.TYPE)) {
            failedJob.setRetries(determineFailedJobRetries(failedJob));
        }
        return super.execute(commandContext);
    }

    private int determineFailedJobRetries(JobEntity failedJob) {
        if (failedJob.getExceptionMessage() != null && failedJob.getExceptionMessage().equals(MonitorParallelMultiInstanceOperation.FAILING_THE_MONITOR)) {
            return 0;
        }
        if (failedJob.getRetries() > 0) {
            return failedJob.getRetries() - 1;
        }
        return DEFAULT_MONITOR_JOB_RETRIES_COUNT;
    }

}
