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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.runtime.Execution;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.rest.service.api.engine.variable.RestVariable;
import org.flowable.rest.service.api.engine.variable.RestVariable.RestVariableScope;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Executions" }, description = "Manage Executions", authorizations = { @Authorization(value = "basicAuth") })
public class ExecutionVariableResource extends BaseExecutionVariableResource {

    @Autowired
    protected ObjectMapper objectMapper;

    @ApiOperation(value = "Get a variable for an execution", tags = { "Executions" }, nickname = "getExecutionVariable")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates both the execution and variable were found and variable is returned."),
            @ApiResponse(code = 400, message = "Indicates the request body is incomplete or contains illegal values. The status description contains additional information about the error."),
            @ApiResponse(code = 404, message = "Indicates the requested execution was not found or the execution does not have a variable with the given name in the requested scope (in case scope-query parameter was omitted, variable doesn’t exist in local and global scope). Status description contains additional information about the error.")
    })
    @GetMapping(value = "/runtime/executions/{executionId}/variables/{variableName}", produces = "application/json")
    public RestVariable getVariable(@ApiParam(name = "executionId") @PathVariable("executionId") String executionId, @ApiParam(name = "variableName") @PathVariable("variableName") String variableName,
            @RequestParam(value = "scope", required = false) String scope,
            HttpServletRequest request) {

        Execution execution = getExecutionFromRequest(executionId);
        return getVariableFromRequest(execution, variableName, scope, false);
    }

    // FIXME OASv3 to solve Multiple Endpoint issue
    @ApiOperation(value = "Update a variable on an execution", tags = { "Executions" }, nickname = "updateExecutionVariable",
            notes = "This endpoint can be used in 2 ways: By passing a JSON Body (RestVariable) or by passing a multipart/form-data Object.\n"
                    + "NB: Swagger V2 specification doesn't support this use case that's why this endpoint might be buggy/incomplete if used with other tools.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "body", type = "org.flowable.rest.service.api.engine.variable.RestVariable", value = "Update a variable on an execution", paramType = "body", example = "{\n" +
                    "    \"name\":\"intProcVar\"\n" +
                    "    \"type\":\"integer\"\n" +
                    "    \"value\":123,\n" +
                    "    \"scope\":\"global\"\n" +
                    " }"),
            @ApiImplicitParam(name = "file", dataType = "file", paramType = "form"),
            @ApiImplicitParam(name = "name", dataType = "string", paramType = "form", example = "intProcVar"),
            @ApiImplicitParam(name = "type", dataType = "string", paramType = "form", example = "integer"),
            @ApiImplicitParam(name = "scope", dataType = "string",  paramType = "form", example = "global"),

    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates both the process instance and variable were found and variable is updated."),
            @ApiResponse(code = 404, message = "Indicates the requested process instance was not found or the process instance does not have a variable with the given name. Status description contains additional information about the error.")
    })
    @PutMapping(value = "/runtime/executions/{executionId}/variables/{variableName}", produces = "application/json", consumes = {"application/json", "multipart/form-data"})
    public RestVariable updateVariable(@ApiParam(name = "executionId") @PathVariable("executionId") String executionId, @ApiParam(name = "variableName") @PathVariable("variableName") String variableName, HttpServletRequest request) {

        Execution execution = getExecutionFromRequest(executionId);

        RestVariable result = null;
        if (request instanceof MultipartHttpServletRequest) {
            result = setBinaryVariable((MultipartHttpServletRequest) request, execution, RestResponseFactory.VARIABLE_EXECUTION, false);

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

            result = setSimpleVariable(restVariable, execution, false);
        }
        return result;
    }

    @ApiOperation(value = "Delete a variable for an execution", tags = { "Executions" }, nickname = "deletedExecutionVariable")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates both the execution and variable were found and variable has been deleted."),
            @ApiResponse(code = 404, message = "Indicates the requested execution was not found or the execution does not have a variable with the given name in the requested scope. Status description contains additional information about the error.")
    })
    @DeleteMapping(value = "/runtime/executions/{executionId}/variables/{variableName}")
    public void deleteVariable(@ApiParam(name = "executionId") @PathVariable("executionId") String executionId, @ApiParam(name = "variableName") @PathVariable("variableName") String variableName,
            @RequestParam(value = "scope", required = false) String scope,
            HttpServletResponse response) {

        Execution execution = getExecutionFromRequest(executionId);
        // Determine scope
        RestVariableScope variableScope = RestVariableScope.LOCAL;
        if (scope != null) {
            variableScope = RestVariable.getScopeFromString(scope);
        }

        if (!hasVariableOnScope(execution, variableName, variableScope)) {
            throw new FlowableObjectNotFoundException("Execution '" + execution.getId() + "' doesn't have a variable '" + variableName + "' in scope " + variableScope.name().toLowerCase(),
                    VariableInstanceEntity.class);
        }

        if (variableScope == RestVariableScope.LOCAL) {
            runtimeService.removeVariableLocal(execution.getId(), variableName);
        } else {
            // Safe to use parentId, as the hasVariableOnScope would have
            // stopped a global-var update on a root-execution
            runtimeService.removeVariable(execution.getParentId(), variableName);
        }
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }
}
