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
package org.flowable.test.spring.boot.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.flowable.test.spring.boot.util.DeploymentCleanerUtil.deleteDeployments;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManagerFactory;

import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.engine.AppEngine;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.common.engine.api.async.AsyncTaskExecutor;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.persistence.StrongUuidGenerator;
import org.flowable.common.spring.AutoDeploymentStrategy;
import org.flowable.common.spring.async.SpringAsyncTaskExecutor;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.cfg.HttpClientConfig;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.db.DbIdGenerator;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.http.common.api.client.FlowableAsyncHttpClient;
import org.flowable.http.common.api.client.FlowableHttpClient;
import org.flowable.idm.spring.SpringIdmEngineConfiguration;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration;
import org.flowable.spring.boot.process.Process;
import org.flowable.spring.configurator.DefaultAutoDeploymentStrategy;
import org.flowable.spring.configurator.ResourceParentFolderAutoDeploymentStrategy;
import org.flowable.spring.configurator.SingleResourceAutoDeploymentStrategy;
import org.flowable.spring.job.service.SpringAsyncExecutor;
import org.flowable.spring.job.service.SpringAsyncHistoryExecutor;
import org.flowable.test.spring.boot.util.CustomUserEngineConfigurerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Filip Hrisafov
 */
public class ProcessEngineAutoConfigurationTest {
    
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            ProcessEngineServicesAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            TransactionAutoConfiguration.class
        ))
        .withUserConfiguration(CustomUserEngineConfigurerConfiguration.class)
        .withClassLoader(new FilteredClassLoader(EntityManagerFactory.class));

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
            assertThat(context).doesNotHaveBean(FlowableHttpClient.class);
            ProcessEngine processEngine = context.getBean(ProcessEngine.class);
            HttpClientConfig httpClientConfig = processEngine.getProcessEngineConfiguration().getHttpClientConfig();

            assertThat(httpClientConfig.isUseSystemProperties()).isTrue();
            assertThat(httpClientConfig.getConnectTimeout()).isEqualTo(250);
            assertThat(httpClientConfig.getSocketTimeout()).isEqualTo(500);
            assertThat(httpClientConfig.getConnectionRequestTimeout()).isEqualTo(1000);
            assertThat(httpClientConfig.getRequestRetryLimit()).isEqualTo(1);
            assertThat(httpClientConfig.isDisableCertVerify()).isTrue();
            assertThat(httpClientConfig.getHttpClient()).isNull();

            deleteDeployments(processEngine);
        });
    }

    @Test
    public void historyCleaningProperties() {
        contextRunner.withPropertyValues(
            "flowable.history-cleaning-cycle=0 2 * * * ?",
            "flowable.history-cleaning-after=P90D",
            "flowable.history-cleaning-batch-size=500",
            "flowable.history-cleaning-sequential=true"
        ).run(context -> {
            ProcessEngine engine = context.getBean(ProcessEngine.class);
            ProcessEngineConfiguration engineConfiguration = engine.getProcessEngineConfiguration();

            assertThat(engineConfiguration.getHistoryCleaningTimeCycleConfig()).isEqualTo("0 2 * * * ?");
            assertThat(engineConfiguration.getCleanInstancesEndedAfter()).isEqualTo(Duration.ofDays(90));
            assertThat(engineConfiguration.getCleanInstancesBatchSize()).isEqualTo(500);
            assertThat(engineConfiguration.isCleanInstancesSequentially()).isTrue();

            deleteDeployments(engine);
        });
    }

    @Test
    public void historyCleaningPropertiesBackwardsCompatible() {
        contextRunner.withPropertyValues(
            "flowable.history-cleaning-cycle=0 2 * * * ?",
            "flowable.history-cleaning-after-days=90",
            "flowable.history-cleaning-batch-size=500",
            "flowable.history-cleaning-sequential=true"
        ).run(context -> {
            ProcessEngine engine = context.getBean(ProcessEngine.class);
            ProcessEngineConfiguration engineConfiguration = engine.getProcessEngineConfiguration();

            assertThat(engineConfiguration.getHistoryCleaningTimeCycleConfig()).isEqualTo("0 2 * * * ?");
            assertThat(engineConfiguration.getCleanInstancesEndedAfter()).isEqualTo(Duration.ofDays(90));
            assertThat(engineConfiguration.getCleanInstancesBatchSize()).isEqualTo(500);
            assertThat(engineConfiguration.isCleanInstancesSequentially()).isTrue();

            deleteDeployments(engine);
        });
    }

    @Test
    public void standaloneProcessEngineWithBasicDatasource() {
        contextRunner.run(context -> {
            assertThat(context).as("Process engine")
                    .hasSingleBean(ProcessEngine.class)
                    .doesNotHaveBean(AppEngine.class)
                    .doesNotHaveBean(IdGenerator.class)
                    .doesNotHaveBean("processAppEngineConfigurationConfigurer");

            ProcessEngine processEngine = context.getBean(ProcessEngine.class);

            assertThat(processEngine.getProcessEngineConfiguration().getIdGenerator()).isInstanceOf(StrongUuidGenerator.class);

            assertAllServicesPresent(context, processEngine);
            assertAutoDeployment(context);

            assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                .getBean(CustomUserEngineConfigurerConfiguration.class)
                .satisfies(configuration -> {
                    assertThat(configuration.getInvokedConfigurations())
                        .containsExactly(
                            SpringProcessEngineConfiguration.class
                        );
                });

            SpringProcessEngineConfiguration springProcessEngineConfiguration = (SpringProcessEngineConfiguration) processEngine
                .getProcessEngineConfiguration();

            Collection<AutoDeploymentStrategy<ProcessEngine>> deploymentStrategies = springProcessEngineConfiguration.getDeploymentStrategies();

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

            assertThat(springProcessEngineConfiguration.getHistoryCleaningTimeCycleConfig()).isEqualTo("0 0 1 * * ?");
            assertThat(springProcessEngineConfiguration.getCleanInstancesEndedAfter()).isEqualTo(Duration.ofDays(365));
            assertThat(springProcessEngineConfiguration.getCleanInstancesBatchSize()).isEqualTo(100);
            assertThat(springProcessEngineConfiguration.isCleanInstancesSequentially()).isFalse();

            deleteDeployments(processEngine);
        });
    }

    @Test
    public void standaloneProcessEngineWithJackson() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).as("Process engine")
                            .hasSingleBean(ProcessEngine.class)
                            .hasSingleBean(ObjectMapper.class);

                    ProcessEngine processEngine = context.getBean(ProcessEngine.class);

                    assertThat(processEngine.getProcessEngineConfiguration().getObjectMapper()).isEqualTo(context.getBean(ObjectMapper.class));

                    deleteDeployments(processEngine);
                });
    }
    
    @Test
    public void standaloneProcessEngineWithBasicDatasourceAndAutoDeploymentWithLocking() {
        contextRunner
            .withPropertyValues(
                "flowable.auto-deployment.engine.bpmn.use-lock=true",
                "flowable.auto-deployment.engine.bpmn.lock-wait-time=10m",
                "flowable.auto-deployment.engine.bpmn.throw-exception-on-deployment-failure=false",
                "flowable.auto-deployment.engine.bpmn.lock-name=testLock"
            )
            .run(context -> {
                assertThat(context).as("Process engine")
                        .hasSingleBean(ProcessEngine.class)
                        .doesNotHaveBean(AppEngine.class)
                        .doesNotHaveBean(IdGenerator.class)
                        .doesNotHaveBean("processAppEngineConfigurationConfigurer");

                ProcessEngine processEngine = context.getBean(ProcessEngine.class);

                assertThat(processEngine.getProcessEngineConfiguration().getIdGenerator()).isInstanceOf(StrongUuidGenerator.class);

                assertAllServicesPresent(context, processEngine);
                assertAutoDeployment(context);

                assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                    .getBean(CustomUserEngineConfigurerConfiguration.class)
                    .satisfies(configuration -> {
                        assertThat(configuration.getInvokedConfigurations())
                            .containsExactly(
                                SpringProcessEngineConfiguration.class
                            );
                    });

                SpringProcessEngineConfiguration springProcessEngineConfiguration = (SpringProcessEngineConfiguration) processEngine
                    .getProcessEngineConfiguration();

                Collection<AutoDeploymentStrategy<ProcessEngine>> deploymentStrategies = springProcessEngineConfiguration.getDeploymentStrategies();

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

                deleteDeployments(processEngine);
            });
    }

    @Test
    public void standaloneProcessEngineWithBasicDatasourceAndCustomAutoDeploymentStrategies() {
        contextRunner.withUserConfiguration(CustomAutoDeploymentStrategyConfiguration.class)
            .run(context -> {
                assertThat(context).as("Process engine").hasSingleBean(ProcessEngine.class);
                assertThat(context)
                    .doesNotHaveBean(AppEngine.class)
                    .doesNotHaveBean(IdGenerator.class)
                    .doesNotHaveBean("processAppEngineConfigurationConfigurer");

                ProcessEngine processEngine = context.getBean(ProcessEngine.class);

                assertThat(processEngine.getProcessEngineConfiguration().getIdGenerator()).isInstanceOf(StrongUuidGenerator.class);

                assertAllServicesPresent(context, processEngine);
                assertAutoDeployment(context);

                assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                    .getBean(CustomUserEngineConfigurerConfiguration.class)
                    .satisfies(configuration -> {
                        assertThat(configuration.getInvokedConfigurations())
                            .containsExactly(
                                SpringProcessEngineConfiguration.class
                            );
                    });

                SpringProcessEngineConfiguration springProcessEngineConfiguration = (SpringProcessEngineConfiguration) processEngine
                    .getProcessEngineConfiguration();

                Collection<AutoDeploymentStrategy<ProcessEngine>> deploymentStrategies = springProcessEngineConfiguration.getDeploymentStrategies();

                assertThat(deploymentStrategies).element(0)
                    .isInstanceOf(TestProcessEngineAutoDeploymentStrategy.class);

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

                deleteDeployments(processEngine);
            });
    }

    @Test
    public void processEngineWithBasicDataSourceAndAppEngine() {
        contextRunner.withConfiguration(AutoConfigurations.of(
            DataSourceTransactionManagerAutoConfiguration.class,
            AppEngineServicesAutoConfiguration.class,
            AppEngineAutoConfiguration.class,
            IdmEngineAutoConfiguration.class,
            IdmEngineServicesAutoConfiguration.class
        )).run(context -> {
            assertThat(context).hasBean("processAppEngineConfigurationConfigurer");
            AppEngine appEngine = context.getBean(AppEngine.class);
            assertThat(appEngine).as("App engine").isNotNull();
            ProcessEngineConfiguration processConfiguration = processEngine(appEngine);

            ProcessEngine processEngine = context.getBean(ProcessEngine.class);
            assertThat(processEngine.getProcessEngineConfiguration()).as("Proccess Engine Configuration").isEqualTo(processConfiguration);
            assertThat(processEngine).as("Process engine").isNotNull();

            assertAllServicesPresent(context, processEngine);
            assertAutoDeploymentWithAppEngine(context);

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
            deleteDeployments(processEngine);
        });
    }

    @Test
    public void processEngineWithBasicDataSourceAndAppEngineWithJackson() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(
                        AppEngineServicesAutoConfiguration.class,
                        AppEngineAutoConfiguration.class,
                        IdmEngineAutoConfiguration.class,
                        IdmEngineServicesAutoConfiguration.class,

                        JacksonAutoConfiguration.class
                ))
                .run(context -> {
                    assertThat(context).as("Process engine")
                            .hasSingleBean(ProcessEngine.class)
                            .hasSingleBean(AppEngine.class)
                            .hasSingleBean(ObjectMapper.class);

                    ProcessEngine processEngine = context.getBean(ProcessEngine.class);
                    AppEngine appEngine = context.getBean(AppEngine.class);

                    assertThat(processEngine.getProcessEngineConfiguration().getObjectMapper()).isEqualTo(context.getBean(ObjectMapper.class));
                    assertThat(processEngine.getProcessEngineConfiguration().getObjectMapper()).isEqualTo(appEngine.getAppEngineConfiguration().getObjectMapper());

                    deleteDeployments(appEngine);
                    deleteDeployments(processEngine);
                });
    }

    @Test
    public void processEngineWithCustomIdGenerator() {
        contextRunner.withUserConfiguration(CustomIdGeneratorConfiguration.class)
            .run(context -> {
                assertThat(context).as("Process engine").hasSingleBean(ProcessEngine.class);
                assertThat(context).as("IdGenerator").doesNotHaveBean(IdGenerator.class);

                ProcessEngine processEngine = context.getBean(ProcessEngine.class);

                ProcessEngineConfiguration engineConfiguration = processEngine.getProcessEngineConfiguration();
                assertThat(engineConfiguration.getIdGenerator())
                    .isInstanceOfSatisfying(DbIdGenerator.class, dbIdGenerator -> {
                        assertThat(dbIdGenerator.getIdBlockSize()).isEqualTo(engineConfiguration.getIdBlockSize());
                        assertThat(dbIdGenerator.getCommandExecutor()).isEqualTo(engineConfiguration.getCommandExecutor());
                        assertThat(dbIdGenerator.getCommandConfig())
                            .isEqualToComparingFieldByField(engineConfiguration.getDefaultCommandConfig().transactionRequiresNew());
                    });
            });
    }

    @Test
    public void processEngineWithCustomIdGeneratorAsBean() {
        contextRunner.withUserConfiguration(CustomBeanIdGeneratorConfiguration.class)
            .run(context -> {
                assertThat(context)
                    .as("Process engine").hasSingleBean(ProcessEngine.class)
                    .as("Id generator").hasSingleBean(IdGenerator.class);

                ProcessEngine processEngine = context.getBean(ProcessEngine.class);

                ProcessEngineConfiguration engineConfiguration = processEngine.getProcessEngineConfiguration();
                assertThat(engineConfiguration.getIdGenerator())
                    .isInstanceOfSatisfying(DbIdGenerator.class, dbIdGenerator -> {
                        assertThat(dbIdGenerator.getIdBlockSize()).isEqualTo(engineConfiguration.getIdBlockSize());
                        assertThat(dbIdGenerator.getCommandExecutor()).isEqualTo(engineConfiguration.getCommandExecutor());
                        assertThat(dbIdGenerator.getCommandConfig())
                            .isEqualToComparingFieldByField(engineConfiguration.getDefaultCommandConfig().transactionRequiresNew());
                    })
                    .isEqualTo(context.getBean(IdGenerator.class));
            });
    }

    @Test
    public void processEngineWithMultipleCustomIdGeneratorsAsBean() {
        contextRunner.withUserConfiguration(
            CustomBeanIdGeneratorConfiguration.class,
            SecondCustomBeanIdGeneratorConfiguration.class
        ).run(context -> {
            assertThat(context)
                .as("Process engine").hasSingleBean(ProcessEngine.class)
                .as("Custom Id generator").hasBean("customIdGenerator")
                .as("Second Custom Id generator").hasBean("secondCustomIdGenerator");

            Map<String, IdGenerator> idGenerators = context.getBeansOfType(IdGenerator.class);
            assertThat(idGenerators).containsOnlyKeys("customIdGenerator", "secondCustomIdGenerator");

            IdGenerator customIdGenerator = idGenerators.get("customIdGenerator");
            assertThat(customIdGenerator).isInstanceOf(DbIdGenerator.class);

            IdGenerator secondCustomIdGenerator = idGenerators.get("secondCustomIdGenerator");
            assertThat(secondCustomIdGenerator).isInstanceOf(StrongUuidGenerator.class);

            ProcessEngine processEngine = context.getBean(ProcessEngine.class);

            ProcessEngineConfiguration engineConfiguration = processEngine.getProcessEngineConfiguration();
            assertThat(engineConfiguration.getIdGenerator())
                .isInstanceOf(StrongUuidGenerator.class)
                .isNotEqualTo(customIdGenerator)
                .isNotEqualTo(secondCustomIdGenerator);
        });
    }

    @Test
    public void processEngineWithMultipleCustomIdGeneratorsAndAQualifiedProcessOneAsBean() {
        contextRunner.withUserConfiguration(
            CustomBeanIdGeneratorConfiguration.class,
            SecondCustomBeanIdGeneratorConfiguration.class,
            ProcessQualifiedCustomBeanIdGeneratorConfiguration.class
        ).run(context -> {
            assertThat(context)
                .as("Process engine").hasSingleBean(ProcessEngine.class)
                .as("Custom Id generator").hasBean("customIdGenerator")
                .as("Second Custom Id generator").hasBean("secondCustomIdGenerator")
                .as("Process Custom Id generator").hasBean("processQualifiedCustomIdGenerator");

            Map<String, IdGenerator> idGenerators = context.getBeansOfType(IdGenerator.class);
            assertThat(idGenerators).containsOnlyKeys(
                "customIdGenerator",
                "secondCustomIdGenerator",
                "processQualifiedCustomIdGenerator"
            );

            IdGenerator customIdGenerator = idGenerators.get("customIdGenerator");
            assertThat(customIdGenerator).isInstanceOf(DbIdGenerator.class);

            IdGenerator secondCustomIdGenerator = idGenerators.get("secondCustomIdGenerator");
            assertThat(secondCustomIdGenerator).isInstanceOf(StrongUuidGenerator.class);

            IdGenerator processCustomIdGenerator = idGenerators.get("processQualifiedCustomIdGenerator");
            assertThat(processCustomIdGenerator).isInstanceOf(StrongUuidGenerator.class);

            ProcessEngine processEngine = context.getBean(ProcessEngine.class);

            ProcessEngineConfiguration engineConfiguration = processEngine.getProcessEngineConfiguration();
            assertThat(engineConfiguration.getIdGenerator())
                .isInstanceOf(StrongUuidGenerator.class)
                .isNotEqualTo(customIdGenerator)
                .isNotEqualTo(secondCustomIdGenerator)
                .isEqualTo(processCustomIdGenerator);
        });
    }

    @Test
    public void processEngineWithCustomHttpClient() {
        contextRunner.withUserConfiguration(CustomHttpClientConfiguration.class)
                .run(context -> {
                    assertThat(context)
                            .as("Process engine").hasSingleBean(ProcessEngine.class)
                            .as("Http Client").hasSingleBean(FlowableHttpClient.class);

                    ProcessEngine processEngine = context.getBean(ProcessEngine.class);

                    ProcessEngineConfiguration engineConfiguration = processEngine.getProcessEngineConfiguration();
                    assertThat(engineConfiguration.getHttpClientConfig().getHttpClient())
                            .isEqualTo(context.getBean(FlowableHttpClient.class));
                });
    }

    @Test
    public void processEngineWithCustomAsyncHttpClient() {
        contextRunner.withUserConfiguration(CustomAsyncHttpClientConfiguration.class)
                .run(context -> {
                    assertThat(context)
                            .as("Process engine").hasSingleBean(ProcessEngine.class)
                            .as("Http Client").hasSingleBean(FlowableAsyncHttpClient.class);

                    ProcessEngine processEngine = context.getBean(ProcessEngine.class);

                    ProcessEngineConfiguration engineConfiguration = processEngine.getProcessEngineConfiguration();
                    assertThat(engineConfiguration.getHttpClientConfig().getHttpClient())
                            .isEqualTo(context.getBean(FlowableAsyncHttpClient.class));
                });
    }

    @Test
    void processEngineShouldUseSpringTaskExecutor() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
                .run(context -> {
                    assertThat(context)
                            .hasSingleBean(ProcessEngineConfigurationImpl.class)
                            .hasSingleBean(TaskExecutor.class);

                    ProcessEngineConfigurationImpl configuration = context.getBean(ProcessEngineConfigurationImpl.class);

                    AsyncExecutor asyncExecutor = configuration.getAsyncExecutor();
                    assertThat(asyncExecutor).isInstanceOf(SpringAsyncExecutor.class);

                    AsyncTaskExecutor asyncTaskExecutor = configuration.getAsyncTaskExecutor();
                    assertThat(asyncTaskExecutor).isInstanceOf(SpringAsyncTaskExecutor.class);
                    assertThat(asyncExecutor.getTaskExecutor()).isEqualTo(asyncTaskExecutor);
                    assertThat(configuration.getAsyncHistoryTaskExecutor()).isEqualTo(asyncTaskExecutor);
                    assertThat(((SpringAsyncTaskExecutor) asyncTaskExecutor).getAsyncTaskExecutor())
                            .isEqualTo(context.getBean(TaskExecutor.class));
                });
    }

    @Test
    void processEngineDefaultMailProperties(){
        contextRunner
                .run(context -> {
                    ProcessEngine processEngine = context.getBean(ProcessEngine.class);
                    ProcessEngineConfiguration engineConfiguration = processEngine.getProcessEngineConfiguration();

                    assertThat(engineConfiguration).isNotNull();
                    assertThat(engineConfiguration.getMailServerDefaultCharset()).isEqualTo(StandardCharsets.UTF_8);
                    assertThat(engineConfiguration.getMailServerDefaultFrom()).isEqualTo("flowable@localhost");
                    assertThat(engineConfiguration.getMailServerHost()).isEqualTo("localhost");
                    assertThat(engineConfiguration.getMailServerUsername()).isNull();
                    assertThat(engineConfiguration.getMailServerPassword()).isNull();
                    assertThat(engineConfiguration.getMailServerPort()).isEqualTo(1025);
                    assertThat(engineConfiguration.getMailServerSSLPort()).isEqualTo(1465);
                    assertThat(engineConfiguration.getMailServerUseSSL()).isFalse();
                    assertThat(engineConfiguration.getMailServerUseTLS()).isFalse();
                });
    }

    @Test
    void processEngineMailProperties(){
        contextRunner
                .withPropertyValues(
                        "flowable.mail.server.host=my-server",
                        "flowable.mail.server.port=4040",
                        "flowable.mail.server.sslPort=5050",
                        "flowable.mail.server.username=username",
                        "flowable.mail.server.password=password",
                        "flowable.mail.server.defaultFrom=customfrom@localhost",
                        "flowable.mail.server.forceTo=internal@localhost",
                        "flowable.mail.server.defaultCharset=utf-16",
                        "flowable.mail.server.useSsl=true",
                        "flowable.mail.server.useTls=true"
                )
                .run(context -> {
                    ProcessEngine processEngine = context.getBean(ProcessEngine.class);
                    ProcessEngineConfiguration engineConfiguration = processEngine.getProcessEngineConfiguration();

                    assertThat(engineConfiguration).isNotNull();
                    assertThat(engineConfiguration.getMailServerHost()).isEqualTo("my-server");
                    assertThat(engineConfiguration.getMailServerPort()).isEqualTo(4040);
                    assertThat(engineConfiguration.getMailServerSSLPort()).isEqualTo(5050);
                    assertThat(engineConfiguration.getMailServerUsername()).isEqualTo("username");
                    assertThat(engineConfiguration.getMailServerPassword()).isEqualTo("password");
                    assertThat(engineConfiguration.getMailServerDefaultFrom()).isEqualTo("customfrom@localhost");
                    assertThat(engineConfiguration.getMailServerForceTo()).isEqualTo("internal@localhost");
                    assertThat(engineConfiguration.getMailServerDefaultCharset()).isEqualTo(StandardCharsets.UTF_16);
                    assertThat(engineConfiguration.getMailServerUseSSL()).isTrue();
                    assertThat(engineConfiguration.getMailServerUseTLS()).isTrue();
                });
    }

    @Test
    void customAsyncExecutorProperties() {
        contextRunner
                .withPropertyValues(
                        "flowable.check-process-definitions=false",
                        "flowable.process.async.executor.move-timer-executor-pool-size=10",
                        "flowable.process.async.executor.max-timer-jobs-per-acquisition=1024",
                        "flowable.process.async.executor.max-async-jobs-due-per-acquisition=2048",
                        "flowable.process.async.executor.default-timer-job-acquire-wait-time-in-millis=20000",
                        "flowable.process.async.executor.default-async-job-acquire-wait-time-in-millis=30000",
                        "flowable.process.async.executor.default-queue-size-full-wait-time-in-millis=15000",
                        "flowable.process.async.executor.lock-owner=test-lock-owner",
                        "flowable.process.async.executor.timer-lock-time-in-millis=7200000",
                        "flowable.process.async.executor.async-job-lock-time-in-millis=10800000",
                        "flowable.process.async.executor.async-jobs-global-lock-wait-time=PT2M",
                        "flowable.process.async.executor.async-jobs-global-lock-poll-rate=PT1S",
                        "flowable.process.async.executor.timer-lock-wait-time=PT3M",
                        "flowable.process.async.executor.timer-lock-poll-rate=PT2S",
                        "flowable.process.async.executor.reset-expired-jobs-interval=300000",
                        "flowable.process.async.executor.reset-expired-jobs-page-size=5"
                )
                .run(context -> {
                    assertThat(context)
                            .hasBean("processAsyncExecutor")
                            .hasSingleBean(ProcessEngineConfigurationImpl.class);

                    ProcessEngineConfigurationImpl configuration = context.getBean(ProcessEngineConfigurationImpl.class);
                    SpringAsyncExecutor executor = context.getBean("processAsyncExecutor", SpringAsyncExecutor.class);

                    assertThat(configuration.getAsyncExecutor()).isEqualTo(executor);

                    assertThat(executor.getMoveTimerExecutorPoolSize()).isEqualTo(10);
                    assertThat(executor.getMaxTimerJobsPerAcquisition()).isEqualTo(1024);
                    assertThat(executor.getMaxAsyncJobsDuePerAcquisition()).isEqualTo(2048);
                    assertThat(executor.getDefaultTimerJobAcquireWaitTimeInMillis()).isEqualTo(Duration.ofSeconds(20).toMillis());
                    assertThat(executor.getDefaultAsyncJobAcquireWaitTimeInMillis()).isEqualTo(Duration.ofSeconds(30).toMillis());
                    assertThat(executor.getDefaultQueueSizeFullWaitTimeInMillis()).isEqualTo(Duration.ofSeconds(15).toMillis());
                    assertThat(executor.getLockOwner()).isEqualTo("test-lock-owner");
                    assertThat(executor.getTimerLockTimeInMillis()).isEqualTo(Duration.ofHours(2).toMillis());
                    assertThat(executor.getAsyncJobLockTimeInMillis()).isEqualTo(Duration.ofHours(3).toMillis());
                    assertThat(executor.getAsyncJobsGlobalLockWaitTime()).isEqualTo(Duration.ofMinutes(2));
                    assertThat(executor.getAsyncJobsGlobalLockPollRate()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(executor.getTimerLockWaitTime()).isEqualTo(Duration.ofMinutes(3));
                    assertThat(executor.getTimerLockPollRate()).isEqualTo(Duration.ofSeconds(2));
                    assertThat(executor.getResetExpiredJobsInterval()).isEqualTo(Duration.ofMinutes(5).toMillis());
                    assertThat(executor.getResetExpiredJobsPageSize()).isEqualTo(5);
                });
    }

    @Test
    void customAsyncExecutorPropertiesWithNewPropertiesWithDuration() {
        contextRunner
                .withPropertyValues(
                        "flowable.check-process-definitions=false",
                        "flowable.process.async.executor.move-timer-executor-pool-size=10",
                        "flowable.process.async.executor.max-timer-jobs-per-acquisition=1024",
                        "flowable.process.async.executor.max-async-jobs-due-per-acquisition=2048",
                        "flowable.process.async.executor.default-timer-job-acquire-wait-time=PT20S",
                        "flowable.process.async.executor.default-async-job-acquire-wait-time=PT30S",
                        "flowable.process.async.executor.default-queue-size-full-wait-time=PT15S",
                        "flowable.process.async.executor.lock-owner=test-lock-owner",
                        "flowable.process.async.executor.timer-lock-time=PT2H",
                        "flowable.process.async.executor.async-job-lock-time=PT3H",
                        "flowable.process.async.executor.async-jobs-global-lock-wait-time=PT2M",
                        "flowable.process.async.executor.async-jobs-global-lock-poll-rate=PT1S",
                        "flowable.process.async.executor.timer-lock-wait-time=PT3M",
                        "flowable.process.async.executor.timer-lock-poll-rate=PT2S",
                        "flowable.process.async.executor.reset-expired-jobs-interval=PT5M",
                        "flowable.process.async.executor.reset-expired-jobs-page-size=5"
                )
                .run(context -> {
                    assertThat(context)
                            .hasBean("processAsyncExecutor")
                            .hasSingleBean(ProcessEngineConfigurationImpl.class);

                    ProcessEngineConfigurationImpl configuration = context.getBean(ProcessEngineConfigurationImpl.class);
                    SpringAsyncExecutor executor = context.getBean("processAsyncExecutor", SpringAsyncExecutor.class);

                    assertThat(configuration.getAsyncExecutor()).isEqualTo(executor);

                    assertThat(executor.getMoveTimerExecutorPoolSize()).isEqualTo(10);
                    assertThat(executor.getMaxTimerJobsPerAcquisition()).isEqualTo(1024);
                    assertThat(executor.getMaxAsyncJobsDuePerAcquisition()).isEqualTo(2048);
                    assertThat(executor.getDefaultTimerJobAcquireWaitTimeInMillis()).isEqualTo(Duration.ofSeconds(20).toMillis());
                    assertThat(executor.getDefaultAsyncJobAcquireWaitTimeInMillis()).isEqualTo(Duration.ofSeconds(30).toMillis());
                    assertThat(executor.getDefaultQueueSizeFullWaitTimeInMillis()).isEqualTo(Duration.ofSeconds(15).toMillis());
                    assertThat(executor.getLockOwner()).isEqualTo("test-lock-owner");
                    assertThat(executor.getTimerLockTimeInMillis()).isEqualTo(Duration.ofHours(2).toMillis());
                    assertThat(executor.getAsyncJobLockTimeInMillis()).isEqualTo(Duration.ofHours(3).toMillis());
                    assertThat(executor.getAsyncJobsGlobalLockWaitTime()).isEqualTo(Duration.ofMinutes(2));
                    assertThat(executor.getAsyncJobsGlobalLockPollRate()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(executor.getTimerLockWaitTime()).isEqualTo(Duration.ofMinutes(3));
                    assertThat(executor.getTimerLockPollRate()).isEqualTo(Duration.ofSeconds(2));
                    assertThat(executor.getResetExpiredJobsInterval()).isEqualTo(Duration.ofMinutes(5).toMillis());
                    assertThat(executor.getResetExpiredJobsPageSize()).isEqualTo(5);
                });
    }

    @Test
    void customAsyncHistoryExecutorProperties() {
        contextRunner
                .withPropertyValues(
                        "flowable.check-process-definitions=false",
                        "flowable.process.async-history.enable=true",
                        "flowable.process.async-history.executor.max-async-jobs-due-per-acquisition=2048",
                        "flowable.process.async-history.executor.default-async-job-acquire-wait-time-in-millis=30000",
                        "flowable.process.async-history.executor.default-queue-size-full-wait-time-in-millis=15000",
                        "flowable.process.async-history.executor.lock-owner=test-lock-owner",
                        "flowable.process.async-history.executor.async-job-lock-time-in-millis=10800000",
                        "flowable.process.async-history.executor.async-jobs-global-lock-wait-time=PT2M",
                        "flowable.process.async-history.executor.async-jobs-global-lock-poll-rate=PT1S",
                        "flowable.process.async-history.executor.reset-expired-jobs-interval=300000",
                        "flowable.process.async-history.executor.reset-expired-jobs-page-size=5"
                )
                .run(context -> {
                    assertThat(context)
                            .hasBean("asyncHistoryExecutor")
                            .hasSingleBean(ProcessEngineConfigurationImpl.class);

                    ProcessEngineConfigurationImpl configuration = context.getBean(ProcessEngineConfigurationImpl.class);
                    SpringAsyncHistoryExecutor executor = context.getBean("asyncHistoryExecutor", SpringAsyncHistoryExecutor.class);

                    assertThat(configuration.getAsyncHistoryExecutor()).isEqualTo(executor);

                    assertThat(executor.isTimerRunnableNeeded()).isFalse();
                    assertThat(executor.getMaxAsyncJobsDuePerAcquisition()).isEqualTo(2048);
                    assertThat(executor.getDefaultAsyncJobAcquireWaitTimeInMillis()).isEqualTo(Duration.ofSeconds(30).toMillis());
                    assertThat(executor.getDefaultQueueSizeFullWaitTimeInMillis()).isEqualTo(Duration.ofSeconds(15).toMillis());
                    assertThat(executor.getLockOwner()).isEqualTo("test-lock-owner");
                    assertThat(executor.getAsyncJobLockTimeInMillis()).isEqualTo(Duration.ofHours(3).toMillis());
                    assertThat(executor.getAsyncJobsGlobalLockWaitTime()).isEqualTo(Duration.ofMinutes(2));
                    assertThat(executor.getAsyncJobsGlobalLockPollRate()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(executor.getResetExpiredJobsInterval()).isEqualTo(Duration.ofMinutes(5).toMillis());
                    assertThat(executor.getResetExpiredJobsPageSize()).isEqualTo(5);
                });
    }

    @Test
    void customAsyncHistoryExecutorPropertiesWithNewPropertiesWithDuration() {
        contextRunner
                .withPropertyValues(
                        "flowable.check-process-definitions=false",
                        "flowable.process.async-history.enable=true",
                        "flowable.process.async-history.executor.max-async-jobs-due-per-acquisition=2048",
                        "flowable.process.async-history.executor.default-async-job-acquire-wait-time=PT30S",
                        "flowable.process.async-history.executor.default-queue-size-full-wait-time=PT15S",
                        "flowable.process.async-history.executor.lock-owner=test-lock-owner",
                        "flowable.process.async-history.executor.async-job-lock-time=PT3H",
                        "flowable.process.async-history.executor.async-jobs-global-lock-wait-time=PT2M",
                        "flowable.process.async-history.executor.async-jobs-global-lock-poll-rate=PT1S",
                        "flowable.process.async-history.executor.reset-expired-jobs-interval=PT5M",
                        "flowable.process.async-history.executor.reset-expired-jobs-page-size=5"
                )
                .run(context -> {
                    assertThat(context)
                            .hasBean("asyncHistoryExecutor")
                            .hasSingleBean(ProcessEngineConfigurationImpl.class);

                    ProcessEngineConfigurationImpl configuration = context.getBean(ProcessEngineConfigurationImpl.class);
                    SpringAsyncHistoryExecutor executor = context.getBean("asyncHistoryExecutor", SpringAsyncHistoryExecutor.class);

                    assertThat(configuration.getAsyncHistoryExecutor()).isEqualTo(executor);

                    assertThat(executor.isTimerRunnableNeeded()).isFalse();
                    assertThat(executor.getMaxAsyncJobsDuePerAcquisition()).isEqualTo(2048);
                    assertThat(executor.getDefaultAsyncJobAcquireWaitTimeInMillis()).isEqualTo(Duration.ofSeconds(30).toMillis());
                    assertThat(executor.getDefaultQueueSizeFullWaitTimeInMillis()).isEqualTo(Duration.ofSeconds(15).toMillis());
                    assertThat(executor.getLockOwner()).isEqualTo("test-lock-owner");
                    assertThat(executor.getAsyncJobLockTimeInMillis()).isEqualTo(Duration.ofHours(3).toMillis());
                    assertThat(executor.getAsyncJobsGlobalLockWaitTime()).isEqualTo(Duration.ofMinutes(2));
                    assertThat(executor.getAsyncJobsGlobalLockPollRate()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(executor.getResetExpiredJobsInterval()).isEqualTo(Duration.ofMinutes(5).toMillis());
                    assertThat(executor.getResetExpiredJobsPageSize()).isEqualTo(5);
                });
    }


    private void assertAllServicesPresent(ApplicationContext context, ProcessEngine processEngine) {
        List<Method> methods = Stream.of(ProcessEngine.class.getDeclaredMethods())
            .filter(method -> !(method.getReturnType().equals(void.class) || "getName".equals(method.getName()))).collect(Collectors.toList());

        assertThat(methods).allSatisfy(method -> {
            try {
                assertThat(context.getBean(method.getReturnType())).as(method.getReturnType() + " bean").isEqualTo(method.invoke(processEngine));
            } catch (IllegalAccessException | InvocationTargetException e) {
                fail("Failed to invoke method " + method, e);
            }
        });
    }

    private void assertAutoDeployment(ApplicationContext context) {
        RepositoryService repositoryService = context.getBean(RepositoryService.class);

        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery().orderByProcessDefinitionKey().asc().list();
        assertThat(definitions)
            .extracting(ProcessDefinition::getKey)
            .containsExactly("integrationGatewayProcess", "waiter");
        List<Deployment> deployments = repositoryService.createDeploymentQuery().list();

        assertThat(deployments)
            .hasSize(1)
            .first()
            .satisfies(deployment -> assertThat(deployment.getName()).isEqualTo("SpringBootAutoDeployment"));
    }
    
    private void assertAutoDeploymentWithAppEngine(ApplicationContext context) {
        RepositoryService repositoryService = context.getBean(RepositoryService.class);

        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery().orderByProcessDefinitionKey().asc().list();
        assertThat(definitions)
            .extracting(ProcessDefinition::getKey)
            .containsExactly("inclusiveGateway", "integrationGatewayProcess", "simpleTasks", "vacationRequest", "waiter");
        
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().latestVersion().processDefinitionKey("simpleTasks").singleResult();
        assertThat(processDefinition.getVersion()).isOne();
        
        processDefinition = repositoryService.createProcessDefinitionQuery().latestVersion().processDefinitionKey("integrationGatewayProcess").singleResult();
        assertThat(processDefinition.getVersion()).isOne();
        
        List<Deployment> deployments = repositoryService.createDeploymentQuery().list();

        assertThat(deployments).hasSize(4)
            .extracting(Deployment::getName)
            .contains("SpringBootAutoDeployment", "simple.bar", "vacationRequest.zip", "processTask.bar");
        
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
    static class CustomBeanIdGeneratorConfiguration {

        @Bean
        public IdGenerator customIdGenerator() {
            return new DbIdGenerator();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class SecondCustomBeanIdGeneratorConfiguration {

        @Bean
        public IdGenerator secondCustomIdGenerator() {
            return new StrongUuidGenerator();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ProcessQualifiedCustomBeanIdGeneratorConfiguration {

        @Bean
        @Process
        public IdGenerator processQualifiedCustomIdGenerator() {
            return new StrongUuidGenerator();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomAutoDeploymentStrategyConfiguration {

        @Bean
        @Order(10)
        public TestProcessEngineAutoDeploymentStrategy testProcessEngineAutoDeploymentStrategy() {
            return new TestProcessEngineAutoDeploymentStrategy();
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
