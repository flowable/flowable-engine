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

import org.springframework.security.authentication.AuthenticationTrustResolverImpl;

/**
 * Flowable Specific implementation of {@link org.springframework.security.authentication.AuthenticationTrustResolver}
 * that returns {@code false} when checking if an authentication is a remember me authentication.
 * The reason for this is that we don't want to force a new start of an authentication if the user accessed resources that they
 * don't have access to and thus leading to a loop.
 *
 * @author Filip Hrisafov
 * @see org.springframework.security.web.access.ExceptionTranslationFilter
 */
public class FlowableAuthenticationTrustResolver extends AuthenticationTrustResolverImpl {

    public FlowableAuthenticationTrustResolver() {
        setRememberMeClass(null);
    }

}
