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

    String JOB_CREATE_TIME = "jobCreateTime";
}
