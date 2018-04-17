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
package org.flowable.form.rest.service.api.form;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.form.api.FormInfo;
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
 * @author Yvo Swillens
 */
@RestController
@Api(tags = { "Form Instances" }, description = "Manage Form Instances", authorizations = { @Authorization(value = "basicAuth") })
public class FormInstanceCollectionResource extends BaseFormInstanceResource {

    @ApiOperation(value = "List of form instances", nickname = "listFormInstances", tags = { "Form Instances" })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "string", value = "Only return form instances with the given id.", paramType = "query"),
            @ApiImplicitParam(name = "formDefinitionId", dataType = "string", value = "Only return form instances with the given form definition id.", paramType = "query"),
            @ApiImplicitParam(name = "formDefinitionIdLike", dataType = "string", value = "Only return form instances with a form definition id like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "taskId", dataType = "string", value = "Only return form instances with the given task id.", paramType = "query"),
            @ApiImplicitParam(name = "taskIdLike", dataType = "string", value = "Only return form instances with a task id like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "processInstanceId", dataType = "string", value = "Only return form instances with the given process instance id.", paramType = "query"),
            @ApiImplicitParam(name = "processInstanceIdLike", dataType = "string", value = "Only return form instances with a process instance id like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "processDefinitionId", dataType = "string", value = "Only return form instances with the given process definition id.", paramType = "query"),
            @ApiImplicitParam(name = "processDefinitionIdLike", dataType = "string", value = "Only return form instances with a process definition id like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "scopeId", dataType = "string", value = "Only return form instances with the given scope id.", paramType = "query"),
            @ApiImplicitParam(name = "scopeType", dataType = "string", value = "Only return form instances with a scope type like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "scopeId", dataType = "string", value = "Only return form instances with the given scope definition id.", paramType = "query"),
            @ApiImplicitParam(name = "submittedBy", dataType = "string", value = "Only return form instances submitted by the given value.", paramType = "query"),
            @ApiImplicitParam(name = "submittedByLike", dataType = "string", value = "Only return form instances submitted by like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return form instances with the given tenantId.", paramType = "query"),
            @ApiImplicitParam(name = "tenantIdLike", dataType = "string", value = "Only return form instances with a tenantId like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "withoutTenantId", dataType = "boolean", value = "If true, only returns form instances without a tenantId set. If false, the withoutTenantId parameter is ignored.", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues = "submittedDate,tenantId", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the form instances are returned"),
            @ApiResponse(code = 400, message = "Indicates a parameter was passed in the wrong format . The status-message contains additional information.")
    })
    @GetMapping(value = "/form/form-instances", produces = "application/json")
    public DataResponse<FormInstanceResponse> getFormInstances(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        // Populate query based on request
        FormInstanceQueryRequest queryRequest = new FormInstanceQueryRequest();

        if (allRequestParams.containsKey("id")) {
            queryRequest.setId(allRequestParams.get("id"));
        }
        if (allRequestParams.containsKey("formDefinitionId")) {
            queryRequest.setFormDefinitionId(allRequestParams.get("formDefinitionId"));
        }
        if (allRequestParams.containsKey("formDefinitionIdLike")) {
            queryRequest.setFormDefinitionIdLike(allRequestParams.get("formDefinitionIdLike"));
        }
        if (allRequestParams.containsKey("taskId")) {
            queryRequest.setTaskId(allRequestParams.get("taskId"));
        }
        if (allRequestParams.containsKey("taskIdLike")) {
            queryRequest.setTaskIdLike(allRequestParams.get("taskIdLike"));
        }
        if (allRequestParams.containsKey("processInstanceId")) {
            queryRequest.setProcessInstanceId(allRequestParams.get("processInstanceId"));
        }
        if (allRequestParams.containsKey("processInstanceIdLike")) {
            queryRequest.setProcessInstanceIdLike(allRequestParams.get("processInstanceIdLike"));
        }
        if (allRequestParams.containsKey("processDefinitionId")) {
            queryRequest.setProcessDefinitionId(allRequestParams.get("processDefinitionId"));
        }
        if (allRequestParams.containsKey("processDefinitionIdLike")) {
            queryRequest.setProcessDefinitionIdLike(allRequestParams.get("processDefinitionIdLike"));
        }
        if (allRequestParams.containsKey("scopeId")) {
            queryRequest.setScopeId(allRequestParams.get("scopeId"));
        }
        if (allRequestParams.containsKey("scopeType")) {
            queryRequest.setScopeType(allRequestParams.get("scopeType"));
        }
        if (allRequestParams.containsKey("scopeDefinitionId")) {
            queryRequest.setScopeDefinitionId(allRequestParams.get("scopeDefinitionId"));
        }
        if (allRequestParams.containsKey("submittedBy")) {
            queryRequest.setSubmittedBy(allRequestParams.get("submittedBy"));
        }
        if (allRequestParams.containsKey("submittedByLike")) {
            queryRequest.setSubmittedByLike(allRequestParams.get("submittedByLike"));
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

    @ApiOperation(value = "Store a form instance", tags = { "Form Instances" }, nickname = "storeFormInstance",
            notes = "Provide either a FormDefinitionKey or a FormDefinitionId together with the other properties.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the form instance was stored."),
            @ApiResponse(code = 404, message = "Indicates the related form model was not found.")
    })
    @PostMapping(value = "/form/form-instances", produces = "application/json")
    public void storeFormInstance(@RequestBody FormRequest formRequest, HttpServletRequest request) {

        FormInfo formModel;

        if (formRequest.getFormDefinitionKey() != null) {
            formModel = formService.getFormModelWithVariablesByKey(
                    formRequest.getFormDefinitionKey(),
                    formRequest.getTaskId(),
                    formRequest.getVariables(),
                    formRequest.getTenantId());
            
        } else if (formRequest.getFormDefinitionId() != null) {
            formModel = formService.getFormModelWithVariablesById(
                    formRequest.getFormDefinitionId(),
                    formRequest.getTaskId(),
                    formRequest.getVariables(),
                    formRequest.getTenantId());
            
        } else {
            throw new FlowableIllegalArgumentException("Either form definition key or form definition id must be provided in the request");
        }

        if (formModel == null) {
            throw new FlowableObjectNotFoundException("Could not find a form definition");
        }

        if (formRequest.getScopeId() != null) {
            formService.createFormInstanceWithScopeId(formRequest.getVariables(), formModel, formRequest.getTaskId(),
                            formRequest.getScopeId(), formRequest.getScopeType(), formRequest.getScopeDefinitionId());
            
        } else {
            formService.createFormInstance(formRequest.getVariables(), formModel, formRequest.getTaskId(),
                            formRequest.getProcessInstanceId(), formRequest.getProcessDefinitionId());
        }
    }
}
