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
package org.flowable.ui.common.security;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.flowable.ui.common.model.RemoteGroup;
import org.flowable.ui.common.model.RemoteUser;
import org.flowable.ui.common.service.idm.RemoteIdmService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * @author Filip Hrisafov
 */
public class RemoteIdmUserDetailsService implements UserDetailsService {

    protected final RemoteIdmService remoteIdmService;

    public RemoteIdmUserDetailsService(RemoteIdmService remoteIdmService) {
        this.remoteIdmService = remoteIdmService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        RemoteUser user = remoteIdmService.getUser(username);
        if (user == null) {
            throw new UsernameNotFoundException("user not found " + username);
        }

        Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        for (String privilege : user.getPrivileges()) {
            grantedAuthorities.add(new SimpleGrantedAuthority(privilege));
        }

        for (RemoteGroup group : user.getGroups()) {
            grantedAuthorities.add(SecurityUtils.createGroupAuthority(group.getId()));
        }

        if (StringUtils.isNotBlank(user.getTenantId())) {
            grantedAuthorities.add(SecurityUtils.createTenantAuthority(user.getTenantId()));
        }

        return User.withUsername(user.getId())
                .password("")
                .authorities(grantedAuthorities)
                .build();
    }
}
