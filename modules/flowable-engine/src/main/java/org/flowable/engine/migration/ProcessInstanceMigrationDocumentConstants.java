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
package org.flowable.engine.migration;

/**
 * @author Dennis
 */
public interface ProcessInstanceMigrationDocumentConstants {

    String TO_PROCESS_DEFINITION_ID_JSON_PROPERTY = "toProcessDefinitionId";
    String TO_PROCESS_DEFINITION_KEY_JSON_PROPERTY = "toProcessDefinitionKey";
    String TO_PROCESS_DEFINITION_VERSION_JSON_PROPERTY = "toProcessDefinitionVersion";
    String TO_PROCESS_DEFINITION_TENANT_ID_JSON_PROPERTY = "toProcessDefinitionTenantId";

    String FROM_ACTIVITY_ID_JSON_PROPERTY = "fromActivityId";
    String FROM_ACTIVITY_IDS_JSON_PROPERTY = "fromActivityIds";
    String TO_ACTIVITY_ID_JSON_PROPERTY = "toActivityId";
    String TO_ACTIVITY_IDS_JSON_PROPERTY = "toActivityIds";
    String NEW_ASSIGNEE_JSON_PROPERTY = "newAssignee";

    String ACTIVITY_MAPPINGS_JSON_SECTION = "activityMappings";
    String LOCAL_VARIABLES_JSON_SECTION = "localVariables";
    String PROCESS_INSTANCE_VARIABLES_JSON_SECTION = "processInstanceVariables";

}

