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
package org.flowable.ui.task.service.runtime;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.identitylink.service.IdentityLinkType;
import org.flowable.idm.api.User;
import org.flowable.task.api.TaskInfo;
import org.flowable.ui.common.model.RemoteGroup;
import org.flowable.ui.common.service.idm.RemoteIdmService;
import org.flowable.ui.task.model.runtime.TaskRepresentation;
import org.flowable.ui.task.service.api.UserCache;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class FlowableAbstractTaskService {

    @Autowired
    protected RemoteIdmService remoteIdmService;

    @Autowired
    protected RepositoryService repositoryService;
    
    @Autowired
    protected CmmnRepositoryService cmmnRepositoryService;

    @Autowired
    protected TaskService taskService;
    
    @Autowired
    protected CmmnTaskService cmmnTaskService;
    
    @Autowired
    protected HistoryService historyService;
    
    @Autowired
    protected UserCache userCache;
    
    @Autowired
    protected PermissionService permissionService;

    public void fillPermissionInformation(TaskRepresentation taskRepresentation, TaskInfo task, User currentUser) {
        verifyProcessInstanceStartUser(taskRepresentation, task);

        List<HistoricIdentityLink> taskIdentityLinks = historyService.getHistoricIdentityLinksForTask(task.getId());
        verifyCandidateGroups(taskRepresentation, currentUser, taskIdentityLinks);
        verifyCandidateUsers(taskRepresentation, currentUser, taskIdentityLinks);
    }

    protected void verifyProcessInstanceStartUser(TaskRepresentation taskRepresentation, TaskInfo task) {
        if (task.getProcessInstanceId() != null) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
            if (historicProcessInstance != null && StringUtils.isNotEmpty(historicProcessInstance.getStartUserId())) {
                taskRepresentation.setProcessInstanceStartUserId(historicProcessInstance.getStartUserId());
                BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
                FlowElement flowElement = bpmnModel.getFlowElement(task.getTaskDefinitionKey());
                if (flowElement instanceof UserTask) {
                    UserTask userTask = (UserTask) flowElement;
                    List<ExtensionElement> extensionElements = userTask.getExtensionElements().get("initiator-can-complete");
                    if (CollectionUtils.isNotEmpty(extensionElements)) {
                        String value = extensionElements.get(0).getElementText();
                        if (StringUtils.isNotEmpty(value)) {
                            taskRepresentation.setInitiatorCanCompleteTask(Boolean.valueOf(value));
                        }
                    }
                }
            }
        }
    }
    
    protected void verifyCandidateGroups(TaskRepresentation taskRepresentation, User currentUser, List<HistoricIdentityLink> taskIdentityLinks) {
        List<RemoteGroup> userGroups = remoteIdmService.getUser(currentUser.getId()).getGroups();
        taskRepresentation.setMemberOfCandidateGroup(userGroupsMatchTaskCandidateGroups(userGroups, taskIdentityLinks));
    }
    
    protected boolean userGroupsMatchTaskCandidateGroups(List<RemoteGroup> userGroups, List<HistoricIdentityLink> taskIdentityLinks) {
        for (RemoteGroup group : userGroups) {
            for (HistoricIdentityLink identityLink : taskIdentityLinks) {
                if (identityLink.getGroupId() != null 
                        && identityLink.getType().equals(IdentityLinkType.CANDIDATE) 
                        && group.getId().equals(identityLink.getGroupId())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    protected void verifyCandidateUsers(TaskRepresentation taskRepresentation, User currentUser, List<HistoricIdentityLink> taskIdentityLinks) {
        taskRepresentation.setMemberOfCandidateUsers(currentUserMatchesTaskCandidateUsers(currentUser, taskIdentityLinks));
    }
    
    protected boolean currentUserMatchesTaskCandidateUsers(User currentUser, List<HistoricIdentityLink> taskIdentityLinks) {
        for (HistoricIdentityLink identityLink : taskIdentityLinks) {
            if (identityLink.getUserId() != null
                    && identityLink.getType().equals(IdentityLinkType.CANDIDATE)
                    && identityLink.getUserId().equals(currentUser.getId())) {
                return true;
            }
        }
        return false;
    }

}
