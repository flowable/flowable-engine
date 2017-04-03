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

    String CATEGORY = "category";

    String EXECUTION_ID = "executionId";

    String PROCESS_INSTANCE_ID = "processInstanceId";

    String BUSINESS_KEY = "businessKey";

    String PROCESS_DEFINITION_ID = "processDefinitionId";

    String PROCESS_DEFINITION_KEY = "processDefinitionKey";

    String PROCESS_DEFINITION_NAME = "processDefinitionName";

    String PROCESS_DEFINITION_VERSION = "processDefinitionVersion";

    String DEPLOYMENT_ID = "deploymentId";

    String START_TIME = "startTime";

    String END_TIME = "endTime";

    String CLAIM_TIME = "claimTime";

    String START_USER_ID = "startUserId";

    String START_ACTIVITY_ID = "startActivityId";

    String ACTIVITY_ID = "activityId";

    String ACTIVITY_NAME = "activityName";

    String ACTIVITY_TYPE = "activityType";

    String SUPER_PROCESS_INSTANCE_ID = "superProcessInstanceId";

    String DELETE_REASON = "deleteReason";

    String PARENT_TASK_ID = "parentTaskId";

    String ASSIGNEE = "assignee";

    String OWNER = "owner";

    String TASK_DEFINITION_KEY = "taskDefinitionKey";

    String FORM_KEY = "formKey";

    String PRIORITY = "priority";

    String DUE_DATE = "dueDate";

    String PROPERTY = "property";

    String TENANT_ID = "tenantId";

}
