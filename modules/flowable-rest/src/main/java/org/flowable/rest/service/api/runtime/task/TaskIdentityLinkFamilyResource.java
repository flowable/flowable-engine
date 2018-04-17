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
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.rest.service.api.engine.RestIdentityLink;
import org.flowable.task.api.Task;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Task Identity Links" }, description = "Manage Tasks", authorizations = { @Authorization(value = "basicAuth") })
public class TaskIdentityLinkFamilyResource extends TaskBaseResource {

    @ApiOperation(value = "List identity links for a task for either groups or users", tags = { "Task Identity Links" },  nickname = "listIdentityLinksForFamily",
            notes = "Returns only identity links targetting either users or groups. Response body and status-codes are exactly the same as when getting the full list of identity links for a task.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the task was found and the requested identity links are returned."),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found.")
    })
    @GetMapping(value = "/runtime/tasks/{taskId}/identitylinks/{family}", produces = "application/json")
    public List<RestIdentityLink> getIdentityLinksForFamily(@ApiParam(name = "taskId") @PathVariable("taskId") String taskId, @ApiParam(name = "family") @PathVariable("family") String family, HttpServletRequest request) {

        Task task = getTaskFromRequest(taskId);

        if (family == null || (!RestUrls.SEGMENT_IDENTITYLINKS_FAMILY_GROUPS.equals(family) && !RestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS.equals(family))) {
            throw new FlowableIllegalArgumentException("Identity link family should be 'users' or 'groups'.");
        }

        boolean isUser = family.equals(RestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS);
        List<RestIdentityLink> results = new ArrayList<>();

        List<IdentityLink> allLinks = taskService.getIdentityLinksForTask(task.getId());
        for (IdentityLink link : allLinks) {
            boolean match = false;
            if (isUser) {
                match = link.getUserId() != null;
            } else {
                match = link.getGroupId() != null;
            }

            if (match) {
                results.add(restResponseFactory.createRestIdentityLink(link));
            }
        }
        return results;
    }
}
