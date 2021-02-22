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

import org.springframework.security.core.Authentication;

/**
 * Interface responsible for providing the security for a given authentication.
 *
 * @author Filip Hrisafov
 */
public interface SecurityScopeProvider {

    /**
     * Get the Flowable Security scope from the given authentication. It should never be null.
     *
     * @param authentication the authentication for the security scope
     * @return the non null security scope for the given authentication
     */
    SecurityScope getSecurityScope(Authentication authentication);

}
