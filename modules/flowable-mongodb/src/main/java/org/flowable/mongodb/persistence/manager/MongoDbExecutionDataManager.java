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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.bson.conversions.Bson;
import org.flowable.engine.impl.ExecutionQueryImpl;
import org.flowable.engine.impl.ProcessInstanceQueryImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.ExecutionDataManager;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;

import com.mongodb.client.model.Filters;

/**
 * @author Joram Barrez
 */
public class MongoDbExecutionDataManager extends AbstractMongoDbDataManager implements ExecutionDataManager {
    
    public static final String COLLECTION_EXECUTIONS = "executions";

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

    public void delete(String id) {
        throw new UnsupportedOperationException();
    }

    public void delete(ExecutionEntity entity) {
        throw new UnsupportedOperationException();        
    }

    public ExecutionEntity findSubProcessInstanceBySuperExecutionId(String superExecutionId) {
       throw new UnsupportedOperationException();
    }

    public List<ExecutionEntity> findChildExecutionsByParentExecutionId(String parentExecutionId) {
        Bson filter = Filters.eq("parentExecutionId", parentExecutionId);
        return getMongoDbSession().find(COLLECTION_EXECUTIONS, filter);
    }

    public List<ExecutionEntity> findChildExecutionsByProcessInstanceId(String processInstanceId) {
       throw new UnsupportedOperationException();
    }

    public List<ExecutionEntity> findExecutionsByParentExecutionAndActivityIds(String parentExecutionId,
            Collection<String> activityIds) {
       throw new UnsupportedOperationException();
    }

    public long findExecutionCountByQueryCriteria(ExecutionQueryImpl executionQuery) {
        return 0;
    }

    public List<ExecutionEntity> findExecutionsByQueryCriteria(ExecutionQueryImpl executionQuery) {
       throw new UnsupportedOperationException();
    }

    public long findProcessInstanceCountByQueryCriteria(ProcessInstanceQueryImpl executionQuery) {
        return 0;
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
       throw new UnsupportedOperationException();
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
    
}
