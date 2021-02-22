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
package org.flowable.ui.idm.security;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.User;
import org.flowable.spring.boot.ldap.FlowableLdapProperties;
import org.flowable.ui.common.security.SecurityUtils;
import org.flowable.ui.common.service.idm.cache.UserCache;
import org.flowable.ui.idm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is called AFTER successful authentication, to populate the user object with additional details The default (no ldap) way of authentication is a bit hidden in Spring Security magic. But
 * basically, the user object is fetched from the db and the hashed password is compared with the hash of the provided password (using the Spring {@link StandardPasswordEncoder}).
 */
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    @Autowired
    protected UserCache userCache;

    @Autowired
    protected IdmIdentityService identityService;

    @Autowired
    protected UserService userService;

    @Autowired(required = false)
    protected FlowableLdapProperties ldapProperties;

    protected long userValidityPeriod;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(final String login) {

        // This method is only called during the login.
        // All subsequent calls use the method with the long userId as parameter.
        // (Hence why the cache is NOT used here, but it is used in the loadByUserId)

        String actualLogin = login;
        User userFromDatabase = null;

        if (ldapProperties == null || !ldapProperties.isEnabled()) {
            actualLogin = login.toLowerCase();
            userFromDatabase = identityService.createUserQuery().userIdIgnoreCase(actualLogin).singleResult();

        } else {
            userFromDatabase = identityService.createUserQuery().userId(actualLogin).singleResult();
        }

        // Verify user
        if (userFromDatabase == null) {
            throw new UsernameNotFoundException("User " + actualLogin + " was not found in the database");
        }

        String userId = userFromDatabase.getId();
        List<Privilege> userPrivileges = identityService.createPrivilegeQuery().userId(userId).list();
        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
        for (Privilege userPrivilege : userPrivileges) {
            grantedAuthorities.add(new SimpleGrantedAuthority(userPrivilege.getName()));
        }

        List<Group> groups = identityService.createGroupQuery().groupMember(userId).list();
        if (!groups.isEmpty()) {
            List<String> groupIds = new ArrayList<>(groups.size());
            for (Group group : groups) {
                grantedAuthorities.add(SecurityUtils.createGroupAuthority(group.getId()));
                groupIds.add(group.getId());
            }

            List<Privilege> groupPrivileges = identityService.createPrivilegeQuery().groupIds(groupIds).list();
            for (Privilege groupPrivilege : groupPrivileges) {
                grantedAuthorities.add(new SimpleGrantedAuthority(groupPrivilege.getName()));
            }
        }

        if (StringUtils.isNotBlank(userFromDatabase.getTenantId())) {
            grantedAuthorities.add(SecurityUtils.createTenantAuthority(userFromDatabase.getTenantId()));
        }

        userCache.putUser(userFromDatabase.getId(), new UserCache.CachedUser(userFromDatabase, grantedAuthorities));
        return org.springframework.security.core.userdetails.User.withUsername(userId)
                .password(StringUtils.defaultIfBlank(userFromDatabase.getPassword(), ""))
                .authorities(grantedAuthorities)
                .build();
    }

    public void setUserValidityPeriod(long userValidityPeriod) {
        this.userValidityPeriod = userValidityPeriod;
    }
}
