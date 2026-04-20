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

package org.flowable.cmmn.rest.service.api.runtime.planitem;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.rest.service.api.runtime.caze.BaseVariableResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
 * @author Christopher Welsch
 */
@RestController
@Api(tags = { "Plan Item Instance" }, authorizations = { @Authorization(value = "basicAuth") })
public class PlanItemInstanceVariableCollectionResource extends BaseVariableResource {

    @Autowired
    protected CmmnEngineConfiguration cmmnEngineConfiguration;

    @ApiOperation(value = "Create a variable on a plan item", tags = { "Plan Item Instances" }, nickname = "createPlanItemInstanceVariable",
            notes = "This endpoint can be used in 2 ways: By passing a JSON Body (RestVariable) or by passing a multipart/form-data Object.\n"
                    + "NB: Swagger V2 specification does not support this use case that is why this endpoint might be buggy/incomplete if used with other tools.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "body", type = "org.flowable.cmmn.rest.service.api.engine.variable.RestVariable", value = "Create a variable on a plan item instance", paramType = "body", example =
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
            @ApiResponse(code = 201, message = "Indicates both the plan item instance and variable were found and variable is created."),
            @ApiResponse(code = 400, message = "Indicates the request body is incomplete or contains illegal values. The status description contains additional information about the error."),
            @ApiResponse(code = 404, message = "Indicates the requested plan item instance was not found or the plan item instance does not have a variable with the given name. Status description contains additional information about the error."),
            @ApiResponse(code = 409, message = "Indicates the plan item instance was found but already contains a variable with the given name. Use the update-method instead.")
    })
    @PostMapping(value = "/cmmn-runtime/plan-item-instances/{planItemInstanceId}/variables", produces = "application/json", consumes = {
            "application/json", "multipart/form-data" })
    public Object createPlanItemInstanceVariable(@ApiParam(name = "planItemInstanceId") @PathVariable("planItemInstanceId") String planItemInstanceId,
            HttpServletRequest request, HttpServletResponse response) {

        PlanItemInstance planItem = getPlanItemInstanceFromRequest(planItemInstanceId);
        return createVariable(planItem, false, request, response);
    }

    @ApiOperation(value = "Create a variable on a plan item asynchronously", tags = { "Plan Item Instances" }, nickname = "createPlanItemInstanceVariableAsync",
            notes = "This endpoint can be used in 2 ways: By passing a JSON Body (RestVariable) or by passing a multipart/form-data Object.\n"
                    + "NB: Swagger V2 specification does not support this use case that is why this endpoint might be buggy/incomplete if used with other tools.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "body", type = "org.flowable.cmmn.rest.service.api.engine.variable.RestVariable", value = "Create a variable on a plan item instance", paramType = "body", example =
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
            @ApiResponse(code = 201, message = "Indicates the job to create a variable is created."),
            @ApiResponse(code = 400, message = "Indicates the request body is incomplete or contains illegal values. The status description contains additional information about the error."),
            @ApiResponse(code = 404, message = "Indicates the plan item instance does not have a variable with the given name. Status description contains additional information about the error."),
            @ApiResponse(code = 409, message = "Indicates the plan item instance already contains a variable with the given name. Use the update-method instead.")
    })
    @PostMapping(value = "/cmmn-runtime/plan-item-instances/{planItemInstanceId}/variables-async", consumes = {"application/json", "multipart/form-data" })
    public void createPlanItemInstanceVariableAsync(@ApiParam(name = "planItemInstanceId") @PathVariable("planItemInstanceId") String planItemInstanceId,
            HttpServletRequest request, HttpServletResponse response) {

        PlanItemInstance planItem = getPlanItemInstanceFromRequest(planItemInstanceId);
        createVariable(planItem, true, request, response);
    }
}
