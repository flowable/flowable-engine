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
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.app.service.exception.NotFoundException;
import org.flowable.app.service.exception.NotPermittedException;
import org.flowable.app.service.idm.RemoteIdmService;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.content.api.ContentItem;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.history.HistoricTaskInstance;
import org.flowable.engine.history.HistoricTaskInstanceQuery;
import org.flowable.engine.task.Task;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Centralized service for all permission-checks.
 * 
 * @author Frederik Heremans
 */
@Service
@Transactional
public class PermissionService {

  @Autowired
  protected TaskService taskService;

  @Autowired
  protected RuntimeService runtimeService;

  @Autowired
  protected RepositoryService repositoryService;

  @Autowired
  protected HistoryService historyService;
  
  @Autowired
  protected RemoteIdmService remoteIdmService;

  /**
   * Check if the given user is allowed to read the task.
   */
  public HistoricTaskInstance validateReadPermissionOnTask(User user, String taskId) {

    List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery().taskId(taskId).taskInvolvedUser(String.valueOf(user.getId())).list();

    if (CollectionUtils.isNotEmpty(tasks)) {
      return tasks.get(0);
    }

    // Task is maybe accessible through groups of user
    HistoricTaskInstanceQuery historicTaskInstanceQuery = historyService.createHistoricTaskInstanceQuery();
    historicTaskInstanceQuery.taskId(taskId);

    List<String> groupIds = getGroupIdsForUser(user);
    if (!groupIds.isEmpty()) {
      historicTaskInstanceQuery.taskCandidateGroupIn(getGroupIdsForUser(user));
    }

    tasks = historicTaskInstanceQuery.list();
    if (CollectionUtils.isNotEmpty(tasks)) {
      return tasks.get(0);
    }

    // Last resort: user has access to proc inst -> can see task
    tasks = historyService.createHistoricTaskInstanceQuery().taskId(taskId).list();
    if (CollectionUtils.isNotEmpty(tasks)) {
      HistoricTaskInstance task = tasks.get(0);
      if (task != null && task.getProcessInstanceId() != null) {
        boolean hasReadPermissionOnProcessInstance = hasReadPermissionOnProcessInstance(user, task.getProcessInstanceId());
        if (hasReadPermissionOnProcessInstance) {
          return task;
        }
      }
    }
    throw new NotPermittedException("User is not allowed to work with task " + taskId);
  }

  private List<String> getGroupIdsForUser(User user) {
    List<String> groupIds = new ArrayList<String>();
    for (Group group : remoteIdmService.getUser(user.getId()).getGroups()) {
      groupIds.add(String.valueOf(group.getId()));
    }
    return groupIds;
  }

  public boolean isTaskOwnerOrAssignee(User user, String taskId) {
    return isTaskOwnerOrAssignee(user, taskService.createTaskQuery().taskId(taskId).singleResult());
  }

  public boolean isTaskOwnerOrAssignee(User user, Task task) {
    String currentUser = String.valueOf(user.getId());
    return currentUser.equals(task.getAssignee()) || currentUser.equals(task.getOwner());
  }

  public boolean validateIfUserIsInitiatorAndCanCompleteTask(User user, Task task) {
    boolean canCompleteTask = false;
    if (task.getProcessInstanceId() != null) {
      HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
      if (historicProcessInstance != null && StringUtils.isNotEmpty(historicProcessInstance.getStartUserId())) {
        String processInstanceStartUserId = historicProcessInstance.getStartUserId();
        if (String.valueOf(user.getId()).equals(processInstanceStartUserId)) {
          BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
          FlowElement flowElement = bpmnModel.getFlowElement(task.getTaskDefinitionKey());
          if (flowElement instanceof UserTask) {
            UserTask userTask = (UserTask) flowElement;
            List<ExtensionElement> extensionElements = userTask.getExtensionElements().get("initiator-can-complete");
            if (CollectionUtils.isNotEmpty(extensionElements)) {
              String value = extensionElements.get(0).getElementText();
              if (StringUtils.isNotEmpty(value) && Boolean.valueOf(value)) {
                canCompleteTask = true;
              }
            }
          }
        }
      }
    }
    return canCompleteTask;
  }

  public boolean isInvolved(User user, String taskId) {
    return historyService.createHistoricTaskInstanceQuery().taskId(taskId).taskInvolvedUser(String.valueOf(user.getId())).count() == 1;
  }

  /**
   * Check if the given user is allowed to read the process instance.
   */
  public boolean hasReadPermissionOnProcessInstance(User user, String processInstanceId) {
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
    return hasReadPermissionOnProcessInstance(user, historicProcessInstance, processInstanceId);
  }

  /**
   * Check if the given user is allowed to read the process instance.
   */
  public boolean hasReadPermissionOnProcessInstance(User user, HistoricProcessInstance historicProcessInstance, String processInstanceId) {
    if (historicProcessInstance == null) {
      throw new NotFoundException("Process instance not found for id " + processInstanceId);
    }

    // Start user check
    if (historicProcessInstance.getStartUserId() != null && historicProcessInstance.getStartUserId().equals(user.getId())) {
      return true;
    }

    // check if the user is involved in the task
    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
    historicProcessInstanceQuery.processInstanceId(processInstanceId);
    historicProcessInstanceQuery.involvedUser(user.getId());
    if (historicProcessInstanceQuery.count() > 0) {
      return true;
    }

    // Visibility: check if there are any tasks for the current user
    HistoricTaskInstanceQuery historicTaskInstanceQuery = historyService.createHistoricTaskInstanceQuery();
    historicTaskInstanceQuery.processInstanceId(processInstanceId);
    historicTaskInstanceQuery.taskInvolvedUser(user.getId());
    if (historicTaskInstanceQuery.count() > 0) {
      return true;
    }

    List<String> groupIds = getGroupIdsForUser(user);
    if (!groupIds.isEmpty()) {
      historicTaskInstanceQuery = historyService.createHistoricTaskInstanceQuery();
      historicTaskInstanceQuery.processInstanceId(processInstanceId).taskCandidateGroupIn(groupIds);
      return historicTaskInstanceQuery.count() > 0;
    }

    return false;
  }

  public boolean canAddRelatedContentToTask(User user, String taskId) {
    validateReadPermissionOnTask(user, taskId);
    return true;
  }

  public boolean canAddRelatedContentToProcessInstance(User user, String processInstanceId) {
    return hasReadPermissionOnProcessInstance(user, processInstanceId);
  }

  public boolean canDownloadContent(User currentUserObject, ContentItem content) {
    if (content.getTaskId() != null) {
      validateReadPermissionOnTask(currentUserObject, content.getTaskId());
      return true;
    } else if (content.getProcessInstanceId() != null) {
      return hasReadPermissionOnProcessInstance(currentUserObject, content.getProcessInstanceId());
    } else {
      return false;
    }
  }

  public boolean hasWritePermissionOnRelatedContent(User user, ContentItem content) {
    if (content.getProcessInstanceId() != null) {
      return hasReadPermissionOnProcessInstance(user, content.getProcessInstanceId());
    } else {
      if (content.getCreatedBy() != null) {
        return content.getCreatedBy().equals(user.getId());
      } else {
        return false;
      }
    }
  }

  public boolean canDeleteProcessInstance(User currentUser, HistoricProcessInstance processInstance) {
    boolean canDelete = false;
    if (processInstance.getStartUserId() != null) {
      try {
        Long starterId = Long.parseLong(processInstance.getStartUserId());
        canDelete = starterId.equals(currentUser.getId());
      } catch (NumberFormatException nfe) {
        // Ignore illegal starter id value
      }
    }

    return canDelete;
  }

}
