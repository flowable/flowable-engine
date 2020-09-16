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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.flowable.cmmn.api.migration.ActivatePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationDocument;
import org.flowable.cmmn.api.migration.MoveToAvailablePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.TerminatePlanItemDefinitionMapping;
import org.flowable.common.engine.api.FlowableException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Valentin Zickner
 */
public class CaseInstanceMigrationDocumentConverter implements CaseInstanceMigrationDocumentConstants {

    protected static Predicate<JsonNode> isNotNullNode = jsonNode -> jsonNode != null && !jsonNode.isNull();
    protected static Predicate<JsonNode> isSingleTextValue = jsonNode -> isNotNullNode.test(jsonNode) && jsonNode.isTextual();
    protected static Predicate<JsonNode> isMultiValue = jsonNode -> isNotNullNode.test(jsonNode) && jsonNode.isArray();

    protected static ObjectMapper objectMapper = new ObjectMapper();

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
            documentNode.set(TERMINATE_PLAN_ITEM_DEFINITIONS_JSON_SECTION, moveToAvailableMappingNodes);
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
        } catch (JsonProcessingException e) {
            return jsonNode.toString();
        }
    }

    protected static ArrayNode convertToJsonActivatePlanItemDefinitionMappings(List<ActivatePlanItemDefinitionMapping> planItemDefinitionMappings) {
        ArrayNode mappingsArray = objectMapper.createArrayNode();

        for (ActivatePlanItemDefinitionMapping mapping : planItemDefinitionMappings) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            mappingNode.put(PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mapping.getPlanItemDefinitionId());
            mappingNode.put(NEW_ASSIGNEE_JSON_PROPERTY, mapping.getNewAssignee());
            mappingsArray.add(mappingNode);
        }

        return mappingsArray;
    }
    
    protected static ArrayNode convertToJsonTerminatePlanItemDefinitionMappings(List<TerminatePlanItemDefinitionMapping> planItemDefinitionMappings) {
        ArrayNode mappingsArray = objectMapper.createArrayNode();

        for (TerminatePlanItemDefinitionMapping mapping : planItemDefinitionMappings) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            mappingNode.put(PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mapping.getPlanItemDefinitionId());
            mappingsArray.add(mappingNode);
        }

        return mappingsArray;
    }
    
    protected static ArrayNode convertToJsonMoveToAvailablePlanItemDefinitionMappings(List<MoveToAvailablePlanItemDefinitionMapping> planItemDefinitionMappings) {
        ArrayNode mappingsArray = objectMapper.createArrayNode();

        for (MoveToAvailablePlanItemDefinitionMapping mapping : planItemDefinitionMappings) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            mappingNode.put(PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mapping.getPlanItemDefinitionId());
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

            JsonNode activateMappingNodes = rootNode.get(ACTIVATE_PLAN_ITEM_DEFINITIONS_JSON_SECTION);
            if (activateMappingNodes != null) {
                for (JsonNode mappingNode : activateMappingNodes) {
                    String planItemDefinitionId = getJsonProperty(PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mappingNode);
                    ActivatePlanItemDefinitionMapping activateDefinitionMapping = new ActivatePlanItemDefinitionMapping(planItemDefinitionId);
                    String newAssginee = getJsonProperty(NEW_ASSIGNEE_JSON_PROPERTY, mappingNode);
                    activateDefinitionMapping.setNewAssignee(newAssginee);
                    
                    documentBuilder.addActivatePlanItemDefinitionMapping(activateDefinitionMapping);
                }
            }
            
            JsonNode terminateMappingNodes = rootNode.get(TERMINATE_PLAN_ITEM_DEFINITIONS_JSON_SECTION);
            if (terminateMappingNodes != null) {
                for (JsonNode mappingNode : terminateMappingNodes) {
                    String planItemDefinitionId = getJsonProperty(PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mappingNode);
                    TerminatePlanItemDefinitionMapping terminateDefinitionMapping = new TerminatePlanItemDefinitionMapping(planItemDefinitionId);
                    documentBuilder.addTerminatePlanItemDefinitionMapping(terminateDefinitionMapping);
                }
            }
            
            JsonNode moveToAvailableMappingNodes = rootNode.get(MOVE_TO_AVAILABLE_PLAN_ITEM_DEFINITIONS_JSON_SECTION);
            if (moveToAvailableMappingNodes != null) {
                for (JsonNode mappingNode : moveToAvailableMappingNodes) {
                    String planItemDefinitionId = getJsonProperty(PLAN_ITEM_DEFINITION_ID_JSON_PROPERTY, mappingNode);
                    MoveToAvailablePlanItemDefinitionMapping moveToAvailableDefinitionMapping = new MoveToAvailablePlanItemDefinitionMapping(planItemDefinitionId);
                    documentBuilder.addMoveToAvailablePlanItemDefinitionMapping(moveToAvailableDefinitionMapping);
                }
            }

            JsonNode caseInstanceVariablesNode = rootNode.get(CASE_INSTANCE_VARIABLES_JSON_SECTION);
            if (caseInstanceVariablesNode != null) {
                Map<String, Object> caseInstanceVariables = convertFromJsonNodeToObject(caseInstanceVariablesNode, objectMapper);
                documentBuilder.addCaseInstanceVariables(caseInstanceVariables);
            }
            return documentBuilder.build();

        } catch (IOException e) {
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
        return objectMapper.convertValue(jsonNode, new TypeReference<T>() {

        });
    }
    
    protected static String getJsonProperty(String propertyName, JsonNode jsonNode) {
        if (jsonNode.has(propertyName) && !jsonNode.get(propertyName).isNull()) {
            return jsonNode.get(propertyName).asText();
        }
        
        return null;
    }
    
    protected static Integer getJsonPropertyAsInteger(String propertyName, JsonNode jsonNode) {
        if (jsonNode.has(propertyName) && !jsonNode.get(propertyName).isNull()) {
            return jsonNode.get(propertyName).asInt();
        }
        
        return null;
    }
}

