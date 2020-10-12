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
package org.flowable.test.spring.boot.idm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.flowable.test.spring.boot.util.DeploymentCleanerUtil.deleteDeployments;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.app.engine.AppEngine;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.util.EngineServiceUtil;
import org.flowable.idm.api.IdmEngineConfigurationApi;
import org.flowable.idm.engine.IdmEngine;
import org.flowable.idm.spring.SpringIdmEngineConfiguration;
import org.flowable.idm.spring.authentication.SpringEncoder;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration;
import org.flowable.test.spring.boot.util.CustomUserEngineConfigurerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author Filip Hrisafov
 */
public class IdmEngineAutoConfigurationTest {

    private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            IdmEngineServicesAutoConfiguration.class,
            IdmEngineAutoConfiguration.class
        ))
        .withUserConfiguration(CustomUserEngineConfigurerConfiguration.class);

    @Test
    public void standaloneIdmEngineWithBasicDataSource() {
        contextRunner.run(context -> {
            assertThat(context)
                .doesNotHaveBean(AppEngine.class)
                .doesNotHaveBean(ProcessEngine.class)
                .doesNotHaveBean("idmProcessEngineConfigurationConfigurer")
                .doesNotHaveBean("idmAppEngineConfigurationConfigurer");
            IdmEngine idmEngine = context.getBean(IdmEngine.class);
            assertThat(idmEngine).as("Idm engine").isNotNull();
            assertAllServicesPresent(context, idmEngine);

            assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                .hasSingleBean(PasswordEncoder.class)
                .getBean(CustomUserEngineConfigurerConfiguration.class)
                .satisfies(configuration -> {
                    assertThat(configuration.getInvokedConfigurations())
                        .containsExactly(
                            SpringIdmEngineConfiguration.class
                        );
                });

            org.flowable.idm.api.PasswordEncoder flowablePasswordEncoder = idmEngine.getIdmEngineConfiguration().getPasswordEncoder();
            PasswordEncoder passwordEncoder = context.getBean(PasswordEncoder.class);
            assertThat(flowablePasswordEncoder)
                .isInstanceOfSatisfying(SpringEncoder.class, springEncoder -> {
                    assertThat(springEncoder.getSpringEncodingProvider()).isEqualTo(passwordEncoder);
                });
            assertThat(passwordEncoder).isInstanceOf(NoOpPasswordEncoder.class);
        });
    }

    @Test
    public void standaloneIdmEngineWithBCryptPasswordEncoder() {
        contextRunner
            .withPropertyValues("flowable.idm.password-encoder=spring_bcrypt")
            .run(context -> {
                IdmEngine idmEngine = context.getBean(IdmEngine.class);

                assertThat(context).hasSingleBean(PasswordEncoder.class);

                org.flowable.idm.api.PasswordEncoder flowablePasswordEncoder = idmEngine.getIdmEngineConfiguration().getPasswordEncoder();
                PasswordEncoder passwordEncoder = context.getBean(PasswordEncoder.class);
                assertThat(flowablePasswordEncoder)
                    .isInstanceOfSatisfying(SpringEncoder.class, springEncoder -> {
                        assertThat(springEncoder.getSpringEncodingProvider()).isEqualTo(passwordEncoder);
                    });
                assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
            });
    }

    @Test
    public void standaloneIdmEngineWithDelegatingPasswordEncoder() {
        contextRunner
            .withPropertyValues("flowable.idm.password-encoder=spring_delegating")
            .run(context -> {
                IdmEngine idmEngine = context.getBean(IdmEngine.class);

                assertThat(context).hasSingleBean(PasswordEncoder.class);

                org.flowable.idm.api.PasswordEncoder flowablePasswordEncoder = idmEngine.getIdmEngineConfiguration().getPasswordEncoder();
                PasswordEncoder passwordEncoder = context.getBean(PasswordEncoder.class);
                assertThat(flowablePasswordEncoder)
                    .isInstanceOfSatisfying(SpringEncoder.class, springEncoder -> {
                        assertThat(springEncoder.getSpringEncodingProvider()).isEqualTo(passwordEncoder);
                    });
                assertThat(passwordEncoder).isInstanceOf(DelegatingPasswordEncoder.class);

                assertThat(flowablePasswordEncoder.encode("test", null))
                    .as("encoded password")
                    .startsWith("{bcrypt}");

                assertThatThrownBy(() -> flowablePasswordEncoder.isMatches("test", "test", null))
                    .as("encoder matches password")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("There is no PasswordEncoder mapped for the id \"null\"");
            });
    }

    @Test
    public void standaloneIdmEngineWithDelegatingBCryptDefaultPasswordEncoder() {
        contextRunner
            .withPropertyValues("flowable.idm.password-encoder=spring_delegating_bcrypt")
            .run(context -> {
                IdmEngine idmEngine = context.getBean(IdmEngine.class);

                assertThat(context).hasSingleBean(PasswordEncoder.class);

                org.flowable.idm.api.PasswordEncoder flowablePasswordEncoder = idmEngine.getIdmEngineConfiguration().getPasswordEncoder();
                PasswordEncoder passwordEncoder = context.getBean(PasswordEncoder.class);
                assertThat(flowablePasswordEncoder)
                    .isInstanceOfSatisfying(SpringEncoder.class, springEncoder -> {
                        assertThat(springEncoder.getSpringEncodingProvider()).isEqualTo(passwordEncoder);
                    });
                assertThat(passwordEncoder).isInstanceOf(DelegatingPasswordEncoder.class);

                assertThat(flowablePasswordEncoder.encode("test", null))
                    .as("encoded password")
                    .startsWith("{bcrypt}");

                assertThat(flowablePasswordEncoder.isMatches("test", "test", null))
                    .as("encoder matchers clear text password")
                    .isFalse();

                assertThat(flowablePasswordEncoder.isMatches("test", new BCryptPasswordEncoder().encode("test"), null))
                    .as("encoder matchers only bcrypt text password")
                    .isTrue();
            });
    }

    @Test
    public void standaloneIdmEngineWithDelegatingNoopDefaultPasswordEncoder() {
        contextRunner
            .withPropertyValues("flowable.idm.password-encoder=spring_delegating_noop")
            .run(context -> {
                IdmEngine idmEngine = context.getBean(IdmEngine.class);

                assertThat(context).hasSingleBean(PasswordEncoder.class);

                org.flowable.idm.api.PasswordEncoder flowablePasswordEncoder = idmEngine.getIdmEngineConfiguration().getPasswordEncoder();
                PasswordEncoder passwordEncoder = context.getBean(PasswordEncoder.class);
                assertThat(flowablePasswordEncoder)
                    .isInstanceOfSatisfying(SpringEncoder.class, springEncoder -> {
                        assertThat(springEncoder.getSpringEncodingProvider()).isEqualTo(passwordEncoder);
                    });
                assertThat(passwordEncoder).isInstanceOf(DelegatingPasswordEncoder.class);

                assertThat(flowablePasswordEncoder.encode("test", null))
                    .as("encoded password")
                    .startsWith("{bcrypt}");

                assertThat(flowablePasswordEncoder.isMatches("test", "test", null))
                    .as("encoder matchers clear text password")
                    .isTrue();

                assertThat(flowablePasswordEncoder.isMatches("test", new BCryptPasswordEncoder().encode("test"), null))
                    .as("encoder matchers only bcrypt text password")
                    .isFalse();
            });
    }


    @Test
    public void idmEngineWithBasicDataSourceAndProcessEngine() {
        contextRunner.withConfiguration(AutoConfigurations.of(
            ProcessEngineServicesAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class
        )).run(context -> {
            assertThat(context)
                .doesNotHaveBean(AppEngine.class)
                .hasBean("idmProcessEngineConfigurationConfigurer")
                .doesNotHaveBean("idmAppEngineConfigurationConfigurer");
            ProcessEngine processEngine = context.getBean(ProcessEngine.class);
            assertThat(processEngine).as("Process engine").isNotNull();
            IdmEngineConfigurationApi idmProcessConfiguration = idmEngine(processEngine);

            IdmEngine idmEngine = context.getBean(IdmEngine.class);
            assertThat(idmEngine).as("Idm engine").isNotNull();

            assertThat(idmEngine.getIdmEngineConfiguration()).as("Idm Engine Configuration").isEqualTo(idmProcessConfiguration);

            assertAllServicesPresent(context, idmEngine);

            assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                .getBean(CustomUserEngineConfigurerConfiguration.class)
                .satisfies(configuration -> {
                    assertThat(configuration.getInvokedConfigurations())
                        .containsExactly(
                            SpringIdmEngineConfiguration.class,
                            SpringProcessEngineConfiguration.class
                        );
                });

            deleteDeployments(processEngine);
        });

    }
    
    @Test
    public void idmEngineWithBasicDataSourceAndAppEngine() {
        contextRunner.withConfiguration(AutoConfigurations.of(
            AppEngineServicesAutoConfiguration.class,
            AppEngineAutoConfiguration.class,
            ProcessEngineServicesAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class
        )).run(context -> {
            assertThat(context)
                .doesNotHaveBean("idmProcessEngineConfigurationConfigurer")
                .hasBean("idmAppEngineConfigurationConfigurer");
            AppEngine appEngine = context.getBean(AppEngine.class);
            assertThat(appEngine).as("App engine").isNotNull();
            IdmEngineConfigurationApi idmProcessConfiguration = idmEngine(appEngine);

            IdmEngine idmEngine = context.getBean(IdmEngine.class);
            assertThat(idmEngine).as("Idm engine").isNotNull();

            assertThat(idmEngine.getIdmEngineConfiguration()).as("Idm Engine Configuration").isEqualTo(idmProcessConfiguration);

            assertAllServicesPresent(context, idmEngine);

            assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                .getBean(CustomUserEngineConfigurerConfiguration.class)
                .satisfies(configuration -> {
                    assertThat(configuration.getInvokedConfigurations())
                        .containsExactly(
                            SpringProcessEngineConfiguration.class,
                            SpringIdmEngineConfiguration.class,
                            SpringAppEngineConfiguration.class
                        );
                });

            deleteDeployments(appEngine);
            deleteDeployments(context.getBean(ProcessEngine.class));
        });
    }

    private void assertAllServicesPresent(ApplicationContext context, IdmEngine idmEngine) {
        List<Method> methods = Stream.of(IdmEngine.class.getDeclaredMethods())
            .filter(method -> !("close".equals(method.getName()) || "getName".equals(method.getName())))
            .collect(Collectors.toList());

        assertThat(methods).allSatisfy(method -> {
            try {
                assertThat(context.getBean(method.getReturnType()))
                    .as(method.getReturnType() + " bean")
                    .isEqualTo(method.invoke(idmEngine));
            } catch (IllegalAccessException | InvocationTargetException e) {
                fail("Failed to invoke method " + method, e);
            }
        });
    }

    private static IdmEngineConfigurationApi idmEngine(ProcessEngine processEngine) {
        ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration();
        return EngineServiceUtil.getIdmEngineConfiguration(processEngineConfiguration);
    }
    
    private static IdmEngineConfigurationApi idmEngine(AppEngine appEngine) {
        AppEngineConfiguration appEngineConfiguration = appEngine.getAppEngineConfiguration();
        return EngineServiceUtil.getIdmEngineConfiguration(appEngineConfiguration);
    }
}
