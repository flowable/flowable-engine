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
package org.flowable.app.service.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.flowable.app.model.runtime.TaskRepresentation;
import org.flowable.app.service.api.UserCache;
import org.flowable.app.service.idm.RemoteIdmService;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.task.TaskInfo;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.variable.service.history.HistoricVariableInstance;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class FlowableAbstractTaskService {

    @Autowired
    protected RemoteIdmService remoteIdmService;

    @Autowired
    protected HistoryService historyService;

    @Autowired
    protected RepositoryService repositoryService;

    @Autowired
    protected TaskService taskService;

    @Autowired
    protected UserCache userCache;

    @Autowired
    protected PermissionService permissionService;

    public void fillPermissionInformation(TaskRepresentation taskRepresentation, TaskInfo task, User currentUser) {

        String processInstanceStartUserId = null;
        boolean initiatorCanCompleteTask = true;
        boolean isMemberOfCandidateGroup = false;
        boolean isMemberOfCandidateUsers = false;

        if (task.getProcessInstanceId() != null) {

            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();

            if (historicProcessInstance != null && StringUtils.isNotEmpty(historicProcessInstance.getStartUserId())) {
                processInstanceStartUserId = historicProcessInstance.getStartUserId();
                BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
                FlowElement flowElement = bpmnModel.getFlowElement(task.getTaskDefinitionKey());
                if (flowElement instanceof UserTask) {
                    UserTask userTask = (UserTask) flowElement;
                    List<ExtensionElement> extensionElements = userTask.getExtensionElements().get("initiator-can-complete");
                    if (CollectionUtils.isNotEmpty(extensionElements)) {
                        String value = extensionElements.get(0).getElementText();
                        if (StringUtils.isNotEmpty(value)) {
                            initiatorCanCompleteTask = Boolean.valueOf(value);
                        }
                    }

                    Map<String, Object> variableMap = new HashMap<String, Object>();
                    if ((CollectionUtils.isNotEmpty(userTask.getCandidateGroups()) && userTask.getCandidateGroups().size() == 1
                            && userTask.getCandidateGroups().get(0).contains("${taskAssignmentBean.assignTaskToCandidateGroups('"))
                            || (CollectionUtils.isNotEmpty(userTask.getCandidateUsers()) && userTask.getCandidateUsers().size() == 1
                                    && userTask.getCandidateUsers().get(0).contains("${taskAssignmentBean.assignTaskToCandidateUsers('"))) {

                        List<HistoricVariableInstance> processVariables = historyService.createHistoricVariableInstanceQuery().processInstanceId(task.getProcessInstanceId()).list();
                        if (CollectionUtils.isNotEmpty(processVariables)) {
                            for (HistoricVariableInstance historicVariableInstance : processVariables) {
                                variableMap.put(historicVariableInstance.getVariableName(), historicVariableInstance.getValue());
                            }
                        }
                    }

                    if (CollectionUtils.isNotEmpty(userTask.getCandidateGroups())) {
                        List<? extends Group> groups = remoteIdmService.getUser(currentUser.getId()).getGroups();
                        if (CollectionUtils.isNotEmpty(groups)) {

                            List<String> groupIds = new ArrayList<String>();
                            if (userTask.getCandidateGroups().size() == 1 && userTask.getCandidateGroups().get(0).contains("${taskAssignmentBean.assignTaskToCandidateGroups('")) {

                                String candidateGroupString = userTask.getCandidateGroups().get(0);
                                candidateGroupString = candidateGroupString.replace("${taskAssignmentBean.assignTaskToCandidateGroups('", "");
                                candidateGroupString = candidateGroupString.replace("', execution)}", "");
                                String groupsArray[] = candidateGroupString.split(",");
                                for (String group : groupsArray) {
                                    if (group.contains("field(")) {
                                        String fieldCandidate = group.trim().substring(6, group.length() - 1);
                                        Object fieldValue = variableMap.get(fieldCandidate);
                                        if (fieldValue != null && NumberUtils.isNumber(fieldValue.toString())) {
                                            groupIds.add(fieldValue.toString());
                                        }

                                    } else {
                                        groupIds.add(group);
                                    }
                                }

                            } else {
                                groupIds.addAll(userTask.getCandidateGroups());
                            }

                            for (Group group : groups) {
                                if (groupIds.contains(String.valueOf(group.getId()))) {
                                    isMemberOfCandidateGroup = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (CollectionUtils.isNotEmpty(userTask.getCandidateUsers())) {
                        if (userTask.getCandidateUsers().size() == 1 && userTask.getCandidateUsers().get(0).contains("${taskAssignmentBean.assignTaskToCandidateUsers('")) {

                            String candidateUserString = userTask.getCandidateUsers().get(0);
                            candidateUserString = candidateUserString.replace("${taskAssignmentBean.assignTaskToCandidateUsers('", "");
                            candidateUserString = candidateUserString.replace("', execution)}", "");
                            String users[] = candidateUserString.split(",");
                            for (String user : users) {
                                if (user.contains("field(")) {
                                    String fieldCandidate = user.substring(6, user.length() - 1);
                                    Object fieldValue = variableMap.get(fieldCandidate);
                                    if (fieldValue != null && NumberUtils.isNumber(fieldValue.toString()) && String.valueOf(currentUser.getId()).equals(fieldValue.toString())) {

                                        isMemberOfCandidateGroup = true;
                                        break;
                                    }

                                } else if (user.equals(String.valueOf(currentUser.getId()))) {
                                    isMemberOfCandidateGroup = true;
                                    break;
                                }
                            }

                        } else if (userTask.getCandidateUsers().contains(String.valueOf(currentUser.getId()))) {
                            isMemberOfCandidateUsers = true;
                        }
                    }
                }
            }
        }

        taskRepresentation.setProcessInstanceStartUserId(processInstanceStartUserId);
        taskRepresentation.setInitiatorCanCompleteTask(initiatorCanCompleteTask);
        taskRepresentation.setMemberOfCandidateGroup(isMemberOfCandidateGroup);
        taskRepresentation.setMemberOfCandidateUsers(isMemberOfCandidateUsers);
    }

}
