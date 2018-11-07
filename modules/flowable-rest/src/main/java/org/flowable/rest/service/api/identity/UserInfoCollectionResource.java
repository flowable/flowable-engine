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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.rest.exception.FlowableConflictException;
import org.flowable.engine.IdentityService;
import org.flowable.idm.api.User;
import org.flowable.rest.service.api.RestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Users" }, description = "Manage Users", authorizations = { @Authorization(value = "basicAuth") })
public class UserInfoCollectionResource extends BaseUserResource {

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected IdentityService identityService;

    @ApiOperation(value = "List user’s info", tags = { "Users" }, nickname = "listUserInfo")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the user was found and list of info (key and url) is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested user was not found.")
    })
    @GetMapping(value = "/identity/users/{userId}/info", produces = "application/json")
    public List<UserInfoResponse> getUserInfo(@ApiParam(name = "userId") @PathVariable String userId, HttpServletRequest request) {
        User user = getUserFromRequest(userId);

        return restResponseFactory.createUserInfoKeysResponse(identityService.getUserInfoKeys(user.getId()), user.getId());
    }

    @ApiOperation(value = "Create a new user’s info entry", tags = { "Users" }, nickname = "createUserInfo")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the user was found and the info has been created."),
            @ApiResponse(code = 400, message = "Indicates the key or value was missing from the request body. Status description contains additional information about the error."),
            @ApiResponse(code = 404, message = "Indicates the requested user was not found."),
            @ApiResponse(code = 409, message = "Indicates there is already an info-entry with the given key for the user, update the resource instance (PUT).")
    })
    @PostMapping(value = "/identity/users/{userId}/info", produces = "application/json")
    public UserInfoResponse setUserInfo(@ApiParam(name = "userId") @PathVariable String userId, @RequestBody UserInfoRequest userRequest, HttpServletRequest request, HttpServletResponse response) {

        User user = getUserFromRequest(userId);

        if (userRequest.getKey() == null) {
            throw new FlowableIllegalArgumentException("The key cannot be null.");
        }
        if (userRequest.getValue() == null) {
            throw new FlowableIllegalArgumentException("The value cannot be null.");
        }

        String existingValue = identityService.getUserInfo(user.getId(), userRequest.getKey());
        if (existingValue != null) {
            throw new FlowableConflictException("User info with key '" + userRequest.getKey() + "' already exists for this user.");
        }

        identityService.setUserInfo(user.getId(), userRequest.getKey(), userRequest.getValue());

        response.setStatus(HttpStatus.CREATED.value());
        return restResponseFactory.createUserInfoResponse(userRequest.getKey(), userRequest.getValue(), user.getId());
    }
}
