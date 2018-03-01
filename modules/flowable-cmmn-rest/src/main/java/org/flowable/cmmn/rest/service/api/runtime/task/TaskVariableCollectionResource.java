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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.cmmn.rest.service.api.RestResponseFactory;
import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable;
import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable.RestVariableScope;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.rest.exception.FlowableConflictException;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
@Api(tags = { "Task Variables" }, description = "Manage Tasks variables", authorizations = { @Authorization(value = "basicAuth") })
public class TaskVariableCollectionResource extends TaskVariableBaseResource {

    @Autowired
    protected ObjectMapper objectMapper;

    @ApiOperation(value = "List variables for a task", tags = {"Task Variables" }, nickname = "listTaskVariables")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the task was found and the requested variables are returned"),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found..")
    })
    @ApiImplicitParams(@ApiImplicitParam(name = "scope", dataType = "string", value = "Scope of variable to be returned. When local, only task-local variable value is returned. When global, only variable value from the task’s parent execution-hierarchy are returned. When the parameter is omitted, a local variable will be returned if it exists, otherwise a global variable.", paramType = "query"))
    @GetMapping(value = "/cmmn-runtime/tasks/{taskId}/variables", produces = "application/json")
    public List<RestVariable> getVariables(@ApiParam(name = "taskId") @PathVariable String taskId, @ApiParam(hidden = true) @RequestParam(value = "scope", required = false) String scope, HttpServletRequest request) {

        List<RestVariable> result = new ArrayList<>();
        Map<String, RestVariable> variableMap = new HashMap<>();

        // Check if it's a valid task to get the variables for
        Task task = getTaskFromRequest(taskId);

        RestVariableScope variableScope = RestVariable.getScopeFromString(scope);
        if (variableScope == null) {
            // Use both local and global variables
            addLocalVariables(task, variableMap);
            addGlobalVariables(task, variableMap);

        } else if (variableScope == RestVariableScope.GLOBAL) {
            addGlobalVariables(task, variableMap);

        } else if (variableScope == RestVariableScope.LOCAL) {
            addLocalVariables(task, variableMap);
        }

        // Get unique variables from map
        result.addAll(variableMap.values());
        return result;
    }

    // FIXME OASv3 to solve Multiple Endpoint issue
    @ApiOperation(value = "Create new variables on a task", tags = { "Tasks", "Task Variables" },
            notes = "This endpoint can be used in 2 ways: By passing a JSON Body (RestVariable or an Array of RestVariable) or by passing a multipart/form-data Object.\n"
                    + "It's possible to create simple (non-binary) variable or list of variables or new binary variable \n"
                    + "Any number of variables can be passed into the request body array.\n"
                    + "NB: Swagger V2 specification doesn't support this use case that's why this endpoint might be buggy/incomplete if used with other tools.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "body", type = "org.flowable.rest.service.api.engine.variable.RestVariable", value = "Create a variable on a task", paramType = "body", example = "{\n" +
                    "    \"name\":\"intProcVar\"\n" +
                    "    \"type\":\"integer\"\n" +
                    "    \"value\":123,\n" +
                    " }"),
            @ApiImplicitParam(name = "name", value = "Required name of the variable", dataType = "string", paramType = "form", example = "Simple content item"),
            @ApiImplicitParam(name = "type", value = "Type of variable that is created. If omitted, reverts to raw JSON-value type (string, boolean, integer or double)",dataType = "string", paramType = "form", example = "integer"),
            @ApiImplicitParam(name = "scope",value = "Scope of variable that is created. If omitted, local is assumed.", dataType = "string",  paramType = "form", example = "local")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the variables were created and the result is returned."),
            @ApiResponse(code = 400, message = "Indicates the name of a variable to create was missing or that an attempt is done to create a variable on a standalone task (without a process associated) with scope global or an empty array of variables was included in the request or request did not contain an array of variables. Status message provides additional information."),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found."),
            @ApiResponse(code = 409, message = "Indicates the task already has a variable with the given name. Use the PUT method to update the task variable instead."),
            @ApiResponse(code = 415, message = "Indicates the serializable data contains an object for which no class is present in the JVM running the Flowable engine and therefore cannot be deserialized.")
    })
    @PostMapping(value = "/cmmn-runtime/tasks/{taskId}/variables", produces = "application/json", consumes = {"text/plain", "application/json", "multipart/form-data"})
    public Object createTaskVariable(@ApiParam(name = "taskId") @PathVariable String taskId, HttpServletRequest request, HttpServletResponse response) {

        Task task = getTaskFromRequest(taskId);

        Object result = null;
        if (request instanceof MultipartHttpServletRequest) {
            result = setBinaryVariable((MultipartHttpServletRequest) request, task, true);
        } else {

            List<RestVariable> inputVariables = new ArrayList<>();
            List<RestVariable> resultVariables = new ArrayList<>();
            result = resultVariables;

            try {
                @SuppressWarnings("unchecked")
                List<Object> variableObjects = (List<Object>) objectMapper.readValue(request.getInputStream(), List.class);
                for (Object restObject : variableObjects) {
                    RestVariable restVariable = objectMapper.convertValue(restObject, RestVariable.class);
                    inputVariables.add(restVariable);
                }
                
            } catch (Exception e) {
                throw new FlowableIllegalArgumentException("Failed to serialize to a RestVariable instance", e);
            }

            if (inputVariables == null || inputVariables.size() == 0) {
                throw new FlowableIllegalArgumentException("Request didn't contain a list of variables to create.");
            }

            RestVariableScope sharedScope = null;
            RestVariableScope varScope = null;
            Map<String, Object> variablesToSet = new HashMap<>();

            for (RestVariable var : inputVariables) {
                // Validate if scopes match
                varScope = var.getVariableScope();
                if (var.getName() == null) {
                    throw new FlowableIllegalArgumentException("Variable name is required");
                }

                if (varScope == null) {
                    varScope = RestVariableScope.LOCAL;
                }
                if (sharedScope == null) {
                    sharedScope = varScope;
                }
                if (varScope != sharedScope) {
                    throw new FlowableIllegalArgumentException("Only allowed to update multiple variables in the same scope.");
                }

                if (hasVariableOnScope(task, var.getName(), varScope)) {
                    throw new FlowableConflictException("Variable '" + var.getName() + "' is already present on task '" + task.getId() + "'.");
                }

                Object actualVariableValue = restResponseFactory.getVariableValue(var);
                variablesToSet.put(var.getName(), actualVariableValue);
                resultVariables.add(restResponseFactory.createRestVariable(var.getName(), actualVariableValue, varScope, task.getId(), RestResponseFactory.VARIABLE_TASK, false));
            }

            if (!variablesToSet.isEmpty()) {
                if (sharedScope == RestVariableScope.LOCAL) {
                    taskService.setVariablesLocal(task.getId(), variablesToSet);
                } else {
                    if (task.getScopeId() != null) {
                        // Explicitly set on case, setting non-local
                        // variables on task will override local-variables if exists
                        runtimeService.setVariables(task.getScopeId(), variablesToSet);
                    } else {
                        // Standalone task, no global variables possible
                        throw new FlowableIllegalArgumentException("Cannot set global variables on task '" + task.getId() + "', task is not part of process.");
                    }
                }
            }
        }

        response.setStatus(HttpStatus.CREATED.value());
        return result;
    }

    @ApiOperation(value = "Delete all local variables on a task", tags = { "Tasks" })
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates all local task variables have been deleted. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found.")
    })
    @DeleteMapping(value = "/cmmn-runtime/tasks/{taskId}/variables")
    public void deleteAllLocalTaskVariables(@ApiParam(name = "taskId") @PathVariable String taskId, HttpServletResponse response) {
        Task task = getTaskFromRequest(taskId);
        Collection<String> currentVariables = taskService.getVariablesLocal(task.getId()).keySet();
        taskService.removeVariablesLocal(task.getId(), currentVariables);

        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    protected void addGlobalVariables(Task task, Map<String, RestVariable> variableMap) {
        if (task.getScopeId() != null) {
            Map<String, Object> rawVariables = runtimeService.getVariables(task.getScopeId());
            List<RestVariable> globalVariables = restResponseFactory.createRestVariables(rawVariables, task.getId(), RestResponseFactory.VARIABLE_TASK, RestVariableScope.GLOBAL);

            // Overlay global variables over local ones. In case they are
            // present the values are not overridden,
            // since local variables get precedence over global ones at all times.
            for (RestVariable var : globalVariables) {
                if (!variableMap.containsKey(var.getName())) {
                    variableMap.put(var.getName(), var);
                }
            }
        }
    }

    protected void addLocalVariables(Task task, Map<String, RestVariable> variableMap) {
        Map<String, Object> rawVariables = taskService.getVariablesLocal(task.getId());
        List<RestVariable> localVariables = restResponseFactory.createRestVariables(rawVariables, task.getId(), RestResponseFactory.VARIABLE_TASK, RestVariableScope.LOCAL);

        for (RestVariable var : localVariables) {
            variableMap.put(var.getName(), var);
        }
    }
}
