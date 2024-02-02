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

package org.flowable.job.service.impl.persistence.entity;

import java.util.List;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayRef;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.event.impl.FlowableJobEventBuilder;
import org.flowable.job.service.impl.DeadLetterJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.data.DeadLetterJobDataManager;

/**
 * @author Tijs Rademakers
 */
public class DeadLetterJobEntityManagerImpl
        extends AbstractJobServiceEngineEntityManager<DeadLetterJobEntity, DeadLetterJobDataManager>
        implements DeadLetterJobEntityManager {

    public DeadLetterJobEntityManagerImpl(JobServiceConfiguration jobServiceConfiguration, DeadLetterJobDataManager jobDataManager) {
        super(jobServiceConfiguration, jobServiceConfiguration.getEngineName(), jobDataManager);
    }

    @Override
    public DeadLetterJobEntity findJobByCorrelationId(String correlationId) {
        return dataManager.findJobByCorrelationId(correlationId);
    }

    @Override
    public List<DeadLetterJobEntity> findJobsByExecutionId(String id) {
        return dataManager.findJobsByExecutionId(id);
    }
    
    @Override
    public List<DeadLetterJobEntity> findJobsByProcessInstanceId(String id) {
        return dataManager.findJobsByProcessInstanceId(id);
    }

    @Override
    public List<Job> findJobsByQueryCriteria(DeadLetterJobQueryImpl jobQuery) {
        return dataManager.findJobsByQueryCriteria(jobQuery);
    }

    @Override
    public long findJobCountByQueryCriteria(DeadLetterJobQueryImpl jobQuery) {
        return dataManager.findJobCountByQueryCriteria(jobQuery);
    }

    @Override
    public void updateJobTenantIdForDeployment(String deploymentId, String newTenantId) {
        dataManager.updateJobTenantIdForDeployment(deploymentId, newTenantId);
    }

    @Override
    public void insert(DeadLetterJobEntity jobEntity, boolean fireCreateEvent) {
        if (getServiceConfiguration().getInternalJobManager() != null) {
            getServiceConfiguration().getInternalJobManager().handleJobInsert(jobEntity);
        }

        jobEntity.setCreateTime(getServiceConfiguration().getClock().getCurrentTime());
        if (jobEntity.getCorrelationId() == null) {
            jobEntity.setCorrelationId(serviceConfiguration.getIdGenerator().getNextId());
        }
        super.insert(jobEntity, fireCreateEvent);
    }

    @Override
    public void insert(DeadLetterJobEntity jobEntity) {
        insert(jobEntity, true);
    }

    @Override
    public void delete(DeadLetterJobEntity jobEntity) {
        super.delete(jobEntity);

        deleteByteArrayRef(jobEntity.getExceptionByteArrayRef());
        deleteByteArrayRef(jobEntity.getCustomValuesByteArrayRef());

        // If the job used to be a history job, the configuration contains the id of the byte array containing the history json
        // (because deadletter jobs don't have an advanced configuration column)
        if (HistoryJobEntity.HISTORY_JOB_TYPE.equals(jobEntity.getJobType()) && jobEntity.getJobHandlerConfiguration() != null) {
            // To avoid duplicating the byteArrayEntityManager lookup, a (fake) ByteArrayRef is created.
            new ByteArrayRef(jobEntity.getJobHandlerConfiguration(), serviceConfiguration.getCommandExecutor()).delete(serviceConfiguration.getEngineName());
        }

        if (getServiceConfiguration().getInternalJobManager() != null) {
            getServiceConfiguration().getInternalJobManager().handleJobDelete(jobEntity);
        }

        // Send event
        if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
            getEventDispatcher().dispatchEvent(FlowableJobEventBuilder.createEntityEvent(
                    FlowableEngineEventType.ENTITY_DELETED, jobEntity), engineType);
        }
    }

    protected DeadLetterJobEntity createDeadLetterJob(AbstractRuntimeJobEntity job) {
        DeadLetterJobEntity newJobEntity = create();
        newJobEntity.setJobHandlerConfiguration(job.getJobHandlerConfiguration());
        newJobEntity.setCustomValues(job.getCustomValues());
        newJobEntity.setJobHandlerType(job.getJobHandlerType());
        newJobEntity.setExclusive(job.isExclusive());
        newJobEntity.setRepeat(job.getRepeat());
        newJobEntity.setRetries(job.getRetries());
        newJobEntity.setEndDate(job.getEndDate());
        newJobEntity.setExecutionId(job.getExecutionId());
        newJobEntity.setProcessInstanceId(job.getProcessInstanceId());
        newJobEntity.setProcessDefinitionId(job.getProcessDefinitionId());
        newJobEntity.setScopeId(job.getScopeId());
        newJobEntity.setSubScopeId(job.getSubScopeId());
        newJobEntity.setScopeType(job.getScopeType());
        newJobEntity.setScopeDefinitionId(job.getScopeDefinitionId());

        // Inherit tenant
        newJobEntity.setTenantId(job.getTenantId());
        newJobEntity.setJobType(job.getJobType());
        return newJobEntity;
    }

}
