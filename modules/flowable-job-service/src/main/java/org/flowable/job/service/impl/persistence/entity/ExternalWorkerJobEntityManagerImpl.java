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

import org.flowable.job.api.ExternalWorkerJob;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.ExternalWorkerJobAcquireBuilderImpl;
import org.flowable.job.service.impl.ExternalWorkerJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.data.ExternalWorkerJobDataManager;

/**
 * @author Filip Hrisafov
 */
public class ExternalWorkerJobEntityManagerImpl
        extends JobInfoEntityManagerImpl<ExternalWorkerJobEntity, ExternalWorkerJobDataManager>
        implements ExternalWorkerJobEntityManager {

    public ExternalWorkerJobEntityManagerImpl(JobServiceConfiguration jobServiceConfiguration, ExternalWorkerJobDataManager jobDataManager) {
        super(jobServiceConfiguration, jobDataManager);
    }

    @Override
    public boolean insertExternalWorkerJobEntity(ExternalWorkerJobEntity jobEntity) {
        return doInsert(jobEntity, true);
    }

    @Override
    public void insert(ExternalWorkerJobEntity jobEntity, boolean fireCreateEvent) {
        doInsert(jobEntity, fireCreateEvent);
    }

    protected boolean doInsert(ExternalWorkerJobEntity jobEntity, boolean fireCreateEvent) {
        if (serviceConfiguration.getInternalJobManager() != null) {
            boolean handledJob = serviceConfiguration.getInternalJobManager().handleJobInsert(jobEntity);
            if (!handledJob) {
                return false;
            }
        }

        jobEntity.setCreateTime(getClock().getCurrentTime());
        if (jobEntity.getCorrelationId() == null) {
            jobEntity.setCorrelationId(serviceConfiguration.getIdGenerator().getNextId());
        }
        super.insert(jobEntity, fireCreateEvent);
        return true;
    }

    @Override
    public ExternalWorkerJobEntity findJobByCorrelationId(String correlationId) {
        return dataManager.findJobByCorrelationId(correlationId);
    }

    @Override
    public List<ExternalWorkerJobEntity> findJobsByScopeIdAndSubScopeId(String scopeId, String subScopeId) {
        return dataManager.findJobsByScopeIdAndSubScopeId(scopeId, subScopeId);
    }

    @Override
    public List<ExternalWorkerJob> findJobsByQueryCriteria(ExternalWorkerJobQueryImpl jobQuery) {
        return dataManager.findJobsByQueryCriteria(jobQuery);
    }

    @Override
    public long findJobCountByQueryCriteria(ExternalWorkerJobQueryImpl jobQuery) {
        return dataManager.findJobCountByQueryCriteria(jobQuery);
    }

    @Override
    public List<ExternalWorkerJobEntity> findExternalJobsToExecute(ExternalWorkerJobAcquireBuilderImpl builder, int numberOfJobs) {
        return dataManager.findExternalJobsToExecute(builder, numberOfJobs);
    }

    @Override
    public void delete(ExternalWorkerJobEntity entity, boolean fireDeleteEvent) {
        deleteByteArrayRef(entity.getExceptionByteArrayRef());
        deleteByteArrayRef(entity.getCustomValuesByteArrayRef());

        if (serviceConfiguration.getInternalJobManager() != null) {
            serviceConfiguration.getInternalJobManager().handleJobDelete(entity);
        }

        super.delete(entity, fireDeleteEvent);
    }

}
