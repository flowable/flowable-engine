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

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.migration.ProcessInstanceMigrationDocumentBuilderImpl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Dennis
 */
public class ProcessInstanceMigrationDocumentConverter {

    protected static Predicate<JsonNode> isNotNullNode = jsonNode -> jsonNode != null && !jsonNode.isNull();
    protected static Predicate<JsonNode> isSingleTextValue = jsonNode -> isNotNullNode.test(jsonNode) && jsonNode.isTextual();
    protected static Predicate<JsonNode> isMultiValue = jsonNode -> isNotNullNode.test(jsonNode) && jsonNode.isArray();

    protected static Map<Class<? extends ProcessInstanceActivityMigrationMapping>, BaseActivityMigrationMappingConverter> activityMigrationMappingConverters = new HashMap<>();

    static {
        activityMigrationMappingConverters.put(ProcessInstanceActivityMigrationMapping.OneToOneMapping.class, new OneToOneMappingConverter());
        activityMigrationMappingConverters.put(ProcessInstanceActivityMigrationMapping.OneToManyMapping.class, new OneToManyMappingConverter());
        activityMigrationMappingConverters.put(ProcessInstanceActivityMigrationMapping.ManyToOneMapping.class, new ManyToOneMappingConverter());
    }

    protected static <T> T convertJsonNode(ObjectMapper objectMapper, JsonNode jsonNode) {
        return objectMapper.convertValue(jsonNode, new TypeReference<T>() {

        });
    }

    public static JsonNode convertToJson(ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode documentNode = objectMapper.createObjectNode();

        documentNode.setAll(convertToJsonProcessDefinitionToMigrate(processInstanceMigrationDocument, objectMapper));

        ArrayNode mappingNodes = convertToJsonActivityMigrationMappings(processInstanceMigrationDocument.getActivityMigrationMappings(), objectMapper);
        if (mappingNodes != null && !mappingNodes.isNull()) {
            documentNode.set("activityMigrationMappings", mappingNodes);
        }

        JsonNode processInstanceVariablesNode = convertToJsonProcessInstanceVariables(processInstanceMigrationDocument.getProcessInstanceVariables(), objectMapper);
        if (processInstanceVariablesNode != null && !processInstanceVariablesNode.isNull()) {
            documentNode.set("processInstanceVariables", processInstanceVariablesNode);
        }

        return documentNode;
    }

    public static ProcessInstanceMigrationDocument convertFromJson(String jsonProcessInstanceMigrationDocument) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(jsonProcessInstanceMigrationDocument);
            ProcessInstanceMigrationDocumentBuilderImpl documentBuilder = new ProcessInstanceMigrationDocumentBuilderImpl();

            String processDefinitionId = Optional.ofNullable(rootNode.get("migrateToProcessDefinitionId"))
                .map(JsonNode::textValue).orElse(null);
            documentBuilder.setProcessDefinitionToMigrateTo(processDefinitionId);

            String processDefinitionKey = Optional.ofNullable(rootNode.get("migrateToProcessDefinitionKey"))
                .map(JsonNode::textValue).orElse(null);
            Integer processDefinitionVersion = (Integer) Optional.ofNullable(rootNode.get("migrateToProcessDefinitionVersion"))
                .map(JsonNode::numberValue).orElse(null);
            documentBuilder.setProcessDefinitionToMigrateTo(processDefinitionKey, processDefinitionVersion);

            String processDefinitionTenantId = Optional.ofNullable(rootNode.get("migrateToProcessDefinitionTenantId"))
                .map(JsonNode::textValue).orElse(null);
            documentBuilder.setTenantId(processDefinitionTenantId);

            JsonNode activityMigrationMappings = rootNode.get("activityMigrationMappings");
            if (activityMigrationMappings != null) {
                Iterator<JsonNode> mappingNodes = activityMigrationMappings.iterator();

                while (mappingNodes.hasNext()) {
                    JsonNode mappingNode = mappingNodes.next();

                    Class<? extends ProcessInstanceActivityMigrationMapping> mappingClass = null;
                    if (isMultiValue.test(mappingNode.get("fromActivityIds")) && isSingleTextValue.test(mappingNode.get("toActivityId"))) {
                        mappingClass = ProcessInstanceActivityMigrationMapping.ManyToOneMapping.class;
                    }
                    if (isSingleTextValue.test(mappingNode.get("toActivityId")) && isSingleTextValue.test(mappingNode.get("fromActivityId"))) {
                        mappingClass = ProcessInstanceActivityMigrationMapping.OneToOneMapping.class;
                    }
                    if (isMultiValue.test(mappingNode.get("toActivityIds")) && isSingleTextValue.test(mappingNode.get("fromActivityId"))) {
                        mappingClass = ProcessInstanceActivityMigrationMapping.OneToManyMapping.class;
                    }

                    BaseActivityMigrationMappingConverter mappingConverter = activityMigrationMappingConverters.get(mappingClass);
                    ProcessInstanceActivityMigrationMapping mapping = mappingConverter.convertFromJson(mappingNode, objectMapper);
                    documentBuilder.addActivityMigrationMapping(mapping);
                }
            }

            JsonNode processInstanceVariablesNode = rootNode.get("processInstanceVariables");
            if (processInstanceVariablesNode != null) {
                Map<String, Object> processInstanceVariables = ProcessInstanceMigrationDocumentConverter.convertJsonNode(objectMapper, processInstanceVariablesNode);
                documentBuilder.addProcessInstanceVariables(processInstanceVariables);
            }
            return documentBuilder.build();

        } catch (IOException e) {
            throw new FlowableException("Error parsing Process Instance Migration Document", e);
        }

    }

    protected static ObjectNode convertToJsonProcessDefinitionToMigrate(ProcessInstanceMigrationDocument processInstanceMigrationDocument, ObjectMapper objectMapper) {
        ObjectNode processDefinitionNode = objectMapper.createObjectNode();
        if (processInstanceMigrationDocument.getMigrateToProcessDefinitionId() != null) {
            processDefinitionNode.put("migrateToProcessDefinitionId", processInstanceMigrationDocument.getMigrateToProcessDefinitionId());
        }

        if (processInstanceMigrationDocument.getMigrateToProcessDefinitionKey() != null) {
            processDefinitionNode.put("migrateToProcessDefinitionKey", processInstanceMigrationDocument.getMigrateToProcessDefinitionKey());
        }

        if (processInstanceMigrationDocument.getMigrateToProcessDefinitionVersion() != null) {
            processDefinitionNode.put("migrateToProcessDefinitionVersion", processInstanceMigrationDocument.getMigrateToProcessDefinitionVersion());
        }

        if (processInstanceMigrationDocument.getMigrateToProcessDefinitionTenantId() != null) {
            processDefinitionNode.put("migrateToProcessDefinitionTenantId", processInstanceMigrationDocument.getMigrateToProcessDefinitionTenantId());
        }

        return processDefinitionNode;
    }

    protected static ArrayNode convertToJsonActivityMigrationMappings(List<? extends ProcessInstanceActivityMigrationMapping> activityMigrationMappings, ObjectMapper objectMapper) {
        ArrayNode mappingsArray = objectMapper.createArrayNode();

        for (ProcessInstanceActivityMigrationMapping mapping : activityMigrationMappings) {
            BaseActivityMigrationMappingConverter mappingConverter = activityMigrationMappingConverters.get(mapping.getClass());
            if (mappingConverter == null) {
                throw new FlowableException("Cannot convert mapping of type '" + mapping.getClass() + "'");
            }
            ObjectNode mappingNode = mappingConverter.convertToJson(mapping, objectMapper);
            mappingsArray.add(mappingNode);
        }

        return mappingsArray;
    }

    protected static JsonNode convertToJsonProcessInstanceVariables(Map<String, Object> variables, ObjectMapper objectMapper) {
        return objectMapper.valueToTree(variables);
    }

    public static abstract class BaseActivityMigrationMappingConverter<T extends ProcessInstanceActivityMigrationMapping> {

        public ObjectNode convertToJson(T mapping, ObjectMapper objectMapper) {
            ObjectNode mappingNode = convertMappingInfoToJson(mapping, objectMapper);

            JsonNode newAssigneeToJson = convertNewAssigneeToJson(mapping, objectMapper);
            if (newAssigneeToJson != null && !newAssigneeToJson.isNull()) {
                mappingNode.set("withNewAssignee", newAssigneeToJson);
            }

            JsonNode variablesToJson = convertLocalVariablesToJson(mapping, objectMapper);
            if (variablesToJson != null && !variablesToJson.isNull()) {
                mappingNode.set("withLocalVariables", variablesToJson);
            }

            return mappingNode;
        }

        protected abstract ObjectNode convertMappingInfoToJson(T mapping, ObjectMapper objectMapper);

        protected abstract JsonNode convertLocalVariablesToJson(T mapping, ObjectMapper objectMapper);

        protected JsonNode convertNewAssigneeToJson(T mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewAssignee());
        }

        public abstract T convertFromJson(JsonNode jsonNode, ObjectMapper objectMapper);

        protected String getNewAssigneeFromJson(JsonNode jsonNode) {
            if (isSingleTextValue.test(jsonNode.get("withNewAssignee"))) {
                return jsonNode.get("withNewAssignee").textValue();
            }
            return null;
        }

    }

    public static class OneToOneMappingConverter extends BaseActivityMigrationMappingConverter<ProcessInstanceActivityMigrationMapping.OneToOneMapping> {

        @Override
        protected ObjectNode convertMappingInfoToJson(ProcessInstanceActivityMigrationMapping.OneToOneMapping mapping, ObjectMapper objectMapper) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            mappingNode.put("fromActivityId", mapping.getFromActivityId());
            mappingNode.put("toActivityId", mapping.getToActivityId());
            return mappingNode;
        }

        @Override
        public JsonNode convertLocalVariablesToJson(ProcessInstanceActivityMigrationMapping.OneToOneMapping mapping, ObjectMapper objectMapper) {
            Map<String, Object> activityLocalVariables = mapping.getActivityLocalVariables();
            return objectMapper.valueToTree(activityLocalVariables);
        }

        @Override
        public ProcessInstanceActivityMigrationMapping.OneToOneMapping convertFromJson(JsonNode jsonNode, ObjectMapper objectMapper) {
            String fromActivityId = jsonNode.get("fromActivityId").textValue();
            String toActivityId = jsonNode.get("toActivityId").textValue();

            ProcessInstanceActivityMigrationMapping.OneToOneMapping oneToOneMapping = ProcessInstanceActivityMigrationMapping.createMappingFor(fromActivityId, toActivityId);

            Optional.ofNullable(getNewAssigneeFromJson(jsonNode)).ifPresent(oneToOneMapping::withNewAssignee);
            Optional<JsonNode> withLocalVariablesNode = Optional.ofNullable(jsonNode.get("withLocalVariables"));
            if (withLocalVariablesNode.isPresent()) {
                Map<String, Object> localVariables = ProcessInstanceMigrationDocumentConverter.convertJsonNode(objectMapper, withLocalVariablesNode.get());
                oneToOneMapping.withLocalVariables(localVariables);
            }

            return oneToOneMapping;
        }

    }

    public static class ManyToOneMappingConverter extends BaseActivityMigrationMappingConverter<ProcessInstanceActivityMigrationMapping.ManyToOneMapping> {

        @Override
        protected ObjectNode convertMappingInfoToJson(ProcessInstanceActivityMigrationMapping.ManyToOneMapping mapping, ObjectMapper objectMapper) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            JsonNode fromActivityIdsNode = objectMapper.valueToTree(mapping.getFromActivityIds());
            mappingNode.set("fromActivityIds", fromActivityIdsNode);
            mappingNode.put("toActivityId", mapping.getToActivityId());
            return mappingNode;
        }

        @Override
        public JsonNode convertLocalVariablesToJson(ProcessInstanceActivityMigrationMapping.ManyToOneMapping mapping, ObjectMapper objectMapper) {
            Map<String, Object> activityLocalVariables = mapping.getActivityLocalVariables();
            return objectMapper.valueToTree(activityLocalVariables);
        }

        @Override
        public ProcessInstanceActivityMigrationMapping.ManyToOneMapping convertFromJson(JsonNode jsonNode, ObjectMapper objectMapper) {
            JsonNode fromActivityIdsNode = jsonNode.get("fromActivityIds");
            List<String> fromActivityIds = objectMapper.convertValue(fromActivityIdsNode, new TypeReference<List<String>>() {

            });
            String toActivityId = jsonNode.get("toActivityId").textValue();

            ProcessInstanceActivityMigrationMapping.ManyToOneMapping manyToOneMapping = ProcessInstanceActivityMigrationMapping.createMappingFor(fromActivityIds, toActivityId);

            Optional.ofNullable(getNewAssigneeFromJson(jsonNode)).ifPresent(manyToOneMapping::withNewAssignee);
            Optional<JsonNode> withLocalVariablesNode = Optional.ofNullable(jsonNode.get("withLocalVariables"));
            if (withLocalVariablesNode.isPresent()) {
                Map<String, Object> localVariables = ProcessInstanceMigrationDocumentConverter.convertJsonNode(objectMapper, withLocalVariablesNode.get());
                manyToOneMapping.withLocalVariables(localVariables);
            }

            return manyToOneMapping;
        }
    }

    public static class OneToManyMappingConverter extends BaseActivityMigrationMappingConverter<ProcessInstanceActivityMigrationMapping.OneToManyMapping> {

        @Override
        protected ObjectNode convertMappingInfoToJson(ProcessInstanceActivityMigrationMapping.OneToManyMapping mapping, ObjectMapper objectMapper) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            mappingNode.put("fromActivityId", mapping.getFromActivityId());
            JsonNode toActivityIdsNode = objectMapper.valueToTree(mapping.getToActivityIds());
            mappingNode.set("toActivityIds", toActivityIdsNode);
            return mappingNode;
        }

        @Override
        public JsonNode convertLocalVariablesToJson(ProcessInstanceActivityMigrationMapping.OneToManyMapping mapping, ObjectMapper objectMapper) {
            Map<String, Map<String, Object>> activitiesLocalVariables = mapping.getActivitiesLocalVariables();
            return objectMapper.valueToTree(activitiesLocalVariables);
        }

        @Override
        public ProcessInstanceActivityMigrationMapping.OneToManyMapping convertFromJson(JsonNode jsonNode, ObjectMapper objectMapper) {
            String fromActivityId = jsonNode.get("fromActivityId").textValue();
            JsonNode toActivityIdsNode = jsonNode.get("toActivityIds");
            List<String> toActivityIds = objectMapper.convertValue(toActivityIdsNode, new TypeReference<List<String>>() {

            });

            ProcessInstanceActivityMigrationMapping.OneToManyMapping oneToManyMapping = ProcessInstanceActivityMigrationMapping.createMappingFor(fromActivityId, toActivityIds);

            Optional.ofNullable(getNewAssigneeFromJson(jsonNode)).ifPresent(oneToManyMapping::withNewAssignee);
            Optional<JsonNode> withLocalVariablesNode = Optional.ofNullable(jsonNode.get("withLocalVariables"));
            if (withLocalVariablesNode.isPresent()) {
                Map<String, Map<String, Object>> localVariables = ProcessInstanceMigrationDocumentConverter.convertJsonNode(objectMapper, withLocalVariablesNode.get());
                oneToManyMapping.withLocalVariables(localVariables);
            }

            return oneToManyMapping;
        }
    }

}

