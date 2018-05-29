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

import java.security.Principal;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.identity.AuthenticationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A default Spring Security {@link AuthenticationContext} implementation that uses {@link SecurityContextHolder} and
 * {@link org.springframework.security.core.context.SecurityContext} to provide the information.
 *
 * @author Filip Hrisafov
 */
public class SpringSecurityAuthenticationContext implements AuthenticationContext {

    @Override
    public String getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    @Override
    public Authentication getPrincipal() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Override
    public void setPrincipal(Principal principal) {
        if (principal == null) {
            SecurityContextHolder.getContext().setAuthentication(null);
        } else if (principal instanceof Authentication) {
            SecurityContextHolder.getContext().setAuthentication((Authentication) principal);
        } else {
            throw new FlowableIllegalArgumentException("Principal must be of Authentication type. It was of " + principal.getClass());
        }
    }
}
