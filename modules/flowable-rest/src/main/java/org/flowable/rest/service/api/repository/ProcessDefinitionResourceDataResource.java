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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

/**
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Process Definitions" }, description = "Manage Process Definitions", authorizations = { @Authorization(value = "basicAuth") })
public class ProcessDefinitionResourceDataResource extends BaseDeploymentResourceDataResource {

    @ApiOperation(value = "Get a process definition resource content", tags = { "Process Definitions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates both process definition and resource have been found and the resource data has been returned."),
            @ApiResponse(code = 404, message = "Indicates the requested process definition was not found or there is no resource with the given id present in the process definition. The status-description contains additional information.")
    })
    @GetMapping(value = "/repository/process-definitions/{processDefinitionId}/resourcedata")
    public byte[] getProcessDefinitionResource(@ApiParam(name = "processDefinitionId") @PathVariable String processDefinitionId, HttpServletResponse response) {
        ProcessDefinition processDefinition = getProcessDefinitionFromRequest(processDefinitionId);
        return getDeploymentResourceData(processDefinition.getDeploymentId(), processDefinition.getResourceName(), response);
    }

    /**
     * Returns the {@link ProcessDefinition} that is requested. Throws the right exceptions when bad request was made or definition is not found.
     */
    protected ProcessDefinition getProcessDefinitionFromRequest(String processDefinitionId) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();

        if (processDefinition == null) {
            throw new FlowableObjectNotFoundException("Could not find a process definition with id '" + processDefinitionId + "'.", ProcessDefinition.class);
        }
        return processDefinition;
    }
}
