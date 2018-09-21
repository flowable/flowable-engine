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
package org.flowable.mongodb.persistence.mapper;

import org.bson.Document;
import org.flowable.mongodb.persistence.EntityToDocumentMapper;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;

/**
 * @author Joram Barrez
 */
public class TaskEntityMapper extends AbstractEntityToDocumentMapper<TaskEntityImpl> {

    @Override
    public TaskEntityImpl fromDocument(Document document) {
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

        // Entity counts
        taskEntity.setCountEnabled(document.getBoolean("countEnabled"));
        taskEntity.setVariableCount(document.getInteger("variableCount"));
        taskEntity.setIdentityLinkCount(document.getInteger("identityLinkCount"));
        taskEntity.setSubTaskCount(document.getInteger("subTaskCount"));

        return taskEntity;
    }

    @Override
    public Document toDocument(TaskEntityImpl taskEntity) {
        Document taskDocument = new Document();
        
        appendIfNotNull(taskDocument, "_id", taskEntity.getId());
        appendIfNotNull(taskDocument, "name", taskEntity.getName());
        appendIfNotNull(taskDocument, "parentTaskId", taskEntity.getParentTaskId());
        appendIfNotNull(taskDocument, "description", taskEntity.getDescription());
        appendIfNotNull(taskDocument, "priority", taskEntity.getPriority());
        appendIfNotNull(taskDocument, "createTime", taskEntity.getCreateTime());
        appendIfNotNull(taskDocument, "owner", taskEntity.getOwner());
        appendIfNotNull(taskDocument, "assignee", taskEntity.getAssignee());
        appendIfNotNull(taskDocument, "delegationState", taskEntity.getDelegationState() != null ? taskEntity.getDelegationState().toString() : null);
        appendIfNotNull(taskDocument, "executionId", taskEntity.getExecutionId());
        appendIfNotNull(taskDocument, "processInstanceId", taskEntity.getProcessInstanceId());
        appendIfNotNull(taskDocument, "processDefinitionId", taskEntity.getProcessDefinitionId());
        appendIfNotNull(taskDocument, "taskDefinitionId", taskEntity.getTaskDefinitionId());
        appendIfNotNull(taskDocument, "scopeId", taskEntity.getScopeId());
        appendIfNotNull(taskDocument, "subScopeId", taskEntity.getSubScopeId());
        appendIfNotNull(taskDocument, "scopeType", taskEntity.getScopeType());
        appendIfNotNull(taskDocument, "scopeDefinitionId", taskEntity.getScopeDefinitionId());
        appendIfNotNull(taskDocument, "taskDefinitionKey", taskEntity.getTaskDefinitionKey());
        appendIfNotNull(taskDocument, "dueDate", taskEntity.getDueDate());
        appendIfNotNull(taskDocument, "category", taskEntity.getCategory());
        appendIfNotNull(taskDocument, "suspensionState", taskEntity.getSuspensionState());
        appendIfNotNull(taskDocument, "formKey", taskEntity.getFormKey());
        appendIfNotNull(taskDocument, "claimTime", taskEntity.getClaimTime());
        appendIfNotNull(taskDocument, "tenantId", taskEntity.getTenantId());

        // Entity counts
        appendIfNotNull(taskDocument, "countEnabled", taskEntity.isCountEnabled());
        appendIfNotNull(taskDocument, "variableCount", taskEntity.getVariableCount());
        appendIfNotNull(taskDocument, "identityLinkCount", taskEntity.getIdentityLinkCount());
        appendIfNotNull(taskDocument, "subTaskCount", taskEntity.getSubTaskCount());

        return taskDocument;
    }

}
