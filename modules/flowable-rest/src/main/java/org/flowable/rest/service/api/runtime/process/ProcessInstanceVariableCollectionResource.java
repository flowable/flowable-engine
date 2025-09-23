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

package org.flowable.rest.service.api.runtime.process;

import java.util.List;
import java.util.Map;

import org.flowable.engine.runtime.Execution;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.rest.service.api.engine.variable.RestVariable;
import org.flowable.rest.service.api.engine.variable.RestVariable.RestVariableScope;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Process Instance Variables" }, authorizations = { @Authorization(value = "basicAuth") })
public class ProcessInstanceVariableCollectionResource extends BaseVariableCollectionResource {

    public ProcessInstanceVariableCollectionResource() {
        super(RestResponseFactory.VARIABLE_PROCESS);
    }

    @ApiOperation(value = "List variables for a process instance", nickname="listProcessInstanceVariables", tags = {"Process Instance Variables" },
            notes = "In case the variable is a binary variable or serializable, the valueUrl points to an URL to fetch the raw value. If it’s a plain variable, the value is present in the response. Note that only local scoped variables are returned, as there is no global scope for process-instance variables.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the process instance was found and variables are returned."),
            @ApiResponse(code = 400, message = "Indicates the requested process instance was not found.")
    })
    @GetMapping(value = "/runtime/process-instances/{processInstanceId}/variables", produces = "application/json")
    public List<RestVariable> getVariables(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId, @RequestParam(value = "scope", required = false) String scope) {

        Execution execution = getExecutionFromRequestWithoutAccessCheck(processInstanceId);
        return processVariables(execution, scope);
    }

    @ApiOperation(value = "Update a multiple/single (non)binary variable on a process instance", tags = { "Process Instance Variables" }, nickname = "createOrUpdateProcessVariable",
            notes = "This endpoint can be used in 2 ways: By passing a JSON Body (RestVariable or an array of RestVariable) or by passing a multipart/form-data Object.\n"
                    + "Nonexistent variables are created on the process-instance and existing ones are overridden without any error.\n"
                    + "Any number of variables can be passed into the request body array.\n"
                    + "Note that scope is ignored, only local variables can be set in a process instance.\n"
                    + "NB: Swagger V2 specification does not support this use case that is why this endpoint might be buggy/incomplete if used with other tools.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "body", type = "org.flowable.rest.service.api.engine.variable.RestVariable", value = "Create a variable on a process instance", paramType = "body", example = "{\n" +
                    "    \"name\":\"intProcVar\"\n" +
                    "    \"type\":\"integer\"\n" +
                    "    \"value\":123,\n" +
                    " }"),
            @ApiImplicitParam(name = "file", dataType = "file", paramType = "form"),
            @ApiImplicitParam(name = "name", dataType = "string", paramType = "form", example = "Simple content item"),
            @ApiImplicitParam(name = "type", dataType = "string", paramType = "form", example = "integer"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the process instance was found and variable is created."),
            @ApiResponse(code = 400, message = "Indicates the request body is incomplete or contains illegal values. The status description contains additional information about the error."),
            @ApiResponse(code = 404, message = "Indicates the requested process instance was not found."),
            @ApiResponse(code = 415, message = "Indicates the serializable data contains an object for which no class is present in the JVM running the Flowable engine and therefore cannot be deserialized.")

    })
    @PutMapping(value = "/runtime/process-instances/{processInstanceId}/variables", produces = "application/json", consumes = {"application/json", "multipart/form-data"})
    public Object createOrUpdateExecutionVariable(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId, HttpServletRequest request, HttpServletResponse response) {

        Execution execution = getExecutionFromRequestWithoutAccessCheck(processInstanceId);
        return createExecutionVariable(execution, true, false, request, response);
    }
    
    @ApiOperation(value = "Update multiple/single (non)binary variables on a process instance asynchronously", tags = { "Process Instance Variables" }, nickname = "createOrUpdateProcessVariableAsync",
            notes = "This endpoint can be used in 2 ways: By passing a JSON Body (RestVariable or an array of RestVariable) or by passing a multipart/form-data Object.\n"
                    + "Nonexistent variables are created on the process-instance and existing ones are overridden without any error.\n"
                    + "Any number of variables can be passed into the request body array.\n"
                    + "Note that scope is ignored, only local variables can be set in a process instance.\n"
                    + "NB: Swagger V2 specification does not support this use case that is why this endpoint might be buggy/incomplete if used with other tools.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "body", type = "org.flowable.rest.service.api.engine.variable.RestVariable", value = "Create a variable on a process instance", paramType = "body", example = "{\n" +
                    "    \"name\":\"intProcVar\"\n" +
                    "    \"type\":\"integer\"\n" +
                    "    \"value\":123,\n" +
                    " }"),
            @ApiImplicitParam(name = "file", dataType = "file", paramType = "form"),
            @ApiImplicitParam(name = "name", dataType = "string", paramType = "form", example = "Simple content item"),
            @ApiImplicitParam(name = "type", dataType = "string", paramType = "form", example = "integer"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the job to create or update the variables was created."),
            @ApiResponse(code = 400, message = "Indicates the request body is incomplete or contains illegal values. The status description contains additional information about the error."),
            @ApiResponse(code = 415, message = "Indicates the serializable data contains an object for which no class is present in the JVM running the Flowable engine and therefore cannot be deserialized.")

    })
    @PutMapping(value = "/runtime/process-instances/{processInstanceId}/variables-async", consumes = {"application/json", "multipart/form-data"})
    public void createOrUpdateExecutionVariableAsync(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId, HttpServletRequest request, HttpServletResponse response) {
        Execution execution = getExecutionFromRequestWithoutAccessCheck(processInstanceId);
        createExecutionVariable(execution, true, true, request, response);
    }

    @ApiOperation(value = "Create variables or new binary variable on a process instance", tags = { "Process Instance Variables" }, nickname = "createProcessInstanceVariable",
            notes = "This endpoint can be used in 2 ways: By passing a JSON Body (RestVariable or an array of RestVariable) or by passing a multipart/form-data Object.\n"
                    + "Nonexistent variables are created on the process-instance and existing ones are overridden without any error.\n"
                    + "Any number of variables can be passed into the request body array.\n"
                    + "Note that scope is ignored, only local variables can be set in a process instance.\n"
                    + "NB: Swagger V2 specification does not support this use case that is why this endpoint might be buggy/incomplete if used with other tools.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "body", type = "org.flowable.rest.service.api.engine.variable.RestVariable", value = "Create a variable on a process instance", paramType = "body", example = "{\n" +
                    "    \"name\":\"intProcVar\"\n" +
                    "    \"type\":\"integer\"\n" +
                    "    \"value\":123,\n" +
                    " }"),
            @ApiImplicitParam(name = "file", dataType = "file", paramType = "form"),
            @ApiImplicitParam(name = "name", dataType = "string", paramType = "form", example = "Simple content item"),
            @ApiImplicitParam(name = "type", dataType = "string", paramType = "form", example = "integer"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the process instance was found and variable is created."),
            @ApiResponse(code = 400, message = "Indicates the request body is incomplete or contains illegal values. The status description contains additional information about the error."),
            @ApiResponse(code = 404, message = "Indicates the requested process instance was not found."),
            @ApiResponse(code = 409, message = "Indicates the process instance was found but already contains a variable with the given name (only thrown when POST method is used). Use the update-method instead."),

    })
    @PostMapping(value = "/runtime/process-instances/{processInstanceId}/variables", produces = "application/json", consumes = {"application/json", "multipart/form-data"})
    public Object createExecutionVariable(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId, HttpServletRequest request, HttpServletResponse response) {

        Execution execution = getExecutionFromRequestWithoutAccessCheck(processInstanceId);
        return createExecutionVariable(execution, false, false, request, response);
    }
    
    @ApiOperation(value = "Create variables or new binary variable on a process instance asynchronously", tags = { "Process Instance Variables" }, nickname = "createProcessInstanceVariableAsync",
            notes = "This endpoint can be used in 2 ways: By passing a JSON Body (RestVariable or an array of RestVariable) or by passing a multipart/form-data Object.\n"
                    + "Nonexistent variables are created on the process-instance and existing ones are overridden without any error.\n"
                    + "Any number of variables can be passed into the request body array.\n"
                    + "Note that scope is ignored, only local variables can be set in a process instance.\n"
                    + "NB: Swagger V2 specification does not support this use case that is why this endpoint might be buggy/incomplete if used with other tools.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "body", type = "org.flowable.rest.service.api.engine.variable.RestVariable", value = "Create a variable on a process instance", paramType = "body", example = "{\n" +
                    "    \"name\":\"intProcVar\"\n" +
                    "    \"type\":\"integer\"\n" +
                    "    \"value\":123,\n" +
                    " }"),
            @ApiImplicitParam(name = "file", dataType = "file", paramType = "form"),
            @ApiImplicitParam(name = "name", dataType = "string", paramType = "form", example = "Simple content item"),
            @ApiImplicitParam(name = "type", dataType = "string", paramType = "form", example = "integer"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the job to create the variables was created."),
            @ApiResponse(code = 400, message = "Indicates the request body is incomplete or contains illegal values. The status description contains additional information about the error."),
            @ApiResponse(code = 409, message = "Indicates the process instance was found but already contains a variable with the given name (only thrown when POST method is used). Use the update-method instead."),

    })
    @PostMapping(value = "/runtime/process-instances/{processInstanceId}/variables-async", consumes = {"application/json", "multipart/form-data"})
    public void createExecutionVariableAsync(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId, HttpServletRequest request, HttpServletResponse response) {
        Execution execution = getExecutionFromRequestWithoutAccessCheck(processInstanceId);
        createExecutionVariable(execution, false, true, request, response);
    }

    // FIXME Documentation
    @ApiOperation(value = "Delete all variables", tags = { "Process Instance Variables" }, nickname = "deleteLocalProcessVariable", code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates variables were found and have been deleted. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested process instance was not found.")
    })
    @DeleteMapping(value = "/runtime/process-instances/{processInstanceId}/variables")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLocalVariables(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId) {
        Execution execution = getExecutionFromRequestWithoutAccessCheck(processInstanceId);
        deleteAllLocalVariables(execution);
    }

    @Override
    protected void addGlobalVariables(Execution execution, Map<String, RestVariable> variableMap) {
        // no global variables
    }

    // For process instance there's only one scope. Using the local variables
    // method for that
    @Override
    protected void addLocalVariables(Execution execution, Map<String, RestVariable> variableMap) {
        Map<String, Object> rawVariables = runtimeService.getVariables(execution.getId());
        List<RestVariable> globalVariables = restResponseFactory.createRestVariables(rawVariables, execution.getId(), variableType, RestVariableScope.LOCAL);

        // Overlay global variables over local ones. In case they are present
        // the values are not overridden,
        // since local variables get precedence over global ones at all times.
        for (RestVariable var : globalVariables) {
            if (!variableMap.containsKey(var.getName())) {
                variableMap.put(var.getName(), var);
            }
        }
    }
}
