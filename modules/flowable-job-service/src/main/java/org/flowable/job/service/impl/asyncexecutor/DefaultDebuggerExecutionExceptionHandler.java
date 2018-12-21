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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.flowable.job.api.JobInfo;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Swallow exception for the debugger executions and add debugger breakpoint again to the suspended jobs.
 * 
 * @author martin.grofcik
 */
public class DefaultDebuggerExecutionExceptionHandler implements AsyncRunnableExecutionExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDebuggerExecutionExceptionHandler.class);
    private static final String HANDLER_TYPE_BREAK_POINT = "breakpoint";

    @Override
    public boolean handleException(final JobServiceConfiguration jobServiceConfiguration, final JobInfo job, final Throwable exception) {
        if (HANDLER_TYPE_BREAK_POINT.equals(job.getJobHandlerType())) {
            LOGGER.debug("break point execution throws an exception which will be swallowed", exception);
            jobServiceConfiguration.getCommandExecutor().execute( 
                    commandContext -> {
                        JobEntity jobEntity = jobServiceConfiguration.getJobService().findJobById(job.getId());
                        SuspendedJobEntity suspendedJobEntity = jobServiceConfiguration.getJobService().moveJobToSuspendedJob(jobEntity);
                        if (exception != null) {
                            LOGGER.info("Debugger exception ", exception);
                            suspendedJobEntity.setExceptionMessage(exception.getMessage());
                            suspendedJobEntity.setExceptionStacktrace(ExceptionUtils.getStackTrace(exception));
                        }
                        return null;
                    }
            );
            return true;
        }
        return false;
    }
    
}
