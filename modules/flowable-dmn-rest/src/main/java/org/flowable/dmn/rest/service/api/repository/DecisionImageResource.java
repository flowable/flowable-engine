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

package org.flowable.dmn.rest.service.api.repository;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.dmn.api.DmnDecision;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
 * @author Yvo Swillens
 */
@RestController
@Api(tags = { "Decision" }, authorizations = { @Authorization(value = "basicAuth") })
public class DecisionImageResource extends BaseDecisionResource {

    @ApiOperation(value = "Get a decision requirements diagram image", tags = { "Decisions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the decision requirements diagram image returned"),
            @ApiResponse(code = 404, message = "Indicates the requested decision requirements diagram image was not found.")
    })
    @GetMapping(value = "/dmn-repository/decisions/{decisionId}/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getImageResource(@ApiParam(name = "decisionId") @PathVariable String decisionId) {
        DmnDecision decision = getDecisionFromRequest(decisionId);
        
        try (final InputStream imageStream = dmnRepositoryService.getDecisionRequirementsDiagram(decision.getId())) {
            if (imageStream != null) {
                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.set("Content-Type", MediaType.IMAGE_PNG_VALUE);
                try {
                    return new ResponseEntity<>(IOUtils.toByteArray(imageStream), responseHeaders, HttpStatus.OK);
                } catch (Exception e) {
                    throw new FlowableException("Error reading image stream", e);
                }
            } else {
                throw new FlowableObjectNotFoundException("Decision with id '" + decision.getId() + "' has no image.");
            }
            
        } catch (IOException e) {
            throw new FlowableException("Error reading image stream", e);
        }
    }
}
