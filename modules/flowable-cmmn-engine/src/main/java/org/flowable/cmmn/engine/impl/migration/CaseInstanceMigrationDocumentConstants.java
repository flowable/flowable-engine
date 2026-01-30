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
    String ENABLE_AUTOMATIC_PLAN_ITEM_INSTANCE_CREATION_JSON_PROPERTY = "enableAutomaticPlanItemInstanceCreation";
    

    String PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY = "planItemDefinitionId";
    String EXISTING_PLAN_ITEM_ID_JSON_PROPERTY = "existingPlanItemId";
    String NEW_PLAN_ITEM_ID_JSON_PROPERTY = "newPlanItemId";
    String EXISTING_PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY = "existingPlanItemDefinitionId";
    String NEW_PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY = "newPlanItemDefinitionId";
    String NEW_ASSIGNEE_JSON_PROPERTY = "newAssignee";
    String CONDITION_JSON_PROPERTY = "condition";

    String ACTIVATE_PLAN_ITEM_DEFINITIONS_JSON_SECTION = "activatePlanItemDefinitions";
    String TERMINATE_PLAN_ITEM_DEFINITIONS_JSON_SECTION = "terminatePlanItemDefinitions";
    String MOVE_TO_AVAILABLE_PLAN_ITEM_DEFINITIONS_JSON_SECTION = "moveToAvailablePlanItemDefinitions";
    String WAITING_FOR_REPETITION_PLAN_ITEM_DEFINITIONS_JSON_SECTION = "waitingForRepetitionPlanItemDefinitions";
    String REMOVE_WAITING_FOR_REPETITION_PLAN_ITEM_DEFINITIONS_JSON_SECTION = "removeWaitingForRepetitionPlanItemDefinitions";
    String CHANGE_PLAN_ITEM_IDS_JSON_SECTION = "changePlanItemIds";
    String CHANGE_PLAN_ITEM_IDS_WITH_DEFINITION_ID_JSON_SECTION = "changePlanItemIdsWithDefinitionId";
    String CHANGE_PLAN_ITEM_DEFINITION_WITH_NEW_TARGET_IDS_JSON_SECTION = "changePlanItemDefinitionWithNewTargetIds";
    String PRE_UPGRADE_EXPRESSION_KEY_JSON_PROPERTY = "preUpgradeExpression";
    String POST_UPGRADE_EXPRESSION_KEY_JSON_PROPERTY = "postUpgradeExpression";
    String LOCAL_VARIABLES_JSON_SECTION = "localVariables";
    String CASE_INSTANCE_VARIABLES_JSON_SECTION = "caseInstanceVariables";

}

