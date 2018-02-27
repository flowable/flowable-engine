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
package org.flowable.rest.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flowable.engine.IdentityService;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.Privilege;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * @author Joram Barrez
 */
public class BasicAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    protected IdentityService identityService;
    
    @Autowired
    protected IdmIdentityService idmIdentityService;
    
    protected boolean verifyRestApiPrivilege;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String userId = authentication.getName();
        String password = authentication.getCredentials().toString();

        boolean authenticated = idmIdentityService.checkPassword(userId, password);
        if (authenticated) {
            
            Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>(1);
            if (isVerifyRestApiPrivilege()) {
                List<Privilege> privileges = idmIdentityService.createPrivilegeQuery().userId(userId).list();
                for (Privilege privilege : privileges) {
                    grantedAuthorities.add(new SimpleGrantedAuthority(privilege.getName()));
                }
            } else {
                // Always add the role when it's not verified: this makes the config easier (i.e. user needs to have it)
                grantedAuthorities.add(new SimpleGrantedAuthority(SecurityConstants.PRIVILEGE_ACCESS_REST_API));
            }
            
            identityService.setAuthenticatedUserId(userId);
            return new UsernamePasswordAuthenticationToken(userId, password, grantedAuthorities);
        } else {
            throw new BadCredentialsException("Authentication failed for this username and password");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

    public boolean isVerifyRestApiPrivilege() {
        return verifyRestApiPrivilege;
    }

    public void setVerifyRestApiPrivilege(boolean verifyRestApiPrivilege) {
        this.verifyRestApiPrivilege = verifyRestApiPrivilege;
    }
    
}
