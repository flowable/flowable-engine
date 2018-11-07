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

import org.flowable.idm.api.IdmIdentityService;
import org.flowable.ui.common.security.FlowableAppUser;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * @author Tijs Rademakers
 */
public class CustomLdapAuthenticationProvider implements AuthenticationProvider {
    
    protected UserDetailsService userDetailsService;
    protected IdmIdentityService identityService;
    
    protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
    protected GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

    public CustomLdapAuthenticationProvider(UserDetailsService userDetailsService, IdmIdentityService identityService) {
        this.userDetailsService = userDetailsService;
        this.identityService = identityService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        UsernamePasswordAuthenticationToken authenticationToken = (UsernamePasswordAuthenticationToken) authentication;
        
        boolean authenticated = identityService.checkPassword(authenticationToken.getName(), authenticationToken.getCredentials().toString());
        if (!authenticated) {
            throw new BadCredentialsException(messages.getMessage("LdapAuthenticationProvider.badCredentials", "Bad credentials"));
        }
        
        FlowableAppUser userDetails = (FlowableAppUser) userDetailsService.loadUserByUsername(authenticationToken.getName());
        
        UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(
                userDetails, authenticationToken.getCredentials(), 
                authoritiesMapper.mapAuthorities(userDetails.getAuthorities()));
        result.setDetails(authentication.getDetails());
        
        return result;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
