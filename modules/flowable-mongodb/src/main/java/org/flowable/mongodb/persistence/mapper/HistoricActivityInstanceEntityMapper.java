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
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntityImpl;
import org.flowable.mongodb.persistence.EntityToDocumentMapper;

/**
 * @author Joram Barrez
 */
public class HistoricActivityInstanceEntityMapper implements EntityToDocumentMapper<HistoricActivityInstanceEntityImpl> {

    @Override
    public HistoricActivityInstanceEntityImpl fromDocument(Document document) {
        HistoricActivityInstanceEntityImpl activityEntity = new HistoricActivityInstanceEntityImpl();
        activityEntity.setId(document.getString("_id")); 
        activityEntity.setActivityId(document.getString("activityId"));
        activityEntity.setActivityName(document.getString("activityName"));
        activityEntity.setActivityType(document.getString("activityType"));
        activityEntity.setProcessInstanceId(document.getString("processInstanceId"));
        activityEntity.setProcessDefinitionId(document.getString("processDefinitionId"));
        activityEntity.setAssignee(document.getString("assignee"));
        activityEntity.setCalledProcessInstanceId(document.getString("calledProcessInstanceId"));
        activityEntity.setDeleteReason(document.getString("deleteReason"));
        activityEntity.setDurationInMillis(document.getLong("durationInMillis"));
        activityEntity.setEndTime(document.getDate("endTime"));
        activityEntity.setExecutionId(document.getString("executionId"));
        activityEntity.setRevision(document.getInteger("revision"));
        activityEntity.setStartTime(document.getDate("startTime"));
        activityEntity.setTaskId(document.getString("taskId"));
        activityEntity.setTenantId(document.getString("tenantId"));
        
        return activityEntity;
    }

    @Override
    public Document toDocument(HistoricActivityInstanceEntityImpl activityEntity) {
        Document instanceDocument = new Document();
        instanceDocument.append("_id", activityEntity.getId());
        instanceDocument.append("activityId", activityEntity.getActivityId());
        instanceDocument.append("activityName", activityEntity.getActivityName());
        instanceDocument.append("activityType", activityEntity.getActivityType());
        instanceDocument.append("processInstanceId", activityEntity.getProcessInstanceId());
        instanceDocument.append("processDefinitionId", activityEntity.getProcessDefinitionId());
        instanceDocument.append("assignee", activityEntity.getAssignee());
        instanceDocument.append("calledProcessInstanceId", activityEntity.getCalledProcessInstanceId());
        instanceDocument.append("deleteReason", activityEntity.getDeleteReason());
        instanceDocument.append("durationInMillis", activityEntity.getDurationInMillis());
        instanceDocument.append("endTime", activityEntity.getEndTime());
        instanceDocument.append("executionId", activityEntity.getExecutionId());
        instanceDocument.append("revision", activityEntity.getRevision());
        instanceDocument.append("startTime", activityEntity.getStartTime());
        instanceDocument.append("taskId", activityEntity.getTaskId());
        instanceDocument.append("tenantId", activityEntity.getTenantId());
        
        return instanceDocument;
    }

}
