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

package org.flowable.rest.idm.service.api.group;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.query.QueryProperty;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.GroupQuery;
import org.flowable.idm.api.GroupQueryProperty;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.rest.api.DataResponse;
import org.flowable.rest.exception.FlowableConflictException;
import org.flowable.rest.idm.service.api.IdmRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
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
public class GroupCollectionResource {

    protected static HashMap<String, QueryProperty> properties = new HashMap<>();

    static {
        properties.put("id", GroupQueryProperty.GROUP_ID);
        properties.put("name", GroupQueryProperty.NAME);
        properties.put("type", GroupQueryProperty.TYPE);
    }

    @Autowired
    protected IdmRestResponseFactory restResponseFactory;

    @Autowired
    protected IdmIdentityService identityService;

    @ApiOperation(value = "List groups", nickname="listGroups", tags = { "Groups" }, produces = "application/json")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "string", value = "Only return group with the given id", paramType = "query"),
            @ApiImplicitParam(name = "name", dataType = "string", value = "Only return groups with the given name", paramType = "query"),
            @ApiImplicitParam(name = "type", dataType = "string", value = "Only return groups with the given type", paramType = "query"),
            @ApiImplicitParam(name = "nameLike", dataType = "string", value = "Only return groups with a name like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "member", dataType = "string", value = "Only return groups which have a member with the given username.", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues = "id,name,type", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the requested groups were returned.")
    })
    @GetMapping(value = "/groups", produces = "application/json")
    public DataResponse<GroupResponse> getGroups(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams) {
        GroupQuery query = identityService.createGroupQuery();

        if (allRequestParams.containsKey("id")) {
            query.groupId(allRequestParams.get("id"));
        }
        if (allRequestParams.containsKey("name")) {
            query.groupName(allRequestParams.get("name"));
        }
        if (allRequestParams.containsKey("nameLike")) {
            query.groupNameLike(allRequestParams.get("nameLike"));
        }
        if (allRequestParams.containsKey("type")) {
            query.groupType(allRequestParams.get("type"));
        }
        if (allRequestParams.containsKey("member")) {
            query.groupMember(allRequestParams.get("member"));
        }

        return new GroupPaginateList(restResponseFactory).paginateList(allRequestParams, query, "id", properties);
    }

    @ApiOperation(value = "Create a group", tags = { "Groups" })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the group was created."),
            @ApiResponse(code = 400, message = "Indicates the id of the group was missing.")
    })
    @PostMapping(value = "/groups", produces = "application/json")
    public GroupResponse createGroup(@RequestBody GroupRequest groupRequest, HttpServletRequest httpRequest, HttpServletResponse response) {
        if (groupRequest.getId() == null) {
            throw new FlowableIllegalArgumentException("Id cannot be null.");
        }

        // Check if a user with the given ID already exists so we return a CONFLICT
        if (identityService.createGroupQuery().groupId(groupRequest.getId()).count() > 0) {
            throw new FlowableConflictException("A group with id '" + groupRequest.getId() + "' already exists.");
        }

        Group created = identityService.newGroup(groupRequest.getId());
        created.setId(groupRequest.getId());
        created.setName(groupRequest.getName());
        created.setType(groupRequest.getType());
        identityService.saveGroup(created);

        response.setStatus(HttpStatus.CREATED.value());

        return restResponseFactory.createGroupResponse(created);
    }

}
