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

import org.bson.Document;
import org.bson.conversions.Bson;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.TaskQueryImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.flowable.task.service.impl.persistence.entity.data.TaskDataManager;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
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
        return transformToEntity(getMongoDbSession().findOne(COLLECTION_TASKS, taskId));
    }

    @Override
    public void insert(TaskEntity taskEntity) {
        Document taskDocument = new Document();
        
        taskDocument.append("name", taskEntity.getName());
        taskDocument.append("parentTaskId", taskEntity.getParentTaskId());
        taskDocument.append("description", taskEntity.getDescription());
        taskDocument.append("priority", taskEntity.getPriority());
        taskDocument.append("createTime", taskEntity.getCreateTime());
        taskDocument.append("owner", taskEntity.getOwner());
        taskDocument.append("assignee", taskEntity.getAssignee());
        taskDocument.append("delegationState", taskEntity.getDelegationState() != null ? taskEntity.getDelegationState().toString() : null);
        taskDocument.append("executionId", taskEntity.getExecutionId());
        taskDocument.append("processInstanceId", taskEntity.getProcessInstanceId());
        taskDocument.append("processDefinitionId", taskEntity.getProcessDefinitionId());
        taskDocument.append("taskDefinitionId", taskEntity.getTaskDefinitionId());
        taskDocument.append("scopeId", taskEntity.getScopeId());
        taskDocument.append("subScopeId", taskEntity.getSubScopeId());
        taskDocument.append("scopeType", taskEntity.getScopeType());
        taskDocument.append("scopeDefinitionId", taskEntity.getScopeDefinitionId());
        taskDocument.append("taskDefinitionKey", taskEntity.getTaskDefinitionKey());
        taskDocument.append("dueDate", taskEntity.getDueDate());
        taskDocument.append("category", taskEntity.getCategory());
        taskDocument.append("suspensionState", taskEntity.getSuspensionState());
        taskDocument.append("formKey", taskEntity.getFormKey());
        taskDocument.append("claimTime", taskEntity.getClaimTime());
        taskDocument.append("tenantId", taskEntity.getTenantId());
        
        // TODO performance settings
        
        getMongoDbSession().insertOne(taskEntity, COLLECTION_TASKS, taskDocument);
    }

    @Override
    public TaskEntity update(TaskEntity entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String id) {
        getMongoDbSession().delete(COLLECTION_TASKS, id);
    }

    @Override
    public void delete(TaskEntity taskEntity) {
        delete(taskEntity.getId());
    }

    @Override
    public List<TaskEntity> findTasksByExecutionId(String executionId) {
        Bson filter = Filters.eq("executionId", executionId);
        FindIterable<Document> taskDocuments = getMongoDbSession().find(COLLECTION_TASKS, filter);
        List<TaskEntity> taskEntities = new ArrayList<>();
        for (Document taskDocument : taskDocuments) {
            taskEntities.add(transformToEntity(taskDocument));
        }
        return taskEntities;
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
        FindIterable<Document> taskDocuments = getMongoDbSession().find(COLLECTION_TASKS, null);
        List<Task> tasks = new ArrayList<>();
        for (Document document : taskDocuments) {
            tasks.add(transformToEntity(document));
        }
        return tasks;
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
        FindIterable<Document> taskDocuments = getMongoDbSession().find(COLLECTION_TASKS, Filters.eq("parentTaskId", parentTaskId));
        List<Task> tasks = new ArrayList<>();
        for (Document taskDocument : taskDocuments) {
            tasks.add(transformToEntity(taskDocument));
        }
        return tasks;
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
        throw new UnsupportedOperationException();        
    }
    
    public TaskEntityImpl transformToEntity(Document document) {
        TaskEntityImpl taskEntity = new TaskEntityImpl();
        
        taskEntity.setId(document.getString("_id"));
        taskEntity.setName(document.getString("name"));
        taskEntity.setDescription(document.getString("description"));
        taskEntity.setPriority(document.getInteger("priority"));
        taskEntity.setCreateTime(document.getDate("createTime"));
        taskEntity.setOwner(document.getString("owner"));
        taskEntity.setAssignee(document.getString("assignee"));
        taskEntity.setDelegationStateString(document.getString("delegationState"));
        taskEntity.setExecutionId(document.getString("executionId"));
        taskEntity.setProcessInstanceId(document.getString("processInstanceId"));
        taskEntity.setProcessDefinitionId(document.getString("processDefinitionId"));
        taskEntity.setTaskDefinitionId(document.getString("taskDefinitionId"));
        taskEntity.setScopeId(document.getString("scopeId"));
        taskEntity.setSubScopeId(document.getString("subScopeId"));
        taskEntity.setScopeType(document.getString("scopeType"));
        taskEntity.setScopeDefinitionId(document.getString("scopeDefinitionId"));
        taskEntity.setTaskDefinitionKey(document.getString("taskDefinitionKey"));
        taskEntity.setDueDate(document.getDate("dueDate"));
        taskEntity.setCategory(document.getString("category"));
        taskEntity.setSuspensionState(document.getInteger("suspensionState"));
        taskEntity.setFormKey(document.getString("formKey"));
        taskEntity.setClaimTime(document.getDate("claimTime"));
        taskEntity.setTenantId(document.getString("tenantId"));
        
        return taskEntity;
    }

}
