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
package org.flowable.rest.service.api.repository;

import java.util.List;

import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.form.api.FormDefinition;
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
@Api(tags = { "Process Definitions" }, authorizations = { @Authorization(value = "basicAuth") })
public class ProcessDefinitionFormDefinitionCollectionResource extends BaseProcessDefinitionResource {

    @ApiOperation(value = "List form definitions for a process-definition", nickname = "listProcessDefinitionFormDefinitions", tags = { "Process Definitions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the process definition was found and the form definitions are returned.", response = FormDefinitionResponse.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "Indicates the requested process definition was not found.")
    })
    @GetMapping(value = "/repository/process-definitions/{processDefinitionId}/form-definitions", produces = "application/json")
    public List<FormDefinitionResponse> getFormDefinitionsForProcessDefinition(
            @ApiParam(name = "processDefinitionId") @PathVariable String processDefinitionId) {
        
        ProcessDefinition processDefinition = getProcessDefinitionFromRequest(processDefinitionId);

        List<FormDefinition> formDefinitions = repositoryService.getFormDefinitionsForProcessDefinition(processDefinition.getId());

        return restResponseFactory.createFormDefinitionResponseList(formDefinitions, processDefinitionId);
    }
}
