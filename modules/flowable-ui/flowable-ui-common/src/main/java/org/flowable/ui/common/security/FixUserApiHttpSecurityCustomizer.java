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

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * @author Filip Hrisafov
 */
public class FixUserApiHttpSecurityCustomizer implements ApiHttpSecurityCustomizer {

    protected final String username;
    protected final String password;

    public FixUserApiHttpSecurityCustomizer(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void customize(HttpSecurity http) throws Exception {
        http
                .userDetailsService(new InMemoryUserDetailsManager(
                        User.withUsername(username)
                                .password(password)
                                .authorities(DefaultPrivileges.ACCESS_REST_API, DefaultPrivileges.ACCESS_ADMIN)
                                .build()
                ))
                .httpBasic();

    }
}
