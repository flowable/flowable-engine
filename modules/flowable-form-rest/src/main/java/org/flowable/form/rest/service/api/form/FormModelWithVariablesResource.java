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

import javax.servlet.http.HttpServletRequest;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormService;
import org.flowable.form.rest.FormRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Yvo Swillens
 */
@RestController
@Api(tags = { "Form Models" }, description = "Manage Form Models", authorizations = { @Authorization(value = "basicAuth") })
public class FormModelWithVariablesResource {

    @Autowired
    protected FormService formService;

    @Autowired
    protected FormRestResponseFactory formRestResponseFactory;

    @ApiOperation(value = "Get a populated form model", tags = { "Form Models" }, notes = "Provide variables needed to pre populated form fields " +
            "and to render expression based form fields")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the form model was found and returned."),
            @ApiResponse(code = 404, message = "Indicates the requested form model was not found.")
    })
    @PostMapping(value = "/form/model", produces = "application/json")
    public FormModelResponse getFormModel(@RequestBody FormRequest formRequest, HttpServletRequest request) {

        FormInfo formModel;

        if (formRequest.getParentDeploymentId() != null) {
            formModel = formService.getFormModelWithVariablesByKeyAndParentDeploymentId(
                    formRequest.getParentDeploymentId(),
                    formRequest.getFormDefinitionKey(),
                    formRequest.getTaskId(),
                    formRequest.getVariables(),
                    formRequest.getTenantId());
            
        } else if (formRequest.getFormDefinitionKey() != null) {
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
            throw new FlowableIllegalArgumentException("Either parent deployment key, form definition key or form definition id must be provided in the request");
        }

        if (formModel == null) {
            throw new FlowableObjectNotFoundException("Could not find a form model");
        }

        return formRestResponseFactory.createFormModelResponse(formModel);
    }

}
