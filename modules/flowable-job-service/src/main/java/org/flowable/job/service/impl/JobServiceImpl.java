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
package org.flowable.job.service.impl;

import java.util.Collection;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.job.api.JobInfo;
import org.flowable.job.service.JobService;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.event.impl.FlowableJobEventBuilder;
import org.flowable.job.service.impl.persistence.entity.AbstractRuntimeJobEntity;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntity;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntityManager;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntity;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntityManager;

/**
 * @author Tijs Rademakers
 */
public class JobServiceImpl extends ServiceImpl implements JobService {

    public JobServiceImpl(JobServiceConfiguration jobServiceConfiguration) {
        super(jobServiceConfiguration);
    }
    
    @Override
    public void scheduleAsyncJob(JobEntity job) {
        getJobManager().scheduleAsyncJob(job);
    }

    @Override
    public JobEntity findJobById(String jobId) {
        return getJobEntityManager().findById(jobId);
    }

    @Override
    public List<JobEntity> findJobsByExecutionId(String executionId) {
        return getJobEntityManager().findJobsByExecutionId(executionId);
    }
    
    @Override
    public List<SuspendedJobEntity> findSuspendedJobsByExecutionId(String executionId) {
        return getSuspendedJobEntityManager().findJobsByExecutionId(executionId);
    }
    
    @Override
    public List<DeadLetterJobEntity> findDeadLetterJobsByExecutionId(String executionId) {
        return getDeadLetterJobEntityManager().findJobsByExecutionId(executionId);
    }
    
    @Override
    public List<JobEntity> findJobsByProcessInstanceId(String processInstanceId) {
        return getJobEntityManager().findJobsByProcessInstanceId(processInstanceId);
    }

    @Override
    public List<SuspendedJobEntity> findSuspendedJobsByProcessInstanceId(String processInstanceId) {
        return getSuspendedJobEntityManager().findJobsByProcessInstanceId(processInstanceId);
    }
    
    @Override
    public List<DeadLetterJobEntity> findDeadLetterJobsByProcessInstanceId(String processInstanceId) {
        return getDeadLetterJobEntityManager().findJobsByProcessInstanceId(processInstanceId);
    }
    
    @Override
    public void updateAllJobTypesTenantIdForDeployment(String deploymentId, String newTenantId) {
        getJobEntityManager().updateJobTenantIdForDeployment(deploymentId, newTenantId);
        getTimerJobEntityManager().updateJobTenantIdForDeployment(deploymentId, newTenantId);
        getSuspendedJobEntityManager().updateJobTenantIdForDeployment(deploymentId, newTenantId);
        getDeadLetterJobEntityManager().updateJobTenantIdForDeployment(deploymentId, newTenantId);
    }
    
    @Override
    public AbstractRuntimeJobEntity activateSuspendedJob(SuspendedJobEntity job) {
        if (configuration.getJobParentStateResolver().isSuspended(job)) {
            throw new FlowableIllegalArgumentException("Can not activate job "+ job.getId() +". Parent is suspended.");
        }
        return getJobManager().activateSuspendedJob(job);
    }

    @Override
    public SuspendedJobEntity moveJobToSuspendedJob(AbstractRuntimeJobEntity job) {
        return getJobManager().moveJobToSuspendedJob(job);
    }

    @Override
    public AbstractRuntimeJobEntity moveJobToDeadLetterJob(AbstractRuntimeJobEntity job) {
        return getJobManager().moveJobToDeadLetterJob(job);
    }

    @Override
    public void unacquireWithDecrementRetries(JobInfo job) {
        getJobManager().unacquireWithDecrementRetries(job);
    }
    
    @Override
    public JobEntity createJob() {
        return getJobEntityManager().create();
    }
    
    @Override
    public void createAsyncJob(JobEntity job, boolean isExclusive) {
        getJobManager().createAsyncJob(job, isExclusive);
    }

    @Override
    public void insertJob(JobEntity job) {
        getJobEntityManager().insert(job);
    }

    @Override
    public DeadLetterJobEntity createDeadLetterJob() {
        return getDeadLetterJobEntityManager().create();
    }

    @Override
    public void insertDeadLetterJob(DeadLetterJobEntity deadLetterJob) {
        getDeadLetterJobEntityManager().insert(deadLetterJob);
    }

    @Override
    public void updateJob(JobEntity job) {
        getJobEntityManager().update(job);
    }

    @Override
    public void deleteJob(String jobId) {
        getJobEntityManager().delete(jobId);
    }

    @Override
    public void deleteJob(JobEntity job) {
        getJobEntityManager().delete(job);
    }

    @Override
    public void deleteJobsByExecutionId(String executionId) {
        JobEntityManager jobEntityManager = getJobEntityManager();
        Collection<JobEntity> jobsForExecution = jobEntityManager.findJobsByExecutionId(executionId);
        for (JobEntity job : jobsForExecution) {
            getJobEntityManager().delete(job);
            if (getEventDispatcher().isEnabled()) {
                getEventDispatcher().dispatchEvent(FlowableJobEventBuilder.createEntityEvent(FlowableEngineEventType.JOB_CANCELED, job));
            }
        }
    }
    
    @Override
    public void deleteSuspendedJobsByExecutionId(String executionId) {
        SuspendedJobEntityManager suspendedJobEntityManager = getSuspendedJobEntityManager();
        Collection<SuspendedJobEntity> suspendedJobsForExecution = suspendedJobEntityManager.findJobsByExecutionId(executionId);
        for (SuspendedJobEntity job : suspendedJobsForExecution) {
            suspendedJobEntityManager.delete(job);
            if (getEventDispatcher().isEnabled()) {
                getEventDispatcher().dispatchEvent(FlowableJobEventBuilder.createEntityEvent(FlowableEngineEventType.JOB_CANCELED, job));
            }
        }
    }
    
    @Override
    public void deleteDeadLetterJobsByExecutionId(String executionId) {
        DeadLetterJobEntityManager deadLetterJobEntityManager = getDeadLetterJobEntityManager();
        Collection<DeadLetterJobEntity> deadLetterJobsForExecution = deadLetterJobEntityManager.findJobsByExecutionId(executionId);
        for (DeadLetterJobEntity job : deadLetterJobsForExecution) {
            deadLetterJobEntityManager.delete(job);
            if (getEventDispatcher().isEnabled()) {
                getEventDispatcher().dispatchEvent(FlowableJobEventBuilder.createEntityEvent(FlowableEngineEventType.JOB_CANCELED, job));
            }
        }
    }
    
}
