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
import org.flowable.common.engine.api.FlowableException;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.idm.api.User;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.ui.common.model.UserRepresentation;
import org.flowable.ui.common.security.SecurityUtils;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.flowable.ui.common.service.exception.NotFoundException;
import org.flowable.ui.common.service.exception.NotPermittedException;
import org.flowable.ui.task.model.runtime.TaskRepresentation;
import org.flowable.ui.task.service.api.UserCache.CachedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
@Service
@Transactional
public class FlowableTaskActionService extends FlowableAbstractTaskService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableTaskActionService.class);

    public void completeTask(String taskId) {
        User currentUser = SecurityUtils.getCurrentUserObject();
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        if (task == null) {
            throw new NotFoundException("Task with id: " + taskId + " does not exist");
        }

        if (!permissionService.isTaskOwnerOrAssignee(currentUser, task)) {
            if (StringUtils.isEmpty(task.getScopeType()) && !permissionService.validateIfUserIsInitiatorAndCanCompleteTask(currentUser, task)) {
                throw new NotPermittedException();
            }
        }

        try {
            if (StringUtils.isEmpty(task.getScopeType())) {
                taskService.complete(task.getId());
            } else {
                cmmnTaskService.complete(task.getId());
            }
            
        } catch (FlowableException e) {
            LOGGER.error("Error completing task {}", taskId, e);
            throw new BadRequestException("Task " + taskId + " can't be completed", e);
        }
    }

    public TaskRepresentation assignTask(String taskId, ObjectNode requestNode) {
        User currentUser = SecurityUtils.getCurrentUserObject();
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        if (task == null) {
            throw new NotFoundException("Task with id: " + taskId + " does not exist");
        }

        checkTaskPermissions(taskId, currentUser, task);

        if (requestNode.get("assignee") != null) {

            // This method can only be called by someone in a tenant. Check if the user is part of the tenant
            String assigneeIdString = requestNode.get("assignee").asText();

            CachedUser cachedUser = userCache.getUser(assigneeIdString);
            if (cachedUser == null) {
                throw new BadRequestException("Invalid assignee id");
            }
            assignTask(currentUser, task, assigneeIdString);

        } else {
            throw new BadRequestException("Assignee is required");
        }

        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        TaskRepresentation rep = new TaskRepresentation(task);
        fillPermissionInformation(rep, task, currentUser);

        populateAssignee(task, rep);
        rep.setInvolvedPeople(getInvolvedUsers(taskId));
        return rep;
    }

    public void involveUser(String taskId, ObjectNode requestNode) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        if (task == null) {
            throw new NotFoundException("Task with id: " + taskId + " does not exist");
        }

        User currentUser = SecurityUtils.getCurrentUserObject();
        permissionService.validateReadPermissionOnTask(currentUser, task.getId());

        if (requestNode.get("userId") != null) {
            String userId = requestNode.get("userId").asText();
            CachedUser user = userCache.getUser(userId);
            if (user == null) {
                throw new BadRequestException("Invalid user id");
            }
            taskService.addUserIdentityLink(taskId, userId, IdentityLinkType.PARTICIPANT);

        } else {
            throw new BadRequestException("User id is required");
        }

    }

    public void removeInvolvedUser(String taskId, ObjectNode requestNode) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        if (task == null) {
            throw new NotFoundException("Task with id: " + taskId + " does not exist");
        }

        permissionService.validateReadPermissionOnTask(SecurityUtils.getCurrentUserObject(), task.getId());

        String assigneeString = null;
        if (requestNode.get("userId") != null) {
            String userId = requestNode.get("userId").asText();
            if (userCache.getUser(userId) == null) {
                throw new BadRequestException("Invalid user id");
            }
            assigneeString = String.valueOf(userId);

        } else if (requestNode.get("email") != null) {

            String email = requestNode.get("email").asText();
            assigneeString = email;

        } else {
            throw new BadRequestException("User id or email is required");
        }

        taskService.deleteUserIdentityLink(taskId, assigneeString, IdentityLinkType.PARTICIPANT);
    }

    public void claimTask(String taskId) {

        User currentUser = SecurityUtils.getCurrentUserObject();
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        if (task == null) {
            throw new NotFoundException("Task with id: " + taskId + " does not exist");
        }

        permissionService.validateReadPermissionOnTask(currentUser, task.getId());

        try {
            taskService.claim(task.getId(), String.valueOf(currentUser.getId()));
        } catch (FlowableException e) {
            throw new BadRequestException("Task " + taskId + " can't be claimed", e);
        }
    }

    protected void checkTaskPermissions(String taskId, User currentUser, Task task) {
        permissionService.validateReadPermissionOnTask(currentUser, task.getId());
    }

    protected String validateEmail(ObjectNode requestNode) {
        String email = requestNode.get("email") != null ? requestNode.get("email").asText() : null;
        if (email == null) {
            throw new BadRequestException("Email is mandatory");
        }
        return email;
    }

    protected void assignTask(User currentUser, Task task, String assigneeIdString) {
        try {
            String oldAssignee = task.getAssignee();
            taskService.setAssignee(task.getId(), assigneeIdString);

            // If the old assignee user wasn't part of the involved users yet, make it so
            addIdentiyLinkForUser(task, oldAssignee, IdentityLinkType.PARTICIPANT);

            // If the current user wasn't part of the involved users yet, make it so
            String currentUserIdString = String.valueOf(currentUser.getId());
            addIdentiyLinkForUser(task, currentUserIdString, IdentityLinkType.PARTICIPANT);

        } catch (FlowableException e) {
            throw new BadRequestException("Task " + task.getId() + " can't be assigned", e);
        }
    }

    protected void addIdentiyLinkForUser(Task task, String userId, String linkType) {
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
        boolean isOldUserInvolved = false;
        for (IdentityLink identityLink : identityLinks) {
            if (userId.equals(identityLink.getUserId()) && (identityLink.getType().equals(IdentityLinkType.PARTICIPANT) || identityLink.getType().equals(IdentityLinkType.CANDIDATE))) {
                isOldUserInvolved = true;
            }
        }
        if (!isOldUserInvolved) {
            taskService.addUserIdentityLink(task.getId(), userId, linkType);
        }
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
}
