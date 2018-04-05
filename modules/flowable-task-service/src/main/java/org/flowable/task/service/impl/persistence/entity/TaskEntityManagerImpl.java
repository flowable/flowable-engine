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

package org.flowable.task.service.impl.persistence.entity;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.common.impl.persistence.entity.data.DataManager;
import org.flowable.task.api.Task;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.TaskQueryImpl;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.flowable.task.service.impl.persistence.entity.data.TaskDataManager;
import org.flowable.task.service.impl.util.CountingTaskUtil;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class TaskEntityManagerImpl extends AbstractEntityManager<TaskEntity> implements TaskEntityManager {

    protected TaskDataManager taskDataManager;

    public TaskEntityManagerImpl(TaskServiceConfiguration taskServiceConfiguration, TaskDataManager taskDataManager) {
        super(taskServiceConfiguration);
        this.taskDataManager = taskDataManager;
    }

    @Override
    protected DataManager<TaskEntity> getDataManager() {
        return taskDataManager;
    }

    @Override
    public TaskEntity create() {
        TaskEntity taskEntity = super.create();
        taskEntity.setCreateTime(getClock().getCurrentTime());
        if (CountingTaskUtil.isTaskRelatedEntityCountEnabledGlobally()) {
            ((CountingTaskEntity) taskEntity).setCountEnabled(true);
        }
        return taskEntity;
    }

    @Override
    public void changeTaskAssignee(TaskEntity taskEntity, String assignee) {
        if ((taskEntity.getAssignee() != null && !taskEntity.getAssignee().equals(assignee))
                || (taskEntity.getAssignee() == null && assignee != null)) {
            
            taskEntity.setAssignee(assignee);
            
            if (taskEntity.getId() != null) {
                getTaskServiceConfiguration().getInternalHistoryTaskManager().recordTaskInfoChange(taskEntity);
                update(taskEntity);
            }
        }
    }

    @Override
    public void changeTaskOwner(TaskEntity taskEntity, String owner) {
        if ((taskEntity.getOwner() != null && !taskEntity.getOwner().equals(owner))
                || (taskEntity.getOwner() == null && owner != null)) {
            
            taskEntity.setOwner(owner);

            if (taskEntity.getId() != null) {
                getTaskServiceConfiguration().getInternalHistoryTaskManager().recordTaskInfoChange(taskEntity);
                update(taskEntity);
            }
        }
    }

    @Override
    public List<TaskEntity> findTasksByExecutionId(String executionId) {
        return taskDataManager.findTasksByExecutionId(executionId);
    }

    @Override
    public List<TaskEntity> findTasksByProcessInstanceId(String processInstanceId) {
        return taskDataManager.findTasksByProcessInstanceId(processInstanceId);
    }
    
    @Override
    public List<TaskEntity> findTasksByScopeIdAndScopeType(String scopeId, String scopeType) {
        return taskDataManager.findTasksByScopeIdAndScopeType(scopeId, scopeType);
    }
    
    @Override
    public List<TaskEntity> findTasksBySubScopeIdAndScopeType(String subScopeId, String scopeType) {
        return taskDataManager.findTasksBySubScopeIdAndScopeType(subScopeId, scopeType);
    }

    @Override
    public List<Task> findTasksByQueryCriteria(TaskQueryImpl taskQuery) {
        return taskDataManager.findTasksByQueryCriteria(taskQuery);
    }

    @Override
    public List<Task> findTasksWithRelatedEntitiesByQueryCriteria(TaskQueryImpl taskQuery) {
        return taskDataManager.findTasksWithRelatedEntitiesByQueryCriteria(taskQuery);
    }

    @Override
    public long findTaskCountByQueryCriteria(TaskQueryImpl taskQuery) {
        return taskDataManager.findTaskCountByQueryCriteria(taskQuery);
    }

    @Override
    public List<Task> findTasksByNativeQuery(Map<String, Object> parameterMap) {
        return taskDataManager.findTasksByNativeQuery(parameterMap);
    }

    @Override
    public long findTaskCountByNativeQuery(Map<String, Object> parameterMap) {
        return taskDataManager.findTaskCountByNativeQuery(parameterMap);
    }

    @Override
    public List<Task> findTasksByParentTaskId(String parentTaskId) {
        return taskDataManager.findTasksByParentTaskId(parentTaskId);
    }

    @Override
    public void updateTaskTenantIdForDeployment(String deploymentId, String newTenantId) {
        taskDataManager.updateTaskTenantIdForDeployment(deploymentId, newTenantId);
    }
    
    @Override
    public void updateAllTaskRelatedEntityCountFlags(boolean configProperty) {
        taskDataManager.updateAllTaskRelatedEntityCountFlags(configProperty);
    }
    
    @Override
    public void deleteTasksByExecutionId(String executionId) {
        taskDataManager.deleteTasksByExecutionId(executionId);
    }

    @Override
    public void insert(TaskEntity taskEntity, boolean fireCreateEvent) {
        if (StringUtils.isEmpty(taskEntity.getId())) {
            taskEntity.setId(this.taskServiceConfiguration.getIdGenerator().getNextId());
        }
        super.insert(taskEntity, fireCreateEvent);
    }

    public TaskDataManager getTaskDataManager() {
        return taskDataManager;
    }

    public void setTaskDataManager(TaskDataManager taskDataManager) {
        this.taskDataManager = taskDataManager;
    }

}
