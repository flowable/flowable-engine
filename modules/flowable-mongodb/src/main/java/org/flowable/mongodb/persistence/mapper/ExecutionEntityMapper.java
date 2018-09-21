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
import org.flowable.mongodb.persistence.EntityToDocumentMapper;

/**
 * @author Joram Barrez
 */
public class ExecutionEntityMapper extends AbstractEntityToDocumentMapper<ExecutionEntityImpl> {

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
        executionEntity.setCallbackId(document.getString("callBackId"));
        executionEntity.setCallbackType(document.getString("callbackType"));

        // Entity counts settings
        executionEntity.setCountEnabled(document.getBoolean("countEnabled"));
        executionEntity.setEventSubscriptionCount(document.getInteger("eventSubscriptionCount"));
        executionEntity.setTaskCount(document.getInteger("taskCount"));
        executionEntity.setJobCount(document.getInteger("jobCount"));
        executionEntity.setTimerJobCount(document.getInteger("timerJobCount"));
        executionEntity.setSuspendedJobCount(document.getInteger("suspendedJobCount"));
        executionEntity.setDeadLetterJobCount(document.getInteger("deadLetterJobCount"));
        executionEntity.setVariableCount(document.getInteger("variableCount"));
        executionEntity.setIdentityLinkCount(document.getInteger("identityLinkCount"));

        return executionEntity;
    }

    @Override
    public Document toDocument(ExecutionEntityImpl executionEntity) {
        Document executionDocument = new Document();
        appendIfNotNull(executionDocument, "_id", executionEntity.getId());
        appendIfNotNull(executionDocument, "processInstanceId", executionEntity.getProcessInstanceId());
        appendIfNotNull(executionDocument, "businessKey", executionEntity.getBusinessKey());
        appendIfNotNull(executionDocument, "processDefinitionId", executionEntity.getProcessDefinitionId());
        appendIfNotNull(executionDocument, "activityId", executionEntity.getActivityId());
        appendIfNotNull(executionDocument, "isActive", executionEntity.isActive());
        appendIfNotNull(executionDocument, "isConcurrent", executionEntity.isConcurrent());
        appendIfNotNull(executionDocument, "isScope", executionEntity.isScope());
        appendIfNotNull(executionDocument, "isEventScope", executionEntity.isEventScope());
        appendIfNotNull(executionDocument, "isMultiInstanceRoot", executionEntity.isMultiInstanceRoot());
        appendIfNotNull(executionDocument, "parentId", executionEntity.getParentId());
        appendIfNotNull(executionDocument, "superExecutionId", executionEntity.getSuperExecutionId());
        appendIfNotNull(executionDocument, "rootProcessInstanceId", executionEntity.getRootProcessInstanceId());
        appendIfNotNull(executionDocument, "suspensionState", executionEntity.getSuspensionState());
        appendIfNotNull(executionDocument, "tenantId", executionEntity.getTenantId());
        appendIfNotNull(executionDocument, "name", executionEntity.getName());
        appendIfNotNull(executionDocument, "startActivityId", executionEntity.getStartActivityId());
        appendIfNotNull(executionDocument, "startTime", executionEntity.getStartTime());
        appendIfNotNull(executionDocument, "startUserId", executionEntity.getStartUserId());
        appendIfNotNull(executionDocument, "callbackId", executionEntity.getCallbackId());
        appendIfNotNull(executionDocument, "callbackType", executionEntity.getCallbackType());

        // Entity counts settings
        appendIfNotNull(executionDocument, "countEnabled", executionEntity.isCountEnabled());
        appendIfNotNull(executionDocument, "eventSubscriptionCount", executionEntity.getEventSubscriptionCount());
        appendIfNotNull(executionDocument, "taskCount", executionEntity.getTaskCount());
        appendIfNotNull(executionDocument, "jobCount", executionEntity.getJobCount());
        appendIfNotNull(executionDocument, "timerJobCount", executionEntity.getTimerJobCount());
        appendIfNotNull(executionDocument, "suspendedJobCount", executionEntity.getSuspendedJobCount());
        appendIfNotNull(executionDocument, "deadLetterJobCount", executionEntity.getDeadLetterJobCount());
        appendIfNotNull(executionDocument, "variableCount", executionEntity.getVariableCount());
        appendIfNotNull(executionDocument, "identityLinkCount", executionEntity.getIdentityLinkCount());

        return executionDocument;
    }

}
