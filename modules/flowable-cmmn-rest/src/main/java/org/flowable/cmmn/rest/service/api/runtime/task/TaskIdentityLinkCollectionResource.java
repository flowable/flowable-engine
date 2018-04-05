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

package org.flowable.cmmn.rest.service.api.runtime.task;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.cmmn.rest.service.api.engine.RestIdentityLink;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.task.api.Task;
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
@Api(tags = { "Task Identity Links" }, description = "Manage Tasks Identity Links", authorizations = { @Authorization(value = "basicAuth") })
public class TaskIdentityLinkCollectionResource extends TaskBaseResource {

    @ApiOperation(value = "List identity links for a task", tags = { "Task Identity Links" }, nickname = "listTasksInstanceIdentityLinks")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the task was found and the requested identity links are returned."),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found.")
    })
    @GetMapping(value = "/cmmn-runtime/tasks/{taskId}/identitylinks", produces = "application/json")
    public List<RestIdentityLink> getIdentityLinks(@ApiParam(name = "taskId") @PathVariable("taskId") String taskId, HttpServletRequest request) {
        Task task = getTaskFromRequest(taskId);
        return restResponseFactory.createRestIdentityLinks(taskService.getIdentityLinksForTask(task.getId()));
    }

    @ApiOperation(value = "Create an identity link on a task", tags = { "Task Identity Links" }, nickname = "createTaskInstanceIdentityLinks",
            notes = "It's possible to add either a user or a group.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the task was found and the identity link was created."),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found or the task doesn’t have the requested identityLink. The status contains additional information about this error.")
    })
    @PostMapping(value = "/cmmn-runtime/tasks/{taskId}/identitylinks", produces = "application/json")
    public RestIdentityLink createIdentityLink(@ApiParam(name = "taskId") @PathVariable("taskId") String taskId, @RequestBody RestIdentityLink identityLink, HttpServletRequest request, HttpServletResponse response) {

        Task task = getTaskFromRequest(taskId);

        if (identityLink.getGroup() == null && identityLink.getUser() == null) {
            throw new FlowableIllegalArgumentException("A group or a user is required to create an identity link.");
        }

        if (identityLink.getGroup() != null && identityLink.getUser() != null) {
            throw new FlowableIllegalArgumentException("Only one of user or group can be used to create an identity link.");
        }

        if (identityLink.getType() == null) {
            throw new FlowableIllegalArgumentException("The identity link type is required.");
        }

        if (identityLink.getGroup() != null) {
            taskService.addGroupIdentityLink(task.getId(), identityLink.getGroup(), identityLink.getType());
        } else {
            taskService.addUserIdentityLink(task.getId(), identityLink.getUser(), identityLink.getType());
        }

        response.setStatus(HttpStatus.CREATED.value());

        return restResponseFactory.createRestIdentityLink(identityLink.getType(), identityLink.getUser(), identityLink.getGroup(), task.getId(), null, null);
    }
}
