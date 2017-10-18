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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.repository.ProcessDefinition;
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
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Process Definitions" }, description = "Manage Process Definitions", authorizations = { @Authorization(value = "basicAuth") })
public class ProcessDefinitionModelResource extends BaseProcessDefinitionResource {

    @ApiOperation(value = "Get a process definition BPMN model", tags = { "Process Definitions" }, nickname = "getBpmnModelResource")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the process definition was found and the model is returned. The response contains the full process definition model."),
            @ApiResponse(code = 404, message = "Indicates the requested process definition was not found.")
    })
    @GetMapping(value = "/repository/process-definitions/{processDefinitionId}/model", produces = "application/json")
    public BpmnModel getModelResource(@ApiParam(name = "processDefinitionId") @PathVariable String processDefinitionId) {
        ProcessDefinition processDefinition = getProcessDefinitionFromRequest(processDefinitionId);
        return repositoryService.getBpmnModel(processDefinition.getId());
    }

}
