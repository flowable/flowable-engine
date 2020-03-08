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
package org.flowable.cmmn.engine.impl.migration;

/**
 * @author Valentin Zickner
 */
public interface CaseInstanceMigrationDocumentConstants {

    String TO_CASE_DEFINITION_ID_JSON_PROPERTY = "toCaseDefinitionId";
    String TO_CASE_DEFINITION_KEY_JSON_PROPERTY = "toCaseDefinitionKey";
    String TO_CASE_DEFINITION_VERSION_JSON_PROPERTY = "toCaseDefinitionVersion";
    String TO_CASE_DEFINITION_TENANT_ID_JSON_PROPERTY = "toCaseDefinitionTenantId";

    String FROM_PLAN_ITEM_ID_JSON_PROPERTY = "fromPlanItemId";
    String FROM_PLAN_ITEM_IDS_JSON_PROPERTY = "fromPlanItemIds";
    String TO_PLAN_ITEM_ID_JSON_PROPERTY = "toPlanItemId";
    String TO_PLAN_ITEM_IDS_JSON_PROPERTY = "toPlanItemIds";
    String NEW_ASSIGNEE_JSON_PROPERTY = "newAssignee";

    String PLAN_ITEM_MAPPINGS_JSON_SECTION = "planItemMappings";
    String LOCAL_VARIABLES_JSON_SECTION = "localVariables";
    String CASE_INSTANCE_VARIABLES_JSON_SECTION = "caseInstanceVariables";

}

