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
package org.flowable.dmn.rest.service.api.repository;

import org.flowable.dmn.api.DmnDecision;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Yvo Swillens
 *
 * @deprecated use {@link DecisionResource} instead.
 */
@Deprecated
@RestController
@Api(tags = { "Decision Tables" }, description = "Manage Decision Tables", authorizations = { @Authorization(value = "basicAuth") })
public class DecisionTableResource extends BaseDecisionResource {

    @ApiOperation(value = "Get a decision table", tags = { "Decision Tables" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the decision table is returned"),
            @ApiResponse(code = 404, message = "Indicates the requested decision table was not found.")
    })
    @GetMapping(value = "/dmn-repository/decision-tables/{decisionTableId}", produces = "application/json")
    public DecisionResponse getDecisionTable(@ApiParam(name = "decisionTableId") @PathVariable String decisionTableId) {
        DmnDecision decisionTable = getDecisionFromRequest(decisionTableId);

        return dmnRestResponseFactory.createDecisionTableResponse(decisionTable);
    }
}
