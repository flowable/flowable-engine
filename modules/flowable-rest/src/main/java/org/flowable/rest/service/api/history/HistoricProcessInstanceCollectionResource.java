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

package org.flowable.rest.service.api.history;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.common.rest.api.RequestUtil;
import org.flowable.rest.service.api.BulkDeleteInstancesRestActionRequest;
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
@Api(tags = { "History Process" }, authorizations = { @Authorization(value = "basicAuth") })
public class HistoricProcessInstanceCollectionResource extends HistoricProcessInstanceBaseResource {

    @ApiOperation(value = "List of historic process instances", tags = { "History Process" }, nickname = "listHistoricProcessInstances")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "processInstanceId", dataType = "string", value = "An id of the historic process instance.", paramType = "query"),
        @ApiImplicitParam(name = "processInstanceName", dataType = "string", value = "A name of the historic process instance.", paramType = "query"),
        @ApiImplicitParam(name = "processInstanceNameLike", dataType = "string", value = "A name of the historic process instance used in a like query.", paramType = "query"),
        @ApiImplicitParam(name = "processInstanceNameLikeIgnoreCase", dataType = "string", value = "A name of the historic process instance used in a like query ignoring case.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionKey", dataType = "string", value = "The process definition key of the historic process instance.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionKeyLike", dataType = "string", value = "The process definition key used in a like query.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionKeyLikeIgnoreCase", dataType = "string", value = "The process definition key used in a like query ignoring case.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionId", dataType = "string", value = "The process definition id of the historic process instance.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionName", dataType = "string", value = "The process definition name of the historic process instance.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionNameLike", dataType = "string", value = "The process definition name used in a like query.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionNameLikeIgnoreCase", dataType = "string", value = "The process definition name used in a like query ignoring case.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionCategory", dataType = "string", value = "The process definition category of the historic process instance.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionCategoryLike", dataType = "string", value = "The process definition category used in a like query.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionCategoryLikeIgnoreCase", dataType = "string", value = "The process definition category used in a like query ignoring case.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionVersion", dataType = "string", value = "The process definition version of the historic process instance.", paramType = "query"),
        @ApiImplicitParam(name = "deploymentId", dataType = "string", value = "The deployment id of the historic process instance.", paramType = "query"),
        @ApiImplicitParam(name = "rootScopeId", dataType = "string", value = "Only return process instances which have the given root scope id (that can be a process or case instance ID).", paramType = "query"),
        @ApiImplicitParam(name = "parentScopeId", dataType = "string", value = "Only return process instances which have the given parent scope id (that can be a process or case instance ID).", paramType = "query"),
        @ApiImplicitParam(name = "businessKey", dataType = "string", value = "The business key of the historic process instance.", paramType = "query"),
        @ApiImplicitParam(name = "businessKeyLike", dataType = "string", value = "Only return instances with a business key like this key.", paramType = "query"),
        @ApiImplicitParam(name = "businessKeyLikeIgnoreCase", dataType = "string", value = "Only return instances with a business key like this key ignoring case.", paramType = "query"),
        @ApiImplicitParam(name = "businessStatus", dataType = "string", value = "The business status of the historic process instance.", paramType = "query"),
        @ApiImplicitParam(name = "businessStatusLike", dataType = "string", value = "Only return instances with a business status like this status.", paramType = "query"),
        @ApiImplicitParam(name = "businessStatusLikeIgnoreCase", dataType = "string", value = "Only return instances with a business status like this status ignoring case.", paramType = "query"),
        @ApiImplicitParam(name = "activeActivityId", dataType = "string", value = "Only return instances which have an active activity instance with the provided activity id.", paramType = "query"),
        @ApiImplicitParam(name = "involvedUser", dataType = "string", value = "An involved user of the historic process instance.", paramType = "query"),
        @ApiImplicitParam(name = "finished", dataType = "boolean", value = "Indication if the historic process instance is finished.", paramType = "query"),
        @ApiImplicitParam(name = "superProcessInstanceId", dataType = "string", value = "An optional parent process id of the historic process instance.", paramType = "query"),
        @ApiImplicitParam(name = "excludeSubprocesses", dataType = "boolean", value = "Return only historic process instances which are not sub processes.", paramType = "query"),
        @ApiImplicitParam(name = "finishedAfter", dataType = "string", format="date-time",  value = "Return only historic process instances that were finished after this date.", paramType = "query"),
        @ApiImplicitParam(name = "finishedBefore", dataType = "string", format="date-time", value = "Return only historic process instances that were finished before this date.", paramType = "query"),
        @ApiImplicitParam(name = "startedAfter", dataType = "string", format="date-time", value = "Return only historic process instances that were started after this date.", paramType = "query"),
        @ApiImplicitParam(name = "startedBefore", dataType = "string", format="date-time", value = "Return only historic process instances that were started before this date.", paramType = "query"),
        @ApiImplicitParam(name = "startedBy", dataType = "string", value = "Return only historic process instances that were started by this user.", paramType = "query"),
        @ApiImplicitParam(name = "includeProcessVariables", dataType = "boolean", value = "An indication if the historic process instance variables should be returned as well.", paramType = "query"),
        @ApiImplicitParam(name = "includeProcessVariablesName", dataType = "string", value = "Indication to include process variables with the given names in the result.", paramType = "query"),
        @ApiImplicitParam(name = "callbackId", dataType = "string", value = "Only return instances with the given callbackId.", paramType = "query"),
        @ApiImplicitParam(name = "callbackIds", dataType = "string", value = "Only return instances with the given callbackIds.", paramType = "query"),
        @ApiImplicitParam(name = "callbackType", dataType = "string", value = "Only return instances with the given callbackType.", paramType = "query"),
        @ApiImplicitParam(name = "parentCaseInstanceId", dataType = "string", value = "Only return instances with the given parent case instance id.", paramType = "query"),
        @ApiImplicitParam(name = "withoutCallbackId", dataType = "boolean", value = "Only return instances that do not have a callbackId.", paramType = "query"),
        @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return instances with the given tenantId.", paramType = "query"),
        @ApiImplicitParam(name = "tenantIdLike", dataType = "string", value = "Only return instances with a tenant id like the given value.", paramType = "query"),
        @ApiImplicitParam(name = "tenantIdLikeIgnoreCase", dataType = "string", value = "Only return instances with a tenant id like the given value ignoring case.", paramType = "query"),
        @ApiImplicitParam(name = "withoutTenantId", dataType = "boolean", value = "If true, only returns instances without a tenantId set. If false, the withoutTenantId parameter is ignored.\n", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates that historic process instances could be queried."),
            @ApiResponse(code = 400, message = "Indicates an parameter was passed in the wrong format. The status-message contains additional information.") })
    @GetMapping(value = "/history/historic-process-instances", produces = "application/json")
    public DataResponse<HistoricProcessInstanceResponse> getHistoricProcessInstances(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams) {
        // Populate query based on request
        HistoricProcessInstanceQueryRequest queryRequest = new HistoricProcessInstanceQueryRequest();

        if (allRequestParams.get("processInstanceId") != null) {
            queryRequest.setProcessInstanceId(allRequestParams.get("processInstanceId"));
        }
        
        if (allRequestParams.get("processInstanceName") != null) {
            queryRequest.setProcessInstanceName(allRequestParams.get("processInstanceName"));
        }
        
        if (allRequestParams.get("processInstanceNameLike") != null) {
            queryRequest.setProcessInstanceNameLike(allRequestParams.get("processInstanceNameLike"));
        }
        
        if (allRequestParams.get("processInstanceNameLikeIgnoreCase") != null) {
            queryRequest.setProcessInstanceNameLikeIgnoreCase(allRequestParams.get("processInstanceNameLikeIgnoreCase"));
        }

        if (allRequestParams.get("processDefinitionKey") != null) {
            queryRequest.setProcessDefinitionKey(allRequestParams.get("processDefinitionKey"));
        }
        
        if (allRequestParams.get("processDefinitionKeyLike") != null) {
            queryRequest.setProcessDefinitionKeyLike(allRequestParams.get("processDefinitionKeyLike"));
        }
        
        if (allRequestParams.get("processDefinitionKeyLikeIgnoreCase") != null) {
            queryRequest.setProcessDefinitionKeyLikeIgnoreCase(allRequestParams.get("processDefinitionKeyLikeIgnoreCase"));
        }

        if (allRequestParams.get("processDefinitionId") != null) {
            queryRequest.setProcessDefinitionId(allRequestParams.get("processDefinitionId"));
        }
        
        if (allRequestParams.get("processDefinitionName") != null) {
            queryRequest.setProcessDefinitionName(allRequestParams.get("processDefinitionName"));
        }
        
        if (allRequestParams.get("processDefinitionNameLike") != null) {
            queryRequest.setProcessDefinitionNameLike(allRequestParams.get("processDefinitionNameLike"));
        }
        
        if (allRequestParams.get("processDefinitionNameLikeIgnoreCase") != null) {
            queryRequest.setProcessDefinitionNameLikeIgnoreCase(allRequestParams.get("processDefinitionNameLikeIgnoreCase"));
        }
        
        if (allRequestParams.get("processDefinitionCategory") != null) {
            queryRequest.setProcessDefinitionCategory(allRequestParams.get("processDefinitionCategory"));
        }
        
        if (allRequestParams.get("processDefinitionCategoryLike") != null) {
            queryRequest.setProcessDefinitionCategoryLike(allRequestParams.get("processDefinitionCategoryLike"));
        }
        
        if (allRequestParams.get("processDefinitionCategoryLikeIgnoreCase") != null) {
            queryRequest.setProcessDefinitionCategoryLikeIgnoreCase(allRequestParams.get("processDefinitionCategoryLikeIgnoreCase"));
        }
        
        if (allRequestParams.get("processDefinitionVersion") != null) {
            queryRequest.setProcessDefinitionVersion(Integer.valueOf(allRequestParams.get("processDefinitionVersion")));
        }
        
        if (allRequestParams.containsKey("rootScopeId")) {
            queryRequest.setRootScopeId(allRequestParams.get("rootScopeId"));
        }

        if (allRequestParams.containsKey("parentScopeId")) {
            queryRequest.setParentScopeId(allRequestParams.get("parentScopeId"));
        }

        if (allRequestParams.get("deploymentId") != null) {
            queryRequest.setDeploymentId(allRequestParams.get("deploymentId"));
        }

        if (allRequestParams.get("businessKey") != null) {
            queryRequest.setProcessBusinessKey(allRequestParams.get("businessKey"));
        }
        
        if (allRequestParams.get("businessKeyLike") != null) {
            queryRequest.setProcessBusinessKeyLike(allRequestParams.get("businessKeyLike"));
        }
        
        if (allRequestParams.get("businessKeyLikeIgnoreCase") != null) {
            queryRequest.setProcessBusinessKeyLikeIgnoreCase(allRequestParams.get("businessKeyLikeIgnoreCase"));
        }
        
        if (allRequestParams.get("businessStatus") != null) {
            queryRequest.setProcessBusinessStatus(allRequestParams.get("businessStatus"));
        }
        
        if (allRequestParams.get("businessStatusLike") != null) {
            queryRequest.setProcessBusinessStatusLike(allRequestParams.get("businessStatusLike"));
        }
        
        if (allRequestParams.get("businessStatusLikeIgnoreCase") != null) {
            queryRequest.setProcessBusinessStatusLikeIgnoreCase(allRequestParams.get("businessStatusLikeIgnoreCase"));
        }
        
        if (allRequestParams.get("activeActivityId") != null) {
            queryRequest.setActiveActivityId(allRequestParams.get("activeActivityId"));
        }

        if (allRequestParams.get("involvedUser") != null) {
            queryRequest.setInvolvedUser(allRequestParams.get("involvedUser"));
        }

        if (allRequestParams.get("finished") != null) {
            queryRequest.setFinished(Boolean.valueOf(allRequestParams.get("finished")));
        }

        if (allRequestParams.get("superProcessInstanceId") != null) {
            queryRequest.setSuperProcessInstanceId(allRequestParams.get("superProcessInstanceId"));
        }

        if (allRequestParams.get("excludeSubprocesses") != null) {
            queryRequest.setExcludeSubprocesses(Boolean.valueOf(allRequestParams.get("excludeSubprocesses")));
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

        if (allRequestParams.get("includeProcessVariables") != null) {
            queryRequest.setIncludeProcessVariables(Boolean.valueOf(allRequestParams.get("includeProcessVariables")));
        }

        if (allRequestParams.get("includeProcessVariablesNames") != null) {
            queryRequest.setIncludeProcessVariablesNames(RequestUtil.parseToList(allRequestParams.get("includeProcessVariablesNames")));
        }
        
        if (allRequestParams.get("callbackId") != null) {
            queryRequest.setCallbackId(allRequestParams.get("callbackId"));
        }
        
        if (allRequestParams.get("callbackIds") != null) {
            String[] callbackIds = allRequestParams.get("callbackIds").split(",");
            queryRequest.setCallbackIds(new HashSet<>(Arrays.asList(callbackIds)));
        }

        if (allRequestParams.get("callbackType") != null) {
            queryRequest.setCallbackType(allRequestParams.get("callbackType"));
        }
        
        if (allRequestParams.get("parentCaseInstanceId") != null) {
            queryRequest.setParentCaseInstanceId(allRequestParams.get("parentCaseInstanceId"));
        }
        
        if (allRequestParams.get("withoutCallbackId") != null) {
            queryRequest.setWithoutCallbackId(Boolean.valueOf(allRequestParams.get("withoutCallbackId")));
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

        return getQueryResponse(queryRequest, allRequestParams);
    }

    @ApiOperation(value = "Post action request to delete a bulk of historic process instances", tags = {
            "Manage History Process Instances" }, nickname = "bulkDeleteHistoricProcessInstances", code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the bulk of historic process instances was found and deleted. Response body is left empty intentionally."),
            @ApiResponse(code = 404, message = "Indicates at least one requested process instance was not found.")
    })
    @PostMapping(value = "/history/historic-process-instances/delete")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void bulkDeleteHistoricProcessInstances(@ApiParam(name = "bulkDeleteRestActionRequest")
    @RequestBody BulkDeleteInstancesRestActionRequest request) {
        if (BulkDeleteInstancesRestActionRequest.DELETE_ACTION.equals(request.getAction())) {
            if (restApiInterceptor != null) {
                restApiInterceptor.bulkDeleteHistoricProcessInstances(request.getInstanceIds());
            }
            historyService.bulkDeleteHistoricProcessInstances(request.getInstanceIds());
        } else {
            throw new FlowableIllegalArgumentException("Illegal action: '" + request.getAction() + "'.");
        }
    }
}
