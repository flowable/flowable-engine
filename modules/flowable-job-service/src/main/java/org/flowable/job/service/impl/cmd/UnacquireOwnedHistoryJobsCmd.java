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
import org.flowable.job.api.HistoryJob;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.HistoryJobQueryImpl;

public class UnacquireOwnedHistoryJobsCmd implements Command<Void> {

    private final String lockOwner;
    private final String tenantId;
    private final JobServiceConfiguration jobServiceConfiguration;

    public UnacquireOwnedHistoryJobsCmd(String lockOwner, String tenantId, JobServiceConfiguration jobServiceConfiguration) {
        this.lockOwner = lockOwner;
        this.tenantId = tenantId;
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        HistoryJobQueryImpl jobQuery = new HistoryJobQueryImpl(commandContext, jobServiceConfiguration);
        jobQuery.lockOwner(lockOwner);
        jobQuery.jobTenantId(tenantId);
        List<HistoryJob> jobs = jobServiceConfiguration.getHistoryJobEntityManager().findHistoryJobsByQueryCriteria(jobQuery);
        for (HistoryJob job : jobs) {
            jobServiceConfiguration.getJobManager().unacquire(job);
        }
        return null;
    }
}