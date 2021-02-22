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
package org.flowable.test.spring.boot.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
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
import org.flowable.app.spring.autodeployment.DefaultAutoDeploymentStrategy;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.spring.AutoDeploymentStrategy;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.db.DbIdGenerator;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.idm.spring.SpringIdmEngineConfiguration;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration;
import org.flowable.task.api.Task;
import org.flowable.test.spring.boot.util.CustomUserEngineConfigurerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;

/**
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 */
public class AppEngineAutoConfigurationTest {

    private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            AppEngineServicesAutoConfiguration.class,
            AppEngineAutoConfiguration.class,
            IdmEngineAutoConfiguration.class,
            IdmEngineServicesAutoConfiguration.class
        ))
        .withUserConfiguration(CustomUserEngineConfigurerConfiguration.class);

    @Test
    public void standaloneAppEngineWithBasicDatasource() {
        contextRunner
            .run(context -> {
                AppEngine appEngine = context.getBean(AppEngine.class);
                assertThat(appEngine).as("App engine").isNotNull();

                assertAllServicesPresent(context, appEngine);
                assertAutoDeployment(context);

                SpringAppEngineConfiguration engineConfiguration = (SpringAppEngineConfiguration) appEngine.getAppEngineConfiguration();
                Collection<AutoDeploymentStrategy<AppEngine>> deploymentStrategies = engineConfiguration.getDeploymentStrategies();

                assertThat(deploymentStrategies).element(0)
                    .isInstanceOfSatisfying(DefaultAutoDeploymentStrategy.class, strategy -> {
                        assertThat(strategy.isUseLockForDeployments()).isFalse();
                        assertThat(strategy.getDeploymentLockWaitTime()).isEqualTo(Duration.ofMinutes(5));
                        assertThat(strategy.isThrowExceptionOnDeploymentFailure()).isTrue();
                        assertThat(strategy.getLockName()).isNull();
                    });

                deleteDeployments(appEngine);

                assertThat(engineConfiguration.getEngineConfigurations())
                        .containsOnlyKeys(
                                EngineConfigurationConstants.KEY_APP_ENGINE_CONFIG,
                                ScopeTypes.APP,
                                EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG,
                                ScopeTypes.EVENT_REGISTRY,
                                EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG,
                                "idm"
                        );

                assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                    .getBean(CustomUserEngineConfigurerConfiguration.class)
                    .satisfies(configuration -> {
                        assertThat(configuration.getInvokedConfigurations())
                            .containsExactly(
                                SpringIdmEngineConfiguration.class,
                                SpringAppEngineConfiguration.class
                            );
                    });
            });
    }

    @Test
    public void standaloneAppEngineWithDisabledIdmAndEventRegistryEngines() {
        contextRunner
            .withPropertyValues(
                    "flowable.eventregistry.enabled=false",
                    "flowable.idm.enabled=false"
            )
            .run(context -> {
                AppEngine appEngine = context.getBean(AppEngine.class);
                assertThat(appEngine).as("App engine").isNotNull();

                assertAllServicesPresent(context, appEngine);
                assertAutoDeployment(context);

                SpringAppEngineConfiguration engineConfiguration = (SpringAppEngineConfiguration) appEngine.getAppEngineConfiguration();
                Collection<AutoDeploymentStrategy<AppEngine>> deploymentStrategies = engineConfiguration.getDeploymentStrategies();

                assertThat(deploymentStrategies).element(0)
                    .isInstanceOfSatisfying(DefaultAutoDeploymentStrategy.class, strategy -> {
                        assertThat(strategy.isUseLockForDeployments()).isFalse();
                        assertThat(strategy.getDeploymentLockWaitTime()).isEqualTo(Duration.ofMinutes(5));
                        assertThat(strategy.isThrowExceptionOnDeploymentFailure()).isTrue();
                        assertThat(strategy.getLockName()).isNull();
                    });

                deleteDeployments(appEngine);

                assertThat(engineConfiguration.getEngineConfigurations())
                        .containsOnlyKeys(
                                EngineConfigurationConstants.KEY_APP_ENGINE_CONFIG,
                                ScopeTypes.APP
                        );

                assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                    .getBean(CustomUserEngineConfigurerConfiguration.class)
                    .satisfies(configuration -> {
                        assertThat(configuration.getInvokedConfigurations())
                            .containsExactly(
                                SpringAppEngineConfiguration.class
                            );
                    });
            });
    }

    @Test
    public void standaloneAppEngineWithBasicDatasourceAndAutoDeploymentWithLocking() {
        contextRunner
            .withPropertyValues(
                "flowable.auto-deployment.engine.app.use-lock=true",
                "flowable.auto-deployment.engine.app.lock-wait-time=10m",
                "flowable.auto-deployment.engine.app.throw-exception-on-deployment-failure=false",
                "flowable.auto-deployment.engine.app.lock-name=testLock"
            )
            .run(context -> {
                AppEngine appEngine = context.getBean(AppEngine.class);
                assertThat(appEngine).as("App engine").isNotNull();

                assertAllServicesPresent(context, appEngine);
                assertAutoDeployment(context);

                SpringAppEngineConfiguration engineConfiguration = (SpringAppEngineConfiguration) appEngine.getAppEngineConfiguration();
                Collection<AutoDeploymentStrategy<AppEngine>> deploymentStrategies = engineConfiguration.getDeploymentStrategies();

                assertThat(deploymentStrategies).element(0)
                    .isInstanceOfSatisfying(DefaultAutoDeploymentStrategy.class, strategy -> {
                        assertThat(strategy.isUseLockForDeployments()).isTrue();
                        assertThat(strategy.getDeploymentLockWaitTime()).isEqualTo(Duration.ofMinutes(10));
                        assertThat(strategy.isThrowExceptionOnDeploymentFailure()).isFalse();
                        assertThat(strategy.getLockName()).isEqualTo("testLock");
                    });

                deleteDeployments(appEngine);

                assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                    .getBean(CustomUserEngineConfigurerConfiguration.class)
                    .satisfies(configuration -> {
                        assertThat(configuration.getInvokedConfigurations())
                            .containsExactly(
                                SpringIdmEngineConfiguration.class,
                                SpringAppEngineConfiguration.class
                            );
                    });
            });
    }

    @Test
    public void standaloneAppEngineWithBasicDatasourceAndCustomAutoDeploymentStrategies() {
        contextRunner
            .withUserConfiguration(CustomAutoDeploymentStrategyConfiguration.class)
            .run(context -> {
                AppEngine appEngine = context.getBean(AppEngine.class);
                assertThat(appEngine).as("App engine").isNotNull();

                assertAllServicesPresent(context, appEngine);
                assertAutoDeployment(context);

                SpringAppEngineConfiguration engineConfiguration = (SpringAppEngineConfiguration) appEngine.getAppEngineConfiguration();
                Collection<AutoDeploymentStrategy<AppEngine>> deploymentStrategies = engineConfiguration.getDeploymentStrategies();

                assertThat(deploymentStrategies).element(0)
                    .isInstanceOf(TestAppEngineAutoDeploymentStrategy.class);

                assertThat(deploymentStrategies).element(1)
                    .isInstanceOfSatisfying(DefaultAutoDeploymentStrategy.class, strategy -> {
                        assertThat(strategy.isUseLockForDeployments()).isFalse();
                        assertThat(strategy.getDeploymentLockWaitTime()).isEqualTo(Duration.ofMinutes(5));
                    });

                deleteDeployments(appEngine);

                assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                    .getBean(CustomUserEngineConfigurerConfiguration.class)
                    .satisfies(configuration -> {
                        assertThat(configuration.getInvokedConfigurations())
                            .containsExactly(
                                SpringIdmEngineConfiguration.class,
                                SpringAppEngineConfiguration.class
                            );
                    });
            });
    }


    
    @Test
    public void appEngineWithBasicDataSourceAndProcessEngine() {
        contextRunner.withConfiguration(AutoConfigurations.of(
            ProcessEngineServicesAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class
        )).run(context -> {
            AppEngine appEngine = context.getBean(AppEngine.class);
            assertThat(appEngine).as("App engine").isNotNull();
            ProcessEngineConfiguration processConfiguration = processEngine(appEngine);

        ProcessEngine processEngine = context.getBean(ProcessEngine.class);
        ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration();
        assertThat(processEngineConfiguration).as("Proccess Engine Configuration").isEqualTo(processConfiguration);
        assertThat(processEngine).as("Process engine").isNotNull();

            assertAllServicesPresent(context, appEngine);
            assertAutoDeployment(context);

            processEngineConfiguration.getIdentityService().setAuthenticatedUserId("test");
            ProcessInstance processInstance = processEngineConfiguration.getRuntimeService().startProcessInstanceByKey("vacationRequest");
            Task task = processEngineConfiguration.getTaskService().createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();

            assertThat(appEngine.getAppEngineConfiguration().getEngineConfigurations())
                    .containsOnlyKeys(
                            EngineConfigurationConstants.KEY_APP_ENGINE_CONFIG,
                            ScopeTypes.APP,
                            EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG,
                            ScopeTypes.EVENT_REGISTRY,
                            EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG,
                            "idm",
                            EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG,
                            ScopeTypes.BPMN
                    );

            assertThat(processConfiguration.getEngineConfigurations())
                    .containsOnlyKeys(
                            EngineConfigurationConstants.KEY_APP_ENGINE_CONFIG,
                            ScopeTypes.APP,
                            EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG,
                            ScopeTypes.EVENT_REGISTRY,
                            EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG,
                            "idm",
                            EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG,
                            ScopeTypes.BPMN
                    );

            deleteDeployments(appEngine);
            deleteDeployments(processEngine);

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
        });
    }

    @Test
    public void appEngineWithBasicDataSourceAndProcessEngineAndDisabledEventRegistryEngine() {
        contextRunner
                .withPropertyValues(
                        "flowable.eventregistry.enabled=false",
                        "flowable.idm.enabled=false"
                )
                .withConfiguration(AutoConfigurations.of(
                        ProcessEngineServicesAutoConfiguration.class,
                        ProcessEngineAutoConfiguration.class
                )).run(context -> {
            AppEngine appEngine = context.getBean(AppEngine.class);
            assertThat(appEngine).as("App engine").isNotNull();
            ProcessEngineConfiguration processConfiguration = processEngine(appEngine);

            ProcessEngine processEngine = context.getBean(ProcessEngine.class);
            ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration();
            assertThat(processEngineConfiguration).as("Proccess Engine Configuration").isEqualTo(processConfiguration);
            assertThat(processEngine).as("Process engine").isNotNull();

            assertAllServicesPresent(context, appEngine);
            assertAutoDeployment(context);

            processEngineConfiguration.getIdentityService().setAuthenticatedUserId("test");
            ProcessInstance processInstance = processEngineConfiguration.getRuntimeService().startProcessInstanceByKey("vacationRequest");
            Task task = processEngineConfiguration.getTaskService().createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();

            assertThat(appEngine.getAppEngineConfiguration().getEngineConfigurations())
                    .containsOnlyKeys(
                            EngineConfigurationConstants.KEY_APP_ENGINE_CONFIG,
                            ScopeTypes.APP,
                            EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG,
                            ScopeTypes.BPMN
                    );

            assertThat(processConfiguration.getEngineConfigurations())
                    .containsOnlyKeys(
                            EngineConfigurationConstants.KEY_APP_ENGINE_CONFIG,
                            ScopeTypes.APP,
                            EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG,
                            ScopeTypes.BPMN
                    );

            deleteDeployments(appEngine);
            deleteDeployments(processEngine);

            assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                    .getBean(CustomUserEngineConfigurerConfiguration.class)
                    .satisfies(configuration -> {
                        assertThat(configuration.getInvokedConfigurations())
                                .containsExactly(
                                        SpringProcessEngineConfiguration.class,
                                        SpringAppEngineConfiguration.class
                                );
                    });
        });
    }

    @Test
    public void appEngineWithProcessEngineAndTaskIdGenerator() {
        contextRunner.withUserConfiguration(CustomIdGeneratorConfiguration.class
        ).withConfiguration(AutoConfigurations.of(
            ProcessEngineServicesAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class
        )).run(context -> {
            ProcessEngine processEngine = context.getBean(ProcessEngine.class);
            ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration();
            assertThat(processEngineConfiguration.getIdGenerator().getNextId()).as("Process id generator must be DB id generator").doesNotContain("-");
            
            AppEngine appEngine = context.getBean(AppEngine.class);
            deleteDeployments(appEngine);
            deleteDeployments(processEngine);
        });
    }

    private void assertAllServicesPresent(ApplicationContext context, AppEngine appEngine) {
        List<Method> methods = Stream.of(AppEngine.class.getDeclaredMethods())
            .filter(method -> !("close".equals(method.getName()) || "getName".equals(method.getName()))).collect(Collectors.toList());

        assertThat(methods).allSatisfy(method -> {
            try {
                assertThat(context.getBean(method.getReturnType())).as(method.getReturnType() + " bean").isEqualTo(method.invoke(appEngine));
            } catch (IllegalAccessException | InvocationTargetException e) {
                fail("Failed to invoke method " + method, e);
            }
        });
    }

    private void assertAutoDeployment(ApplicationContext context) {
        AppRepositoryService appRepositoryService = context.getBean(AppRepositoryService.class);

        List<AppDefinition> definitions = appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionKey().asc().list();
        assertThat(definitions)
            .extracting(AppDefinition::getKey)
            .containsExactlyInAnyOrder("simpleApp", "vacationRequestApp");
        List<AppDeployment> deployments = appRepositoryService.createDeploymentQuery().orderByDeploymentName().asc().list();

        assertThat(deployments)
            .hasSize(3)
            .first()
            .satisfies(deployment -> assertThat(deployment.getName()).isEqualTo("processTask.bar"));
    }
    
    private static ProcessEngineConfiguration processEngine(AppEngine appEngine) {
        AppEngineConfiguration appEngineConfiguration = appEngine.getAppEngineConfiguration();
        return (ProcessEngineConfiguration) appEngineConfiguration.getEngineConfigurations().get(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG);
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomIdGeneratorConfiguration {

        @Bean
        public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> customIdGeneratorConfigurer() {
            return engineConfiguration -> engineConfiguration.setIdGenerator(new DbIdGenerator());
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomAutoDeploymentStrategyConfiguration {

        @Bean
        @Order(10)
        public TestAppEngineAutoDeploymentStrategy testAppEngineAutoDeploymentStrategy() {
            return new TestAppEngineAutoDeploymentStrategy();
        }
    }

    static class TestAppEngineAutoDeploymentStrategy implements AutoDeploymentStrategy<AppEngine> {

        @Override
        public boolean handlesMode(String mode) {
            return false;
        }

        @Override
        public void deployResources(String deploymentNameHint, Resource[] resources, AppEngine engine) {

        }
    }

}
