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
package org.flowable.job.service.impl.cmd;

import java.util.List;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.JobQueryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnacquireOwnedJobsCmd implements Command<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnacquireOwnedJobsCmd.class);
    
    protected final JobServiceConfiguration jobServiceConfiguration;
    
    protected final String lockOwner;
    protected final String tenantId;

    public UnacquireOwnedJobsCmd(String lockOwner, String tenantId, JobServiceConfiguration jobServiceConfiguration) {
        this.lockOwner = lockOwner;
        this.tenantId = tenantId;
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        JobQueryImpl jobQuery = new JobQueryImpl(commandContext, jobServiceConfiguration);
        jobQuery.lockOwner(lockOwner);

        if (tenantId != null) {
            jobQuery.jobTenantId(tenantId);
        }

        List<Job> jobs = jobServiceConfiguration.getJobEntityManager().findJobsByQueryCriteria(jobQuery);
        for (Job job : jobs) {
            logJobUnlocking(job);
            jobServiceConfiguration.getJobManager().unacquire(job);
        }
        return null;
    }

    protected void logJobUnlocking(Job job) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Unacquiring job {} with owner {} and tenantId {}", job, lockOwner, tenantId);
        }
    }
}
