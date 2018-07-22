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

import org.flowable.common.engine.api.identity.AuthenticationContext;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration;
import org.flowable.spring.security.FlowableAuthenticationProvider;
import org.flowable.spring.security.FlowableUserDetailsService;
import org.flowable.spring.security.SpringSecurityAuthenticationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Installs a Spring Security adapter for the Flowable {@link IdmIdentityService}.
 *
 * @author Josh Long
 */
@Configuration
@ConditionalOnClass({
    AuthenticationManager.class,
    IdmIdentityService.class,
    FlowableAuthenticationProvider.class,
    GlobalAuthenticationConfigurerAdapter.class
})
@ConditionalOnBean(IdmIdentityService.class)
@AutoConfigureBefore(org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class)
@AutoConfigureAfter({
    IdmEngineServicesAutoConfiguration.class,
    ProcessEngineAutoConfiguration.class
})
public class FlowableSecurityAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableSecurityAutoConfiguration.class);

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
        }
    }

    @Configuration
    @ConditionalOnClass(AuthenticationContext.class)
    public static class SpringSecurityAuthenticationContextConfiguration {

        public SpringSecurityAuthenticationContextConfiguration(ObjectProvider<AuthenticationContext> authenticationContext) {
            AuthenticationContext context = authenticationContext.getIfAvailable();
            if (context == null) {
                context = new SpringSecurityAuthenticationContext();
            }

            Authentication.setAuthenticationContext(context);
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
}