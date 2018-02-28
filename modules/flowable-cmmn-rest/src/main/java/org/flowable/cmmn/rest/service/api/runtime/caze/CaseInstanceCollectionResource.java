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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.rest.api.DataResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
 * Modified the "createCaseInstance" method to conditionally call a "createCaseInstanceResponse" method with a different signature, which will conditionally return the case variables that
 * exist when the case instance either enters its first wait state or completes. In this case, the different method is always called with a flag of true, which means that it will always return
 * those variables. If variables are not to be returned, the original method is called, which doesn't return the variables.
 * 
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Case Instances" }, description = "Manage Case Instances", authorizations = { @Authorization(value = "basicAuth") })
public class CaseInstanceCollectionResource extends BaseCaseInstanceResource {

    @Autowired
    protected CmmnHistoryService historyService;

    @ApiOperation(value = "List case instances", nickname ="listCaseInstances", tags = { "Case Instances" })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "string", value = "Only return models with the given version.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionKey", dataType = "string", value = "Only return case instances with the given case definition key.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionId", dataType = "string", value = "Only return case instances with the given case definition id.", paramType = "query"),
            @ApiImplicitParam(name = "businessKey", dataType = "string", value = "Only return case instances with the given businessKey.", paramType = "query"),
            @ApiImplicitParam(name = "superCaseInstanceId", dataType = "string", value = "Only return case instances which have the given super case instance id (for cases that have a case tasks).", paramType = "query"),
            @ApiImplicitParam(name = "includeCaseVariables", dataType = "boolean", value = "Indication to include case variables in the result.", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return case instances with the given tenantId.", paramType = "query"),
            @ApiImplicitParam(name = "tenantIdLike", dataType = "string", value = "Only return case instances with a tenantId like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "withoutTenantId", dataType = "boolean", value = "If true, only returns case instances without a tenantId set. If false, the withoutTenantId parameter is ignored.", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues = "id,caseDefinitionId,tenantId,caseDefinitionKey", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the case instances are returned"),
            @ApiResponse(code = 400, message = "Indicates a parameter was passed in the wrong format . The status message contains additional information.")
    })
    @GetMapping(value = "/cmmn-runtime/case-instances", produces = "application/json")
    public DataResponse<CaseInstanceResponse> getCaseInstances(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        // Populate query based on request
        CaseInstanceQueryRequest queryRequest = new CaseInstanceQueryRequest();

        if (allRequestParams.containsKey("id")) {
            queryRequest.setCaseInstanceId(allRequestParams.get("id"));
        }

        if (allRequestParams.containsKey("caseDefinitionKey")) {
            queryRequest.setCaseDefinitionKey(allRequestParams.get("caseDefinitionKey"));
        }

        if (allRequestParams.containsKey("caseDefinitionId")) {
            queryRequest.setCaseDefinitionId(allRequestParams.get("caseDefinitionId"));
        }

        if (allRequestParams.containsKey("businessKey")) {
            queryRequest.setCaseBusinessKey(allRequestParams.get("businessKey"));
        }

        if (allRequestParams.containsKey("caseInstanceParentId")) {
            queryRequest.setCaseInstanceParentId(allRequestParams.get("caseInstanceParentId"));
        }

        if (allRequestParams.containsKey("includeCaseVariables")) {
            queryRequest.setIncludeCaseVariables(Boolean.valueOf(allRequestParams.get("includeCaseVariables")));
        }

        if (allRequestParams.containsKey("tenantId")) {
            queryRequest.setTenantId(allRequestParams.get("tenantId"));
        }

        if (allRequestParams.containsKey("tenantIdLike")) {
            queryRequest.setTenantIdLike(allRequestParams.get("tenantIdLike"));
        }

        if (allRequestParams.containsKey("withoutTenantId")) {
            if (Boolean.valueOf(allRequestParams.get("withoutTenantId"))) {
                queryRequest.setWithoutTenantId(Boolean.TRUE);
            }
        }

        return getQueryResponse(queryRequest, allRequestParams);
    }

    @ApiOperation(value = "Start a case instance", tags = { "Case Instances" },
            notes = "Note that also a *transientVariables* property is accepted as part of this json, that follows the same structure as the *variables* property.\n\n"
            + "Only one of *caseDefinitionId* or *caseDefinitionKey* an be used in the request body. \n\n"
            + "Parameters *businessKey*, *variables* and *tenantId* are optional.\n\n "
            + "If tenantId is omitted, the default tenant will be used. More information about the variable format can be found in the REST variables section.\n\n "
            + "Note that the variable-scope that is supplied is ignored, process-variables are always local.\n\n")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the case instance was created."),
            @ApiResponse(code = 400, message = "Indicates either the case definition was not found (based on id or key), no process is started by sending the given message or an invalid variable has been passed. Status description contains additional information about the error.")
    })
    @PostMapping(value = "/cmmn-runtime/case-instances", produces = "application/json")
    public CaseInstanceResponse createCaseInstance(@RequestBody CaseInstanceCreateRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {

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
        if (request.getVariables() != null) {
            startVariables = new HashMap<>();
            for (RestVariable variable : request.getVariables()) {
                if (variable.getName() == null) {
                    throw new FlowableIllegalArgumentException("Variable name is required.");
                }
                startVariables.put(variable.getName(), restResponseFactory.getVariableValue(variable));
            }
        }

        Map<String, Object> transientVariables = null;
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
            if (request.getBusinessKey() != null) {
                caseInstanceBuilder.businessKey(request.getBusinessKey());
            }
            if (request.isTenantSet()) {
                caseInstanceBuilder.tenantId(request.getTenantId());
            }
            if (startVariables != null) {
                caseInstanceBuilder.variables(startVariables);
            }
            if (transientVariables != null) {
                caseInstanceBuilder.transientVariables(transientVariables);
            }

            instance = caseInstanceBuilder.start();

            response.setStatus(HttpStatus.CREATED.value());

            if (request.getReturnVariables()) {
                Map<String, Object> runtimeVariableMap = runtimeService.getVariables(instance.getId());
                return restResponseFactory.createCaseInstanceResponse(instance, true, runtimeVariableMap);

            } else {
                return restResponseFactory.createCaseInstanceResponse(instance);
            }

        } catch (FlowableObjectNotFoundException aonfe) {
            throw new FlowableIllegalArgumentException(aonfe.getMessage(), aonfe);
        }
    }
}
