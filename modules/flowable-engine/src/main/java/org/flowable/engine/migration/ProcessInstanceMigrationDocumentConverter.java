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
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.CALL_ACTIVITY_PROCESS_DEFINITION_VERSION_JSON_PROPERTY;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.FROM_ACTIVITY_IDS_JSON_PROPERTY;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.FROM_ACTIVITY_ID_JSON_PROPERTY;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.IN_PARENT_PROCESS_OF_CALL_ACTIVITY_JSON_PROPERTY;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.IN_SUB_PROCESS_OF_CALL_ACTIVITY_ID_JSON_PROPERTY;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.LANGUAGE;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.LOCAL_VARIABLES_JSON_SECTION;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.NEW_ASSIGNEE_JSON_PROPERTY;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.POST_UPGRADE_JAVA_DELEGATE;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.POST_UPGRADE_JAVA_DELEGATE_EXPRESSION;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.POST_UPGRADE_SCRIPT;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.PRE_UPGRADE_JAVA_DELEGATE;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.PRE_UPGRADE_JAVA_DELEGATE_EXPRESSION;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.PRE_UPGRADE_SCRIPT;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.PROCESS_INSTANCE_VARIABLES_JSON_SECTION;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.SCRIPT;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.TO_ACTIVITY_IDS_JSON_PROPERTY;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.TO_ACTIVITY_ID_JSON_PROPERTY;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.TO_PROCESS_DEFINITION_ID_JSON_PROPERTY;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.TO_PROCESS_DEFINITION_KEY_JSON_PROPERTY;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.TO_PROCESS_DEFINITION_TENANT_ID_JSON_PROPERTY;
import static org.flowable.engine.migration.ProcessInstanceMigrationDocumentConstants.TO_PROCESS_DEFINITION_VERSION_JSON_PROPERTY;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.migration.ProcessInstanceMigrationDocumentBuilderImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Dennis
 * @author martin.grofcik
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

    protected static <T> T convertFromJsonNodeToObject(JsonNode jsonNode, ObjectMapper objectMapper) {
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

        JsonNode preUpgradeScriptNode = convertToJsonUpgradeScript(processInstanceMigrationDocument.getPreUpgradeScript(), objectMapper);
        if (preUpgradeScriptNode != null && !preUpgradeScriptNode.isNull()) {
            documentNode.set(PRE_UPGRADE_SCRIPT, preUpgradeScriptNode);
        }

        if (processInstanceMigrationDocument.getPreUpgradeJavaDelegate() != null) {
            documentNode.put(PRE_UPGRADE_JAVA_DELEGATE, processInstanceMigrationDocument.getPreUpgradeJavaDelegate());
        }

        if (processInstanceMigrationDocument.getPreUpgradeJavaDelegateExpression() != null) {
            documentNode.put(PRE_UPGRADE_JAVA_DELEGATE_EXPRESSION, processInstanceMigrationDocument.getPreUpgradeJavaDelegateExpression());
        }

        JsonNode postUpgradeScriptNode = convertToJsonUpgradeScript(processInstanceMigrationDocument.getPostUpgradeScript(), objectMapper);
        if (postUpgradeScriptNode != null && !postUpgradeScriptNode.isNull()) {
            documentNode.set(POST_UPGRADE_SCRIPT, postUpgradeScriptNode);
        }

        if (processInstanceMigrationDocument.getPostUpgradeJavaDelegate() != null) {
            documentNode.put(POST_UPGRADE_JAVA_DELEGATE, processInstanceMigrationDocument.getPostUpgradeJavaDelegate());
        }

        if (processInstanceMigrationDocument.getPostUpgradeJavaDelegateExpression() != null) {
            documentNode.put(POST_UPGRADE_JAVA_DELEGATE_EXPRESSION, processInstanceMigrationDocument.getPostUpgradeJavaDelegateExpression());
        }

        ArrayNode mappingNodes = convertToJsonActivityMigrationMappings(processInstanceMigrationDocument.getActivityMigrationMappings());
        if (mappingNodes != null && !mappingNodes.isNull()) {
            documentNode.set(ACTIVITY_MAPPINGS_JSON_SECTION, mappingNodes);
        }

        JsonNode processInstanceVariablesNode = convertToJsonProcessInstanceVariables(processInstanceMigrationDocument, objectMapper);
        if (processInstanceVariablesNode != null && !processInstanceVariablesNode.isNull()) {
            documentNode.set(PROCESS_INSTANCE_VARIABLES_JSON_SECTION, processInstanceVariablesNode);
        }

        return documentNode;
    }

    public static String convertToJsonString(ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        JsonNode jsonNode = convertToJson(processInstanceMigrationDocument);
        ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
        try {
            return objectWriter.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            return jsonNode.toString();
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

            JsonNode preUpgradeScriptNode = rootNode.get(PRE_UPGRADE_SCRIPT);
            if (preUpgradeScriptNode != null) {
                String language = Optional.ofNullable(preUpgradeScriptNode.get(LANGUAGE)).map(JsonNode::asText).orElse("javascript");
                String script = Optional.ofNullable(preUpgradeScriptNode.get(SCRIPT)).map(JsonNode::asText).orElse("javascript");
                documentBuilder.setPreUpgradeScript(new Script(language, script));
            }

            String javaDelegateClassName = Optional.ofNullable(rootNode.get(PRE_UPGRADE_JAVA_DELEGATE))
                .map(JsonNode::textValue).orElse(null);
            documentBuilder.setPreUpgradeJavaDelegate(javaDelegateClassName);

            String expression = Optional.ofNullable(rootNode.get(PRE_UPGRADE_JAVA_DELEGATE_EXPRESSION))
                .map(JsonNode::textValue).orElse(null);
            documentBuilder.setPreUpgradeJavaDelegateExpression(expression);

            JsonNode postUpgradeScriptNode = rootNode.get(POST_UPGRADE_SCRIPT);
            if (postUpgradeScriptNode != null) {
                String language = Optional.ofNullable(postUpgradeScriptNode.get(LANGUAGE)).map(JsonNode::asText).orElse("javascript");
                String script = Optional.ofNullable(postUpgradeScriptNode.get(SCRIPT)).map(JsonNode::asText).orElse("javascript");
                documentBuilder.setPostUpgradeScript(new Script(language, script));
            }

            String postJavaDelegateClassName = Optional.ofNullable(rootNode.get(POST_UPGRADE_JAVA_DELEGATE))
                .map(JsonNode::textValue).orElse(null);
            documentBuilder.setPostUpgradeJavaDelegate(postJavaDelegateClassName);

            String postExpression = Optional.ofNullable(rootNode.get(POST_UPGRADE_JAVA_DELEGATE_EXPRESSION))
                .map(JsonNode::textValue).orElse(null);
            documentBuilder.setPostUpgradeJavaDelegateExpression(postExpression);

            JsonNode activityMigrationMappings = rootNode.get(ACTIVITY_MAPPINGS_JSON_SECTION);
            if (activityMigrationMappings != null) {

                for (JsonNode mappingNode : activityMigrationMappings) {
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
                Map<String, Object> processInstanceVariables = ProcessInstanceMigrationDocumentConverter.convertFromJsonNodeToObject(processInstanceVariablesNode, objectMapper);
                documentBuilder.addProcessInstanceVariables(processInstanceVariables);
            }
            return documentBuilder.build();

        } catch (IOException e) {
            throw new FlowableException("Error parsing Process Instance Migration Document", e);
        }

    }

    protected static JsonNode convertToJsonProcessInstanceVariables(ProcessInstanceMigrationDocument processInstanceMigrationDocument, ObjectMapper objectMapper) {
        Map<String, Object> processInstanceVariables = processInstanceMigrationDocument.getProcessInstanceVariables();
        if (processInstanceVariables != null && !processInstanceVariables.isEmpty()) {
            return objectMapper.valueToTree(processInstanceVariables);
        }
        return null;
    }

    protected static JsonNode convertToJsonUpgradeScript(Script script, ObjectMapper objectMapper) {
        if (script != null) {
            return objectMapper.valueToTree(script);
        }
        return null;
    }

    public abstract static class BaseActivityMigrationMappingConverter<T extends ActivityMigrationMapping> {

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
            if (mapping.isToParentProcess()) {
                mappingNode.put(IN_PARENT_PROCESS_OF_CALL_ACTIVITY_JSON_PROPERTY, mapping.getFromCallActivityId());
            }
            if (mapping.isToCallActivity()) {
                mappingNode.put(IN_SUB_PROCESS_OF_CALL_ACTIVITY_ID_JSON_PROPERTY, mapping.getToCallActivityId());
                mappingNode.put(CALL_ACTIVITY_PROCESS_DEFINITION_VERSION_JSON_PROPERTY, mapping.getCallActivityProcessDefinitionVersion());
            }
            return mappingNode;
        }

        protected abstract JsonNode convertLocalVariablesToJson(T mapping, ObjectMapper objectMapper);

        protected abstract JsonNode convertNewAssigneeToJson(T mapping, ObjectMapper objectMapper);

        public abstract T convertFromJson(JsonNode jsonNode, ObjectMapper objectMapper);

        protected <M extends ActivityMigrationMappingOptions<T>> void convertAdditionalMappingInfoFromJson(M mapping, JsonNode jsonNode) {
            Optional<JsonNode> callActivityOfParentProcess = Optional.ofNullable(jsonNode.get(IN_PARENT_PROCESS_OF_CALL_ACTIVITY_JSON_PROPERTY));
            if (callActivityOfParentProcess.isPresent()) {
                callActivityOfParentProcess.map(JsonNode::textValue).ifPresent(mapping::inParentProcessOfCallActivityId);
                return; //if its a move to parent, it cannot be also a move to subProcess
            }

            Optional<JsonNode> ofCallActivityId = Optional.ofNullable(jsonNode.get(IN_SUB_PROCESS_OF_CALL_ACTIVITY_ID_JSON_PROPERTY));
            Optional<JsonNode> subProcDefVer = Optional.ofNullable(jsonNode.get(CALL_ACTIVITY_PROCESS_DEFINITION_VERSION_JSON_PROPERTY));
            if (ofCallActivityId.isPresent()) {
                if (subProcDefVer.isPresent()) {
                    mapping.inSubProcessOfCallActivityId(ofCallActivityId.get().textValue(), subProcDefVer.get().intValue());
                } else {
                    mapping.inSubProcessOfCallActivityId(ofCallActivityId.get().textValue());
                }
            }

        }

        protected <V> V getLocalVariablesFromJson(JsonNode jsonNode, ObjectMapper objectMapper) {
            JsonNode localVariablesNode = jsonNode.get(LOCAL_VARIABLES_JSON_SECTION);
            if (localVariablesNode != null) {
                return ProcessInstanceMigrationDocumentConverter.convertFromJsonNodeToObject(localVariablesNode, objectMapper);
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

    public static class OneToOneMappingConverter extends BaseActivityMigrationMappingConverter<ActivityMigrationMapping.OneToOneMapping> {

        @Override
        protected ObjectNode convertMappingInfoToJson(ActivityMigrationMapping.OneToOneMapping mapping, ObjectMapper objectMapper) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            mappingNode.put(FROM_ACTIVITY_ID_JSON_PROPERTY, mapping.getFromActivityId());
            mappingNode.put(TO_ACTIVITY_ID_JSON_PROPERTY, mapping.getToActivityId());
            mappingNode.setAll(convertAdditionalMappingInfoToJson(mapping, objectMapper));
            return mappingNode;
        }

        @Override
        public JsonNode convertLocalVariablesToJson(ActivityMigrationMapping.OneToOneMapping mapping, ObjectMapper objectMapper) {
            Map<String, Object> activityLocalVariables = mapping.getActivityLocalVariables();
            if (activityLocalVariables != null && !activityLocalVariables.isEmpty()) {
                return objectMapper.valueToTree(activityLocalVariables);
            }
            return null;
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

    public static class ManyToOneMappingConverter extends BaseActivityMigrationMappingConverter<ActivityMigrationMapping.ManyToOneMapping> {

        @Override
        protected ObjectNode convertMappingInfoToJson(ActivityMigrationMapping.ManyToOneMapping mapping, ObjectMapper objectMapper) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            JsonNode fromActivityIdsNode = objectMapper.valueToTree(mapping.getFromActivityIds());
            mappingNode.set(FROM_ACTIVITY_IDS_JSON_PROPERTY, fromActivityIdsNode);
            mappingNode.put(TO_ACTIVITY_ID_JSON_PROPERTY, mapping.getToActivityId());
            mappingNode.setAll(convertAdditionalMappingInfoToJson(mapping, objectMapper));
            return mappingNode;
        }

        @Override
        public JsonNode convertLocalVariablesToJson(ActivityMigrationMapping.ManyToOneMapping mapping, ObjectMapper objectMapper) {
            Map<String, Object> activityLocalVariables = mapping.getActivityLocalVariables();
            if (activityLocalVariables != null && !activityLocalVariables.isEmpty()) {
                return objectMapper.valueToTree(activityLocalVariables);
            }
            return null;
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

    public static class OneToManyMappingConverter extends BaseActivityMigrationMappingConverter<ActivityMigrationMapping.OneToManyMapping> {

        @Override
        protected ObjectNode convertMappingInfoToJson(ActivityMigrationMapping.OneToManyMapping mapping, ObjectMapper objectMapper) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            mappingNode.put(FROM_ACTIVITY_ID_JSON_PROPERTY, mapping.getFromActivityId());
            JsonNode toActivityIdsNode = objectMapper.valueToTree(mapping.getToActivityIds());
            mappingNode.set(TO_ACTIVITY_IDS_JSON_PROPERTY, toActivityIdsNode);
            mappingNode.setAll(convertAdditionalMappingInfoToJson(mapping, objectMapper));
            return mappingNode;
        }

        @Override
        public JsonNode convertLocalVariablesToJson(ActivityMigrationMapping.OneToManyMapping mapping, ObjectMapper objectMapper) {
            Map<String, Map<String, Object>> activitiesLocalVariables = mapping.getActivitiesLocalVariables();
            if (activitiesLocalVariables != null && !activitiesLocalVariables.isEmpty()) {
                return objectMapper.valueToTree(activitiesLocalVariables);
            }
            return null;
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
            convertAdditionalMappingInfoFromJson(oneToManyMapping, jsonNode);

            Map<String, Map<String, Object>> localVariables = getLocalVariablesFromJson(jsonNode, objectMapper);
            if (localVariables != null) {
                oneToManyMapping.withLocalVariables(localVariables);
            }

            return oneToManyMapping;
        }
    }
}

