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
package org.flowable.engine.impl.history.async;

public interface HistoryJsonConstants {
    
    String TYPE = "type";
    
    String TYPE_PROCESS_INSTANCE_START = "process-instance-start";
    String TYPE_SUBPROCESS_INSTANCE_START = "subprocess-instance-start";
    String TYPE_PROCESS_INSTANCE_PROPERTY_CHANGED = "process-instance-property-changed";
    String TYPE_SET_PROCESS_DEFINITION = "set-process-definition";
    String TYPE_UPDATE_PROCESS_DEFINITION_CASCADE = "update-process-definition-cascade";
    String TYPE_ACTIVITY_START = "activity-start";
    String TYPE_ACTIVITY_END = "activity-end";
    String TYPE_ACTIVITY_FULL = "activity-full";
    String TYPE_FORM_PROPERTIES_SUBMITTED = "form-properties-submitted";
    String TYPE_HISTORIC_DETAIL_VARIABLE_UPDATE = "historic-detail-variable-update";
    String TYPE_IDENTITY_LINK_CREATED = "identitylink-created";
    String TYPE_IDENTITY_LINK_DELETED = "identitylink-deleted";
    String TYPE_TASK_CREATED = "task-created";
    String TYPE_TASK_ASSIGNEE_CHANGED = "task-assignee-changed";
    String TYPE_TASK_OWNER_CHANGED = "task-owner-changed";
    String TYPE_TASK_PROPERTY_CHANGED = "task-property-changed";
    String TYPE_TASK_ENDED = "task-ended";
    String TYPE_VARIABLE_CREATED = "variable-created";
    String TYPE_VARIABLE_UPDATED = "variable-updated";
    String TYPE_VARIABLE_REMOVED = "variable-removed";
    String TYPE_PROCESS_INSTANCE_END = "process-instance-end";
    String TYPE_PROCESS_INSTANCE_DELETED = "process-instance-deleted";
    String TYPE_PROCESS_INSTANCE_DELETED_BY_PROCDEF_ID = "process-instance-deleted-by-process-definition-id";
    
    String DATA = "data";

    String ID = "id";

    String NAME = "name";

    String DESCRIPTION = "description";
    
    String REVISION = "revision";

    String CATEGORY = "category";

    String EXECUTION_ID = "executionId";
    
    String SOURCE_EXECUTION_ID = "sourceExecutionId";
    
    String IS_MULTI_INSTANCE_ROOT_EXECUTION = "isMiRootExecution";

    String PROCESS_INSTANCE_ID = "processInstanceId";
    
    String TASK_ID = "taskId";

    String BUSINESS_KEY = "businessKey";

    String PROCESS_DEFINITION_ID = "processDefinitionId";

    String PROCESS_DEFINITION_KEY = "processDefinitionKey";

    String PROCESS_DEFINITION_NAME = "processDefinitionName";

    String PROCESS_DEFINITION_VERSION = "processDefinitionVersion";

    String DEPLOYMENT_ID = "deploymentId";

    String START_TIME = "startTime";

    String END_TIME = "endTime";
    
    String CREATE_TIME = "createTime";

    String CLAIM_TIME = "claimTime";
    
    String LAST_UPDATED_TIME = "lastUpdatedTime";

    String START_USER_ID = "startUserId";

    String START_ACTIVITY_ID = "startActivityId";

    String ACTIVITY_ID = "activityId";

    String ACTIVITY_NAME = "activityName";

    String ACTIVITY_TYPE = "activityType";

    String SUPER_PROCESS_INSTANCE_ID = "superProcessInstanceId";

    String DELETE_REASON = "deleteReason";

    String PARENT_TASK_ID = "parentTaskId";

    String ASSIGNEE = "assignee";
    
    String ACTIVITY_ASSIGNEE_HANDLED = "activityAssigneeHandled";

    String OWNER = "owner";
    
    String IDENTITY_LINK_TYPE = "identityLinkType";

    String TASK_DEFINITION_KEY = "taskDefinitionKey";

    String FORM_KEY = "formKey";

    String PRIORITY = "priority";

    String DUE_DATE = "dueDate";

    String PROPERTY = "property";
    
    String VARIABLE_TYPE = "variableType";
    
    String VARIABLE_TEXT_VALUE = "variableTextValue";
    
    String VARIABLE_TEXT_VALUE2 = "variableTextValue2";
    
    String VARIABLE_DOUBLE_VALUE = "variableDoubleValue";
    
    String VARIABLE_LONG_VALUE = "variableLongValue";
    
    String VARIABLE_BYTES_VALUE = "variableBytesValue";
    
    String FORM_PROPERTY_ID = "formPropertyId";
    
    String FORM_PROPERTY_VALUE = "formPropertyValue";
    
    String USER_ID = "userId";
    
    String GROUP_ID = "groupId";

    String TENANT_ID = "tenantId";
    
    String CALLBACK_ID = "callbackId";
    
    String CALLBACK_TYPE = "callbackType";

    String TIMESTAMP = "__timeStamp"; // Two underscores to avoid clashes with other fields
    
}
