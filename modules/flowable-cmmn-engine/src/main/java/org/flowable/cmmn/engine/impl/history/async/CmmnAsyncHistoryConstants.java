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
package org.flowable.cmmn.engine.impl.history.async;

import java.util.Arrays;
import java.util.List;

/**
 * @author Joram Barrez
 */
public interface CmmnAsyncHistoryConstants {
    
    String JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY = "cmmn-async-history";
    String JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY_ZIPPED = "cmmn-async-history-zipped";
    
    String TYPE_CASE_INSTANCE_END = "cmmn-case-instance-end";
    String TYPE_CASE_INSTANCE_START = "cmmn-case-instance-start";
    String TYPE_HISTORIC_CASE_INSTANCE_DELETED = "cmmn-historic-case-instance-deleted";
    String TYPE_IDENTITY_LINK_CREATED = "cmmn-identity-link-created";
    String TYPE_IDENTITY_LINK_DELETED = "cmmn-identity-link-deleted";
    String TYPE_MILESTONE_REACHED = "cmmn-milestone-reached";
    String TYPE_PLAN_ITEM_INSTANCE_AVAILABLE = "cmmn-plan-item-instance-available";
    String TYPE_PLAN_ITEM_INSTANCE_COMPLETED = "cmmn-plan-item-instance-completed";
    String TYPE_PLAN_ITEM_INSTANCE_CREATED = "cmmn-plan-item-instance-created";
    String TYPE_PLAN_ITEM_INSTANCE_DISABLED = "cmmn-plan-item-instance-disabled";
    String TYPE_PLAN_ITEM_INSTANCE_ENABLED = "cmmn-plan-item-instance-enabled";
    String TYPE_PLAN_ITEM_INSTANCE_EXIT = "cmmn-plan-item-instance-exit";
    String TYPE_PLAN_ITEM_INSTANCE_OCCURRED = "cmmn-plan-item-instance-occurred";
    String TYPE_PLAN_ITEM_INSTANCE_SUSPENDED = "cmmn-plan-item-instance-suspended";
    String TYPE_PLAN_ITEM_INSTANCE_STARTED = "cmmn-plan-item-instance-started";
    String TYPE_PLAN_ITEM_INSTANCE_TERMINATED = "cmmn-plan-item-instance-terminated";
    String TYPE_TASK_CREATED = "cmmn-task-created";
    String TYPE_TASK_REMOVED = "cmmn-task-removed";
    String TYPE_TASK_UPDATED = "cmmn-task-updated";
    String TYPE_VARIABLE_CREATED = "cmmn-variable-created";
    String TYPE_VARIABLE_REMOVED = "cmmn-variable-removed";
    String TYPE_VARIABLE_UPDATED = "cmmn-variable-updated";
    
    List<String> ORDERED_TYPES = Arrays.asList(
            TYPE_CASE_INSTANCE_START,
            TYPE_IDENTITY_LINK_CREATED,
            TYPE_IDENTITY_LINK_DELETED,
            TYPE_MILESTONE_REACHED,
            TYPE_PLAN_ITEM_INSTANCE_CREATED,
            TYPE_PLAN_ITEM_INSTANCE_AVAILABLE,
            TYPE_PLAN_ITEM_INSTANCE_COMPLETED,
            TYPE_PLAN_ITEM_INSTANCE_DISABLED,
            TYPE_PLAN_ITEM_INSTANCE_ENABLED,
            TYPE_PLAN_ITEM_INSTANCE_EXIT,
            TYPE_PLAN_ITEM_INSTANCE_OCCURRED,
            TYPE_PLAN_ITEM_INSTANCE_SUSPENDED,
            TYPE_PLAN_ITEM_INSTANCE_STARTED,
            TYPE_PLAN_ITEM_INSTANCE_TERMINATED,
            TYPE_TASK_CREATED,
            TYPE_TASK_REMOVED,
            TYPE_TASK_UPDATED,
            TYPE_VARIABLE_CREATED,
            TYPE_VARIABLE_REMOVED,
            TYPE_VARIABLE_UPDATED,
            TYPE_HISTORIC_CASE_INSTANCE_DELETED,
            TYPE_CASE_INSTANCE_END
    );

    String FIELD_ASSIGNEE = "assignee";
    String FIELD_BUSINESS_KEY = "businessKey";
    String FIELD_CASE_DEFINITION_CATEGORY = "caseDefinitionCategory";
    String FIELD_CASE_DEFINITION_DEPLOYMENT_ID = "caseDefinitionDeploymentId";
    String FIELD_CASE_DEFINITION_DESCRIPTION = "caseDefinitionDescription";
    String FIELD_CASE_DEFINITION_ID = "caseDefinitionId";
    String FIELD_CASE_DEFINITION_KEY = "caseDefinitionKey";
    String FIELD_CASE_DEFINITION_NAME = "caseDefinitionName";
    String FIELD_CASE_DEFINITION_VERSION = "caseDefinitionVersion";
    String FIELD_CASE_INSTANCE_ID = "caseInstanceId";
    String FIELD_CATEGORY = "category";
    String FIELD_CLAIM_TIME = "claimTime";
    String FIELD_COMPLETED_TIME = "completedTime";
    String FIELD_CREATE_TIME = "createTime";
    String FIELD_DELETE_REASON = "deleteReason";
    String FIELD_DESCRIPTION = "description";
    String FIELD_DUE_DATE = "dueDate";
    String FIELD_DURATION = "duration";
    String FIELD_ELEMENT_ID = "elementId";
    String FIELD_END_TIME = "endTime";
    String FIELD_EXIT_TIME = "exitTime";
    String FIELD_FORM_KEY = "formKey";
    String FIELD_GROUP_ID = "groupId";
    String FIELD_ID = "id";
    String FIELD_IS_STAGE = "isStage";
    String FIELD_IDENTITY_LINK_TYPE = "identityLinkType";
    String FIELD_LAST_AVAILABLE_TIME = "lastAvailableTime";
    String FIELD_LAST_DISABLED_TIME = "lastDisabledTime";
    String FIELD_LAST_ENABLED_TIME = "lastEnabledTime";
    String FIELD_LAST_STARTED_TIME = "lastStartedTime";
    String FIELD_LAST_SUSPENDED_TIME = "lastSuspendedTime";
    String FIELD_LAST_UPDATE_TIME = "lastUpdateTime";
    String FIELD_NAME = "name";
    String FIELD_OCCURRED_TIME = "occurredTime";
    String FIELD_OWNER = "owner";
    String FIELD_PARENT_ID = "parentId";
    String FIELD_PARENT_TASK_ID = "parentTaskId";
    String FIELD_PLAN_DEFINITION_ID = "planDefinitionId";
    String FIELD_PLAN_DEFINITION_TYPE = "planDefinitionType";
    String FIELD_PLAN_ITEM_INSTANCE_ID = "planItemInstanceId";
    String FIELD_PRIORITY = "priority";
    String FIELD_REFERENCE_ID = "referenceId";
    String FIELD_REFERENCE_TYPE = "referenceType";
    String FIELD_REVISION = "revision";
    String FIELD_SCOPE_ID = "scopeId";
    String FIELD_SCOPE_DEFINITION_ID = "scopeDefinitionId";
    String FIELD_SCOPE_TYPE = "scopeType";
    String FIELD_SUB_SCOPE_ID = "subScopeId";
    String FIELD_STAGE_INSTANCE_ID = "stageInstanceId";
    String FIELD_START_TIME = "startTime";
    String FIELD_START_USER_ID = "startUserId";
    String FIELD_STATE = "state";
    String FIELD_TASK_ID = "taskId";
    String FIELD_TASK_DEFINITION_ID = "taskDefinitionId";
    String FIELD_TASK_DEFINITION_KEY = "taskDefinitionKey";
    String FIELD_TERMINATED_TIME = "terminatedTime";
    String FIELD_TENANT_ID = "tenantId";
    String FIELD_USER_ID = "userId";
    String FIELD_VARIABLE_BYTES_VALUE = "variableBytesValue";
    String FIELD_VARIABLE_DOUBLE_VALUE = "variableDoubleValue";
    String FIELD_VARIABLE_LONG_VALUE = "variableLongValue";
    String FIELD_VARIABLE_TEXT_VALUE = "variableTextValue";
    String FIELD_VARIABLE_TEXT_VALUE2 = "variableTextValue2";
    String FIELD_VARIABLE_TYPE = "variableType";
 
}
