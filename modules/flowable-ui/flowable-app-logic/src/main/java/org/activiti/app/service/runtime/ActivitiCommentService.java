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
package org.activiti.app.service.runtime;

import java.util.ArrayList;
import java.util.List;

import org.activiti.app.domain.runtime.Comment;
import org.activiti.app.model.common.ResultListDataRepresentation;
import org.activiti.app.model.runtime.CommentRepresentation;
import org.activiti.app.repository.runtime.CommentRepository;
import org.activiti.app.security.SecurityUtils;
import org.activiti.app.service.exception.BadRequestException;
import org.activiti.app.service.exception.NotFoundException;
import org.activiti.app.service.exception.NotPermittedException;
import org.activiti.engine.HistoryService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.runtime.Clock;
import org.activiti.idm.api.User;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Tijs Rademakers
 */
@Service
public class ActivitiCommentService {

  private static final Logger logger = LoggerFactory.getLogger(ActivitiCommentService.class);

  @Autowired
  protected PermissionService permissionService;

  @Autowired
  protected HistoryService historyService;

  @Autowired
  protected CommentRepository commentRepository;

  @Autowired
  protected Clock clock;

  public ResultListDataRepresentation getTaskComments(String taskId, Boolean latestFirst) {

    User currentUser = SecurityUtils.getCurrentUserObject();
    checkReadPermissionOnTask(currentUser, taskId);
    List<Comment> comments = getCommentsForTask(taskId, Boolean.TRUE.equals(latestFirst));

    // Create representation for all comments
    List<CommentRepresentation> commentList = new ArrayList<CommentRepresentation>();
    for (Comment comment : comments) {
      commentList.add(new CommentRepresentation(comment));
    }

    return new ResultListDataRepresentation(commentList);
  }

  public CommentRepresentation addTaskComment(CommentRepresentation commentRequest, String taskId) {

    if (StringUtils.isBlank(commentRequest.getMessage())) {
      throw new BadRequestException("Comment should not be empty");
    }

    HistoricTaskInstance task = historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult();
    if (task == null) {
      throw new NotFoundException("No task found with id: " + taskId);
    }

    // Check read permission and message
    User currentUser = SecurityUtils.getCurrentUserObject();
    checkReadPermissionOnTask(currentUser, taskId);

    // Create comment
    Comment comment = createComment(commentRequest.getMessage(), currentUser, task.getId(), task.getProcessInstanceId());
    return new CommentRepresentation(comment);
  }

  public ResultListDataRepresentation getProcessInstanceComments(String processInstanceId, Boolean latestFirst) {

    User currentUser = SecurityUtils.getCurrentUserObject();
    checkReadPermissionOnProcessInstance(currentUser, processInstanceId);
    List<Comment> comments = getCommentsForProcessInstance(processInstanceId, Boolean.TRUE.equals(latestFirst));

    // Create representation for all comments
    List<CommentRepresentation> commentList = new ArrayList<CommentRepresentation>();
    for (Comment comment : comments) {
      commentList.add(new CommentRepresentation(comment));
    }

    return new ResultListDataRepresentation(commentList);
  }

  public CommentRepresentation addProcessInstanceComment(CommentRepresentation commentRequest, String processInstanceId) {

    if (StringUtils.isBlank(commentRequest.getMessage())) {
      throw new BadRequestException("Comment should not be empty");
    }

    HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
    if (processInstance == null) {
      throw new NotFoundException("No process instance found with id: " + processInstanceId);
    }

    // Check read permission and message
    User currentUser = SecurityUtils.getCurrentUserObject();
    checkReadPermissionOnProcessInstance(currentUser, processInstanceId);

    // Create comment
    Comment comment = createComment(commentRequest.getMessage(), currentUser, processInstanceId);
    return new CommentRepresentation(comment);
  }

  public Long countCommentsForTask(String taskId) {
    return commentRepository.countByTaskId(taskId);
  }

  public Long countCommentsForProcessInstance(String processInstanceId) {
    return commentRepository.countByProcessInstanceId(processInstanceId);
  }

  public List<Comment> getCommentsForTask(String taskId, boolean latestFirst) {
    return commentRepository.findByTaskId(taskId, new Sort((latestFirst ? Direction.DESC : Direction.ASC), Comment.PROPERTY_CREATED));
  }

  public List<Comment> getCommentsForProcessInstance(String processInstanceId, boolean latestFirst) {
    return commentRepository.findByProcessInstanceId(processInstanceId, new Sort((latestFirst ? Direction.DESC : Direction.ASC), Comment.PROPERTY_CREATED));
  }

  public Comment createComment(String message, User createdBy, String processInstanceId) {
    return createComment(message, createdBy, null, processInstanceId);
  }

  public Comment createComment(String message, User createdBy, String taskId, String processInstanceId) {
    Comment newComment = new Comment();
    newComment.setMessage(message);
    newComment.setCreatedBy(createdBy.getId());
    newComment.setCreated(clock.getCurrentTime());
    newComment.setTaskId(taskId);
    newComment.setProcessInstanceId(processInstanceId);

    commentRepository.save(newComment);
    return newComment;
  }

  public void deleteComment(Comment comment) {
    commentRepository.delete(comment);
  }

  /**
   * Deletes all comments related to the given process instance. Includes both comments on the process instance itself and any comments on the tasks in that process.
   */
  @Transactional
  public void deleteAllCommentsForProcessInstance(String processInstanceId) {
    commentRepository.deleteAllByProcessInstanceId(processInstanceId);
  }

  protected void checkReadPermissionOnTask(User user, String taskId) {
    if (taskId == null) {
      throw new BadRequestException("Task id is required");
    }
    permissionService.validateReadPermissionOnTask(SecurityUtils.getCurrentUserObject(), taskId);
  }

  protected void checkReadPermissionOnProcessInstance(User user, String processInstanceId) {
    if (processInstanceId == null) {
      throw new BadRequestException("Process instance id is required");
    }
    if (!permissionService.hasReadPermissionOnProcessInstance(SecurityUtils.getCurrentUserObject(), processInstanceId)) {
      throw new NotPermittedException("You are not permitted to read process instance with id: " + processInstanceId);
    }
  }
}
