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

package org.flowable.rest.service.api.runtime.process;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.common.rest.api.RequestUtil;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.flowable.rest.service.api.BulkDeleteInstancesRestActionRequest;
import org.flowable.rest.service.api.engine.variable.RestVariable;
import org.flowable.variable.api.history.HistoricVariableInstance;
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
 * Modified the "createProcessInstance" method to conditionally call a "createProcessInstanceResponse" method with a different signature, which will conditionally return the process variables that
 * exist when the process instance either enters its first wait state or completes. In this case, the different method is always called with a flag of true, which means that it will always return
 * those variables. If variables are not to be returned, the original method is called, which does not return the variables.
 * 
 * @author Frederik Heremans
 * @author Ryan Johnston (@rjfsu)
 * @author Zheng Ji
 */
@RestController
@Api(tags = { "Process Instances" }, authorizations = { @Authorization(value = "basicAuth") })
public class ProcessInstanceCollectionResource extends BaseProcessInstanceResource {

    @Autowired
    protected HistoryService historyService;
    
    @Autowired
    protected RepositoryService repositoryService;

    @ApiOperation(value = "List process instances", nickname ="listProcessInstances", tags = { "Process Instances" })
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", dataType = "string", value = "Only return process instances with the given version.", paramType = "query"),
        @ApiImplicitParam(name = "name", dataType = "string", value = "Only return process instances with the given name.", paramType = "query"),
        @ApiImplicitParam(name = "nameLike", dataType = "string", value = "Only return process instances like the given name.", paramType = "query"),
        @ApiImplicitParam(name = "nameLikeIgnoreCase", dataType = "string", value = "Only return process instances like the given name ignoring case.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionName", dataType = "string", value = "Only return process instances with the given process definition name.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionNameLike", dataType = "string", value = "Only return process instances like the given process definition name.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionNameLikeIgnoreCase", dataType = "string", value = "Only return process instances like the given process definition name ignoring case.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionKey", dataType = "string", value = "Only return process instances with the given process definition key.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionKeyLike", dataType = "string", value = "Only return process instances like the given process definition key.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionKeyLikeIgnoreCase", dataType = "string", value = "Only return process instances like the given process definition key ignoring case.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionId", dataType = "string", value = "Only return process instances with the given process definition id.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionCategory", dataType = "string", value = "Only return process instances with the given process definition category.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionCategoryLike", dataType = "string", value = "Only return process instances like the given process definition category.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionCategoryLikeIgnoreCase", dataType = "string", value = "Only return process instances like the given process definition category ignoring case.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionVersion", dataType = "integer", value = "Only return process instances with the given process definition version.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionEngineVersion", dataType = "string", value = "Only return process instances with the given process definition engine version.", paramType = "query"),
        @ApiImplicitParam(name = "businessKey", dataType = "string", value = "Only return process instances with the given business key.", paramType = "query"),
        @ApiImplicitParam(name = "businessKeyLike", dataType = "string", value = "Only return process instances with the business key like the given key.", paramType = "query"),
        @ApiImplicitParam(name = "businessKeyLikeIgnoreCase", dataType = "string", value = "Only return process instances with the business key like the given key ignoring case.", paramType = "query"),
        @ApiImplicitParam(name = "businessStatus", dataType = "string", value = "Only return process instances with the given business status.", paramType = "query"),
        @ApiImplicitParam(name = "businessStatusLike", dataType = "string", value = "Only return process instances with the business status like the given status.", paramType = "query"),
        @ApiImplicitParam(name = "businessStatusLikeIgnoreCase", dataType = "string", value = "Only return process instances with the business status like the given status ignoring case.", paramType = "query"),
        @ApiImplicitParam(name = "startedBy", dataType = "string", value = "Only return process instances started by the given user.", paramType = "query"),
        @ApiImplicitParam(name = "startedBefore", dataType = "string", format = "date-time", value = "Only return process instances started before the given date.", paramType = "query"),
        @ApiImplicitParam(name = "startedAfter", dataType = "string", format = "date-time", value = "Only return process instances started after the given date.", paramType = "query"),
        @ApiImplicitParam(name = "activeActivityId", dataType = "string", value = "Only return process instances which have an active activity instance with the provided activity id.", paramType = "query"),
        @ApiImplicitParam(name = "involvedUser", dataType = "string", value = "Only return process instances in which the given user is involved.", paramType = "query"),
        @ApiImplicitParam(name = "suspended", dataType = "boolean", value = "If true, only return process instance which are suspended. If false, only return process instances which are not suspended (active).", paramType = "query"),
        @ApiImplicitParam(name = "superProcessInstanceId", dataType = "string", value = "Only return process instances which have the given super process-instance id (for processes that have a call-activities).", paramType = "query"),
        @ApiImplicitParam(name = "rootScopeId", dataType = "string", value = "Only return process instances which have the given root scope id (that can be a process or case instance ID).", paramType = "query"),
        @ApiImplicitParam(name = "parentScopeId", dataType = "string", value = "Only return process instances which have the given parent scope id (that can be a process or case instance ID).", paramType = "query"),
        @ApiImplicitParam(name = "subProcessInstanceId", dataType = "string", value = "Only return process instances which have the given sub process-instance id (for processes started as a call-activity).", paramType = "query"),
        @ApiImplicitParam(name = "excludeSubprocesses", dataType = "boolean", value = "Return only process instances which are not sub processes.", paramType = "query"),
        @ApiImplicitParam(name = "includeProcessVariables", dataType = "boolean", value = "Indication to include process variables in the result.", paramType = "query"),
        @ApiImplicitParam(name = "includeProcessVariablesName", dataType = "string", value = "Indication to include process variables with the given names in the result.", paramType = "query"),
        @ApiImplicitParam(name = "callbackId", dataType = "string", value = "Only return process instances with the given callbackId.", paramType = "query"),
        @ApiImplicitParam(name = "callbackIds", dataType = "string", value = "Only return process instances with the given callbackIds.", paramType = "query"),
        @ApiImplicitParam(name = "callbackType", dataType = "string", value = "Only return process instances with the given callbackType.", paramType = "query"),
        @ApiImplicitParam(name = "parentCaseInstanceId", dataType = "string", value = "Only return process instances with the given parent case instance id.", paramType = "query"),
        @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return process instances with the given tenant id.", paramType = "query"),
        @ApiImplicitParam(name = "tenantIdLike", dataType = "string", value = "Only return process instances with a tenant id like the given value.", paramType = "query"),
        @ApiImplicitParam(name = "tenantIdLikeIgnoreCase", dataType = "string", value = "Only return process instances with a tenant id like the given value ignoring case.", paramType = "query"),
        @ApiImplicitParam(name = "withoutTenantId", dataType = "boolean", value = "If true, only returns process instances without a tenantId set. If false, the withoutTenantId parameter is ignored.", paramType = "query"),
        @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues = "id,processDefinitionId,tenantId,processDefinitionKey", paramType = "query"),
        @ApiImplicitParam(name = "order", dataType = "string", value = "The sort order, either 'asc' or 'desc'. Defaults to 'asc'.", paramType = "query"),
        @ApiImplicitParam(name = "start", dataType = "integer", value = "Index of the first row to fetch. Defaults to 0.", paramType = "query"),
        @ApiImplicitParam(name = "size", dataType = "integer", value = "Number of rows to fetch, starting from start. Defaults to 10.", paramType = "query"), })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the process-instances are returned"),
            @ApiResponse(code = 400, message = "Indicates a parameter was passed in the wrong format . The status-message contains additional information.")
    })
    @GetMapping(value = "/runtime/process-instances", produces = "application/json")
    public DataResponse<ProcessInstanceResponse> getProcessInstances(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams) {
        // Populate query based on request
        ProcessInstanceQueryRequest queryRequest = new ProcessInstanceQueryRequest();

        if (allRequestParams.containsKey("id")) {
            queryRequest.setProcessInstanceId(allRequestParams.get("id"));
        }
        
        if (allRequestParams.containsKey("name")) {
            queryRequest.setProcessInstanceName(allRequestParams.get("name"));
        }
        
        if (allRequestParams.containsKey("nameLike")) {
            queryRequest.setProcessInstanceNameLike(allRequestParams.get("nameLike"));
        }
        
        if (allRequestParams.containsKey("nameLikeIgnoreCase")) {
            queryRequest.setProcessInstanceNameLikeIgnoreCase(allRequestParams.get("nameLikeIgnoreCase"));
        }
        
        if (allRequestParams.containsKey("processDefinitionName")) {
            queryRequest.setProcessDefinitionName(allRequestParams.get("processDefinitionName"));
        }
        
        if (allRequestParams.containsKey("processDefinitionNameLike")) {
            queryRequest.setProcessDefinitionNameLike(allRequestParams.get("processDefinitionNameLike"));
        }
        
        if (allRequestParams.containsKey("processDefinitionNameLikeIgnoreCase")) {
            queryRequest.setProcessDefinitionNameLikeIgnoreCase(allRequestParams.get("processDefinitionNameLikeIgnoreCase"));
        }

        if (allRequestParams.containsKey("processDefinitionKey")) {
            queryRequest.setProcessDefinitionKey(allRequestParams.get("processDefinitionKey"));
        }
        
        if (allRequestParams.containsKey("processDefinitionKeyLike")) {
            queryRequest.setProcessDefinitionKeyLike(allRequestParams.get("processDefinitionKeyLike"));
        }
        
        if (allRequestParams.containsKey("processDefinitionKeyLikeIgnoreCase")) {
            queryRequest.setProcessDefinitionKeyLikeIgnoreCase(allRequestParams.get("processDefinitionKeyLikeIgnoreCase"));
        }

        if (allRequestParams.containsKey("processDefinitionId")) {
            queryRequest.setProcessDefinitionId(allRequestParams.get("processDefinitionId"));
        }
        
        if (allRequestParams.containsKey("processDefinitionCategory")) {
            queryRequest.setProcessDefinitionCategory(allRequestParams.get("processDefinitionCategory"));
        }
        
        if (allRequestParams.containsKey("processDefinitionCategoryLike")) {
            queryRequest.setProcessDefinitionCategoryLike(allRequestParams.get("processDefinitionCategoryLike"));
        }
        
        if (allRequestParams.containsKey("processDefinitionCategoryLikeIgnoreCase")) {
            queryRequest.setProcessDefinitionCategoryLikeIgnoreCase(allRequestParams.get("processDefinitionCategoryLikeIgnoreCase"));
        }
        
        if (allRequestParams.containsKey("processDefinitionVersion")) {
            queryRequest.setProcessDefinitionVersion(Integer.valueOf(allRequestParams.get("processDefinitionVersion")));
        }
        
        if (allRequestParams.containsKey("processDefinitionEngineVersion")) {
            queryRequest.setProcessDefinitionEngineVersion(allRequestParams.get("processDefinitionEngineVersion"));
        }

        if (allRequestParams.containsKey("businessKey")) {
            queryRequest.setProcessBusinessKey(allRequestParams.get("businessKey"));
        }
        
        if (allRequestParams.containsKey("businessKeyLike")) {
            queryRequest.setProcessBusinessKeyLike(allRequestParams.get("businessKeyLike"));
        }
        
        if (allRequestParams.containsKey("businessKeyLikeIgnoreCase")) {
            queryRequest.setProcessBusinessKeyLikeIgnoreCase(allRequestParams.get("businessKeyLikeIgnoreCase"));
        }
        
        if (allRequestParams.containsKey("businessStatus")) {
            queryRequest.setProcessBusinessStatus(allRequestParams.get("businessStatus"));
        }
        
        if (allRequestParams.containsKey("businessStatusLike")) {
            queryRequest.setProcessBusinessStatusLike(allRequestParams.get("businessStatusLike"));
        }
        
        if (allRequestParams.containsKey("businessStatusLikeIgnoreCase")) {
            queryRequest.setProcessBusinessStatusLikeIgnoreCase(allRequestParams.get("businessStatusLikeIgnoreCase"));
        }
        
        if (allRequestParams.containsKey("startedBy")) {
            queryRequest.setStartedBy(allRequestParams.get("startedBy"));
        }
        
        if (allRequestParams.containsKey("startedBefore")) {
            queryRequest.setStartedBefore(RequestUtil.getDate(allRequestParams, "startedBefore"));
        }
        
        if (allRequestParams.containsKey("startedAfter")) {
            queryRequest.setStartedAfter(RequestUtil.getDate(allRequestParams, "startedAfter"));
        }
        
        if (allRequestParams.containsKey("activeActivityId")) {
            queryRequest.setActiveActivityId(allRequestParams.get("activeActivityId"));
        }

        if (allRequestParams.containsKey("involvedUser")) {
            queryRequest.setInvolvedUser(allRequestParams.get("involvedUser"));
        }

        if (allRequestParams.containsKey("suspended")) {
            queryRequest.setSuspended(Boolean.valueOf(allRequestParams.get("suspended")));
        }

        if (allRequestParams.containsKey("rootScopeId")) {
            queryRequest.setRootScopeId(allRequestParams.get("rootScopeId"));
        }

        if (allRequestParams.containsKey("parentScopeId")) {
            queryRequest.setParentScopeId(allRequestParams.get("parentScopeId"));
        }

        if (allRequestParams.containsKey("superProcessInstanceId")) {
            queryRequest.setSuperProcessInstanceId(allRequestParams.get("superProcessInstanceId"));
        }

        if (allRequestParams.containsKey("subProcessInstanceId")) {
            queryRequest.setSubProcessInstanceId(allRequestParams.get("subProcessInstanceId"));
        }

        if (allRequestParams.containsKey("excludeSubprocesses")) {
            queryRequest.setExcludeSubprocesses(Boolean.valueOf(allRequestParams.get("excludeSubprocesses")));
        }

        if (allRequestParams.containsKey("includeProcessVariables")) {
            queryRequest.setIncludeProcessVariables(Boolean.valueOf(allRequestParams.get("includeProcessVariables")));
        }

        if (allRequestParams.containsKey("includeProcessVariablesNames")) {
            queryRequest.setIncludeProcessVariablesNames(RequestUtil.parseToList(allRequestParams.get("includeProcessVariablesNames")));
        }
        
        if (allRequestParams.containsKey("callbackId")) {
            queryRequest.setCallbackId(allRequestParams.get("callbackId"));
        }

        if (allRequestParams.containsKey("callbackIds")) {
            queryRequest.setCallbackIds(RequestUtil.parseToSet(allRequestParams.get("callbackIds")));
        }
        if (allRequestParams.containsKey("callbackType")) {
            queryRequest.setCallbackType(allRequestParams.get("callbackType"));
        }
        
        if (allRequestParams.containsKey("parentCaseInstanceId")) {
            queryRequest.setParentCaseInstanceId(allRequestParams.get("parentCaseInstanceId"));
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

    @ApiOperation(value = "Start a process instance", tags = { "Process Instances" },
            notes = "Note that also a *transientVariables* property is accepted as part of this json, that follows the same structure as the *variables* property.\n\n"
            + "Only one of *processDefinitionId*, *processDefinitionKey* or *message* can be used in the request body. \n\n"
            + "Parameters *businessKey*, *variables* and *tenantId* are optional.\n\n"
            + "If tenantId is omitted, the default tenant will be used.\n\n "
            + "It is possible to send variables, transientVariables and startFormVariables in one request.\n\n"
            + "More information about the variable format can be found in the REST variables section.\n\n "
            + "Note that the variable-scope that is supplied is ignored, process-variables are always local.\n\n",
            code = 201)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the process instance was created."),
            @ApiResponse(code = 400, message = "Indicates either the process-definition was not found (based on id or key), no process is started by sending the given message or an invalid variable has been passed. Status description contains additional information about the error.")
    })
    @PostMapping(value = "/runtime/process-instances", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public ProcessInstanceResponse createProcessInstance(@RequestBody ProcessInstanceCreateRequest request) {

        if (request.getProcessDefinitionId() == null && request.getProcessDefinitionKey() == null && request.getMessage() == null) {
            throw new FlowableIllegalArgumentException("Either processDefinitionId, processDefinitionKey or message is required.");
        }

        int paramsSet = ((request.getProcessDefinitionId() != null) ? 1 : 0) + ((request.getProcessDefinitionKey() != null) ? 1 : 0) + ((request.getMessage() != null) ? 1 : 0);

        if (paramsSet > 1) {
            throw new FlowableIllegalArgumentException("Only one of processDefinitionId, processDefinitionKey or message should be set.");
        }

        if (request.isTenantSet()) {
            // Tenant-id can only be used with either key or message
            if (request.getProcessDefinitionId() != null) {
                throw new FlowableIllegalArgumentException("TenantId can only be used with either processDefinitionKey or message.");
            }
        }
        
        Map<String, Object> startVariables = null;
        Map<String, Object> transientVariables = null;
        Map<String, Object> startFormVariables = null;
        if (request.getStartFormVariables() != null && !request.getStartFormVariables().isEmpty()) {
            startFormVariables = new HashMap<>();
            for (RestVariable variable : request.getStartFormVariables()) {
                if (variable.getName() == null) {
                    throw new FlowableIllegalArgumentException("Variable name is required.");
                }
                startFormVariables.put(variable.getName(), restResponseFactory.getVariableValue(variable));
            }
        }

        if (request.getVariables() != null && !request.getVariables().isEmpty()) {
            startVariables = new HashMap<>();
            for (RestVariable variable : request.getVariables()) {
                if (variable.getName() == null) {
                    throw new FlowableIllegalArgumentException("Variable name is required.");
                }
                startVariables.put(variable.getName(), restResponseFactory.getVariableValue(variable));
            }
        }

        if (request.getTransientVariables() != null && !request.getTransientVariables().isEmpty()) {
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
            ProcessInstance instance = null;

            ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder();
            if (request.getProcessDefinitionId() != null) {
                processInstanceBuilder.processDefinitionId(request.getProcessDefinitionId());
            }
            if (request.getProcessDefinitionKey() != null) {
                processInstanceBuilder.processDefinitionKey(request.getProcessDefinitionKey());
            }
            if (request.getMessage() != null) {
                processInstanceBuilder.messageName(request.getMessage());
            }
            if (request.getName() != null) {
                processInstanceBuilder.name(request.getName());
            }
            if (request.getBusinessKey() != null) {
                processInstanceBuilder.businessKey(request.getBusinessKey());
            }
            if (request.isTenantSet()) {
                processInstanceBuilder.tenantId(request.getTenantId());
            }
            if (request.getOverrideDefinitionTenantId() != null && !request.getOverrideDefinitionTenantId().isEmpty()) {
                processInstanceBuilder.overrideProcessDefinitionTenantId(request.getOverrideDefinitionTenantId());
            }
            if (startFormVariables != null) {
                processInstanceBuilder.startFormVariables(startFormVariables);
            }
            if (startVariables != null) {
                processInstanceBuilder.variables(startVariables);
            }
            if (transientVariables != null) {
                processInstanceBuilder.transientVariables(transientVariables);
            }
            if (request.getOutcome() != null) {
                processInstanceBuilder.outcome(request.getOutcome());
            }
            
            if (restApiInterceptor != null) {
                restApiInterceptor.createProcessInstance(processInstanceBuilder, request);
            }

            instance = processInstanceBuilder.start();

            ProcessInstanceResponse processInstanceResponse = null;
            if (request.getReturnVariables()) {
                Map<String, Object> runtimeVariableMap = null;
                List<HistoricVariableInstance> historicVariableList = null;
                if (instance.isEnded()) {
                    historicVariableList = historyService.createHistoricVariableInstanceQuery().processInstanceId(instance.getId()).list();
                } else {
                    runtimeVariableMap = runtimeService.getVariables(instance.getId());
                }
                processInstanceResponse = restResponseFactory.createProcessInstanceResponse(instance, true, runtimeVariableMap, historicVariableList);

            } else {
                processInstanceResponse = restResponseFactory.createProcessInstanceResponse(instance);
            }
            
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processInstanceResponse.getProcessDefinitionId()).singleResult();
            
            if (processDefinition != null) {
                processInstanceResponse.setProcessDefinitionName(processDefinition.getName());
                processInstanceResponse.setProcessDefinitionDescription(processDefinition.getDescription());
            }

            return processInstanceResponse;

        } catch (FlowableObjectNotFoundException e) {
            throw new FlowableIllegalArgumentException(e.getMessage(), e);
        }
    }

    @ApiOperation(value = "Bulk delete process instances", tags = { "Process Instances" }, nickname = "deleteProcessInstances", code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the bulk of process instances was found and deleted. Response body is left empty intentionally."),
            @ApiResponse(code = 404, message = "Indicates at least one requested process instance was not found.")
    })
    @PostMapping(value = "/runtime/process-instances/delete")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void bulkDeleteProcessInstances(@RequestBody BulkDeleteInstancesRestActionRequest request) {
        if (BulkDeleteInstancesRestActionRequest.DELETE_ACTION.equals(request.getAction())) {

            if (restApiInterceptor != null) {
                restApiInterceptor.bulkDeleteProcessInstances(request.getInstanceIds());
            }
            runtimeService.bulkDeleteProcessInstances(request.getInstanceIds(), request.getDeleteReason());
        } else {
            throw new FlowableIllegalArgumentException("Illegal action: '" + request.getAction() + "'.");
        }
    }
}
