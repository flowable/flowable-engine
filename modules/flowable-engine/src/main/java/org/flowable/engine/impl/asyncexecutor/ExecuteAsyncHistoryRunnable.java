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
import org.flowable.engine.common.impl.interceptor.CommandConfig;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cmd.ExecuteAsyncHistoryJobCmd;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.interceptor.Command;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.jobexecutor.FailedJobCommandFactory;
import org.flowable.engine.impl.persistence.entity.HistoryJobEntity;
import org.flowable.engine.runtime.HistoryJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class ExecuteAsyncHistoryRunnable implements Runnable {

    private static Logger log = LoggerFactory.getLogger(ExecuteAsyncHistoryRunnable.class);

    protected String jobId;
    protected HistoryJob job;
    protected ProcessEngineConfigurationImpl processEngineConfiguration;

    public ExecuteAsyncHistoryRunnable(String jobId, ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.jobId = jobId;
        this.processEngineConfiguration = processEngineConfiguration;
    }

    public ExecuteAsyncHistoryRunnable(HistoryJob job, ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.job = job;
        this.jobId = job.getId();
        this.processEngineConfiguration = processEngineConfiguration;
    }

    public void run() {

        if (job == null) {
            job = processEngineConfiguration.getCommandExecutor().execute(new Command<HistoryJobEntity>() {
                @Override
                public HistoryJobEntity execute(CommandContext commandContext) {
                    return commandContext.getHistoryJobEntityManager().findById(jobId);
                }
            });
        }

        executeJob();
    }

    protected void executeJob() {
        try {
            processEngineConfiguration.getCommandExecutor().execute(new ExecuteAsyncHistoryJobCmd(jobId));

        } catch (final FlowableOptimisticLockingException e) {

            handleFailedJob(e);

            if (log.isDebugEnabled()) {
                log.debug("Optimistic locking exception during job execution. If you have multiple async executors running against the same database, "
                        + "this exception means that this thread tried to acquire an exclusive job, which already was changed by another async executor thread."
                        + "This is expected behavior in a clustered environment. " + "You can ignore this message if you indeed have multiple job executor threads running against the same database. "
                        + "Exception message: {}", e.getMessage());
            }

        } catch (Throwable exception) {
            handleFailedJob(exception);
        }
    }

    protected void unacquireJob() {
        CommandContext commandContext = Context.getCommandContext();
        if (commandContext != null) {
            commandContext.getJobManager().unacquire(job);
        } else {
            processEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {
                public Void execute(CommandContext commandContext) {
                    commandContext.getJobManager().unacquire(job);
                    return null;
                }
            });
        }
    }

    protected void handleFailedJob(final Throwable exception) {
        AsyncHistoryExceptionHandler exceptionHandler = processEngineConfiguration.getAsyncHistoryExceptionHandler();
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
                String message = "History job " + jobId + " failed";
                log.error(message, exception);

                CommandConfig commandConfig = processEngineConfiguration.getCommandExecutor().getDefaultConfig().transactionRequiresNew();
                FailedJobCommandFactory failedJobCommandFactory = commandContext.getFailedJobCommandFactory();
                Command<Object> cmd = failedJobCommandFactory.getCommand(job.getId(), exception);

                log.trace("Using FailedJobCommandFactory '{}' and command of type '{}'", failedJobCommandFactory.getClass(), cmd.getClass());
                processEngineConfiguration.getCommandExecutor().execute(commandConfig, cmd);

                // Dispatch an event, indicating job execution failed in a
                // try-catch block, to prevent the original exception to be swallowed
                if (commandContext.getEventDispatcher().isEnabled()) {
                    try {
                        commandContext.getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityExceptionEvent(FlowableEngineEventType.JOB_EXECUTION_FAILURE, job, exception));
                    } catch (Throwable ignore) {
                        log.warn("Exception occurred while dispatching job failure event, ignoring.", ignore);
                    }
                }

                return null;
            }

        });
    }

}
