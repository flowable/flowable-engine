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
package org.flowable.job.service.impl.asyncexecutor;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobInfo;
import org.flowable.job.service.InternalJobCompatibilityManager;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.cmd.ExecuteAsyncJobCmd;
import org.flowable.job.service.impl.cmd.LockExclusiveJobCmd;
import org.flowable.job.service.impl.cmd.UnlockExclusiveJobCmd;
import org.flowable.job.service.impl.persistence.entity.AbstractRuntimeJobEntity;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntity;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntityManager;
import org.flowable.job.service.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class ExecuteAsyncRunnable implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteAsyncRunnable.class);

    protected String jobId;
    protected JobInfo job;
    protected JobServiceConfiguration jobServiceConfiguration;
    protected JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager;
    protected List<AsyncRunnableExecutionExceptionHandler> asyncRunnableExecutionExceptionHandlers;

    public ExecuteAsyncRunnable(String jobId, JobServiceConfiguration jobServiceConfiguration,
            JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager,
            AsyncRunnableExecutionExceptionHandler asyncRunnableExecutionExceptionHandler) {
        
        initialize(jobId, null, jobServiceConfiguration, jobEntityManager, asyncRunnableExecutionExceptionHandler);
    }

    public ExecuteAsyncRunnable(JobInfo job, JobServiceConfiguration jobServiceConfiguration,
                                JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager,
                                AsyncRunnableExecutionExceptionHandler asyncRunnableExecutionExceptionHandler) {
        
        initialize(job.getId(), job, jobServiceConfiguration, jobEntityManager, asyncRunnableExecutionExceptionHandler);
    }

    private void initialize(String jobId, JobInfo job, JobServiceConfiguration jobServiceConfiguration, JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager, AsyncRunnableExecutionExceptionHandler asyncRunnableExecutionExceptionHandler) {
        this.job = job;
        this.jobId = jobId;
        this.jobServiceConfiguration = jobServiceConfiguration;
        this.jobEntityManager = jobEntityManager;
        this.asyncRunnableExecutionExceptionHandlers = initializeExceptionHandlers(jobServiceConfiguration, asyncRunnableExecutionExceptionHandler);
    }

    private List<AsyncRunnableExecutionExceptionHandler> initializeExceptionHandlers(JobServiceConfiguration jobServiceConfiguration, AsyncRunnableExecutionExceptionHandler asyncRunnableExecutionExceptionHandler) {
        List<AsyncRunnableExecutionExceptionHandler> asyncRunnableExecutionExceptionHandlers = new ArrayList<>();
        if (asyncRunnableExecutionExceptionHandler != null) {
            asyncRunnableExecutionExceptionHandlers.add(asyncRunnableExecutionExceptionHandler);
        }
        
        if (jobServiceConfiguration.getAsyncRunnableExecutionExceptionHandlers() != null) {
            asyncRunnableExecutionExceptionHandlers.addAll(jobServiceConfiguration.getAsyncRunnableExecutionExceptionHandlers());
        }
        
        return asyncRunnableExecutionExceptionHandlers;
    }

    @Override
    public void run() {

        if (job == null) {
            job = jobServiceConfiguration.getCommandExecutor().execute(new Command<JobInfoEntity>() {
                @Override
                public JobInfoEntity execute(CommandContext commandContext) {
                    return jobEntityManager.findById(jobId);
                }
            });
        }

        if (job instanceof Job) {
            Job jobObject = (Job) job;
            InternalJobCompatibilityManager internalJobCompatibilityManager = jobServiceConfiguration.getInternalJobCompatibilityManager();
            if (internalJobCompatibilityManager != null && internalJobCompatibilityManager.isFlowable5Job(jobObject)) {
                internalJobCompatibilityManager.executeV5JobWithLockAndRetry(jobObject);
                return;
            }
        }

        if (job instanceof AbstractRuntimeJobEntity) {

            boolean lockingNeeded = ((AbstractRuntimeJobEntity) job).isExclusive();
            boolean executeJob = true;
            if (lockingNeeded) {
                executeJob = lockJob();
            }
            if (executeJob) {
                executeJob(lockingNeeded);
            }

        } else { // history jobs
            executeJob(false); // no locking for history jobs needed

        }

    }

    protected void executeJob(final boolean unlock) {
        try {
            jobServiceConfiguration.getCommandExecutor().execute(new Command<Void>() {
                @Override
                public Void execute(CommandContext commandContext) {
                    new ExecuteAsyncJobCmd(jobId, jobEntityManager).execute(commandContext);
                    if (unlock) {
                        // Part of the same transaction to avoid a race condition with the
                        // potentially new jobs (wrt process instance locking) that are created 
                        // during the execution of the original job 
                        new UnlockExclusiveJobCmd((Job) job).execute(commandContext);
                    }
                    return null;
                }
            });

        } catch (final FlowableOptimisticLockingException e) {

            handleFailedJob(e);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Optimistic locking exception during job execution. If you have multiple async executors running against the same database, "
                        + "this exception means that this thread tried to acquire an exclusive job, which already was changed by another async executor thread."
                        + "This is expected behavior in a clustered environment. " + "You can ignore this message if you indeed have multiple job executor threads running against the same database. "
                        + "Exception message: {}", e.getMessage());
            }

        } catch (Throwable exception) {
            handleFailedJob(exception);
        }
    }

    protected void unlockJobIfNeeded() {
        if (this.job instanceof HistoryJob) {
            return;
            // no unlocking needed for history job
        }
        
        Job job = (Job) this.job; // This method is only called for a regular Job
        try {
            if (job.isExclusive()) {
                jobServiceConfiguration.getCommandExecutor().execute(new UnlockExclusiveJobCmd(job));
            }

        } catch (FlowableOptimisticLockingException optimisticLockingException) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Optimistic locking exception while unlocking the job. If you have multiple async executors running against the same database, "
                        + "this exception means that this thread tried to acquire an exclusive job, which already was changed by another async executor thread."
                        + "This is expected behavior in a clustered environment. " + "You can ignore this message if you indeed have multiple job executor acquisition threads running against the same database. "
                        + "Exception message: {}", optimisticLockingException.getMessage());
            }

        } catch (Throwable t) {
            LOGGER.error("Error while unlocking exclusive job {}", job.getId(), t);
        }
    }

    protected boolean lockJob() {
        Job job = (Job) this.job; // This method is only called for a regular Job
        try {
            jobServiceConfiguration.getCommandExecutor().execute(new LockExclusiveJobCmd(job));

        } catch (Throwable lockException) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Could not lock exclusive job. Unlocking job so it can be acquired again. Caught exception: {}", lockException.getMessage());
            }

            // Release the job again so it can be acquired later or by another node
            unacquireJob();

            return false;
        }

        return true;
    }

    protected void unacquireJob() {
        CommandContext commandContext = Context.getCommandContext();
        if (commandContext != null) {
            CommandContextUtil.getJobManager(commandContext).unacquire(job);
        } else {
            jobServiceConfiguration.getCommandExecutor().execute(new Command<Void>() {
                @Override
                public Void execute(CommandContext commandContext) {
                    CommandContextUtil.getJobManager(commandContext).unacquire(job);
                    return null;
                }
            });
        }
    }

    protected void handleFailedJob(final Throwable exception) {
        for (AsyncRunnableExecutionExceptionHandler asyncRunnableExecutionExceptionHandler : asyncRunnableExecutionExceptionHandlers) {
            if (asyncRunnableExecutionExceptionHandler.handleException(this.jobServiceConfiguration, this.job, exception)) {
                
                // Needs to run in a separate transaction as the original transaction has been marked for rollback
                unlockJobIfNeeded();
                
                return;
            }
        }
        
        LOGGER.error("Unable to handle exception {} for job {}.", exception, job);
        throw new FlowableException("Unable to handle exception " + exception.getMessage() + " for job " + job.getId() + ".", exception);
    }

}
