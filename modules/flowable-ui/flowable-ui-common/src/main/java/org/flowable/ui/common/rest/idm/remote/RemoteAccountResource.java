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
package org.flowable.ui.common.rest.idm.remote;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.User;
import org.flowable.ui.common.model.GroupRepresentation;
import org.flowable.ui.common.model.RemoteGroup;
import org.flowable.ui.common.model.RemoteUser;
import org.flowable.ui.common.model.UserRepresentation;
import org.flowable.ui.common.rest.idm.CurrentUserProvider;
import org.flowable.ui.common.service.exception.NotFoundException;
import org.flowable.ui.common.service.idm.RemoteIdmService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({
        "/app",
        "/"
})
public class RemoteAccountResource implements InitializingBean {

    protected final Collection<CurrentUserProvider> currentUserProviders;

    @Autowired(required = false)
    private RemoteIdmService remoteIdmService;

    @Autowired(required = false)
    private IdmIdentityService identityService;

    @Override
    public void afterPropertiesSet() {
        if (remoteIdmService == null && identityService == null) {
            throw new FlowableIllegalStateException("No remoteIdmService or identityService have been provided");
        }
    }

    public RemoteAccountResource(ObjectProvider<CurrentUserProvider> currentUserProviders) {
        this.currentUserProviders = currentUserProviders.orderedStream().collect(Collectors.toList());
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
        UserRepresentation userRepresentation = null;
        if (remoteIdmService != null) {
            RemoteUser remoteUser = remoteIdmService.getUser(currentUserId);
            if (remoteUser != null) {
                userRepresentation = new UserRepresentation(remoteUser);

                if (remoteUser.getGroups() != null && remoteUser.getGroups().size() > 0) {
                    List<GroupRepresentation> groups = new ArrayList<>();
                    for (RemoteGroup remoteGroup : remoteUser.getGroups()) {
                        groups.add(new GroupRepresentation(remoteGroup));
                    }
                    userRepresentation.setGroups(groups);
                }

                if (remoteUser.getPrivileges() != null && remoteUser.getPrivileges().size() > 0) {
                    userRepresentation.setPrivileges(remoteUser.getPrivileges());
                }

            }
        } else {
            User user = identityService.createUserQuery().userId(currentUserId).singleResult();
            if (user != null) {
                userRepresentation = new UserRepresentation(user);

                List<Group> userGroups = identityService.createGroupQuery().groupMember(currentUserId).list();
                if (!userGroups.isEmpty()) {
                    List<GroupRepresentation> groups = new ArrayList<>(userGroups.size());
                    for (Group userGroup : userGroups) {
                        groups.add(new GroupRepresentation(userGroup));
                    }
                    userRepresentation.setGroups(groups);
                }

                List<Privilege> userPrivileges = identityService.createPrivilegeQuery().userId(currentUserId).list();
                if (!userPrivileges.isEmpty()) {
                    userRepresentation.setPrivileges(userPrivileges.stream().map(Privilege::getName).collect(Collectors.toList()));
                }
            }
        }
        return userRepresentation;
    }

}
