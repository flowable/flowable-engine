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
package org.flowable.rest.form.service.api.repository;

import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.form.model.FormModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;

/**
 * @author Yvo Swillens
 */
@RestController
@Api(tags = { "Form Definitions" }, description = "Manage Form Definitions", authorizations = { @Authorization(value = "basicAuth") })
public class FormModelResource {

    @Autowired
    protected FormService formService;

    @Autowired
    protected FormRepositoryService formRepositoryService;

    @ApiOperation(value = "Get a process definition form model", tags = { "Form Definitions" }, nickname = "getModelResource")
    @RequestMapping(value = "/form-repository/form-definitions/{formDefinitionId}/model", method = RequestMethod.GET, produces = "application/json")
    public FormModel getModelResource(@ApiParam(name = "formDefinitionId") @PathVariable String formDefinitionId) {
        FormModel formDefinition = formRepositoryService.getFormModelById(formDefinitionId);

        if (formDefinition == null) {
            throw new FlowableObjectNotFoundException("Could not find a form definition with id '" + formDefinitionId);
        }

        return formDefinition;
    }
}
