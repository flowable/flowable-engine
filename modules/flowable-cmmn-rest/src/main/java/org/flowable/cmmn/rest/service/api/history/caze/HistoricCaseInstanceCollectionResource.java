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

package org.flowable.cmmn.rest.service.api.history.caze;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.flowable.common.rest.api.DataResponse;
import org.flowable.common.rest.api.RequestUtil;
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
@Api(tags = { "History Case" }, description = "Manage History Case Instances", authorizations = { @Authorization(value = "basicAuth") })
public class HistoricCaseInstanceCollectionResource extends HistoricCaseInstanceBaseResource {

    @ApiOperation(value = "List of historic case instances", tags = { "History Case" }, nickname = "listHistoricCaseInstances")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "caseInstanceId", dataType = "string", value = "An id of the historic case instance.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionKey", dataType = "string", value = "The process definition key of the historic case instance.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionId", dataType = "string", value = "The process definition id of the historic case instance.", paramType = "query"),
            @ApiImplicitParam(name = "businessKey", dataType = "string", value = "The business key of the historic case instance.", paramType = "query"),
            @ApiImplicitParam(name = "involvedUser", dataType = "string", value = "An involved user of the historic case instance.", paramType = "query"),
            @ApiImplicitParam(name = "finished", dataType = "boolean", value = "Indication if the historic case instance is finished.", paramType = "query"),
            @ApiImplicitParam(name = "finishedAfter", dataType = "string", format="date-time",  value = "Return only historic case instances that were finished after this date.", paramType = "query"),
            @ApiImplicitParam(name = "finishedBefore", dataType = "string", format="date-time", value = "Return only historic case instances that were finished before this date.", paramType = "query"),
            @ApiImplicitParam(name = "startedAfter", dataType = "string", format="date-time", value = "Return only historic case instances that were started after this date.", paramType = "query"),
            @ApiImplicitParam(name = "startedBefore", dataType = "string", format="date-time", value = "Return only historic case instances that were started before this date.", paramType = "query"),
            @ApiImplicitParam(name = "startedBy", dataType = "string", value = "Return only historic case instances that were started by this user.", paramType = "query"),
            @ApiImplicitParam(name = "includeCaseVariables", dataType = "boolean", value = "An indication if the historic case instance variables should be returned as well.", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return instances with the given tenantId.", paramType = "query"),
            @ApiImplicitParam(name = "withoutTenantId", dataType = "boolean", value = "If true, only returns instances without a tenantId set. If false, the withoutTenantId parameter is ignored.\n", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates that historic case instances could be queried."),
            @ApiResponse(code = 400, message = "Indicates an parameter was passed in the wrong format. The status-message contains additional information.") })
    @GetMapping(value = "/cmmn-history/historic-case-instances", produces = "application/json")
    public DataResponse<HistoricCaseInstanceResponse> getHistoricCasenstances(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        // Populate query based on request
        HistoricCaseInstanceQueryRequest queryRequest = new HistoricCaseInstanceQueryRequest();

        if (allRequestParams.get("caseInstanceId") != null) {
            queryRequest.setCaseInstanceId(allRequestParams.get("caseInstanceId"));
        }

        if (allRequestParams.get("caseDefinitionKey") != null) {
            queryRequest.setCaseDefinitionKey(allRequestParams.get("caseDefinitionKey"));
        }

        if (allRequestParams.get("caseDefinitionId") != null) {
            queryRequest.setCaseDefinitionId(allRequestParams.get("caseDefinitionId"));
        }

        if (allRequestParams.get("businessKey") != null) {
            queryRequest.setCaseBusinessKey(allRequestParams.get("businessKey"));
        }

        if (allRequestParams.get("involvedUser") != null) {
            queryRequest.setInvolvedUser(allRequestParams.get("involvedUser"));
        }

        if (allRequestParams.get("finished") != null) {
            queryRequest.setFinished(Boolean.valueOf(allRequestParams.get("finished")));
        }

        if (allRequestParams.get("finishedAfter") != null) {
            queryRequest.setFinishedAfter(RequestUtil.getDate(allRequestParams, "finishedAfter"));
        }

        if (allRequestParams.get("finishedBefore") != null) {
            queryRequest.setFinishedBefore(RequestUtil.getDate(allRequestParams, "finishedBefore"));
        }

        if (allRequestParams.get("startedAfter") != null) {
            queryRequest.setStartedAfter(RequestUtil.getDate(allRequestParams, "startedAfter"));
        }

        if (allRequestParams.get("startedBefore") != null) {
            queryRequest.setStartedBefore(RequestUtil.getDate(allRequestParams, "startedBefore"));
        }

        if (allRequestParams.get("startedBy") != null) {
            queryRequest.setStartedBy(allRequestParams.get("startedBy"));
        }

        if (allRequestParams.get("includeCaseVariables") != null) {
            queryRequest.setIncludeCaseVariables(Boolean.valueOf(allRequestParams.get("includeCaseVariables")));
        }

        if (allRequestParams.get("tenantId") != null) {
            queryRequest.setTenantId(allRequestParams.get("tenantId"));
        }

        if (allRequestParams.get("withoutTenantId") != null) {
            queryRequest.setWithoutTenantId(Boolean.valueOf(allRequestParams.get("withoutTenantId")));
        }

        return getQueryResponse(queryRequest, allRequestParams);
    }
}
