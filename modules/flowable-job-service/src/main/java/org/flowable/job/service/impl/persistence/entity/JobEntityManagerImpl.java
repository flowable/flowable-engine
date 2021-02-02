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
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.event.impl.FlowableJobEventBuilder;
import org.flowable.job.service.impl.JobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.data.JobDataManager;

/**
 * @author Tom Baeyens
 * @author Daniel Meyer
 * @author Joram Barrez
 */
public class JobEntityManagerImpl
    extends JobInfoEntityManagerImpl<JobEntity, JobDataManager>
    implements JobEntityManager {

    public JobEntityManagerImpl(JobServiceConfiguration jobServiceConfiguration, JobDataManager jobDataManager) {
        super(jobServiceConfiguration, jobDataManager);
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
    public JobEntity findJobByCorrelationId(String correlationId) {
        return dataManager.findJobByCorrelationId(correlationId);
    }

    @Override
    public List<Job> findJobsByQueryCriteria(JobQueryImpl jobQuery) {
        return dataManager.findJobsByQueryCriteria(jobQuery);
    }

    @Override
    public long findJobCountByQueryCriteria(JobQueryImpl jobQuery) {
        return dataManager.findJobCountByQueryCriteria(jobQuery);
    }

    @Override
    public void delete(JobEntity jobEntity) {
        delete(jobEntity, false);

        deleteByteArrayRef(jobEntity.getExceptionByteArrayRef());
        deleteByteArrayRef(jobEntity.getCustomValuesByteArrayRef());

        // Send event
        FlowableEventDispatcher eventDispatcher = getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableJobEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, jobEntity),
                    serviceConfiguration.getEngineName());
        }
    }

    @Override
    public void delete(JobEntity entity, boolean fireDeleteEvent) {
        if (serviceConfiguration.getInternalJobManager() != null) {
            serviceConfiguration.getInternalJobManager().handleJobDelete(entity);
        }
        
        super.delete(entity, fireDeleteEvent);
    }

}
