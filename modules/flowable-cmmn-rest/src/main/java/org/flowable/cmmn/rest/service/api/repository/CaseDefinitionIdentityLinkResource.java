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
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.cmmn.rest.service.api.engine.RestIdentityLink;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
public class CaseDefinitionIdentityLinkResource extends BaseCaseDefinitionResource {

    @ApiOperation(value = "Get a candidate starter from a case definition", tags = { "Case Definitions" }, nickname = "getIdentityLink")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the case definition was found and the identity link was returned."),
            @ApiResponse(code = 404, message = "Indicates the requested case definition was not found or the case definition does not have an identity-link that matches the url.")
    })
    @GetMapping(value = "/cmmn-repository/case-definitions/{caseDefinitionId}/identitylinks/{family}/{identityId}", produces = "application/json")
    public RestIdentityLink getIdentityLinkRequest(@ApiParam(name = "caseDefinitionId") @PathVariable("caseDefinitionId") String caseDefinitionId,
            @ApiParam(name = "family") @PathVariable("family") String family, @ApiParam(name = "identityId") @PathVariable("identityId") String identityId) {

        CaseDefinition caseDefinition = getCaseDefinitionFromRequestWithoutAccessCheck(caseDefinitionId);

        validateIdentityLinkArguments(family, identityId);

        // Check if identitylink to get exists
        IdentityLink link = getIdentityLink(family, identityId, caseDefinition.getId());

        if (restApiInterceptor != null) {
            restApiInterceptor.accessCaseDefinitionIdentityLink(caseDefinition, link);
        }

        return restResponseFactory.createRestIdentityLink(link);
    }

    @ApiOperation(value = "Delete a candidate starter from a case definition", tags = { "Case Definitions" }, code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the case definition was found and the identity link was removed. The response body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested case definition was not found or the case definition does not have an identity-link that matches the url.")
    })
    @DeleteMapping(value = "/cmmn-repository/case-definitions/{caseDefinitionId}/identitylinks/{family}/{identityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIdentityLink(@ApiParam(name = "caseDefinitionId") @PathVariable("caseDefinitionId") String caseDefinitionId,
            @ApiParam(name = "family") @PathVariable("family") String family, @ApiParam(name = "identityId") @PathVariable("identityId") String identityId) {

        CaseDefinition caseDefinition = getCaseDefinitionFromRequestWithoutAccessCheck(caseDefinitionId);

        validateIdentityLinkArguments(family, identityId);

        // Check if identitylink to delete exists
        IdentityLink link = getIdentityLink(family, identityId, caseDefinition.getId());

        if (restApiInterceptor != null) {
            restApiInterceptor.deleteCaseDefinitionIdentityLink(caseDefinition, link);
        }

        if (link.getUserId() != null) {
            repositoryService.deleteCandidateStarterUser(caseDefinition.getId(), link.getUserId());
        } else {
            repositoryService.deleteCandidateStarterGroup(caseDefinition.getId(), link.getGroupId());
        }
    }

    protected void validateIdentityLinkArguments(String family, String identityId) {
        if (family == null || (!CmmnRestUrls.SEGMENT_IDENTITYLINKS_FAMILY_GROUPS.equals(family) && !CmmnRestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS.equals(family))) {
            throw new FlowableIllegalArgumentException("Identity link family should be 'users' or 'groups'.");
        }
        if (identityId == null) {
            throw new FlowableIllegalArgumentException("IdentityId is required.");
        }
    }

    protected IdentityLink getIdentityLink(String family, String identityId, String caseDefinitionId) {
        boolean isUser = family.equals(CmmnRestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS);

        List<IdentityLink> allLinks = repositoryService.getIdentityLinksForCaseDefinition(caseDefinitionId);
        for (IdentityLink link : allLinks) {
            boolean rightIdentity = false;
            if (isUser) {
                rightIdentity = identityId.equals(link.getUserId());
            } else {
                rightIdentity = identityId.equals(link.getGroupId());
            }

            if (rightIdentity && link.getType().equals(IdentityLinkType.CANDIDATE)) {
                return link;
            }
        }
        throw new FlowableObjectNotFoundException("Could not find the requested identity link.", IdentityLink.class);
    }
}
