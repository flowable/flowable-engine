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
package org.flowable.ui.idm.rest.app;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.flowable.idm.api.Group;
import org.flowable.ui.common.model.GroupRepresentation;
import org.flowable.ui.common.model.UserRepresentation;
import org.flowable.ui.common.rest.idm.CurrentUserProvider;
import org.flowable.ui.common.service.exception.NotFoundException;
import org.flowable.ui.common.service.exception.UnauthorizedException;
import org.flowable.ui.idm.model.UserInformation;
import org.flowable.ui.idm.service.UserService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
/**
 * REST controller for managing the current user's account.
 *
 * @author Joram Barrez
 */
@RestController
public class AccountResource {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    protected final Collection<CurrentUserProvider> currentUserProviders;

    public AccountResource(ObjectProvider<CurrentUserProvider> currentUserProviders) {
        this.currentUserProviders = currentUserProviders.orderedStream().collect(Collectors.toList());
    }

    /**
     * GET /rest/authenticate -> check if the user is authenticated, and return its full name.
     */
    @GetMapping(value = "/rest/authenticate", produces = { "application/json" })
    public ObjectNode isAuthenticated(HttpServletRequest request) {
        String user = request.getRemoteUser();

        if (user == null) {
            throw new UnauthorizedException("Request did not contain valid authorization");
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.put("login", user);
        return result;
    }

    /**
     * GET /rest/account -> get the current user.
     */
    @GetMapping(value = "/rest/account", produces = "application/json")
    public UserRepresentation getAccount(Authentication authentication) {
        UserRepresentation userRepresentation = null;

        for (CurrentUserProvider userProvider : currentUserProviders) {
            if (userProvider.supports(authentication)) {
                userRepresentation = userProvider.getCurrentUser(authentication);
            }

            if (userRepresentation != null) {
                break;
            }
        }

        if (userRepresentation == null) {
            userRepresentation = getCurrentUserRepresentation(authentication.getName());
        }

        if (userRepresentation != null) {
            return userRepresentation;
        } else {
            throw new NotFoundException();
        }
    }

    protected UserRepresentation getCurrentUserRepresentation(String currentUserId) {
        UserInformation userInformation = userService.getUserInformation(currentUserId);
        if (userInformation != null) {
            UserRepresentation userRepresentation = new UserRepresentation(userInformation.getUser());
            if (userInformation.getGroups() != null) {
                for (Group group : userInformation.getGroups()) {
                    userRepresentation.getGroups().add(new GroupRepresentation(group));
                }
            }
            if (userInformation.getPrivileges() != null) {
                for (String privilege : userInformation.getPrivileges()) {
                    userRepresentation.getPrivileges().add(privilege);
                }
            }
            return userRepresentation;
        }

        return null;
    }
}
