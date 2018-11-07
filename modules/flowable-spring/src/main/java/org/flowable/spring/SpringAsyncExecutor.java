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

import org.springframework.core.task.TaskExecutor;

/**
 * <p>
 * This is a spring based implementation of the {@link JobExecutor} using spring abstraction {@link TaskExecutor} for performing background task execution.
 * </p>
 * <p>
 * The idea behind this implementation is to externalize the configuration of the task executor, so it can leverage to Application servers controller thread pools, for example using the commonj API.
 * The use of unmanaged thread in application servers is discouraged by the Java EE spec.
 * </p>
 * 
 * @author Pablo Ganga
 * @deprecated use {@link org.flowable.spring.job.service.SpringAsyncExecutor}
 */
@Deprecated
public class SpringAsyncExecutor extends org.flowable.spring.job.service.SpringAsyncExecutor {

    public SpringAsyncExecutor() {
    }

    public SpringAsyncExecutor(TaskExecutor taskExecutor, SpringRejectedJobsHandler rejectedJobsHandler) {
        super(taskExecutor, rejectedJobsHandler);
    }

    @Override
    public SpringRejectedJobsHandler getRejectedJobsHandler() {
        return (SpringRejectedJobsHandler) super.getRejectedJobsHandler();
    }

    /**
     * Required spring injected {@link RejectedJobsHandler} implementation that will be used when jobs were rejected by the task executor.
     * 
     * @param rejectedJobsHandler
     * @deprecated use {@link this#setRejectedJobsHandler(org.flowable.spring.job.service.SpringRejectedJobsHandler)}
     */
    @Deprecated
    public void setRejectedJobsHandler(SpringRejectedJobsHandler rejectedJobsHandler) {
        super.setRejectedJobsHandler(rejectedJobsHandler);
    }
}
