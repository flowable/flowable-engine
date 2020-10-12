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

import java.util.concurrent.ThreadPoolExecutor;

import javax.enterprise.concurrent.ManagedThreadFactory;

import org.flowable.common.engine.impl.async.DefaultAsyncTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple JSR-236 async job executor to allocate threads through {@link ManagedThreadFactory}. Falls back to {@link AsyncExecutor} when a thread factory was not referenced in configuration.
 * 
 * In Java EE 7, all application servers should provide access to a {@link ManagedThreadFactory}.
 * 
 * @author Dimitris Mandalidis
 * @deprecated The factory should be configured in the engine configuration
 */
@Deprecated
public class ManagedAsyncJobExecutor extends DefaultAsyncJobExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedAsyncJobExecutor.class);

    protected ManagedThreadFactory threadFactory;

    public ManagedThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public void setThreadFactory(ManagedThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    @Override
    protected void initAsyncJobExecutionThreadPool() {
        if (threadFactory == null) {
            LOGGER.warn("A managed thread factory was not found, falling back to self-managed threads");
            super.initAsyncJobExecutionThreadPool();
        } else if (taskExecutor != null) {
            // This is for backwards compatibility
            // If there is no task executor then use the Default one and start it immediately.
            DefaultAsyncTaskExecutor defaultAsyncTaskExecutor = new DefaultAsyncTaskExecutor();
            defaultAsyncTaskExecutor.setThreadFactory(threadFactory);
            defaultAsyncTaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
            defaultAsyncTaskExecutor.start();
            this.taskExecutor = defaultAsyncTaskExecutor;
            this.shutdownTaskExecutor = true;
        }
    }
}
