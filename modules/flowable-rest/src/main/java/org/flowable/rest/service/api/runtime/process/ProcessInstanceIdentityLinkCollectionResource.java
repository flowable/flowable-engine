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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.rest.service.api.engine.RestIdentityLink;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
@Api(tags = { "Process Instance Identity Links" }, description = "Manage Process Instances Identity Links", authorizations = { @Authorization(value = "basicAuth") })
public class ProcessInstanceIdentityLinkCollectionResource extends BaseProcessInstanceResource {

    @ApiOperation(value = "Get involved people for process instance", tags = {"Process Instance Identity Links" }, nickname = "listProcessInstanceIdentityLinks",
            notes = "Note that the groupId in Response Body will always be null, as it’s only possible to involve users with a process-instance.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the process instance was found and links are returned."),
            @ApiResponse(code = 404, message = "Indicates the requested process instance was not found.")
    })
    @GetMapping(value = "/runtime/process-instances/{processInstanceId}/identitylinks", produces = "application/json")
    public List<RestIdentityLink> getIdentityLinks(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId, HttpServletRequest request) {
        ProcessInstance processInstance = getProcessInstanceFromRequest(processInstanceId);
        return restResponseFactory.createRestIdentityLinks(runtimeService.getIdentityLinksForProcessInstance(processInstance.getId()));
    }

    @ApiOperation(value = "Add an involved user to a process instance", tags = {"Process Instance Identity Links" }, nickname = "createProcessInstanceIdentityLinks",
            notes = "Note that the groupId in Response Body will always be null, as it’s only possible to involve users with a process-instance.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the process instance was found and the link is created."),
            @ApiResponse(code = 400, message = "Indicates the requested body did not contain a userId or a type."),
            @ApiResponse(code = 404, message = "Indicates the requested process instance was not found.")
    })
    @PostMapping(value = "/runtime/process-instances/{processInstanceId}/identitylinks", produces = "application/json")
    public RestIdentityLink createIdentityLink(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId, @RequestBody RestIdentityLink identityLink, HttpServletRequest request, HttpServletResponse response) {

        ProcessInstance processInstance = getProcessInstanceFromRequest(processInstanceId);

        if (identityLink.getGroup() != null) {
            throw new FlowableIllegalArgumentException("Only user identity links are supported on a process instance.");
        }

        if (identityLink.getUser() == null) {
            throw new FlowableIllegalArgumentException("The user is required.");
        }

        if (identityLink.getType() == null) {
            throw new FlowableIllegalArgumentException("The identity link type is required.");
        }

        runtimeService.addUserIdentityLink(processInstance.getId(), identityLink.getUser(), identityLink.getType());

        response.setStatus(HttpStatus.CREATED.value());

        return restResponseFactory.createRestIdentityLink(identityLink.getType(), identityLink.getUser(), identityLink.getGroup(), null, null, processInstance.getId());
    }
}
