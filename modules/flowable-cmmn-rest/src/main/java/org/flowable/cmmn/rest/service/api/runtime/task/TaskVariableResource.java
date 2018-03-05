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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable;
import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable.RestVariableScope;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.task.api.Task;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

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
@Api(tags = { "Task Variables" }, description = "Manage Tasks", authorizations = { @Authorization(value = "basicAuth") })
public class TaskVariableResource extends TaskVariableBaseResource {

    @Autowired
    protected ObjectMapper objectMapper;

    @ApiOperation(value = "Get a variable from a task", tags = { "Task Variables" }, nickname = "getTaskInstanceVariable")
    @ApiImplicitParams(@ApiImplicitParam(name = "scope", dataType = "string", value = "Scope of variable to be returned. When local, only task-local variable value is returned. When global, only variable value from the task’s parent execution-hierarchy are returned. When the parameter is omitted, a local variable will be returned if it exists, otherwise a global variable.", paramType = "query"))
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the task was found and the requested variables are returned."),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found or the task doesn’t have a variable with the given name (in the given scope). Status message provides additional information.")
    })
    @GetMapping(value = "/cmmn-runtime/tasks/{taskId}/variables/{variableName}", produces = "application/json")
    public RestVariable getVariable(@ApiParam(name = "taskId") @PathVariable("taskId") String taskId, @ApiParam(name = "variableName") @PathVariable("variableName") String variableName,
            @ApiParam(hidden = true) @RequestParam(value = "scope", required = false) String scope,
            HttpServletRequest request, HttpServletResponse response) {

        return getVariableFromRequest(taskId, variableName, scope, false);
    }

    // FIXME OASv3 to solve Multiple Endpoint issue
    @ApiOperation(value = "Update an existing variable on a task", tags = { "Task Variables" }, nickname = "updateTaskInstanceVariable",
            notes = "This endpoint can be used in 2 ways: By passing a JSON Body (RestVariable) or by passing a multipart/form-data Object.\n"
                    + "It's possible to update simple (non-binary) variable or  binary variable \n"
                    + "Any number of variables can be passed into the request body array.\n"
                    + "NB: Swagger V2 specification doesn't support this use case that's why this endpoint might be buggy/incomplete if used with other tools.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "body", type = "org.flowable.rest.service.api.engine.variable.RestVariable", value = "Update a task variable", paramType = "body", example = "{\n" +
                    "    \"name\":\"intProcVar\"\n" +
                    "    \"type\":\"integer\"\n" +
                    "    \"value\":123,\n" +
                    " }"),
            @ApiImplicitParam(name = "name", value = "Required name of the variable", dataType = "string", paramType = "form", example = "Simple content item"),
            @ApiImplicitParam(name = "type", value = "Type of variable that is updated. If omitted, reverts to raw JSON-value type (string, boolean, integer or double)",dataType = "string", paramType = "form", example = "integer"),
            @ApiImplicitParam(name = "scope",value = "Scope of variable to be returned. When local, only task-local variable value is returned. When global, only variable value from the task’s parent execution-hierarchy are returned. When the parameter is omitted, a local variable will be returned if it exists, otherwise a global variable..", dataType = "string",  paramType = "form", example = "local")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the variables was updated and the result is returned."),
            @ApiResponse(code = 400, message = "Indicates the name of a variable to update was missing or that an attempt is done to update a variable on a standalone task (without a process associated) with scope global. Status message provides additional information."),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found or the task doesn’t have a variable with the given name in the given scope. Status message contains additional information about the error."),
            @ApiResponse(code = 415, message = "Indicates the serializable data contains an object for which no class is present in the JVM running the Flowable engine and therefore cannot be deserialized."),
    })
    @PutMapping(value = "/cmmn-runtime/tasks/{taskId}/variables/{variableName}", produces = "application/json", consumes = {"text/plain", "application/json", "multipart/form-data"})
    public RestVariable updateVariable(@ApiParam(name = "taskId") @PathVariable("taskId") String taskId,
            @ApiParam(name = "variableName") @PathVariable("variableName") String variableName,
            @ApiParam(hidden = true) @RequestParam(value = "scope", required = false) String scope,
            HttpServletRequest request) {

        Task task = getTaskFromRequest(taskId);

        RestVariable result = null;
        if (request instanceof MultipartHttpServletRequest) {
            result = setBinaryVariable((MultipartHttpServletRequest) request, task, false);

            if (!result.getName().equals(variableName)) {
                throw new FlowableIllegalArgumentException("Variable name in the body should be equal to the name used in the requested URL.");
            }

        } else {

            RestVariable restVariable = null;

            try {
                restVariable = objectMapper.readValue(request.getInputStream(), RestVariable.class);
            } catch (Exception e) {
                throw new FlowableIllegalArgumentException("Error converting request body to RestVariable instance", e);
            }

            if (restVariable == null) {
                throw new FlowableException("Invalid body was supplied");
            }
            if (!restVariable.getName().equals(variableName)) {
                throw new FlowableIllegalArgumentException("Variable name in the body should be equal to the name used in the requested URL.");
            }

            result = setSimpleVariable(restVariable, task, false);
        }
        return result;
    }

    @ApiOperation(value = "Delete a variable on a task", tags = { "Task Variables" }, nickname = "deleteTaskInstanceVariable")
    @ApiImplicitParams(@ApiImplicitParam(name = "scope", dataType = "string", value = "Scope of variable to be returned. When local, only task-local variable value is returned. When global, only variable value from the task’s parent execution-hierarchy are returned. When the parameter is omitted, a local variable will be returned if it exists, otherwise a global variable.", paramType = "query"))
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the task variable was found and has been deleted. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found or the task doesn’t have a variable with the given name. Status message contains additional information about the error.")
    })
    @DeleteMapping(value = "/cmmn-runtime/tasks/{taskId}/variables/{variableName}")
    public void deleteVariable(@ApiParam(name = "taskId") @PathVariable("taskId") String taskId,
            @ApiParam(name = "variableName") @PathVariable("variableName") String variableName,
            @ApiParam(hidden = true) @RequestParam(value = "scope", required = false) String scopeString,
            HttpServletResponse response) {

        Task task = getTaskFromRequest(taskId);

        // Determine scope
        RestVariableScope scope = RestVariableScope.LOCAL;
        if (scopeString != null) {
            scope = RestVariable.getScopeFromString(scopeString);
        }

        if (!hasVariableOnScope(task, variableName, scope)) {
            throw new FlowableObjectNotFoundException("Task '" + task.getId() + "' doesn't have a variable '" + variableName + "' in scope " + scope.name().toLowerCase(), VariableInstanceEntity.class);
        }

        if (scope == RestVariableScope.LOCAL) {
            taskService.removeVariableLocal(task.getId(), variableName);
        } else {
            // Safe to use scope id, as the hasVariableOnScope would have
            // stopped a global-var update on standalone task
            runtimeService.removeVariable(task.getScopeId(), variableName);
        }
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }
}
