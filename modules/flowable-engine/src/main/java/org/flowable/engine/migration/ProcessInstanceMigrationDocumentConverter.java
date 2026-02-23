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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.migration.ProcessInstanceMigrationDocumentBuilderImpl;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * @author Dennis
 * @author martin.grofcik
 */
public class ProcessInstanceMigrationDocumentConverter {

    protected static Predicate<JsonNode> isNotNullNode = jsonNode -> jsonNode != null && !jsonNode.isNull();
    protected static Predicate<JsonNode> isSingleTextValue = jsonNode -> isNotNullNode.test(jsonNode) && jsonNode.isString();
    protected static Predicate<JsonNode> isMultiValue = jsonNode -> isNotNullNode.test(jsonNode) && jsonNode.isArray();

    protected static ObjectMapper objectMapper = JsonMapper.shared();

    protected static Map<Class<? extends ActivityMigrationMapping>, BaseActivityMigrationMappingConverter> activityMigrationMappingConverters = new HashMap<>();

    static {
        activityMigrationMappingConverters.put(ActivityMigrationMapping.OneToOneMapping.class, new OneToOneMappingConverter());
        activityMigrationMappingConverters.put(ActivityMigrationMapping.OneToManyMapping.class, new OneToManyMappingConverter());
        activityMigrationMappingConverters.put(ActivityMigrationMapping.ManyToOneMapping.class, new ManyToOneMappingConverter());
    }

    protected static <T> T convertFromJsonNodeToObject(JsonNode jsonNode, ObjectMapper objectMapper) {
        return objectMapper.convertValue(jsonNode, new TypeReference<>() {

        });
    }

    public static JsonNode convertToJson(ProcessInstanceMigrationDocument processInstanceMigrationDocument) {

        ObjectNode documentNode = objectMapper.createObjectNode();

        if (processInstanceMigrationDocument.getMigrateToProcessDefinitionId() != null) {
            documentNode.put(ProcessInstanceMigrationDocumentConstants.TO_PROCESS_DEFINITION_ID_JSON_PROPERTY, processInstanceMigrationDocument.getMigrateToProcessDefinitionId());
        }

        if (processInstanceMigrationDocument.getMigrateToProcessDefinitionKey() != null) {
            documentNode.put(ProcessInstanceMigrationDocumentConstants.TO_PROCESS_DEFINITION_KEY_JSON_PROPERTY, processInstanceMigrationDocument.getMigrateToProcessDefinitionKey());
        }

        if (processInstanceMigrationDocument.getMigrateToProcessDefinitionVersion() != null) {
            documentNode.put(ProcessInstanceMigrationDocumentConstants.TO_PROCESS_DEFINITION_VERSION_JSON_PROPERTY, processInstanceMigrationDocument.getMigrateToProcessDefinitionVersion());
        }

        if (processInstanceMigrationDocument.getMigrateToProcessDefinitionTenantId() != null) {
            documentNode.put(ProcessInstanceMigrationDocumentConstants.TO_PROCESS_DEFINITION_TENANT_ID_JSON_PROPERTY, processInstanceMigrationDocument.getMigrateToProcessDefinitionTenantId());
        }

        JsonNode preUpgradeScriptNode = convertToJsonUpgradeScript(processInstanceMigrationDocument.getPreUpgradeScript(), objectMapper);
        if (preUpgradeScriptNode != null && !preUpgradeScriptNode.isNull()) {
            documentNode.set(ProcessInstanceMigrationDocumentConstants.PRE_UPGRADE_SCRIPT, preUpgradeScriptNode);
        }

        if (processInstanceMigrationDocument.getPreUpgradeJavaDelegate() != null) {
            documentNode.put(ProcessInstanceMigrationDocumentConstants.PRE_UPGRADE_JAVA_DELEGATE, processInstanceMigrationDocument.getPreUpgradeJavaDelegate());
        }

        if (processInstanceMigrationDocument.getPreUpgradeJavaDelegateExpression() != null) {
            documentNode.put(ProcessInstanceMigrationDocumentConstants.PRE_UPGRADE_JAVA_DELEGATE_EXPRESSION, processInstanceMigrationDocument.getPreUpgradeJavaDelegateExpression());
        }

        JsonNode postUpgradeScriptNode = convertToJsonUpgradeScript(processInstanceMigrationDocument.getPostUpgradeScript(), objectMapper);
        if (postUpgradeScriptNode != null && !postUpgradeScriptNode.isNull()) {
            documentNode.set(ProcessInstanceMigrationDocumentConstants.POST_UPGRADE_SCRIPT, postUpgradeScriptNode);
        }

        if (processInstanceMigrationDocument.getPostUpgradeJavaDelegate() != null) {
            documentNode.put(ProcessInstanceMigrationDocumentConstants.POST_UPGRADE_JAVA_DELEGATE, processInstanceMigrationDocument.getPostUpgradeJavaDelegate());
        }

        if (processInstanceMigrationDocument.getPostUpgradeJavaDelegateExpression() != null) {
            documentNode.put(ProcessInstanceMigrationDocumentConstants.POST_UPGRADE_JAVA_DELEGATE_EXPRESSION, processInstanceMigrationDocument.getPostUpgradeJavaDelegateExpression());
        }

        ArrayNode mappingNodes = convertToJsonActivityMigrationMappings(processInstanceMigrationDocument.getActivityMigrationMappings());
        if (mappingNodes != null && !mappingNodes.isNull()) {
            documentNode.set(ProcessInstanceMigrationDocumentConstants.ACTIVITY_MAPPINGS_JSON_SECTION, mappingNodes);
        }
        
        ArrayNode enableActivityNodes = convertToJsonEnableActivityMappings(processInstanceMigrationDocument.getEnableActivityMappings());
        if (enableActivityNodes != null && !enableActivityNodes.isNull()) {
            documentNode.set(ProcessInstanceMigrationDocumentConstants.ENABLE_ACTIVITY_MAPPINGS_JSON_SECTION, enableActivityNodes);
        }

        JsonNode processInstanceVariablesNode = convertToJsonProcessInstanceVariables(processInstanceMigrationDocument, objectMapper);
        if (processInstanceVariablesNode != null && !processInstanceVariablesNode.isNull()) {
            documentNode.set(ProcessInstanceMigrationDocumentConstants.PROCESS_INSTANCE_VARIABLES_JSON_SECTION, processInstanceVariablesNode);
        }

        return documentNode;
    }

    public static String convertToJsonString(ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        JsonNode jsonNode = convertToJson(processInstanceMigrationDocument);
        ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
        try {
            return objectWriter.writeValueAsString(jsonNode);
        } catch (JacksonException e) {
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
    
    protected static ArrayNode convertToJsonEnableActivityMappings(List<? extends EnableActivityMapping> enableActivityMappings) {
        ArrayNode mappingsArray = objectMapper.createArrayNode();

        for (EnableActivityMapping mapping : enableActivityMappings) {
            ObjectNode mappingNode = mappingsArray.addObject();
            mappingNode.put(ProcessInstanceMigrationDocumentConstants.ACTIVITY_ID_JSON_PROPERTY, mapping.getActivityId());
        }

        return mappingsArray;
    }

    public static ProcessInstanceMigrationDocument convertFromJson(String jsonProcessInstanceMigrationDocument) {

        try {
            JsonNode rootNode = objectMapper.readTree(jsonProcessInstanceMigrationDocument);
            ProcessInstanceMigrationDocumentBuilderImpl documentBuilder = new ProcessInstanceMigrationDocumentBuilderImpl();

            String processDefinitionId = rootNode.path(ProcessInstanceMigrationDocumentConstants.TO_PROCESS_DEFINITION_ID_JSON_PROPERTY).stringValue(null);
            documentBuilder.setProcessDefinitionToMigrateTo(processDefinitionId);

            String processDefinitionKey = rootNode.path(ProcessInstanceMigrationDocumentConstants.TO_PROCESS_DEFINITION_KEY_JSON_PROPERTY).stringValue(null);
            Integer processDefinitionVersion = (Integer) Optional.ofNullable(rootNode.get(ProcessInstanceMigrationDocumentConstants.TO_PROCESS_DEFINITION_VERSION_JSON_PROPERTY))
                .map(JsonNode::numberValue).orElse(null);
            documentBuilder.setProcessDefinitionToMigrateTo(processDefinitionKey, processDefinitionVersion);

            String processDefinitionTenantId = rootNode.path(ProcessInstanceMigrationDocumentConstants.TO_PROCESS_DEFINITION_TENANT_ID_JSON_PROPERTY)
                    .stringValue(null);
            documentBuilder.setTenantId(processDefinitionTenantId);

            JsonNode preUpgradeScriptNode = rootNode.get(ProcessInstanceMigrationDocumentConstants.PRE_UPGRADE_SCRIPT);
            if (preUpgradeScriptNode != null) {
                String language = preUpgradeScriptNode.path(ProcessInstanceMigrationDocumentConstants.LANGUAGE).stringValue("javascript");
                String script = preUpgradeScriptNode.path(ProcessInstanceMigrationDocumentConstants.SCRIPT).stringValue("javascript");
                documentBuilder.setPreUpgradeScript(new Script(language, script));
            }

            String javaDelegateClassName = rootNode.path(ProcessInstanceMigrationDocumentConstants.PRE_UPGRADE_JAVA_DELEGATE).stringValue(null);
            documentBuilder.setPreUpgradeJavaDelegate(javaDelegateClassName);

            String expression = rootNode.path(ProcessInstanceMigrationDocumentConstants.PRE_UPGRADE_JAVA_DELEGATE_EXPRESSION).stringValue(null);
            documentBuilder.setPreUpgradeJavaDelegateExpression(expression);

            JsonNode postUpgradeScriptNode = rootNode.get(ProcessInstanceMigrationDocumentConstants.POST_UPGRADE_SCRIPT);
            if (postUpgradeScriptNode != null) {
                String language = postUpgradeScriptNode.path(ProcessInstanceMigrationDocumentConstants.LANGUAGE).stringValue("javascript");
                String script = postUpgradeScriptNode.path(ProcessInstanceMigrationDocumentConstants.SCRIPT).stringValue("javascript");
                documentBuilder.setPostUpgradeScript(new Script(language, script));
            }

            String postJavaDelegateClassName = rootNode.path(ProcessInstanceMigrationDocumentConstants.POST_UPGRADE_JAVA_DELEGATE).stringValue(null);
            documentBuilder.setPostUpgradeJavaDelegate(postJavaDelegateClassName);

            String postExpression = rootNode.path(ProcessInstanceMigrationDocumentConstants.POST_UPGRADE_JAVA_DELEGATE_EXPRESSION).stringValue(null);
            documentBuilder.setPostUpgradeJavaDelegateExpression(postExpression);

            JsonNode activityMigrationMappings = rootNode.get(ProcessInstanceMigrationDocumentConstants.ACTIVITY_MAPPINGS_JSON_SECTION);
            if (activityMigrationMappings != null) {

                for (JsonNode mappingNode : activityMigrationMappings) {
                    Class<? extends ActivityMigrationMapping> mappingClass = null;
                    if (isSingleTextValue.test(mappingNode.get(ProcessInstanceMigrationDocumentConstants.FROM_ACTIVITY_ID_JSON_PROPERTY)) && 
                            isSingleTextValue.test(mappingNode.get(ProcessInstanceMigrationDocumentConstants.TO_ACTIVITY_ID_JSON_PROPERTY))) {
                        mappingClass = ActivityMigrationMapping.OneToOneMapping.class;
                    }
                    if (isSingleTextValue.test(mappingNode.get(ProcessInstanceMigrationDocumentConstants.FROM_ACTIVITY_ID_JSON_PROPERTY)) && 
                            isMultiValue.test(mappingNode.get(ProcessInstanceMigrationDocumentConstants.TO_ACTIVITY_IDS_JSON_PROPERTY))) {
                        mappingClass = ActivityMigrationMapping.OneToManyMapping.class;
                    }
                    if (isMultiValue.test(mappingNode.get(ProcessInstanceMigrationDocumentConstants.FROM_ACTIVITY_IDS_JSON_PROPERTY)) && 
                            isSingleTextValue.test(mappingNode.get(ProcessInstanceMigrationDocumentConstants.TO_ACTIVITY_ID_JSON_PROPERTY))) {
                        mappingClass = ActivityMigrationMapping.ManyToOneMapping.class;
                    }

                    BaseActivityMigrationMappingConverter mappingConverter = activityMigrationMappingConverters.get(mappingClass);
                    ActivityMigrationMapping mapping = mappingConverter.convertFromJson(mappingNode, objectMapper);
                    documentBuilder.addActivityMigrationMapping(mapping);
                }
            }
            
            JsonNode enableActivityMappings = rootNode.get(ProcessInstanceMigrationDocumentConstants.ENABLE_ACTIVITY_MAPPINGS_JSON_SECTION);
            if (enableActivityMappings != null) {

                for (JsonNode mappingNode : enableActivityMappings) {
                    if (mappingNode.hasNonNull(ProcessInstanceMigrationDocumentConstants.ACTIVITY_ID_JSON_PROPERTY)) {
                        EnableActivityMapping.EnableMapping enableMapping = new EnableActivityMapping.EnableMapping(
                                mappingNode.get(ProcessInstanceMigrationDocumentConstants.ACTIVITY_ID_JSON_PROPERTY).asString());
                        documentBuilder.addEnableActivityMapping(enableMapping);
                    }
                }
            }

            JsonNode processInstanceVariablesNode = rootNode.get(ProcessInstanceMigrationDocumentConstants.PROCESS_INSTANCE_VARIABLES_JSON_SECTION);
            if (processInstanceVariablesNode != null) {
                Map<String, Object> processInstanceVariables = ProcessInstanceMigrationDocumentConverter.convertFromJsonNodeToObject(processInstanceVariablesNode, objectMapper);
                documentBuilder.addProcessInstanceVariables(processInstanceVariables);
            }
            return documentBuilder.build();

        } catch (JacksonException e) {
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
            
            JsonNode newNameToJsonNode = convertNewNameToJson(mapping, objectMapper);
            if (newNameToJsonNode != null && !newNameToJsonNode.isNull()) {
                mappingNode.set(ProcessInstanceMigrationDocumentConstants.NEW_NAME_JSON_PROPERTY, newNameToJsonNode);
            }
            
            JsonNode newDueDateToJsonNode = convertNewDueDateToJson(mapping, objectMapper);
            if (newDueDateToJsonNode != null && !newDueDateToJsonNode.isNull()) {
                mappingNode.set(ProcessInstanceMigrationDocumentConstants.NEW_DUE_DATE_JSON_PROPERTY, newDueDateToJsonNode);
            }
            
            JsonNode newPriorityToJsonNode = convertNewPriorityToJson(mapping, objectMapper);
            if (newPriorityToJsonNode != null && !newPriorityToJsonNode.isNull()) {
                mappingNode.set(ProcessInstanceMigrationDocumentConstants.NEW_PRIORITY_JSON_PROPERTY, newPriorityToJsonNode);
            }
            
            JsonNode newCategoryToJsonNode = convertNewCategoryToJson(mapping, objectMapper);
            if (newCategoryToJsonNode != null && !newCategoryToJsonNode.isNull()) {
                mappingNode.set(ProcessInstanceMigrationDocumentConstants.NEW_CATEGORY_JSON_PROPERTY, newCategoryToJsonNode);
            }
            
            JsonNode newFormKeyToJsonNode = convertNewFormKeyToJson(mapping, objectMapper);
            if (newFormKeyToJsonNode != null && !newFormKeyToJsonNode.isNull()) {
                mappingNode.set(ProcessInstanceMigrationDocumentConstants.NEW_FORM_KEY_JSON_PROPERTY, newFormKeyToJsonNode);
            }

            JsonNode newAssigneeToJsonNode = convertNewAssigneeToJson(mapping, objectMapper);
            if (newAssigneeToJsonNode != null && !newAssigneeToJsonNode.isNull()) {
                mappingNode.set(ProcessInstanceMigrationDocumentConstants.NEW_ASSIGNEE_JSON_PROPERTY, newAssigneeToJsonNode);
            }
            
            JsonNode newOwnerToJsonNode = convertNewOwnerToJson(mapping, objectMapper);
            if (newOwnerToJsonNode != null && !newOwnerToJsonNode.isNull()) {
                mappingNode.set(ProcessInstanceMigrationDocumentConstants.NEW_OWNER_JSON_PROPERTY, newOwnerToJsonNode);
            }
            
            JsonNode newCandidateUsersToJsonNode = convertNewCandidateUsersToJson(mapping, objectMapper);
            if (newCandidateUsersToJsonNode != null && !newCandidateUsersToJsonNode.isNull()) {
                mappingNode.set(ProcessInstanceMigrationDocumentConstants.NEW_CANDIDATE_USERS_JSON_PROPERTY, newCandidateUsersToJsonNode);
            }
            
            JsonNode newCandidateGroupsToJsonNode = convertNewCandidateGroupsToJson(mapping, objectMapper);
            if (newCandidateGroupsToJsonNode != null && !newCandidateGroupsToJsonNode.isNull()) {
                mappingNode.set(ProcessInstanceMigrationDocumentConstants.NEW_CANDIDATE_GROUPS_JSON_PROPERTY, newCandidateGroupsToJsonNode);
            }

            JsonNode variablesToJsonNode = convertLocalVariablesToJson(mapping, objectMapper);
            if (variablesToJsonNode != null && !variablesToJsonNode.isNull()) {
                mappingNode.set(ProcessInstanceMigrationDocumentConstants.LOCAL_VARIABLES_JSON_SECTION, variablesToJsonNode);
            }

            return mappingNode;
        }

        protected abstract ObjectNode convertMappingInfoToJson(T mapping, ObjectMapper objectMapper);

        protected ObjectNode convertAdditionalMappingInfoToJson(T mapping, ObjectMapper objectMapper) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            if (mapping.isToParentProcess()) {
                mappingNode.put(ProcessInstanceMigrationDocumentConstants.IN_PARENT_PROCESS_OF_CALL_ACTIVITY_JSON_PROPERTY, mapping.getFromCallActivityId());
            }
            if (mapping.isToCallActivity()) {
                mappingNode.put(ProcessInstanceMigrationDocumentConstants.IN_SUB_PROCESS_OF_CALL_ACTIVITY_ID_JSON_PROPERTY, mapping.getToCallActivityId());
                mappingNode.put(ProcessInstanceMigrationDocumentConstants.CALL_ACTIVITY_PROCESS_DEFINITION_VERSION_JSON_PROPERTY, mapping.getCallActivityProcessDefinitionVersion());
            }
            return mappingNode;
        }

        protected abstract JsonNode convertLocalVariablesToJson(T mapping, ObjectMapper objectMapper);
        
        protected abstract JsonNode convertNewNameToJson(T mapping, ObjectMapper objectMapper);
        
        protected abstract JsonNode convertNewDueDateToJson(T mapping, ObjectMapper objectMapper);
        
        protected abstract JsonNode convertNewPriorityToJson(T mapping, ObjectMapper objectMapper);
        
        protected abstract JsonNode convertNewCategoryToJson(T mapping, ObjectMapper objectMapper);
        
        protected abstract JsonNode convertNewFormKeyToJson(T mapping, ObjectMapper objectMapper);

        protected abstract JsonNode convertNewAssigneeToJson(T mapping, ObjectMapper objectMapper);
        
        protected abstract JsonNode convertNewOwnerToJson(T mapping, ObjectMapper objectMapper);
        
        protected abstract JsonNode convertNewCandidateUsersToJson(T mapping, ObjectMapper objectMapper);
        
        protected abstract JsonNode convertNewCandidateGroupsToJson(T mapping, ObjectMapper objectMapper);

        public abstract T convertFromJson(JsonNode jsonNode, ObjectMapper objectMapper);

        protected <M extends ActivityMigrationMappingOptions<T>> void convertAdditionalMappingInfoFromJson(M mapping, JsonNode jsonNode) {
            JsonNode callActivityOfParentProcess = jsonNode.get(ProcessInstanceMigrationDocumentConstants.IN_PARENT_PROCESS_OF_CALL_ACTIVITY_JSON_PROPERTY);
            if (callActivityOfParentProcess != null) {
                mapping.inParentProcessOfCallActivityId(callActivityOfParentProcess.stringValue());
                return; //if its a move to parent, it cannot be also a move to subProcess
            }

            JsonNode ofCallActivityId = jsonNode.get(ProcessInstanceMigrationDocumentConstants.IN_SUB_PROCESS_OF_CALL_ACTIVITY_ID_JSON_PROPERTY);
            JsonNode subProcDefVer = jsonNode.get(ProcessInstanceMigrationDocumentConstants.CALL_ACTIVITY_PROCESS_DEFINITION_VERSION_JSON_PROPERTY);
            if (ofCallActivityId != null) {
                if (subProcDefVer != null) {
                    mapping.inSubProcessOfCallActivityId(ofCallActivityId.stringValue(), subProcDefVer.intValue());
                } else {
                    mapping.inSubProcessOfCallActivityId(ofCallActivityId.stringValue());
                }
            }

        }

        protected <V> V getLocalVariablesFromJson(JsonNode jsonNode, ObjectMapper objectMapper) {
            JsonNode localVariablesNode = jsonNode.get(ProcessInstanceMigrationDocumentConstants.LOCAL_VARIABLES_JSON_SECTION);
            if (localVariablesNode != null) {
                return ProcessInstanceMigrationDocumentConverter.convertFromJsonNodeToObject(localVariablesNode, objectMapper);
            }
            return null;
        }
        
        protected String getNewNameFromJson(JsonNode jsonNode) {
            return jsonNode.path(ProcessInstanceMigrationDocumentConstants.NEW_NAME_JSON_PROPERTY).stringValue(null);
        }
        
        protected String getNewDueDateFromJson(JsonNode jsonNode) {
            return jsonNode.path(ProcessInstanceMigrationDocumentConstants.NEW_DUE_DATE_JSON_PROPERTY).stringValue(null);
        }
        
        protected String getNewPriorityFromJson(JsonNode jsonNode) {
            return jsonNode.path(ProcessInstanceMigrationDocumentConstants.NEW_PRIORITY_JSON_PROPERTY).stringValue(null);
        }
        
        protected String getNewCategoryFromJson(JsonNode jsonNode) {
            return jsonNode.path(ProcessInstanceMigrationDocumentConstants.NEW_CATEGORY_JSON_PROPERTY).stringValue(null);
        }
        
        protected String getNewFormKeyFromJson(JsonNode jsonNode) {
            return jsonNode.path(ProcessInstanceMigrationDocumentConstants.NEW_FORM_KEY_JSON_PROPERTY).stringValue(null);
        }

        protected String getNewAssigneeFromJson(JsonNode jsonNode) {
            return jsonNode.path(ProcessInstanceMigrationDocumentConstants.NEW_ASSIGNEE_JSON_PROPERTY).stringValue(null);
        }

        protected String getNewOwnerFromJson(JsonNode jsonNode) {
            return jsonNode.path(ProcessInstanceMigrationDocumentConstants.NEW_OWNER_JSON_PROPERTY).stringValue(null);
        }
        
        protected List<String> getNewCandidateUsersFromJson(JsonNode jsonNode) {
            List<String> candidateUsers = null;
            JsonNode candidateUserArray = jsonNode.path(ProcessInstanceMigrationDocumentConstants.NEW_CANDIDATE_USERS_JSON_PROPERTY);
            if (candidateUserArray != null && candidateUserArray.isArray() && !candidateUserArray.isEmpty()) {
                candidateUsers = new ArrayList<>();
                for (JsonNode userNode : candidateUserArray) {
                    candidateUsers.add(userNode.asString());
                }
            }
            
            return candidateUsers;
        }
        
        protected List<String> getNewCandidateGroupsFromJson(JsonNode jsonNode) {
            List<String> candidateGroups = null;
            JsonNode candidateGroupArray = jsonNode.path(ProcessInstanceMigrationDocumentConstants.NEW_CANDIDATE_GROUPS_JSON_PROPERTY);
            if (candidateGroupArray != null && candidateGroupArray.isArray() && !candidateGroupArray.isEmpty()) {
                candidateGroups = new ArrayList<>();
                for (JsonNode groupNode : candidateGroupArray) {
                    candidateGroups.add(groupNode.asString());
                }
            }
            
            return candidateGroups;
        }
    }

    public static class OneToOneMappingConverter extends BaseActivityMigrationMappingConverter<ActivityMigrationMapping.OneToOneMapping> {

        @Override
        protected ObjectNode convertMappingInfoToJson(ActivityMigrationMapping.OneToOneMapping mapping, ObjectMapper objectMapper) {
            ObjectNode mappingNode = objectMapper.createObjectNode();
            mappingNode.put(ProcessInstanceMigrationDocumentConstants.FROM_ACTIVITY_ID_JSON_PROPERTY, mapping.getFromActivityId());
            mappingNode.put(ProcessInstanceMigrationDocumentConstants.TO_ACTIVITY_ID_JSON_PROPERTY, mapping.getToActivityId());
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
        protected JsonNode convertNewNameToJson(ActivityMigrationMapping.OneToOneMapping mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewName());
        }
        
        @Override
        protected JsonNode convertNewDueDateToJson(ActivityMigrationMapping.OneToOneMapping mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewDueDate());
        }
        
        @Override
        protected JsonNode convertNewPriorityToJson(ActivityMigrationMapping.OneToOneMapping mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewPriority());
        }
        
        @Override
        protected JsonNode convertNewCategoryToJson(ActivityMigrationMapping.OneToOneMapping mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewCategory());
        }
        
        @Override
        protected JsonNode convertNewFormKeyToJson(ActivityMigrationMapping.OneToOneMapping mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewFormKey());
        }

        @Override
        protected JsonNode convertNewAssigneeToJson(ActivityMigrationMapping.OneToOneMapping mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewAssignee());
        }
        
        @Override
        protected JsonNode convertNewOwnerToJson(ActivityMigrationMapping.OneToOneMapping mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewOwner());
        }
        
        @Override
        protected JsonNode convertNewCandidateUsersToJson(ActivityMigrationMapping.OneToOneMapping mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewCandidateUsers());
        }
        
        @Override
        protected JsonNode convertNewCandidateGroupsToJson(ActivityMigrationMapping.OneToOneMapping mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewCandidateGroups());
        }

        @Override
        public ActivityMigrationMapping.OneToOneMapping convertFromJson(JsonNode jsonNode, ObjectMapper objectMapper) {
            String fromActivityId = jsonNode.get(ProcessInstanceMigrationDocumentConstants.FROM_ACTIVITY_ID_JSON_PROPERTY).stringValue();
            String toActivityId = jsonNode.get(ProcessInstanceMigrationDocumentConstants.TO_ACTIVITY_ID_JSON_PROPERTY).stringValue();

            ActivityMigrationMapping.OneToOneMapping oneToOneMapping = ActivityMigrationMapping.createMappingFor(fromActivityId, toActivityId);
            convertAdditionalMappingInfoFromJson(oneToOneMapping, jsonNode);

            oneToOneMapping.withNewName(getNewNameFromJson(jsonNode));
            oneToOneMapping.withNewDueDate(getNewDueDateFromJson(jsonNode));
            oneToOneMapping.withNewPriority(getNewPriorityFromJson(jsonNode));
            oneToOneMapping.withNewCategory(getNewCategoryFromJson(jsonNode));
            oneToOneMapping.withNewFormKey(getNewFormKeyFromJson(jsonNode));
            oneToOneMapping.withNewAssignee(getNewAssigneeFromJson(jsonNode));
            oneToOneMapping.withNewOwner(getNewOwnerFromJson(jsonNode));
            oneToOneMapping.withNewCandidateUsers(getNewCandidateUsersFromJson(jsonNode));
            oneToOneMapping.withNewCandidateGroups(getNewCandidateGroupsFromJson(jsonNode));

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
            mappingNode.set(ProcessInstanceMigrationDocumentConstants.FROM_ACTIVITY_IDS_JSON_PROPERTY, fromActivityIdsNode);
            mappingNode.put(ProcessInstanceMigrationDocumentConstants.TO_ACTIVITY_ID_JSON_PROPERTY, mapping.getToActivityId());
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
        protected JsonNode convertNewNameToJson(ActivityMigrationMapping.ManyToOneMapping mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewName());
        }
        
        @Override
        protected JsonNode convertNewDueDateToJson(ActivityMigrationMapping.ManyToOneMapping mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewDueDate());
        }
        
        @Override
        protected JsonNode convertNewPriorityToJson(ActivityMigrationMapping.ManyToOneMapping mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewPriority());
        }
        
        @Override
        protected JsonNode convertNewCategoryToJson(ActivityMigrationMapping.ManyToOneMapping mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewCategory());
        }
        
        @Override
        protected JsonNode convertNewFormKeyToJson(ActivityMigrationMapping.ManyToOneMapping mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewFormKey());
        }

        @Override
        protected JsonNode convertNewAssigneeToJson(ActivityMigrationMapping.ManyToOneMapping mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewAssignee());
        }
        
        @Override
        protected JsonNode convertNewOwnerToJson(ActivityMigrationMapping.ManyToOneMapping mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewOwner());
        }
        
        @Override
        protected JsonNode convertNewCandidateUsersToJson(ActivityMigrationMapping.ManyToOneMapping mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewCandidateUsers());
        }
        
        @Override
        protected JsonNode convertNewCandidateGroupsToJson(ActivityMigrationMapping.ManyToOneMapping mapping, ObjectMapper objectMapper) {
            return objectMapper.valueToTree(mapping.getWithNewCandidateGroups());
        }

        @Override
        public ActivityMigrationMapping.ManyToOneMapping convertFromJson(JsonNode jsonNode, ObjectMapper objectMapper) {
            JsonNode fromActivityIdsNode = jsonNode.get(ProcessInstanceMigrationDocumentConstants.FROM_ACTIVITY_IDS_JSON_PROPERTY);
            List<String> fromActivityIds = objectMapper.convertValue(fromActivityIdsNode, new TypeReference<>() {

            });
            String toActivityId = jsonNode.get(ProcessInstanceMigrationDocumentConstants.TO_ACTIVITY_ID_JSON_PROPERTY).stringValue();

            ActivityMigrationMapping.ManyToOneMapping manyToOneMapping = ActivityMigrationMapping.createMappingFor(fromActivityIds, toActivityId);
            convertAdditionalMappingInfoFromJson(manyToOneMapping, jsonNode);

            manyToOneMapping.withNewName(getNewNameFromJson(jsonNode));
            manyToOneMapping.withNewDueDate(getNewDueDateFromJson(jsonNode));
            manyToOneMapping.withNewPriority(getNewPriorityFromJson(jsonNode));
            manyToOneMapping.withNewCategory(getNewCategoryFromJson(jsonNode));
            manyToOneMapping.withNewFormKey(getNewFormKeyFromJson(jsonNode));
            manyToOneMapping.withNewAssignee(getNewAssigneeFromJson(jsonNode));
            manyToOneMapping.withNewOwner(getNewOwnerFromJson(jsonNode));
            manyToOneMapping.withNewCandidateUsers(getNewCandidateUsersFromJson(jsonNode));
            manyToOneMapping.withNewCandidateGroups(getNewCandidateGroupsFromJson(jsonNode));

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
            mappingNode.put(ProcessInstanceMigrationDocumentConstants.FROM_ACTIVITY_ID_JSON_PROPERTY, mapping.getFromActivityId());
            JsonNode toActivityIdsNode = objectMapper.valueToTree(mapping.getToActivityIds());
            mappingNode.set(ProcessInstanceMigrationDocumentConstants.TO_ACTIVITY_IDS_JSON_PROPERTY, toActivityIdsNode);
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
        protected JsonNode convertNewNameToJson(ActivityMigrationMapping.OneToManyMapping mapping, ObjectMapper objectMapper) {
            return null;
        }
        
        @Override
        protected JsonNode convertNewDueDateToJson(ActivityMigrationMapping.OneToManyMapping mapping, ObjectMapper objectMapper) {
            return null;
        }
        
        @Override
        protected JsonNode convertNewPriorityToJson(ActivityMigrationMapping.OneToManyMapping mapping, ObjectMapper objectMapper) {
            return null;
        }
        
        @Override
        protected JsonNode convertNewCategoryToJson(ActivityMigrationMapping.OneToManyMapping mapping, ObjectMapper objectMapper) {
            return null;
        }
        
        @Override
        protected JsonNode convertNewFormKeyToJson(ActivityMigrationMapping.OneToManyMapping mapping, ObjectMapper objectMapper) {
            return null;
        }

        @Override
        protected JsonNode convertNewAssigneeToJson(ActivityMigrationMapping.OneToManyMapping mapping, ObjectMapper objectMapper) {
            return null;
        }
        
        @Override
        protected JsonNode convertNewOwnerToJson(ActivityMigrationMapping.OneToManyMapping mapping, ObjectMapper objectMapper) {
            return null;
        }
        
        @Override
        protected JsonNode convertNewCandidateUsersToJson(ActivityMigrationMapping.OneToManyMapping mapping, ObjectMapper objectMapper) {
            return null;
        }
        
        @Override
        protected JsonNode convertNewCandidateGroupsToJson(ActivityMigrationMapping.OneToManyMapping mapping, ObjectMapper objectMapper) {
            return null;
        }

        @Override
        public ActivityMigrationMapping.OneToManyMapping convertFromJson(JsonNode jsonNode, ObjectMapper objectMapper) {
            String fromActivityId = jsonNode.get(ProcessInstanceMigrationDocumentConstants.FROM_ACTIVITY_ID_JSON_PROPERTY).stringValue();
            JsonNode toActivityIdsNode = jsonNode.get(ProcessInstanceMigrationDocumentConstants.TO_ACTIVITY_IDS_JSON_PROPERTY);
            List<String> toActivityIds = objectMapper.convertValue(toActivityIdsNode, new TypeReference<>() {

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

