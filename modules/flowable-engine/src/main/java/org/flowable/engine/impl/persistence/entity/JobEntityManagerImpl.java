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

package org.flowable.engine.impl.persistence.entity;

import java.util.List;

import org.flowable.engine.common.impl.persistence.entity.data.DataManager;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.JobQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.CountingExecutionEntity;
import org.flowable.engine.impl.persistence.entity.data.JobDataManager;
import org.flowable.engine.runtime.Job;

/**
 * @author Tom Baeyens
 * @author Daniel Meyer
 * @author Joram Barrez
 */
public class JobEntityManagerImpl extends JobInfoEntityManagerImpl<JobEntity> implements JobEntityManager {

    protected JobDataManager jobDataManager;

    public JobEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, JobDataManager jobDataManager) {
        super(processEngineConfiguration, jobDataManager);
        this.jobDataManager = jobDataManager;
    }

    @Override
    protected DataManager<JobEntity> getDataManager() {
        return jobDataManager;
    }

    @Override
    public boolean insertJobEntity(JobEntity timerJobEntity) {
        return doInsert(timerJobEntity, true);
    }

    @Override
    public void insert(JobEntity jobEntity, boolean fireCreateEvent) {
        doInsert(jobEntity, fireCreateEvent);
    }

    protected boolean doInsert(JobEntity jobEntity, boolean fireCreateEvent) {
        // add link to execution
        if (jobEntity.getExecutionId() != null) {
            ExecutionEntity execution = getExecutionEntityManager().findById(jobEntity.getExecutionId());
            if (execution != null) {
                execution.getJobs().add(jobEntity);

                // Inherit tenant if (if applicable)
                if (execution.getTenantId() != null) {
                    jobEntity.setTenantId(execution.getTenantId());
                }

                if (isExecutionRelatedEntityCountEnabled(execution)) {
                    CountingExecutionEntity countingExecutionEntity = (CountingExecutionEntity) execution;
                    countingExecutionEntity.setJobCount(countingExecutionEntity.getJobCount() + 1);
                }

            } else {
                return false;
            }
        }

        jobEntity.setCreateTime(getProcessEngineConfiguration().getClock().getCurrentTime());
        super.insert(jobEntity, fireCreateEvent);
        return true;
    }

    @Override
    public List<Job> findJobsByQueryCriteria(JobQueryImpl jobQuery) {
        return jobDataManager.findJobsByQueryCriteria(jobQuery);
    }

    @Override
    public long findJobCountByQueryCriteria(JobQueryImpl jobQuery) {
        return jobDataManager.findJobCountByQueryCriteria(jobQuery);
    }

    @Override
    public void delete(JobEntity jobEntity) {
        super.delete(jobEntity);

        deleteExceptionByteArrayRef(jobEntity);

        removeExecutionLink(jobEntity);

        // Send event
        if (getEventDispatcher().isEnabled()) {
            getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, this));
        }
    }

    @Override
    public void delete(JobEntity entity, boolean fireDeleteEvent) {
        if (entity.getExecutionId() != null && isExecutionRelatedEntityCountEnabledGlobally()) {
            CountingExecutionEntity executionEntity = (CountingExecutionEntity) getExecutionEntityManager().findById(entity.getExecutionId());
            if (isExecutionRelatedEntityCountEnabled(executionEntity)) {
                executionEntity.setJobCount(executionEntity.getJobCount() - 1);
            }
        }
        super.delete(entity, fireDeleteEvent);
    }

    /**
     * Removes the job's execution's reference to this job, if the job has an associated execution. Subclasses may override to provide custom implementations.
     */
    protected void removeExecutionLink(JobEntity jobEntity) {
        if (jobEntity.getExecutionId() != null) {
            ExecutionEntity execution = getExecutionEntityManager().findById(jobEntity.getExecutionId());
            if (execution != null) {
                execution.getJobs().remove(jobEntity);
            }
        }
    }

    /**
     * Deletes a the byte array used to store the exception information. Subclasses may override to provide custom implementations.
     */
    protected void deleteExceptionByteArrayRef(JobEntity jobEntity) {
        ByteArrayRef exceptionByteArrayRef = jobEntity.getExceptionByteArrayRef();
        if (exceptionByteArrayRef != null) {
            exceptionByteArrayRef.delete();
        }
    }

    public JobDataManager getJobDataManager() {
        return jobDataManager;
    }

    public void setJobDataManager(JobDataManager jobDataManager) {
        this.jobDataManager = jobDataManager;
    }

}
