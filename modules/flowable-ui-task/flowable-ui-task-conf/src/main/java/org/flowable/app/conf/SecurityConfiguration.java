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

import org.flowable.app.filter.FlowableCookieFilterCallback;
import org.flowable.app.filter.FlowableCookieFilterRegistrationBean;
import org.flowable.app.properties.FlowableRemoteIdmProperties;
import org.flowable.app.properties.FlowableRestAppProperties;
import org.flowable.app.properties.FlowableTaskAppProperties;
import org.flowable.app.security.AjaxLogoutSuccessHandler;
import org.flowable.app.security.ClearFlowableCookieLogoutHandler;
import org.flowable.app.security.DefaultPrivileges;
import org.flowable.app.security.EngineAuthenticationCookieFilterCallback;
import org.flowable.app.security.RemoteIdmAuthenticationProvider;
import org.flowable.app.service.idm.RemoteIdmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

/**
 * Based on http://docs.spring.io/spring-security/site/docs/3.2.x/reference/htmlsingle/#multiple-httpsecurity
 * 
 * @author Joram Barrez
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 */
@Configuration
public class SecurityConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfiguration.class);

    @Autowired
    protected RemoteIdmAuthenticationProvider authenticationProvider;


    @Bean
    public FlowableCookieFilterRegistrationBean flowableCookieFilterRegistration(RemoteIdmService remoteIdmService, FlowableRemoteIdmProperties properties) {
        FlowableCookieFilterRegistrationBean registrationBean = new FlowableCookieFilterRegistrationBean(remoteIdmService, properties);
        registrationBean.addUrlPatterns("/app/*");
        registrationBean.setRequiredPrivileges(Collections.singletonList(DefaultPrivileges.ACCESS_TASK));
        return registrationBean;
    }

    @Bean
    public FlowableCookieFilterCallback flowableCookieFilterCallback() {
        return new EngineAuthenticationCookieFilterCallback();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) {

        // Default auth (database backed)
        try {
            auth.authenticationProvider(authenticationProvider);
        } catch (Exception e) {
            LOGGER.error("Could not configure authentication mechanism:", e);
        }
    }

    //
    // REGULAR WEBAP CONFIG
    //

    @Configuration
    @Order(10) // API config first (has Order(1))
    public static class FormLoginWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

        @Autowired
        protected FilterRegistrationBean flowableCookieFilterRegistration;

        @Autowired
        protected AjaxLogoutSuccessHandler ajaxLogoutSuccessHandler;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                    .addFilterBefore(flowableCookieFilterRegistration.getFilter(), UsernamePasswordAuthenticationFilter.class)
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
                .antMatchers("/rest/**").hasAuthority(DefaultPrivileges.ACCESS_TASK);
        }
    }

    //
    // BASIC AUTH
    //

    @Configuration
    @Order(1)
    public static class ApiWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        protected final FlowableRestAppProperties restAppProperties;
        protected final FlowableTaskAppProperties taskAppProperties;

        public ApiWebSecurityConfigurationAdapter(FlowableRestAppProperties restAppProperties,
            FlowableTaskAppProperties taskAppProperties) {
            this.restAppProperties = restAppProperties;
            this.taskAppProperties = taskAppProperties;
        }

        protected void configure(HttpSecurity http) throws Exception {

            http.sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and().csrf().disable();

            if (taskAppProperties.isRestEnabled()) {

                if (restAppProperties.isVerifyRestApiPrivilege()) {
                    http.antMatcher("/*-api/**").authorizeRequests().antMatchers("/*-api/**").hasAuthority(DefaultPrivileges.ACCESS_REST_API).and().httpBasic();
                } else {
                    http.antMatcher("/*-api/**").authorizeRequests().antMatchers("/*-api/**").authenticated().and().httpBasic();
                    
                }
                
            } else {
                http.antMatcher("/*-api/**").authorizeRequests().antMatchers("/*-api/**").denyAll();
                
            }
                   
        }
    }
    
}
