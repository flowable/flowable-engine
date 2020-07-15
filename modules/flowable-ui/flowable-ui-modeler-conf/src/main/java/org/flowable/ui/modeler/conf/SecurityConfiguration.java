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

import org.flowable.ui.common.filter.FlowableCookieFilterRegistrationBean;
import org.flowable.ui.common.properties.FlowableCommonAppProperties;
import org.flowable.ui.common.properties.FlowableRestAppProperties;
import org.flowable.ui.common.security.ClearFlowableCookieLogoutHandler;
import org.flowable.ui.common.security.DefaultPrivileges;
import org.flowable.ui.common.security.SecurityConstants;
import org.flowable.ui.common.service.idm.RemoteIdmService;
import org.flowable.ui.common.service.idm.RemoteIdmServiceImpl;
import org.flowable.ui.modeler.properties.FlowableModelerAppProperties;
import org.flowable.ui.modeler.security.AjaxLogoutSuccessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * @author Filip Hrisafov
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfiguration {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfiguration.class);

    public static final String REST_ENDPOINTS_PREFIX = "/app/rest";

    @Bean
    public FlowableCookieFilterRegistrationBean flowableCookieFilterRegistrationBean(RemoteIdmService remoteIdmService, FlowableCommonAppProperties properties) {
        FlowableCookieFilterRegistrationBean filter = new FlowableCookieFilterRegistrationBean(remoteIdmService, properties);
        filter.addUrlPatterns("/app/*");
        filter.setRequiredPrivileges(Collections.singletonList(DefaultPrivileges.ACCESS_MODELER));
        return filter;
    }

    @Bean
    public RemoteIdmService remoteIdmService(FlowableCommonAppProperties properties) {
        return new RemoteIdmServiceImpl(properties);
    }
    
    @Configuration
    @Order(SecurityConstants.FORM_LOGIN_SECURITY_ORDER)
    public static class FormLoginWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

        @Autowired
        protected FlowableCookieFilterRegistrationBean flowableCookieFilterRegistrationBean;

        @Autowired
        protected AjaxLogoutSuccessHandler ajaxLogoutSuccessHandler;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                    .addFilterBefore(flowableCookieFilterRegistrationBean.getFilter(), UsernamePasswordAuthenticationFilter.class)
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
    @Order(SecurityConstants.MODELER_API_SECURITY_ORDER)
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
                    http.antMatcher("/api/editor/**").authorizeRequests().antMatchers("/api/editor/**").hasAuthority(DefaultPrivileges.ACCESS_REST_API).and().httpBasic();
                } else {
                    http.antMatcher("/api/editor/**").authorizeRequests().antMatchers("/api/editor/**").authenticated().and().httpBasic();
                    
                }
                
            } else {
                http.antMatcher("/api/editor/**").authorizeRequests().antMatchers("/api/editor/**").denyAll();
                
            }
            
        }
    }

}
