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
package org.flowable.engine.impl.asyncexecutor;

import org.flowable.engine.common.api.FlowableOptimisticLockingException;
import org.flowable.engine.common.impl.context.Context;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandConfig;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cmd.ExecuteAsyncJobCmd;
import org.flowable.engine.impl.cmd.LockExclusiveJobCmd;
import org.flowable.engine.impl.cmd.UnlockExclusiveJobCmd;
import org.flowable.engine.impl.jobexecutor.FailedJobCommandFactory;
import org.flowable.engine.impl.persistence.entity.AbstractRuntimeJobEntity;
import org.flowable.engine.impl.persistence.entity.JobInfoEntity;
import org.flowable.engine.impl.persistence.entity.JobInfoEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.runtime.Job;
import org.flowable.engine.runtime.JobInfo;
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
    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    protected JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager;
    protected AsyncRunnableExecutionExceptionHandler asyncRunnableExecutionExceptionHandler;

    public ExecuteAsyncRunnable(String jobId, ProcessEngineConfigurationImpl processEngineConfiguration, 
            JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager,
            AsyncRunnableExecutionExceptionHandler asyncRunnableExecutionExceptionHandler) {
        this.jobId = jobId;
        this.processEngineConfiguration = processEngineConfiguration;
        this.jobEntityManager = jobEntityManager;
        this.asyncRunnableExecutionExceptionHandler = asyncRunnableExecutionExceptionHandler;
    }

    public ExecuteAsyncRunnable(JobInfo job, ProcessEngineConfigurationImpl processEngineConfiguration, 
            JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager,
            AsyncRunnableExecutionExceptionHandler asyncRunnableExecutionExceptionHandler) {
        this.job = job;
        this.jobId = job.getId();
        this.processEngineConfiguration = processEngineConfiguration;
        this.jobEntityManager = jobEntityManager;
        this.asyncRunnableExecutionExceptionHandler = asyncRunnableExecutionExceptionHandler;
    }

    public void run() {

        if (job == null) {
            job = processEngineConfiguration.getCommandExecutor().execute(new Command<JobInfoEntity>() {
                @Override
                public JobInfoEntity execute(CommandContext commandContext) {
                    return jobEntityManager.findById(jobId);
                }
            });
        }
        
        if (isHandledByV5Engine()) {
            return;
        }
        
        if (job instanceof AbstractRuntimeJobEntity) {

            boolean lockNotNeededOrSuccess = lockJobIfNeeded();
    
            if (lockNotNeededOrSuccess) {
                executeJob();
                unlockJobIfNeeded();
            }
            
        } else { // history jobs
            executeJob();
            
        }

    }

    protected boolean isHandledByV5Engine() {
        if (!(job instanceof Job)) { // v5 only knew one type of jobs
            return false;
        }
        boolean isFlowable5ProcessDefinition = Flowable5Util.isFlowable5ProcessDefinitionId(processEngineConfiguration, ((Job) job).getProcessDefinitionId());
        if (isFlowable5ProcessDefinition) {
            return processEngineConfiguration.getCommandExecutor().execute(new Command<Boolean>() {
                @Override
                public Boolean execute(CommandContext commandContext) {
                    CommandContextUtil.getProcessEngineConfiguration(commandContext).getFlowable5CompatibilityHandler().executeJobWithLockAndRetry((Job) job);
                    return true;
                }
            });
        }
        return false;
    }

    protected void executeJob() {
        try {
            processEngineConfiguration.getCommandExecutor().execute(new ExecuteAsyncJobCmd(jobId, jobEntityManager));

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
        Job job = (Job) this.job; // This method is only called for a regular Job
        try {
            if (job.isExclusive()) {
                processEngineConfiguration.getCommandExecutor().execute(new UnlockExclusiveJobCmd(job));
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

    /**
     * Returns true if lock succeeded, or no lock was needed. Returns false if locking was unsuccessful.
     */
    protected boolean lockJobIfNeeded() {
        Job job = (Job) this.job; // This method is only called for a regular Job
        try {
            if (job.isExclusive()) {
                processEngineConfiguration.getCommandExecutor().execute(new LockExclusiveJobCmd(job));
            }

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
            processEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {
                public Void execute(CommandContext commandContext) {
                    CommandContextUtil.getJobManager(commandContext).unacquire(job);
                    return null;
                }
            });
        }
    }

    protected void handleFailedJob(final Throwable exception) {
        AsyncRunnableExecutionExceptionHandler exceptionHandler;
        if (asyncRunnableExecutionExceptionHandler != null) {
            exceptionHandler = asyncRunnableExecutionExceptionHandler;
        } else {
            exceptionHandler = processEngineConfiguration.getAsyncRunnableExecutionExceptionHandler();
        }
        
        if (exceptionHandler != null && exceptionHandler.handleException(processEngineConfiguration, job, exception)) {
            return;
        }
        defaultHandleFailedJob(exception);
    }

    protected void defaultHandleFailedJob(final Throwable exception) {
        processEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                
                // Finally, Throw the exception to indicate the ExecuteAsyncJobCmd failed
                String message = "Job " + jobId + " failed";
                LOGGER.error(message, exception);
                
                if (job instanceof AbstractRuntimeJobEntity) {
                    AbstractRuntimeJobEntity runtimeJob = (AbstractRuntimeJobEntity) job;
                    if (runtimeJob.getProcessDefinitionId() != null && Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, runtimeJob.getProcessDefinitionId())) {
                        Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
                        compatibilityHandler.handleFailedJob(runtimeJob, exception);
                        return null;
                    }
                }

                CommandConfig commandConfig = processEngineConfiguration.getCommandExecutor().getDefaultConfig().transactionRequiresNew();
                FailedJobCommandFactory failedJobCommandFactory = CommandContextUtil.getFailedJobCommandFactory();
                Command<Object> cmd = failedJobCommandFactory.getCommand(job.getId(), exception);

                LOGGER.trace("Using FailedJobCommandFactory '{}' and command of type '{}'", failedJobCommandFactory.getClass(), cmd.getClass());
                processEngineConfiguration.getCommandExecutor().execute(commandConfig, cmd);

                // Dispatch an event, indicating job execution failed in a
                // try-catch block, to prevent the original exception to be swallowed
                if (CommandContextUtil.getEventDispatcher().isEnabled()) {
                    try {
                        CommandContextUtil.getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityExceptionEvent(FlowableEngineEventType.JOB_EXECUTION_FAILURE, job, exception));
                    } catch (Throwable ignore) {
                        LOGGER.warn("Exception occurred while dispatching job failure event, ignoring.", ignore);
                    }
                }

                return null;
            }

        });
    }

}
