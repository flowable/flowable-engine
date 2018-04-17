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
import java.util.Collection;

import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.User;
import org.flowable.spring.boot.ldap.FlowableLdapProperties;
import org.flowable.ui.common.security.FlowableAppUser;
import org.flowable.ui.idm.cache.UserCache;
import org.flowable.ui.idm.cache.UserCache.CachedUser;
import org.flowable.ui.idm.model.UserInformation;
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
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService, CustomUserDetailService {

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

        Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        UserInformation userInformation = userService.getUserInformation(userFromDatabase.getId());
        for (String privilege : userInformation.getPrivileges()) {
            grantedAuthorities.add(new SimpleGrantedAuthority(privilege));
        }

        userCache.putUser(userFromDatabase.getId(), new CachedUser(userFromDatabase, grantedAuthorities));
        return new FlowableAppUser(userFromDatabase, actualLogin, grantedAuthorities);
    }

    @Transactional
    public UserDetails loadByUserId(final String userId) {
        CachedUser cachedUser = userCache.getUser(userId, true, true, false); // Do not check for validity. This would lead to A LOT of db requests! For login, there is a validity period (see below)
        if (cachedUser == null) {
            throw new UsernameNotFoundException("User " + userId + " was not found in the database");
        }

        long lastDatabaseCheck = cachedUser.getLastDatabaseCheck();
        long currentTime = System.currentTimeMillis(); // No need to create a Date object. The Date constructor simply calls this method too!

        if (userValidityPeriod <= 0L || (currentTime - lastDatabaseCheck >= userValidityPeriod)) {

            userCache.invalidate(userId);
            cachedUser = userCache.getUser(userId, true, true, false); // Fetching it again will refresh data

            cachedUser.setLastDatabaseCheck(currentTime);
        }

        // The Spring security docs clearly state a new instance must be returned on every invocation
        User user = cachedUser.getUser();
        String actualUserId = user.getId();

        return new FlowableAppUser(cachedUser.getUser(), actualUserId, cachedUser.getGrantedAuthorities());
    }

    public void setUserValidityPeriod(long userValidityPeriod) {
        this.userValidityPeriod = userValidityPeriod;
    }
}
