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

import java.util.Arrays;
import java.util.Map;

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
@Api(tags = { "Plan Item Instances" }, authorizations = { @Authorization(value = "basicAuth") })
public class PlanItemInstanceCollectionResource extends PlanItemInstanceBaseResource {

    // FIXME naming issue ?
    @ApiOperation(value = "List of plan item instances", tags = { "Plan Item Instances" }, nickname = "listPlanItemInstances")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "string", value = "Only return models with the given version.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionId", dataType = "string", value = "Only return plan item instances with the given case definition id.", paramType = "query"),
            @ApiImplicitParam(name = "caseInstanceId", dataType = "string", value = "Only return plan item instances which are part of the case instance with the given id.", paramType = "query"),
            @ApiImplicitParam(name = "caseInstanceIds", dataType = "string", value = "Only return plan item instances which are part of the case instance with the given ids.", paramType = "query"),
            @ApiImplicitParam(name = "stageInstanceId", dataType = "string", value = "Only return plan item instances which are part of the given stage instance.", paramType = "query"),
            @ApiImplicitParam(name = "planItemDefinitionId", dataType = "string", value = "Only return plan item instances which have the given plan item definition id.", paramType = "query"),
            @ApiImplicitParam(name = "planItemDefinitionType", dataType = "string", value = "Only return plan item instances which have the given plan item definition type.", paramType = "query"),
            @ApiImplicitParam(name = "planItemDefinitionTypes", dataType = "string", value = "Only return plan item instances which have any of the given plan item definition types. Comma-separated string e.g. humantask, stage", paramType = "query"),
            @ApiImplicitParam(name = "state", dataType = "string", value = "Only return plan item instances which have the given state.", paramType = "query"),
            @ApiImplicitParam(name = "name", dataType = "string", value = "Only return plan item instances which have the given name.", paramType = "query"),
            @ApiImplicitParam(name = "elementId", dataType = "string", value = "Only return plan item instances which have the given element id.", paramType = "query"),
            @ApiImplicitParam(name = "referenceId", dataType = "string", value = "Only return plan item instances which have the given reference id.", paramType = "query"),
            @ApiImplicitParam(name = "referenceType", dataType = "string", value = "Only return plan item instances which have the given reference type.", paramType = "query"),
            @ApiImplicitParam(name = "createdBefore", dataType = "date", value = "Only return plan item instances which are created before the given date.", paramType = "query"),
            @ApiImplicitParam(name = "createdAfter", dataType = "date", value = "Only return plan item instances which are created after the given date.", paramType = "query"),
            @ApiImplicitParam(name = "startUserId", dataType = "string", value = "Only return plan item instances which are started by the given user id.", paramType = "query"),
            @ApiImplicitParam(name = "includeEnded", dataType = "boolean", value = "Define if ended plan item instances should be included.", paramType = "query"),
            @ApiImplicitParam(name = "includeLocalVariables", dataType = "boolean", value = "Indication to include local variables in the result.", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return plan item instances with the given tenantId.", paramType = "query"),
            @ApiImplicitParam(name = "withoutTenantId", dataType = "boolean", value = "If true, only returns plan item instances without a tenantId set. If false, the withoutTenantId parameter is ignored.", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues = "name, createTime, startTime", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the executions are returned"),
            @ApiResponse(code = 400, message = "Indicates a parameter was passed in the wrong format . The status-message contains additional information.")
    })
    @GetMapping(value = "/cmmn-runtime/plan-item-instances", produces = "application/json")
    public DataResponse<PlanItemInstanceResponse> getPlanItemInstances(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams) {
        // Populate query based on request
        PlanItemInstanceQueryRequest queryRequest = new PlanItemInstanceQueryRequest();

        if (allRequestParams.containsKey("id")) {
            queryRequest.setId(allRequestParams.get("id"));
        }

        if (allRequestParams.containsKey("caseInstanceId")) {
            queryRequest.setCaseInstanceId(allRequestParams.get("caseInstanceId"));
        }

        if (allRequestParams.containsKey("caseInstanceIds")) {
            queryRequest.setCaseInstanceIds(RequestUtil.parseToSet(allRequestParams.get("caseInstanceIds")));
        }

        if (allRequestParams.containsKey("caseDefinitionId")) {
            queryRequest.setCaseDefinitionId(allRequestParams.get("caseDefinitionId"));
        }

        if (allRequestParams.containsKey("stageInstanceId")) {
            queryRequest.setStageInstanceId(allRequestParams.get("stageInstanceId"));
        }
        
        if (allRequestParams.containsKey("planItemDefinitionId")) {
            queryRequest.setPlanItemDefinitionId(allRequestParams.get("planItemDefinitionId"));
        }
        
        if (allRequestParams.containsKey("planItemDefinitionType")) {
            queryRequest.setPlanItemDefinitionType(allRequestParams.get("planItemDefinitionType"));
        }

        if (allRequestParams.containsKey("planItemDefinitionTypes")) {
            String typesString = allRequestParams.get("planItemDefinitionTypes");
            queryRequest.setPlanItemDefinitionTypes(Arrays.asList(typesString.split(",")));
        }
        
        if (allRequestParams.containsKey("state")) {
            queryRequest.setState(allRequestParams.get("state"));
        }
        
        if (allRequestParams.containsKey("elementId")) {
            queryRequest.setElementId(allRequestParams.get("elementId"));
        }
        
        if (allRequestParams.containsKey("referenceId")) {
            queryRequest.setReferenceId(allRequestParams.get("referenceId"));
        }
        
        if (allRequestParams.containsKey("referenceType")) {
            queryRequest.setReferenceType(allRequestParams.get("referenceType"));
        }
        
        if (allRequestParams.containsKey("createdBefore")) {
            queryRequest.setCreatedBefore(RequestUtil.getDate(allRequestParams, "createdBefore"));
        }
        
        if (allRequestParams.containsKey("createdAfter")) {
            queryRequest.setCreatedAfter(RequestUtil.getDate(allRequestParams, "createdAfter"));
        }
        
        if (allRequestParams.containsKey("startUserId")) {
            queryRequest.setStartUserId(allRequestParams.get("startUserId"));
        }
        
        if (allRequestParams.containsKey("includeEnded")) {
            queryRequest.setIncludeEnded(RequestUtil.getBoolean(allRequestParams, "includeEnded", false));
        }

        if (allRequestParams.containsKey("includeLocalVariables")) {
            queryRequest.setIncludeLocalVariables(RequestUtil.getBoolean(allRequestParams, "includeLocalVariables", false));
        }
        
        if (allRequestParams.containsKey("name")) {
            queryRequest.setName(allRequestParams.get("name"));
        }
        
        if (allRequestParams.containsKey("tenantId")) {
            queryRequest.setTenantId(allRequestParams.get("tenantId"));
        }

        if (allRequestParams.containsKey("withoutTenantId")) {
            if (Boolean.valueOf(allRequestParams.get("withoutTenantId"))) {
                queryRequest.setWithoutTenantId(Boolean.TRUE);
            }
        }

        return getQueryResponse(queryRequest, allRequestParams);
    }
}
