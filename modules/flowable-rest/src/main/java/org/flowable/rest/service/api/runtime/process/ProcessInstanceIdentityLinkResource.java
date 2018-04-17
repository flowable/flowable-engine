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

package org.flowable.rest.service.api.runtime.process;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.rest.service.api.engine.RestIdentityLink;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Process Instance Identity Links" }, description = "Manage Process Instances Identity Links", authorizations = { @Authorization(value = "basicAuth") })
public class ProcessInstanceIdentityLinkResource extends BaseProcessInstanceResource {


    @ApiOperation(value = "Get a specific involved people from process instance", tags = { "Process Instance Identity Links" }, nickname = "getProcessInstanceIdentityLinks")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the process instance was found and the specified link is retrieved."),
            @ApiResponse(code = 404, message = "Indicates the requested process instance was not found or the link to delete doesn’t exist. The response status contains additional information about the error.")
    })
    @GetMapping(value = "/runtime/process-instances/{processInstanceId}/identitylinks/users/{identityId}/{type}", produces = "application/json")
    public RestIdentityLink getIdentityLink(@ApiParam(name = "processInstanceId") @PathVariable("processInstanceId") String processInstanceId, @ApiParam(name = "identityId") @PathVariable("identityId") String identityId,
            @ApiParam(name = "type") @PathVariable("type") String type,
            HttpServletRequest request) {

        ProcessInstance processInstance = getProcessInstanceFromRequest(processInstanceId);

        validateIdentityLinkArguments(identityId, type);

        IdentityLink link = getIdentityLink(identityId, type, processInstance.getId());
        return restResponseFactory.createRestIdentityLink(link);
    }

    @ApiOperation(value = "Remove an involved user to from process instance", tags = { "Process Instance Identity Links" }, nickname = "deleteProcessInstanceIdentityLinks")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the process instance was found and the link has been deleted. Response body is left empty intentionally."),
            @ApiResponse(code = 404, message = "Indicates the requested process instance was not found or the link to delete doesn’t exist. The response status contains additional information about the error.")
    })
    @DeleteMapping(value = "/runtime/process-instances/{processInstanceId}/identitylinks/users/{identityId}/{type}")
    public void deleteIdentityLink(@ApiParam(name = "processInstanceId") @PathVariable("processInstanceId") String processInstanceId, @ApiParam(name = "identityId") @PathVariable("identityId") String identityId,
            @ApiParam(name = "type") @PathVariable("type") String type,
            HttpServletResponse response) {

        ProcessInstance processInstance = getProcessInstanceFromRequest(processInstanceId);

        validateIdentityLinkArguments(identityId, type);

        getIdentityLink(identityId, type, processInstance.getId());

        runtimeService.deleteUserIdentityLink(processInstance.getId(), identityId, type);

        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    protected void validateIdentityLinkArguments(String identityId, String type) {
        if (identityId == null) {
            throw new FlowableIllegalArgumentException("IdentityId is required.");
        }
        if (type == null) {
            throw new FlowableIllegalArgumentException("Type is required.");
        }
    }

    protected IdentityLink getIdentityLink(String identityId, String type, String processInstanceId) {
        // Perhaps it would be better to offer getting a single identity link
        // from the API
        List<IdentityLink> allLinks = runtimeService.getIdentityLinksForProcessInstance(processInstanceId);
        for (IdentityLink link : allLinks) {
            if (identityId.equals(link.getUserId()) && link.getType().equals(type)) {
                return link;
            }
        }
        throw new FlowableObjectNotFoundException("Could not find the requested identity link.", IdentityLink.class);
    }
}
