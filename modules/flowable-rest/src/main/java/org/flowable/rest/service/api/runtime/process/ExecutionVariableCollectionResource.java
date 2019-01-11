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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import org.flowable.engine.runtime.Execution;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.rest.service.api.engine.variable.RestVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Executions" }, description = "Manage Executions", authorizations = { @Authorization(value = "basicAuth") })
public class ExecutionVariableCollectionResource extends BaseVariableCollectionResource {

    @ApiOperation(value = "List variables for an execution", tags = { "Executions" }, nickname = "listExecutionVariables")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the execution was found and variables are returned."),
            @ApiResponse(code = 404, message = "Indicates the requested execution was not found.")
    })
    @GetMapping(value = "/runtime/executions/{executionId}/variables", produces = "application/json")
    public List<RestVariable> getVariables(@ApiParam(name = "executionId") @PathVariable String executionId, @RequestParam(value = "scope", required = false) String scope, HttpServletRequest request) {

        Execution execution = getExecutionFromRequest(executionId);
        return processVariables(execution, scope, RestResponseFactory.VARIABLE_EXECUTION);
    }

    // FIXME OASv3 to solve Multiple Endpoint issue
    @ApiOperation(value = "Update variables on an execution", tags = { "Executions" }, nickname = "createOrUpdateExecutionVariable",
            notes = "This endpoint can be used in 2 ways: By passing a JSON Body (array of RestVariable) or by passing a multipart/form-data Object.\n"
            + "Any number of variables can be passed into the request body array.\n"
            + "NB: Swagger V2 specification does not support this use case that is why this endpoint might be buggy/incomplete if used with other tools.")
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
            @ApiResponse(code = 201, message = "Indicates the execution was found and variable is created/updated."),
            @ApiResponse(code = 400, message = "Indicates the request body is incomplete or contains illegal values. The status description contains additional information about the error."),
            @ApiResponse(code = 404, message = "Indicates the requested execution was not found.")
    })
    @PutMapping(value = "/runtime/executions/{executionId}/variables", produces = "application/json", consumes = {"application/json", "multipart/form-data"})
    public Object createOrUpdateExecutionVariable(@ApiParam(name = "executionId") @PathVariable String executionId, HttpServletRequest request, HttpServletResponse response) {

        Execution execution = getExecutionFromRequest(executionId);
        return createExecutionVariable(execution, true, RestResponseFactory.VARIABLE_EXECUTION, request, response);
    }

    // FIXME OASv3 to solve Multiple Endpoint issue
    @ApiOperation(value = "Create variables on an execution", tags = { "Executions" }, nickname = "createExecutionVariable",
            notes = "This endpoint can be used in 2 ways: By passing a JSON Body (array of RestVariable) or by passing a multipart/form-data Object.\n"
            + "Any number of variables can be passed into the request body array.\n"
            + "NB: Swagger V2 specification does not support this use case that is why this endpoint might be buggy/incomplete if used with other tools.")
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
            @ApiResponse(code = 201, message = "Indicates the execution was found and variable is created/updated."),
            @ApiResponse(code = 400, message = "Indicates the request body is incomplete or contains illegal values. The status description contains additional information about the error."),
            @ApiResponse(code = 404, message = "Indicates the requested execution was not found."),
            @ApiResponse(code = 409, message = "Indicates the execution was found but already contains a variable with the given name. Use the update-method instead.")

    })
    @PostMapping(value = "/runtime/executions/{executionId}/variables", produces = "application/json", consumes = {"application/json", "multipart/form-data"})
    public Object createExecutionVariable(@ApiParam(name = "executionId") @PathVariable String executionId, HttpServletRequest request, HttpServletResponse response) {

        Execution execution = getExecutionFromRequest(executionId);
        return createExecutionVariable(execution, false, RestResponseFactory.VARIABLE_EXECUTION, request, response);
    }

    @ApiOperation(value = "Delete all variables for an execution", tags = { "Executions" })
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the execution was found and variables have been deleted."),
            @ApiResponse(code = 404, message = "Indicates the requested execution was not found.")
    })
    @DeleteMapping(value = "/runtime/executions/{executionId}/variables")
    public void deleteLocalVariables(@ApiParam(name = "executionId") @PathVariable String executionId, HttpServletResponse response) {
        Execution execution = getExecutionFromRequest(executionId);
        deleteAllLocalVariables(execution, response);
    }

}
