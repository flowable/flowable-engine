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

import org.flowable.app.security.AjaxAuthenticationFailureHandler;
import org.flowable.app.security.AjaxAuthenticationSuccessHandler;
import org.flowable.app.security.AjaxLogoutSuccessHandler;
import org.flowable.app.security.ClearFlowableCookieLogoutHandler;
import org.flowable.app.security.CustomDaoAuthenticationProvider;
import org.flowable.app.security.CustomLdapAuthenticationProvider;
import org.flowable.app.security.CustomPersistentRememberMeServices;
import org.flowable.app.security.DefaultPrivileges;
import org.flowable.app.security.Http401UnauthorizedEntryPoint;
import org.flowable.app.web.CustomFormLoginConfig;
import org.flowable.idm.api.IdmIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.RememberMeAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

/**
 * Based on http://docs.spring.io/spring-security/site/docs/3.2.x/reference/htmlsingle/#multiple-httpsecurity
 *
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, jsr250Enabled = true)
public class SecurityConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfiguration.class);

    //
    // GLOBAL CONFIG
    //

    @Qualifier("defaultIdmIdentityService")
    @Autowired
    protected IdmIdentityService identityService;
    
    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Qualifier("customAuthenticationProvider")
    @Autowired(required = false)
    protected AuthenticationProvider customAuthenticationProvider;
    
    @Autowired
    protected Environment env;
    
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) {

        if (env.getProperty("ldap.enabled", Boolean.class, false)) {
            // LDAP auth
            try {
                auth.authenticationProvider(ldapAuthenticationProvider());
            } catch (Exception e) {
                LOGGER.error("Could not configure ldap authentication mechanism:", e);
            }
        } else if (customAuthenticationProvider != null) {
            auth.authenticationProvider(customAuthenticationProvider);
        } else {
            // Default auth (database backed)
            try {
                auth.authenticationProvider(dbAuthenticationProvider());
            } catch (Exception e) {
                LOGGER.error("Could not configure authentication mechanism:", e);
            }
        }
    }

    @Bean
    public UserDetailsService userDetailsService() {
        org.flowable.app.security.UserDetailsService userDetailsService = new org.flowable.app.security.UserDetailsService();
        userDetailsService.setUserValidityPeriod(env.getProperty("cache.users.recheck.period", Long.class, 30000L));
        return userDetailsService;
    }

    @Bean(name = "dbAuthenticationProvider")
    public AuthenticationProvider dbAuthenticationProvider() {
        CustomDaoAuthenticationProvider daoAuthenticationProvider = new CustomDaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userDetailsService());
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        return daoAuthenticationProvider;
    }

    @Bean(name = "ldapAuthenticationProvider")
    public AuthenticationProvider ldapAuthenticationProvider() {
        CustomLdapAuthenticationProvider ldapAuthenticationProvider = new CustomLdapAuthenticationProvider(
                userDetailsService(), identityService);
        return ldapAuthenticationProvider;
    }

    //
    // REGULAR WEBAP CONFIG
    //

    @Configuration
    @Order(10) // API config first (has Order(1))
    public static class FormLoginWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

        @Autowired
        private Environment env;

        @Autowired
        private AjaxAuthenticationSuccessHandler ajaxAuthenticationSuccessHandler;

        @Autowired
        private AjaxAuthenticationFailureHandler ajaxAuthenticationFailureHandler;

        @Autowired
        private AjaxLogoutSuccessHandler ajaxLogoutSuccessHandler;

        @Autowired
        private Http401UnauthorizedEntryPoint authenticationEntryPoint;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .exceptionHandling()
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .and()
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                    .rememberMe()
                    .rememberMeServices(rememberMeServices())
                    .key(env.getProperty("security.rememberme.key"))
                    .and()
                    .logout()
                    .logoutUrl("/app/logout")
                    .logoutSuccessHandler(ajaxLogoutSuccessHandler)
                    .addLogoutHandler(new ClearFlowableCookieLogoutHandler())
                    .permitAll()
                    .and()
                    .csrf()
                    .disable() // Disabled, cause enabling it will cause sessions
                    .headers()
                    .frameOptions()
                    .sameOrigin()
                    .addHeaderWriter(new XXssProtectionHeaderWriter())
                    .and()
                    .authorizeRequests()
                    .antMatchers("/*").permitAll()
                    .antMatchers("/app/rest/authenticate").permitAll()
                    .antMatchers("/app/**").hasAuthority(DefaultPrivileges.ACCESS_IDM);

            // Custom login form configurer to allow for non-standard HTTP-methods (eg. LOCK)
            CustomFormLoginConfig<HttpSecurity> loginConfig = new CustomFormLoginConfig<>();
            loginConfig.loginProcessingUrl("/app/authentication")
                    .successHandler(ajaxAuthenticationSuccessHandler)
                    .failureHandler(ajaxAuthenticationFailureHandler)
                    .usernameParameter("j_username")
                    .passwordParameter("j_password")
                    .permitAll();

            http.apply(loginConfig);
        }

        @Bean
        public RememberMeServices rememberMeServices() {
            return new CustomPersistentRememberMeServices(env, userDetailsService());
        }

        @Bean
        public RememberMeAuthenticationProvider rememberMeAuthenticationProvider() {
            return new RememberMeAuthenticationProvider(env.getProperty("security.rememberme.key"));
        }
    }

    //
    // BASIC AUTH
    //

    @Configuration
    @Order(1)
    public static class ApiWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {
        
        @Autowired
        protected Environment env;

        protected void configure(HttpSecurity http) throws Exception {

            http
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                    .csrf()
                    .disable();

            if (isEnableRestApi()) {
                
                if (RestApiUtil.isVerifyRestApiPrivilege(env)) {
                    http.antMatcher("/api/**").authorizeRequests().antMatchers("/api/**").hasAuthority(DefaultPrivileges.ACCESS_REST_API).and().httpBasic();
                } else {
                    http.antMatcher("/api/**").authorizeRequests().antMatchers("/api/**").authenticated().and().httpBasic();
                    
                }
                
            } else {
                http.antMatcher("/api/**").authorizeRequests().antMatchers("/api/**").denyAll();
                
            }
            
        }
        
        protected boolean isEnableRestApi() {
            return env.getProperty("rest.idm-app.enabled", Boolean.class, true);
        }
        
    }

}
