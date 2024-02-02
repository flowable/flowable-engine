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
package org.flowable.common.engine.impl.identity;

import java.security.Principal;

import org.flowable.common.engine.api.identity.AuthenticationContext;

/**
 * Default implementation of the {@link AuthenticationContext} that uses a {@link ThreadLocal} that stores the {@link Principal}
 *
 * @author Filip Hrisafov
 */
public class UserIdAuthenticationContext implements AuthenticationContext {

    private static ThreadLocal<Principal> authenticatedUserIdThreadLocal = new ThreadLocal<>();

    @Override
    public String getAuthenticatedUserId() {
        Principal principal = authenticatedUserIdThreadLocal.get();
        return principal == null ? null : principal.getName();
    }

    @Override
    public Principal getPrincipal() {
        return authenticatedUserIdThreadLocal.get();
    }

    @Override
    public void setPrincipal(Principal principal) {
        authenticatedUserIdThreadLocal.set(principal);
    }
}
