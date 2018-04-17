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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.rest.service.api.RestActionRequest;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Plan Item Instances" }, description = "Manage Plan Item Instances", authorizations = { @Authorization(value = "basicAuth") })
public class PlanItemInstanceResource extends PlanItemInstanceBaseResource {

    @ApiOperation(value = "Get an plan item instance", tags = { "Plan Item Instances" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the plan item instance was found and returned."),
            @ApiResponse(code = 404, message = "Indicates the plan item instance was not found.")
    })
    @GetMapping(value = "/cmmn-runtime/plan-item-instances/{planItemInstanceId}", produces = "application/json")
    public PlanItemInstanceResponse getPlanItemInstance(@ApiParam(name = "planItemInstanceId") @PathVariable String planItemInstanceId, HttpServletRequest request) {
        return restResponseFactory.createPlanItemInstanceResponse(getPlanItemInstanceFromRequest(planItemInstanceId));
    }

    @ApiOperation(value = "Execute an action on a plan item instance", tags = { "Plan Item Instances" }, notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the plan item instance was found and the action is performed."),
            @ApiResponse(code = 204, message = "Indicates the plan item instance was found, the action was performed and the action caused the plan item instance to end."),
            @ApiResponse(code = 400, message = "Indicates an illegal action was requested, required parameters are missing in the request body or illegal variables are passed in. Status description contains additional information about the error."),
            @ApiResponse(code = 404, message = "Indicates the plan item instance was not found.")
    })
    @PutMapping(value = "/cmmn-runtime/plan-item-instances/{planItemInstanceId}", produces = "application/json")
    public PlanItemInstanceResponse performPlanItemInstanceAction(@ApiParam(name = "planItemInstanceId") @PathVariable String planItemInstanceId, 
                    @RequestBody RestActionRequest actionRequest, HttpServletRequest request, HttpServletResponse response) {

        PlanItemInstance planItemInstance = getPlanItemInstanceFromRequest(planItemInstanceId);

        if (RestActionRequest.TRIGGER.equals(actionRequest.getAction())) {
            runtimeService.triggerPlanItemInstance(planItemInstance.getId());
            
        } else if (RestActionRequest.ENABLE.equals(actionRequest.getAction())) {
            runtimeService.startPlanItemInstance(planItemInstanceId);
            
        } else if (RestActionRequest.DISABLE.equals(actionRequest.getAction())) {
            runtimeService.disablePlanItemInstance(planItemInstanceId);
            
        } else if (RestActionRequest.START.equals(actionRequest.getAction())) {
            runtimeService.startPlanItemInstance(planItemInstanceId);
            
        } else {
            throw new FlowableIllegalArgumentException("Invalid action: '" + actionRequest.getAction() + "'.");
        }

        // Re-fetch the execution, could have changed due to action or even completed
        planItemInstance = runtimeService.createPlanItemInstanceQuery().planItemInstanceId(planItemInstance.getId()).singleResult();
        if (planItemInstance == null) {
            // Execution is finished, return empty body to inform user
            response.setStatus(HttpStatus.NO_CONTENT.value());
            return null;
        } else {
            return restResponseFactory.createPlanItemInstanceResponse(planItemInstance);
        }
    }
}
