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
import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.flowable.job.service.impl.asyncexecutor.ExecuteAsyncRunnable;
import org.springframework.core.task.TaskExecutor;

/**
 * <p>
 * This is a spring based implementation of the {@link JobExecutor} using spring abstraction {@link TaskExecutor} for performing background task execution.
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

    protected TaskExecutor taskExecutor;
    protected SpringRejectedJobsHandler rejectedJobsHandler;

    public SpringAsyncExecutor() {
    }
    
    public SpringAsyncExecutor(TaskExecutor taskExecutor) {
        this(taskExecutor, null);
    }

    public SpringAsyncExecutor(TaskExecutor taskExecutor, SpringRejectedJobsHandler rejectedJobsHandler) {
        this.taskExecutor = taskExecutor;
        this.rejectedJobsHandler = rejectedJobsHandler;
    }

    public TaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    /**
     * Required spring injected {@link TaskExecutor} implementation that will be used to execute runnable jobs.
     * 
     * @param taskExecutor
     */
    public void setTaskExecutor(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public SpringRejectedJobsHandler getRejectedJobsHandler() {
        return rejectedJobsHandler;
    }

    /**
     * {@link RejectedJobsHandler} implementation that will be used when jobs were rejected by the task executor.
     * 
     * @param rejectedJobsHandler
     */
    public void setRejectedJobsHandler(SpringRejectedJobsHandler rejectedJobsHandler) {
        this.rejectedJobsHandler = rejectedJobsHandler;
    }

    @Override
    public boolean executeAsyncJob(JobInfo job) {
        try {
            taskExecutor.execute(new ExecuteAsyncRunnable(job, jobServiceConfiguration, jobEntityManager, asyncRunnableExecutionExceptionHandler));
            return true;
        } catch (RejectedExecutionException e) {
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
