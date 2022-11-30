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
package org.flowable.ui.modeler.conf;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import org.flowable.ui.common.properties.FlowableRestAppProperties;
import org.flowable.ui.common.security.ApiHttpSecurityCustomizer;
import org.flowable.ui.common.security.DefaultPrivileges;
import org.flowable.ui.common.security.SecurityConstants;
import org.flowable.ui.modeler.properties.FlowableModelerAppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Based on http://docs.spring.io/spring-security/site/docs/3.2.x/reference/htmlsingle/#multiple-httpsecurity
 * 
 * @author Joram Barrez
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class ModelerSecurityConfiguration {
    
    //
    // BASIC AUTH
    //

    @Configuration(proxyBeanMethods = false)
    public static class ModelerApiWebSecurityConfigurationAdapter {

        protected final FlowableRestAppProperties restAppProperties;
        protected final FlowableModelerAppProperties modelerAppProperties;
        protected final ApiHttpSecurityCustomizer apiHttpSecurityCustomizer;

        public ModelerApiWebSecurityConfigurationAdapter(FlowableRestAppProperties restAppProperties,
                FlowableModelerAppProperties modelerAppProperties, ApiHttpSecurityCustomizer apiHttpSecurityCustomizer) {
            this.restAppProperties = restAppProperties;
            this.modelerAppProperties = modelerAppProperties;
            this.apiHttpSecurityCustomizer = apiHttpSecurityCustomizer;
        }

        @Bean
        @Order(SecurityConstants.MODELER_API_SECURITY_ORDER)
        public SecurityFilterChain modelerApiSecurity(HttpSecurity http) throws Exception {

            http
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                    .csrf()
                    .disable();

            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl modelerHttpRequestsConfigurer = http
                    .securityMatcher(antMatcher("/api/editor/**"))
                    .authorizeHttpRequests()
                    .requestMatchers(antMatcher("/api/editor/**"));
            if (modelerAppProperties.isRestEnabled()) {

                if (restAppProperties.isVerifyRestApiPrivilege()) {
                    modelerHttpRequestsConfigurer.hasAuthority(DefaultPrivileges.ACCESS_REST_API);
                } else {
                    modelerHttpRequestsConfigurer.authenticated();
                    
                }

                apiHttpSecurityCustomizer.customize(http);
                
            } else {
                modelerHttpRequestsConfigurer.denyAll();
                
            }

            return http.build();
        }
    }

}
