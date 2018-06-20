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
package org.flowable.test.spring.boot;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.spring.boot.FlowableSecurityAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration;
import org.flowable.spring.security.FlowableAuthenticationProvider;
import org.flowable.spring.security.FlowableUserDetailsService;
import org.flowable.spring.security.SpringSecurityAuthenticationContext;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * @author Filip Hrisafov
 */
public class FlowableSecurityAutoConfigurationTest {

    private static final AutoConfigurations IDM_CONFIGURATION = AutoConfigurations.of(
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        IdmEngineAutoConfiguration.class,
        IdmEngineServicesAutoConfiguration.class
    );

    private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            FlowableSecurityAutoConfiguration.class,
            org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
        ));

    @Test
    public void withMissingAuthenticationManager() {
        contextRunner
            .withConfiguration(IDM_CONFIGURATION)
            .withClassLoader(new FilteredClassLoader(AuthenticationManager.class))
            .run(context -> assertThat(context)
                .hasSingleBean(IdmIdentityService.class)
                .doesNotHaveBean(FlowableSecurityAutoConfiguration.class));
    }

    @Test
    public void withMissingIdmIdentityService() {
        contextRunner
            .withConfiguration(IDM_CONFIGURATION)
            .withClassLoader(new FilteredClassLoader(IdmIdentityService.class))
            .run(context -> assertThat(context)
                .hasSingleBean(IdmIdentityService.class)
                .doesNotHaveBean(FlowableSecurityAutoConfiguration.class));
    }

    @Test
    public void withMissingFlowableAuthenticationProvider() {
        contextRunner
            .withConfiguration(IDM_CONFIGURATION)
            .withClassLoader(new FilteredClassLoader(FlowableAuthenticationProvider.class))
            .run(context -> assertThat(context)
                .hasSingleBean(IdmIdentityService.class)
                .doesNotHaveBean(FlowableSecurityAutoConfiguration.class));
    }

    @Test
    public void withMissingGlobalAuthenticationConfigurerAdapter() {
        contextRunner
            .withConfiguration(IDM_CONFIGURATION)
            .withClassLoader(new FilteredClassLoader(GlobalAuthenticationConfigurerAdapter.class))
            .run(context -> assertThat(context)
                .hasSingleBean(IdmIdentityService.class)
                .doesNotHaveBean(FlowableSecurityAutoConfiguration.class));
    }

    @Test
    public void withMissingIdmIdentityServiceBean() {
        contextRunner
            .run(context -> assertThat(context)
                .doesNotHaveBean(IdmIdentityService.class)
                .doesNotHaveBean(FlowableSecurityAutoConfiguration.class));
    }

    @Test
    public void securityConfigurationShouldUseFlowableSecurity() {
        contextRunner
            .withConfiguration(IDM_CONFIGURATION)
            .run(context -> {
                    assertThat(context)
                        .hasSingleBean(IdmIdentityService.class)
                        .hasSingleBean(FlowableSecurityAutoConfiguration.class)
                        .hasSingleBean(UserDetailsService.class)
                        .hasSingleBean(AuthenticationProvider.class);
                    assertThat(context.getBean(UserDetailsService.class)).isInstanceOf(FlowableUserDetailsService.class);
                    assertThat(context.getBean(AuthenticationProvider.class)).isInstanceOf(FlowableAuthenticationProvider.class);

                    assertThat(Authentication.getAuthenticationContext()).isInstanceOf(SpringSecurityAuthenticationContext.class);
                }
            );
    }
}
