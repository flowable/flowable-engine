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
package org.flowable.form.rest.service.api.repository;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.rest.FormRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Yvo Swillens
 */
@RestController
@Api(tags = { "Form Definitions" }, description = "Manage Form Definitions", authorizations = { @Authorization(value = "basicAuth") })
public class FormDefinitionResource {

    @Autowired
    protected FormRestResponseFactory formRestResponseFactory;

    @Autowired
    protected FormRepositoryService formRepositoryService;

    @ApiOperation(value = "Get a form definition", tags = { "Form Definitions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the form definition was found returned."),
            @ApiResponse(code = 404, message = "Indicates the form definition was not found.")
    })
    @GetMapping(value = "/form-repository/form-definitions/{formDefinitionId}", produces = "application/json")
    public FormDefinitionResponse getForm(@ApiParam(name = "formDefinitionId") @PathVariable String formDefinitionId, HttpServletRequest request) {
        FormDefinition formDefinition = formRepositoryService.getFormDefinition(formDefinitionId);

        if (formDefinition == null) {
            throw new FlowableObjectNotFoundException("Could not find a form definition with id '" + formDefinitionId);
        }

        return formRestResponseFactory.createFormDefinitionResponse(formDefinition);
    }
}
