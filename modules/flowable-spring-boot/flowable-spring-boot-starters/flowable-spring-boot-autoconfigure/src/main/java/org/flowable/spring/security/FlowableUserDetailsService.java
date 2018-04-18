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
package org.flowable.spring.security;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * {@link UserDetails} provider that uses the {@link IdmIdentityService} to load users.
 *
 * @author Josh Long
 * @author Filip Hrisafov
 */
public class FlowableUserDetailsService
        implements UserDetailsService {

    protected final IdmIdentityService identityService;

    public FlowableUserDetailsService(IdmIdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public UserDetails loadUserByUsername(String userId)
            throws UsernameNotFoundException {
        User user = null;
        try {
            user = this.identityService.createUserQuery()
                    .userId(userId)
                    .singleResult();
        } catch (FlowableException ex) {
            // don't care
        }

        if (null == user) {
            throw new UsernameNotFoundException(
                    String.format("user (%s) could not be found", userId));
        }

        return createFlowableUser(user);
    }

    protected FlowableUser createFlowableUser(User user) {

        String userId = user.getId();
        List<Privilege> userPrivileges = identityService.createPrivilegeQuery().userId(userId).list();
        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
        for (Privilege userPrivilege : userPrivileges) {
            grantedAuthorities.add(new SimpleGrantedAuthority(userPrivilege.getName()));
        }

        List<Group> groups = identityService.createGroupQuery().groupMember(userId).list();
        if (!groups.isEmpty()) {
            List<String> groupIds = new ArrayList<>(groups.size());
            for (Group group : groups) {
                groupIds.add(group.getId());
            }

            List<Privilege> groupPrivileges = identityService.createPrivilegeQuery().groupIds(groupIds).list();
            for (Privilege groupPrivilege : groupPrivileges) {
                grantedAuthorities.add(new SimpleGrantedAuthority(groupPrivilege.getName()));
            }
        }

        return new FlowableUser(user, true, groups, userPrivileges, grantedAuthorities);
    }
}
