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

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.JobInfo;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.util.CommandContextUtil;

public class UnacquireAsyncHistoryJobExceptionHandler implements AsyncRunnableExecutionExceptionHandler {

    @Override
    public boolean handleException(final JobServiceConfiguration jobServiceConfiguration, final JobInfo job, final Throwable exception) {
        if (job != null 
                && ("async-history".equals(job.getJobHandlerType()) || "async-history-zipped".equals(job.getJobHandlerType()) ) ) {
            
            return jobServiceConfiguration.getCommandExecutor().execute(new Command<Boolean>() {
                @Override
                public Boolean execute(CommandContext commandContext) {
                    CommandConfig commandConfig = jobServiceConfiguration.getCommandExecutor().getDefaultConfig().transactionRequiresNew();
                    return jobServiceConfiguration.getCommandExecutor().execute(commandConfig, new Command<Boolean>() {
                        @Override
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
