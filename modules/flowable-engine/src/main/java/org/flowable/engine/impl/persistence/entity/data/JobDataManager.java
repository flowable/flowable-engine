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
package org.flowable.engine.impl.persistence.entity.data;

import java.util.List;

import org.flowable.engine.common.impl.Page;
import org.flowable.engine.common.impl.persistence.entity.data.DataManager;
import org.flowable.engine.impl.JobQueryImpl;
import org.flowable.engine.impl.persistence.entity.JobEntity;
import org.flowable.engine.runtime.Job;

/**
 * @author Joram Barrez
 */
public interface JobDataManager extends DataManager<JobEntity> {

    List<JobEntity> findJobsToExecute(Page page);

    List<JobEntity> findJobsByExecutionId(final String executionId);

    List<JobEntity> findJobsByProcessInstanceId(final String processInstanceId);

    List<JobEntity> findExpiredJobs(Page page);

    List<Job> findJobsByQueryCriteria(JobQueryImpl jobQuery, Page page);

    long findJobCountByQueryCriteria(JobQueryImpl jobQuery);

    void updateJobTenantIdForDeployment(String deploymentId, String newTenantId);

    void resetExpiredJob(String jobId);

}
