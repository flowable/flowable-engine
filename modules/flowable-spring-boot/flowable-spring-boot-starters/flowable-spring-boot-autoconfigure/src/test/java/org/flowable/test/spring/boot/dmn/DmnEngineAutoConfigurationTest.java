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
package org.flowable.test.spring.boot.dmn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
import static org.flowable.test.spring.boot.util.DeploymentCleanerUtil.deleteDeployments;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.engine.AppEngine;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.common.spring.AutoDeploymentStrategy;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.DmnEngineConfigurationApi;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.spring.SpringDmnEngineConfiguration;
import org.flowable.dmn.spring.autodeployment.DefaultAutoDeploymentStrategy;
import org.flowable.dmn.spring.autodeployment.ResourceParentFolderAutoDeploymentStrategy;
import org.flowable.dmn.spring.autodeployment.SingleResourceAutoDeploymentStrategy;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.util.EngineServiceUtil;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.dmn.DmnEngineAutoConfiguration;
import org.flowable.spring.boot.dmn.DmnEngineServicesAutoConfiguration;
import org.flowable.test.spring.boot.util.CustomUserEngineConfigurerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DmnEngineAutoConfigurationTest {

    private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            DmnEngineServicesAutoConfiguration.class,
            DmnEngineAutoConfiguration.class
        ))
        .withUserConfiguration(CustomUserEngineConfigurerConfiguration.class);
    
    @Test
    public void standaloneDmnEngineWithBasicDataSource() {
        contextRunner.run(context -> {
            assertThat(context)
                .doesNotHaveBean(AppEngine.class)
                .doesNotHaveBean(ProcessEngine.class)
                .doesNotHaveBean("dmnProcessEngineConfigurationConfigurer")
                .doesNotHaveBean("dmnAppEngineConfigurationConfigurer");
            DmnEngine dmnEngine = context.getBean(DmnEngine.class);
            assertThat(dmnEngine).as("Dmn engine").isNotNull();

            assertAllServicesPresent(context, dmnEngine);
            assertAutoDeployment(context.getBean(DmnRepositoryService.class));

            assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                .getBean(CustomUserEngineConfigurerConfiguration.class)
                .satisfies(configuration -> {
                    assertThat(configuration.getInvokedConfigurations())
                        .containsExactly(
                            SpringDmnEngineConfiguration.class
                        );
                });

            SpringDmnEngineConfiguration engineConfiguration = (SpringDmnEngineConfiguration) dmnEngine.getDmnEngineConfiguration();
            Collection<AutoDeploymentStrategy<DmnEngine>> deploymentStrategies = engineConfiguration.getDeploymentStrategies();

            assertThat(deploymentStrategies).element(0)
                .isInstanceOfSatisfying(DefaultAutoDeploymentStrategy.class, strategy -> {
                    assertThat(strategy.isUseLockForDeployments()).isFalse();
                    assertThat(strategy.getDeploymentLockWaitTime()).isEqualTo(Duration.ofMinutes(5));
                    assertThat(strategy.isThrowExceptionOnDeploymentFailure()).isTrue();
                    assertThat(strategy.getLockName()).isNull();
                });

            assertThat(deploymentStrategies).element(1)
                .isInstanceOfSatisfying(SingleResourceAutoDeploymentStrategy.class, strategy -> {
                    assertThat(strategy.isUseLockForDeployments()).isFalse();
                    assertThat(strategy.getDeploymentLockWaitTime()).isEqualTo(Duration.ofMinutes(5));
                    assertThat(strategy.isThrowExceptionOnDeploymentFailure()).isTrue();
                    assertThat(strategy.getLockName()).isNull();
                });

            assertThat(deploymentStrategies).element(2)
                .isInstanceOfSatisfying(ResourceParentFolderAutoDeploymentStrategy.class, strategy -> {
                    assertThat(strategy.isUseLockForDeployments()).isFalse();
                    assertThat(strategy.getDeploymentLockWaitTime()).isEqualTo(Duration.ofMinutes(5));
                    assertThat(strategy.isThrowExceptionOnDeploymentFailure()).isTrue();
                    assertThat(strategy.getLockName()).isNull();
                });

            deleteDeployments(dmnEngine);
        });

    }

    @Test
    public void standaloneDmnEngineWithJackson() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .run(context -> {
                    assertThat(context)
                            .doesNotHaveBean(AppEngine.class)
                            .doesNotHaveBean(ProcessEngine.class)
                            .hasSingleBean(ObjectMapper.class);
                    DmnEngine dmnEngine = context.getBean(DmnEngine.class);
                    assertThat(dmnEngine).as("Dmn engine").isNotNull();

                    assertThat(dmnEngine.getDmnEngineConfiguration().getObjectMapper()).isEqualTo(context.getBean(ObjectMapper.class));

                    deleteDeployments(dmnEngine);
                });

    }

    @Test
    public void standaloneDmnEngineWithBasicDataSourceAndAutoDeploymentWithLocking() {
        contextRunner
            .withPropertyValues(
                "flowable.auto-deployment.engine.dmn.use-lock=true",
                "flowable.auto-deployment.engine.dmn.lock-wait-time=10m",
                "flowable.auto-deployment.engine.dmn.throw-exception-on-deployment-failure=false",
                "flowable.auto-deployment.engine.dmn.lock-name=testLock"
            )
            .run(context -> {
                assertThat(context)
                    .doesNotHaveBean(AppEngine.class)
                    .doesNotHaveBean(ProcessEngine.class)
                    .doesNotHaveBean("dmnProcessEngineConfigurationConfigurer")
                    .doesNotHaveBean("dmnAppEngineConfigurationConfigurer");
                DmnEngine dmnEngine = context.getBean(DmnEngine.class);
                assertThat(dmnEngine).as("Dmn engine").isNotNull();

                assertAllServicesPresent(context, dmnEngine);
                assertAutoDeployment(context.getBean(DmnRepositoryService.class));

                assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                    .getBean(CustomUserEngineConfigurerConfiguration.class)
                    .satisfies(configuration -> {
                        assertThat(configuration.getInvokedConfigurations())
                            .containsExactly(
                                SpringDmnEngineConfiguration.class
                            );
                    });

                SpringDmnEngineConfiguration engineConfiguration = (SpringDmnEngineConfiguration) dmnEngine.getDmnEngineConfiguration();
                Collection<AutoDeploymentStrategy<DmnEngine>> deploymentStrategies = engineConfiguration.getDeploymentStrategies();

                assertThat(deploymentStrategies).element(0)
                    .isInstanceOfSatisfying(DefaultAutoDeploymentStrategy.class, strategy -> {
                        assertThat(strategy.isUseLockForDeployments()).isTrue();
                        assertThat(strategy.getDeploymentLockWaitTime()).isEqualTo(Duration.ofMinutes(10));
                        assertThat(strategy.isThrowExceptionOnDeploymentFailure()).isFalse();
                        assertThat(strategy.getLockName()).isEqualTo("testLock");
                    });

                assertThat(deploymentStrategies).element(1)
                    .isInstanceOfSatisfying(SingleResourceAutoDeploymentStrategy.class, strategy -> {
                        assertThat(strategy.isUseLockForDeployments()).isTrue();
                        assertThat(strategy.getDeploymentLockWaitTime()).isEqualTo(Duration.ofMinutes(10));
                        assertThat(strategy.isThrowExceptionOnDeploymentFailure()).isFalse();
                        assertThat(strategy.getLockName()).isEqualTo("testLock");
                    });

                assertThat(deploymentStrategies).element(2)
                    .isInstanceOfSatisfying(ResourceParentFolderAutoDeploymentStrategy.class, strategy -> {
                        assertThat(strategy.isUseLockForDeployments()).isTrue();
                        assertThat(strategy.getDeploymentLockWaitTime()).isEqualTo(Duration.ofMinutes(10));
                        assertThat(strategy.isThrowExceptionOnDeploymentFailure()).isFalse();
                        assertThat(strategy.getLockName()).isEqualTo("testLock");
                    });

                deleteDeployments(dmnEngine);
            });

    }

    @Test
    public void standaloneDmnEngineWithBasicDataSourceAndCustomAutoDeploymentStrategies() {
        contextRunner
            .withUserConfiguration(CustomAutoDeploymentStrategyConfiguration.class)
            .run(context -> {
            assertThat(context)
                .doesNotHaveBean(AppEngine.class)
                .doesNotHaveBean(ProcessEngine.class)
                .doesNotHaveBean("dmnProcessEngineConfigurationConfigurer")
                .doesNotHaveBean("dmnAppEngineConfigurationConfigurer");
            DmnEngine dmnEngine = context.getBean(DmnEngine.class);
            assertThat(dmnEngine).as("Dmn engine").isNotNull();

            assertAllServicesPresent(context, dmnEngine);
            assertAutoDeployment(context.getBean(DmnRepositoryService.class));

            assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                .getBean(CustomUserEngineConfigurerConfiguration.class)
                .satisfies(configuration -> {
                    assertThat(configuration.getInvokedConfigurations())
                        .containsExactly(
                            SpringDmnEngineConfiguration.class
                        );
                });

            SpringDmnEngineConfiguration engineConfiguration = (SpringDmnEngineConfiguration) dmnEngine.getDmnEngineConfiguration();
            Collection<AutoDeploymentStrategy<DmnEngine>> deploymentStrategies = engineConfiguration.getDeploymentStrategies();

            assertThat(deploymentStrategies).element(0)
                .isInstanceOf(TestDmnEngineAutoDeploymentStrategy.class);

            assertThat(deploymentStrategies).element(1)
                .isInstanceOfSatisfying(DefaultAutoDeploymentStrategy.class, strategy -> {
                    assertThat(strategy.isUseLockForDeployments()).isFalse();
                    assertThat(strategy.getDeploymentLockWaitTime()).isEqualTo(Duration.ofMinutes(5));
                });

            assertThat(deploymentStrategies).element(2)
                .isInstanceOfSatisfying(SingleResourceAutoDeploymentStrategy.class, strategy -> {
                    assertThat(strategy.isUseLockForDeployments()).isFalse();
                    assertThat(strategy.getDeploymentLockWaitTime()).isEqualTo(Duration.ofMinutes(5));
                });

            assertThat(deploymentStrategies).element(3)
                .isInstanceOfSatisfying(ResourceParentFolderAutoDeploymentStrategy.class, strategy -> {
                    assertThat(strategy.isUseLockForDeployments()).isFalse();
                    assertThat(strategy.getDeploymentLockWaitTime()).isEqualTo(Duration.ofMinutes(5));
                });

            deleteDeployments(dmnEngine);
        });

    }

    @Test
    public void dmnEngineWithBasicDataSourceAndProcessEngine() {
        contextRunner.withConfiguration(AutoConfigurations.of(
            ProcessEngineServicesAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class
        )).run(context -> {
            assertThat(context)
                .doesNotHaveBean(AppEngine.class)
                .hasBean("dmnProcessEngineConfigurationConfigurer")
                .doesNotHaveBean("dmnAppEngineConfigurationConfigurer");
            ProcessEngine processEngine = context.getBean(ProcessEngine.class);
            assertThat(processEngine).as("Process engine").isNotNull();
            DmnEngineConfigurationApi dmnProcessConfigurationApi = dmnEngine(processEngine);

            DmnEngine dmnEngine = context.getBean(DmnEngine.class);
            assertThat(dmnEngine.getDmnEngineConfiguration()).as("Dmn Engine Configuration").isEqualTo(dmnProcessConfigurationApi);
            assertThat(dmnEngine).as("Dmn engine").isNotNull();

            assertAllServicesPresent(context, dmnEngine);
            assertAutoDeployment(context.getBean(DmnRepositoryService.class));

            assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                .getBean(CustomUserEngineConfigurerConfiguration.class)
                .satisfies(configuration -> {
                    assertThat(configuration.getInvokedConfigurations())
                        .containsExactly(
                            SpringDmnEngineConfiguration.class,
                            SpringProcessEngineConfiguration.class
                        );
                });

            deleteDeployments(dmnEngine);
            deleteDeployments(processEngine);
        });
    }

    @Test
    public void dmnEngineWithProcessEngineAndJackson() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(
                        ProcessEngineServicesAutoConfiguration.class,
                        ProcessEngineAutoConfiguration.class,
                        JacksonAutoConfiguration.class
                ))
                .run(context -> {
                    assertThat(context)
                            .doesNotHaveBean(AppEngine.class)
                            .hasSingleBean(ProcessEngine.class)
                            .hasSingleBean(ObjectMapper.class);
                    DmnEngine dmnEngine = context.getBean(DmnEngine.class);
                    assertThat(dmnEngine).as("Dmn engine").isNotNull();
                    ProcessEngine processEngine = context.getBean(ProcessEngine.class);

                    assertThat(dmnEngine.getDmnEngineConfiguration().getObjectMapper()).isEqualTo(context.getBean(ObjectMapper.class));
                    assertThat(dmnEngine.getDmnEngineConfiguration().getObjectMapper()).isEqualTo(processEngine.getProcessEngineConfiguration().getObjectMapper());

                    deleteDeployments(dmnEngine);
                    deleteDeployments(processEngine);
                });
    }
    
    @Test
    public void dmnEngineWithBasicDataSourceAndAppEngine() {
        contextRunner.withConfiguration(AutoConfigurations.of(
            AppEngineServicesAutoConfiguration.class,
            AppEngineAutoConfiguration.class,
            ProcessEngineServicesAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class
        )).run(context -> {
            assertThat(context)
                .doesNotHaveBean("dmnProcessEngineConfigurationConfigurer")
                .hasBean("dmnAppEngineConfigurationConfigurer");
            AppEngine appEngine = context.getBean(AppEngine.class);
            assertThat(appEngine).as("app engine").isNotNull();
            DmnEngineConfigurationApi dmnProcessConfigurationApi = dmnEngine(appEngine);

            DmnEngine dmnEngine = context.getBean(DmnEngine.class);
            assertThat(dmnEngine.getDmnEngineConfiguration()).as("Dmn Engine Configuration").isEqualTo(dmnProcessConfigurationApi);
            assertThat(dmnEngine).as("Dmn engine").isNotNull();

            assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                .getBean(CustomUserEngineConfigurerConfiguration.class)
                .satisfies(configuration -> {
                    assertThat(configuration.getInvokedConfigurations())
                        .containsExactly(
                            SpringProcessEngineConfiguration.class,
                            SpringDmnEngineConfiguration.class,
                            SpringAppEngineConfiguration.class
                        );
                });

            assertAllServicesPresent(context, dmnEngine);
            assertAutoDeploymentWithAppEngine(context);

            deleteDeployments(appEngine);
            deleteDeployments(context.getBean(ProcessEngine.class));
            deleteDeployments(dmnEngine);
        });

    }

    private void assertAllServicesPresent(ApplicationContext context, DmnEngine dmnEngine) {
        List<Method> methods = Stream.of(DmnEngine.class.getDeclaredMethods())
                        .filter(method -> !("close".equals(method.getName()) || "getName".equals(method.getName()))).collect(Collectors.toList());

        assertThat(methods).allSatisfy(method -> {
            try {
                assertThat(context.getBean(method.getReturnType())).as(method.getReturnType() + " bean").isEqualTo(method.invoke(dmnEngine));
            } catch (IllegalAccessException | InvocationTargetException e) {
                fail("Failed to invoke method " + method, e);
            }
        });
    }

    protected void assertAutoDeployment(DmnRepositoryService repositoryService) {
        List<DmnDecision> definitions = repositoryService.createDecisionQuery().list();
        assertThat(definitions)
            .extracting(DmnDecision::getKey, DmnDecision::getName)
            .containsExactlyInAnyOrder(
                tuple("RiskRating", "Risk Rating Decision Table"),
                tuple("simple", "Full Decision"),
                tuple("strings1", "Simple decision"),
                tuple("strings2", "Simple decision"),
                tuple("decisionService13", "Decision Service 1_3")
            );
    }

    protected void assertAutoDeploymentWithAppEngine(AssertableApplicationContext context) {
        DmnRepositoryService repositoryService = context.getBean(DmnRepositoryService.class);
        List<DmnDecision> definitions = repositoryService.createDecisionQuery().list();
        assertThat(definitions)
            .extracting(DmnDecision::getKey, DmnDecision::getName)
            .containsExactlyInAnyOrder(
                tuple("RiskRating", "Risk Rating Decision Table"),
                tuple("simple", "Full Decision"),
                tuple("strings1", "Simple decision"),
                tuple("strings2", "Simple decision"),
                tuple("managerApprovalNeeded", "Manager approval needed2"),
                tuple("decisionService13", "Decision Service 1_3")
            );
        
        DmnDecision definition = repositoryService.createDecisionQuery().latestVersion().decisionKey("strings1").singleResult();
        assertThat(definition.getVersion()).isOne();
        
        definition = repositoryService.createDecisionQuery().latestVersion().decisionKey("managerApprovalNeeded").singleResult();
        assertThat(definition.getVersion()).isOne();
        
        List<DmnDeployment> deployments = repositoryService.createDeploymentQuery().list();

        assertThat(deployments).hasSize(2)
            .extracting(DmnDeployment::getName)
            .containsExactlyInAnyOrder("SpringBootAutoDeployment", "vacationRequest.zip");
        
        AppRepositoryService appRepositoryService = context.getBean(AppRepositoryService.class);
        List<AppDefinition> appDefinitions = appRepositoryService.createAppDefinitionQuery().list();
        
        assertThat(appDefinitions)
            .extracting(AppDefinition::getKey)
            .contains("simpleApp", "vacationRequestApp");
        
        AppDefinition appDefinition = appRepositoryService.createAppDefinitionQuery().latestVersion().appDefinitionKey("simpleApp").singleResult();
        assertThat(appDefinition.getVersion()).isOne();
        
        appDefinition = appRepositoryService.createAppDefinitionQuery().latestVersion().appDefinitionKey("vacationRequestApp").singleResult();
        assertThat(appDefinition.getVersion()).isOne();
        
        List<AppDeployment> appDeployments = appRepositoryService.createDeploymentQuery().list();
        assertThat(appDeployments).hasSize(3)
            .extracting(AppDeployment::getName)
            .containsExactlyInAnyOrder("simple.bar", "vacationRequest.zip", "processTask.bar");
    }

    private static DmnEngineConfigurationApi dmnEngine(ProcessEngine processEngine) {
        ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration();
        return EngineServiceUtil.getDmnEngineConfiguration(processEngineConfiguration);
    }
    
    private static DmnEngineConfigurationApi dmnEngine(AppEngine appEngine) {
        AppEngineConfiguration appEngineConfiguration = appEngine.getAppEngineConfiguration();
        return EngineServiceUtil.getDmnEngineConfiguration(appEngineConfiguration);
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomAutoDeploymentStrategyConfiguration {

        @Bean
        @Order(10)
        public TestDmnEngineAutoDeploymentStrategy testDmnEngineAutoDeploymentStrategy() {
            return new TestDmnEngineAutoDeploymentStrategy();
        }
    }

    static class TestDmnEngineAutoDeploymentStrategy implements AutoDeploymentStrategy<DmnEngine> {

        @Override
        public boolean handlesMode(String mode) {
            return false;
        }

        @Override
        public void deployResources(String deploymentNameHint, Resource[] resources, DmnEngine engine) {

        }
    }
}
