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

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class CaseDefinitionImageResource extends BaseCaseDefinitionResource {

    @ApiOperation(value = "Get a case definition image", tags = { "Case Definitions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the case definitions are returned"),
            @ApiResponse(code = 404, message = "Indicates the requested case definition was not found.")
    })
    @GetMapping("/cmmn-repository/case-definitions/{caseDefinitionId}/image")
    public ResponseEntity<byte[]> getImageResource(@ApiParam(name = "caseDefinitionId") @PathVariable String caseDefinitionId) {
        CaseDefinition caseDefinition = getCaseDefinitionFromRequest(caseDefinitionId);
        InputStream imageStream = repositoryService.getCaseDiagram(caseDefinition.getId());

        if (imageStream != null) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.set("Content-Type", "image/png");
            try {
                return new ResponseEntity<>(IOUtils.toByteArray(imageStream), responseHeaders, HttpStatus.OK);
            } catch (Exception e) {
                throw new FlowableException("Error reading image stream", e);
            }
        } else {
            throw new FlowableObjectNotFoundException("Case definition with id '" + caseDefinition.getId() + "' has no image.");
        }
    }

}
