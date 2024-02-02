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

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandContextCloseListener;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class AsyncJobAddedNotification implements CommandContextCloseListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncJobAddedNotification.class);

    protected JobInfoEntity job;
    protected AsyncExecutor asyncExecutor;

    public AsyncJobAddedNotification(JobInfoEntity job, AsyncExecutor asyncExecutor) {
        this.job = job;
        this.asyncExecutor = asyncExecutor;
    }

    @Override
    public void closed(CommandContext commandContext) {
        execute(commandContext);
    }

    public void execute(CommandContext commandContext) {
        asyncExecutor.executeAsyncJob(job);
    }

    @Override
    public void closing(CommandContext commandContext) {
    }

    @Override
    public void afterSessionsFlush(CommandContext commandContext) {
    }

    @Override
    public void closeFailure(CommandContext commandContext) {
    }
    
    @Override
    public Integer order() {
        return 10;
    }

    @Override
    public boolean multipleAllowed() {
        return true;
    }
}
