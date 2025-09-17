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

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.JobInfo;
import org.flowable.job.service.InternalJobCompatibilityManager;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.event.impl.FlowableJobEventBuilder;
import org.flowable.job.service.impl.persistence.entity.AbstractRuntimeJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author martin.grofcik
 */
public class DefaultAsyncRunnableExecutionExceptionHandler implements AsyncRunnableExecutionExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAsyncRunnableExecutionExceptionHandler.class);

    @Override
    public boolean handleException(final JobServiceConfiguration jobServiceConfiguration, final JobInfo job, final Throwable exception) {
        jobServiceConfiguration.getCommandExecutor().execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {

                // Finally, Throw the exception to indicate the ExecuteAsyncJobCmd failed
                String message = "Job " + job.getId() + " failed";
                LOGGER.error(message, exception);

                if (job instanceof AbstractRuntimeJobEntity runtimeJob) {
                    InternalJobCompatibilityManager internalJobCompatibilityManager = jobServiceConfiguration.getInternalJobCompatibilityManager();
                    if (internalJobCompatibilityManager != null && internalJobCompatibilityManager.isFlowable5Job(runtimeJob)) {
                        internalJobCompatibilityManager.handleFailedV5Job(runtimeJob, exception);
                        return null;
                    }
                }

                CommandConfig commandConfig = jobServiceConfiguration.getCommandExecutor().getDefaultConfig().transactionRequiresNew();
                FailedJobCommandFactory failedJobCommandFactory = jobServiceConfiguration.getFailedJobCommandFactory();
                Command<Object> cmd = failedJobCommandFactory.getCommand(job.getId(), exception);

                LOGGER.trace("Using FailedJobCommandFactory '{}' and command of type '{}'", failedJobCommandFactory.getClass(), cmd.getClass());
                jobServiceConfiguration.getCommandExecutor().execute(commandConfig, cmd);

                // Dispatch an event, indicating job execution failed in a
                // try-catch block, to prevent the original exception to be swallowed
                FlowableEventDispatcher eventDispatcher = jobServiceConfiguration.getEventDispatcher();
                if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                    try {
                        eventDispatcher.dispatchEvent(FlowableJobEventBuilder.createEntityExceptionEvent(
                                FlowableEngineEventType.JOB_EXECUTION_FAILURE, job, exception), jobServiceConfiguration.getEngineName());
                    } catch (Throwable ignore) {
                        LOGGER.warn("Exception occurred while dispatching job failure event, ignoring.", ignore);
                    }
                }

                return null;
            }

        });

        return true;
    }


}
