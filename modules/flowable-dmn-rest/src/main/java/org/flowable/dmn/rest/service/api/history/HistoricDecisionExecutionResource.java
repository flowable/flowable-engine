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
package org.flowable.dmn.rest.service.api.history;

import org.flowable.dmn.api.DmnHistoricDecisionExecution;
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
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Historic Decision Executions" }, authorizations = { @Authorization(value = "basicAuth") })
public class HistoricDecisionExecutionResource extends BaseHistoricDecisionExecutionResource {

    @ApiOperation(value = "Get a historic decision execution", tags = { "Historic Decision Executions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the historic decision execution is returned"),
            @ApiResponse(code = 404, message = "Indicates the requested historic decision execution was not found.")
    })
    @GetMapping(value = "/dmn-history/historic-decision-executions/{historicDecisionExecutionId}", produces = "application/json")
    public HistoricDecisionExecutionResponse getHistoricDecisionExecution(@ApiParam(name = "historicDecisionExecutionId") @PathVariable String historicDecisionExecutionId) {
        DmnHistoricDecisionExecution decisionExecution = getHistoricDecisionExecutionFromRequest(historicDecisionExecutionId);

        return dmnRestResponseFactory.createHistoryDecisionExecutionResponse(decisionExecution);
    }
}
