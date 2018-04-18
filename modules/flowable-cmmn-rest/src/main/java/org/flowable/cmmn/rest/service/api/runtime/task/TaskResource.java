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

package org.flowable.cmmn.rest.service.api.runtime.task;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.rest.exception.FlowableForbiddenException;
import org.flowable.task.api.Task;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Tasks" }, description = "Manage Tasks", authorizations = { @Authorization(value = "basicAuth") })
public class TaskResource extends TaskBaseResource {

    @ApiOperation(value = "Get a task", tags = { "Tasks" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the task was found and returned."),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found.")
    })
    @GetMapping(value = "/cmmn-runtime/tasks/{taskId}", produces = "application/json")
    public TaskResponse getTask(@ApiParam(name = "taskId") @PathVariable String taskId, HttpServletRequest request) {
        return restResponseFactory.createTaskResponse(getTaskFromRequest(taskId));
    }

    @ApiOperation(value = "Update a task", tags = {
            "Tasks" }, notes = "All request values are optional. For example, you can only include the assignee attribute in the request body JSON-object, only updating the assignee of the task, leaving all other fields unaffected. When an attribute is explicitly included and is set to null, the task-value will be updated to null. Example: {\"dueDate\" : null} will clear the duedate of the task).")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the task was updated."),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found."),
            @ApiResponse(code = 409, message = "Indicates the requested task was updated simultaneously.")
    })
    @PutMapping(value = "/cmmn-runtime/tasks/{taskId}", produces = "application/json")
    public TaskResponse updateTask(@ApiParam(name = "taskId") @PathVariable String taskId, @RequestBody TaskRequest taskRequest, HttpServletRequest request) {

        if (taskRequest == null) {
            throw new FlowableException("A request body was expected when updating the task.");
        }

        Task task = getTaskFromRequest(taskId);

        // Populate the task properties based on the request
        populateTaskFromRequest(task, taskRequest);

        // Save the task and fetch again, it's possible that an
        // assignment-listener has updated
        // fields after it was saved so we can't use the in-memory task
        taskService.saveTask(task);
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();

        return restResponseFactory.createTaskResponse(task);
    }

    @ApiOperation(value = "Tasks actions", tags = { "Tasks" },
            notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the action was executed."),
            @ApiResponse(code = 400, message = "When the body contains an invalid value or when the assignee is missing when the action requires it."),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found."),
            @ApiResponse(code = 409, message = "Indicates the action cannot be performed due to a conflict. Either the task was updates simultaneously or the task was claimed by another user, in case of the claim action.")
    })
    @PostMapping(value = "/cmmn-runtime/tasks/{taskId}")
    @ResponseStatus(value = HttpStatus.OK)
    public void executeTaskAction(@ApiParam(name = "taskId") @PathVariable String taskId, @RequestBody TaskActionRequest actionRequest) {
        if (actionRequest == null) {
            throw new FlowableException("A request body was expected when executing a task action.");
        }

        Task task = getTaskFromRequest(taskId);

        if (TaskActionRequest.ACTION_COMPLETE.equals(actionRequest.getAction())) {
            completeTask(task, actionRequest);

        } else if (TaskActionRequest.ACTION_CLAIM.equals(actionRequest.getAction())) {
            claimTask(task, actionRequest);

        } else if (TaskActionRequest.ACTION_DELEGATE.equals(actionRequest.getAction())) {
            delegateTask(task, actionRequest);

        } else if (TaskActionRequest.ACTION_RESOLVE.equals(actionRequest.getAction())) {
            resolveTask(task, actionRequest);

        } else {
            throw new FlowableIllegalArgumentException("Invalid action: '" + actionRequest.getAction() + "'.");
        }
    }

    @ApiOperation(value = "Delete a task", tags = { "Tasks" })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "cascadeHistory", dataType = "string", value = "Whether or not to delete the HistoricTask instance when deleting the task (if applicable). If not provided, this value defaults to false.", paramType = "query"),
            @ApiImplicitParam(name = "deleteReason", dataType = "string", value = "Reason why the task is deleted. This value is ignored when cascadeHistory is true.", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the task was found and has been deleted. Response-body is intentionally empty."),
            @ApiResponse(code = 403, message = "Indicates the requested task cannot be deleted because it’s part of a workflow."),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found.")
    })
    @DeleteMapping(value = "/cmmn-runtime/tasks/{taskId}")
    public void deleteTask(@ApiParam(name = "taskId") @PathVariable String taskId, @ApiParam(hidden = true) @RequestParam(value = "cascadeHistory", required = false) Boolean cascadeHistory,
            @ApiParam(hidden = true) @RequestParam(value = "deleteReason", required = false) String deleteReason, HttpServletResponse response) {

        Task taskToDelete = getTaskFromRequest(taskId);
        if (taskToDelete.getScopeId() != null) {
            // Can't delete a task that is part of a process instance
            throw new FlowableForbiddenException("Cannot delete a task that is part of a case instance.");
        }

        if (cascadeHistory != null) {
            // Ignore delete-reason since the task-history (where the reason is
            // recorded) will be deleted anyway
            taskService.deleteTask(taskToDelete.getId(), cascadeHistory);
        } else {
            // Delete with delete-reason
            taskService.deleteTask(taskToDelete.getId(), deleteReason);
        }
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    protected void completeTask(Task task, TaskActionRequest actionRequest) {
        Map<String, Object> variablesToSet = null;
        Map<String, Object> transientVariablesToSet = null;

        if (actionRequest.getVariables() != null) {
            variablesToSet = new HashMap<>();
            for (RestVariable var : actionRequest.getVariables()) {
                if (var.getName() == null) {
                    throw new FlowableIllegalArgumentException("Variable name is required");
                }

                Object actualVariableValue = restResponseFactory.getVariableValue(var);
                variablesToSet.put(var.getName(), actualVariableValue);
            }
        }

        if (actionRequest.getTransientVariables() != null) {
            transientVariablesToSet = new HashMap<>();
            for (RestVariable var : actionRequest.getTransientVariables()) {
                if (var.getName() == null) {
                    throw new FlowableIllegalArgumentException("Transient variable name is required");
                }

                Object actualVariableValue = restResponseFactory.getVariableValue(var);
                transientVariablesToSet.put(var.getName(), actualVariableValue);
            }
        }

        taskService.complete(task.getId(), variablesToSet, transientVariablesToSet);
    }

    protected void resolveTask(Task task, TaskActionRequest actionRequest) {
        taskService.resolveTask(task.getId());
    }

    protected void delegateTask(Task task, TaskActionRequest actionRequest) {
        if (actionRequest.getAssignee() == null) {
            throw new FlowableIllegalArgumentException("An assignee is required when delegating a task.");
        }
        taskService.delegateTask(task.getId(), actionRequest.getAssignee());
    }

    protected void claimTask(Task task, TaskActionRequest actionRequest) {
        // In case the task is already claimed, a
        // FlowableTaskAlreadyClaimedException is thrown and converted to
        // a CONFLICT response by the ExceptionHandlerAdvice
        taskService.claim(task.getId(), actionRequest.getAssignee());
    }
}
