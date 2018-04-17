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

package org.flowable.rest.service.api.runtime.task;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.rest.service.api.engine.RestIdentityLink;
import org.flowable.task.api.Task;
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
@Api(tags = { "Task Identity Links" }, description = "Manage Tasks", authorizations = { @Authorization(value = "basicAuth") })
public class TaskIdentityLinkResource extends TaskBaseResource {

    @ApiOperation(value = "Get a single identity link on a task", tags = { "Task Identity Links" }, nickname = "getTaskInstanceIdentityLinks")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the task and identity link was found and returned."),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found or the task doesn’t have the requested identityLink. The status contains additional information about this error.")
    })
    @GetMapping(value = "/runtime/tasks/{taskId}/identitylinks/{family}/{identityId}/{type}", produces = "application/json")
    public RestIdentityLink getIdentityLink(@ApiParam(name = "taskId") @PathVariable("taskId") String taskId, @ApiParam(name = "family") @PathVariable("family") String family,
            @ApiParam(name = "identityId") @PathVariable("identityId") String identityId,
            @ApiParam(name = "type") @PathVariable("type") String type, HttpServletRequest request) {

        Task task = getTaskFromRequest(taskId);
        validateIdentityLinkArguments(family, identityId, type);

        IdentityLink link = getIdentityLink(family, identityId, type, task.getId());

        return restResponseFactory.createRestIdentityLink(link);
    }

    @ApiOperation(value = "Delete an identity link on a task", tags = { "Task Identity Links" }, nickname = "deleteTaskInstanceIdentityLinks")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the task and identity link were found and the link has been deleted. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found or the task doesn’t have the requested identityLink. The status contains additional information about this error.")
    })
    @DeleteMapping(value = "/runtime/tasks/{taskId}/identitylinks/{family}/{identityId}/{type}")
    public void deleteIdentityLink(@ApiParam(name = "taskId") @PathVariable("taskId") String taskId, @ApiParam(name = "family") @PathVariable("family") String family, @ApiParam(name = "identityId") @PathVariable("identityId") String identityId,
            @ApiParam(name = "type") @PathVariable("type") String type,
            HttpServletResponse response) {

        Task task = getTaskFromRequest(taskId);

        validateIdentityLinkArguments(family, identityId, type);

        // Check if identitylink to delete exists
        getIdentityLink(family, identityId, type, task.getId());

        if (RestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS.equals(family)) {
            taskService.deleteUserIdentityLink(task.getId(), identityId, type);
        } else {
            taskService.deleteGroupIdentityLink(task.getId(), identityId, type);
        }

        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    protected void validateIdentityLinkArguments(String family, String identityId, String type) {
        if (family == null || (!RestUrls.SEGMENT_IDENTITYLINKS_FAMILY_GROUPS.equals(family) && !RestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS.equals(family))) {
            throw new FlowableIllegalArgumentException("Identity link family should be 'users' or 'groups'.");
        }
        if (identityId == null) {
            throw new FlowableIllegalArgumentException("IdentityId is required.");
        }
        if (type == null) {
            throw new FlowableIllegalArgumentException("Type is required.");
        }
    }

    protected IdentityLink getIdentityLink(String family, String identityId, String type, String taskId) {
        boolean isUser = family.equals(RestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS);

        // Perhaps it would be better to offer getting a single identitylink
        // from the API
        List<IdentityLink> allLinks = taskService.getIdentityLinksForTask(taskId);
        for (IdentityLink link : allLinks) {
            boolean rightIdentity = false;
            if (isUser) {
                rightIdentity = identityId.equals(link.getUserId());
            } else {
                rightIdentity = identityId.equals(link.getGroupId());
            }

            if (rightIdentity && link.getType().equals(type)) {
                return link;
            }
        }
        throw new FlowableObjectNotFoundException("Could not find the requested identity link.", IdentityLink.class);
    }
}
