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

package org.flowable.rest.service.api.history;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.rest.service.api.engine.CommentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "History Process" }, description = "Manage History Process Instances", authorizations = { @Authorization(value = "basicAuth") })
public class HistoricProcessInstanceCommentResource {

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected HistoryService historyService;

    @Autowired
    protected TaskService taskService;

    @ApiOperation(value = "Get a comment on a historic process instance", tags = { "History Process" }, notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the historic process instance and comment were found and the comment is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested historic process instance was not found or the historic process instance doesn’t have a comment with the given ID.") })
    @GetMapping(value = "/history/historic-process-instances/{processInstanceId}/comments/{commentId}", produces = "application/json")
    public CommentResponse getComment(@ApiParam(name = "processInstanceId") @PathVariable("processInstanceId") String processInstanceId, @ApiParam(name = "commentId") @PathVariable("commentId") String commentId, HttpServletRequest request) {

        HistoricProcessInstance instance = getHistoricProcessInstanceFromRequest(processInstanceId);

        Comment comment = taskService.getComment(commentId);
        if (comment == null || comment.getProcessInstanceId() == null || !comment.getProcessInstanceId().equals(instance.getId())) {
            throw new FlowableObjectNotFoundException("Process instance '" + instance.getId() + "' doesn't have a comment with id '" + commentId + "'.", Comment.class);
        }

        return restResponseFactory.createRestComment(comment);
    }

    @ApiOperation(value = "Delete a comment on a historic process instance", tags = { "History Process" }, notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the historic process instance and comment were found and the comment is deleted. Response body is left empty intentionally."),
            @ApiResponse(code = 404, message = "Indicates the requested historic process instance was not found or the historic process instance doesn’t have a comment with the given ID.") })
    @DeleteMapping(value = "/history/historic-process-instances/{processInstanceId}/comments/{commentId}")
    public void deleteComment(@ApiParam(name = "processInstanceId") @PathVariable("processInstanceId") String processInstanceId, @ApiParam(name = "commentId") @PathVariable("commentId") String commentId, HttpServletRequest request,
            HttpServletResponse response) {

        HistoricProcessInstance instance = getHistoricProcessInstanceFromRequest(processInstanceId);

        Comment comment = taskService.getComment(commentId);
        if (comment == null || comment.getProcessInstanceId() == null || !comment.getProcessInstanceId().equals(instance.getId())) {
            throw new FlowableObjectNotFoundException("Process instance '" + instance.getId() + "' doesn't have a comment with id '" + commentId + "'.", Comment.class);
        }

        taskService.deleteComment(commentId);
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    protected HistoricProcessInstance getHistoricProcessInstanceFromRequest(String processInstanceId) {
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if (processInstance == null) {
            throw new FlowableObjectNotFoundException("Could not find a process instance with id '" + processInstanceId + "'.", HistoricProcessInstance.class);
        }
        return processInstance;
    }
}
