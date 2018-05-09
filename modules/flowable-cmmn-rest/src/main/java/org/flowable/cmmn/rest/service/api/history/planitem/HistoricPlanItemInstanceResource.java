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

package org.flowable.cmmn.rest.service.api.history.planitem;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Tijs Rademakers
 * @author Dennis Federico
 */
@RestController
@Api(tags = {"History PlanItem"}, description = "Manage History Plan Item Instances", authorizations = {@Authorization(value = "basicAuth")})
public class HistoricPlanItemInstanceResource extends HistoricPlanItemInstanceBaseResource {

    @ApiOperation(value = "Get a historic plan item instance", tags = {"History PlanItem"}, nickname = "getHistoricPlanItemInstance")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates that the historic plan item instances could be found."),
            @ApiResponse(code = 404, message = "Indicates that the historic plan item instances could not be found.")})
    @GetMapping(value = "/cmmn-history/historic-planitem-instances/{planItemInstanceId}", produces = "application/json")
    public HistoricPlanItemInstanceResponse getPlanItemInstance(@ApiParam(name = "planItemInstanceId") @PathVariable String planItemInstanceId, HttpServletRequest request) {
        HistoricPlanItemInstance planItemInstance = historyService.createHistoricPlanItemInstanceQuery().planItemInstanceId(planItemInstanceId).singleResult();
        if (planItemInstance == null) {
            throw new FlowableObjectNotFoundException("Could not find a plan item instance with id '" + planItemInstanceId + "'.", HistoricPlanItemInstance.class);
        }
        return restResponseFactory.createHistoricPlanItemInstanceResponse(planItemInstance);
    }

}
