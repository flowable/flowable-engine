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
package org.flowable.mongodb.persistence.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bson.conversions.Bson;
import org.flowable.common.engine.impl.Page;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.TimerJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.data.TimerJobDataManager;

import com.mongodb.client.model.Filters;

/**
 * @author Joram Barrez
 */
public class MongoDbTimerJobDataManager extends AbstractMongoDbDataManager implements TimerJobDataManager {
    
    public static final String COLLECTION_TIMER_JOBS = "timerJobs";

    @Override
    public TimerJobEntity create() {
        return new TimerJobEntityImpl();
    }

    @Override
    public TimerJobEntity findById(String jobId) {
        return getMongoDbSession().findOne(COLLECTION_TIMER_JOBS, jobId);
    }

    @Override
    public void insert(TimerJobEntity entity) {
        getMongoDbSession().insertOne(entity);
    }

    @Override
    public TimerJobEntity update(TimerJobEntity entity) {
        return null;
    }

    @Override
    public void delete(String id) {
        TimerJobEntity jobEntity = findById(id);
        delete(jobEntity);
    }

    @Override
    public void delete(TimerJobEntity jobEntity) {
        getMongoDbSession().delete(COLLECTION_TIMER_JOBS, jobEntity);
    }

    @Override
    public List<TimerJobEntity> findTimerJobsToExecute(Page page) {
        return Collections.emptyList();
    }

    @Override
    public List<TimerJobEntity> findJobsByTypeAndProcessDefinitionId(String jobHandlerType, String processDefinitionId) {
        return Collections.emptyList();
    }

    @Override
    public List<TimerJobEntity> findJobsByTypeAndProcessDefinitionKeyNoTenantId(String jobHandlerType, String processDefinitionKey) {
        return Collections.emptyList();
    }

    @Override
    public List<TimerJobEntity> findJobsByTypeAndProcessDefinitionKeyAndTenantId(String jobHandlerType, String processDefinitionKey, String tenantId) {
        return Collections.emptyList();
    }

    @Override
    public List<TimerJobEntity> findJobsByExecutionId(String executionId) {
        Bson filter = Filters.eq("executionId", executionId);
        return getMongoDbSession().find(COLLECTION_TIMER_JOBS, filter);
    }

    @Override
    public List<TimerJobEntity> findJobsByProcessInstanceId(String processInstanceId) {
        Bson filter = Filters.eq("processInstanceId", processInstanceId);
        return getMongoDbSession().find(COLLECTION_TIMER_JOBS, filter);
    }

    @Override
    public List<Job> findJobsByQueryCriteria(TimerJobQueryImpl jobQuery) {
        List<Bson> andFilters = new ArrayList<>();
        if (jobQuery.getExecutionId() != null) {
            andFilters.add(Filters.eq("executionId", jobQuery.getExecutionId()));
        }
        
        if (jobQuery.getProcessInstanceId() != null) {
            andFilters.add(Filters.eq("processInstanceId", jobQuery.getProcessInstanceId()));
        }
        
        Bson filter = null;
        if (andFilters.size() > 0) {
            filter = Filters.and(andFilters.toArray(new Bson[andFilters.size()]));
        }
        
        return getMongoDbSession().find(COLLECTION_TIMER_JOBS, filter);
    }

    @Override
    public long findJobCountByQueryCriteria(TimerJobQueryImpl jobQuery) {
        List<Bson> andFilters = new ArrayList<>();
        if (jobQuery.getExecutionId() != null) {
            andFilters.add(Filters.eq("executionId", jobQuery.getExecutionId()));
        }
        
        if (jobQuery.getProcessInstanceId() != null) {
            andFilters.add(Filters.eq("processInstanceId", jobQuery.getProcessInstanceId()));
        }
        
        Bson filter = null;
        if (andFilters.size() > 0) {
            filter = Filters.and(andFilters.toArray(new Bson[andFilters.size()]));
        }
        
        return getMongoDbSession().count(COLLECTION_TIMER_JOBS, filter);
    }

    @Override
    public void updateJobTenantIdForDeployment(String deploymentId, String newTenantId) {
        
    }

}
