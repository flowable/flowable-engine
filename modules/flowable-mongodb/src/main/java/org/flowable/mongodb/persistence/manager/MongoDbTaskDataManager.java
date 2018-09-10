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
import java.util.List;
import java.util.Map;

import org.bson.conversions.Bson;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.TaskQueryImpl;
import org.flowable.task.service.impl.TaskQueryProperty;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.flowable.task.service.impl.persistence.entity.data.TaskDataManager;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

/**
 * @author Joram Barrez
 */
public class MongoDbTaskDataManager extends AbstractMongoDbDataManager<TaskEntity> implements TaskDataManager {
    
    public static final String COLLECTION_TASKS = "tasks";
    
    @Override
    public String getCollection() {
        return COLLECTION_TASKS;
    }

    @Override
    public TaskEntity create() {
        return new TaskEntityImpl();
    }

    @Override
    public BasicDBObject createUpdateObject(Entity entity) {
        TaskEntity taskEntity = (TaskEntity) entity;
        BasicDBObject updateObject = null;
        updateObject = setUpdateProperty(taskEntity, "assignee", taskEntity.getAssignee(), updateObject);
        updateObject = setUpdateProperty(taskEntity, "owner", taskEntity.getOwner(), updateObject);
        return updateObject;
    }

    @Override
    public List<TaskEntity> findTasksByExecutionId(String executionId) {
        Bson filter = Filters.eq("executionId", executionId);
        return getMongoDbSession().find(COLLECTION_TASKS, filter);
    }

    @Override
    public List<TaskEntity> findTasksByProcessInstanceId(String processInstanceId) {
        Bson filter = Filters.eq("processInstanceId", processInstanceId);
        return getMongoDbSession().find(COLLECTION_TASKS, filter);
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
        return getMongoDbSession().find(COLLECTION_TASKS, createFilter(taskQuery), createSort(taskQuery));
    }

    @Override
    public long findTaskCountByQueryCriteria(TaskQueryImpl taskQuery) {
        Bson filter = createFilter(taskQuery);
        return getMongoDbSession().count(COLLECTION_TASKS, filter);
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
    
    protected Bson createFilter(TaskQueryImpl taskQuery) {
        List<Bson> andFilters = new ArrayList<>();
        if (taskQuery.getExecutionId() != null) {
            andFilters.add(Filters.eq("executionId", taskQuery.getExecutionId()));
        }
        
        if (taskQuery.getProcessInstanceId() != null) {
            andFilters.add(Filters.eq("processInstanceId", taskQuery.getProcessInstanceId()));
        }
        
        if (taskQuery.getAssignee() != null) {
            andFilters.add(Filters.eq("assignee", taskQuery.getAssignee()));
        }
        
        if (taskQuery.getUnassigned()) {
            andFilters.add(Filters.eq("assignee", null));
        }
        
        if (taskQuery.getName() != null) {
            andFilters.add(Filters.eq("name", taskQuery.getName()));
        }
        
        Bson filter = null;
        if (andFilters.size() > 0) {
            filter = Filters.and(andFilters.toArray(new Bson[andFilters.size()]));
        }
        
        return filter;
    }
    
    protected Bson createSort(TaskQueryImpl taskQuery) {
        List<Bson> bsonSorts = new ArrayList<>();
        for (String column : taskQuery.getOrderByColumnMap().keySet()) {
            boolean isAscending = taskQuery.getOrderByColumnMap().get(column);
            String columnName = null;
            if (TaskQueryProperty.NAME.getName().equals(column)) {
                columnName = "name";
            }
            
            if (columnName != null) {
                if (isAscending) {
                    bsonSorts.add(Sorts.ascending(columnName));
                } else {
                    bsonSorts.add(Sorts.descending(columnName));
                }
            }
        }
        
        Bson bsonSortResult = null;
        if (bsonSorts.size() > 0) {
            bsonSortResult = Sorts.orderBy(bsonSorts);
        }
        
        return bsonSortResult;
    }
}
