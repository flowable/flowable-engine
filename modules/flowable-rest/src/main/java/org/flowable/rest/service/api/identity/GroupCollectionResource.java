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

import static org.flowable.common.rest.api.PaginateListUtil.paginateList;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.common.rest.exception.FlowableConflictException;
import org.flowable.engine.IdentityService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.GroupQuery;
import org.flowable.idm.api.GroupQueryProperty;
import org.flowable.rest.service.api.BpmnRestApiInterceptor;
import org.flowable.rest.service.api.RestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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
@Api(tags = { "Groups" }, authorizations = { @Authorization(value = "basicAuth") })
public class GroupCollectionResource {

    protected static HashMap<String, QueryProperty> properties = new HashMap<>();

    static {
        properties.put("id", GroupQueryProperty.GROUP_ID);
        properties.put("name", GroupQueryProperty.NAME);
        properties.put("type", GroupQueryProperty.TYPE);
    }

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected IdentityService identityService;
    
    @Autowired(required=false)
    protected BpmnRestApiInterceptor restApiInterceptor;

    @ApiOperation(value = "List groups", nickname="listGroups", tags = { "Groups" }, produces = "application/json")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "string", value = "Only return group with the given id", paramType = "query"),
            @ApiImplicitParam(name = "name", dataType = "string", value = "Only return groups with the given name", paramType = "query"),
            @ApiImplicitParam(name = "type", dataType = "string", value = "Only return groups with the given type", paramType = "query"),
            @ApiImplicitParam(name = "nameLike", dataType = "string", value = "Only return groups with a name like the given value. Use % as wildcard-character.", paramType = "query"),
            @ApiImplicitParam(name = "member", dataType = "string", value = "Only return groups which have a member with the given username.", paramType = "query"),
            @ApiImplicitParam(name = "potentialStarter", dataType = "string", value = "Only return groups which members are potential starters for a process-definition with the given id.", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues = "id,name,type", paramType = "query"),
            @ApiImplicitParam(name = "order", dataType = "string", value = "The sort order, either 'asc' or 'desc'. Defaults to 'asc'.", paramType = "query"),
            @ApiImplicitParam(name = "start", dataType = "integer", value = "Index of the first row to fetch. Defaults to 0.", paramType = "query"),
            @ApiImplicitParam(name = "size", dataType = "integer", value = "Number of rows to fetch, starting from start. Defaults to 10.", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the requested groups were returned.")
    })
    @GetMapping(value = "/identity/groups", produces = "application/json")
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
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessGroupInfoWithQuery(query);
        }

        return paginateList(allRequestParams, query, "id", properties, restResponseFactory::createGroupResponseList);
    }

    @ApiOperation(value = "Create a group", tags = { "Groups" }, code = 201)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the group was created."),
            @ApiResponse(code = 400, message = "Indicates the id of the group was missing.")
    })
    @PostMapping(value = "/identity/groups", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse createGroup(@RequestBody GroupRequest groupRequest) {
        if (groupRequest.getId() == null) {
            throw new FlowableIllegalArgumentException("Id cannot be null.");
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.createGroup(groupRequest);
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

        return restResponseFactory.createGroupResponse(created);
    }

}
