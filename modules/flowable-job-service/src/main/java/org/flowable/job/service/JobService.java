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
package org.flowable.job.service;

import java.util.List;

import org.flowable.job.api.DeadLetterJobQuery;
import org.flowable.job.api.HistoryJobQuery;
import org.flowable.job.api.JobInfo;
import org.flowable.job.api.JobQuery;
import org.flowable.job.api.SuspendedJobQuery;
import org.flowable.job.api.TimerJobQuery;
import org.flowable.job.service.impl.persistence.entity.AbstractRuntimeJobEntity;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntity;

/**
 * Service which provides access to jobs.
 * 
 * @author Tijs Rademakers
 */
public interface JobService {
    
    void scheduleAsyncJob(JobEntity job);
    
    JobQuery createJobQuery();

    TimerJobQuery createTimerJobQuery();

    SuspendedJobQuery createSuspendedJobQuery();

    DeadLetterJobQuery createDeadLetterJobQuery();
    
    HistoryJobQuery createHistoryJobQuery();
    
    JobEntity findJobById(String jobId);
    
    List<JobEntity> findJobsByExecutionId(String executionId);
    
    List<SuspendedJobEntity> findSuspendedJobsByExecutionId(String executionId);
    
    List<DeadLetterJobEntity> findDeadLetterJobsByExecutionId(String executionId);
    
    List<JobEntity> findJobsByProcessInstanceId(String processInstanceId);
    
    List<SuspendedJobEntity> findSuspendedJobsByProcessInstanceId(String processInstanceId);
    
    List<DeadLetterJobEntity> findDeadLetterJobsByProcessInstanceId(String processInstanceId);
    
    AbstractRuntimeJobEntity activateSuspendedJob(SuspendedJobEntity job);
    
    SuspendedJobEntity moveJobToSuspendedJob(AbstractRuntimeJobEntity job);
    
    AbstractRuntimeJobEntity moveJobToDeadLetterJob(AbstractRuntimeJobEntity job);
    
    void updateAllJobTypesTenantIdForDeployment(String deploymentId, String newTenantId);
    
    void unacquireWithDecrementRetries(JobInfo job);
    
    void createAsyncJob(JobEntity job, boolean isExclusive);
    
    JobEntity createJob();
    
    void insertJob(JobEntity job);
    
    DeadLetterJobEntity createDeadLetterJob();
    
    void insertDeadLetterJob(DeadLetterJobEntity deadLetterJob);
    
    void updateJob(JobEntity job);
    
    void deleteJob(String jobId);
    
    void deleteJob(JobEntity job);
    
    void deleteJobsByExecutionId(String executionId);
    
    void deleteSuspendedJobsByExecutionId(String executionId);
    
    void deleteDeadLetterJobsByExecutionId(String executionId);
}
