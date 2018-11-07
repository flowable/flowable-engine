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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.idm.api.User;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.ui.common.model.UserRepresentation;
import org.flowable.ui.common.security.SecurityUtils;
import org.flowable.ui.common.service.exception.NotFoundException;
import org.flowable.ui.task.model.runtime.TaskRepresentation;
import org.flowable.ui.task.model.runtime.TaskUpdateRepresentation;
import org.flowable.ui.task.service.api.UserCache.CachedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Tijs Rademakers
 */
@Service
@Transactional
public class FlowableTaskService extends FlowableAbstractTaskService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableTaskService.class);

    public TaskRepresentation getTask(String taskId) {
        User currentUser = SecurityUtils.getCurrentUserObject();
        HistoricTaskInstance task = permissionService.validateReadPermissionOnTask(currentUser, taskId);

        TaskRepresentation rep = null;
        if (StringUtils.isNotEmpty(task.getProcessDefinitionId())) {
            try {
                ProcessDefinition processDefinition = repositoryService.getProcessDefinition(task.getProcessDefinitionId());
                rep = new TaskRepresentation(task, processDefinition);

            } catch (FlowableException e) {
                LOGGER.error("Error getting process definition {}", task.getProcessDefinitionId(), e);
            }

        } else if (StringUtils.isNotEmpty(task.getScopeDefinitionId())) {
            try {
                CaseDefinition caseDefinition = cmmnRepositoryService.getCaseDefinition(task.getScopeDefinitionId());
                rep = new TaskRepresentation(task, caseDefinition);

            } catch (FlowableException e) {
                LOGGER.error("Error getting case definition {}", task.getScopeDefinitionId(), e);
            }
            
        } else if (StringUtils.isNotEmpty(task.getParentTaskId())) {
            HistoricTaskInstance parentTask = permissionService.validateReadPermissionOnTask(currentUser, task.getParentTaskId());
            rep = new TaskRepresentation(task, parentTask);
        } else {
            rep = new TaskRepresentation(task);
        }

        fillPermissionInformation(rep, task, currentUser);

        // Populate the people
        populateAssignee(task, rep);
        rep.setInvolvedPeople(getInvolvedUsers(taskId));

        return rep;
    }

    public List<TaskRepresentation> getSubTasks(String taskId) {
        User currentUser = SecurityUtils.getCurrentUserObject();
        HistoricTaskInstance parentTask = permissionService.validateReadPermissionOnTask(currentUser, taskId);
        
        List<Task> subTasks = this.taskService.getSubTasks(taskId);
        List<TaskRepresentation> subTasksRepresentations = new ArrayList<>(subTasks.size());
        for (Task subTask : subTasks) {
            TaskRepresentation representation = new TaskRepresentation(subTask, parentTask);
            
            fillPermissionInformation(representation, subTask, currentUser);
            populateAssignee(subTask, representation);
            representation.setInvolvedPeople(getInvolvedUsers(subTask.getId()));
            
            subTasksRepresentations.add(representation);
        }
        
        return subTasksRepresentations;
    }

    protected void populateAssignee(TaskInfo task, TaskRepresentation rep) {
        if (task.getAssignee() != null) {
            CachedUser cachedUser = userCache.getUser(task.getAssignee());
            if (cachedUser != null && cachedUser.getUser() != null) {
                rep.setAssignee(new UserRepresentation(cachedUser.getUser()));
            }
        }
    }

    protected List<UserRepresentation> getInvolvedUsers(String taskId) {
        List<HistoricIdentityLink> idLinks = historyService.getHistoricIdentityLinksForTask(taskId);
        List<UserRepresentation> result = new ArrayList<>(idLinks.size());

        for (HistoricIdentityLink link : idLinks) {
            // Only include users and non-assignee links
            if (link.getUserId() != null && !IdentityLinkType.ASSIGNEE.equals(link.getType())) {
                CachedUser cachedUser = userCache.getUser(link.getUserId());
                if (cachedUser != null && cachedUser.getUser() != null) {
                    result.add(new UserRepresentation(cachedUser.getUser()));
                }
            }
        }
        return result;
    }

    public TaskRepresentation updateTask(String taskId, TaskUpdateRepresentation updated) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        if (task == null) {
            throw new NotFoundException("Task with id: " + taskId + " does not exist");
        }

        permissionService.validateReadPermissionOnTask(SecurityUtils.getCurrentUserObject(), task.getId());

        if (updated.isNameSet()) {
            task.setName(updated.getName());
        }

        if (updated.isDescriptionSet()) {
            task.setDescription(updated.getDescription());
        }

        if (updated.isDueDateSet()) {
            task.setDueDate(updated.getDueDate());
        }

        taskService.saveTask(task);

        return new TaskRepresentation(task);
    }
}
