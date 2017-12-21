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

package org.activiti.engine.impl;

import java.util.List;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.DynamicBpmnService;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.cmd.GetBpmnModelCmd;
import org.activiti.engine.impl.cmd.GetProcessDefinitionInfoCmd;
import org.activiti.engine.impl.cmd.SaveProcessDefinitionInfoCmd;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.dynamic.DynamicProcessDefinitionSummary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class DynamicBpmnServiceImpl extends ServiceImpl implements DynamicBpmnService, DynamicBpmnConstants {

    public DynamicBpmnServiceImpl(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }

    @Override
    public ObjectNode getProcessDefinitionInfo(String processDefinitionId) {
        return commandExecutor.execute(new GetProcessDefinitionInfoCmd(processDefinitionId));
    }

    @Override
    public void saveProcessDefinitionInfo(String processDefinitionId, ObjectNode infoNode) {
        commandExecutor.execute(new SaveProcessDefinitionInfoCmd(processDefinitionId, infoNode));
    }

    @Override
    public ObjectNode changeServiceTaskClassName(String id, String className) {
        ObjectNode infoNode = processEngineConfiguration.getObjectMapper().createObjectNode();
        changeServiceTaskClassName(id, className, infoNode);
        return infoNode;
    }

    @Override
    public void changeServiceTaskClassName(String id, String className, ObjectNode infoNode) {
        setElementProperty(id, SERVICE_TASK_CLASS_NAME, className, infoNode);
    }

    @Override
    public ObjectNode changeServiceTaskExpression(String id, String expression) {
        ObjectNode infoNode = processEngineConfiguration.getObjectMapper().createObjectNode();
        changeServiceTaskExpression(id, expression, infoNode);
        return infoNode;
    }

    @Override
    public void changeServiceTaskExpression(String id, String expression, ObjectNode infoNode) {
        setElementProperty(id, SERVICE_TASK_EXPRESSION, expression, infoNode);
    }

    @Override
    public ObjectNode changeServiceTaskDelegateExpression(String id, String expression) {
        ObjectNode infoNode = processEngineConfiguration.getObjectMapper().createObjectNode();
        changeServiceTaskDelegateExpression(id, expression, infoNode);
        return infoNode;
    }

    @Override
    public void changeServiceTaskDelegateExpression(String id, String expression, ObjectNode infoNode) {
        setElementProperty(id, SERVICE_TASK_DELEGATE_EXPRESSION, expression, infoNode);
    }

    @Override
    public ObjectNode changeScriptTaskScript(String id, String script) {
        ObjectNode infoNode = processEngineConfiguration.getObjectMapper().createObjectNode();
        changeScriptTaskScript(id, script, infoNode);
        return infoNode;
    }

    @Override
    public void changeScriptTaskScript(String id, String script, ObjectNode infoNode) {
        setElementProperty(id, SCRIPT_TASK_SCRIPT, script, infoNode);
    }

    @Override
    public ObjectNode changeUserTaskName(String id, String name) {
        ObjectNode infoNode = processEngineConfiguration.getObjectMapper().createObjectNode();
        changeUserTaskName(id, name, infoNode);
        return infoNode;
    }

    @Override
    public void changeUserTaskName(String id, String name, ObjectNode infoNode) {
        setElementProperty(id, USER_TASK_NAME, name, infoNode);
    }

    @Override
    public ObjectNode changeUserTaskDescription(String id, String description) {
        ObjectNode infoNode = processEngineConfiguration.getObjectMapper().createObjectNode();
        changeUserTaskDescription(id, description, infoNode);
        return infoNode;
    }

    @Override
    public void changeUserTaskDescription(String id, String description, ObjectNode infoNode) {
        setElementProperty(id, USER_TASK_DESCRIPTION, description, infoNode);
    }

    @Override
    public ObjectNode changeUserTaskDueDate(String id, String dueDate) {
        ObjectNode infoNode = processEngineConfiguration.getObjectMapper().createObjectNode();
        changeUserTaskDueDate(id, dueDate, infoNode);
        return infoNode;
    }

    @Override
    public void changeUserTaskDueDate(String id, String dueDate, ObjectNode infoNode) {
        setElementProperty(id, USER_TASK_DUEDATE, dueDate, infoNode);
    }

    @Override
    public ObjectNode changeUserTaskPriority(String id, String priority) {
        ObjectNode infoNode = processEngineConfiguration.getObjectMapper().createObjectNode();
        changeUserTaskPriority(id, priority, infoNode);
        return infoNode;
    }

    @Override
    public void changeUserTaskPriority(String id, String priority, ObjectNode infoNode) {
        setElementProperty(id, USER_TASK_PRIORITY, priority, infoNode);
    }

    @Override
    public ObjectNode changeUserTaskCategory(String id, String category) {
        ObjectNode infoNode = processEngineConfiguration.getObjectMapper().createObjectNode();
        changeUserTaskCategory(id, category, infoNode);
        return infoNode;
    }

    @Override
    public void changeUserTaskCategory(String id, String category, ObjectNode infoNode) {
        setElementProperty(id, USER_TASK_CATEGORY, category, infoNode);
    }

    @Override
    public ObjectNode changeUserTaskFormKey(String id, String formKey) {
        ObjectNode infoNode = processEngineConfiguration.getObjectMapper().createObjectNode();
        changeUserTaskFormKey(id, formKey, infoNode);
        return infoNode;
    }

    @Override
    public void changeUserTaskFormKey(String id, String formKey, ObjectNode infoNode) {
        setElementProperty(id, USER_TASK_FORM_KEY, formKey, infoNode);
    }

    @Override
    public ObjectNode changeUserTaskAssignee(String id, String assignee) {
        ObjectNode infoNode = processEngineConfiguration.getObjectMapper().createObjectNode();
        changeUserTaskAssignee(id, assignee, infoNode);
        return infoNode;
    }

    @Override
    public void changeUserTaskAssignee(String id, String assignee, ObjectNode infoNode) {
        setElementProperty(id, USER_TASK_ASSIGNEE, assignee, infoNode);
    }

    @Override
    public ObjectNode changeUserTaskOwner(String id, String owner) {
        ObjectNode infoNode = processEngineConfiguration.getObjectMapper().createObjectNode();
        changeUserTaskOwner(id, owner, infoNode);
        return infoNode;
    }

    @Override
    public void changeUserTaskOwner(String id, String owner, ObjectNode infoNode) {
        setElementProperty(id, USER_TASK_OWNER, owner, infoNode);
    }

    @Override
    public ObjectNode changeUserTaskCandidateUser(String id, String candidateUser, boolean overwriteOtherChangedEntries) {
        ObjectNode infoNode = processEngineConfiguration.getObjectMapper().createObjectNode();
        changeUserTaskCandidateUser(id, candidateUser, overwriteOtherChangedEntries, infoNode);
        return infoNode;
    }

    @Override
    public void changeUserTaskCandidateUser(String id, String candidateUser, boolean overwriteOtherChangedEntries, ObjectNode infoNode) {
        ArrayNode valuesNode = null;
        if (overwriteOtherChangedEntries) {
            valuesNode = processEngineConfiguration.getObjectMapper().createArrayNode();
        } else {
            if (doesElementPropertyExist(id, USER_TASK_CANDIDATE_USERS, infoNode)) {
                valuesNode = (ArrayNode) infoNode.get(BPMN_NODE).get(id).get(USER_TASK_CANDIDATE_USERS);
            }

            if (valuesNode == null || valuesNode.isNull()) {
                valuesNode = processEngineConfiguration.getObjectMapper().createArrayNode();
            }
        }

        valuesNode.add(candidateUser);
        setElementProperty(id, USER_TASK_CANDIDATE_USERS, valuesNode, infoNode);
    }

    @Override
    public ObjectNode changeUserTaskCandidateGroup(String id, String candidateGroup, boolean overwriteOtherChangedEntries) {
        ObjectNode infoNode = processEngineConfiguration.getObjectMapper().createObjectNode();
        changeUserTaskCandidateGroup(id, candidateGroup, overwriteOtherChangedEntries, infoNode);
        return infoNode;
    }

    @Override
    public void changeUserTaskCandidateGroup(String id, String candidateGroup, boolean overwriteOtherChangedEntries, ObjectNode infoNode) {
        ArrayNode valuesNode = null;
        if (overwriteOtherChangedEntries) {
            valuesNode = processEngineConfiguration.getObjectMapper().createArrayNode();
        } else {
            if (doesElementPropertyExist(id, USER_TASK_CANDIDATE_GROUPS, infoNode)) {
                valuesNode = (ArrayNode) infoNode.get(BPMN_NODE).get(id).get(USER_TASK_CANDIDATE_GROUPS);
            }

            if (valuesNode == null || valuesNode.isNull()) {
                valuesNode = processEngineConfiguration.getObjectMapper().createArrayNode();
            }
        }

        valuesNode.add(candidateGroup);
        setElementProperty(id, USER_TASK_CANDIDATE_GROUPS, valuesNode, infoNode);
    }

    @Override
    public ObjectNode changeUserTaskCandidateUsers(String id, List<String> candidateUsers) {
        ObjectNode infoNode = processEngineConfiguration.getObjectMapper().createObjectNode();
        changeUserTaskCandidateUsers(id, candidateUsers, infoNode);
        return infoNode;
    }

    @Override
    public void changeUserTaskCandidateUsers(String id, List<String> candidateUsers, ObjectNode infoNode) {
        ArrayNode candidateUsersNode = processEngineConfiguration.getObjectMapper().createArrayNode();
        for (String candidateUser : candidateUsers) {
            candidateUsersNode.add(candidateUser);
        }
        setElementProperty(id, USER_TASK_CANDIDATE_USERS, candidateUsersNode, infoNode);
    }

    @Override
    public ObjectNode changeUserTaskCandidateGroups(String id, List<String> candidateGroups) {
        ObjectNode infoNode = processEngineConfiguration.getObjectMapper().createObjectNode();
        changeUserTaskCandidateGroups(id, candidateGroups, infoNode);
        return infoNode;
    }

    @Override
    public void changeUserTaskCandidateGroups(String id, List<String> candidateGroups, ObjectNode infoNode) {
        ArrayNode candidateGroupsNode = processEngineConfiguration.getObjectMapper().createArrayNode();
        for (String candidateGroup : candidateGroups) {
            candidateGroupsNode.add(candidateGroup);
        }
        setElementProperty(id, USER_TASK_CANDIDATE_GROUPS, candidateGroupsNode, infoNode);
    }

    @Override
    public ObjectNode changeSequenceFlowCondition(String id, String condition) {
        ObjectNode infoNode = processEngineConfiguration.getObjectMapper().createObjectNode();
        changeSequenceFlowCondition(id, condition, infoNode);
        return infoNode;
    }

    @Override
    public void changeSequenceFlowCondition(String id, String condition, ObjectNode infoNode) {
        setElementProperty(id, SEQUENCE_FLOW_CONDITION, condition, infoNode);
    }

    @Override
    public ObjectNode getBpmnElementProperties(String id, ObjectNode infoNode) {
        ObjectNode propertiesNode = null;
        ObjectNode bpmnNode = getBpmnNode(infoNode);
        if (bpmnNode != null) {
            propertiesNode = (ObjectNode) bpmnNode.get(id);
        }
        return propertiesNode;
    }

    @Override
    public ObjectNode changeLocalizationName(String language, String id, String value) {
        ObjectNode infoNode = processEngineConfiguration.getObjectMapper().createObjectNode();
        changeLocalizationName(language, id, value, infoNode);
        return infoNode;
    }

    @Override
    public void changeLocalizationName(String language, String id, String value, ObjectNode infoNode) {
        setLocalizationProperty(language, id, LOCALIZATION_NAME, value, infoNode);
    }

    @Override
    public ObjectNode changeLocalizationDescription(String language, String id, String value) {
        ObjectNode infoNode = processEngineConfiguration.getObjectMapper().createObjectNode();
        changeLocalizationDescription(language, id, value, infoNode);
        return infoNode;
    }

    @Override
    public void changeLocalizationDescription(String language, String id, String value, ObjectNode infoNode) {
        setLocalizationProperty(language, id, LOCALIZATION_DESCRIPTION, value, infoNode);
    }

    @Override
    public ObjectNode getLocalizationElementProperties(String language, String id, ObjectNode infoNode) {
        ObjectNode propertiesNode = null;
        ObjectNode localizationNode = getLocalizationNode(infoNode);
        if (localizationNode != null) {
            JsonNode languageNode = localizationNode.get(language);
            if (languageNode != null) {
                propertiesNode = (ObjectNode) languageNode.get(id);
            }
        }
        return propertiesNode;
    }

    @Override
    public void resetProperty(String elementId, String property, ObjectNode infoNode) {
        ObjectNode path = (ObjectNode) infoNode.path(BPMN_NODE).path(elementId);
        if (!path.isMissingNode()) {
            path.remove(property);
        }
    }

    @Override
    public DynamicProcessDefinitionSummary getDynamicProcessDefinitionSummary(String processDefinitionId) {
        ObjectNode infoNode = getProcessDefinitionInfo(processDefinitionId);
        ObjectMapper objectMapper = processEngineConfiguration.getObjectMapper();
        BpmnModel bpmnModel = commandExecutor.execute(new GetBpmnModelCmd(processDefinitionId));

        // aggressive exception. this method should not be called if the process definition does not exists.
        if (bpmnModel == null) {
            throw new ActivitiException("ProcessDefinition " + processDefinitionId + " does not exists");
        }

        // to avoid redundant null checks we create an new node
        if (infoNode == null) {
            infoNode = processEngineConfiguration.getObjectMapper().createObjectNode();
            createOrGetBpmnNode(infoNode);
        }

        return new DynamicProcessDefinitionSummary(bpmnModel, infoNode, objectMapper);
    }

    protected boolean doesElementPropertyExist(String id, String propertyName, ObjectNode infoNode) {
        boolean exists = false;
        if (infoNode.get(BPMN_NODE) != null && infoNode.get(BPMN_NODE).get(id) != null && infoNode.get(BPMN_NODE).get(id).get(propertyName) != null) {
            JsonNode propNode = infoNode.get(BPMN_NODE).get(id).get(propertyName);
            if (!propNode.isNull()) {
                exists = true;
            }
        }
        return exists;
    }

    protected void setElementProperty(String id, String propertyName, String propertyValue, ObjectNode infoNode) {
        ObjectNode bpmnNode = createOrGetBpmnNode(infoNode);
        if (!bpmnNode.has(id)) {
            bpmnNode.set(id, processEngineConfiguration.getObjectMapper().createObjectNode());
        }

        ((ObjectNode) bpmnNode.get(id)).put(propertyName, propertyValue);
    }

    protected void setElementProperty(String id, String propertyName, JsonNode propertyValue, ObjectNode infoNode) {
        ObjectNode bpmnNode = createOrGetBpmnNode(infoNode);
        if (!bpmnNode.has(id)) {
            bpmnNode.set(id, processEngineConfiguration.getObjectMapper().createObjectNode());
        }

        ((ObjectNode) bpmnNode.get(id)).replace(propertyName, propertyValue);
    }

    protected ObjectNode createOrGetBpmnNode(ObjectNode infoNode) {
        if (!infoNode.has(BPMN_NODE)) {
            infoNode.set(BPMN_NODE, processEngineConfiguration.getObjectMapper().createObjectNode());
        }
        return (ObjectNode) infoNode.get(BPMN_NODE);
    }

    protected ObjectNode getBpmnNode(ObjectNode infoNode) {
        return (ObjectNode) infoNode.get(BPMN_NODE);
    }

    protected void setLocalizationProperty(String language, String id, String propertyName, String propertyValue, ObjectNode infoNode) {
        ObjectNode localizationNode = createOrGetLocalizationNode(infoNode);
        if (!localizationNode.has(language)) {
            localizationNode.set(language, processEngineConfiguration.getObjectMapper().createObjectNode());
        }

        ObjectNode languageNode = (ObjectNode) localizationNode.get(language);
        if (!languageNode.has(id)) {
            languageNode.set(id, processEngineConfiguration.getObjectMapper().createObjectNode());
        }

        ((ObjectNode) languageNode.get(id)).put(propertyName, propertyValue);
    }

    protected ObjectNode createOrGetLocalizationNode(ObjectNode infoNode) {
        if (!infoNode.has(LOCALIZATION_NODE)) {
            infoNode.set(LOCALIZATION_NODE, processEngineConfiguration.getObjectMapper().createObjectNode());
        }
        return (ObjectNode) infoNode.get(LOCALIZATION_NODE);
    }

    protected ObjectNode getLocalizationNode(ObjectNode infoNode) {
        return (ObjectNode) infoNode.get(LOCALIZATION_NODE);
    }

}
