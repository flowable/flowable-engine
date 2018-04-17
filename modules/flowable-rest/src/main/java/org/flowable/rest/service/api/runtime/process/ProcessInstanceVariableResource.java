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
@Api(tags = { "Process Instance Variables" }, description = "Manage Process Instances", authorizations = { @Authorization(value = "basicAuth") })
public class ProcessInstanceVariableResource extends BaseExecutionVariableResource {

    @Autowired
    protected ObjectMapper objectMapper;

    @ApiOperation(value = "Get a variable for a process instance", tags = { "Process Instance Variables" }, nickname = "getProcessInstanceVariable")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates both the process instance and variable were found and variable is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested process instance was not found or the process instance does not have a variable with the given name. Status description contains additional information about the error.")
    })
    @GetMapping(value = "/runtime/process-instances/{processInstanceId}/variables/{variableName}", produces = "application/json")
    public RestVariable getVariable(@ApiParam(name = "processInstanceId") @PathVariable("processInstanceId") String processInstanceId, @ApiParam(name = "variableName") @PathVariable("variableName") String variableName,
            @RequestParam(value = "scope", required = false) String scope, HttpServletRequest request) {

        Execution execution = getProcessInstanceFromRequest(processInstanceId);
        return getVariableFromRequest(execution, variableName, scope, false);
    }

    // FIXME OASv3 to solve Multiple Endpoint issue
    @ApiOperation(value = "Update a single variable on a process instance", tags = { "Process Instance Variables" }, nickname = "updateProcessInstanceVariable",
            notes = "This endpoint can be used in 2 ways: By passing a JSON Body (RestVariable) or by passing a multipart/form-data Object.\n"
                    + "Nonexistent variables are created on the process-instance and existing ones are overridden without any error.\n"
                    + "Note that scope is ignored, only local variables can be set in a process instance.\n"
                    + "NB: Swagger V2 specification doesn't support this use case that's why this endpoint might be buggy/incomplete if used with other tools.")
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
            @ApiResponse(code = 201, message = "Indicates both the process instance and variable were found and variable is updated."),
            @ApiResponse(code = 404, message = "Indicates the requested process instance was not found or the process instance does not have a variable with the given name. Status description contains additional information about the error.")
    })
    @PutMapping(value = "/runtime/process-instances/{processInstanceId}/variables/{variableName}", produces = "application/json", consumes = {"application/json", "multipart/form-data"})
    public RestVariable updateVariable(@ApiParam(name = "processInstanceId") @PathVariable("processInstanceId") String processInstanceId, @ApiParam(name = "variableName") @PathVariable("variableName") String variableName,
            HttpServletRequest request) {

        Execution execution = getProcessInstanceFromRequest(processInstanceId);

        RestVariable result = null;
        if (request instanceof MultipartHttpServletRequest) {
            result = setBinaryVariable((MultipartHttpServletRequest) request, execution, RestResponseFactory.VARIABLE_PROCESS, false);

            if (!result.getName().equals(variableName)) {
                throw new FlowableIllegalArgumentException("Variable name in the body should be equal to the name used in the requested URL.");
            }

        } else {
            RestVariable restVariable = null;
            try {
                restVariable = objectMapper.readValue(request.getInputStream(), RestVariable.class);
            } catch (Exception e) {
                throw new FlowableIllegalArgumentException("request body could not be transformed to a RestVariable instance.");
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

    // FIXME Documentation
    @ApiOperation(value = "Delete a variable", tags = { "Process Instance Variables" }, nickname = "deleteProcessInstanceVariable")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the variable was found and has been deleted. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested variable was not found.")
    })
    @DeleteMapping(value = "/runtime/process-instances/{processInstanceId}/variables/{variableName}")
    public void deleteVariable(@ApiParam(name = "processInstanceId") @PathVariable("processInstanceId") String processInstanceId, @ApiParam(name = "variableName") @PathVariable("variableName") String variableName,
            @RequestParam(value = "scope", required = false) String scope, HttpServletResponse response) {

        Execution execution = getProcessInstanceFromRequest(processInstanceId);
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

    @Override
    protected RestVariable constructRestVariable(String variableName, Object value, RestVariableScope variableScope, String executionId, boolean includeBinary) {

        return restResponseFactory.createRestVariable(variableName, value, null, executionId, RestResponseFactory.VARIABLE_PROCESS, includeBinary);
    }
}
