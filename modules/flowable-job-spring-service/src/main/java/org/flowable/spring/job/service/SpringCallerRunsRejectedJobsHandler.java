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

import org.flowable.job.api.JobInfo;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.asyncexecutor.ExecuteAsyncRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Tijs Rademakers
 */
public class SpringCallerRunsRejectedJobsHandler implements SpringRejectedJobsHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringCallerRunsRejectedJobsHandler.class);

    @Override
    public void jobRejected(AsyncExecutor asyncExecutor, JobInfo job) {
        try {
            // execute rejected work in caller thread (potentially blocking job
            // acquisition)
            new ExecuteAsyncRunnable(job, asyncExecutor.getJobServiceConfiguration(), 
                    asyncExecutor.getJobServiceConfiguration().getJobEntityManager(), null).run();
        } catch (Exception e) {
            LOGGER.error("Failed to execute rejected job {}", job.getId(), e);
        }
    }

}
