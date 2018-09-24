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
package org.flowable.common.engine.api.identity;

import java.security.Principal;

/**
 * Flowable Authentication context that can be implemented in different ways to hold and store the current authentication information.
 *
 * @author Filip Hrisafov
 */
public interface AuthenticationContext {

    /**
     * The user id of the authenticated principal.
     *
     * @return the id of the authenticated user
     */
    String getAuthenticatedUserId();

    /**
     * Obtains the currently authenticated principal, or an authentication request token.
     *
     * @return the <code>Principal</code> or <code>null</code> if no principal
     * information is available
     */
    Principal getPrincipal();

    /**
     * Changes the currently authenticated principal, or removes the authentication
     * information.
     *
     * @param principal the new <code>Authentication</code> token, or
     * <code>null</code> if no further principal information should be stored
     */
    void setPrincipal(Principal principal);

}
