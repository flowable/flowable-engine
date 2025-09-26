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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.rest.service.api.BulkDeleteInstancesRestActionRequest;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.common.rest.api.RequestUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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
@Api(tags = { "History Case" }, authorizations = { @Authorization(value = "basicAuth") })
public class HistoricCaseInstanceCollectionResource extends HistoricCaseInstanceBaseResource {

    @ApiOperation(value = "List of historic case instances", tags = { "History Case" }, nickname = "listHistoricCaseInstances")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "caseInstanceId", dataType = "string", value = "An id of the historic case instance.", paramType = "query"),
            @ApiImplicitParam(name = "caseInstanceIds", dataType = "string", value = "Only return historic case instances with the given comma-separated ids.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionKey", dataType = "string", value = "The case definition key of the historic case instance.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionKeyLike", dataType = "string", value = "Only return historic case instances like the given case definition key.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionKeyLikeIgnoreCase", dataType = "string", value = "Only return historic case instances like the given case definition key, ignoring case.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionId", dataType = "string", value = "The case definition id of the historic case instance.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionCategory", dataType = "string", value = "Only return historic case instances with the given case definition category.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionCategoryLike", dataType = "string", value = "Only return historic case instances like the given case definition category.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionCategoryLikeIgnoreCase", dataType = "string", value = "Only return historic case instances like the given case definition category, ignoring case.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionName", dataType = "string", value = "Only return historic case instances with the given case definition name.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionNameLike", dataType = "string", value = "Only return historic case instances like the given case definition name.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionNameLikeIgnoreCase", dataType = "string", value = "Only return historic case instances like the given case definition name, ignoring case.", paramType = "query"),
            @ApiImplicitParam(name = "name", dataType = "string", value = "Only return historic case instances with the given name.", paramType = "query"),
            @ApiImplicitParam(name = "nameLike", dataType = "string", value = "Only return historic case instances like the given name.", paramType = "query"),
            @ApiImplicitParam(name = "nameLikeIgnoreCase", dataType = "string", value = "Only return historic case instances like the given name ignoring case.", paramType = "query"),
            @ApiImplicitParam(name = "rootScopeId", dataType = "string", value = "Only return case instances which have the given root scope id (that can be a process or case instance ID).", paramType = "query"),
            @ApiImplicitParam(name = "parentScopeId", dataType = "string", value = "Only return case instances which have the given parent scope id (that can be a process or case instance ID).", paramType = "query"),
            @ApiImplicitParam(name = "businessKey", dataType = "string", value = "The business key of the historic case instance.", paramType = "query"),
            @ApiImplicitParam(name = "businessKeyLike", dataType = "string", value = "Only return historic case instances like the given business key.", paramType = "query"),
            @ApiImplicitParam(name = "businessKeyLikeIgnoreCase", dataType = "string", value = "Only return historic case instances like the given business key, ignoring case.", paramType = "query"),
            @ApiImplicitParam(name = "businessStatus", dataType = "string", value = "The business status of the historic case instance.", paramType = "query"),
            @ApiImplicitParam(name = "businessStatusLike", dataType = "string", value = "Only return historic case instances like the given business status.", paramType = "query"),
            @ApiImplicitParam(name = "businessStatusLikeIgnoreCase", dataType = "string", value = "Only return historic case instances like the given business status, ignoring case.", paramType = "query"),
            @ApiImplicitParam(name = "involvedUser", dataType = "string", value = "An involved user of the historic case instance.", paramType = "query"),
            @ApiImplicitParam(name = "finished", dataType = "boolean", value = "Indication if the historic case instance is finished.", paramType = "query"),
            @ApiImplicitParam(name = "finishedAfter", dataType = "string", format="date-time",  value = "Return only historic case instances that were finished after this date.", paramType = "query"),
            @ApiImplicitParam(name = "finishedBefore", dataType = "string", format="date-time", value = "Return only historic case instances that were finished before this date.", paramType = "query"),
            @ApiImplicitParam(name = "startedAfter", dataType = "string", format="date-time", value = "Return only historic case instances that were started after this date.", paramType = "query"),
            @ApiImplicitParam(name = "startedBefore", dataType = "string", format="date-time", value = "Return only historic case instances that were started before this date.", paramType = "query"),
            @ApiImplicitParam(name = "startedBy", dataType = "string", value = "Return only historic case instances that were started by this user.", paramType = "query"),
            @ApiImplicitParam(name = "state", dataType = "string", value = "Only return historic case instances with the given state.", paramType = "query"),
            @ApiImplicitParam(name = "callbackId", dataType = "string", value = "Only return historic case instances which have the given callback id.", paramType = "query"),
            @ApiImplicitParam(name = "callbackType", dataType = "string", value = "Only return historic case instances which have the given callback type.", paramType = "query"),
            @ApiImplicitParam(name = "parentCaseInstanceId", dataType = "string", value = "Only return historic case instances which have the given parent case instance id.", paramType = "query"),
            @ApiImplicitParam(name = "referenceId", dataType = "string", value = "Only return historic case instances which have the given reference id.", paramType = "query"),
            @ApiImplicitParam(name = "referenceType", dataType = "string", value = "Only return historic case instances which have the given reference type.", paramType = "query"),
            @ApiImplicitParam(name = "lastReactivatedBy", dataType = "string", value = "Only return historic case instances last reactived by the given user.", paramType = "query"),
            @ApiImplicitParam(name = "lastReactivatedBefore", dataType = "string", format = "date-time", value = "Only return historic case instances last reactivated before the given date.", paramType = "query"),
            @ApiImplicitParam(name = "lastReactivatedAfter", dataType = "string", format = "date-time", value = "Only return historic case instances last reactivated after the given date.", paramType = "query"),
            @ApiImplicitParam(name = "activePlanItemDefinitionId", dataType = "string", value = "Only return historic case instances that have an active plan item instance with the given plan item definition id.", paramType = "query"),
            @ApiImplicitParam(name = "includeCaseVariables", dataType = "boolean", value = "An indication if the historic case instance variables should be returned as well.", paramType = "query"),
            @ApiImplicitParam(name = "includeCaseVariablesName", dataType = "string", value = "Indication to include case variables with the given names in the result.", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return instances with the given tenant id.", paramType = "query"),
            @ApiImplicitParam(name = "tenantIdLike", dataType = "string", value = "Only return instances like the given tenant id.", paramType = "query"),
            @ApiImplicitParam(name = "tenantIdLikeIgnoreCase", dataType = "string", value = "Only return instances like the given tenant id, ignoring case.", paramType = "query"),
            @ApiImplicitParam(name = "withoutTenantId", dataType = "boolean", value = "If true, only returns instances without a tenantId set. If false, the withoutTenantId parameter is ignored.\n", paramType = "query"),
            @ApiImplicitParam(name = "withoutCaseInstanceParentId", dataType = "boolean", value = "If true, only returns instances without a parent set. If false, the withoutCaseInstanceParentId parameter is ignored.\n", paramType = "query"),
            @ApiImplicitParam(name = "withoutCaseInstanceCallbackId", dataType = "boolean", value = "If true, only returns instances without a callbackId set. If false, the withoutCaseInstanceCallbackId parameter is ignored.\n", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates that historic case instances could be queried."),
            @ApiResponse(code = 400, message = "Indicates an parameter was passed in the wrong format. The status-message contains additional information.") })
    @GetMapping(value = "/cmmn-history/historic-case-instances", produces = "application/json")
    public DataResponse<HistoricCaseInstanceResponse> getHistoricCasenstances(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams) {
        // Populate query based on request
        HistoricCaseInstanceQueryRequest queryRequest = new HistoricCaseInstanceQueryRequest();

        if (allRequestParams.get("caseInstanceId") != null) {
            queryRequest.setCaseInstanceId(allRequestParams.get("caseInstanceId"));
        }

        if (allRequestParams.get("caseInstanceIds") != null) {
            queryRequest.setCaseInstanceIds(RequestUtil.parseToSet(allRequestParams.get("caseInstanceIds")));
        }

        if (allRequestParams.get("caseDefinitionKey") != null) {
            queryRequest.setCaseDefinitionKey(allRequestParams.get("caseDefinitionKey"));
        }
        
        if (allRequestParams.get("caseDefinitionKeyLike") != null) {
            queryRequest.setCaseDefinitionKeyLike(allRequestParams.get("caseDefinitionKeyLike"));
        }
        
        if (allRequestParams.get("caseDefinitionKeyLikeIgnoreCase") != null) {
            queryRequest.setCaseDefinitionKeyLikeIgnoreCase(allRequestParams.get("caseDefinitionKeyLikeIgnoreCase"));
        }

        if (allRequestParams.get("caseDefinitionId") != null) {
            queryRequest.setCaseDefinitionId(allRequestParams.get("caseDefinitionId"));
        }
        
        if (allRequestParams.get("caseDefinitionCategory") != null) {
            queryRequest.setCaseDefinitionCategory(allRequestParams.get("caseDefinitionCategory"));
        }
        
        if (allRequestParams.get("caseDefinitionCategoryLike") != null) {
            queryRequest.setCaseDefinitionCategoryLike(allRequestParams.get("caseDefinitionCategoryLike"));
        }
        
        if (allRequestParams.get("caseDefinitionCategoryLikeIgnoreCase") != null) {
            queryRequest.setCaseDefinitionCategoryLikeIgnoreCase(allRequestParams.get("caseDefinitionCategoryLikeIgnoreCase"));
        }
        
        if (allRequestParams.get("caseDefinitionName") != null) {
            queryRequest.setCaseDefinitionName(allRequestParams.get("caseDefinitionName"));
        }
        
        if (allRequestParams.get("caseDefinitionNameLike") != null) {
            queryRequest.setCaseDefinitionNameLike(allRequestParams.get("caseDefinitionNameLike"));
        }
        
        if (allRequestParams.get("caseDefinitionNameLikeIgnoreCase") != null) {
            queryRequest.setCaseDefinitionNameLikeIgnoreCase(allRequestParams.get("caseDefinitionNameLikeIgnoreCase"));
        }
        
        if (allRequestParams.get("name") != null) {
            queryRequest.setCaseInstanceName(allRequestParams.get("name"));
        }
        
        if (allRequestParams.get("nameLike") != null) {
            queryRequest.setCaseInstanceNameLike(allRequestParams.get("nameLike"));
        }
        
        if (allRequestParams.get("nameLikeIgnoreCase") != null) {
            queryRequest.setCaseInstanceNameLikeIgnoreCase(allRequestParams.get("nameLikeIgnoreCase"));
        }

        if (allRequestParams.get("businessKey") != null) {
            queryRequest.setCaseInstanceBusinessKey(allRequestParams.get("businessKey"));
        }
        
        if (allRequestParams.get("businessKeyLike") != null) {
            queryRequest.setCaseInstanceBusinessKeyLike(allRequestParams.get("businessKeyLike"));
        }
        
        if (allRequestParams.get("businessKeyLikeIgnoreCase") != null) {
            queryRequest.setCaseInstanceBusinessKeyLikeIgnoreCase(allRequestParams.get("businessKeyLikeIgnoreCase"));
        }
        
        if (allRequestParams.get("businessStatus") != null) {
            queryRequest.setCaseInstanceBusinessStatus(allRequestParams.get("businessStatus"));
        }
        
        if (allRequestParams.get("businessStatusLike") != null) {
            queryRequest.setCaseInstanceBusinessStatusLike(allRequestParams.get("businessStatusLike"));
        }
        
        if (allRequestParams.get("businessStatusLikeIgnoreCase") != null) {
            queryRequest.setCaseInstanceBusinessStatusLikeIgnoreCase(allRequestParams.get("businessStatusLikeIgnoreCase"));
        }

        if (allRequestParams.containsKey("rootScopeId")) {
            queryRequest.setCaseInstanceRootScopeId(allRequestParams.get("rootScopeId"));
        }

        if (allRequestParams.containsKey("parentScopeId")) {
            queryRequest.setCaseInstanceParentScopeId(allRequestParams.get("parentScopeId"));
        }

        if (allRequestParams.get("involvedUser") != null) {
            queryRequest.setInvolvedUser(allRequestParams.get("involvedUser"));
        }
        
        if (allRequestParams.get("state") != null) {
            queryRequest.setCaseInstanceState(allRequestParams.get("state"));
        }
        
        if (allRequestParams.get("callbackId") != null) {
            queryRequest.setCaseInstanceCallbackId(allRequestParams.get("callbackId"));
        }

        if (allRequestParams.containsKey("callbackIds")) {
            queryRequest.setCaseInstanceCallbackIds(RequestUtil.parseToSet(allRequestParams.get("callbackIds")));
        }

        if (allRequestParams.get("callbackType") != null) {
            queryRequest.setCaseInstanceCallbackType(allRequestParams.get("callbackType"));
        }
        
        if (allRequestParams.containsKey("parentCaseInstanceId")) {
            queryRequest.setParentCaseInstanceId(allRequestParams.get("parentCaseInstanceId"));
        }

        if (allRequestParams.get("referenceId") != null) {
            queryRequest.setCaseInstanceReferenceId(allRequestParams.get("referenceId"));
        }
        
        if (allRequestParams.get("referenceType") != null) {
            queryRequest.setCaseInstanceReferenceType(allRequestParams.get("referenceType"));
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
        
        if (allRequestParams.containsKey("activePlanItemDefinitionId")) {
            queryRequest.setActivePlanItemDefinitionId(allRequestParams.get("activePlanItemDefinitionId"));
        }

        if (allRequestParams.get("startedBy") != null) {
            queryRequest.setStartedBy(allRequestParams.get("startedBy"));
        }
        
        if (allRequestParams.get("lastReactivatedAfter") != null) {
            queryRequest.setLastReactivatedAfter(RequestUtil.getDate(allRequestParams, "lastReactivatedAfter"));
        }

        if (allRequestParams.get("lastReactivatedBefore") != null) {
            queryRequest.setLastReactivatedBefore(RequestUtil.getDate(allRequestParams, "lastReactivatedBefore"));
        }

        if (allRequestParams.get("lastReactivatedBy") != null) {
            queryRequest.setLastReactivatedBy(allRequestParams.get("lastReactivatedBy"));
        }

        if (allRequestParams.get("includeCaseVariables") != null) {
            queryRequest.setIncludeCaseVariables(Boolean.valueOf(allRequestParams.get("includeCaseVariables")));
        }

        if (allRequestParams.containsKey("includeCaseVariablesNames")) {
            queryRequest.setIncludeCaseVariablesNames(RequestUtil.parseToList(allRequestParams.get("includeCaseVariablesNames")));
        }

        if (allRequestParams.get("tenantId") != null) {
            queryRequest.setTenantId(allRequestParams.get("tenantId"));
        }
        
        if (allRequestParams.get("tenantIdLike") != null) {
            queryRequest.setTenantIdLike(allRequestParams.get("tenantIdLike"));
        }
        
        if (allRequestParams.get("tenantIdLikeIgnoreCase") != null) {
            queryRequest.setTenantIdLikeIgnoreCase(allRequestParams.get("tenantIdLikeIgnoreCase"));
        }

        if (allRequestParams.get("withoutTenantId") != null) {
            queryRequest.setWithoutTenantId(Boolean.valueOf(allRequestParams.get("withoutTenantId")));
        }
        if (allRequestParams.get("withoutCaseInstanceParentId") != null) {
            queryRequest.setWithoutCaseInstanceParentId(Boolean.valueOf(allRequestParams.get("withoutCaseInstanceParentId")));
        }
        if (allRequestParams.get("withoutCaseInstanceCallbackId") != null) {
            queryRequest.setWithoutCaseInstanceCallbackId(Boolean.valueOf(allRequestParams.get("withoutCaseInstanceCallbackId")));
        }
        return getQueryResponse(queryRequest, allRequestParams);
    }

    @ApiOperation(value = "Post action request to delete a bulk of historic case instances", tags = {
            "Manage History Case Instances" }, nickname = "bulkDeleteHistoricCaseInstances",
            code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the bulk of historic case instances was found and deleted. Response body is left empty intentionally."),
            @ApiResponse(code = 404, message = "Indicates at least one requested case instance was not found.")
    })
    @PostMapping(value = "/cmmn-history/historic-case-instances/delete")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void bulkDeleteHistoricCaseInstances(@ApiParam(name = "bulkDeleteRestActionRequest")
    @RequestBody BulkDeleteInstancesRestActionRequest request) {
        if (BulkDeleteInstancesRestActionRequest.DELETE_ACTION.equals(request.getAction())) {
            if (restApiInterceptor != null) {
                restApiInterceptor.bulkDeleteHistoricCases(request.getInstanceIds());
            }
            historyService.bulkDeleteHistoricCaseInstances(request.getInstanceIds());
        } else {
            throw new FlowableIllegalArgumentException("Illegal action: '" + request.getAction() + "'.");
        }
    }
}
