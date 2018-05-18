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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.rest.service.api.engine.RestIdentityLink;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class ProcessDefinitionIdentityLinkResource extends BaseProcessDefinitionResource {

    @ApiOperation(value = "Get a candidate starter from a process definition", tags = { "Process Definitions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the process definition was found and the identity link was returned."),
            @ApiResponse(code = 404, message = "Indicates the requested process definition was not found or the process definition doesn’t have an identity-link that matches the url.")
    })
    @GetMapping(value = "/repository/process-definitions/{processDefinitionId}/identitylinks/{family}/{identityId}", produces = "application/json")
    public RestIdentityLink getIdentityLink(@ApiParam(name = "processDefinitionId") @PathVariable("processDefinitionId") String processDefinitionId,
            @ApiParam(name = "family") @PathVariable("family") String family, @ApiParam(name = "identityId") @PathVariable("identityId") String identityId,
            HttpServletRequest request) {

        ProcessDefinition processDefinition = getProcessDefinitionFromRequest(processDefinitionId);

        validateIdentityLinkArguments(family, identityId);

        // Check if identitylink to get exists
        IdentityLink link = getIdentityLink(family, identityId, processDefinition.getId());

        return restResponseFactory.createRestIdentityLink(link);
    }

    @ApiOperation(value = "Delete a candidate starter from a process definition", tags = { "Process Definitions" })
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the process definition was found and the identity link was removed. The response body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested process definition was not found or the process definition doesn’t have an identity-link that matches the url.")
    })
    @DeleteMapping(value = "/repository/process-definitions/{processDefinitionId}/identitylinks/{family}/{identityId}")
    public void deleteIdentityLink(@ApiParam(name = "processDefinitionId") @PathVariable("processDefinitionId") String processDefinitionId,
            @ApiParam(name = "family") @PathVariable("family") String family, @ApiParam(name = "identityId") @PathVariable("identityId") String identityId,
            HttpServletResponse response) {

        ProcessDefinition processDefinition = getProcessDefinitionFromRequest(processDefinitionId);

        validateIdentityLinkArguments(family, identityId);

        // Check if identitylink to delete exists
        IdentityLink link = getIdentityLink(family, identityId, processDefinition.getId());
        if (link.getUserId() != null) {
            repositoryService.deleteCandidateStarterUser(processDefinition.getId(), link.getUserId());
        } else {
            repositoryService.deleteCandidateStarterGroup(processDefinition.getId(), link.getGroupId());
        }

        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    protected void validateIdentityLinkArguments(String family, String identityId) {
        if (family == null || (!RestUrls.SEGMENT_IDENTITYLINKS_FAMILY_GROUPS.equals(family) && !RestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS.equals(family))) {
            throw new FlowableIllegalArgumentException("Identity link family should be 'users' or 'groups'.");
        }
        if (identityId == null) {
            throw new FlowableIllegalArgumentException("IdentityId is required.");
        }
    }

    protected IdentityLink getIdentityLink(String family, String identityId, String processDefinitionId) {
        boolean isUser = family.equals(RestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS);

        // Perhaps it would be better to offer getting a single identitylink
        // from
        // the API
        List<IdentityLink> allLinks = repositoryService.getIdentityLinksForProcessDefinition(processDefinitionId);
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
