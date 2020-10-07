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
package org.flowable.test.spring.boot.cmmn;

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
import org.flowable.cmmn.api.CmmnEngineConfigurationApi;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.HttpClientConfig;
import org.flowable.cmmn.spring.SpringCmmnEngineConfiguration;
import org.flowable.cmmn.spring.autodeployment.DefaultAutoDeploymentStrategy;
import org.flowable.cmmn.spring.autodeployment.ResourceParentFolderAutoDeploymentStrategy;
import org.flowable.cmmn.spring.autodeployment.SingleResourceAutoDeploymentStrategy;
import org.flowable.common.spring.AutoDeploymentStrategy;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.util.EngineServiceUtil;
import org.flowable.http.common.api.client.FlowableAsyncHttpClient;
import org.flowable.http.common.api.client.FlowableHttpClient;
import org.flowable.idm.spring.SpringIdmEngineConfiguration;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.cmmn.CmmnEngineAutoConfiguration;
import org.flowable.spring.boot.cmmn.CmmnEngineServicesAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration;
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
import org.springframework.core.io.Resource;

/**
 * @author Filip Hrisafov
 */
public class CmmnEngineAutoConfigurationTest {

    private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            IdmEngineAutoConfiguration.class,
            IdmEngineServicesAutoConfiguration.class,
            CmmnEngineServicesAutoConfiguration.class,
            CmmnEngineAutoConfiguration.class
        ))
        .withUserConfiguration(CustomUserEngineConfigurerConfiguration.class);

    @Test
    public void httpProperties() {
        contextRunner.withPropertyValues(
            "flowable.http.useSystemProperties=true",
            "flowable.http.connectTimeout=PT0.250S",
            "flowable.http.socketTimeout=PT0.500S",
            "flowable.http.connectionRequestTimeout=PT1S",
            "flowable.http.requestRetryLimit=1",
            "flowable.http.disableCertVerify=true"
        ).run(context -> {
            CmmnEngine cmmnEngine = context.getBean(CmmnEngine.class);
            HttpClientConfig httpClientConfig = cmmnEngine.getCmmnEngineConfiguration().getHttpClientConfig();

            assertThat(httpClientConfig.isUseSystemProperties()).isTrue();
            assertThat(httpClientConfig.getConnectTimeout()).isEqualTo(250);
            assertThat(httpClientConfig.getSocketTimeout()).isEqualTo(500);
            assertThat(httpClientConfig.getConnectionRequestTimeout()).isEqualTo(1000);
            assertThat(httpClientConfig.getRequestRetryLimit()).isEqualTo(1);
            assertThat(httpClientConfig.isDisableCertVerify()).isTrue();
            assertThat(httpClientConfig.getHttpClient()).isNull();

            deleteDeployments(cmmnEngine);
        });
    }

    @Test
    public void standaloneCmmnEngineWithBasicDataSource() {
        contextRunner.run(context -> {
            assertThat(context)
                .doesNotHaveBean(AppEngine.class)
                .doesNotHaveBean(ProcessEngine.class)
                .doesNotHaveBean("cmmnProcessEngineConfigurationConfigurer")
                .doesNotHaveBean("cmmnAppEngineConfigurationConfigurer");
            CmmnEngine cmmnEngine = context.getBean(CmmnEngine.class);
            assertThat(cmmnEngine).as("Cmmn engine").isNotNull();

            assertAllServicesPresent(context, cmmnEngine);

            assertAutoDeployment(context);

            assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                .getBean(CustomUserEngineConfigurerConfiguration.class)
                .satisfies(configuration -> {
                    assertThat(configuration.getInvokedConfigurations())
                        .containsExactlyInAnyOrder(
                            SpringCmmnEngineConfiguration.class,
                            SpringIdmEngineConfiguration.class
                        );
                });

            SpringCmmnEngineConfiguration engineConfiguration = (SpringCmmnEngineConfiguration) cmmnEngine.getCmmnEngineConfiguration();
            Collection<AutoDeploymentStrategy<CmmnEngine>> deploymentStrategies = engineConfiguration.getDeploymentStrategies();

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

            deleteDeployments(cmmnEngine);
        });
    }

    @Test
    public void standaloneCmmnEngineWithBasicDataSourceAndAutoDeploymentWithLocking() {
        contextRunner
            .withPropertyValues(
                "flowable.auto-deployment.engine.cmmn.use-lock=true",
                "flowable.auto-deployment.engine.cmmn.lock-wait-time=10m",
                "flowable.auto-deployment.engine.cmmn.throw-exception-on-deployment-failure=false",
                "flowable.auto-deployment.engine.cmmn.lock-name=testLock"
            )
            .run(context -> {
                assertThat(context)
                    .doesNotHaveBean(AppEngine.class)
                    .doesNotHaveBean(ProcessEngine.class)
                    .doesNotHaveBean("cmmnProcessEngineConfigurationConfigurer")
                    .doesNotHaveBean("cmmnAppEngineConfigurationConfigurer");
                CmmnEngine cmmnEngine = context.getBean(CmmnEngine.class);
                assertThat(cmmnEngine).as("Cmmn engine").isNotNull();

                assertAllServicesPresent(context, cmmnEngine);

                assertAutoDeployment(context);

                assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                    .getBean(CustomUserEngineConfigurerConfiguration.class)
                    .satisfies(configuration -> {
                        assertThat(configuration.getInvokedConfigurations())
                            .containsExactlyInAnyOrder(
                                SpringCmmnEngineConfiguration.class,
                                SpringIdmEngineConfiguration.class
                            );
                    });

                SpringCmmnEngineConfiguration engineConfiguration = (SpringCmmnEngineConfiguration) cmmnEngine.getCmmnEngineConfiguration();
                Collection<AutoDeploymentStrategy<CmmnEngine>> deploymentStrategies = engineConfiguration.getDeploymentStrategies();

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

                deleteDeployments(cmmnEngine);
            });
    }

    @Test
    public void standaloneCmmnEngineWithBasicDataSourceAndCustomAutoDeploymentStrategies() {
        contextRunner
            .withUserConfiguration(CustomAutoDeploymentStrategyConfiguration.class)
            .withPropertyValues("flowable.auto-deployment.lock-wait-time=10m")
            .run(context -> {
                assertThat(context)
                    .doesNotHaveBean(AppEngine.class)
                    .doesNotHaveBean(ProcessEngine.class)
                    .doesNotHaveBean("cmmnProcessEngineConfigurationConfigurer")
                    .doesNotHaveBean("cmmnAppEngineConfigurationConfigurer");
                CmmnEngine cmmnEngine = context.getBean(CmmnEngine.class);
                assertThat(cmmnEngine).as("Cmmn engine").isNotNull();

                assertAllServicesPresent(context, cmmnEngine);

                assertAutoDeployment(context);

                assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                    .getBean(CustomUserEngineConfigurerConfiguration.class)
                    .satisfies(configuration -> {
                        assertThat(configuration.getInvokedConfigurations())
                            .containsExactlyInAnyOrder(
                                SpringCmmnEngineConfiguration.class,
                                SpringIdmEngineConfiguration.class
                            );
                    });

                SpringCmmnEngineConfiguration engineConfiguration = (SpringCmmnEngineConfiguration) cmmnEngine.getCmmnEngineConfiguration();
                Collection<AutoDeploymentStrategy<CmmnEngine>> deploymentStrategies = engineConfiguration.getDeploymentStrategies();

                assertThat(deploymentStrategies).element(0)
                    .isInstanceOf(TestCmmnEngineAutoDeploymentStrategy.class);
                assertThat(deploymentStrategies).element(1)
                    .isInstanceOfSatisfying(DefaultAutoDeploymentStrategy.class, strategy -> {
                        assertThat(strategy.isUseLockForDeployments()).isFalse();
                        assertThat(strategy.getDeploymentLockWaitTime()).isEqualTo(Duration.ofMinutes(10));
                    });

                assertThat(deploymentStrategies).element(2)
                    .isInstanceOfSatisfying(SingleResourceAutoDeploymentStrategy.class, strategy -> {
                        assertThat(strategy.isUseLockForDeployments()).isFalse();
                        assertThat(strategy.getDeploymentLockWaitTime()).isEqualTo(Duration.ofMinutes(10));
                    });

                assertThat(deploymentStrategies).element(3)
                    .isInstanceOfSatisfying(ResourceParentFolderAutoDeploymentStrategy.class, strategy -> {
                        assertThat(strategy.isUseLockForDeployments()).isFalse();
                        assertThat(strategy.getDeploymentLockWaitTime()).isEqualTo(Duration.ofMinutes(10));
                    });

                deleteDeployments(cmmnEngine);
            });
    }

    @Test
    public void cmmnEngineWithBasicDataSourceAndProcessEngine() {
        contextRunner.withConfiguration(AutoConfigurations.of(
            HibernateJpaAutoConfiguration.class,
            ProcessEngineServicesAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class
        )).run(context -> {
            assertThat(context)
                .doesNotHaveBean(AppEngine.class)
                .hasBean("cmmnProcessEngineConfigurationConfigurer")
                .doesNotHaveBean("cmmnAppEngineConfigurationConfigurer");
            ProcessEngine processEngine = context.getBean(ProcessEngine.class);
            assertThat(processEngine).as("Process engine").isNotNull();
            CmmnEngineConfigurationApi cmmnProcessConfigurationApi = cmmnEngine(processEngine);

            CmmnEngine cmmnEngine = context.getBean(CmmnEngine.class);
            assertThat(cmmnEngine.getCmmnEngineConfiguration()).as("Cmmn Engine Configuration").isEqualTo(cmmnProcessConfigurationApi);
            assertThat(cmmnEngine).as("Cmmn engine").isNotNull();

            assertAllServicesPresent(context, cmmnEngine);
            assertAutoDeployment(context);

            assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                .getBean(CustomUserEngineConfigurerConfiguration.class)
                .satisfies(configuration -> {
                    assertThat(configuration.getInvokedConfigurations())
                        .containsExactlyInAnyOrder(
                            SpringCmmnEngineConfiguration.class,
                            SpringIdmEngineConfiguration.class,
                            SpringProcessEngineConfiguration.class
                        );
                });

            deleteDeployments(processEngine);
            deleteDeployments(cmmnEngine);
        });
    }

    @Test
    public void cmmnEngineWithBasicDataSourceAndProcessEngineAndCustomAutoDeploymentStrategies() {
        contextRunner.withConfiguration(AutoConfigurations.of(
            HibernateJpaAutoConfiguration.class,
            ProcessEngineServicesAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class
        )).withUserConfiguration(CustomAutoDeploymentStrategyConfiguration.class, CustomProcessAutoDeploymentStrategyConfiguration.class)
            .run(context -> {
                assertThat(context)
                    .doesNotHaveBean(AppEngine.class)
                    .hasBean("cmmnProcessEngineConfigurationConfigurer")
                    .doesNotHaveBean("cmmnAppEngineConfigurationConfigurer");
                ProcessEngine processEngine = context.getBean(ProcessEngine.class);
                assertThat(processEngine).as("Process engine").isNotNull();
                CmmnEngineConfigurationApi cmmnProcessConfigurationApi = cmmnEngine(processEngine);

                CmmnEngine cmmnEngine = context.getBean(CmmnEngine.class);
                assertThat(cmmnEngine.getCmmnEngineConfiguration()).as("Cmmn Engine Configuration").isEqualTo(cmmnProcessConfigurationApi);
                assertThat(cmmnEngine).as("Cmmn engine").isNotNull();

                assertAllServicesPresent(context, cmmnEngine);
                assertAutoDeployment(context);

                assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                    .getBean(CustomUserEngineConfigurerConfiguration.class)
                    .satisfies(configuration -> {
                        assertThat(configuration.getInvokedConfigurations())
                            .containsExactlyInAnyOrder(
                                SpringCmmnEngineConfiguration.class,
                                SpringIdmEngineConfiguration.class,
                                SpringProcessEngineConfiguration.class
                            );
                    });

                SpringCmmnEngineConfiguration cmmnEngineConfiguration = (SpringCmmnEngineConfiguration) cmmnEngine.getCmmnEngineConfiguration();
                Collection<AutoDeploymentStrategy<CmmnEngine>> cmmnDeploymentStrategies = cmmnEngineConfiguration.getDeploymentStrategies();

                assertThat(cmmnDeploymentStrategies).element(0)
                    .isInstanceOf(TestCmmnEngineAutoDeploymentStrategy.class);

                assertThat(cmmnDeploymentStrategies).element(1)
                    .isInstanceOfSatisfying(DefaultAutoDeploymentStrategy.class, strategy -> {
                        assertThat(strategy.isUseLockForDeployments()).isFalse();
                        assertThat(strategy.getDeploymentLockWaitTime()).isEqualTo(Duration.ofMinutes(5));
                    });

                assertThat(cmmnDeploymentStrategies).element(2)
                    .isInstanceOfSatisfying(SingleResourceAutoDeploymentStrategy.class, strategy -> {
                        assertThat(strategy.isUseLockForDeployments()).isFalse();
                        assertThat(strategy.getDeploymentLockWaitTime()).isEqualTo(Duration.ofMinutes(5));
                    });

                assertThat(cmmnDeploymentStrategies).element(3)
                    .isInstanceOfSatisfying(ResourceParentFolderAutoDeploymentStrategy.class, strategy -> {
                        assertThat(strategy.isUseLockForDeployments()).isFalse();
                        assertThat(strategy.getDeploymentLockWaitTime()).isEqualTo(Duration.ofMinutes(5));
                    });

                SpringProcessEngineConfiguration processEngineConfiguration = (SpringProcessEngineConfiguration) processEngine.getProcessEngineConfiguration();
                Collection<AutoDeploymentStrategy<ProcessEngine>> processDeploymentStrategies = processEngineConfiguration.getDeploymentStrategies();

                assertThat(processDeploymentStrategies).element(0)
                    .isInstanceOf(TestProcessEngineAutoDeploymentStrategy.class);

                assertThat(processDeploymentStrategies).element(1)
                    .isInstanceOfSatisfying(org.flowable.spring.configurator.DefaultAutoDeploymentStrategy.class, strategy -> {
                        assertThat(strategy.isUseLockForDeployments()).isFalse();
                        assertThat(strategy.getDeploymentLockWaitTime()).isEqualTo(Duration.ofMinutes(5));
                    });

                assertThat(processDeploymentStrategies).element(2)
                    .isInstanceOfSatisfying(org.flowable.spring.configurator.SingleResourceAutoDeploymentStrategy.class, strategy -> {
                        assertThat(strategy.isUseLockForDeployments()).isFalse();
                        assertThat(strategy.getDeploymentLockWaitTime()).isEqualTo(Duration.ofMinutes(5));
                    });

                assertThat(processDeploymentStrategies).element(3)
                    .isInstanceOfSatisfying(org.flowable.spring.configurator.ResourceParentFolderAutoDeploymentStrategy.class, strategy -> {
                        assertThat(strategy.isUseLockForDeployments()).isFalse();
                        assertThat(strategy.getDeploymentLockWaitTime()).isEqualTo(Duration.ofMinutes(5));
                    });

                deleteDeployments(processEngine);
                deleteDeployments(cmmnEngine);
            });

    }

    @Test
    public void cmmnEngineWithBasicDataSourceAndAppEngine() {
        contextRunner.withConfiguration(AutoConfigurations.of(
            HibernateJpaAutoConfiguration.class,
            AppEngineServicesAutoConfiguration.class,
            AppEngineAutoConfiguration.class,
            ProcessEngineServicesAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class
        )).run(context -> {
            assertThat(context)
                .doesNotHaveBean("cmmnProcessEngineConfigurationConfigurer")
                .hasBean("cmmnAppEngineConfigurationConfigurer");
            AppEngine appEngine = context.getBean(AppEngine.class);
            assertThat(appEngine).as("App engine").isNotNull();
            CmmnEngineConfigurationApi cmmnProcessConfigurationApi = cmmnEngine(appEngine);

            CmmnEngine cmmnEngine = context.getBean(CmmnEngine.class);
            assertThat(cmmnEngine.getCmmnEngineConfiguration()).as("Cmmn Engine Configuration").isEqualTo(cmmnProcessConfigurationApi);
            assertThat(cmmnEngine).as("Cmmn engine").isNotNull();

            assertAllServicesPresent(context, cmmnEngine);
            assertAutoDeploymentWithAppEngine(context);

            assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                .getBean(CustomUserEngineConfigurerConfiguration.class)
                .satisfies(configuration -> {
                    assertThat(configuration.getInvokedConfigurations())
                        .containsExactlyInAnyOrder(
                            SpringProcessEngineConfiguration.class,
                            SpringCmmnEngineConfiguration.class,
                            SpringIdmEngineConfiguration.class,
                            SpringAppEngineConfiguration.class
                        );
                });

            deleteDeployments(appEngine);
            deleteDeployments(context.getBean(ProcessEngine.class));
            deleteDeployments(cmmnEngine);
        });
    }

    @Test
    public void cmmnEngineWithCustomHttpClient() {
        contextRunner.withUserConfiguration(CustomHttpClientConfiguration.class)
                .run(context -> {
                    assertThat(context)
                            .as("CMMN engine").hasSingleBean(CmmnEngine.class)
                            .as("Http Client").hasSingleBean(FlowableHttpClient.class);

                    CmmnEngine cmmnEngine = context.getBean(CmmnEngine.class);

                    CmmnEngineConfiguration engineConfiguration = cmmnEngine.getCmmnEngineConfiguration();
                    assertThat(engineConfiguration.getHttpClientConfig().getHttpClient())
                            .isEqualTo(context.getBean(FlowableHttpClient.class));
                });
    }

    @Test
    public void cmmnEngineWithCustomAsyncHttpClient() {
        contextRunner.withUserConfiguration(CustomAsyncHttpClientConfiguration.class)
                .run(context -> {
                    assertThat(context)
                            .as("CMMN engine").hasSingleBean(CmmnEngine.class)
                            .as("Http Client").hasSingleBean(FlowableAsyncHttpClient.class);

                    CmmnEngine cmmnEngine = context.getBean(CmmnEngine.class);

                    CmmnEngineConfiguration engineConfiguration = cmmnEngine.getCmmnEngineConfiguration();
                    assertThat(engineConfiguration.getHttpClientConfig().getHttpClient())
                            .isEqualTo(context.getBean(FlowableAsyncHttpClient.class));
                });
    }

    private void assertAllServicesPresent(ApplicationContext context, CmmnEngine cmmnEngine) {
        List<Method> methods = Stream.of(CmmnEngine.class.getDeclaredMethods())
            .filter(method -> !(method.getReturnType().equals(void.class) || "getName".equals(method.getName()))).collect(Collectors.toList());

        assertThat(methods).allSatisfy(method -> {
            try {
                assertThat(context.getBean(method.getReturnType())).as(method.getReturnType() + " bean").isEqualTo(method.invoke(cmmnEngine));
            } catch (IllegalAccessException | InvocationTargetException e) {
                fail("Failed to invoke method " + method, e);
            }
        });
    }

    private void assertAutoDeployment(ApplicationContext context) {
        CmmnRepositoryService repositoryService = context.getBean(CmmnRepositoryService.class);

        List<CaseDefinition> caseDefinitions = repositoryService.createCaseDefinitionQuery().orderByCaseDefinitionKey().asc().list();
        assertThat(caseDefinitions)
            .extracting(CaseDefinition::getKey)
            .containsExactly("case1", "case2", "case3", "case4");
        List<CmmnDeployment> deployments = repositoryService.createDeploymentQuery().list();

        assertThat(deployments)
            .hasSize(1)
            .first()
            .satisfies(deployment -> assertThat(deployment.getName()).isEqualTo("SpringBootAutoDeployment"));
    }

    private void assertAutoDeploymentWithAppEngine(ApplicationContext context) {
        CmmnRepositoryService repositoryService = context.getBean(CmmnRepositoryService.class);

        List<CaseDefinition> caseDefinitions = repositoryService.createCaseDefinitionQuery().orderByCaseDefinitionKey().asc().list();
        assertThat(caseDefinitions)
            .extracting(CaseDefinition::getKey)
            .contains("case1", "case2", "case3", "case4", "caseB");
        
        CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().latestVersion().caseDefinitionKey("case2").singleResult();
        assertThat(caseDefinition.getVersion()).isOne();
        
        caseDefinition = repositoryService.createCaseDefinitionQuery().latestVersion().caseDefinitionKey("caseB").singleResult();
        assertThat(caseDefinition.getVersion()).isOne();
        
        List<CmmnDeployment> deployments = repositoryService.createDeploymentQuery().list();

        assertThat(deployments).hasSize(3)
            .extracting(CmmnDeployment::getName)
            .contains("SpringBootAutoDeployment", "simple.bar", "processTask.bar");
        
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
            .contains("simple.bar", "vacationRequest.zip", "processTask.bar");
    }

    private static CmmnEngineConfigurationApi cmmnEngine(ProcessEngine processEngine) {
        ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration();
        return EngineServiceUtil.getCmmnEngineConfiguration(processEngineConfiguration);
    }
    
    private static CmmnEngineConfigurationApi cmmnEngine(AppEngine appEngine) {
        AppEngineConfiguration appEngineConfiguration = appEngine.getAppEngineConfiguration();
        return EngineServiceUtil.getCmmnEngineConfiguration(appEngineConfiguration);
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomAutoDeploymentStrategyConfiguration {

        @Bean
        public TestCmmnEngineAutoDeploymentStrategy testCmmnEngineAutoDeploymentStrategy() {
            return new TestCmmnEngineAutoDeploymentStrategy();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomProcessAutoDeploymentStrategyConfiguration {

        @Bean
        public TestProcessEngineAutoDeploymentStrategy testProcessEngineAutoDeploymentStrategy() {
            return new TestProcessEngineAutoDeploymentStrategy();
        }
    }

    static class TestCmmnEngineAutoDeploymentStrategy implements AutoDeploymentStrategy<CmmnEngine> {

        @Override
        public boolean handlesMode(String mode) {
            return false;
        }

        @Override
        public void deployResources(String deploymentNameHint, Resource[] resources, CmmnEngine engine) {

        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomHttpClientConfiguration {

        @Bean
        public FlowableHttpClient customHttpClient() {
            return request -> null;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomAsyncHttpClientConfiguration {

        @Bean
        public FlowableAsyncHttpClient customAsyncHttpClient() {
            return request -> null;
        }
    }

    static class TestProcessEngineAutoDeploymentStrategy implements AutoDeploymentStrategy<ProcessEngine> {

        @Override
        public boolean handlesMode(String mode) {
            return false;
        }

        @Override
        public void deployResources(String deploymentNameHint, Resource[] resources, ProcessEngine engine) {

        }
    }
}
