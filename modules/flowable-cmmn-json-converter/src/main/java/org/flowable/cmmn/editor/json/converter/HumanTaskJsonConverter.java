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
package org.flowable.cmmn.editor.json.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.flowable.cmmn.editor.json.converter.CmmnJsonConverter.CmmnModelIdHelper;
import org.flowable.cmmn.editor.json.converter.util.CollectionUtils;
import org.flowable.cmmn.editor.json.converter.util.ListenerConverterUtil;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CaseElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.FlowableListener;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.PlanItem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class HumanTaskJsonConverter extends BaseCmmnJsonConverter {

    public static void fillTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToCmmnMap,
            Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToCmmnMap);
        fillCmmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_TASK_HUMAN, HumanTaskJsonConverter.class);
    }

    public static void fillCmmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        convertersToJsonMap.put(HumanTask.class, HumanTaskJsonConverter.class);
    }

    @Override
    protected String getStencilId(BaseElement baseElement) {
        return STENCIL_TASK_HUMAN;
    }

    @Override
    protected void convertElementToJson(ObjectNode elementNode, ObjectNode propertiesNode, ActivityProcessor processor, BaseElement baseElement,
            CmmnModel cmmnModel, CmmnJsonConverterContext converterContext) {
        PlanItem planItem = (PlanItem) baseElement;
        HumanTask humanTask = (HumanTask) planItem.getPlanItemDefinition();
        String assignee = humanTask.getAssignee();

        convertAssignmentSettings(propertiesNode, humanTask, assignee);

        if (humanTask.getPriority() != null) {
            setPropertyValue(PROPERTY_USERTASK_PRIORITY, humanTask.getPriority(), propertiesNode);
        }

        if (StringUtils.isNotEmpty(humanTask.getFormKey())) {
            Map<String, String> modelInfo = converterContext.getFormModelInfoForFormModelKey(humanTask.getFormKey());
            if (modelInfo != null) {
                ObjectNode formRefNode = objectMapper.createObjectNode();
                formRefNode.put("id", modelInfo.get("id"));
                formRefNode.put("name", modelInfo.get("name"));
                formRefNode.put("key", modelInfo.get("key"));
                propertiesNode.set(PROPERTY_FORM_REFERENCE, formRefNode);

            } else {
                setPropertyValue(PROPERTY_FORMKEY, humanTask.getFormKey(), propertiesNode);
            }
        }

        setPropertyValue(PROPERTY_USERTASK_DUEDATE, humanTask.getDueDate(), propertiesNode);
        setPropertyValue(PROPERTY_USERTASK_CATEGORY, humanTask.getCategory(), propertiesNode);
        setPropertyValue(PROPERTY_USERTASK_TASK_ID_VARIABLE_NAME, humanTask.getTaskIdVariableName(), propertiesNode);

        convertTaskListenersToJson(propertiesNode, humanTask);
        ListenerConverterUtil.convertLifecycleListenersToJson(objectMapper, propertiesNode, humanTask);
    }

    protected void convertAssignmentSettings(ObjectNode propertiesNode, HumanTask humanTask, String assignee) {
        if (StringUtils.isNotEmpty(assignee) || CollectionUtils.isNotEmpty(humanTask.getCandidateUsers()) || CollectionUtils.isNotEmpty(humanTask.getCandidateGroups())) {

            ObjectNode assignmentNode = objectMapper.createObjectNode();
            ObjectNode assignmentValuesNode = objectMapper.createObjectNode();

            List<ExtensionElement> idmAssigneeList = humanTask.getExtensionElements().get("flowable-idm-assignee");
            if (CollectionUtils.isNotEmpty(idmAssigneeList)
                    || CollectionUtils.isNotEmpty(humanTask.getExtensionElements().get("flowable-idm-candidate-user"))
                    || CollectionUtils.isNotEmpty(humanTask.getExtensionElements().get("flowable-idm-candidate-group"))) {

                assignmentValuesNode.put("type", "idm");
                ObjectNode idmNode = objectMapper.createObjectNode();
                assignmentValuesNode.set("idm", idmNode);

                List<ExtensionElement> canCompleteList = humanTask.getExtensionElements().get("initiator-can-complete");
                if (CollectionUtils.isNotEmpty(canCompleteList)) {
                    assignmentValuesNode.put("initiatorCanCompleteTask", Boolean.valueOf(canCompleteList.get(0).getElementText()));
                }

                if (StringUtils.isNotEmpty(humanTask.getAssignee())) {
                    ObjectNode assigneeNode = objectMapper.createObjectNode();
                    assigneeNode.put("id", humanTask.getAssignee());
                    idmNode.set("assignee", assigneeNode);
                    idmNode.put("type", "user");

                    fillProperty("email", "assignee-info-email", assigneeNode, humanTask);
                    fillProperty("firstName", "assignee-info-firstname", assigneeNode, humanTask);
                    fillProperty("lastName", "assignee-info-lastname", assigneeNode, humanTask);
                }

                List<ExtensionElement> idmCandidateUserList = humanTask.getExtensionElements().get("flowable-idm-candidate-user");
                if (CollectionUtils.isNotEmpty(humanTask.getCandidateUsers()) && CollectionUtils.isNotEmpty(idmCandidateUserList)) {

                    List<String> candidateUserIds = new ArrayList<>();

                    if (humanTask.getCandidateUsers().size() == 1) {
                        idmNode.put("type", "users");

                        String candidateUsersString = humanTask.getCandidateUsers().get(0);
                        String[] candidateUserArray = candidateUsersString.split(",");
                        for (String candidate : candidateUserArray) {
                            candidateUserIds.add(candidate.trim());
                        }

                    } else {
                        candidateUserIds.addAll(humanTask.getCandidateUsers());
                    }

                    if (candidateUserIds.size() > 0) {
                        ArrayNode candidateUsersNode = objectMapper.createArrayNode();
                        idmNode.set("candidateUsers", candidateUsersNode);
                        idmNode.put("type", "users");
                        for (String candidateUser : humanTask.getCandidateUsers()) {
                            ObjectNode candidateUserNode = objectMapper.createObjectNode();
                            candidateUserNode.put("id", candidateUser);
                            candidateUsersNode.add(candidateUserNode);

                            fillProperty("email", "user-info-email-" + candidateUser, candidateUserNode, humanTask);
                            fillProperty("firstName", "user-info-firstname-" + candidateUser, candidateUserNode, humanTask);
                            fillProperty("lastName", "user-info-lastname-" + candidateUser, candidateUserNode, humanTask);
                        }
                    }
                }

                List<ExtensionElement> idmCandidateGroupList = humanTask.getExtensionElements().get("flowable-idm-candidate-group");
                if (CollectionUtils.isNotEmpty(humanTask.getCandidateGroups()) && CollectionUtils.isNotEmpty(idmCandidateGroupList)) {

                    List<String> candidateGroupIds = new ArrayList<>();

                    if (humanTask.getCandidateGroups().size() == 1) {
                        idmNode.put("type", "groups");

                        String candidateGroupsString = humanTask.getCandidateGroups().get(0);
                        String[] candidateGroupArray = candidateGroupsString.split(",");
                        for (String candidate : candidateGroupArray) {
                            candidateGroupIds.add(candidate.trim());
                        }

                    } else {
                        candidateGroupIds.addAll(humanTask.getCandidateGroups());
                    }

                    if (candidateGroupIds.size() > 0) {
                        ArrayNode candidateGroupsNode = objectMapper.createArrayNode();
                        idmNode.set("candidateGroups", candidateGroupsNode);
                        idmNode.put("type", "groups");
                        for (String candidateGroup : humanTask.getCandidateGroups()) {
                            ObjectNode candidateGroupNode = objectMapper.createObjectNode();
                            candidateGroupNode.put("id", candidateGroup);
                            candidateGroupsNode.add(candidateGroupNode);

                            fillProperty("name", "group-info-name-" + candidateGroup, candidateGroupNode, humanTask);
                        }
                    }
                }

            } else {
                assignmentValuesNode.put("type", "static");

                if (StringUtils.isNotEmpty(assignee)) {
                    assignmentValuesNode.put(PROPERTY_USERTASK_ASSIGNEE, assignee);
                }

                if (CollectionUtils.isNotEmpty(humanTask.getCandidateUsers())) {
                    ArrayNode candidateArrayNode = objectMapper.createArrayNode();
                    for (String candidateUser : humanTask.getCandidateUsers()) {
                        ObjectNode candidateNode = objectMapper.createObjectNode();
                        candidateNode.put("value", candidateUser);
                        candidateArrayNode.add(candidateNode);
                    }
                    assignmentValuesNode.set(PROPERTY_USERTASK_CANDIDATE_USERS, candidateArrayNode);
                }

                if (CollectionUtils.isNotEmpty(humanTask.getCandidateGroups())) {
                    ArrayNode candidateArrayNode = objectMapper.createArrayNode();
                    for (String candidateGroup : humanTask.getCandidateGroups()) {
                        ObjectNode candidateNode = objectMapper.createObjectNode();
                        candidateNode.put("value", candidateGroup);
                        candidateArrayNode.add(candidateNode);
                    }
                    assignmentValuesNode.set(PROPERTY_USERTASK_CANDIDATE_GROUPS, candidateArrayNode);
                }
            }

            assignmentNode.set("assignment", assignmentValuesNode);
            propertiesNode.set(PROPERTY_USERTASK_ASSIGNMENT, assignmentNode);
        }
    }

    protected void convertTaskListenersToJson(ObjectNode propertiesNode, HumanTask humanTask) {
        ObjectNode taskListenersNode = ListenerConverterUtil.convertListenersToJson(objectMapper, "taskListeners", humanTask.getTaskListeners());
        if (taskListenersNode != null) {
            propertiesNode.set(PROPERTY_USERTASK_LISTENERS, taskListenersNode);
        }
    }

    protected int getExtensionElementValueAsInt(String name, HumanTask humanTask) {
        int intValue = 0;
        String value = getExtensionElementValue(name, humanTask);
        if (NumberUtils.isCreatable(value)) {
            intValue = Integer.valueOf(value);
        }
        return intValue;
    }

    protected String getExtensionElementValue(String name, HumanTask humanTask) {
        String value = "";
        if (CollectionUtils.isNotEmpty(humanTask.getExtensionElements().get(name))) {
            ExtensionElement extensionElement = humanTask.getExtensionElements().get(name).get(0);
            value = extensionElement.getElementText();
        }
        return value;
    }

    @Override
    protected CaseElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, ActivityProcessor processor,
                    BaseElement parentElement, Map<String, JsonNode> shapeMap, CmmnModel cmmnModel, CmmnJsonConverterContext converterContext, CmmnModelIdHelper cmmnModelIdHelper) {

        HumanTask task = new HumanTask();

        task.setPriority(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_USERTASK_PRIORITY, elementNode));
        task.setFormKey(CmmnJsonConverterUtil.getPropertyFormKey(elementNode, converterContext));
        task.setValidateFormFields(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_FORM_FIELD_VALIDATION, elementNode));

        task.setDueDate(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_USERTASK_DUEDATE, elementNode));
        task.setCategory(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_USERTASK_CATEGORY, elementNode));
        task.setTaskIdVariableName(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_USERTASK_TASK_ID_VARIABLE_NAME, elementNode));

        convertJsonToTaskListeners(elementNode, task);
        ListenerConverterUtil.convertJsonToLifeCycleListeners(elementNode, task);

        JsonNode assignmentNode = CmmnJsonConverterUtil.getProperty(PROPERTY_USERTASK_ASSIGNMENT, elementNode);
        if (assignmentNode != null) {
            JsonNode assignmentDefNode = assignmentNode.get("assignment");
            if (assignmentDefNode != null) {

                JsonNode typeNode = assignmentDefNode.get("type");
                JsonNode canCompleteTaskNode = assignmentDefNode.get("initiatorCanCompleteTask");
                if (typeNode == null || "static".equalsIgnoreCase(typeNode.asText())) {
                    JsonNode assigneeNode = assignmentDefNode.get(PROPERTY_USERTASK_ASSIGNEE);
                    if (assigneeNode != null && !assigneeNode.isNull()) {
                        task.setAssignee(assigneeNode.asText());
                    }

                    task.setCandidateUsers(getValueAsList(PROPERTY_USERTASK_CANDIDATE_USERS, assignmentDefNode));
                    task.setCandidateGroups(getValueAsList(PROPERTY_USERTASK_CANDIDATE_GROUPS, assignmentDefNode));

                    if (StringUtils.isNotEmpty(task.getAssignee())) {

                        if (canCompleteTaskNode != null && !canCompleteTaskNode.isNull()) {
                            addInitiatorCanCompleteExtensionElement(Boolean.valueOf(canCompleteTaskNode.asText()), task);
                        } else {
                            addInitiatorCanCompleteExtensionElement(false, task);
                        }

                    } else if (StringUtils.isNotEmpty(task.getAssignee())) {
                        addInitiatorCanCompleteExtensionElement(true, task);
                    }

                } else if ("idm".equalsIgnoreCase(typeNode.asText())) {
                    JsonNode idmDefNode = assignmentDefNode.get("idm");
                    if (idmDefNode != null && idmDefNode.has("type")) {
                        JsonNode idmTypeNode = idmDefNode.get("type");
                        if (idmTypeNode != null && "user".equalsIgnoreCase(idmTypeNode.asText()) && idmDefNode.has("assignee")) {

                            fillAssigneeInfo(idmDefNode, canCompleteTaskNode, task);

                        } else if (idmTypeNode != null && "users".equalsIgnoreCase(idmTypeNode.asText()) && idmDefNode.has("candidateUsers")) {

                            fillCandidateUsers(idmDefNode, canCompleteTaskNode, task);

                        } else if (idmTypeNode != null && "groups".equalsIgnoreCase(idmTypeNode.asText()) && idmDefNode.has("candidateGroups")) {

                            fillCandidateGroups(idmDefNode, canCompleteTaskNode, task);

                        } else {
                            task.setAssignee("${" + cmmnModel.getPrimaryCase().getInitiatorVariableName() + "}");
                            addExtensionElement("flowable-idm-initiator", String.valueOf(true), task);
                        }
                    }
                }
            }
        }

        return task;
    }

    protected void convertJsonToTaskListeners(JsonNode elementNode, HumanTask task) {
        List<FlowableListener> listeners = ListenerConverterUtil.convertJsonToListeners(elementNode, PROPERTY_USERTASK_LISTENERS, "taskListeners");
        if (listeners != null && !listeners.isEmpty()) {
            task.setTaskListeners(listeners);
        }
    }

    protected void fillAssigneeInfo(JsonNode idmDefNode, JsonNode canCompleteTaskNode, HumanTask task) {
        JsonNode assigneeNode = idmDefNode.get("assignee");
        if (assigneeNode != null && !assigneeNode.isNull()) {
            JsonNode idNode = assigneeNode.get("id");
            JsonNode emailNode = assigneeNode.get("email");
            if (idNode != null && !idNode.isNull() && StringUtils.isNotEmpty(idNode.asText())) {
                task.setAssignee(idNode.asText());
                addExtensionElement("flowable-idm-assignee", String.valueOf(true), task);
                addExtensionElement("assignee-info-email", emailNode, task);
                addExtensionElement("assignee-info-firstname", assigneeNode.get("firstName"), task);
                addExtensionElement("assignee-info-lastname", assigneeNode.get("lastName"), task);
            }
        }

        if (canCompleteTaskNode != null && !canCompleteTaskNode.isNull()) {
            addInitiatorCanCompleteExtensionElement(Boolean.valueOf(canCompleteTaskNode.asText()), task);
        } else {
            addInitiatorCanCompleteExtensionElement(false, task);
        }
    }

    protected void fillCandidateUsers(JsonNode idmDefNode, JsonNode canCompleteTaskNode, HumanTask task) {
        List<String> candidateUsers = new ArrayList<>();
        JsonNode candidateUsersNode = idmDefNode.get("candidateUsers");
        if (candidateUsersNode != null && candidateUsersNode.isArray()) {
            List<String> emails = new ArrayList<>();
            for (JsonNode userNode : candidateUsersNode) {
                if (userNode != null && !userNode.isNull()) {
                    JsonNode idNode = userNode.get("id");
                    JsonNode emailNode = userNode.get("email");
                    if (idNode != null && !idNode.isNull() && StringUtils.isNotEmpty(idNode.asText())) {
                        String id = idNode.asText();
                        candidateUsers.add(id);

                        addExtensionElement("user-info-email-" + id, emailNode, task);
                        addExtensionElement("user-info-firstname-" + id, userNode.get("firstName"), task);
                        addExtensionElement("user-info-lastname-" + id, userNode.get("lastName"), task);

                    } else if (emailNode != null && !emailNode.isNull() && StringUtils.isNotEmpty(emailNode.asText())) {
                        String email = emailNode.asText();
                        candidateUsers.add(email);
                        emails.add(email);
                    }
                }
            }

            if (emails.size() > 0) {
                // Email extension element
                addExtensionElement("flowable-candidate-users-emails", StringUtils.join(emails, ","), task);
            }

            if (candidateUsers.size() > 0) {
                addExtensionElement("flowable-idm-candidate-user", String.valueOf(true), task);
                if (canCompleteTaskNode != null && !canCompleteTaskNode.isNull()) {
                    addInitiatorCanCompleteExtensionElement(Boolean.valueOf(canCompleteTaskNode.asText()), task);
                } else {
                    addInitiatorCanCompleteExtensionElement(false, task);
                }
            }
        }

        if (candidateUsers.size() > 0) {
            task.setCandidateUsers(candidateUsers);
        }
    }

    protected void fillCandidateGroups(JsonNode idmDefNode, JsonNode canCompleteTaskNode, HumanTask task) {
        List<String> candidateGroups = new ArrayList<>();
        JsonNode candidateGroupsNode = idmDefNode.get("candidateGroups");
        if (candidateGroupsNode != null && candidateGroupsNode.isArray()) {
            for (JsonNode groupNode : candidateGroupsNode) {
                if (groupNode != null && !groupNode.isNull()) {
                    JsonNode idNode = groupNode.get("id");
                    JsonNode nameNode = groupNode.get("name");
                    if (idNode != null && !idNode.isNull() && StringUtils.isNotEmpty(idNode.asText())) {
                        String id = idNode.asText();
                        candidateGroups.add(id);

                        addExtensionElement("group-info-name-" + id, nameNode, task);
                    }
                }
            }
        }

        if (candidateGroups.size() > 0) {
            task.setCandidateGroups(candidateGroups);
            addExtensionElement("flowable-idm-candidate-group", String.valueOf(true), task);

            if (canCompleteTaskNode != null && !canCompleteTaskNode.isNull()) {
                addInitiatorCanCompleteExtensionElement(Boolean.valueOf(canCompleteTaskNode.asText()), task);
            } else {
                addInitiatorCanCompleteExtensionElement(false, task);
            }
        }
    }

    protected void addInitiatorCanCompleteExtensionElement(boolean canCompleteTask, HumanTask task) {
        addExtensionElement("initiator-can-complete", String.valueOf(canCompleteTask), task);
    }

    protected void addExtensionElement(String name, JsonNode elementNode, HumanTask task) {
        if (elementNode != null && !elementNode.isNull() && StringUtils.isNotEmpty(elementNode.asText())) {
            addExtensionElement(name, elementNode.asText(), task);
        }
    }

    protected void addExtensionElement(String name, String elementText, HumanTask task) {
        ExtensionElement extensionElement = new ExtensionElement();
        extensionElement.setNamespace(NAMESPACE);
        extensionElement.setNamespacePrefix("modeler");
        extensionElement.setName(name);
        extensionElement.setElementText(elementText);
        task.addExtensionElement(extensionElement);
    }

    protected void fillProperty(String propertyName, String extensionElementName, ObjectNode elementNode, HumanTask task) {
        List<ExtensionElement> extensionElementList = task.getExtensionElements().get(extensionElementName);
        if (CollectionUtils.isNotEmpty(extensionElementList)) {
            elementNode.put(propertyName, extensionElementList.get(0).getElementText());
        }
    }

}
