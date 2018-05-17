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
    String TYPE_VARIABLE_CREATED = "cmmn-variable-created";
    String TYPE_VARIABLE_REMOVED = "cmmn-variable-removed";
    String TYPE_VARIABLE_UPDATED = "cmmn-variable-updated";
    
    List<String> ORDERED_TYPES = Arrays.asList(
            TYPE_CASE_INSTANCE_END,
            TYPE_CASE_INSTANCE_START,
            TYPE_HISTORIC_CASE_INSTANCE_DELETED,
            TYPE_IDENTITY_LINK_CREATED,
            TYPE_IDENTITY_LINK_DELETED,
            TYPE_MILESTONE_REACHED,
            TYPE_VARIABLE_CREATED,
            TYPE_VARIABLE_REMOVED,
            TYPE_VARIABLE_UPDATED
    );

    String FIELD_BUSINESS_KEY = "businessKey";
    String FIELD_CASE_DEFINITION_CATEGORY = "caseDefinitionCategory";
    String FIELD_CASE_DEFINITION_DEPLOYMENT_ID = "caseDefinitionDeploymentId";
    String FIELD_CASE_DEFINITION_DESCRIPTION = "caseDefinitionDescription";
    String FIELD_CASE_DEFINITION_ID = "caseDefinitionId";
    String FIELD_CASE_DEFINITION_KEY = "caseDefinitionKey";
    String FIELD_CASE_DEFINITION_NAME = "caseDefinitionName";
    String FIELD_CASE_DEFINITION_VERSION = "caseDefinitionVersion";
    String FIELD_CASE_INSTANCE_ID = "caseInstanceId";
    String FIELD_CREATE_TIME = "createTime";
    String FIELD_DURATION = "duration";
    String FIELD_ELEMENT_ID = "elementId";
    String FIELD_END_TIME = "endTime";
    String FIELD_GROUP_ID = "groupId";
    String FIELD_ID = "id";
    String FIELD_IDENTITY_LINK_TYPE = "identityLinkType";
    String FIELD_LAST_UPDATE_TIME = "lastUpdateTime";
    String FIELD_NAME = "name";
    String FIELD_PARENT_ID = "parentId";
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
    String FIELD_TENANT_ID = "tenantId";
    String FIELD_USER_ID = "userId";
    String FIELD_VARIABLE_BYTES_VALUE = "variableBytesValue";
    String FIELD_VARIABLE_DOUBLE_VALUE = "variableDoubleValue";
    String FIELD_VARIABLE_LONG_VALUE = "variableLongValue";
    String FIELD_VARIABLE_TEXT_VALUE = "variableTextValue";
    String FIELD_VARIABLE_TEXT_VALUE2 = "variableTextValue2";
    String FIELD_VARIABLE_TYPE = "variableType";
 
}
