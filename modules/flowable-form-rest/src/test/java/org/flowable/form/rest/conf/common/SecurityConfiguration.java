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
package org.flowable.form.rest.conf.common;

import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.spring.security.FlowableAuthenticationProvider;
import org.flowable.spring.security.FlowableUserDetailsService;
import org.flowable.spring.security.SpringSecurityAuthenticationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfiguration {

    public SecurityConfiguration() {
        Authentication.setAuthenticationContext(new SpringSecurityAuthenticationContext());
    }

    @Bean
    public FlowableAuthenticationProvider authenticationProvider(IdmIdentityService idmIdentityService, UserDetailsService userDetailsService) {
        return new FlowableAuthenticationProvider(idmIdentityService, userDetailsService);
    }

    @Bean
    public FlowableUserDetailsService userDetailsService(IdmIdentityService identityService) {
        return new FlowableUserDetailsService(identityService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationProvider authenticationProvider) throws Exception {
        http.authenticationProvider(authenticationProvider)
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and().csrf().disable()
            .authorizeHttpRequests()
            .anyRequest()
            .authenticated().and().httpBasic();

        return http.build();
    }

    /* Needed for allowing slashes in urls, needed for getting deployment resources */
    @Bean
    public HttpFirewall defaultFireWall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        return firewall;
    }

    @Bean
    public WebSecurityCustomizer fireWallCustomizer(HttpFirewall defaultFireWall) {
        return web -> web.httpFirewall(defaultFireWall);
    }

}
