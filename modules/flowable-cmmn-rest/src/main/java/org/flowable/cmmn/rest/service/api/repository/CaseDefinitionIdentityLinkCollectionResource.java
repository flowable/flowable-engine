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

import java.util.List;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.rest.service.api.engine.RestIdentityLink;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.identitylink.api.IdentityLinkType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
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
@Api(tags = { "Case Definitions" }, authorizations = { @Authorization(value = "basicAuth") })
public class CaseDefinitionIdentityLinkCollectionResource extends BaseCaseDefinitionResource {

    @ApiOperation(value = "List candidate starters for a case definition", nickname = "listCaseDefinitionIdentityLinks", tags = { "Case Definitions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the case definition was found and the requested identity links are returned."),
            @ApiResponse(code = 404, message = "Indicates the requested case definition was not found.")
    })
    @GetMapping(value = "/cmmn-repository/case-definitions/{caseDefinitionId}/identitylinks", produces = "application/json")
    public List<RestIdentityLink> getIdentityLinks(@ApiParam(name = "caseDefinitionId") @PathVariable String caseDefinitionId) {
        CaseDefinition caseDefinition = getCaseDefinitionFromRequestWithoutAccessCheck(caseDefinitionId);

        if (restApiInterceptor != null) {
            restApiInterceptor.accessCaseDefinitionIdentityLinks(caseDefinition);
        }

        return restResponseFactory.createRestIdentityLinks(repositoryService.getIdentityLinksForCaseDefinition(caseDefinition.getId()));
    }

    @ApiOperation(value = "Add a candidate starter to a case definition", tags = { "Case Definitions" },
            notes = "It is possible to add either a user or a group.", code = 201)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the case definition was found and the identity link was created."),
            @ApiResponse(code = 400, message = "Indicates the body does not contain the correct information."),
            @ApiResponse(code = 404, message = "Indicates the requested case definition was not found.")
    })
    @PostMapping(value = "/cmmn-repository/case-definitions/{caseDefinitionId}/identitylinks", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public RestIdentityLink createIdentityLink(@ApiParam(name = "caseDefinitionId") @PathVariable String caseDefinitionId, @RequestBody RestIdentityLink identityLink) {

        CaseDefinition caseDefinition = getCaseDefinitionFromRequestWithoutAccessCheck(caseDefinitionId);

        if (identityLink.getGroup() == null && identityLink.getUser() == null) {
            throw new FlowableIllegalArgumentException("A group or a user is required to create an identity link.");
        }

        if (identityLink.getGroup() != null && identityLink.getUser() != null) {
            throw new FlowableIllegalArgumentException("Only one of user or group can be used to create an identity link.");
        }

        if (restApiInterceptor != null) {
            restApiInterceptor.createCaseDefinitionIdentityLink(caseDefinition, identityLink);
        }

        if (identityLink.getGroup() != null) {
            repositoryService.addCandidateStarterGroup(caseDefinition.getId(), identityLink.getGroup());
        } else {
            repositoryService.addCandidateStarterUser(caseDefinition.getId(), identityLink.getUser());
        }

        // Always candidate for case definition. User-provided value is ignored
        identityLink.setType(IdentityLinkType.CANDIDATE);

        return restResponseFactory.createRestIdentityLink(identityLink.getType(), identityLink.getUser(), identityLink.getGroup(), null, caseDefinition.getId(), null);
    }

}
