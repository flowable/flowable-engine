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
package org.flowable.engine.impl.persistence.entity.data.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.impl.TaskQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.CachedEntityMatcher;
import org.flowable.engine.impl.persistence.entity.TaskEntity;
import org.flowable.engine.impl.persistence.entity.TaskEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.AbstractDataManager;
import org.flowable.engine.impl.persistence.entity.data.TaskDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.TasksByExecutionIdMatcher;
import org.flowable.engine.task.Task;

/**
 * @author Joram Barrez
 */
public class MybatisTaskDataManager extends AbstractDataManager<TaskEntity> implements TaskDataManager {

    protected CachedEntityMatcher<TaskEntity> tasksByExecutionIdMatcher = new TasksByExecutionIdMatcher();

    public MybatisTaskDataManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
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
        return getList("selectTasksByExecutionId", executionId, tasksByExecutionIdMatcher, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TaskEntity> findTasksByProcessInstanceId(String processInstanceId) {
        return getDbSqlSession().selectList("selectTasksByProcessInstanceId", processInstanceId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Task> findTasksByQueryCriteria(TaskQueryImpl taskQuery) {
        final String query = "selectTaskByQueryCriteria";
        return getDbSqlSession().selectList(query, taskQuery);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Task> findTasksWithRelatedEntitiesByQueryCriteria(TaskQueryImpl taskQuery) {
        final String query = "selectTasksWithRelatedEntitiesByQueryCriteria";
        // paging doesn't work for combining task instances and variables due to
        // an outer join, so doing it in-memory

        int firstResult = taskQuery.getFirstResult();
        int maxResults = taskQuery.getMaxResults();

        // setting max results, limit to 20000 results for performance reasons
        if (taskQuery.getTaskVariablesLimit() != null) {
            taskQuery.setMaxResults(taskQuery.getTaskVariablesLimit());
        } else {
            taskQuery.setMaxResults(getProcessEngineConfiguration().getTaskQueryLimit());
        }
        taskQuery.setFirstResult(0);

        List<Task> instanceList = getDbSqlSession().selectListWithRawParameterNoCacheCheck(query, taskQuery);

        if (instanceList != null && !instanceList.isEmpty()) {
            if (firstResult > 0) {
                if (firstResult <= instanceList.size()) {
                    int toIndex = firstResult + Math.min(maxResults, instanceList.size() - firstResult);
                    return instanceList.subList(firstResult, toIndex);
                } else {
                    return Collections.EMPTY_LIST;
                }
            } else {
                int toIndex = maxResults > 0 ?  Math.min(maxResults, instanceList.size()) : instanceList.size();
                return instanceList.subList(0, toIndex);
            }
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public long findTaskCountByQueryCriteria(TaskQueryImpl taskQuery) {
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
        getDbSqlSession().update("updateTaskTenantIdForDeployment", params);
    }

    @Override
    public void updateAllTaskRelatedEntityCountFlags(boolean newValue) {
        getDbSqlSession().update("updateTaskRelatedEntityCountEnabled", newValue);
    }

}
