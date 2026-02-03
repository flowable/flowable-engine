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

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.flowable.cmmn.api.migration.ActivatePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationDocument;
import org.flowable.cmmn.api.migration.ChangePlanItemDefinitionWithNewTargetIdsMapping;
import org.flowable.cmmn.api.migration.ChangePlanItemIdMapping;
import org.flowable.cmmn.api.migration.ChangePlanItemIdWithDefinitionIdMapping;
import org.flowable.cmmn.api.migration.MoveToAvailablePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.RemoveWaitingForRepetitionPlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.TerminatePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.WaitingForRepetitionPlanItemDefinitionMapping;
import org.flowable.common.engine.api.FlowableException;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * @author Valentin Zickner
 */
public class CaseInstanceMigrationDocumentConverter implements CaseInstanceMigrationDocumentConstants {

    protected static Predicate<JsonNode> isNotNullNode = jsonNode -> jsonNode != null && !jsonNode.isNull();
    protected static Predicate<JsonNode> isSingleTextValue = jsonNode -> isNotNullNode.test(jsonNode) && jsonNode.isString();
    protected static Predicate<JsonNode> isMultiValue = jsonNode -> isNotNullNode.test(jsonNode) && jsonNode.isArray();

    protected static ObjectMapper objectMapper = JsonMapper.shared();

    public static JsonNode convertToJson(CaseInstanceMigrationDocument caseInstanceMigrationDocument) {
        ObjectNode documentNode = objectMapper.createObjectNode();

        if (caseInstanceMigrationDocument.getMigrateToCaseDefinitionId() != null) {
            documentNode.put(TO_CASE_DEFINITION_ID_JSON_PROPERTY, caseInstanceMigrationDocument.getMigrateToCaseDefinitionId());
        }

        if (caseInstanceMigrationDocument.getMigrateToCaseDefinitionKey() != null) {
            documentNode.put(TO_CASE_DEFINITION_KEY_JSON_PROPERTY, caseInstanceMigrationDocument.getMigrateToCaseDefinitionKey());
        }

        if (caseInstanceMigrationDocument.getMigrateToCaseDefinitionVersion() != null) {
            documentNode.put(TO_CASE_DEFINITION_VERSION_JSON_PROPERTY, caseInstanceMigrationDocument.getMigrateToCaseDefinitionVersion());
        }

        if (caseInstanceMigrationDocument.getMigrateToCaseDefinitionTenantId() != null) {
            documentNode.put(TO_CASE_DEFINITION_TENANT_ID_JSON_PROPERTY, caseInstanceMigrationDocument.getMigrateToCaseDefinitionTenantId());
        }
        
        if (caseInstanceMigrationDocument.getEnableAutomaticPlanItemInstanceCreation() != null) {
            documentNode.put(ENABLE_AUTOMATIC_PLAN_ITEM_INSTANCE_CREATION_JSON_PROPERTY, caseInstanceMigrationDocument.getEnableAutomaticPlanItemInstanceCreation());
        }

        ArrayNode activateMappingNodes = convertToJsonActivatePlanItemDefinitionMappings(caseInstanceMigrationDocument.getActivatePlanItemDefinitionMappings());
        if (activateMappingNodes != null && !activateMappingNodes.isNull()) {
            documentNode.set(ACTIVATE_PLAN_ITEM_DEFINITIONS_JSON_SECTION, activateMappingNodes);
        }
        
        ArrayNode terminateMappingNodes = convertToJsonTerminatePlanItemDefinitionMappings(caseInstanceMigrationDocument.getTerminatePlanItemDefinitionMappings());
        if (terminateMappingNodes != null && !terminateMappingNodes.isNull()) {
            documentNode.set(TERMINATE_PLAN_ITEM_DEFINITIONS_JSON_SECTION, terminateMappingNodes);
        }
        
        ArrayNode moveToAvailableMappingNodes = convertToJsonMoveToAvailablePlanItemDefinitionMappings(caseInstanceMigrationDocument.getMoveToAvailablePlanItemDefinitionMappings());
        if (moveToAvailableMappingNodes != null && !moveToAvailableMappingNodes.isNull()) {
            documentNode.set(MOVE_TO_AVAILABLE_PLAN_ITEM_DEFINITIONS_JSON_SECTION, moveToAvailableMappingNodes);
        }
        
        ArrayNode waitingForRepetitionMappingNodes = convertToJsonWaitingForRepetitionPlanItemDefinitionMappings(caseInstanceMigrationDocument.getWaitingForRepetitionPlanItemDefinitionMappings());
        if (waitingForRepetitionMappingNodes != null && !waitingForRepetitionMappingNodes.isNull()) {
            documentNode.set(WAITING_FOR_REPETITION_PLAN_ITEM_DEFINITIONS_JSON_SECTION, waitingForRepetitionMappingNodes);
        }
        
        ArrayNode removeWaitingForRepetitionMappingNodes = convertToJsonRemoveWaitingForRepetitionPlanItemDefinitionMappings(caseInstanceMigrationDocument.getRemoveWaitingForRepetitionPlanItemDefinitionMappings());
        if (removeWaitingForRepetitionMappingNodes != null && !removeWaitingForRepetitionMappingNodes.isNull()) {
            documentNode.set(REMOVE_WAITING_FOR_REPETITION_PLAN_ITEM_DEFINITIONS_JSON_SECTION, removeWaitingForRepetitionMappingNodes);
        }
        
        ArrayNode changePlanItemIdMappingNodes = convertToJsonChangePlanItemIdMappings(caseInstanceMigrationDocument.getChangePlanItemIdMappings());
        if (changePlanItemIdMappingNodes != null && !changePlanItemIdMappingNodes.isNull()) {
            documentNode.set(CHANGE_PLAN_ITEM_IDS_JSON_SECTION, changePlanItemIdMappingNodes);
        }
        
        ArrayNode changePlanItemIdWithDefinitionIdMappingNodes = convertToJsonChangePlanItemIdWithDefinitionIdMappings(caseInstanceMigrationDocument.getChangePlanItemIdWithDefinitionIdMappings());
        if (changePlanItemIdWithDefinitionIdMappingNodes != null && !changePlanItemIdWithDefinitionIdMappingNodes.isNull()) {
            documentNode.set(CHANGE_PLAN_ITEM_IDS_WITH_DEFINITION_ID_JSON_SECTION, changePlanItemIdWithDefinitionIdMappingNodes);
        }
        
        ArrayNode changePlanItemDefinitionWithNewTargetIdsMappingNodes = convertToJsonChangePlanItemDefinitionWithNewTargetIdsMappings(caseInstanceMigrationDocument.getChangePlanItemDefinitionWithNewTargetIdsMappings());
        if (changePlanItemDefinitionWithNewTargetIdsMappingNodes != null && !changePlanItemDefinitionWithNewTargetIdsMappingNodes.isNull()) {
            documentNode.set(CHANGE_PLAN_ITEM_DEFINITION_WITH_NEW_TARGET_IDS_JSON_SECTION, changePlanItemDefinitionWithNewTargetIdsMappingNodes);
        }

        if (caseInstanceMigrationDocument.getPreUpgradeExpression() != null) {
            documentNode.put(PRE_UPGRADE_EXPRESSION_KEY_JSON_PROPERTY, caseInstanceMigrationDocument.getPreUpgradeExpression());
        }

        if (caseInstanceMigrationDocument.getPostUpgradeExpression() != null) {
            documentNode.put(POST_UPGRADE_EXPRESSION_KEY_JSON_PROPERTY, caseInstanceMigrationDocument.getPostUpgradeExpression());
        }
        
        JsonNode caseInstanceVariablesNode = convertToJsonCaseInstanceVariables(caseInstanceMigrationDocument, objectMapper);
        if (caseInstanceVariablesNode != null && !caseInstanceVariablesNode.isNull()) {
            documentNode.set(CASE_INSTANCE_VARIABLES_JSON_SECTION, caseInstanceVariablesNode);
        }

        return documentNode;
    }

    public static String convertToJsonString(CaseInstanceMigrationDocument caseInstanceMigrationDocument) {
        JsonNode jsonNode = convertToJson(caseInstanceMigrationDocument);
        ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
        try {
            return objectWriter.writeValueAsString(jsonNode);
        } catch (JacksonException e) {
            return jsonNode.toString();
        }
    }

    protected static ArrayNode convertToJsonActivatePlanItemDefinitionMappings(List<ActivatePlanItemDefinitionMapping> planItemDefinitionMappings) {
        ArrayNode mappingsArray = objectMapper.createArrayNode();

        for (ActivatePlanItemDefinitionMapping mapping : planItemDefinitionMappings) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            mappingNode.put(PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mapping.getPlanItemDefinitionId());
            mappingNode.put(NEW_ASSIGNEE_JSON_PROPERTY, mapping.getNewAssignee());
            mappingNode.put(CONDITION_JSON_PROPERTY, mapping.getCondition());
            Map<String, Object> localVariables = mapping.getWithLocalVariables();
            if (localVariables != null && !localVariables.isEmpty()) {
                mappingNode.set(LOCAL_VARIABLES_JSON_SECTION, objectMapper.valueToTree(localVariables));
            }

            mappingsArray.add(mappingNode);
        }

        return mappingsArray;
    }
    
    protected static ArrayNode convertToJsonTerminatePlanItemDefinitionMappings(List<TerminatePlanItemDefinitionMapping> planItemDefinitionMappings) {
        ArrayNode mappingsArray = objectMapper.createArrayNode();

        for (TerminatePlanItemDefinitionMapping mapping : planItemDefinitionMappings) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            mappingNode.put(PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mapping.getPlanItemDefinitionId());
            mappingNode.put(CONDITION_JSON_PROPERTY, mapping.getCondition());
            mappingsArray.add(mappingNode);
        }

        return mappingsArray;
    }
    
    protected static ArrayNode convertToJsonMoveToAvailablePlanItemDefinitionMappings(List<MoveToAvailablePlanItemDefinitionMapping> planItemDefinitionMappings) {
        ArrayNode mappingsArray = objectMapper.createArrayNode();

        for (MoveToAvailablePlanItemDefinitionMapping mapping : planItemDefinitionMappings) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            mappingNode.put(PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mapping.getPlanItemDefinitionId());
            mappingNode.put(CONDITION_JSON_PROPERTY, mapping.getCondition());
            Map<String, Object> localVariables = mapping.getWithLocalVariables();
            if (localVariables != null && !localVariables.isEmpty()) {
                mappingNode.set(LOCAL_VARIABLES_JSON_SECTION, objectMapper.valueToTree(localVariables));
            }

            mappingsArray.add(mappingNode);
        }

        return mappingsArray;
    }
    
    protected static ArrayNode convertToJsonWaitingForRepetitionPlanItemDefinitionMappings(List<WaitingForRepetitionPlanItemDefinitionMapping> planItemDefinitionMappings) {
        ArrayNode mappingsArray = objectMapper.createArrayNode();

        for (WaitingForRepetitionPlanItemDefinitionMapping mapping : planItemDefinitionMappings) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            mappingNode.put(PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mapping.getPlanItemDefinitionId());
            mappingNode.put(CONDITION_JSON_PROPERTY, mapping.getCondition());
            mappingsArray.add(mappingNode);
        }

        return mappingsArray;
    }
    
    protected static ArrayNode convertToJsonRemoveWaitingForRepetitionPlanItemDefinitionMappings(List<RemoveWaitingForRepetitionPlanItemDefinitionMapping> planItemDefinitionMappings) {
        ArrayNode mappingsArray = objectMapper.createArrayNode();

        for (RemoveWaitingForRepetitionPlanItemDefinitionMapping mapping : planItemDefinitionMappings) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            mappingNode.put(PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mapping.getPlanItemDefinitionId());
            mappingNode.put(CONDITION_JSON_PROPERTY, mapping.getCondition());
            mappingsArray.add(mappingNode);
        }

        return mappingsArray;
    }
    
    protected static ArrayNode convertToJsonChangePlanItemIdMappings(List<ChangePlanItemIdMapping> planItemIdMappings) {
        ArrayNode mappingsArray = objectMapper.createArrayNode();

        for (ChangePlanItemIdMapping mapping : planItemIdMappings) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            mappingNode.put(EXISTING_PLAN_ITEM_ID_JSON_PROPERTY, mapping.getExistingPlanItemId());
            mappingNode.put(NEW_PLAN_ITEM_ID_JSON_PROPERTY, mapping.getNewPlanItemId());
            mappingsArray.add(mappingNode);
        }

        return mappingsArray;
    }
    
    protected static ArrayNode convertToJsonChangePlanItemIdWithDefinitionIdMappings(List<ChangePlanItemIdWithDefinitionIdMapping> definitionIdMappings) {
        ArrayNode mappingsArray = objectMapper.createArrayNode();

        for (ChangePlanItemIdWithDefinitionIdMapping mapping : definitionIdMappings) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            mappingNode.put(EXISTING_PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mapping.getExistingPlanItemDefinitionId());
            mappingNode.put(NEW_PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mapping.getNewPlanItemDefinitionId());
            mappingsArray.add(mappingNode);
        }

        return mappingsArray;
    }
    
    protected static ArrayNode convertToJsonChangePlanItemDefinitionWithNewTargetIdsMappings(List<ChangePlanItemDefinitionWithNewTargetIdsMapping> definitionIdMappings) {
        ArrayNode mappingsArray = objectMapper.createArrayNode();

        for (ChangePlanItemDefinitionWithNewTargetIdsMapping mapping : definitionIdMappings) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            mappingNode.put(EXISTING_PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mapping.getExistingPlanItemDefinitionId());
            mappingNode.put(NEW_PLAN_ITEM_ID_JSON_PROPERTY, mapping.getNewPlanItemId());
            mappingNode.put(NEW_PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mapping.getNewPlanItemDefinitionId());
            mappingsArray.add(mappingNode);
        }

        return mappingsArray;
    }

    public static CaseInstanceMigrationDocument convertFromJson(String jsonCaseInstanceMigrationDocument) {

        try {
            JsonNode rootNode = objectMapper.readTree(jsonCaseInstanceMigrationDocument);
            CaseInstanceMigrationDocumentBuilderImpl documentBuilder = new CaseInstanceMigrationDocumentBuilderImpl();

            documentBuilder.setCaseDefinitionToMigrateTo(getJsonProperty(TO_CASE_DEFINITION_ID_JSON_PROPERTY, rootNode));
            
            String caseDefinitionKey = getJsonProperty(TO_CASE_DEFINITION_KEY_JSON_PROPERTY, rootNode);
            Integer caseDefinitionVersion = getJsonPropertyAsInteger(TO_CASE_DEFINITION_VERSION_JSON_PROPERTY, rootNode);
            documentBuilder.setCaseDefinitionToMigrateTo(caseDefinitionKey, caseDefinitionVersion);

            documentBuilder.setTenantId(getJsonProperty(TO_CASE_DEFINITION_TENANT_ID_JSON_PROPERTY, rootNode));
            
            documentBuilder.setEnableAutomaticPlanItemInstanceCreation(getJsonPropertyAsBoolean(ENABLE_AUTOMATIC_PLAN_ITEM_INSTANCE_CREATION_JSON_PROPERTY, rootNode));

            JsonNode activateMappingNodes = rootNode.get(ACTIVATE_PLAN_ITEM_DEFINITIONS_JSON_SECTION);
            if (activateMappingNodes != null) {
                for (JsonNode mappingNode : activateMappingNodes) {
                    String planItemDefinitionId = getJsonProperty(PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mappingNode);
                    ActivatePlanItemDefinitionMapping activateDefinitionMapping = new ActivatePlanItemDefinitionMapping(planItemDefinitionId);
                    String newAssginee = getJsonProperty(NEW_ASSIGNEE_JSON_PROPERTY, mappingNode);
                    activateDefinitionMapping.setNewAssignee(newAssginee);
                    String condition = getJsonProperty(CONDITION_JSON_PROPERTY, mappingNode);
                    activateDefinitionMapping.setCondition(condition);
                    Map<String, Object> localVariables = getLocalVariablesFromJson(mappingNode, objectMapper);
                    if (localVariables != null) {
                        activateDefinitionMapping.setWithLocalVariables(localVariables);
                    }
                    
                    documentBuilder.addActivatePlanItemDefinitionMapping(activateDefinitionMapping);
                }
            }
            
            JsonNode terminateMappingNodes = rootNode.get(TERMINATE_PLAN_ITEM_DEFINITIONS_JSON_SECTION);
            if (terminateMappingNodes != null) {
                for (JsonNode mappingNode : terminateMappingNodes) {
                    String planItemDefinitionId = getJsonProperty(PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mappingNode);
                    TerminatePlanItemDefinitionMapping terminateDefinitionMapping = new TerminatePlanItemDefinitionMapping(planItemDefinitionId);
                    String condition = getJsonProperty(CONDITION_JSON_PROPERTY, mappingNode);
                    terminateDefinitionMapping.setCondition(condition);
                    documentBuilder.addTerminatePlanItemDefinitionMapping(terminateDefinitionMapping);
                }
            }
            
            JsonNode moveToAvailableMappingNodes = rootNode.get(MOVE_TO_AVAILABLE_PLAN_ITEM_DEFINITIONS_JSON_SECTION);
            if (moveToAvailableMappingNodes != null) {
                for (JsonNode mappingNode : moveToAvailableMappingNodes) {
                    String planItemDefinitionId = getJsonProperty(PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mappingNode);
                    MoveToAvailablePlanItemDefinitionMapping moveToAvailableDefinitionMapping = new MoveToAvailablePlanItemDefinitionMapping(planItemDefinitionId);
                    String condition = getJsonProperty(CONDITION_JSON_PROPERTY, mappingNode);
                    moveToAvailableDefinitionMapping.setCondition(condition);
                    documentBuilder.addMoveToAvailablePlanItemDefinitionMapping(moveToAvailableDefinitionMapping);
                    Map<String, Object> localVariables = getLocalVariablesFromJson(mappingNode, objectMapper);
                    if (localVariables != null) {
                        moveToAvailableDefinitionMapping.setWithLocalVariables(localVariables);
                    }

                }
            }
            
            JsonNode waitingForRepetitionMappingNodes = rootNode.get(WAITING_FOR_REPETITION_PLAN_ITEM_DEFINITIONS_JSON_SECTION);
            if (waitingForRepetitionMappingNodes != null) {
                for (JsonNode mappingNode : waitingForRepetitionMappingNodes) {
                    String planItemDefinitionId = getJsonProperty(PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mappingNode);
                    WaitingForRepetitionPlanItemDefinitionMapping waitingForRepetitionDefinitionMapping = new WaitingForRepetitionPlanItemDefinitionMapping(planItemDefinitionId);
                    String condition = getJsonProperty(CONDITION_JSON_PROPERTY, mappingNode);
                    waitingForRepetitionDefinitionMapping.setCondition(condition);
                    documentBuilder.addWaitingForRepetitionPlanItemDefinitionMapping(waitingForRepetitionDefinitionMapping);
                }
            }
            
            JsonNode removeWaitingForRepetitionMappingNodes = rootNode.get(REMOVE_WAITING_FOR_REPETITION_PLAN_ITEM_DEFINITIONS_JSON_SECTION);
            if (removeWaitingForRepetitionMappingNodes != null) {
                for (JsonNode mappingNode : removeWaitingForRepetitionMappingNodes) {
                    String planItemDefinitionId = getJsonProperty(PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mappingNode);
                    RemoveWaitingForRepetitionPlanItemDefinitionMapping removeWaitingForRepetitionDefinitionMapping = new RemoveWaitingForRepetitionPlanItemDefinitionMapping(planItemDefinitionId);
                    String condition = getJsonProperty(CONDITION_JSON_PROPERTY, mappingNode);
                    removeWaitingForRepetitionDefinitionMapping.setCondition(condition);
                    documentBuilder.addRemoveWaitingForRepetitionPlanItemDefinitionMapping(removeWaitingForRepetitionDefinitionMapping);
                }
            }
            
            JsonNode changePlanItemIdMappingNodes = rootNode.get(CHANGE_PLAN_ITEM_IDS_JSON_SECTION);
            if (changePlanItemIdMappingNodes != null) {
                for (JsonNode mappingNode : changePlanItemIdMappingNodes) {
                    String existingPlanItemId = getJsonProperty(EXISTING_PLAN_ITEM_ID_JSON_PROPERTY, mappingNode);
                    String newPlanItemId = getJsonProperty(NEW_PLAN_ITEM_ID_JSON_PROPERTY, mappingNode);
                    ChangePlanItemIdMapping changePlanItemIdMapping = new ChangePlanItemIdMapping(existingPlanItemId, newPlanItemId);
                    documentBuilder.addChangePlanItemIdMapping(changePlanItemIdMapping);
                }
            }
            
            JsonNode changePlanItemIdWithDefinitionIdMappingNodes = rootNode.get(CHANGE_PLAN_ITEM_IDS_WITH_DEFINITION_ID_JSON_SECTION);
            if (changePlanItemIdWithDefinitionIdMappingNodes != null) {
                for (JsonNode mappingNode : changePlanItemIdWithDefinitionIdMappingNodes) {
                    String existingPlanItemDefinitionId = getJsonProperty(EXISTING_PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mappingNode);
                    String newPlanItemDefinitionId = getJsonProperty(NEW_PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mappingNode);
                    ChangePlanItemIdWithDefinitionIdMapping changePlanItemIdWithDefinitionIdMapping = new ChangePlanItemIdWithDefinitionIdMapping(existingPlanItemDefinitionId, newPlanItemDefinitionId);
                    documentBuilder.addChangePlanItemIdWithDefinitionIdMapping(changePlanItemIdWithDefinitionIdMapping);
                }
            }
            
            JsonNode changePlanItemDefinitionWithNewTargetIdsMappingNodes = rootNode.get(CHANGE_PLAN_ITEM_DEFINITION_WITH_NEW_TARGET_IDS_JSON_SECTION);
            if (changePlanItemDefinitionWithNewTargetIdsMappingNodes != null) {
                for (JsonNode mappingNode : changePlanItemDefinitionWithNewTargetIdsMappingNodes) {
                    String existingPlanItemDefinitionId = getJsonProperty(EXISTING_PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mappingNode);
                    String newPlanItemId = getJsonProperty(NEW_PLAN_ITEM_ID_JSON_PROPERTY, mappingNode);
                    String newPlanItemDefinitionId = getJsonProperty(NEW_PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mappingNode);
                    ChangePlanItemDefinitionWithNewTargetIdsMapping changePlanItemDefinitionWithNewTargetIdsMapping = new ChangePlanItemDefinitionWithNewTargetIdsMapping(existingPlanItemDefinitionId, newPlanItemId, newPlanItemDefinitionId);
                    documentBuilder.addChangePlanItemDefinitionWithNewTargetIdsMapping(changePlanItemDefinitionWithNewTargetIdsMapping);
                }
            }

            JsonNode caseInstanceVariablesNode = rootNode.get(CASE_INSTANCE_VARIABLES_JSON_SECTION);
            if (caseInstanceVariablesNode != null) {
                Map<String, Object> caseInstanceVariables = convertFromJsonNodeToObject(caseInstanceVariablesNode, objectMapper);
                documentBuilder.addCaseInstanceVariables(caseInstanceVariables);
            }

            String preUpgradeExpression = getJsonProperty(PRE_UPGRADE_EXPRESSION_KEY_JSON_PROPERTY, rootNode);
            documentBuilder.preUpgradeExpression(preUpgradeExpression);

            String postUpgradeExpression = getJsonProperty(POST_UPGRADE_EXPRESSION_KEY_JSON_PROPERTY, rootNode);
            documentBuilder.postUpgradeExpression(postUpgradeExpression);

            return documentBuilder.build();

        } catch (JacksonException e) {
            throw new FlowableException("Error parsing Case Instance Migration Document", e);
        }

    }

    protected static JsonNode convertToJsonCaseInstanceVariables(CaseInstanceMigrationDocument caseInstanceMigrationDocument, ObjectMapper objectMapper) {
        Map<String, Object> caseInstanceVariables = caseInstanceMigrationDocument.getCaseInstanceVariables();
        if (caseInstanceVariables != null && !caseInstanceVariables.isEmpty()) {
            return objectMapper.valueToTree(caseInstanceVariables);
        }
        return null;
    }
    
    protected static <T> T convertFromJsonNodeToObject(JsonNode jsonNode, ObjectMapper objectMapper) {
        return objectMapper.convertValue(jsonNode, new TypeReference<>() {

        });
    }
    
    protected static String getJsonProperty(String propertyName, JsonNode jsonNode) {
        if (jsonNode.has(propertyName) && !jsonNode.get(propertyName).isNull()) {
            return jsonNode.get(propertyName).asString();
        }
        
        return null;
    }
    
    protected static Boolean getJsonPropertyAsBoolean(String propertyName, JsonNode jsonNode) {
        if (jsonNode.has(propertyName) && !jsonNode.get(propertyName).isNull()) {
            return jsonNode.get(propertyName).asBoolean();
        }
        
        return null;
    }
    
    protected static Integer getJsonPropertyAsInteger(String propertyName, JsonNode jsonNode) {
        if (jsonNode.has(propertyName) && !jsonNode.get(propertyName).isNull()) {
            return jsonNode.get(propertyName).asInt();
        }
        
        return null;
    }

    protected static <V> V getLocalVariablesFromJson(JsonNode jsonNode, ObjectMapper objectMapper) {
        JsonNode localVariablesNode = jsonNode.get(LOCAL_VARIABLES_JSON_SECTION);
        if (localVariablesNode != null) {
            return convertFromJsonNodeToObject(localVariablesNode, objectMapper);
        }
        return null;
    }
}

