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
    
    List<String> ORDERED_TYPES = Arrays.asList();
    
    String JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY = "cmmn-async-history";
    String JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY_ZIPPED = "cmmn-async-history-zipped";
    
    String TYPE_CASE_INSTANCE_END = "cmmn-case-instance-end";
    String TYPE_CASE_INSTANCE_START = "cmmn-case-instance-start";

    String FIELD_BUSINESS_KEY = "businessKey";
    String FIELD_CASE_DEFINITION_CATEGORY = "caseDefinitionCategory";
    String FIELD_CASE_DEFINITION_DEPLOYMENT_ID = "caseDefinitionDeploymentId";
    String FIELD_CASE_DEFINITION_DESCRIPTION = "caseDefinitionDescription";
    String FIELD_CASE_DEFINITION_ID = "caseDefinitionId";
    String FIELD_CASE_DEFINITION_KEY = "caseDefinitionKey";
    String FIELD_CASE_DEFINITION_NAME = "caseDefinitionName";
    String FIELD_CASE_DEFINITION_VERSION = "caseDefinitionVersion";
    String FIELD_CASE_INSTANCE_ID = "caseInstanceId";
    String FIELD_DURATION = "duration";
    String FIELD_END_TIME = "endTime";
    String FIELD_ID = "id";
    String FIELD_NAME = "name";
    String FIELD_PARENT_ID = "parentId";
    String FIELD_STAGE_INSTANCE_ID = "stageInstanceId";
    String FIELD_START_TIME = "startTime";
    String FIELD_START_USER_ID = "startUserId";
    String FIELD_STATE = "state";
    String FIELD_TENANT_ID = "tenantId";
 
}
