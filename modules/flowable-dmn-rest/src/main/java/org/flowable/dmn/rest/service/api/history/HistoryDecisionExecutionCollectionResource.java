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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.dmn.api.DmnHistoricDecisionExecutionQuery;
import org.flowable.dmn.api.DmnHistoryService;
import org.flowable.dmn.engine.impl.HistoricDecisionExecutionQueryProperty;
import org.flowable.dmn.rest.service.api.DmnRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
@Api(tags = { "Historic Decision Executions" }, description = "Query Historic Decision Executions", authorizations = { @Authorization(value = "basicAuth") })
public class HistoryDecisionExecutionCollectionResource {

    private static final Map<String, QueryProperty> properties = new HashMap<>();

    static {
        properties.put("startTime", HistoricDecisionExecutionQueryProperty.START_TIME);
        properties.put("endTime", HistoricDecisionExecutionQueryProperty.END_TIME);
        properties.put("tenantId", HistoricDecisionExecutionQueryProperty.TENANT_ID);
    }

    @Autowired
    protected DmnRestResponseFactory dmnRestResponseFactory;

    @Autowired
    protected DmnHistoryService dmnHistoryService;

    @ApiOperation(value = "List of historic decision executions", nickname ="listHistoricDecisionExecutions", tags = { "Historic Decision Executions" })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "string", value = "Only return historic decision executions with the given id.", paramType = "query"),
            @ApiImplicitParam(name = "decisionDefinitionId", dataType = "string", value = "Only return historic decision executions with the given definition id.", paramType = "query"),
            @ApiImplicitParam(name = "deploymentId", dataType = "string", value = "Only return historic decision executions with the given deployment id.", paramType = "query"),
            @ApiImplicitParam(name = "decisionKey", dataType = "string", value = "Only return historic decision executions with the given decision key.", paramType = "query"),
            @ApiImplicitParam(name = "activityId", dataType = "string", value = "Only return historic decision executions with the given activity id.", paramType = "query"),
            @ApiImplicitParam(name = "executionId", dataType = "string", value = "Only return historic decision executions with the given execution id.", paramType = "query"),
            @ApiImplicitParam(name = "instanceId", dataType = "string", value = "Only return historic decision executions with the given instance id.", paramType = "query"),
            @ApiImplicitParam(name = "scopeType", dataType = "string", value = "Only return historic decision executions with the given scope type.", paramType = "query"),
            @ApiImplicitParam(name = "failed", dataType = "string", value = "Only return historic decision executions with the failed state.", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return historic decision executions with the given tenant id.", paramType = "query"),
            @ApiImplicitParam(name = "tenantIdLike", dataType = "string", value = "Only return historic decision executions like the given tenant id.", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues = "startTime,endTime,tenantId", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the historic decision executions are returned"),
            @ApiResponse(code = 400, message = "Indicates a parameter was passed in the wrong format. The status-message contains additional information.")
    })
    @GetMapping(value = "/dmn-history/historic-decision-executions", produces = "application/json")
    public DataResponse<HistoricDecisionExecutionResponse> getDecisionTables(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        DmnHistoricDecisionExecutionQuery historicDecisionExecutionQuery = dmnHistoryService.createHistoricDecisionExecutionQuery();

        // Populate filter-parameters
        if (allRequestParams.containsKey("id")) {
            historicDecisionExecutionQuery.id(allRequestParams.get("id"));
        }
        if (allRequestParams.containsKey("decisionDefinitionId")) {
            historicDecisionExecutionQuery.decisionDefinitionId(allRequestParams.get("decisionDefinitionId"));
        }
        if (allRequestParams.containsKey("deploymentId")) {
            historicDecisionExecutionQuery.deploymentId(allRequestParams.get("deploymentId"));
        }
        if (allRequestParams.containsKey("decisionKey")) {
            historicDecisionExecutionQuery.decisionKey(allRequestParams.get("decisionKey"));
        }
        if (allRequestParams.containsKey("activityId")) {
            historicDecisionExecutionQuery.activityId(allRequestParams.get("activityId"));
        }
        if (allRequestParams.containsKey("executionId")) {
            historicDecisionExecutionQuery.executionId(allRequestParams.get("executionId"));
        }
        if (allRequestParams.containsKey("instanceId")) {
            historicDecisionExecutionQuery.instanceId(allRequestParams.get("instanceId"));
        }
        if (allRequestParams.containsKey("scopeType")) {
            historicDecisionExecutionQuery.scopeType(allRequestParams.get("scopeType"));
        }
        if (allRequestParams.containsKey("failed")) {
            historicDecisionExecutionQuery.failed(new Boolean(allRequestParams.get("failed")));
        }
        if (allRequestParams.containsKey("tenantId")) {
            historicDecisionExecutionQuery.tenantId(allRequestParams.get("tenantId"));
        }
        if (allRequestParams.containsKey("tenantIdLike")) {
            historicDecisionExecutionQuery.tenantIdLike(allRequestParams.get("tenantIdLike"));
        }

        return new HistoricDecisionExecutionsDmnPaginateList(dmnRestResponseFactory).paginateList(allRequestParams, historicDecisionExecutionQuery, "startTime", properties);
    }
}
