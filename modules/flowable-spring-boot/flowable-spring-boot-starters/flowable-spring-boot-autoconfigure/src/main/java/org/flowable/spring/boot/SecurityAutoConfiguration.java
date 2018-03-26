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
package org.flowable.spring.boot;

import org.flowable.engine.IdentityService;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.spring.boot.condition.ConditionalOnIdmEngine;
import org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration;
import org.flowable.spring.security.FlowableAuthenticationProvider;
import org.flowable.spring.security.FlowableUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Installs a Spring Security adapter for the Flowable {@link IdmIdentityService}.
 *
 * @author Josh Long
 */
@Configuration
@ConditionalOnIdmEngine
@ConditionalOnClass({
    AuthenticationManager.class,
    GlobalAuthenticationConfigurerAdapter.class
})
@AutoConfigureBefore(org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration.class)
@AutoConfigureAfter({
    IdmEngineServicesAutoConfiguration.class,
    ProcessEngineAutoConfiguration.class
})
public class SecurityAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAutoConfiguration.class);

    @Configuration
    public static class UserDetailsServiceConfiguration
            extends GlobalAuthenticationConfigurerAdapter {

        protected final ObjectProvider<AuthenticationProvider> authenticationProviderProvider;
        protected final ObjectProvider<UserDetailsService> userDetailsServiceProvider;

        public UserDetailsServiceConfiguration(
            ObjectProvider<AuthenticationProvider> authenticationProviderProvider,
            ObjectProvider<UserDetailsService> userDetailsServiceProvider) {
            this.authenticationProviderProvider = authenticationProviderProvider;
            this.userDetailsServiceProvider = userDetailsServiceProvider;
        }

        @Override
        public void init(AuthenticationManagerBuilder auth) throws Exception {
            if (!auth.isConfigured()) {
                AuthenticationProvider authenticationProvider = authenticationProviderProvider.getIfUnique();
                if (authenticationProvider != null) {
                    auth.authenticationProvider(authenticationProvider);
                } else {
                    LOGGER.warn("There is no authentication provider configured. However, there is no single one in the context."
                        + " Please configure the global authentication provider by yourself.");
                }
            }

            if (auth.getDefaultUserDetailsService() == null) {
                UserDetailsService userDetailsService = userDetailsServiceProvider.getIfUnique();
                if (userDetailsService != null) {
                    auth.userDetailsService(userDetailsService);
                } else {
                    LOGGER.warn("There is no default UserDetailsService configured, but there is no single one in the context."
                        + " Please configure it by yourself");
                }
            }
        }
    }

    @Bean
    @ConditionalOnMissingBean(UserDetailsService.class)
    public FlowableUserDetailsService flowableUserDetailsService(IdmIdentityService identityService) {
        return new FlowableUserDetailsService(identityService);
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationProvider.class)
    public FlowableAuthenticationProvider flowableAuthenticationProvider(IdmIdentityService idmIdentityService, UserDetailsService userDetailsService) {
        return new FlowableAuthenticationProvider(idmIdentityService, userDetailsService);
    }

    @ConditionalOnBean(type = "org.flowable.engine.IdentityService")
    @ConditionalOnMissingBean(type = "org.flowable.idm.api.IdmIdentityService")
    @Configuration
    protected static class IdentitySecurityConfiguration implements ApplicationListener<AuthenticationSuccessEvent> {

        protected final IdentityService identityService;

        public IdentitySecurityConfiguration(IdentityService identityService) {
            this.identityService = identityService;
        }

        @Override
        public void onApplicationEvent(AuthenticationSuccessEvent event) {
            identityService.setAuthenticatedUserId(event.getAuthentication().getName());
        }
    }

    @ConditionalOnBean(type = "org.flowable.idm.api.IdmIdentityService")
    @Configuration
    protected static class IdmIdentitySecurityConfiguration implements ApplicationListener<AuthenticationSuccessEvent> {

        protected final IdmIdentityService identityService;

        public IdmIdentitySecurityConfiguration(IdmIdentityService identityService) {
            this.identityService = identityService;
        }

        @Override
        public void onApplicationEvent(AuthenticationSuccessEvent event) {
            identityService.setAuthenticatedUserId(event.getAuthentication().getName());
        }
    }
}