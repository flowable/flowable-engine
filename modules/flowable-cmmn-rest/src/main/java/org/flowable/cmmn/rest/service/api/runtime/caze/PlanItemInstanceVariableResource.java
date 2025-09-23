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

import java.util.Collections;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.rest.service.api.CmmnRestResponseFactory;
import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author Christopher Welsch
 */
@RestController
@Api(tags = { "Plan Item Instance" }, authorizations = { @Authorization(value = "basicAuth") })
public class PlanItemInstanceVariableResource extends BaseVariableResource {

    @Autowired
    protected CmmnEngineConfiguration cmmnEngineConfiguration;

    @ApiOperation(value = "Update a variable on a plan item", tags = { "Plan Item Instances" }, nickname = "updatePlanItemVariable",
            notes = "This endpoint can be used in 2 ways: By passing a JSON Body (RestVariable) or by passing a multipart/form-data Object.\n"
                    + "NB: Swagger V2 specification does not support this use case that is why this endpoint might be buggy/incomplete if used with other tools.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "body", type = "org.flowable.rest.service.api.engine.variable.RestVariable", value = "Update a variable on a plan item instance", paramType = "body", example =
                    "{\n" +
                            "    \"name\":\"intProcVar\"\n" +
                            "    \"type\":\"integer\"\n" +
                            "    \"value\":123,\n" +
                            "    \"scope\":\"global\"\n" +
                            " }"),
            @ApiImplicitParam(name = "file", dataType = "file", paramType = "form"),
            @ApiImplicitParam(name = "name", dataType = "string", paramType = "form", example = "intProcVar"),
            @ApiImplicitParam(name = "type", dataType = "string", paramType = "form", example = "integer"),
            @ApiImplicitParam(name = "scope", dataType = "string", paramType = "form", example = "global"),

    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates both the plan item instance and variable were found and variable is updated."),
            @ApiResponse(code = 404, message = "Indicates the requested plan item instance was not found or the plan item instance does not have a variable with the given name. Status description contains additional information about the error.")
    })
    @PutMapping(value = "/cmmn-runtime/plan-item-instances/{planItemInstanceId}/variables/{variableName}", produces = "application/json", consumes = {
            "application/json", "multipart/form-data" })
    public RestVariable updateVariable(@ApiParam(name = "planItemInstanceId") @PathVariable("planItemInstanceId") String planItemInstanceId,
            @ApiParam(name = "variableName") @PathVariable("variableName") String variableName, HttpServletRequest request) {

        PlanItemInstance planItem = getPlanItemInstanceFromRequest(planItemInstanceId);

        RestVariable result = null;
        if (request instanceof MultipartHttpServletRequest) {
            result = setBinaryVariable((MultipartHttpServletRequest) request, planItem.getId(), CmmnRestResponseFactory.VARIABLE_PLAN_ITEM, false,
                    false, RestVariable.RestVariableScope.LOCAL, createVariableInterceptor(planItem));

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

            result = setSimpleVariable(restVariable, planItem.getId(), false, false, RestVariable.RestVariableScope.LOCAL, CmmnRestResponseFactory.VARIABLE_PLAN_ITEM, createVariableInterceptor(planItem));
        }
        return result;
    }
    
    @ApiOperation(value = "Update a variable on a plan item asynchronously", tags = { "Plan Item Instances" }, nickname = "updatePlanItemVariableAsync",
            notes = "This endpoint can be used in 2 ways: By passing a JSON Body (RestVariable) or by passing a multipart/form-data Object.\n"
                    + "NB: Swagger V2 specification does not support this use case that is why this endpoint might be buggy/incomplete if used with other tools.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "body", type = "org.flowable.rest.service.api.engine.variable.RestVariable", value = "Update a variable on a plan item instance", paramType = "body", example =
                    "{\n" +
                            "    \"name\":\"intProcVar\"\n" +
                            "    \"type\":\"integer\"\n" +
                            "    \"value\":123,\n" +
                            "    \"scope\":\"global\"\n" +
                            " }"),
            @ApiImplicitParam(name = "file", dataType = "file", paramType = "form"),
            @ApiImplicitParam(name = "name", dataType = "string", paramType = "form", example = "intProcVar"),
            @ApiImplicitParam(name = "type", dataType = "string", paramType = "form", example = "integer"),
            @ApiImplicitParam(name = "scope", dataType = "string", paramType = "form", example = "global"),

    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the job to update a variable is created."),
            @ApiResponse(code = 404, message = "Indicates the plan item instance does not have a variable with the given name. Status description contains additional information about the error.")
    })
    @PutMapping(value = "/cmmn-runtime/plan-item-instances/{planItemInstanceId}/variables-async/{variableName}", consumes = {"application/json", "multipart/form-data" })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateVariableAsync(@ApiParam(name = "planItemInstanceId") @PathVariable("planItemInstanceId") String planItemInstanceId,
            @ApiParam(name = "variableName") @PathVariable("variableName") String variableName, HttpServletRequest request) {

        PlanItemInstance planItem = getPlanItemInstanceFromRequest(planItemInstanceId);

        if (request instanceof MultipartHttpServletRequest) {
            setBinaryVariable((MultipartHttpServletRequest) request, planItem.getId(), CmmnRestResponseFactory.VARIABLE_PLAN_ITEM, false,
                    true, RestVariable.RestVariableScope.LOCAL, createVariableInterceptor(planItem));

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

            setSimpleVariable(restVariable, planItem.getId(), false, true, RestVariable.RestVariableScope.LOCAL, CmmnRestResponseFactory.VARIABLE_PLAN_ITEM, createVariableInterceptor(planItem));
        }
    }

    @ApiOperation(value = "Delete a variable for a plan item instance", tags = { "Plan Item Instances" }, nickname = "deletePlanItemVariable", code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates both the plan item and variable were found and variable has been deleted."),
            @ApiResponse(code = 404, message = "Indicates the requested plan item was not found or the plan item does not have a variable with the given name in the requested scope. Status description contains additional information about the error.")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(value = "/cmmn-runtime/plan-item-instances/{planItemInstanceId}/variables/{variableName}")
    public void deleteVariable(@ApiParam(name = "planItemInstanceId") @PathVariable("planItemInstanceId") String planItemInstanceId,
            @ApiParam(name = "variableName") @PathVariable("variableName") String variableName,
            @RequestParam(value = "scope", required = false) String scope) {

        PlanItemInstance planItem = getPlanItemInstanceFromRequest(planItemInstanceId);

        if (!runtimeService.hasLocalVariable(planItem.getId(), variableName)) {
            throw new FlowableObjectNotFoundException(
                    "Plan item instance '" + planItem.getId() + "' does not have a variable '" + variableName + "' in local scope",
                    VariableInstance.class);
        }

        if (restApiInterceptor != null) {
            restApiInterceptor.deletePlanItemInstanceVariables(planItem, Collections.singleton(variableName));
        }
        runtimeService.removeLocalVariable(planItem.getId(), variableName);
    }
}
