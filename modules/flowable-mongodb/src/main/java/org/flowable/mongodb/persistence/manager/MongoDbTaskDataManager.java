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

import java.util.List;
import java.util.Map;

import org.bson.conversions.Bson;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.TaskQueryImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.flowable.task.service.impl.persistence.entity.data.TaskDataManager;

import com.mongodb.client.model.Filters;

/**
 * @author Joram Barrez
 */
public class MongoDbTaskDataManager extends AbstractMongoDbDataManager implements TaskDataManager {
    
    public static final String COLLECTION_TASKS = "tasks";

    @Override
    public TaskEntity create() {
        return new TaskEntityImpl();
    }

    @Override
    public TaskEntity findById(String taskId) {
        return getMongoDbSession().findOne(COLLECTION_TASKS, taskId);
    }

    @Override
    public void insert(TaskEntity taskEntity) {
        getMongoDbSession().insertOne(taskEntity);
    }

    @Override
    public TaskEntity update(TaskEntity entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String id) {
        TaskEntityImpl taskEntity = (TaskEntityImpl) findById(id);
        getMongoDbSession().delete(COLLECTION_TASKS, taskEntity);
    }

    @Override
    public void delete(TaskEntity taskEntity) {
        delete(taskEntity.getId());
    }

    @Override
    public List<TaskEntity> findTasksByExecutionId(String executionId) {
        Bson filter = Filters.eq("executionId", executionId);
        return getMongoDbSession().find(COLLECTION_TASKS, filter);
    }

    @Override
    public List<TaskEntity> findTasksByProcessInstanceId(String processInstanceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TaskEntity> findTasksByScopeIdAndScopeType(String scopeId, String scopeType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TaskEntity> findTasksBySubScopeIdAndScopeType(String subScopeId, String scopeType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Task> findTasksByQueryCriteria(TaskQueryImpl taskQuery) {
        // TODO: extract and implement properly
        return getMongoDbSession().find(COLLECTION_TASKS, null);
    }

    @Override
    public long findTaskCountByQueryCriteria(TaskQueryImpl taskQuery) {
        // TODO: extract and implement properly
        return getMongoDbSession().count(COLLECTION_TASKS, null);
    }
    
    @Override
    public List<Task> findTasksWithRelatedEntitiesByQueryCriteria(TaskQueryImpl taskQuery) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Task> findTasksByNativeQuery(Map<String, Object> parameterMap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long findTaskCountByNativeQuery(Map<String, Object> parameterMap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Task> findTasksByParentTaskId(String parentTaskId) {
        return getMongoDbSession().find(COLLECTION_TASKS, Filters.eq("parentTaskId", parentTaskId));
    }

    @Override
    public void updateTaskTenantIdForDeployment(String deploymentId, String newTenantId) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void updateAllTaskRelatedEntityCountFlags(boolean newValue) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void deleteTasksByExecutionId(String executionId) {
        // TODO: add support for bulkDelete operation (cfr relational logic)
        List<TaskEntity> tasksEntities = findTasksByExecutionId(executionId);
        for (TaskEntity taskEntity : tasksEntities) {
            getMongoDbSession().delete(COLLECTION_TASKS, taskEntity);
        }
    }
    
}
