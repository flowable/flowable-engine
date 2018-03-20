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
import org.flowable.spring.security.IdentityServiceUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Installs a Spring Security adapter for the Flowable {@link org.flowable.engine.IdentityService}.
 *
 * @author Josh Long
 */
@Configuration
@ConditionalOnClass({
    AuthenticationManager.class,
    GlobalAuthenticationConfigurerAdapter.class
})
@AutoConfigureBefore(org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration.class)
public class SecurityAutoConfiguration {

    @Configuration
    @ConditionalOnMissingBean(UserDetailsService.class)
    public static class UserDetailsServiceConfiguration
            extends GlobalAuthenticationConfigurerAdapter {

        @Override
        public void init(AuthenticationManagerBuilder auth) throws Exception {
            auth.userDetailsService(userDetailsService());
        }

        @Bean
        public UserDetailsService userDetailsService() {
            return new IdentityServiceUserDetailsService(this.identityService);
        }

        @Autowired
        private IdentityService identityService;
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