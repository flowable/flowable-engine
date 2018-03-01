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

package org.flowable.cmmn.rest.service.api.runtime.caze;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.rest.service.api.RestResponseFactory;
import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Case Instance Variables" }, description = "Manage Case Instances Variables", authorizations = { @Authorization(value = "basicAuth") })
public class CaseInstanceVariableCollectionResource extends BaseVariableResource {

    @ApiOperation(value = "List variables for a case instance", nickname="listCaseInstanceVariables", tags = {"Case Instance Variables" },
            notes = "In case the variable is a binary variable or serializable, the valueUrl points to an URL to fetch the raw value. If it’s a plain variable, the value is present in the response. Note that only local scoped variables are returned, as there is no global scope for process-instance variables.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the case instance was found and variables are returned."),
            @ApiResponse(code = 400, message = "Indicates the requested case instance was not found.")
    })
    @GetMapping(value = "/cmmn-runtime/case-instances/{caseInstanceId}/variables", produces = "application/json")
    public List<RestVariable> getVariables(@ApiParam(name = "caseInstanceId") @PathVariable String caseInstanceId, HttpServletRequest request) {

        CaseInstance caseInstance = getCaseInstanceFromRequest(caseInstanceId);
        return processCaseVariables(caseInstance, RestResponseFactory.VARIABLE_CASE);
    }

    @ApiOperation(value = "Update a multiple/single (non)binary variable on a case instance", tags = { "Case Instance Variables" }, nickname = "createOrUpdateCaseVariable",
            notes = "This endpoint can be used in 2 ways: By passing a JSON Body (RestVariable or an array of RestVariable) or by passing a multipart/form-data Object.\n"
                    + "Nonexistent variables are created on the process-instance and existing ones are overridden without any error.\n"
                    + "Any number of variables can be passed into the request body array.\n"
                    + "Note that scope is ignored, only local variables can be set in a case instance.\n"
                    + "NB: Swagger V2 specification doesn't support this use case that's why this endpoint might be buggy/incomplete if used with other tools.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "body", type = "org.flowable.cmmn.rest.service.api.engine.variable.RestVariable", value = "Create a variable on a process instance", paramType = "body", example = "{\n" +
                    "    \"name\":\"intProcVar\"\n" +
                    "    \"type\":\"integer\"\n" +
                    "    \"value\":123,\n" +
                    " }"),
            @ApiImplicitParam(name = "file", dataType = "file", paramType = "form"),
            @ApiImplicitParam(name = "name", dataType = "string", paramType = "form", example = "Simple content item"),
            @ApiImplicitParam(name = "type", dataType = "string", paramType = "form", example = "integer"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the case instance was found and variable is created."),
            @ApiResponse(code = 400, message = "Indicates the request body is incomplete or contains illegal values. The status description contains additional information about the error."),
            @ApiResponse(code = 404, message = "Indicates the requested case instance was not found."),
            @ApiResponse(code = 415, message = "Indicates the serializable data contains an object for which no class is present in the JVM running the Flowable engine and therefore cannot be deserialized.")

    })
    @PutMapping(value = "/cmmn-runtime/case-instances/{caseInstanceId}/variables", produces = "application/json", consumes = {"application/json", "multipart/form-data"})
    public Object createOrUpdateExecutionVariable(@ApiParam(name = "caseInstanceId") @PathVariable String caseInstanceId, HttpServletRequest request, HttpServletResponse response) {

        CaseInstance caseInstance = getCaseInstanceFromRequest(caseInstanceId);
        return createVariable(caseInstance, RestResponseFactory.VARIABLE_CASE, request, response);
    }

    @ApiOperation(value = "Create variables or new binary variable on a case instance", tags = { "Case Instance Variables" }, nickname = "createCaseInstanceVariable",
            notes = "This endpoint can be used in 2 ways: By passing a JSON Body (RestVariable or an array of RestVariable) or by passing a multipart/form-data Object.\n"
                    + "Nonexistent variables are created on the process-instance and existing ones are overridden without any error.\n"
                    + "Any number of variables can be passed into the request body array.\n"
                    + "Note that scope is ignored, only local variables can be set in a case instance.\n"
                    + "NB: Swagger V2 specification doesn't support this use case that's why this endpoint might be buggy/incomplete if used with other tools.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "body", type = "org.flowable.cmmn.rest.service.api.engine.variable.RestVariable", value = "Create a variable on a case instance", paramType = "body", example = "{\n" +
                    "    \"name\":\"intProcVar\"\n" +
                    "    \"type\":\"integer\"\n" +
                    "    \"value\":123,\n" +
                    " }"),
            @ApiImplicitParam(name = "file", dataType = "file", paramType = "form"),
            @ApiImplicitParam(name = "name", dataType = "string", paramType = "form", example = "Simple content item"),
            @ApiImplicitParam(name = "type", dataType = "string", paramType = "form", example = "integer"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the case instance was found and variable is created."),
            @ApiResponse(code = 400, message = "Indicates the request body is incomplete or contains illegal values. The status description contains additional information about the error."),
            @ApiResponse(code = 404, message = "Indicates the requested case instance was not found."),
            @ApiResponse(code = 409, message = "Indicates the case instance was found but already contains a variable with the given name (only thrown when POST method is used). Use the update-method instead."),

    })
    
    @PostMapping(value = "/cmmn-runtime/case-instances/{caseInstanceId}/variables", produces = "application/json", consumes = {"application/json", "multipart/form-data", "text/plain"})
    public Object createExecutionVariable(@ApiParam(name = "caseInstanceId") @PathVariable String caseInstanceId, HttpServletRequest request, HttpServletResponse response) {
        CaseInstance caseInstance = getCaseInstanceFromRequest(caseInstanceId);
        return createVariable(caseInstance, RestResponseFactory.VARIABLE_CASE, request, response);
    }

    @ApiOperation(value = "Delete all variables", tags = { "Case Instance Variables" }, nickname = "deleteCaseVariable")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates variables were found and have been deleted. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested case instance was not found.")
    })
    @DeleteMapping(value = "/cmmn-runtime/case-instances/{caseInstanceId}/variables")
    public void deleteLocalVariables(@ApiParam(name = "caseInstanceId") @PathVariable String caseInstanceId, HttpServletResponse response) {
        CaseInstance caseInstance = getCaseInstanceFromRequest(caseInstanceId);
        deleteAllVariables(caseInstance, response);
    }
}
