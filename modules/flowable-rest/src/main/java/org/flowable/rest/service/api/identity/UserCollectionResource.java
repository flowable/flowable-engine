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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.common.rest.exception.FlowableConflictException;
import org.flowable.engine.IdentityService;
import org.flowable.idm.api.User;
import org.flowable.idm.api.UserQuery;
import org.flowable.idm.api.UserQueryProperty;
import org.flowable.rest.service.api.BpmnRestApiInterceptor;
import org.flowable.rest.service.api.RestResponseFactory;
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
 * @author Filip Hrisafov
 */
@RestController
@Api(tags = { "Users" }, description = "Manage Users", authorizations = { @Authorization(value = "basicAuth") })
public class UserCollectionResource {

    protected static HashMap<String, QueryProperty> properties = new HashMap<>();

    static {
        properties.put("id", UserQueryProperty.USER_ID);
        properties.put("firstName", UserQueryProperty.FIRST_NAME);
        properties.put("lastName", UserQueryProperty.LAST_NAME);
        properties.put("displayName", UserQueryProperty.DISPLAY_NAME);
        properties.put("email", UserQueryProperty.EMAIL);
    }

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected IdentityService identityService;
    
    @Autowired(required=false)
    protected BpmnRestApiInterceptor restApiInterceptor;

    @ApiOperation(value = "List users", nickname = "listUsers", tags = { "Users" })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "string", value = "Only return group with the given id", paramType = "query"),
            @ApiImplicitParam(name = "firstName", dataType = "string", value = "Only return users with the given firstname", paramType = "query"),
            @ApiImplicitParam(name = "lastName", dataType = "string", value = "Only return users with the given lastname", paramType = "query"),
            @ApiImplicitParam(name = "displayName", dataType = "string", value = "Only return users with the given displayName", paramType = "query"),
            @ApiImplicitParam(name = "email", dataType = "string", value = "Only return users with the given email", paramType = "query"),
            @ApiImplicitParam(name = "firstNameLike", dataType = "string", value = "Only return userswith a firstname like the given value. Use % as wildcard-character.", paramType = "query"),
            @ApiImplicitParam(name = "lastNameLike", dataType = "string", value = "Only return users with a lastname like the given value. Use % as wildcard-character.", paramType = "query"),
            @ApiImplicitParam(name = "displayNameLike", dataType = "string", value = "Only return users with a displayName like the given value. Use % as wildcard-character.", paramType = "query"),
            @ApiImplicitParam(name = "emailLike", dataType = "string", value = "Only return users with an email like the given value. Use % as wildcard-character.", paramType = "query"),
            @ApiImplicitParam(name = "memberOfGroup", dataType = "string", value = "Only return users which are a member of the given group.", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return users which are a members of the given tenant.", paramType = "query"),
            @ApiImplicitParam(name = "potentialStarter", dataType = "string", value = "Only return users  which members are potential starters for a process-definition with the given id.", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues = "id,firstName,lastname,email,displayName", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the group exists and is returned.")
    })
    @GetMapping(value = "/identity/users", produces = "application/json")
    public DataResponse<UserResponse> getUsers(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        UserQuery query = identityService.createUserQuery();

        if (allRequestParams.containsKey("id")) {
            query.userId(allRequestParams.get("id"));
        }
        if (allRequestParams.containsKey("firstName")) {
            query.userFirstName(allRequestParams.get("firstName"));
        }
        if (allRequestParams.containsKey("lastName")) {
            query.userLastName(allRequestParams.get("lastName"));
        }
        if (allRequestParams.containsKey("displayName")) {
            query.userDisplayName(allRequestParams.get("displayName"));
        }
        if (allRequestParams.containsKey("email")) {
            query.userEmail(allRequestParams.get("email"));
        }
        if (allRequestParams.containsKey("firstNameLike")) {
            query.userFirstNameLike(allRequestParams.get("firstNameLike"));
        }
        if (allRequestParams.containsKey("lastNameLike")) {
            query.userLastNameLike(allRequestParams.get("lastNameLike"));
        }
        if (allRequestParams.containsKey("displayNameLike")) {
            query.userDisplayNameLike(allRequestParams.get("displayNameLike"));
        }
        if (allRequestParams.containsKey("emailLike")) {
            query.userEmailLike(allRequestParams.get("emailLike"));
        }
        if (allRequestParams.containsKey("memberOfGroup")) {
            query.memberOfGroup(allRequestParams.get("memberOfGroup"));
        }
        if (allRequestParams.containsKey("tenantId")) {
            query.tenantId(allRequestParams.get("tenantId"));
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessUserInfoWithQuery(query);
        }

        return new UserPaginateList(restResponseFactory).paginateList(allRequestParams, query, "id", properties);
    }

    @ApiOperation(value = "Create a user", tags = { "Users" })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the user was created."),
            @ApiResponse(code = 400, message = "Indicates the id of the user was missing.")

    })
    @PostMapping(value = "/identity/users", produces = "application/json")
    public UserResponse createUser(@RequestBody UserRequest userRequest, HttpServletRequest request, HttpServletResponse response) {
        if (userRequest.getId() == null) {
            throw new FlowableIllegalArgumentException("Id cannot be null.");
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.createUser(userRequest);
        }

        // Check if a user with the given ID already exists so we return a CONFLICT
        if (identityService.createUserQuery().userId(userRequest.getId()).count() > 0) {
            throw new FlowableConflictException("A user with id '" + userRequest.getId() + "' already exists.");
        }

        User created = identityService.newUser(userRequest.getId());
        created.setEmail(userRequest.getEmail());
        created.setFirstName(userRequest.getFirstName());
        created.setLastName(userRequest.getLastName());
        created.setDisplayName(userRequest.getDisplayName());
        created.setPassword(userRequest.getPassword());
        created.setTenantId(userRequest.getTenantId());
        identityService.saveUser(created);

        response.setStatus(HttpStatus.CREATED.value());

        return restResponseFactory.createUserResponse(created, true);
    }

}
