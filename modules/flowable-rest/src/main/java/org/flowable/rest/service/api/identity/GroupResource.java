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

package org.flowable.rest.service.api.identity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.idm.api.Group;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
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
@Api(tags = { "Groups" }, description = "Manage Groups", authorizations = { @Authorization(value = "basicAuth") })
public class GroupResource extends BaseGroupResource {

    @ApiOperation(value = "Get a single group", tags = { "Groups" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the group exists and is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested group does not exist.")
    })
    @GetMapping(value = "/identity/groups/{groupId}", produces = "application/json")
    public GroupResponse getGroup(@ApiParam(name = "groupId") @PathVariable String groupId, HttpServletRequest request) {
        return restResponseFactory.createGroupResponse(getGroupFromRequest(groupId));
    }

    @ApiOperation(value = "Update a group", tags = {
            "Groups" }, notes = "All request values are optional. For example, you can only include the name attribute in the request body JSON-object, only updating the name of the group, leaving all other fields unaffected. When an attribute is explicitly included and is set to null, the group-value will be updated to null.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the group was updated."),
            @ApiResponse(code = 404, message = "Indicates the requested group was not found."),
            @ApiResponse(code = 409, message = "Indicates the requested group was updated simultaneously.")
    })
    @PutMapping(value = "/identity/groups/{groupId}", produces = "application/json")
    public GroupResponse updateGroup(@ApiParam(name = "groupId") @PathVariable String groupId, @RequestBody GroupRequest groupRequest, HttpServletRequest request) {
        Group group = getGroupFromRequest(groupId);

        if (groupRequest.getId() == null || groupRequest.getId().equals(group.getId())) {
            if (groupRequest.isNameChanged()) {
                group.setName(groupRequest.getName());
            }
            if (groupRequest.isTypeChanged()) {
                group.setType(groupRequest.getType());
            }
            identityService.saveGroup(group);
        } else {
            throw new FlowableIllegalArgumentException("Key provided in request body doesn't match the key in the resource URL.");
        }

        return restResponseFactory.createGroupResponse(group);
    }

    @ApiOperation(value = "Delete a group", tags = { "Groups" })
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the group was found and  has been deleted. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested group does not exist.")
    })
    @DeleteMapping("/identity/groups/{groupId}")
    public void deleteGroup(@ApiParam(name = "groupId") @PathVariable String groupId, HttpServletResponse response) {
        Group group = getGroupFromRequest(groupId);
        identityService.deleteGroup(group.getId());
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }
}
