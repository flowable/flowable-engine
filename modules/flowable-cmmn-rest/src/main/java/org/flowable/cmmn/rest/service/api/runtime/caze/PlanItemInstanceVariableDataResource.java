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

import jakarta.servlet.http.HttpServletResponse;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Christopher Welsch
 */
@RestController
@Api(tags = { "Plan Item Instance Variables" }, authorizations = { @Authorization(value = "basicAuth") })
public class PlanItemInstanceVariableDataResource extends BaseVariableResource {

    @ApiOperation(value = "Get the binary data for a variable", tags = { "Plan Item Instance Variables" }, nickname = "getPlanItemInstanceVariableData")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the plan item instance was found and the requested variables are returned."),
            @ApiResponse(code = 404, message = "Indicates the requested plan item was not found or the plan item does not have a variable with the given name (in the given scope). Status message provides additional information.")
    })
    @ResponseBody
    @GetMapping(value = "/cmmn-runtime/plan-item-instances/{planItemInstanceId}/variables/{variableName}/data")
    public byte[] getVariableData(@ApiParam(name = "planItemInstanceId") @PathVariable("planItemInstanceId") String planItemInstanceId,
            @ApiParam(name = "variableName") @PathVariable("variableName") String variableName,
            @RequestParam(value = "scope", required = false) String scope,
            HttpServletResponse response) {

        PlanItemInstance planItemInstance = getPlanItemInstanceFromRequest(planItemInstanceId);
        return getVariableDataByteArray(planItemInstance, variableName, response);
    }
}
