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

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.form.api.FormInstance;
import org.flowable.form.api.FormService;
import org.flowable.form.rest.FormRestApiInterceptor;
import org.flowable.form.rest.FormRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
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
public class FormInstanceResource {

    @Autowired
    protected FormService formService;

    @Autowired
    protected FormRestResponseFactory formRestResponseFactory;
    
    @Autowired(required=false)
    protected FormRestApiInterceptor restApiInterceptor;

    @ApiOperation(value = "Get a form instance", tags = { "Form Instances" }, nickname = "getFormInstance")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the form instance was found and returned."),
            @ApiResponse(code = 404, message = "Indicates the requested form instance was not found.")
    })
    @GetMapping(value = "/form/form-instance/{formInstanceId}", produces = "application/json")
    public FormInstanceResponse getFormInstance(@ApiParam(name = "formInstanceId") @PathVariable String formInstanceId, HttpServletRequest request) {
        FormInstance formInstance = formService.createFormInstanceQuery().id(formInstanceId).singleResult();
        
        if (formInstance == null) {
            throw new FlowableObjectNotFoundException("Could not find a form instance");
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessFormInstanceById(formInstance);
        }
        
        return formRestResponseFactory.createFormInstanceResponse(formInstance);
    }
}
