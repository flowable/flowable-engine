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
package org.flowable.spring.job.service;

import java.util.concurrent.RejectedExecutionException;

import org.flowable.job.api.JobInfo;
import org.flowable.job.service.impl.asyncexecutor.AsyncJobExecutorConfiguration;
import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncJobExecutor;

/**
 * <p>
 *     This is an {@link org.flowable.job.service.impl.asyncexecutor.AsyncExecutor} implementation which allows invoking a custom job rejected jobs handler.
 * </p>
 * <p>
 * The idea behind this implementation is to externalize the configuration of the task executor, 
 * so it can leverage to Application servers controller thread pools, for example using the commonj API.
 * The use of unmanaged thread in application servers is discouraged by the Java EE spec.
 * </p>
 * 
 * @author Pablo Ganga
 * @author Joram Barrez
 */
public class SpringAsyncExecutor extends DefaultAsyncJobExecutor {

    protected SpringRejectedJobsHandler rejectedJobsHandler;

    public SpringAsyncExecutor() {
        super();
    }

    public SpringAsyncExecutor(AsyncJobExecutorConfiguration configuration) {
        super(configuration);
    }

    public SpringRejectedJobsHandler getRejectedJobsHandler() {
        return rejectedJobsHandler;
    }

    /**
     * {@link SpringRejectedJobsHandler} implementation that will be used when jobs were rejected by the task executor.
     * 
     * @param rejectedJobsHandler
     */
    public void setRejectedJobsHandler(SpringRejectedJobsHandler rejectedJobsHandler) {
        this.rejectedJobsHandler = rejectedJobsHandler;
    }

    @Override
    public boolean executeAsyncJob(JobInfo job) {
        try {
            taskExecutor.execute(createRunnableForJob(job));
            return true;
        } catch (RejectedExecutionException e) {
            sendRejectedEvent(job);
            if (rejectedJobsHandler == null) {
                unacquireJobAfterRejection(job);
            } else {
                rejectedJobsHandler.jobRejected(this, job);
            }
            return false;
        }
    }

    @Override
    protected void initAsyncJobExecutionThreadPool() {
        // Do nothing, using the Spring taskExecutor
    }

}
