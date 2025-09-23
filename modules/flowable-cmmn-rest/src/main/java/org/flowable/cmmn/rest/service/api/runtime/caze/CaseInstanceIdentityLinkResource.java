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

package org.flowable.cmmn.rest.service.api.runtime.caze;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.rest.service.api.engine.RestIdentityLink;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.identitylink.api.IdentityLink;
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
@Api(tags = { "Case Instance Identity Links" }, authorizations = { @Authorization(value = "basicAuth") })
public class CaseInstanceIdentityLinkResource extends BaseCaseInstanceResource {


    @ApiOperation(value = "Get a specific involved people from case instance", tags = { "Case Instance Identity Links" }, nickname = "getCaseInstanceIdentityLinks")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the case instance was found and the specified link is retrieved."),
            @ApiResponse(code = 404, message = "Indicates the requested case instance was not found or the link to delete does not exist. The response status contains additional information about the error.")
    })
    @GetMapping(value = "/cmmn-runtime/case-instances/{caseInstanceId}/identitylinks/users/{identityId}/{type}", produces = "application/json")
    public RestIdentityLink getIdentityLinkRequest(@ApiParam(name = "caseInstanceId") @PathVariable("caseInstanceId") String caseInstanceId, @ApiParam(name = "identityId") @PathVariable("identityId") String identityId,
            @ApiParam(name = "type") @PathVariable("type") String type) {

        CaseInstance caseInstance = getCaseInstanceFromRequestWithoutAccessCheck(caseInstanceId);

        validateIdentityLinkArguments(identityId, type);

        IdentityLink link = getIdentityLink(identityId, type, caseInstance.getId());

        if (restApiInterceptor != null) {
            restApiInterceptor.accessCaseInstanceIdentityLink(caseInstance, link);
        }

        return restResponseFactory.createRestIdentityLink(link);
    }

    @ApiOperation(value = "Remove an involved user to from case instance", tags = { "Case Instance Identity Links" }, nickname = "deleteCaseInstanceIdentityLinks", code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the case instance was found and the link has been deleted. Response body is left empty intentionally."),
            @ApiResponse(code = 404, message = "Indicates the requested case instance was not found or the link to delete does not exist. The response status contains additional information about the error.")
    })
    @DeleteMapping(value = "/cmmn-runtime/case-instances/{caseInstanceId}/identitylinks/users/{identityId}/{type}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIdentityLink(@ApiParam(name = "caseInstanceId") @PathVariable("caseInstanceId") String caseInstanceId, @ApiParam(name = "identityId") @PathVariable("identityId") String identityId,
            @ApiParam(name = "type") @PathVariable("type") String type) {

        CaseInstance caseInstance = getCaseInstanceFromRequestWithoutAccessCheck(caseInstanceId);

        validateIdentityLinkArguments(identityId, type);

        IdentityLink link = getIdentityLink(identityId, type, caseInstance.getId());

        if (restApiInterceptor != null) {
            restApiInterceptor.deleteCaseInstanceIdentityLink(caseInstance, link);
        }

        runtimeService.deleteUserIdentityLink(caseInstance.getId(), identityId, type);
    }

    protected void validateIdentityLinkArguments(String identityId, String type) {
        if (identityId == null) {
            throw new FlowableIllegalArgumentException("IdentityId is required.");
        }
        if (type == null) {
            throw new FlowableIllegalArgumentException("Type is required.");
        }
    }

    protected IdentityLink getIdentityLink(String identityId, String type, String caseInstanceId) {
        // Perhaps it would be better to offer getting a single identity link
        // from the API
        List<IdentityLink> allLinks = runtimeService.getIdentityLinksForCaseInstance(caseInstanceId);
        for (IdentityLink link : allLinks) {
            if (identityId.equals(link.getUserId()) && link.getType().equals(type)) {
                return link;
            }
        }
        throw new FlowableObjectNotFoundException("Could not find the requested identity link.", IdentityLink.class);
    }
}
