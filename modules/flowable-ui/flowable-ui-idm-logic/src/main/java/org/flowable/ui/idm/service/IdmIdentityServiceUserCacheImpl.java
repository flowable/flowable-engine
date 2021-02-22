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
package org.flowable.ui.idm.service;

import java.util.ArrayList;
import java.util.Collection;

import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.User;
import org.flowable.spring.boot.ldap.FlowableLdapProperties;
import org.flowable.ui.common.properties.FlowableCommonAppProperties;
import org.flowable.ui.common.service.idm.cache.BaseUserCache;
import org.flowable.ui.idm.model.UserInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.google.common.cache.LoadingCache;

/**
 * Cache containing User objects to prevent too much DB-traffic (users exist separately from the Flowable tables, they need to be fetched afterward one by one to join with those entities).
 * <p>
 * TODO: This could probably be made more efficient with bulk getting. The Google cache impl allows this: override loadAll and use getAll() to fetch multiple entities.
 *
 * @author Frederik Heremans
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
@Service
public class IdmIdentityServiceUserCacheImpl extends BaseUserCache {

    protected FlowableLdapProperties ldapProperties;

    protected final IdmIdentityService identityService;

    protected final UserService userService;

    protected LoadingCache<String, CachedUser> userCache;

    public IdmIdentityServiceUserCacheImpl(FlowableCommonAppProperties properties, IdmIdentityService identityService, UserService userService) {
        super(properties);
        this.identityService = identityService;
        this.userService = userService;
    }

    @Override
    protected CachedUser loadUser(String userId) {
        User userFromDatabase = null;
        if (ldapProperties == null || !ldapProperties.isEnabled()) {
            userFromDatabase = identityService.createUserQuery().userIdIgnoreCase(userId.toLowerCase()).singleResult();
        } else {
            userFromDatabase = identityService.createUserQuery().userId(userId).singleResult();
        }

        if (userFromDatabase == null) {
            throw new UsernameNotFoundException("User " + userId + " was not found in the database");
        }

        Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        UserInformation userInformation = userService.getUserInformation(userFromDatabase.getId());
        for (String privilege : userInformation.getPrivileges()) {
            grantedAuthorities.add(new SimpleGrantedAuthority(privilege));
        }

        return new CachedUser(userFromDatabase, grantedAuthorities);
    }

    @Autowired(required = false)
    public void setLdapProperties(FlowableLdapProperties ldapProperties) {
        this.ldapProperties = ldapProperties;
    }
}
