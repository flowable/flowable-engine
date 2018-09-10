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
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.TimerJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.data.TimerJobDataManager;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;

/**
 * @author Joram Barrez
 */
public class MongoDbTimerJobDataManager extends AbstractMongoDbDataManager<TimerJobEntity> implements TimerJobDataManager {
    
    public static final String COLLECTION_TIMER_JOBS = "timerJobs";
    
    protected JobServiceConfiguration jobServiceConfiguration;
    
    public MongoDbTimerJobDataManager(JobServiceConfiguration jobServiceConfiguration) {
        this.jobServiceConfiguration = jobServiceConfiguration;
    }
    
    @Override
    public String getCollection() {
        return COLLECTION_TIMER_JOBS;
    }

    @Override
    public TimerJobEntity create() {
        return new TimerJobEntityImpl();
    }

    @Override
    public BasicDBObject createUpdateObject(Entity entity) {
        TimerJobEntity jobEntity = (TimerJobEntity) entity;
        BasicDBObject updateObject = null;
        updateObject = setUpdateProperty(jobEntity, "retries", jobEntity.getRetries(), updateObject);
        updateObject = setUpdateProperty(jobEntity, "exceptionMessage", jobEntity.getExceptionMessage(), updateObject);
        updateObject = setUpdateProperty(jobEntity, "lockOwner", jobEntity.getLockOwner(), updateObject);
        updateObject = setUpdateProperty(jobEntity, "lockExpirationTime", jobEntity.getLockExpirationTime(), updateObject);
        updateObject = setUpdateProperty(jobEntity, "dueDate", jobEntity.getDuedate(), updateObject);
        return updateObject;
    }

    @Override
    public List<TimerJobEntity> findTimerJobsToExecute(Page page) {
        Bson filter = null;
        List<Bson> filterParts = new ArrayList<>();
        if (jobServiceConfiguration.getJobExecutionScope() == null) {
            filterParts.add(Filters.eq("scopeType", null));
            
        } else if (!jobServiceConfiguration.getJobExecutionScope().equals("all")){
            filterParts.add(Filters.eq("scopeType", jobServiceConfiguration.getJobExecutionScope()));  
        }
        
        filterParts.add(Filters.lte("duedate", jobServiceConfiguration.getClock().getCurrentTime()));
        filterParts.add(Filters.eq("lockOwner", null));
        
        filter = Filters.and(filterParts);
        
        return getMongoDbSession().find(COLLECTION_TIMER_JOBS, filter, null, 1);
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
