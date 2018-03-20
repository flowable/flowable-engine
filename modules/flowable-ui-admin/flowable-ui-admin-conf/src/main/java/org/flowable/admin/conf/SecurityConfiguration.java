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
package org.flowable.admin.conf;

import java.util.Collections;

import org.flowable.admin.security.AjaxLogoutSuccessHandler;
import org.flowable.app.filter.FlowableCookieFilter;
import org.flowable.app.security.ClearFlowableCookieLogoutHandler;
import org.flowable.app.security.CookieConstants;
import org.flowable.app.security.DefaultPrivileges;
import org.flowable.app.service.idm.RemoteIdmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public FlowableCookieFilter flowableCookieFilter(Environment env, RemoteIdmService remoteIdmService) {
        FlowableCookieFilter filter = new FlowableCookieFilter(env, remoteIdmService);
        filter.setRequiredPrivileges(Collections.singletonList(DefaultPrivileges.ACCESS_ADMIN));
        return filter;
    }

    @Configuration
    public static class FormLoginWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

        @Autowired
        protected FlowableCookieFilter flowableCookieFilter;

        @Autowired
        private AjaxLogoutSuccessHandler ajaxLogoutSuccessHandler;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .addFilterBefore(flowableCookieFilter, UsernamePasswordAuthenticationFilter.class)
                    .logout()
                    .logoutUrl("/app/logout")
                    .logoutSuccessHandler(ajaxLogoutSuccessHandler)
                    .addLogoutHandler(new ClearFlowableCookieLogoutHandler())
                    .deleteCookies(CookieConstants.COOKIE_NAME)
                    .and()
                    .csrf()
                    .disable()
                    .authorizeRequests()
                    .antMatchers("/app/rest/**").hasAuthority(DefaultPrivileges.ACCESS_ADMIN);
        }
    }
}
