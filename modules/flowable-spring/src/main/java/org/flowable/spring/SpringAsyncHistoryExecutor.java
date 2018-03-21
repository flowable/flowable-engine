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
package org.flowable.spring;

import org.flowable.job.service.impl.asyncexecutor.UnacquireAsyncHistoryJobExceptionHandler;
import org.springframework.core.task.TaskExecutor;

/**
 * @deprecated use {@Link org.flowable.spring.job.service.SpringAsyncHistoryExecutor}
 */
@Deprecated
public class SpringAsyncHistoryExecutor extends SpringAsyncExecutor {

    public SpringAsyncHistoryExecutor() {
        super();
        init();
    }
     
    public SpringAsyncHistoryExecutor(TaskExecutor taskExecutor, SpringRejectedJobsHandler rejectedJobsHandler) {
        super(taskExecutor, rejectedJobsHandler);
        init();
    }
    
    protected void init() {
        setTimerRunnableNeeded(false);
        setAcquireRunnableThreadName("flowable-acquire-history-jobs");
        setResetExpiredRunnableName("flowable-reset-expired-history-jobs");
        setAsyncRunnableExecutionExceptionHandler(new UnacquireAsyncHistoryJobExceptionHandler());
    }
    
    @Override
    protected void initializeJobEntityManager() {
        if (jobEntityManager == null) {
            jobEntityManager = jobServiceConfiguration.getHistoryJobEntityManager();
        }
    }

}
