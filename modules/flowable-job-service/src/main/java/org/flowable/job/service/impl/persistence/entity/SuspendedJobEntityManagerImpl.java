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
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.event.impl.FlowableJobEventBuilder;
import org.flowable.job.service.impl.SuspendedJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.data.SuspendedJobDataManager;

/**
 * @author Tijs Rademakers
 */
public class SuspendedJobEntityManagerImpl
    extends AbstractJobServiceEngineEntityManager<SuspendedJobEntity, SuspendedJobDataManager>
    implements SuspendedJobEntityManager {

    public SuspendedJobEntityManagerImpl(JobServiceConfiguration jobServiceConfiguration, SuspendedJobDataManager jobDataManager) {
        super(jobServiceConfiguration, jobServiceConfiguration.getEngineName(), jobDataManager);
    }

    @Override
    public SuspendedJobEntity findJobByCorrelationId(String correlationId) {
        return dataManager.findJobByCorrelationId(correlationId);
    }

    @Override
    public List<SuspendedJobEntity> findJobsByExecutionId(String id) {
        return dataManager.findJobsByExecutionId(id);
    }

    @Override
    public List<SuspendedJobEntity> findJobsByProcessInstanceId(String id) {
        return dataManager.findJobsByProcessInstanceId(id);
    }

    @Override
    public List<Job> findJobsByQueryCriteria(SuspendedJobQueryImpl jobQuery) {
        return dataManager.findJobsByQueryCriteria(jobQuery);
    }

    @Override
    public long findJobCountByQueryCriteria(SuspendedJobQueryImpl jobQuery) {
        return dataManager.findJobCountByQueryCriteria(jobQuery);
    }

    @Override
    public void updateJobTenantIdForDeployment(String deploymentId, String newTenantId) {
        dataManager.updateJobTenantIdForDeployment(deploymentId, newTenantId);
    }

    @Override
    public void insert(SuspendedJobEntity jobEntity, boolean fireCreateEvent) {
        if (serviceConfiguration.getInternalJobManager() != null) {
            serviceConfiguration.getInternalJobManager().handleJobInsert(jobEntity);
        }

        jobEntity.setCreateTime(getClock().getCurrentTime());
        if (jobEntity.getCorrelationId() == null) {
            jobEntity.setCorrelationId(serviceConfiguration.getIdGenerator().getNextId());
        }
        super.insert(jobEntity, fireCreateEvent);
    }

    @Override
    public void insert(SuspendedJobEntity jobEntity) {
        insert(jobEntity, true);
    }

    @Override
    public void delete(SuspendedJobEntity jobEntity) {
        delete(jobEntity, false);

        deleteByteArrayRef(jobEntity.getExceptionByteArrayRef());
        deleteByteArrayRef(jobEntity.getCustomValuesByteArrayRef());

        // Send event
        if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
            getEventDispatcher().dispatchEvent(FlowableJobEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, jobEntity),
                    serviceConfiguration.getEngineName());
        }
    }

    @Override
    public void delete(SuspendedJobEntity jobEntity, boolean fireDeleteEvent) {
        if (serviceConfiguration.getInternalJobManager() != null) {
            serviceConfiguration.getInternalJobManager().handleJobDelete(jobEntity);
        }

        super.delete(jobEntity, fireDeleteEvent);
    }

    protected SuspendedJobEntity createSuspendedJob(AbstractRuntimeJobEntity job) {
        SuspendedJobEntity newSuspendedJobEntity = create();
        newSuspendedJobEntity.setJobHandlerConfiguration(job.getJobHandlerConfiguration());
        newSuspendedJobEntity.setCustomValues(job.getCustomValues());
        newSuspendedJobEntity.setJobHandlerType(job.getJobHandlerType());
        newSuspendedJobEntity.setExclusive(job.isExclusive());
        newSuspendedJobEntity.setRepeat(job.getRepeat());
        newSuspendedJobEntity.setRetries(job.getRetries());
        newSuspendedJobEntity.setEndDate(job.getEndDate());
        newSuspendedJobEntity.setExecutionId(job.getExecutionId());
        newSuspendedJobEntity.setProcessInstanceId(job.getProcessInstanceId());
        newSuspendedJobEntity.setProcessDefinitionId(job.getProcessDefinitionId());
        newSuspendedJobEntity.setScopeId(job.getScopeId());
        newSuspendedJobEntity.setSubScopeId(job.getSubScopeId());
        newSuspendedJobEntity.setScopeType(job.getScopeType());
        newSuspendedJobEntity.setScopeDefinitionId(job.getScopeDefinitionId());

        // Inherit tenant
        newSuspendedJobEntity.setTenantId(job.getTenantId());
        newSuspendedJobEntity.setJobType(job.getJobType());
        return newSuspendedJobEntity;
    }

}
