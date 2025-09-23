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

package org.flowable.cmmn.rest.service.api.runtime.caze;

import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.rest.service.api.BulkDeleteInstancesRestActionRequest;
import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.common.rest.api.RequestUtil;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Modified the "createCaseInstance" method to conditionally call a "createCaseInstanceResponse" method with a different signature, which will conditionally return the case variables that
 * exist when the case instance either enters its first wait state or completes. In this case, the different method is always called with a flag of true, which means that it will always return
 * those variables. If variables are not to be returned, the original method is called, which doesn't return the variables.
 * 
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Case Instances" }, authorizations = { @Authorization(value = "basicAuth") })
public class CaseInstanceCollectionResource extends BaseCaseInstanceResource {

    @Autowired
    protected CmmnHistoryService historyService;

    @ApiOperation(value = "List case instances", nickname ="listCaseInstances", tags = { "Case Instances" })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "string", value = "Only return case instances with the given id.", paramType = "query"),
            @ApiImplicitParam(name = "ids", dataType = "string", value = "Only return case instances with the given comma-separated ids.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionKey", dataType = "string", value = "Only return case instances with the given case definition key.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionKeyLike", dataType = "string", value = "Only return case instances like given case definition key.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionKeyLikeIgnoreCase", dataType = "string", value = "Only return case instances like given case definition key, ignoring case.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionId", dataType = "string", value = "Only return case instances with the given case definition id.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionCategory", dataType = "string", value = "Only return case instances with the given case definition category.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionCategoryLike", dataType = "string", value = "Only return case instances like the given case definition category.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionCategoryLikeIgnoreCase", dataType = "string", value = "Only return case instances like the given case definition category, ignoring case.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionName", dataType = "string", value = "Only return case instances with the given case definition name.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionNameLike", dataType = "string", value = "Only return case instances like the given case definition name.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionNameLikeIgnoreCase", dataType = "string", value = "Only return case instances like the given case definition name, ignoring case.", paramType = "query"),
            @ApiImplicitParam(name = "name", dataType = "string", value = "Only return case instances with the given name.", paramType = "query"),
            @ApiImplicitParam(name = "nameLike", dataType = "string", value = "Only return case instances like the given name.", paramType = "query"),
            @ApiImplicitParam(name = "nameLikeIgnoreCase", dataType = "string", value = "Only return case instances like the given name ignoring case.", paramType = "query"),
            @ApiImplicitParam(name = "rootScopeId", dataType = "string", value = "Only return case instances which have the given root scope id (that can be a process or case instance ID).", paramType = "query"),
            @ApiImplicitParam(name = "parentScopeId", dataType = "string", value = "Only return case instances which have the given parent scope id (that can be a process or case instance ID).", paramType = "query"),
            @ApiImplicitParam(name = "businessKey", dataType = "string", value = "Only return case instances with the given business key.", paramType = "query"),
            @ApiImplicitParam(name = "businessKeyLike", dataType = "string", value = "Only return case instances like the given business key.", paramType = "query"),
            @ApiImplicitParam(name = "businessKeyLikeIgnoreCase", dataType = "string", value = "Only return case instances like the given business key, ignoring case.", paramType = "query"),
            @ApiImplicitParam(name = "businessStatus", dataType = "string", value = "Only return case instances with the given business status.", paramType = "query"),
            @ApiImplicitParam(name = "businessStatusLike", dataType = "string", value = "Only return case instances like the given business status.", paramType = "query"),
            @ApiImplicitParam(name = "businessStatusLikeIgnoreCase", dataType = "string", value = "Only return case instances like the given business status, ignoring case.", paramType = "query"),
            @ApiImplicitParam(name = "caseInstanceParentId", dataType = "string", value = "Only return case instances with the given parent id.", paramType = "query"),
            @ApiImplicitParam(name = "startedBy", dataType = "string", value = "Only return case instances started by the given user.", paramType = "query"),
            @ApiImplicitParam(name = "startedBefore", dataType = "string", format = "date-time", value = "Only return case instances started before the given date.", paramType = "query"),
            @ApiImplicitParam(name = "startedAfter", dataType = "string", format = "date-time", value = "Only return case instances started after the given date.", paramType = "query"),
            @ApiImplicitParam(name = "state", dataType = "string", value = "Only return case instances with the given state.", paramType = "query"),
            @ApiImplicitParam(name = "callbackId", dataType = "string", value = "Only return case instances which have the given callback id.", paramType = "query"),
            @ApiImplicitParam(name = "callbackIds", dataType = "string", value = "Only return case instances which have the given callback ids.", paramType = "query"),
            @ApiImplicitParam(name = "callbackType", dataType = "string", value = "Only return case instances which have the given callback type.", paramType = "query"),
            @ApiImplicitParam(name = "parentCaseInstanceId", dataType = "string", value = "Only return case instances which have the given parent case instance id.", paramType = "query"),
            @ApiImplicitParam(name = "referenceId", dataType = "string", value = "Only return case instances which have the given reference id.", paramType = "query"),
            @ApiImplicitParam(name = "referenceType", dataType = "string", value = "Only return case instances which have the given reference type.", paramType = "query"),
            @ApiImplicitParam(name = "lastReactivatedBy", dataType = "string", value = "Only return case instances last reactivated by the given user.", paramType = "query"),
            @ApiImplicitParam(name = "lastReactivatedBefore", dataType = "string", format = "date-time", value = "Only return case instances last reactivated before the given date.", paramType = "query"),
            @ApiImplicitParam(name = "lastReactivatedAfter", dataType = "string", format = "date-time", value = "Only return case instances last reactivated after the given date.", paramType = "query"),
            @ApiImplicitParam(name = "includeCaseVariables", dataType = "boolean", value = "Indication to include case variables in the result.", paramType = "query"),
            @ApiImplicitParam(name = "includeCaseVariablesName", dataType = "string", value = "Indication to include case variables with the given names in the result.", paramType = "query"),
            @ApiImplicitParam(name = "activePlanItemDefinitionId", dataType = "string", value = "Only return case instances that have an active plan item instance with the given plan item definition id.", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return case instances with the given tenantId.", paramType = "query"),
            @ApiImplicitParam(name = "tenantIdLike", dataType = "string", value = "Only return case instances with a tenantId like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "tenantIdLikeIgnoreCase", dataType = "string", value = "Only return case instances with a tenantId like the given value, ignoring case.", paramType = "query"),
            @ApiImplicitParam(name = "withoutTenantId", dataType = "boolean", value = "If true, only returns case instances without a tenantId set. If false, the withoutTenantId parameter is ignored.", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues = "id,caseDefinitionId,tenantId,caseDefinitionKey", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the case instances are returned"),
            @ApiResponse(code = 400, message = "Indicates a parameter was passed in the wrong format . The status message contains additional information.")
    })
    @GetMapping(value = "/cmmn-runtime/case-instances", produces = "application/json")
    public DataResponse<CaseInstanceResponse> getCaseInstances(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams) {
        // Populate query based on request
        CaseInstanceQueryRequest queryRequest = new CaseInstanceQueryRequest();

        if (allRequestParams.containsKey("id")) {
            queryRequest.setCaseInstanceId(allRequestParams.get("id"));
        }

        if (allRequestParams.containsKey("ids")) {
            queryRequest.setCaseInstanceIds(RequestUtil.parseToSet(allRequestParams.get("ids")));
        }

        if (allRequestParams.containsKey("caseDefinitionKey")) {
            queryRequest.setCaseDefinitionKey(allRequestParams.get("caseDefinitionKey"));
        }
        
        if (allRequestParams.containsKey("caseDefinitionKeyLike")) {
            queryRequest.setCaseDefinitionKeyLike(allRequestParams.get("caseDefinitionKeyLike"));
        }
        
        if (allRequestParams.containsKey("caseDefinitionKeyLikeIgnoreCase")) {
            queryRequest.setCaseDefinitionKeyLikeIgnoreCase(allRequestParams.get("caseDefinitionKeyLikeIgnoreCase"));
        }

        if (allRequestParams.containsKey("caseDefinitionId")) {
            queryRequest.setCaseDefinitionId(allRequestParams.get("caseDefinitionId"));
        }

        if (allRequestParams.containsKey("caseDefinitionCategory")) {
            queryRequest.setCaseDefinitionCategory(allRequestParams.get("caseDefinitionCategory"));
        }
        
        if (allRequestParams.containsKey("caseDefinitionCategoryLike")) {
            queryRequest.setCaseDefinitionCategoryLike(allRequestParams.get("caseDefinitionCategoryLike"));
        }
        
        if (allRequestParams.containsKey("caseDefinitionCategoryLikeIgnoreCase")) {
            queryRequest.setCaseDefinitionCategoryLikeIgnoreCase(allRequestParams.get("caseDefinitionCategoryLikeIgnoreCase"));
        }
        
        if (allRequestParams.containsKey("caseDefinitionName")) {
            queryRequest.setCaseDefinitionName(allRequestParams.get("caseDefinitionName"));
        }
        
        if (allRequestParams.containsKey("caseDefinitionNameLike")) {
            queryRequest.setCaseDefinitionNameLike(allRequestParams.get("caseDefinitionNameLike"));
        }
        
        if (allRequestParams.containsKey("caseDefinitionNameLikeIgnoreCase")) {
            queryRequest.setCaseDefinitionNameLikeIgnoreCase(allRequestParams.get("caseDefinitionNameLikeIgnoreCase"));
        }
        
        if (allRequestParams.containsKey("name")) {
            queryRequest.setCaseInstanceName(allRequestParams.get("name"));
        }
        
        if (allRequestParams.containsKey("nameLike")) {
            queryRequest.setCaseInstanceNameLike(allRequestParams.get("nameLike"));
        }
        
        if (allRequestParams.containsKey("nameLikeIgnoreCase")) {
            queryRequest.setCaseInstanceNameLikeIgnoreCase(allRequestParams.get("nameLikeIgnoreCase"));
        }

        if (allRequestParams.containsKey("rootScopeId")) {
            queryRequest.setCaseInstanceRootScopeId(allRequestParams.get("rootScopeId"));
        }

        if (allRequestParams.containsKey("parentScopeId")) {
            queryRequest.setCaseInstanceParentScopeId(allRequestParams.get("parentScopeId"));
        }

        if (allRequestParams.containsKey("businessKey")) {
            queryRequest.setCaseInstanceBusinessKey(allRequestParams.get("businessKey"));
        }
        
        if (allRequestParams.containsKey("businessKeyLike")) {
            queryRequest.setCaseInstanceBusinessKeyLike(allRequestParams.get("businessKeyLike"));
        }
        
        if (allRequestParams.containsKey("businessKeyLikeIgnoreCase")) {
            queryRequest.setCaseInstanceBusinessKeyLikeIgnoreCase(allRequestParams.get("businessKeyLikeIgnoreCase"));
        }
        
        if (allRequestParams.containsKey("businessStatus")) {
            queryRequest.setCaseInstanceBusinessStatus(allRequestParams.get("businessStatus"));
        }
        
        if (allRequestParams.containsKey("businessStatusLike")) {
            queryRequest.setCaseInstanceBusinessStatusLike(allRequestParams.get("businessStatusLike"));
        }
        
        if (allRequestParams.containsKey("businessStatusLikeIgnoreCase")) {
            queryRequest.setCaseInstanceBusinessStatusLikeIgnoreCase(allRequestParams.get("businessStatusLikeIgnoreCase"));
        }

        if (allRequestParams.containsKey("caseInstanceParentId")) {
            queryRequest.setCaseInstanceParentId(allRequestParams.get("caseInstanceParentId"));
        }
        
        if (allRequestParams.containsKey("state")) {
            queryRequest.setCaseInstanceState(allRequestParams.get("state"));
        }
        
        if (allRequestParams.containsKey("startedBy")) {
            queryRequest.setCaseInstanceStartedBy(allRequestParams.get("startedBy"));
        }
        
        if (allRequestParams.containsKey("startedBefore")) {
            queryRequest.setCaseInstanceStartedBefore(RequestUtil.getDate(allRequestParams, "startedBefore"));
        }
        
        if (allRequestParams.containsKey("startedAfter")) {
            queryRequest.setCaseInstanceStartedAfter(RequestUtil.getDate(allRequestParams, "startedAfter"));
        }
        
        if (allRequestParams.containsKey("callbackId")) {
            queryRequest.setCaseInstanceCallbackId(allRequestParams.get("callbackId"));
        }
        
        if (allRequestParams.containsKey("callbackIds")) {
            queryRequest.setCaseInstanceCallbackIds(RequestUtil.parseToSet(allRequestParams.get("callbackIds")));
        }

        if (allRequestParams.containsKey("callbackType")) {
            queryRequest.setCaseInstanceCallbackType(allRequestParams.get("callbackType"));
        }
        
        if (allRequestParams.containsKey("parentCaseInstanceId")) {
            queryRequest.setParentCaseInstanceId(allRequestParams.get("parentCaseInstanceId"));
        }

        if (allRequestParams.containsKey("referenceId")) {
            queryRequest.setCaseInstanceReferenceId(allRequestParams.get("referenceId"));
        }
        
        if (allRequestParams.containsKey("referenceType")) {
            queryRequest.setCaseInstanceReferenceType(allRequestParams.get("referenceType"));
        }
        
        if (allRequestParams.containsKey("lastReactivatedBy")) {
            queryRequest.setCaseInstanceLastReactivatedBy(allRequestParams.get("lastReactivatedBy"));
        }
        
        if (allRequestParams.containsKey("lastReactivatedBefore")) {
            queryRequest.setCaseInstanceLastReactivatedBefore(RequestUtil.getDate(allRequestParams, "lastReactivatedBefore"));
        }
        
        if (allRequestParams.containsKey("lastReactivatedAfter")) {
            queryRequest.setCaseInstanceLastReactivatedAfter(RequestUtil.getDate(allRequestParams, "lastReactivatedAfter"));
        }

        if (allRequestParams.containsKey("includeCaseVariables")) {
            queryRequest.setIncludeCaseVariables(Boolean.valueOf(allRequestParams.get("includeCaseVariables")));
        }

        if (allRequestParams.containsKey("includeCaseVariablesNames")) {
            queryRequest.setIncludeCaseVariablesNames(RequestUtil.parseToList(allRequestParams.get("includeCaseVariablesNames")));
        }

        if (allRequestParams.containsKey("activePlanItemDefinitionId")) {
            queryRequest.setActivePlanItemDefinitionId(allRequestParams.get("activePlanItemDefinitionId"));
        }

        if (allRequestParams.containsKey("tenantId")) {
            queryRequest.setTenantId(allRequestParams.get("tenantId"));
        }

        if (allRequestParams.containsKey("tenantIdLike")) {
            queryRequest.setTenantIdLike(allRequestParams.get("tenantIdLike"));
        }
        
        if (allRequestParams.containsKey("tenantIdLikeIgnoreCase")) {
            queryRequest.setTenantIdLikeIgnoreCase(allRequestParams.get("tenantIdLikeIgnoreCase"));
        }

        if (allRequestParams.containsKey("withoutTenantId")) {
            if (Boolean.parseBoolean(allRequestParams.get("withoutTenantId"))) {
                queryRequest.setWithoutTenantId(Boolean.TRUE);
            }
        }

        return getQueryResponse(queryRequest, allRequestParams);
    }

    @ApiOperation(value = "Start a case instance", tags = { "Case Instances" },
            notes = "Note that also a *transientVariables* property is accepted as part of this json, that follows the same structure as the *variables* property.\n\n"
            + "Only one of *caseDefinitionId* or *caseDefinitionKey* an be used in the request body.\n\n"
            + "Parameters *businessKey*, *variables* and *tenantId* are optional.\n\n"
            + "If tenantId is omitted, the default tenant will be used.\n\n "
            + "It is possible to send variables, transientVariables and startFormVariables in one request.\n\n"
            + "More information about the variable format can be found in the REST variables section.\n\n "
            + "Note that the variable-scope that is supplied is ignored, case-variables are always local.\n\n",
            code = 201)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the case instance was created."),
            @ApiResponse(code = 400, message = "Indicates either the case definition was not found (based on id or key), no process is started by sending the given message or an invalid variable has been passed. Status description contains additional information about the error.")
    })
    @PostMapping(value = "/cmmn-runtime/case-instances", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public CaseInstanceResponse createCaseInstance(@RequestBody CaseInstanceCreateRequest request) {

        if (request.getCaseDefinitionId() == null && request.getCaseDefinitionKey() == null) {
            throw new FlowableIllegalArgumentException("Either caseDefinitionId or caseDefinitionKey is required.");
        }

        int paramsSet = ((request.getCaseDefinitionId() != null) ? 1 : 0) + ((request.getCaseDefinitionKey() != null) ? 1 : 0);

        if (paramsSet > 1) {
            throw new FlowableIllegalArgumentException("Only one of caseDefinitionId or caseDefinitionKey should be set.");
        }

        if (request.isTenantSet()) {
            // Tenant-id can only be used with either key or message
            if (request.getCaseDefinitionId() != null) {
                throw new FlowableIllegalArgumentException("TenantId can only be used with either caseDefinitionKey.");
            }
        }

        Map<String, Object> startVariables = null;
        Map<String, Object> transientVariables = null;
        Map<String, Object> startFormVariables = null;
        if (request.getStartFormVariables() != null) {
            startFormVariables = new HashMap<>();
            for (RestVariable variable : request.getStartFormVariables()) {
                if (variable.getName() == null) {
                    throw new FlowableIllegalArgumentException("Variable name is required.");
                }
                startFormVariables.put(variable.getName(), restResponseFactory.getVariableValue(variable));
            }

        }

        if (request.getVariables() != null) {
            startVariables = new HashMap<>();
            for (RestVariable variable : request.getVariables()) {
                if (variable.getName() == null) {
                    throw new FlowableIllegalArgumentException("Variable name is required.");
                }
                startVariables.put(variable.getName(), restResponseFactory.getVariableValue(variable));
            }
        }

        if (request.getTransientVariables() != null) {
            transientVariables = new HashMap<>();
            for (RestVariable variable : request.getTransientVariables()) {
                if (variable.getName() == null) {
                    throw new FlowableIllegalArgumentException("Variable name is required.");
                }
                transientVariables.put(variable.getName(), restResponseFactory.getVariableValue(variable));
            }
        }

        // Actually start the instance based on key or id
        try {
            CaseInstance instance = null;

            CaseInstanceBuilder caseInstanceBuilder = runtimeService.createCaseInstanceBuilder();
            if (request.getCaseDefinitionId() != null) {
                caseInstanceBuilder.caseDefinitionId(request.getCaseDefinitionId());
            }
            if (request.getCaseDefinitionKey() != null) {
                caseInstanceBuilder.caseDefinitionKey(request.getCaseDefinitionKey());
            }
            if (request.getName() != null) {
                caseInstanceBuilder.name(request.getName());
            }
            if (request.getBusinessKey() != null) {
                caseInstanceBuilder.businessKey(request.getBusinessKey());
            }
            if (request.isTenantSet()) {
                caseInstanceBuilder.tenantId(request.getTenantId());
            }
            if (request.getOverrideDefinitionTenantId() != null && !request.getOverrideDefinitionTenantId().isEmpty()) {
                caseInstanceBuilder.overrideCaseDefinitionTenantId(request.getOverrideDefinitionTenantId());
            }
            if (startVariables != null) {
                caseInstanceBuilder.variables(startVariables);
            }
            if (transientVariables != null) {
                caseInstanceBuilder.transientVariables(transientVariables);
            }
            if (startFormVariables != null) {
                caseInstanceBuilder.startFormVariables(startFormVariables);
            }
            if (request.getOutcome() != null) {
                caseInstanceBuilder.outcome(request.getOutcome());
            }
            
            if (restApiInterceptor != null) {
                restApiInterceptor.createCaseInstance(caseInstanceBuilder, request);
            }

            instance = caseInstanceBuilder.start();

            CaseInstanceResponse caseInstanceResponse = null;
            if (request.getReturnVariables()) {
                Map<String, Object> runtimeVariableMap = runtimeService.getVariables(instance.getId());
                caseInstanceResponse = restResponseFactory.createCaseInstanceResponse(instance, true, runtimeVariableMap);

            } else {
                caseInstanceResponse = restResponseFactory.createCaseInstanceResponse(instance);
            }
            
            CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().caseDefinitionId(caseInstanceResponse.getCaseDefinitionId()).singleResult();
            if (caseDefinition != null) {
                caseInstanceResponse.setCaseDefinitionName(caseDefinition.getName());
                caseInstanceResponse.setCaseDefinitionDescription(caseDefinition.getDescription());
            }

            return caseInstanceResponse;

        } catch (FlowableObjectNotFoundException aonfe) {
            throw new FlowableIllegalArgumentException(aonfe.getMessage(), aonfe);
        }
    }

    @ApiOperation(value = "Post action request to delete/terminate a bulk of case instances", tags = { "Case Instances" }, nickname = "bulkDeleteCaseInstances", code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the bulk of case instances was found and deleted. Response body is left empty intentionally."),
            @ApiResponse(code = 404, message = "Indicates at least one requested case instance was not found.")
    })
    @PostMapping(value = "/cmmn-runtime/case-instances/delete")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void bulkDeleteCaseInstances(@RequestBody BulkDeleteInstancesRestActionRequest request) {
        if (BulkDeleteInstancesRestActionRequest.DELETE_ACTION.equals(request.getAction())) {
            if (restApiInterceptor != null) {
                restApiInterceptor.bulkDeleteCaseInstances(request.getInstanceIds());
            }
            runtimeService.bulkDeleteCaseInstances(request.getInstanceIds());
        } else if (BulkDeleteInstancesRestActionRequest.TERMINATE_ACTION.equals(request.getAction())) {
            if (restApiInterceptor != null) {
                restApiInterceptor.bulkTerminateCaseInstances(request.getInstanceIds());
            }
            runtimeService.bulkTerminateCaseInstances(request.getInstanceIds());
        } else {
            throw new FlowableIllegalArgumentException("Illegal action: '" + request.getAction() + "'.");
        }
    }
}
