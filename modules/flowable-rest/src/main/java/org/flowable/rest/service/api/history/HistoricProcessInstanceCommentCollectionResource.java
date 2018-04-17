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

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.rest.service.api.engine.CommentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@Api(tags = { "History Process" }, description = "Manage History Process Instances", authorizations = { @Authorization(value = "basicAuth") })
public class HistoricProcessInstanceCommentCollectionResource {

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected HistoryService historyService;

    @Autowired
    protected TaskService taskService;

    @ApiOperation(value = "List comments on a historic process instance", nickname="listHistoricProcessInstanceComments", tags = { "History Process" }, notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the process instance was found and the comments are returned."),
            @ApiResponse(code = 404, message = "Indicates that the historic process instance could not be found.") })
    @GetMapping(value = "/history/historic-process-instances/{processInstanceId}/comments", produces = "application/json")
    public List<CommentResponse> getComments(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId, HttpServletRequest request) {
        HistoricProcessInstance instance = getHistoricProcessInstanceFromRequest(processInstanceId);
        return restResponseFactory.createRestCommentList(taskService.getProcessInstanceComments(instance.getId()));
    }

    @ApiOperation(value = "Create a new comment on a historic process instance", tags = { "History Process" },
            notes = "Parameter saveProcessInstanceId is optional, if true save process instance id of task with comment.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the comment was created and the result is returned."),
            @ApiResponse(code = 400, message = "Indicates the comment is missing from the request."),
            @ApiResponse(code = 404, message = "Indicates that the historic process instance could not be found.") })
    @PostMapping(value = "/history/historic-process-instances/{processInstanceId}/comments", produces = "application/json")
    public CommentResponse createComment(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId, @RequestBody CommentResponse comment, HttpServletRequest request, HttpServletResponse response) {

        HistoricProcessInstance instance = getHistoricProcessInstanceFromRequest(processInstanceId);

        if (comment.getMessage() == null) {
            throw new FlowableIllegalArgumentException("Comment text is required.");
        }

        Comment createdComment = taskService.addComment(null, instance.getId(), comment.getMessage());
        response.setStatus(HttpStatus.CREATED.value());

        return restResponseFactory.createRestComment(createdComment);
    }

    protected HistoricProcessInstance getHistoricProcessInstanceFromRequest(String processInstanceId) {
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if (processInstance == null) {
            throw new FlowableObjectNotFoundException("Could not find a process instance with id '" + processInstanceId + "'.", HistoricProcessInstance.class);
        }
        return processInstance;
    }
}
