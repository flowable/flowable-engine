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

import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.ACTIVITY_MAPPINGS_JSON_SECTION;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.FROM_ACTIVITY_IDS_JSON_PROPERTY;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.FROM_ACTIVITY_ID_JSON_PROPERTY;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.LOCAL_VARIABLES_JSON_SECTION;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.NEW_ASSIGNEE_JSON_PROPERTY;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.PROCESS_INSTANCE_VARIABLES_JSON_SECTION;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.TO_ACTIVITY_IDS_JSON_PROPERTY;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.TO_ACTIVITY_ID_JSON_PROPERTY;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.TO_PROCESS_DEFINITION_ID_JSON_PROPERTY;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.TO_PROCESS_DEFINITION_KEY_JSON_PROPERTY;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.TO_PROCESS_DEFINITION_TENANT_ID_JSON_PROPERTY;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.TO_PROCESS_DEFINITION_VERSION_JSON_PROPERTY;

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

    protected static ObjectMapper objectMapper = new ObjectMapper();

    protected static Map<Class<? extends ActivityMigrationMapping>, BaseActivityMigrationMappingConverter> activityMigrationMappingConverters = new HashMap<>();

    static {
        activityMigrationMappingConverters.put(ActivityMigrationMapping.OneToOneMapping.class, new OneToOneMappingConverter());
        activityMigrationMappingConverters.put(ActivityMigrationMapping.OneToManyMapping.class, new OneToManyMappingConverter());
        activityMigrationMappingConverters.put(ActivityMigrationMapping.ManyToOneMapping.class, new ManyToOneMappingConverter());
    }

    protected static <T> T convertFromJsonNodeToObject(ObjectMapper objectMapper, JsonNode jsonNode) {
        return objectMapper.convertValue(jsonNode, new TypeReference<T>() {

        });
    }

    public static JsonNode convertToJson(ProcessInstanceMigrationDocument processInstanceMigrationDocument) {

        ObjectNode documentNode = objectMapper.createObjectNode();

        if (processInstanceMigrationDocument.getMigrateToProcessDefinitionId() != null) {
            documentNode.put(TO_PROCESS_DEFINITION_ID_JSON_PROPERTY, processInstanceMigrationDocument.getMigrateToProcessDefinitionId());
        }

        if (processInstanceMigrationDocument.getMigrateToProcessDefinitionKey() != null) {
            documentNode.put(TO_PROCESS_DEFINITION_KEY_JSON_PROPERTY, processInstanceMigrationDocument.getMigrateToProcessDefinitionKey());
        }

        if (processInstanceMigrationDocument.getMigrateToProcessDefinitionVersion() != null) {
            documentNode.put(TO_PROCESS_DEFINITION_VERSION_JSON_PROPERTY, processInstanceMigrationDocument.getMigrateToProcessDefinitionVersion());
        }

        if (processInstanceMigrationDocument.getMigrateToProcessDefinitionTenantId() != null) {
            documentNode.put(TO_PROCESS_DEFINITION_TENANT_ID_JSON_PROPERTY, processInstanceMigrationDocument.getMigrateToProcessDefinitionTenantId());
        }

        ArrayNode mappingNodes = convertToJsonActivityMigrationMappings(processInstanceMigrationDocument.getActivityMigrationMappings());
        if (mappingNodes != null && !mappingNodes.isNull()) {
            documentNode.set(ACTIVITY_MAPPINGS_JSON_SECTION, mappingNodes);
        }

        JsonNode processInstanceVariablesNode = convertToJsonProcessInstanceVariables(processInstanceMigrationDocument.getProcessInstanceVariables());
        if (processInstanceVariablesNode != null && !processInstanceVariablesNode.isNull()) {
            documentNode.set(PROCESS_INSTANCE_VARIABLES_JSON_SECTION, processInstanceVariablesNode);
        }

        return documentNode;
    }

    public static ProcessInstanceMigrationDocument convertFromJson(String jsonProcessInstanceMigrationDocument) {

        try {
            JsonNode rootNode = objectMapper.readTree(jsonProcessInstanceMigrationDocument);
            ProcessInstanceMigrationDocumentBuilderImpl documentBuilder = new ProcessInstanceMigrationDocumentBuilderImpl();

            String processDefinitionId = Optional.ofNullable(rootNode.get(TO_PROCESS_DEFINITION_ID_JSON_PROPERTY))
                .map(JsonNode::textValue).orElse(null);
            documentBuilder.setProcessDefinitionToMigrateTo(processDefinitionId);

            String processDefinitionKey = Optional.ofNullable(rootNode.get(TO_PROCESS_DEFINITION_KEY_JSON_PROPERTY))
                .map(JsonNode::textValue).orElse(null);
            Integer processDefinitionVersion = (Integer) Optional.ofNullable(rootNode.get(TO_PROCESS_DEFINITION_VERSION_JSON_PROPERTY))
                .map(JsonNode::numberValue).orElse(null);
            documentBuilder.setProcessDefinitionToMigrateTo(processDefinitionKey, processDefinitionVersion);

            String processDefinitionTenantId = Optional.ofNullable(rootNode.get(TO_PROCESS_DEFINITION_TENANT_ID_JSON_PROPERTY))
                .map(JsonNode::textValue).orElse(null);
            documentBuilder.setTenantId(processDefinitionTenantId);

            JsonNode activityMigrationMappings = rootNode.get(ACTIVITY_MAPPINGS_JSON_SECTION);
            if (activityMigrationMappings != null) {
                Iterator<JsonNode> mappingNodes = activityMigrationMappings.iterator();

                while (mappingNodes.hasNext()) {
                    JsonNode mappingNode = mappingNodes.next();

                    Class<? extends ActivityMigrationMapping> mappingClass = null;
                    if (isSingleTextValue.test(mappingNode.get(FROM_ACTIVITY_ID_JSON_PROPERTY)) && isSingleTextValue.test(mappingNode.get(TO_ACTIVITY_ID_JSON_PROPERTY))) {
                        mappingClass = ActivityMigrationMapping.OneToOneMapping.class;
                    }
                    if (isSingleTextValue.test(mappingNode.get(FROM_ACTIVITY_ID_JSON_PROPERTY)) && isMultiValue.test(mappingNode.get(TO_ACTIVITY_IDS_JSON_PROPERTY))) {
                        mappingClass = ActivityMigrationMapping.OneToManyMapping.class;
                    }
                    if (isMultiValue.test(mappingNode.get(FROM_ACTIVITY_IDS_JSON_PROPERTY)) && isSingleTextValue.test(mappingNode.get(TO_ACTIVITY_ID_JSON_PROPERTY))) {
                        mappingClass = ActivityMigrationMapping.ManyToOneMapping.class;
                    }

                    BaseActivityMigrationMappingConverter mappingConverter = activityMigrationMappingConverters.get(mappingClass);
                    ActivityMigrationMapping mapping = mappingConverter.convertFromJson(mappingNode, objectMapper);
                    documentBuilder.addActivityMigrationMapping(mapping);
                }
            }

            JsonNode processInstanceVariablesNode = rootNode.get(PROCESS_INSTANCE_VARIABLES_JSON_SECTION);
            if (processInstanceVariablesNode != null) {
                Map<String, Object> processInstanceVariables = ProcessInstanceMigrationDocumentConverter.convertFromJsonNodeToObject(objectMapper, processInstanceVariablesNode);
                documentBuilder.addProcessInstanceVariables(processInstanceVariables);
            }
            return documentBuilder.build();

        } catch (IOException e) {
            throw new FlowableException("Error parsing Process Instance Migration Document", e);
        }

    }

    protected static ArrayNode convertToJsonActivityMigrationMappings(List<? extends ActivityMigrationMapping> activityMigrationMappings) {
        ArrayNode mappingsArray = objectMapper.createArrayNode();

        for (ActivityMigrationMapping mapping : activityMigrationMappings) {
            BaseActivityMigrationMappingConverter mappingConverter = activityMigrationMappingConverters.get(mapping.getClass());
            if (mappingConverter == null) {
                throw new FlowableException("Cannot convert mapping of type '" + mapping.getClass() + "'");
            }
            ObjectNode mappingNode = mappingConverter.convertToJson(mapping, objectMapper);
            mappingsArray.add(mappingNode);
        }

        return mappingsArray;
    }

    protected static JsonNode convertToJsonProcessInstanceVariables(Map<String, Object> variables) {
        return objectMapper.valueToTree(variables);
    }

    public static abstract class BaseActivityMigrationMappingConverter<T extends ActivityMigrationMapping> {

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

        protected abstract JsonNode convertLocalVariablesToJson(T mapping, ObjectMapper objectMapper);

        protected abstract JsonNode convertNewAssigneeToJson(T mapping, ObjectMapper objectMapper);

        public abstract T convertFromJson(JsonNode jsonNode, ObjectMapper objectMapper);

        protected String getNewAssigneeFromJson(JsonNode jsonNode) {
            if (isSingleTextValue.test(jsonNode.get(NEW_ASSIGNEE_JSON_PROPERTY))) {
                return jsonNode.get(NEW_ASSIGNEE_JSON_PROPERTY).textValue();
            }
            return null;
        }

    }

    public static class OneToOneMappingConverter extends BaseActivityMigrationMappingConverter<ActivityMigrationMapping.OneToOneMapping> {

        @Override
        protected ObjectNode convertMappingInfoToJson(ActivityMigrationMapping.OneToOneMapping mapping, ObjectMapper objectMapper) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            mappingNode.put(FROM_ACTIVITY_ID_JSON_PROPERTY, mapping.getFromActivityId());
            mappingNode.put(TO_ACTIVITY_ID_JSON_PROPERTY, mapping.getToActivityId());
            return mappingNode;
        }

        @Override
        public JsonNode convertLocalVariablesToJson(ActivityMigrationMapping.OneToOneMapping mapping, ObjectMapper objectMapper) {
            Map<String, Object> activityLocalVariables = mapping.getActivityLocalVariables();
            return objectMapper.valueToTree(activityLocalVariables);
        }

        @Override
        protected JsonNode convertNewAssigneeToJson(ActivityMigrationMapping.OneToOneMapping mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewAssignee());
        }

        @Override
        public ActivityMigrationMapping.OneToOneMapping convertFromJson(JsonNode jsonNode, ObjectMapper objectMapper) {
            String fromActivityId = jsonNode.get(FROM_ACTIVITY_ID_JSON_PROPERTY).textValue();
            String toActivityId = jsonNode.get(TO_ACTIVITY_ID_JSON_PROPERTY).textValue();

            ActivityMigrationMapping.OneToOneMapping oneToOneMapping = ActivityMigrationMapping.createMappingFor(fromActivityId, toActivityId);

            Optional.ofNullable(getNewAssigneeFromJson(jsonNode)).ifPresent(oneToOneMapping::withNewAssignee);
            Optional<JsonNode> withLocalVariablesNode = Optional.ofNullable(jsonNode.get(LOCAL_VARIABLES_JSON_SECTION));
            if (withLocalVariablesNode.isPresent()) {
                Map<String, Object> localVariables = ProcessInstanceMigrationDocumentConverter.convertFromJsonNodeToObject(objectMapper, withLocalVariablesNode.get());
                oneToOneMapping.withLocalVariables(localVariables);
            }

            return oneToOneMapping;
        }

    }

    public static class ManyToOneMappingConverter extends BaseActivityMigrationMappingConverter<ActivityMigrationMapping.ManyToOneMapping> {

        @Override
        protected ObjectNode convertMappingInfoToJson(ActivityMigrationMapping.ManyToOneMapping mapping, ObjectMapper objectMapper) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            JsonNode fromActivityIdsNode = objectMapper.valueToTree(mapping.getFromActivityIds());
            mappingNode.set(FROM_ACTIVITY_IDS_JSON_PROPERTY, fromActivityIdsNode);
            mappingNode.put(TO_ACTIVITY_ID_JSON_PROPERTY, mapping.getToActivityId());
            return mappingNode;
        }

        @Override
        public JsonNode convertLocalVariablesToJson(ActivityMigrationMapping.ManyToOneMapping mapping, ObjectMapper objectMapper) {
            Map<String, Object> activityLocalVariables = mapping.getActivityLocalVariables();
            return objectMapper.valueToTree(activityLocalVariables);
        }

        @Override
        protected JsonNode convertNewAssigneeToJson(ActivityMigrationMapping.ManyToOneMapping mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewAssignee());
        }

        @Override
        public ActivityMigrationMapping.ManyToOneMapping convertFromJson(JsonNode jsonNode, ObjectMapper objectMapper) {
            JsonNode fromActivityIdsNode = jsonNode.get(FROM_ACTIVITY_IDS_JSON_PROPERTY);
            List<String> fromActivityIds = objectMapper.convertValue(fromActivityIdsNode, new TypeReference<List<String>>() {

            });
            String toActivityId = jsonNode.get(TO_ACTIVITY_ID_JSON_PROPERTY).textValue();

            ActivityMigrationMapping.ManyToOneMapping manyToOneMapping = ActivityMigrationMapping.createMappingFor(fromActivityIds, toActivityId);

            Optional.ofNullable(getNewAssigneeFromJson(jsonNode)).ifPresent(manyToOneMapping::withNewAssignee);
            Optional<JsonNode> withLocalVariablesNode = Optional.ofNullable(jsonNode.get(LOCAL_VARIABLES_JSON_SECTION));
            if (withLocalVariablesNode.isPresent()) {
                Map<String, Object> localVariables = ProcessInstanceMigrationDocumentConverter.convertFromJsonNodeToObject(objectMapper, withLocalVariablesNode.get());
                manyToOneMapping.withLocalVariables(localVariables);
            }

            return manyToOneMapping;
        }
    }

    public static class OneToManyMappingConverter extends BaseActivityMigrationMappingConverter<ActivityMigrationMapping.OneToManyMapping> {

        @Override
        protected ObjectNode convertMappingInfoToJson(ActivityMigrationMapping.OneToManyMapping mapping, ObjectMapper objectMapper) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            mappingNode.put(FROM_ACTIVITY_ID_JSON_PROPERTY, mapping.getFromActivityId());
            JsonNode toActivityIdsNode = objectMapper.valueToTree(mapping.getToActivityIds());
            mappingNode.set(TO_ACTIVITY_IDS_JSON_PROPERTY, toActivityIdsNode);
            return mappingNode;
        }

        @Override
        public JsonNode convertLocalVariablesToJson(ActivityMigrationMapping.OneToManyMapping mapping, ObjectMapper objectMapper) {
            Map<String, Map<String, Object>> activitiesLocalVariables = mapping.getActivitiesLocalVariables();
            return objectMapper.valueToTree(activitiesLocalVariables);
        }

        @Override
        protected JsonNode convertNewAssigneeToJson(ActivityMigrationMapping.OneToManyMapping mapping, ObjectMapper objectMapper) {
            return null;
        }

        @Override
        public ActivityMigrationMapping.OneToManyMapping convertFromJson(JsonNode jsonNode, ObjectMapper objectMapper) {
            String fromActivityId = jsonNode.get(FROM_ACTIVITY_ID_JSON_PROPERTY).textValue();
            JsonNode toActivityIdsNode = jsonNode.get(TO_ACTIVITY_IDS_JSON_PROPERTY);
            List<String> toActivityIds = objectMapper.convertValue(toActivityIdsNode, new TypeReference<List<String>>() {

            });

            ActivityMigrationMapping.OneToManyMapping oneToManyMapping = ActivityMigrationMapping.createMappingFor(fromActivityId, toActivityIds);

            Optional<JsonNode> withLocalVariablesNode = Optional.ofNullable(jsonNode.get(LOCAL_VARIABLES_JSON_SECTION));
            if (withLocalVariablesNode.isPresent()) {
                Map<String, Map<String, Object>> localVariables = ProcessInstanceMigrationDocumentConverter.convertFromJsonNodeToObject(objectMapper, withLocalVariablesNode.get());
                oneToManyMapping.withLocalVariables(localVariables);
            }

            return oneToManyMapping;
        }
    }
}

