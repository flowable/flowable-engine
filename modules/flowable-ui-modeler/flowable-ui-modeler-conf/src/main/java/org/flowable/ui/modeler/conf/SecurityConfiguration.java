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

import java.util.Collections;

import org.flowable.ui.common.filter.FlowableCookieFilter;
import org.flowable.ui.common.properties.FlowableCommonAppProperties;
import org.flowable.ui.common.properties.FlowableRestAppProperties;
import org.flowable.ui.common.security.ClearFlowableCookieLogoutHandler;
import org.flowable.ui.common.security.DefaultPrivileges;
import org.flowable.ui.common.service.idm.RemoteIdmService;
import org.flowable.ui.modeler.properties.FlowableModelerAppProperties;
import org.flowable.ui.modeler.security.AjaxLogoutSuccessHandler;
import org.flowable.ui.modeler.security.RemoteIdmAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
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
 * @author Filip Hrisafov
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfiguration.class);

    public static final String REST_ENDPOINTS_PREFIX = "/app/rest";
    
    @Autowired
    protected RemoteIdmAuthenticationProvider authenticationProvider;

    @Bean
    public FlowableCookieFilter flowableCookieFilter(RemoteIdmService remoteIdmService, FlowableCommonAppProperties properties) {
        FlowableCookieFilter filter = new FlowableCookieFilter(remoteIdmService, properties);
        filter.setRequiredPrivileges(Collections.singletonList(DefaultPrivileges.ACCESS_MODELER));
        return filter;
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
    
    //
    // BASIC AUTH
    //

    @Configuration
    @Order(1)
    public static class ApiWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        protected final FlowableRestAppProperties restAppProperties;
        protected final FlowableModelerAppProperties modelerAppProperties;

        public ApiWebSecurityConfigurationAdapter(FlowableRestAppProperties restAppProperties,
            FlowableModelerAppProperties modelerAppProperties) {
            this.restAppProperties = restAppProperties;
            this.modelerAppProperties = modelerAppProperties;
        }

        protected void configure(HttpSecurity http) throws Exception {

            http
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                    .csrf()
                    .disable();

            if (modelerAppProperties.isRestEnabled()) {

                if (restAppProperties.isVerifyRestApiPrivilege()) {
                    http.antMatcher("/api/**").authorizeRequests().antMatchers("/api/**").hasAuthority(DefaultPrivileges.ACCESS_REST_API).and().httpBasic();
                } else {
                    http.antMatcher("/api/**").authorizeRequests().antMatchers("/api/**").authenticated().and().httpBasic();
                    
                }
                
            } else {
                http.antMatcher("/api/**").authorizeRequests().antMatchers("/api/**").denyAll();
                
            }
            
        }
    }

    //
    // Actuator
    //

    @ConditionalOnClass(EndpointRequest.class)
    @Configuration
    @Order(15) // Actuator configuration should kick in after the Form Login so the custom login filter can be applied before
    public static class ActuatorWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        protected void configure(HttpSecurity http) throws Exception {

            http
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .csrf()
                .disable();

            http
                .authorizeRequests()
                .requestMatchers(EndpointRequest.to(InfoEndpoint.class, HealthEndpoint.class)).authenticated()
                .requestMatchers(EndpointRequest.toAnyEndpoint()).hasAnyAuthority(DefaultPrivileges.ACCESS_ADMIN)
                .and().httpBasic();
        }
    }
}
