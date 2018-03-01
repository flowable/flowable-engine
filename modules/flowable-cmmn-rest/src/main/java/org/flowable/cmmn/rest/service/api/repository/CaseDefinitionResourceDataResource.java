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

package org.flowable.cmmn.rest.service.api.repository;

import javax.servlet.http.HttpServletResponse;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
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
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Case Definitions" }, description = "Manage Case Definitions", authorizations = { @Authorization(value = "basicAuth") })
public class CaseDefinitionResourceDataResource extends BaseDeploymentResourceDataResource {

    @ApiOperation(value = "Get a case definition resource content", tags = { "Case Definitions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates both case definition and resource have been found and the resource data has been returned."),
            @ApiResponse(code = 404, message = "Indicates the requested case definition was not found or there is no resource with the given id present in the case definition. The status-description contains additional information.")
    })
    @GetMapping(value = "/cmmn-repository/case-definitions/{caseDefinitionId}/resourcedata")
    public byte[] getProcessDefinitionResource(@ApiParam(name = "caseDefinitionId") @PathVariable String caseDefinitionId, HttpServletResponse response) {
        CaseDefinition caseDefinition = getCaseDefinitionFromRequest(caseDefinitionId);
        return getDeploymentResourceData(caseDefinition.getDeploymentId(), caseDefinition.getResourceName(), response);
    }

    /**
     * Returns the {@link CaseDefinition} that is requested. Throws the right exceptions when bad request was made or definition is not found.
     */
    protected CaseDefinition getCaseDefinitionFromRequest(String caseDefinitionId) {
        CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionId).singleResult();

        if (caseDefinition == null) {
            throw new FlowableObjectNotFoundException("Could not find a case definition with id '" + caseDefinitionId + "'.", CaseDefinition.class);
        }
        return caseDefinition;
    }
}
