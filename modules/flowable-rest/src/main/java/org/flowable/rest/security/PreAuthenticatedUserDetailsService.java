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

import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.Privilege;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/**
 * Resolves the granted authorities for a user that a trusted reverse proxy has already
 * authenticated and whose id is presented in a request header (see the {@code pre-auth}
 * authentication mode of the REST app).
 *
 * <p>The privileges are loaded from the IDM engine for the header-provided user id, mirroring
 * {@link BasicAuthenticationProvider}: with privilege verification on, the user's actual
 * privileges are granted (so the {@code access-rest-api} / {@code access-admin} checks behave
 * identically to HTTP Basic); with it off, {@code access-rest-api} is granted unconditionally.
 *
 * <p>No password is checked here. Authentication has already happened at the proxy, and this
 * mode is only safe when the app is not reachable except through that proxy and the proxy
 * strips any client-supplied copy of the principal header.
 *
 * @author Arief Hidayat
 */
public class PreAuthenticatedUserDetailsService
        implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

    protected final IdmIdentityService idmIdentityService;
    protected boolean verifyRestApiPrivilege;

    public PreAuthenticatedUserDetailsService(IdmIdentityService idmIdentityService) {
        this.idmIdentityService = idmIdentityService;
    }

    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) {
        String userId = token.getName();

        Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        if (verifyRestApiPrivilege) {
            List<Privilege> privileges = idmIdentityService.createPrivilegeQuery().userId(userId).list();
            for (Privilege privilege : privileges) {
                grantedAuthorities.add(new SimpleGrantedAuthority(privilege.getName()));
            }
        } else {
            // Matches BasicAuthenticationProvider: when the privilege is not verified, grant it
            // so the downstream authorization rule is satisfied for any authenticated user.
            grantedAuthorities.add(new SimpleGrantedAuthority(SecurityConstants.PRIVILEGE_ACCESS_REST_API));
        }

        // A UserDetails must carry a (non-empty) password; it is never used, as no credential is
        // checked in this flow.
        return new User(userId, "", grantedAuthorities);
    }

    public boolean isVerifyRestApiPrivilege() {
        return verifyRestApiPrivilege;
    }

    public void setVerifyRestApiPrivilege(boolean verifyRestApiPrivilege) {
        this.verifyRestApiPrivilege = verifyRestApiPrivilege;
    }
}
