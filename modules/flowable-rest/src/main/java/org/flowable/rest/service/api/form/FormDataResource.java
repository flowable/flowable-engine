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

package org.flowable.rest.service.api.form;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.FormService;
import org.flowable.engine.form.FormData;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.rest.service.api.BpmnRestApiInterceptor;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Forms" }, description = "Manage Forms", authorizations = { @Authorization(value = "basicAuth") })
public class FormDataResource {

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected FormService formService;
    
    @Autowired(required=false)
    protected BpmnRestApiInterceptor restApiInterceptor;

    @ApiOperation(value = "Get form data", tags = { "Forms" }, notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates that form data could be queried."),
            @ApiResponse(code = 404, message = "Indicates that form data could not be found.") })
    @GetMapping(value = "/form/form-data", produces = "application/json")
    public FormDataResponse getFormData(@RequestParam(value = "taskId", required = false) String taskId,
            @RequestParam(value = "processDefinitionId", required = false) String processDefinitionId, HttpServletRequest request) {

        if (taskId == null && processDefinitionId == null) {
            throw new FlowableIllegalArgumentException("The taskId or processDefinitionId parameter has to be provided");
        }

        if (taskId != null && processDefinitionId != null) {
            throw new FlowableIllegalArgumentException("Not both a taskId and a processDefinitionId parameter can be provided");
        }

        FormData formData = null;
        String id = null;
        if (taskId != null) {
            formData = formService.getTaskFormData(taskId);
            id = taskId;
        } else {
            formData = formService.getStartFormData(processDefinitionId);
            id = processDefinitionId;
        }

        if (formData == null) {
            throw new FlowableObjectNotFoundException("Could not find a form data with id '" + id + "'.", FormData.class);
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessFormData(formData);
        }

        return restResponseFactory.createFormDataResponse(formData);
    }

    @ApiOperation(value = "Submit task form data", tags = { "Forms" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the form data was submitted"),
            @ApiResponse(code = 204, message = "If TaskId has been provided, Indicates request was successful and the form data was submitted. Returns empty"),
            @ApiResponse(code = 400, message = "Indicates an parameter was passed in the wrong format. The status-message contains additional information.") })
    @PostMapping(value = "/form/form-data", produces = "application/json")
    public ProcessInstanceResponse submitForm(@RequestBody SubmitFormRequest submitRequest, HttpServletRequest request, HttpServletResponse response) {

        if (submitRequest == null) {
            throw new FlowableException("A request body was expected when executing the form submit.");
        }

        if (submitRequest.getTaskId() == null && submitRequest.getProcessDefinitionId() == null) {
            throw new FlowableIllegalArgumentException("The taskId or processDefinitionId property has to be provided");
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.submitFormData(submitRequest);
        }

        Map<String, String> propertyMap = new HashMap<>();
        if (submitRequest.getProperties() != null) {
            for (RestFormProperty formProperty : submitRequest.getProperties()) {
                propertyMap.put(formProperty.getId(), formProperty.getValue());
            }
        }

        if (submitRequest.getTaskId() != null) {
            formService.submitTaskFormData(submitRequest.getTaskId(), propertyMap);
            response.setStatus(HttpStatus.NO_CONTENT.value());
            return null;

        } else {
            ProcessInstance processInstance = null;
            if (submitRequest.getBusinessKey() != null) {
                processInstance = formService.submitStartFormData(submitRequest.getProcessDefinitionId(), submitRequest.getBusinessKey(), propertyMap);
            } else {
                processInstance = formService.submitStartFormData(submitRequest.getProcessDefinitionId(), propertyMap);
            }
            return restResponseFactory.createProcessInstanceResponse(processInstance);
        }
    }
}
