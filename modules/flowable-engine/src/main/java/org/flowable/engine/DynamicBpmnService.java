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

package org.flowable.engine;

import java.util.List;

import org.flowable.engine.dynamic.DynamicProcessDefinitionSummary;
import org.flowable.engine.impl.dynamic.DynamicEmbeddedSubProcessBuilder;
import org.flowable.engine.impl.dynamic.DynamicUserTaskBuilder;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Service providing access to the repository of process definitions and deployments.
 * 
 * @author Tijs Rademakers
 */
public interface DynamicBpmnService {
    
    void injectUserTaskInProcessInstance(String processInstanceId, DynamicUserTaskBuilder dynamicUserTaskBuilder);

    void injectParallelUserTask(String taskId, DynamicUserTaskBuilder dynamicUserTaskBuilder);
    
    void injectEmbeddedSubProcessInProcessInstance(String processInstanceId, DynamicEmbeddedSubProcessBuilder dynamicEmbeddedSubProcessBuilder);

    void injectParallelEmbeddedSubProcess(String taskId, DynamicEmbeddedSubProcessBuilder dynamicEmbeddedSubProcessBuilder);

    ObjectNode getProcessDefinitionInfo(String processDefinitionId);

    void saveProcessDefinitionInfo(String processDefinitionId, ObjectNode infoNode);

    ObjectNode changeServiceTaskClassName(String id, String className);

    void changeServiceTaskClassName(String id, String className, ObjectNode infoNode);

    ObjectNode changeServiceTaskExpression(String id, String expression);

    void changeServiceTaskExpression(String id, String expression, ObjectNode infoNode);

    ObjectNode changeServiceTaskDelegateExpression(String id, String expression);

    void changeServiceTaskDelegateExpression(String id, String expression, ObjectNode infoNode);

    ObjectNode changeScriptTaskScript(String id, String script);

    void changeScriptTaskScript(String id, String script, ObjectNode infoNode);
    
    ObjectNode changeSkipExpression(String id, String skipExpression);

    void changeSkipExpression(String id, String skipExpression, ObjectNode infoNode);
    
    void removeSkipExpression(String id, ObjectNode infoNode);
    
    ObjectNode enableSkipExpression();
    
    void enableSkipExpression(ObjectNode infoNode);
    
    void removeEnableSkipExpression(ObjectNode infoNode);

    ObjectNode changeUserTaskName(String id, String name);

    void changeUserTaskName(String id, String name, ObjectNode infoNode);

    ObjectNode changeUserTaskDescription(String id, String description);

    void changeUserTaskDescription(String id, String description, ObjectNode infoNode);

    ObjectNode changeUserTaskDueDate(String id, String dueDate);

    void changeUserTaskDueDate(String id, String dueDate, ObjectNode infoNode);

    ObjectNode changeUserTaskPriority(String id, String priority);

    void changeUserTaskPriority(String id, String priority, ObjectNode infoNode);

    ObjectNode changeUserTaskCategory(String id, String category);

    void changeUserTaskCategory(String id, String category, ObjectNode infoNode);

    ObjectNode changeUserTaskFormKey(String id, String formKey);

    void changeUserTaskFormKey(String id, String formKey, ObjectNode infoNode);

    ObjectNode changeUserTaskAssignee(String id, String assignee);

    void changeUserTaskAssignee(String id, String assignee, ObjectNode infoNode);

    ObjectNode changeUserTaskOwner(String id, String owner);

    void changeUserTaskOwner(String id, String owner, ObjectNode infoNode);

    ObjectNode changeUserTaskCandidateUser(String id, String candidateUser, boolean overwriteOtherChangedEntries);

    void changeUserTaskCandidateUser(String id, String candidateUser, boolean overwriteOtherChangedEntries, ObjectNode infoNode);

    ObjectNode changeUserTaskCandidateGroup(String id, String candidateGroup, boolean overwriteOtherChangedEntries);

    void changeUserTaskCandidateGroup(String id, String candidateGroup, boolean overwriteOtherChangedEntries, ObjectNode infoNode);

    /**
     * Creates a new processDefinitionInfo with {@link DynamicBpmnConstants#USER_TASK_CANDIDATE_USERS} for the given BPMN element.
     *
     * <span style="color:red">
     * Don't forget to call {@link DynamicBpmnService#saveProcessDefinitionInfo(String, ObjectNode)}
     * </span>
     *
     *
     * @param id
     *            the bpmn element id (ex. sid-3392FDEE-DD6F-484E-97FE-55F30BFEA77E)
     * @param candidateUsers
     *            the candidate users.
     * @return a new processDefinitionNode with the candidate users for the given bpmn element.
     */
    ObjectNode changeUserTaskCandidateUsers(String id, List<String> candidateUsers);

    /**
     * Updates a processDefinitionInfo's {@link DynamicBpmnConstants#USER_TASK_CANDIDATE_USERS} with the new list. Previous values for the BPMN Element with
     * {@link DynamicBpmnConstants#USER_TASK_CANDIDATE_USERS} as key are ignored.
     *
     * <span style="color:red">
     * Don't forget to call {@link DynamicBpmnService#saveProcessDefinitionInfo(String, ObjectNode)}
     * </span>
     *
     * @param id
     *            the bpmn element id (ex. sid-3392FDEE-DD6F-484E-97FE-55F30BFEA77E)
     * @param candidateUsers
     *            the candidate users.
     * @param infoNode
     *            the current processDefinitionInfo. This object will be modified.
     */
    void changeUserTaskCandidateUsers(String id, List<String> candidateUsers, ObjectNode infoNode);

    /**
     * Creates a new processDefinitionInfo with {@link DynamicBpmnConstants#USER_TASK_CANDIDATE_USERS} for the given BPMN element.
     *
     * <span style="color:red">
     * Don't forget to call {@link DynamicBpmnService#saveProcessDefinitionInfo(String, ObjectNode)}
     * </span>
     *
     * @param id
     *            the bpmn element id (ex. sid-3392FDEE-DD6F-484E-97FE-55F30BFEA77E)
     * @param candidateGroups
     *            the candidate groups.
     * @return a new processDefinitionNode with the candidate users for the given bpmn element.
     */
    ObjectNode changeUserTaskCandidateGroups(String id, List<String> candidateGroups);

    /**
     * Updates a processDefinitionInfo's {@link DynamicBpmnConstants#USER_TASK_CANDIDATE_USERS} with the new list. Previous values for the BPMN Element with
     * {@link DynamicBpmnConstants#USER_TASK_CANDIDATE_USERS} as key are ignored.
     *
     * <span style="color:red">
     * Don't forget to call {@link DynamicBpmnService#saveProcessDefinitionInfo(String, ObjectNode)}
     * </span>
     *
     * @param id
     *            the bpmn element id (ex. sid-3392FDEE-DD6F-484E-97FE-55F30BFEA77E)
     * @param candidateGroups
     *            the candidate groups.
     * @param infoNode
     *            the current processDefinitionInfo. This object will be modified.
     */
    void changeUserTaskCandidateGroups(String id, List<String> candidateGroups, ObjectNode infoNode);
    
    ObjectNode changeMultiInstanceCompletionCondition(String id, String completionCondition);

    void changeMultiInstanceCompletionCondition(String id, String completionCondition, ObjectNode infoNode);

    ObjectNode changeDmnTaskDecisionTableKey(String id, String decisionTableKey);

    void changeDmnTaskDecisionTableKey(String id, String decisionTableKey, ObjectNode infoNode);

    ObjectNode changeSequenceFlowCondition(String id, String condition);

    void changeSequenceFlowCondition(String id, String condition, ObjectNode infoNode);

    ObjectNode changeCallActivityCalledElement(String id, String calledElement);

    void changeCallActivityCalledElement(String id, String calledElement, ObjectNode infoNode);

    ObjectNode getBpmnElementProperties(String id, ObjectNode infoNode);

    ObjectNode changeLocalizationName(String language, String id, String value);

    void changeLocalizationName(String language, String id, String value, ObjectNode infoNode);

    ObjectNode changeLocalizationDescription(String language, String id, String value);

    void changeLocalizationDescription(String language, String id, String value, ObjectNode infoNode);

    ObjectNode getLocalizationElementProperties(String language, String id, ObjectNode infoNode);

    /**
     * <p>
     * Clears the field from the infoNode. So the engine uses the {@link org.flowable.bpmn.model.BpmnModel} value On next instance.
     * </p>
     *
     * <span style="color:red">
     * Don't forget to save the modified infoNode by calling {@link DynamicBpmnService#saveProcessDefinitionInfo(String, ObjectNode)}
     * </span>
     *
     * @param elementId
     *            the flow elements id.
     * @param property
     *            {@link DynamicBpmnConstants} property
     * @param infoNode
     *            to modify
     */
    void resetProperty(String elementId, String property, ObjectNode infoNode);

    /**
     * Gives a summary between the {@link org.flowable.bpmn.model.BpmnModel} and {@link DynamicBpmnService#getProcessDefinitionInfo(String)}
     *
     * @param processDefinitionId
     *            the process definition id (key:version:sequence)
     * @return DynamicProcessDefinitionSummary if the processdefinition exists
     * @throws IllegalStateException
     *             if there is no processDefinition found for the provided processDefinitionId.
     */
    DynamicProcessDefinitionSummary getDynamicProcessDefinitionSummary(String processDefinitionId);
}
