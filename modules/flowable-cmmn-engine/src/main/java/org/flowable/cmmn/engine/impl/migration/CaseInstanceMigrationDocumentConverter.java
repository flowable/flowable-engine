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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationDocument;
import org.flowable.cmmn.api.migration.PlanItemMigrationMapping;
import org.flowable.cmmn.api.migration.PlanItemMigrationMappingOptions;
import org.flowable.common.engine.api.FlowableException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static org.flowable.cmmn.engine.impl.migration.CaseInstanceMigrationDocumentConstants.CASE_INSTANCE_VARIABLES_JSON_SECTION;
import static org.flowable.cmmn.engine.impl.migration.CaseInstanceMigrationDocumentConstants.FROM_PLAN_ITEM_IDS_JSON_PROPERTY;
import static org.flowable.cmmn.engine.impl.migration.CaseInstanceMigrationDocumentConstants.FROM_PLAN_ITEM_ID_JSON_PROPERTY;
import static org.flowable.cmmn.engine.impl.migration.CaseInstanceMigrationDocumentConstants.LOCAL_VARIABLES_JSON_SECTION;
import static org.flowable.cmmn.engine.impl.migration.CaseInstanceMigrationDocumentConstants.NEW_ASSIGNEE_JSON_PROPERTY;
import static org.flowable.cmmn.engine.impl.migration.CaseInstanceMigrationDocumentConstants.PLAN_ITEM_MAPPINGS_JSON_SECTION;
import static org.flowable.cmmn.engine.impl.migration.CaseInstanceMigrationDocumentConstants.TO_CASE_DEFINITION_ID_JSON_PROPERTY;
import static org.flowable.cmmn.engine.impl.migration.CaseInstanceMigrationDocumentConstants.TO_CASE_DEFINITION_KEY_JSON_PROPERTY;
import static org.flowable.cmmn.engine.impl.migration.CaseInstanceMigrationDocumentConstants.TO_CASE_DEFINITION_TENANT_ID_JSON_PROPERTY;
import static org.flowable.cmmn.engine.impl.migration.CaseInstanceMigrationDocumentConstants.TO_CASE_DEFINITION_VERSION_JSON_PROPERTY;
import static org.flowable.cmmn.engine.impl.migration.CaseInstanceMigrationDocumentConstants.TO_PLAN_ITEM_ID_JSON_PROPERTY;

/**
 * @author Valentin Zickner
 */
public class CaseInstanceMigrationDocumentConverter {

    protected static Predicate<JsonNode> isNotNullNode = jsonNode -> jsonNode != null && !jsonNode.isNull();
    protected static Predicate<JsonNode> isSingleTextValue = jsonNode -> isNotNullNode.test(jsonNode) && jsonNode.isTextual();
    protected static Predicate<JsonNode> isMultiValue = jsonNode -> isNotNullNode.test(jsonNode) && jsonNode.isArray();

    protected static ObjectMapper objectMapper = new ObjectMapper();

    protected static Map<Class<? extends PlanItemMigrationMapping>, BasePlanItemMigrationMappingConverter> planItemMigrationMappingConverters = new HashMap<>();

    static {
        planItemMigrationMappingConverters.put(PlanItemMigrationMapping.OneToOneMapping.class, new OneToOneMappingConverter());
        planItemMigrationMappingConverters.put(PlanItemMigrationMapping.ManyToOneMapping.class, new ManyToOneMappingConverter());
    }

    protected static <T> T convertFromJsonNodeToObject(JsonNode jsonNode, ObjectMapper objectMapper) {
        return objectMapper.convertValue(jsonNode, new TypeReference<T>() {

        });
    }

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

        ArrayNode mappingNodes = convertToJsonPlanItemMigrationMappings(caseInstanceMigrationDocument.getPlanItemMigrationMappings());
        if (mappingNodes != null && !mappingNodes.isNull()) {
            documentNode.set(PLAN_ITEM_MAPPINGS_JSON_SECTION, mappingNodes);
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

    protected static ArrayNode convertToJsonPlanItemMigrationMappings(List<? extends PlanItemMigrationMapping> planItemMigrationMappings) {
        ArrayNode mappingsArray = objectMapper.createArrayNode();

        for (PlanItemMigrationMapping mapping : planItemMigrationMappings) {
            BasePlanItemMigrationMappingConverter mappingConverter = planItemMigrationMappingConverters.get(mapping.getClass());
            if (mappingConverter == null) {
                throw new FlowableException("Cannot convert mapping of type '" + mapping.getClass() + "'");
            }
            ObjectNode mappingNode = mappingConverter.convertToJson(mapping, objectMapper);
            mappingsArray.add(mappingNode);
        }

        return mappingsArray;
    }

    public static CaseInstanceMigrationDocument convertFromJson(String jsonCaseInstanceMigrationDocument) {

        try {
            JsonNode rootNode = objectMapper.readTree(jsonCaseInstanceMigrationDocument);
            CaseInstanceMigrationDocumentBuilderImpl documentBuilder = new CaseInstanceMigrationDocumentBuilderImpl();

            String caseDefinitionId = Optional.ofNullable(rootNode.get(TO_CASE_DEFINITION_ID_JSON_PROPERTY))
                    .map(JsonNode::textValue).orElse(null);
            documentBuilder.setCaseDefinitionToMigrateTo(caseDefinitionId);

            String caseDefinitionKey = Optional.ofNullable(rootNode.get(TO_CASE_DEFINITION_KEY_JSON_PROPERTY))
                    .map(JsonNode::textValue).orElse(null);
            Integer caseDefinitionVersion = (Integer) Optional.ofNullable(rootNode.get(TO_CASE_DEFINITION_VERSION_JSON_PROPERTY))
                    .map(JsonNode::numberValue).orElse(null);
            documentBuilder.setCaseDefinitionToMigrateTo(caseDefinitionKey, caseDefinitionVersion);

            String caseDefinitionTenantId = Optional.ofNullable(rootNode.get(TO_CASE_DEFINITION_TENANT_ID_JSON_PROPERTY))
                    .map(JsonNode::textValue).orElse(null);
            documentBuilder.setTenantId(caseDefinitionTenantId);

            JsonNode planItemMigrationMappings = rootNode.get(PLAN_ITEM_MAPPINGS_JSON_SECTION);
            if (planItemMigrationMappings != null) {

                for (JsonNode mappingNode : planItemMigrationMappings) {
                    Class<? extends PlanItemMigrationMapping> mappingClass = null;
                    if (isSingleTextValue.test(mappingNode.get(FROM_PLAN_ITEM_ID_JSON_PROPERTY)) && isSingleTextValue.test(mappingNode.get(TO_PLAN_ITEM_ID_JSON_PROPERTY))) {
                        mappingClass = PlanItemMigrationMapping.OneToOneMapping.class;
                    }
                    if (isMultiValue.test(mappingNode.get(FROM_PLAN_ITEM_IDS_JSON_PROPERTY)) && isSingleTextValue.test(mappingNode.get(TO_PLAN_ITEM_ID_JSON_PROPERTY))) {
                        mappingClass = PlanItemMigrationMapping.ManyToOneMapping.class;
                    }

                    BasePlanItemMigrationMappingConverter mappingConverter = planItemMigrationMappingConverters.get(mappingClass);
                    PlanItemMigrationMapping mapping = mappingConverter.convertFromJson(mappingNode, objectMapper);
                    documentBuilder.addPlanItemMigrationMapping(mapping);
                }
            }

            JsonNode caseInstanceVariablesNode = rootNode.get(CASE_INSTANCE_VARIABLES_JSON_SECTION);
            if (caseInstanceVariablesNode != null) {
                Map<String, Object> caseInstanceVariables = CaseInstanceMigrationDocumentConverter.convertFromJsonNodeToObject(caseInstanceVariablesNode, objectMapper);
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

    public abstract static class BasePlanItemMigrationMappingConverter<T extends PlanItemMigrationMapping> {

        public ObjectNode convertToJson(T mapping, ObjectMapper objectMapper) {
            ObjectNode mappingNode = convertMappingInfoToJson(mapping, objectMapper);

            JsonNode newAssigneeToJsonNode = convertNewAssigneeToJson(mapping, objectMapper);
            if (newAssigneeToJsonNode != null && !newAssigneeToJsonNode.isNull()) {
                mappingNode.set(NEW_ASSIGNEE_JSON_PROPERTY, newAssigneeToJsonNode);
            }

            JsonNode variablesToJsonNode = convertLocalVariablesToJson(mapping, objectMapper);
            if (variablesToJsonNode != null && !variablesToJsonNode.isNull()) {
                mappingNode.set(LOCAL_VARIABLES_JSON_SECTION, variablesToJsonNode);
            }

            return mappingNode;
        }

        protected abstract ObjectNode convertMappingInfoToJson(T mapping, ObjectMapper objectMapper);

        protected ObjectNode convertAdditionalMappingInfoToJson(T mapping, ObjectMapper objectMapper) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            return mappingNode;
        }

        protected abstract JsonNode convertLocalVariablesToJson(T mapping, ObjectMapper objectMapper);

        protected abstract JsonNode convertNewAssigneeToJson(T mapping, ObjectMapper objectMapper);

        public abstract T convertFromJson(JsonNode jsonNode, ObjectMapper objectMapper);

        protected <M extends PlanItemMigrationMappingOptions<T>> void convertAdditionalMappingInfoFromJson(M mapping, JsonNode jsonNode) {
        }

        protected <V> V getLocalVariablesFromJson(JsonNode jsonNode, ObjectMapper objectMapper) {
            JsonNode localVariablesNode = jsonNode.get(LOCAL_VARIABLES_JSON_SECTION);
            if (localVariablesNode != null) {
                return CaseInstanceMigrationDocumentConverter.convertFromJsonNodeToObject(localVariablesNode, objectMapper);
            }
            return null;
        }

        protected String getNewAssigneeFromJson(JsonNode jsonNode) {
            if (isSingleTextValue.test(jsonNode.get(NEW_ASSIGNEE_JSON_PROPERTY))) {
                return jsonNode.get(NEW_ASSIGNEE_JSON_PROPERTY).textValue();
            }
            return null;
        }

    }

    public static class OneToOneMappingConverter extends BasePlanItemMigrationMappingConverter<PlanItemMigrationMapping.OneToOneMapping> {

        @Override
        protected ObjectNode convertMappingInfoToJson(PlanItemMigrationMapping.OneToOneMapping mapping, ObjectMapper objectMapper) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            mappingNode.put(FROM_PLAN_ITEM_ID_JSON_PROPERTY, mapping.getFromPlanItemId());
            mappingNode.put(TO_PLAN_ITEM_ID_JSON_PROPERTY, mapping.getToPlanItemId());
            mappingNode.setAll(convertAdditionalMappingInfoToJson(mapping, objectMapper));
            return mappingNode;
        }

        @Override
        public JsonNode convertLocalVariablesToJson(PlanItemMigrationMapping.OneToOneMapping mapping, ObjectMapper objectMapper) {
            Map<String, Object> planItemLocalVariables = mapping.getPlanItemLocalVariables();
            if (planItemLocalVariables != null && !planItemLocalVariables.isEmpty()) {
                return objectMapper.valueToTree(planItemLocalVariables);
            }
            return null;
        }

        @Override
        protected JsonNode convertNewAssigneeToJson(PlanItemMigrationMapping.OneToOneMapping mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewAssignee());
        }

        @Override
        public PlanItemMigrationMapping.OneToOneMapping convertFromJson(JsonNode jsonNode, ObjectMapper objectMapper) {
            String fromPlanItemId = jsonNode.get(FROM_PLAN_ITEM_ID_JSON_PROPERTY).textValue();
            String toPlanItemId = jsonNode.get(TO_PLAN_ITEM_ID_JSON_PROPERTY).textValue();

            PlanItemMigrationMapping.OneToOneMapping oneToOneMapping = PlanItemMigrationMapping.createMappingFor(fromPlanItemId, toPlanItemId);
            convertAdditionalMappingInfoFromJson(oneToOneMapping, jsonNode);

            Optional.ofNullable(getNewAssigneeFromJson(jsonNode))
                    .ifPresent(oneToOneMapping::withNewAssignee);

            Map<String, Object> localVariables = getLocalVariablesFromJson(jsonNode, objectMapper);
            if (localVariables != null) {
                oneToOneMapping.withLocalVariables(localVariables);
            }

            return oneToOneMapping;
        }

    }

    public static class ManyToOneMappingConverter extends BasePlanItemMigrationMappingConverter<PlanItemMigrationMapping.ManyToOneMapping> {

        @Override
        protected ObjectNode convertMappingInfoToJson(PlanItemMigrationMapping.ManyToOneMapping mapping, ObjectMapper objectMapper) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            JsonNode fromPlanItemIdsNode = objectMapper.valueToTree(mapping.getFromPlanItemIds());
            mappingNode.set(FROM_PLAN_ITEM_IDS_JSON_PROPERTY, fromPlanItemIdsNode);
            mappingNode.put(TO_PLAN_ITEM_ID_JSON_PROPERTY, mapping.getToPlanItemId());
            mappingNode.setAll(convertAdditionalMappingInfoToJson(mapping, objectMapper));
            return mappingNode;
        }

        @Override
        public JsonNode convertLocalVariablesToJson(PlanItemMigrationMapping.ManyToOneMapping mapping, ObjectMapper objectMapper) {
            Map<String, Object> planItemLocalVariables = mapping.getPlanItemLocalVariables();
            if (planItemLocalVariables != null && !planItemLocalVariables.isEmpty()) {
                return objectMapper.valueToTree(planItemLocalVariables);
            }
            return null;
        }

        @Override
        protected JsonNode convertNewAssigneeToJson(PlanItemMigrationMapping.ManyToOneMapping mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewAssignee());
        }

        @Override
        public PlanItemMigrationMapping.ManyToOneMapping convertFromJson(JsonNode jsonNode, ObjectMapper objectMapper) {
            JsonNode fromPlanItemIdsNode = jsonNode.get(FROM_PLAN_ITEM_IDS_JSON_PROPERTY);
            List<String> fromPlanItemIds = objectMapper.convertValue(fromPlanItemIdsNode, new TypeReference<List<String>>() {

            });
            String toPlanItemId = jsonNode.get(TO_PLAN_ITEM_ID_JSON_PROPERTY).textValue();

            PlanItemMigrationMapping.ManyToOneMapping manyToOneMapping = PlanItemMigrationMapping.createMappingFor(fromPlanItemIds, toPlanItemId);
            convertAdditionalMappingInfoFromJson(manyToOneMapping, jsonNode);

            Optional.ofNullable(getNewAssigneeFromJson(jsonNode))
                    .ifPresent(manyToOneMapping::withNewAssignee);

            Map<String, Object> localVariables = getLocalVariablesFromJson(jsonNode, objectMapper);
            if (localVariables != null) {
                manyToOneMapping.withLocalVariables(localVariables);
            }

            return manyToOneMapping;
        }
    }

}

