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

import org.flowable.idm.api.IdmIdentityService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * A flowable implementation of {@link org.springframework.security.authentication.AuthenticationProvider} that uses the {@link IdmIdentityService} and
 * {@link UserDetailsService} to check the user credentials and and load the user. It uses the {@link UsernamePasswordAuthenticationToken}.
 *
 * @author Filip Hrisafov
 */
public class FlowableAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

    protected final IdmIdentityService idmIdentityService;
    protected final UserDetailsService userDetailsService;

    public FlowableAuthenticationProvider(IdmIdentityService idmIdentityService, UserDetailsService userDetailsService) {
        this.idmIdentityService = idmIdentityService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        String name = userDetails.getUsername();
        String password = authentication.getCredentials().toString();

        boolean authenticated = idmIdentityService.checkPassword(name, password);
        if (!authenticated) {
            throw new BadCredentialsException("Authentication failed for this username and password");
        }
    }

    @Override
    protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        return userDetailsService.loadUserByUsername(username);
    }
}
