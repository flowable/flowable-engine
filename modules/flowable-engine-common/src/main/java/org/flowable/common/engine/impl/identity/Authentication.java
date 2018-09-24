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

import org.flowable.common.engine.api.identity.AuthenticationContext;

/**
 * @author Tom Baeyens
 */
public abstract class Authentication {

    private static AuthenticationContext authenticationContext = new UserIdAuthenticationContext();

    public static void setAuthenticatedUserId(String authenticatedUserId) {
        authenticationContext.setPrincipal(authenticatedUserId == null ? null : new UserIdPrincipal(authenticatedUserId));
    }

    public static String getAuthenticatedUserId() {
        return authenticationContext.getAuthenticatedUserId();
    }

    public static AuthenticationContext getAuthenticationContext() {
        return authenticationContext;
    }

    public static void setAuthenticationContext(AuthenticationContext authenticationContext) {
        Authentication.authenticationContext = authenticationContext;
    }
}
