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
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.mongodb.persistence.EntityMapper;

/**
 * @author Joram Barrez
 */
public class ExecutionEntityMapper implements EntityMapper<ExecutionEntityImpl> {

    @Override
    public ExecutionEntityImpl fromDocument(Document document) {
        ExecutionEntityImpl executionEntity = new ExecutionEntityImpl();
        executionEntity.setId(document.getString("_id"));
        executionEntity.setProcessInstanceId(document.getString("processInstanceId"));
        executionEntity.setBusinessKey(document.getString("businessKey"));
        executionEntity.setProcessDefinitionId(document.getString("processDefinitionId"));
        executionEntity.setActivityId(document.getString("activityId"));
        executionEntity.setActive(document.getBoolean("isActive", false));
        executionEntity.setConcurrent(document.getBoolean("isConcurrent", false));
        executionEntity.setScope(document.getBoolean("isScope", false));
        executionEntity.setEventScope(document.getBoolean("isEventScope", false));
        executionEntity.setMultiInstanceRoot(document.getBoolean("isMultiInstanceRoot", false));
        executionEntity.setParentId(document.getString("parentId"));
        executionEntity.setSuperExecutionId(document.getString("superExecutionId"));
        executionEntity.setRootProcessInstanceId(document.getString("rootProcessInstanceId"));
        executionEntity.setSuspensionState(document.getInteger("suspensionState"));
        executionEntity.setTenantId(document.getString("tenantId"));
        executionEntity.setName(document.getString("name"));
        executionEntity.setStartActivityId(document.getString("startActivityId"));
        executionEntity.setStartTime(document.getDate("startTime"));
        executionEntity.setStartUserId(document.getString("startUserId"));
        
        // TODO: performance settings

        executionEntity.setCallbackId(document.getString("callBackId"));
        executionEntity.setCallbackType(document.getString("callbackType"));
        
        return executionEntity;
    }

    @Override
    public Document toDocument(ExecutionEntityImpl executionEntity) {
        Document executionDocument = new Document();
        executionDocument.append("_id", executionEntity.getId());
        executionDocument.append("processInstanceId", executionEntity.getProcessInstanceId());
        executionDocument.append("businessKey", executionEntity.getBusinessKey());
        executionDocument.append("processDefinitionId", executionEntity.getProcessDefinitionId());
        executionDocument.append("activityId", executionEntity.getActivityId());
        executionDocument.append("isActive", executionEntity.isActive());
        executionDocument.append("isConcurrent", executionEntity.isConcurrent());
        executionDocument.append("isScope", executionEntity.isScope());
        executionDocument.append("isEventScope", executionEntity.isEventScope());
        executionDocument.append("isMultiInstanceRoot", executionEntity.isMultiInstanceRoot());
        executionDocument.append("parentId", executionEntity.getParentId());
        executionDocument.append("superExecutionId", executionEntity.getSuperExecutionId());
        executionDocument.append("rootProcessInstanceId", executionEntity.getRootProcessInstanceId());
        executionDocument.append("suspensionState", executionEntity.getSuspensionState());
        executionDocument.append("tenantId", executionEntity.getTenantId());
        executionDocument.append("name", executionEntity.getName());
        executionDocument.append("startActivityId", executionEntity.getStartActivityId());
        executionDocument.append("startTime", executionEntity.getStartTime());
        executionDocument.append("startUserId", executionEntity.getStartUserId());
        
        // TODO: performance settings
        
        executionDocument.append("callbackId", executionEntity.getCallbackId());
        executionDocument.append("callbackType", executionEntity.getCallbackType());
        
        return executionDocument;
    }

}
