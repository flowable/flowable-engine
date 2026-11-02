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

import jakarta.servlet.http.HttpServletResponse;

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
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Historic Decision Executions" }, authorizations = { @Authorization(value = "basicAuth") })
public class HistoricDecisionExecutionResourceDataResource extends BaseHistoricDecisionExecutionResource {

    @ApiOperation(value = "Get a historic decision execution audit content", tags = { "Historic Decision Executions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the historic decision execution has been found and the audit data has been returned."),
            @ApiResponse(code = 404, message = "Indicates the requested historic decision execution was not found. The status-description contains additional information.")
    })
    @GetMapping(value = "/dmn-history/historic-decision-executions/{historicDecisionExecutionId}/auditdata", produces = "application/json")
    @ResponseBody
    public String getHistoricDecisionExecutionAuditData(@ApiParam(name = "historicDecisionExecutionId") @PathVariable String historicDecisionExecutionId, HttpServletResponse response) {
        return getExecutionAuditData(historicDecisionExecutionId, response);
    }
}
