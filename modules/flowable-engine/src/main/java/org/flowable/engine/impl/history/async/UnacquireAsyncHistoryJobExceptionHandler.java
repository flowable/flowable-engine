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
package org.flowable.engine.impl.history.async;

import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandConfig;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.impl.asyncexecutor.AsyncRunnableExecutionExceptionHandler;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.JobInfo;

public class UnacquireAsyncHistoryJobExceptionHandler implements AsyncRunnableExecutionExceptionHandler {

    @Override
    public boolean handleException(final ProcessEngineConfigurationImpl processEngineConfiguration, final JobInfo job, final Throwable exception) {
        if (job != null 
                && (AsyncHistoryJobHandler.JOB_TYPE.equals(job.getJobHandlerType()) || AsyncHistoryJobZippedHandler.JOB_TYPE.equals(job.getJobHandlerType()) ) ) {
            
            return processEngineConfiguration.getCommandExecutor().execute(new Command<Boolean>() {
                public Boolean execute(CommandContext commandContext) {
                    CommandConfig commandConfig = processEngineConfiguration.getCommandExecutor().getDefaultConfig().transactionRequiresNew();
                    return processEngineConfiguration.getCommandExecutor().execute(commandConfig, new Command<Boolean>() {
                        public Boolean execute(CommandContext commandContext2) {
                            CommandContextUtil.getJobManager(commandContext2).unacquireWithDecrementRetries(job);
                            return true;
                        }
                    });
                }
            });
        }
        return false;
    }

}
