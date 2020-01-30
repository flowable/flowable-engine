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

import org.flowable.common.engine.impl.cfg.TransactionListener;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.job.api.JobInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class JobAddedTransactionListener implements TransactionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobAddedTransactionListener.class);

    protected JobInfo job;
    protected AsyncExecutor asyncExecutor;
    protected CommandExecutor commandExecutor;

    public JobAddedTransactionListener(JobInfo job, AsyncExecutor asyncExecutor, CommandExecutor commandExecutor) {
        this.job = job;
        this.asyncExecutor = asyncExecutor;
        this.commandExecutor = commandExecutor;
    }

    @Override
    public void execute(CommandContext commandContext) {
        // No need to wrap this call in a new command context, as otherwise the
        // call to the executeAsyncJob would require a new database connection and transaction
        // which would block the current connection/transaction (of the calling thread)
        // until the job has been handed of to the async executor.
        // When the connection pool is small, this might lead to contention and (temporary) locks.
        asyncExecutor.executeAsyncJob(job);
    }
}
