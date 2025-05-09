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
package org.flowable.task.service.impl.persistence.entity.data.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.task.api.Task;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.TaskQueryImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.flowable.task.service.impl.persistence.entity.data.TaskDataManager;
import org.flowable.task.service.impl.persistence.entity.data.impl.cachematcher.TasksByExecutionIdMatcher;
import org.flowable.task.service.impl.persistence.entity.data.impl.cachematcher.TasksByProcessInstanceIdMatcher;
import org.flowable.task.service.impl.persistence.entity.data.impl.cachematcher.TasksByScopeIdAndScopeTypeMatcher;
import org.flowable.task.service.impl.persistence.entity.data.impl.cachematcher.TasksBySubScopeIdAndScopeTypeMatcher;

/**
 * @author Joram Barrez
 */
public class MybatisTaskDataManager extends AbstractDataManager<TaskEntity> implements TaskDataManager {

    protected CachedEntityMatcher<TaskEntity> tasksByExecutionIdMatcher = new TasksByExecutionIdMatcher();
    
    protected CachedEntityMatcher<TaskEntity> tasksByProcessInstanceIdMatcher = new TasksByProcessInstanceIdMatcher();

    protected CachedEntityMatcher<TaskEntity> tasksBySubScopeIdAndScopeTypeMatcher = new TasksBySubScopeIdAndScopeTypeMatcher();
    
    protected CachedEntityMatcher<TaskEntity> tasksByScopeIdAndScopeTypeMatcher = new TasksByScopeIdAndScopeTypeMatcher();
    
    protected TaskServiceConfiguration taskServiceConfiguration;
    
    public MybatisTaskDataManager(TaskServiceConfiguration taskServiceConfiguration) {
        this.taskServiceConfiguration = taskServiceConfiguration;
    }

    @Override
    public Class<? extends TaskEntity> getManagedEntityClass() {
        return TaskEntityImpl.class;
    }

    @Override
    public TaskEntity create() {
        return new TaskEntityImpl();
    }

    @Override
    public List<TaskEntity> findTasksByExecutionId(final String executionId) {
        DbSqlSession dbSqlSession = getDbSqlSession();
        
        // If the process instance has been inserted in the same command execution as this query, there can't be any in the database
        if (isEntityInserted(dbSqlSession, "execution", executionId)) {
            return getListFromCache(tasksByExecutionIdMatcher, executionId);
        }
        
        return getList(dbSqlSession, "selectTasksByExecutionId", executionId, tasksByExecutionIdMatcher, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TaskEntity> findTasksByProcessInstanceId(String processInstanceId) {
        DbSqlSession dbSqlSession = getDbSqlSession();

        // If the process instance has been inserted in the same command execution as this query, there can't be any in the database
        if (isEntityInserted(dbSqlSession, "execution", processInstanceId)) {
            return getListFromCache(tasksByProcessInstanceIdMatcher, processInstanceId);
        }

        return getList(dbSqlSession, "selectTasksByProcessInstanceId", processInstanceId, tasksByProcessInstanceIdMatcher, true);
    }
    
    @Override
    public List<TaskEntity> findTasksByScopeIdAndScopeType(String scopeId, String scopeType) {
        Map<String, String> params = new HashMap<>();
        params.put("scopeId", scopeId);
        params.put("scopeType", scopeType);
        return getList("selectTasksByScopeIdAndScopeType", params, tasksByScopeIdAndScopeTypeMatcher, true);
    }
    
    @Override
    public List<TaskEntity> findTasksBySubScopeIdAndScopeType(String subScopeId, String scopeType) {
        Map<String, String> params = new HashMap<>();
        params.put("subScopeId", subScopeId);
        params.put("scopeType", scopeType);
        return getList("selectTasksBySubScopeIdAndScopeType", params, tasksBySubScopeIdAndScopeTypeMatcher, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Task> findTasksByQueryCriteria(TaskQueryImpl taskQuery) {
        final String query = "selectTaskByQueryCriteria";
        setSafeInValueLists(taskQuery);
        return getDbSqlSession().selectList(query, taskQuery, getManagedEntityClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Task> findTasksWithRelatedEntitiesByQueryCriteria(TaskQueryImpl taskQuery) {
        final String query = "selectTasksWithRelatedEntitiesByQueryCriteria";
        setSafeInValueLists(taskQuery);
        return getDbSqlSession().selectList(query, taskQuery, getManagedEntityClass());
    }

    @Override
    public long findTaskCountByQueryCriteria(TaskQueryImpl taskQuery) {
        setSafeInValueLists(taskQuery);
        return (Long) getDbSqlSession().selectOne("selectTaskCountByQueryCriteria", taskQuery);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Task> findTasksByNativeQuery(Map<String, Object> parameterMap) {
        return getDbSqlSession().selectListWithRawParameter("selectTaskByNativeQuery", parameterMap);
    }

    @Override
    public long findTaskCountByNativeQuery(Map<String, Object> parameterMap) {
        return (Long) getDbSqlSession().selectOne("selectTaskCountByNativeQuery", parameterMap);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Task> findTasksByParentTaskId(String parentTaskId) {
        return getDbSqlSession().selectList("selectTasksByParentTaskId", parentTaskId);
    }

    @Override
    public void updateTaskTenantIdForDeployment(String deploymentId, String newTenantId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("deploymentId", deploymentId);
        params.put("tenantId", newTenantId);
        getDbSqlSession().directUpdate("updateTaskTenantIdForDeployment", params);
    }

    @Override
    public void updateAllTaskRelatedEntityCountFlags(boolean newValue) {
        getDbSqlSession().directUpdate("updateTaskRelatedEntityCountEnabled", newValue);
    }
    
    @Override
    public void deleteTasksByExecutionId(String executionId) {
        DbSqlSession dbSqlSession = getDbSqlSession();
        if (isEntityInserted(dbSqlSession, "execution", executionId)) {
            deleteCachedEntities(dbSqlSession, tasksByExecutionIdMatcher, executionId);
        } else {
            bulkDelete("deleteTasksByExecutionId", tasksByExecutionIdMatcher, executionId);
        }
    }

    @Override
    protected IdGenerator getIdGenerator() {
        return taskServiceConfiguration.getIdGenerator();
    }
    
    protected void setSafeInValueLists(TaskQueryImpl taskQuery) {
        if (taskQuery.getCandidateGroups() != null) {
            taskQuery.setSafeCandidateGroups(createSafeInValuesList(taskQuery.getCandidateGroups()));
        }
        
        if (taskQuery.getInvolvedGroups() != null) {
            taskQuery.setSafeInvolvedGroups(createSafeInValuesList(taskQuery.getInvolvedGroups()));
        }

        if (taskQuery.getScopeIds() != null) {
            taskQuery.setSafeScopeIds(createSafeInValuesList(taskQuery.getScopeIds()));
        }
        
        if (taskQuery.getOrQueryObjects() != null && !taskQuery.getOrQueryObjects().isEmpty()) {
            for (TaskQueryImpl orTaskQuery : taskQuery.getOrQueryObjects()) {
                setSafeInValueLists(orTaskQuery);
            }
        }
    }
}
