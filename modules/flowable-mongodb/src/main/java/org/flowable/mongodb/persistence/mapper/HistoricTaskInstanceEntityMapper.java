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
public class HistoricTaskInstanceEntityMapper implements EntityToDocumentMapper<HistoricTaskInstanceEntityImpl> {

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
        
        taskDocument.append("_id", taskEntity.getId());
        taskDocument.append("name", taskEntity.getName());
        taskDocument.append("parentTaskId", taskEntity.getParentTaskId());
        taskDocument.append("description", taskEntity.getDescription());
        taskDocument.append("priority", taskEntity.getPriority());
        taskDocument.append("createTime", taskEntity.getCreateTime());
        taskDocument.append("owner", taskEntity.getOwner());
        taskDocument.append("assignee", taskEntity.getAssignee());
        taskDocument.append("executionId", taskEntity.getExecutionId());
        taskDocument.append("processInstanceId", taskEntity.getProcessInstanceId());
        taskDocument.append("processDefinitionId", taskEntity.getProcessDefinitionId());
        taskDocument.append("taskDefinitionId", taskEntity.getTaskDefinitionId());
        taskDocument.append("scopeId", taskEntity.getScopeId());
        taskDocument.append("subScopeId", taskEntity.getSubScopeId());
        taskDocument.append("scopeType", taskEntity.getScopeType());
        taskDocument.append("scopeDefinitionId", taskEntity.getScopeDefinitionId());
        taskDocument.append("taskDefinitionKey", taskEntity.getTaskDefinitionKey());
        taskDocument.append("category", taskEntity.getCategory());
        taskDocument.append("claimTime", taskEntity.getClaimTime());
        taskDocument.append("deleteReason", taskEntity.getDeleteReason());
        taskDocument.append("dueDate", taskEntity.getDueDate());
        taskDocument.append("durationInMillis", taskEntity.getDurationInMillis());
        taskDocument.append("endTime", taskEntity.getEndTime());
        taskDocument.append("formKey", taskEntity.getFormKey());
        taskDocument.append("lastUpdateTime", taskEntity.getLastUpdateTime());
        taskDocument.append("revision", taskEntity.getRevision());
        taskDocument.append("startTime", taskEntity.getStartTime());
        taskDocument.append("tenantId", taskEntity.getTenantId());
        
        return taskDocument;
    }

}
