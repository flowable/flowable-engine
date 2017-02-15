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
package org.flowable.app.conf;

import java.util.Collections;

import org.flowable.app.filter.FlowableCookieFilter;
import org.flowable.app.security.AjaxLogoutSuccessHandler;
import org.flowable.app.security.ClearFlowableCookieLogoutHandler;
import org.flowable.app.security.DefaultPrivileges;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

/**
 * Based on http://docs.spring.io/spring-security/site/docs/3.2.x/reference/htmlsingle/#multiple-httpsecurity
 * 
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    public static final String REST_ENDPOINTS_PREFIX = "/app/rest";

    @Bean
    public FlowableCookieFilter flowableCookieFilter() {
        FlowableCookieFilter filter = new FlowableCookieFilter();
        filter.setRequiredPrivileges(Collections.singletonList(DefaultPrivileges.ACCESS_MODELER));
        return filter;
    }

    @Configuration
    @Order(10)
    public static class FormLoginWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

        @Autowired
        protected FlowableCookieFilter flowableCookieFilter;

        @Autowired
        protected AjaxLogoutSuccessHandler ajaxLogoutSuccessHandler;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                    .addFilterBefore(flowableCookieFilter, UsernamePasswordAuthenticationFilter.class)
                    .logout()
                    .logoutUrl("/app/logout")
                    .logoutSuccessHandler(ajaxLogoutSuccessHandler)
                    .addLogoutHandler(new ClearFlowableCookieLogoutHandler())
                    .and()
                    .csrf()
                    .disable() // Disabled, cause enabling it will cause sessions
                    .headers()
                    .frameOptions()
                    .sameOrigin()
                    .addHeaderWriter(new XXssProtectionHeaderWriter())
                    .and()
                    .authorizeRequests()
                    .antMatchers(REST_ENDPOINTS_PREFIX + "/**").hasAuthority(DefaultPrivileges.ACCESS_MODELER);
        }
    }
}
