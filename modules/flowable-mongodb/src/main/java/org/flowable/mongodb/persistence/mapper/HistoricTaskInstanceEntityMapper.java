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
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntityImpl;

/**
 * @author Tijs Rademakers
 */
public class HistoricTaskInstanceEntityMapper extends AbstractEntityToDocumentMapper<HistoricTaskInstanceEntityImpl> {

    @Override
    public HistoricTaskInstanceEntityImpl fromDocument(Document document) {
        HistoricTaskInstanceEntityImpl taskEntity = new HistoricTaskInstanceEntityImpl();
        
        taskEntity.setId(document.getString("_id"));
        taskEntity.setName(document.getString("name"));
        taskEntity.setDescription(document.getString("description"));
        taskEntity.setPriority(document.getInteger("priority"));
        taskEntity.setOwner(document.getString("owner"));
        taskEntity.setAssignee(document.getString("assignee"));
        taskEntity.setExecutionId(document.getString("executionId"));
        taskEntity.setProcessInstanceId(document.getString("processInstanceId"));
        taskEntity.setProcessDefinitionId(document.getString("processDefinitionId"));
        taskEntity.setParentTaskId(document.getString("parentTaskId"));
        taskEntity.setTaskDefinitionId(document.getString("taskDefinitionId"));
        taskEntity.setScopeId(document.getString("scopeId"));
        taskEntity.setSubScopeId(document.getString("subScopeId"));
        taskEntity.setScopeType(document.getString("scopeType"));
        taskEntity.setScopeDefinitionId(document.getString("scopeDefinitionId"));
        taskEntity.setTaskDefinitionKey(document.getString("taskDefinitionKey"));
        taskEntity.setCategory(document.getString("category"));
        taskEntity.setClaimTime(document.getDate("claimTime"));
        taskEntity.setDeleteReason(document.getString("deleteReason"));
        taskEntity.setDueDate(document.getDate("dueDate"));
        taskEntity.setDurationInMillis(document.getLong("durationInMillis"));
        taskEntity.setEndTime(document.getDate("endTime"));
        taskEntity.setFormKey(document.getString("formKey"));
        taskEntity.setLastUpdateTime(document.getDate("lastUpdateTime"));
        taskEntity.setRevision(document.getInteger("revision"));
        taskEntity.setStartTime(document.getDate("startTime"));
        taskEntity.setTenantId(document.getString("tenantId"));
        
        return taskEntity;
    }

    @Override
    public Document toDocument(HistoricTaskInstanceEntityImpl taskEntity) {
        Document taskDocument = new Document();
        
        appendIfNotNull(taskDocument, "_id", taskEntity.getId());
        appendIfNotNull(taskDocument, "name", taskEntity.getName());
        appendIfNotNull(taskDocument, "parentTaskId", taskEntity.getParentTaskId());
        appendIfNotNull(taskDocument, "description", taskEntity.getDescription());
        appendIfNotNull(taskDocument, "priority", taskEntity.getPriority());
        appendIfNotNull(taskDocument, "createTime", taskEntity.getCreateTime());
        appendIfNotNull(taskDocument, "owner", taskEntity.getOwner());
        appendIfNotNull(taskDocument, "assignee", taskEntity.getAssignee());
        appendIfNotNull(taskDocument, "executionId", taskEntity.getExecutionId());
        appendIfNotNull(taskDocument, "processInstanceId", taskEntity.getProcessInstanceId());
        appendIfNotNull(taskDocument, "processDefinitionId", taskEntity.getProcessDefinitionId());
        appendIfNotNull(taskDocument, "taskDefinitionId", taskEntity.getTaskDefinitionId());
        appendIfNotNull(taskDocument, "scopeId", taskEntity.getScopeId());
        appendIfNotNull(taskDocument, "subScopeId", taskEntity.getSubScopeId());
        appendIfNotNull(taskDocument, "scopeType", taskEntity.getScopeType());
        appendIfNotNull(taskDocument, "scopeDefinitionId", taskEntity.getScopeDefinitionId());
        appendIfNotNull(taskDocument, "taskDefinitionKey", taskEntity.getTaskDefinitionKey());
        appendIfNotNull(taskDocument, "category", taskEntity.getCategory());
        appendIfNotNull(taskDocument, "claimTime", taskEntity.getClaimTime());
        appendIfNotNull(taskDocument, "deleteReason", taskEntity.getDeleteReason());
        appendIfNotNull(taskDocument, "dueDate", taskEntity.getDueDate());
        appendIfNotNull(taskDocument, "durationInMillis", taskEntity.getDurationInMillis());
        appendIfNotNull(taskDocument, "endTime", taskEntity.getEndTime());
        appendIfNotNull(taskDocument, "formKey", taskEntity.getFormKey());
        appendIfNotNull(taskDocument, "lastUpdateTime", taskEntity.getLastUpdateTime());
        appendIfNotNull(taskDocument, "revision", taskEntity.getRevision());
        appendIfNotNull(taskDocument, "startTime", taskEntity.getStartTime());
        appendIfNotNull(taskDocument, "tenantId", taskEntity.getTenantId());
        
        return taskDocument;
    }

}
