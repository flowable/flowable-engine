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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.engine.impl.ExecutionQueryImpl;
import org.flowable.engine.impl.ProcessInstanceQueryImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.ExecutionDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.ExecutionsWithSameRootProcessInstanceIdMatcher;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.InactiveExecutionsInActivityAndProcInstMatcher;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;

/**
 * @author Joram Barrez
 */
public class MongoDbExecutionDataManager extends AbstractMongoDbDataManager implements ExecutionDataManager {
    
    public static final String COLLECTION_EXECUTIONS = "executions";
    
    @SuppressWarnings("unchecked")
    protected CachedEntityMatcher<Entity> executionsWithSameRootProcessInstanceIdMatcher = (CachedEntityMatcher) new ExecutionsWithSameRootProcessInstanceIdMatcher();
    
    protected CachedEntityMatcher<Entity> inactiveExecutionsInActivityAndProcInstMatcher = (CachedEntityMatcher) new InactiveExecutionsInActivityAndProcInstMatcher();

    public ExecutionEntity create() {
       return new ExecutionEntityImpl();
    }

    public ExecutionEntity findById(String executionId) {
       return getMongoDbSession().findOne(COLLECTION_EXECUTIONS, executionId);
    }

    public void insert(ExecutionEntity executionEntity) {
        getMongoDbSession().insertOne(executionEntity);
    }

    public ExecutionEntity update(ExecutionEntity entity) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void updateEntity(Entity entity) {
        ExecutionEntity executionEntity = (ExecutionEntity) entity;
        Map<String, Object> persistentState = (Map<String, Object>) entity.getOriginalPersistentState();
        BasicDBObject updateObject = null;
        updateObject = setBooleanUpdateProperty("isActive", executionEntity.isActive(), persistentState, updateObject);
        updateObject = setBooleanUpdateProperty("isScope", executionEntity.isScope(), persistentState, updateObject);
        updateObject = setStringUpdateProperty("activityId", executionEntity.getActivityId(), persistentState, updateObject);
        
        if (updateObject != null) {
            getMongoDbSession().performUpdate(COLLECTION_EXECUTIONS, entity, new Document().append("$set", updateObject));
        }
    }

    public void delete(String id) {
        throw new UnsupportedOperationException();
    }

    public void delete(ExecutionEntity executionEntity) {
        getMongoDbSession().delete(COLLECTION_EXECUTIONS, executionEntity);    
    }

    public ExecutionEntity findSubProcessInstanceBySuperExecutionId(String superExecutionId) {
       List<ExecutionEntity> executionEntities = getMongoDbSession().find(COLLECTION_EXECUTIONS, Filters.eq("superExecution", superExecutionId));
       if (executionEntities.size() > 1) {
           throw new FlowableException("Programmatics error: multiple super executions found");
       } 
       if (!executionEntities.isEmpty()) {
           executionEntities.get(0);
       }
       return null;
    }

    public List<ExecutionEntity> findChildExecutionsByParentExecutionId(String parentExecutionId) {
        Bson filter = Filters.eq("parentExecutionId", parentExecutionId);
        return getMongoDbSession().find(COLLECTION_EXECUTIONS, filter);
    }

    public List<ExecutionEntity> findChildExecutionsByProcessInstanceId(String processInstanceId) {
       return getMongoDbSession().find(COLLECTION_EXECUTIONS, Filters.eq("processInstanceId", processInstanceId));
    }

    public List<ExecutionEntity> findExecutionsByParentExecutionAndActivityIds(String parentExecutionId,
            Collection<String> activityIds) {
       throw new UnsupportedOperationException();
    }

    public long findExecutionCountByQueryCriteria(ExecutionQueryImpl executionQuery) {
        return 0;
    }

    public List<ExecutionEntity> findExecutionsByQueryCriteria(ExecutionQueryImpl executionQuery) {
        List<Bson> andFilters = new ArrayList<>();
        if (executionQuery.getProcessInstanceId() != null) {
            andFilters.add(Filters.eq("processInstanceId", executionQuery.getProcessInstanceId()));
        }
        
        Bson filter = null;
        if (andFilters.size() > 0) {
            filter = Filters.and(andFilters.toArray(new Bson[andFilters.size()]));
        }
        
        return getMongoDbSession().find(COLLECTION_EXECUTIONS, filter);
    }

    public long findProcessInstanceCountByQueryCriteria(ProcessInstanceQueryImpl executionQuery) {
        return getMongoDbSession().count(COLLECTION_EXECUTIONS, Filters.eq("parentId", null));
    }

    public List<ProcessInstance> findProcessInstanceByQueryCriteria(ProcessInstanceQueryImpl executionQuery) {
       throw new UnsupportedOperationException();
    }

    public List<ExecutionEntity> findExecutionsByRootProcessInstanceId(String rootProcessInstanceId) {
       throw new UnsupportedOperationException();
    }

    public List<ExecutionEntity> findExecutionsByProcessInstanceId(String processInstanceId) {
       throw new UnsupportedOperationException();
    }

    public List<ProcessInstance> findProcessInstanceAndVariablesByQueryCriteria(ProcessInstanceQueryImpl executionQuery) {
       throw new UnsupportedOperationException();
    }

    public Collection<ExecutionEntity> findInactiveExecutionsByProcessInstanceId(String processInstanceId) {
       throw new UnsupportedOperationException();
    }

    public Collection<ExecutionEntity> findInactiveExecutionsByActivityIdAndProcessInstanceId(String activityId, String processInstanceId) {
        HashMap<String, Object> params = new HashMap<>(3);
        params.put("activityId", activityId);
        params.put("processInstanceId", processInstanceId);
        params.put("isActive", false);
        
        if (isExecutionTreeFetched(processInstanceId)) {
            return getMongoDbSession().findFromCache(inactiveExecutionsInActivityAndProcInstMatcher, params, ExecutionEntityImpl.class);
        } else {
            List<ExecutionEntity> executions = getMongoDbSession().find(COLLECTION_EXECUTIONS, Filters.and(Filters.eq("activityId", activityId), 
                    Filters.eq("processInstanceId", processInstanceId), Filters.eq("isActive", false)), params, ExecutionEntityImpl.class,
                    inactiveExecutionsInActivityAndProcInstMatcher, true);
            
            return executions;
        }
    }

    public List<String> findProcessInstanceIdsByProcessDefinitionId(String processDefinitionId) {
       throw new UnsupportedOperationException();
    }

    public List<Execution> findExecutionsByNativeQuery(Map<String, Object> parameterMap) {
       throw new UnsupportedOperationException();
    }

    public List<ProcessInstance> findProcessInstanceByNativeQuery(Map<String, Object> parameterMap) {
       throw new UnsupportedOperationException();
    }

    public long findExecutionCountByNativeQuery(Map<String, Object> parameterMap) {
        return 0;
    }

    public void updateExecutionTenantIdForDeployment(String deploymentId, String newTenantId) {
        throw new UnsupportedOperationException();
    }

    public void updateProcessInstanceLockTime(String processInstanceId, Date lockDate, Date expirationTime) {
        throw new UnsupportedOperationException();        
    }

    public void updateAllExecutionRelatedEntityCountFlags(boolean newValue) {
        throw new UnsupportedOperationException();        
    }

    public void clearProcessInstanceLockTime(String processInstanceId) {
        throw new UnsupportedOperationException();        
    }
    
    protected boolean isExecutionTreeFetched(final String executionId) {
        
        // Need to get the cache result before doing the findById
        ExecutionEntity cachedExecutionEntity = getMongoDbSession().getEntityCache().findInCache(ExecutionEntityImpl.class, executionId);
        
        // Find execution in db or cache to check process definition setting for execution fetch.
        // If not set, no extra work is done. The execution is in the cache however now as a side-effect of calling this method.
        ExecutionEntity executionEntity = (cachedExecutionEntity != null) ? cachedExecutionEntity : getMongoDbSession().findOne(COLLECTION_EXECUTIONS, executionId);
        if (!ProcessDefinitionUtil.getProcess(executionEntity.getProcessDefinitionId()).isEnableEagerExecutionTreeFetching()) {
            return false;
        }
        
        // If it's in the cache, the execution and its tree have been fetched before. No need to do anything more.
        if (cachedExecutionEntity != null) {
            return true;
        }
        
        // Fetches execution tree. This will store them in the cache and thus avoid extra database calls.
        getMongoDbSession().find(COLLECTION_EXECUTIONS, Filters.eq("rootProcessInstanceId", executionEntity.getRootProcessInstanceId()), 
                executionId, ExecutionEntity.class, executionsWithSameRootProcessInstanceIdMatcher, true);
        
        return true;
    }
    
    protected BasicDBObject setStringUpdateProperty(String propertyName, String value, Map<String, Object> persistentState, BasicDBObject updateObject) {
        if (persistentState.get(propertyName) != null && (String) persistentState.get(propertyName) != value) {
            if (updateObject == null) {
                updateObject = new BasicDBObject();
            }
            updateObject.append(propertyName, value);
        }
        
        return updateObject;
    }
    
    protected BasicDBObject setBooleanUpdateProperty(String propertyName, Boolean value, Map<String, Object> persistentState, BasicDBObject updateObject) {
        if (persistentState.get(propertyName) != null && (Boolean) persistentState.get(propertyName) != value) {
            if (updateObject == null) {
                updateObject = new BasicDBObject();
            }
            updateObject.append(propertyName, value);
        }
        
        return updateObject;
    }
}
