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

import org.flowable.idm.api.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
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
@Api(tags = { "Users" }, authorizations = { @Authorization(value = "basicAuth") })
public class UserResource extends BaseUserResource {

    @ApiOperation(value = "Get a single user", tags = { "Users" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the user exists and is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested user does not exist.")
    })
    @GetMapping(value = "/identity/users/{userId}", produces = "application/json")
    public UserResponse getUser(@ApiParam(name = "userId") @PathVariable String userId) {
        return restResponseFactory.createUserResponse(getUserFromRequest(userId), false);
    }

    @ApiOperation(value = "Update a user", tags = { "Users" }, notes = "All request values are optional. "
            + "For example, you can only include the firstName attribute in the request body JSON-object, only updating the firstName of the user, leaving all other fields unaffected. "
            + "When an attribute is explicitly included and is set to null, the user-value will be updated to null. "
            + "Example: {\"firstName\" : null} will clear the firstName of the user).")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the user was updated."),
            @ApiResponse(code = 404, message = "Indicates the requested user was not found."),
            @ApiResponse(code = 409, message = "Indicates the requested user was updated simultaneously.")
    })
    @PutMapping(value = "/identity/users/{userId}", produces = "application/json")
    public UserResponse updateUser(@ApiParam(name = "userId") @PathVariable String userId, @RequestBody UserRequest userRequest) {
        User user = getUserFromRequest(userId);
        if (userRequest.isEmailChanged()) {
            user.setEmail(userRequest.getEmail());
        }
        if (userRequest.isFirstNameChanged()) {
            user.setFirstName(userRequest.getFirstName());
        }
        if (userRequest.isDisplayNameChanged()) {
            user.setDisplayName(userRequest.getDisplayName());
        }
        if (userRequest.isLastNameChanged()) {
            user.setLastName(userRequest.getLastName());
        }
        if (userRequest.isPasswordChanged()) {
            user.setPassword(userRequest.getPassword());
            identityService.updateUserPassword(user);
        } else {
            identityService.saveUser(user);
        }

        return restResponseFactory.createUserResponse(user, false);
    }

    @ApiOperation(value = "Delete a user", tags = { "Users" }, code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the user was found and  has been deleted. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested user was not found.")
    })
    @DeleteMapping("/identity/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@ApiParam(name = "userId") @PathVariable String userId) {
        User user = getUserFromRequest(userId);
        
        if (restApiInterceptor != null) {
            restApiInterceptor.deleteUser(user);
        }
        
        identityService.deleteUser(user.getId());
    }
}
