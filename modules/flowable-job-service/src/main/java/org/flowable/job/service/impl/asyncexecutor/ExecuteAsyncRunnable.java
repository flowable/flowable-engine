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
import java.util.Map;

import org.flowable.common.engine.api.FlowableBatchPartMigrationException;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.tenant.TenantContext;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.tenant.CurrentTenant;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobInfo;
import org.flowable.job.service.InternalJobCompatibilityManager;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.JobProcessorContext;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.event.impl.FlowableJobEventBuilder;
import org.flowable.job.service.impl.cmd.ExecuteAsyncRunnableJobCmd;
import org.flowable.job.service.impl.cmd.LockExclusiveJobCmd;
import org.flowable.job.service.impl.cmd.UnlockExclusiveJobCmd;
import org.flowable.job.service.impl.nontx.NonTransactionalJobHandler;
import org.flowable.job.service.impl.persistence.entity.AbstractRuntimeJobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntity;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntityManager;
import org.flowable.job.service.impl.util.JobProcessorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class ExecuteAsyncRunnable implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteAsyncRunnable.class);

    protected final JobInfo job;
    protected final JobExecutionObservationProvider jobExecutionObservationProvider;
    protected JobServiceConfiguration jobServiceConfiguration;
    protected JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager;
    protected List<AsyncRunnableExecutionExceptionHandler> asyncRunnableExecutionExceptionHandlers;

    public ExecuteAsyncRunnable(JobInfo job, JobServiceConfiguration jobServiceConfiguration,
                                JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager,
                                AsyncRunnableExecutionExceptionHandler asyncRunnableExecutionExceptionHandler,
                                JobExecutionObservationProvider jobExecutionObservationProvider) {

        this.job = job;
        this.jobServiceConfiguration = jobServiceConfiguration;
        this.jobEntityManager = jobEntityManager;
        this.asyncRunnableExecutionExceptionHandlers = initializeExceptionHandlers(jobServiceConfiguration, asyncRunnableExecutionExceptionHandler);
        this.jobExecutionObservationProvider = jobExecutionObservationProvider;
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
        TenantContext tenantContext = CurrentTenant.getTenantContext();
        JobExecutionObservation observation = jobExecutionObservationProvider.create(job);
        try {
            tenantContext.setTenantId(job.getTenantId());
            observation.start();
            runInternally(observation);
        } finally {
            observation.stop();
            tenantContext.clearTenantId();
        }
    }

    protected void runInternally(JobExecutionObservation observation) {

        if (job instanceof Job jobObject) {
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
                executeJob = lockJob(observation);
            }
            if (executeJob) {
                executeJob(lockingNeeded, observation);
            }

        } else { // history jobs
            executeJob(false, observation); // no locking for history jobs needed

        }

    }

    protected void executeJob(final boolean unlock, JobExecutionObservation observation) {
        try (JobExecutionObservation.Scope ignored = observation.executionScope()) {

            boolean jobHandled = false;
            if (job instanceof JobEntity) { // only instances of JobEntity can be made non-transactional
                Map<String, JobHandler> jobHandlers = jobServiceConfiguration.getJobHandlers();
                JobHandler jobHandler = jobHandlers.get(job.getJobHandlerType());

                if (jobHandler instanceof NonTransactionalJobHandler nonTransactionalJobHandler) {
                    jobHandled = handleNontransactionalJob(nonTransactionalJobHandler, unlock);
                }
            }

            if (!jobHandled) {
                handleTransactionalJob(unlock);
            }

        } catch (final FlowableOptimisticLockingException e) {

            try {
                handleFailedJob(e);
            } catch (Exception fe) {
                // no additional handling is needed
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Optimistic locking exception during job execution. If you have multiple async executors running against the same database, "
                        + "this exception means that this thread tried to acquire an exclusive job, which already was changed by another async executor thread."
                        + "This is expected behavior in a clustered environment. " + "You can ignore this message if you indeed have multiple job executor threads running against the same database. "
                        + "Exception message: {}", e.getMessage());
            }

            observation.executionError(e);

        } catch (Throwable exception) {
            try {
                handleFailedJob(exception);
            } finally {
                observation.executionError(exception);
            }
        }
    }

    protected void handleTransactionalJob(boolean unlock) {
        // Wrapping in a command and passing to the command executor starts a new transaction
        jobServiceConfiguration.getCommandExecutor().execute(
                new ExecuteAsyncRunnableJobCmd(job.getId(), jobEntityManager, jobServiceConfiguration, unlock));
    }

    protected boolean handleNontransactionalJob(NonTransactionalJobHandler<Object> jobHandler, boolean unlock) {

        JobProcessorUtil.callJobProcessors(jobServiceConfiguration, JobProcessorContext.Phase.BEFORE_EXECUTE, (JobEntity) job);

        // If an exception is thrown during job handler exception, it goes up and will be caught in the general exception handling.
        // The delete at the end won't happen in that case.

        Object nonTransactionalOutput = jobHandler.executeNonTransactionally((JobEntity) job, job.getJobHandlerConfiguration());

        FlowableEventDispatcher eventDispatcher = jobServiceConfiguration.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableJobEventBuilder.createEntityEvent(FlowableEngineEventType.JOB_EXECUTION_SUCCESS, job),
                    jobServiceConfiguration.getEngineName());
        }

        // The delete still needs to happen in a new transaction
        jobServiceConfiguration.getCommandExecutor().execute(commandContext -> {
            jobHandler.afterExecute((JobEntity) job, job.getJobHandlerConfiguration(), nonTransactionalOutput, commandContext);

            if (unlock) {
                // Part of the same transaction to avoid a race condition with the
                // potentially new jobs (wrt process instance locking) that are created
                // during the execution of the original job
                new UnlockExclusiveJobCmd((Job) job, jobServiceConfiguration).execute(commandContext);
            }

            jobServiceConfiguration.getJobEntityManager().delete((JobEntity) job);
            return null;
        });

        return true;

    }

    protected void unlockJobIfNeeded() {
        if (this.job instanceof HistoryJob) {
            return;
            // no unlocking needed for history job
        }
        
        Job job = (Job) this.job; // This method is only called for a regular Job
        try {
            if (job.isExclusive()) {
                jobServiceConfiguration.getCommandExecutor().execute(new UnlockExclusiveJobCmd(job, jobServiceConfiguration));
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

    protected boolean lockJob(JobExecutionObservation observation) {
        Job job = (Job) this.job; // This method is only called for a regular Job
        try (JobExecutionObservation.Scope ignored = observation.lockScope()) {
            jobServiceConfiguration.getCommandExecutor().execute(new LockExclusiveJobCmd(job, jobServiceConfiguration));

        } catch (Throwable lockException) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Could not lock exclusive job. Unlocking job so it can be acquired again. Caught exception: {}", lockException.getMessage());
            }

            // Release the job again so it can be acquired later or by another node
            unacquireJob();

            observation.lockError(lockException);

            return false;
        }

        return true;
    }

    protected void unacquireJob() {
        CommandContext commandContext = Context.getCommandContext();
        if (commandContext != null) {
            jobServiceConfiguration.getJobManager().unacquire(job);
        } else {
            jobServiceConfiguration.getCommandExecutor().execute(new Command<Void>() {
                @Override
                public Void execute(CommandContext commandContext) {
                    jobServiceConfiguration.getJobManager().unacquire(job);
                    return null;
                }
            });
        }
    }

    protected void handleFailedJob(final Throwable exception) {
        if (exception instanceof FlowableBatchPartMigrationException && ((FlowableBatchPartMigrationException) exception).isIgnoreFailedJob()) {
            jobServiceConfiguration.getCommandExecutor().execute(new Command<>() {
                @Override
                public Void execute(CommandContext commandContext) {
                    CommandConfig commandConfig = jobServiceConfiguration.getCommandExecutor().getDefaultConfig().transactionRequiresNew();
                    return jobServiceConfiguration.getCommandExecutor().execute(commandConfig, new Command<>() {
                        @Override
                        public Void execute(CommandContext commandContext2) {
                            jobServiceConfiguration.getJobManager().deleteExecutableJob(job);
                            return null;
                        }
                    });
                }
            });
            
            return;
        }
        
        for (AsyncRunnableExecutionExceptionHandler asyncRunnableExecutionExceptionHandler : asyncRunnableExecutionExceptionHandlers) {
            if (asyncRunnableExecutionExceptionHandler.handleException(this.jobServiceConfiguration, this.job, exception)) {
                
                // Needs to run in a separate transaction as the original transaction has been marked for rollback
                unlockJobIfNeeded();
                
                return;
            }
        }
        
        LOGGER.error("Unable to handle exception {} for job {}.", exception, job);
        throw new FlowableException("Unable to handle exception " + exception.getMessage() + " for " + job + ".", exception);
    }

}
