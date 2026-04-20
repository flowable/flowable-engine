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

import jakarta.servlet.http.HttpServletResponse;

import org.flowable.dmn.api.DmnDecision;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Yvo Swillens
 */
@RestController
@Api(tags = { "Decisions" }, authorizations = { @Authorization(value = "basicAuth") })
public class DecisionResourceDataResource extends BaseDecisionResource {

    @ApiOperation(value = "Get a decision resource content", tags = { "Decisions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates both decision and resource have been found and the resource data has been returned."),
            @ApiResponse(code = 404, message = "Indicates the requested decision was not found or there is no resource with the given id present in the decision . The status-description contains additional information.")
    })
    @GetMapping(value = "/dmn-repository/decisions/{decisionId}/resourcedata", produces = "application/json")
    @ResponseBody
    public byte[] getDecisionResource(@ApiParam(name = "decisionId") @PathVariable String decisionId, HttpServletResponse response) {
        DmnDecision decision = getDecisionFromRequest(decisionId);
        return getDeploymentResourceData(decision.getDeploymentId(), decision.getResourceName(), response);
    }
}
