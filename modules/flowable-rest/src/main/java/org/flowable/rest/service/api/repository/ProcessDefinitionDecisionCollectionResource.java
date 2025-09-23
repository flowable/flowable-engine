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

import org.flowable.dmn.api.DmnDecision;
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
 * @author Yvo Swillens
 */
@RestController
@Api(tags = { "Process Definitions" }, authorizations = { @Authorization(value = "basicAuth") })
public class ProcessDefinitionDecisionCollectionResource extends BaseProcessDefinitionResource {

    @ApiOperation(value = "List decisions for a process-definition", nickname = "listProcessDefinitionDecisions", tags = { "Process Definitions" })
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Indicates the process definition was found and the decisions are returned.", response = DecisionResponse.class, responseContainer = "List"),
        @ApiResponse(code = 404, message = "Indicates the requested process definition was not found.")
    })
    @GetMapping(value = "/repository/process-definitions/{processDefinitionId}/decisions", produces = "application/json")
    public List<DecisionResponse> getDecisionsForProcessDefinition(
        @ApiParam(name = "processDefinitionId") @PathVariable String processDefinitionId) {

        ProcessDefinition processDefinition = getProcessDefinitionFromRequest(processDefinitionId);

        List<DmnDecision> decisions = repositoryService.getDecisionsForProcessDefinition(processDefinition.getId());

        return restResponseFactory.createDecisionResponseList(decisions, processDefinitionId);
    }

    /**
     * @deprecated
     */
    @Deprecated
    @ApiOperation(value = "List decision tables for a process-definition", nickname = "listProcessDefinitionDecisionTables", tags = { "Process Definitions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the process definition was found and the decision tables are returned.", response = DecisionResponse.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "Indicates the requested process definition was not found.")
    })
    @GetMapping(value = "/repository/process-definitions/{processDefinitionId}/decision-tables", produces = "application/json")
    public List<DecisionResponse> getDecisionTablesForProcessDefinition(
            @ApiParam(name = "processDefinitionId") @PathVariable String processDefinitionId) {

        return getDecisionsForProcessDefinition(processDefinitionId);
    }
}
