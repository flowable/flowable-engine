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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManagerFactory;

import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.engine.AppEngine;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.persistence.StrongUuidGenerator;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.impl.db.DbIdGenerator;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.idm.spring.SpringIdmEngineConfiguration;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.flowable.spring.boot.FlowableTransactionAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration;
import org.flowable.test.spring.boot.util.CustomUserEngineConfigurerConfiguration;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Filip Hrisafov
 */
public class ProcessEngineAutoConfigurationTest {
    
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            ProcessEngineServicesAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class,
            FlowableTransactionAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            TransactionAutoConfiguration.class
        ))
        .withUserConfiguration(CustomUserEngineConfigurerConfiguration.class)
        .withClassLoader(new FilteredClassLoader(EntityManagerFactory.class));

    @Test
    public void standaloneProcessEngineWithBasicDatasource() {
        contextRunner.run(context -> {
            assertThat(context).as("Process engine").hasSingleBean(ProcessEngine.class);
            assertThat(context)
                .doesNotHaveBean(AppEngine.class)
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

            deleteDeployments(processEngine);
        });
    }
    
    @Test
    public void processEngineWithBasicDataSourceAndAppEngine() {
        contextRunner.withConfiguration(AutoConfigurations.of(
            DataSourceTransactionManagerAutoConfiguration.class,
            FlowableTransactionAutoConfiguration.class,
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
    public void processEngineWithCustomIdGenerator() {
        contextRunner.withUserConfiguration(CustomIdGeneratorConfiguration.class)
            .run(context -> {
                assertThat(context).as("Process engine").hasSingleBean(ProcessEngine.class);

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

    private void assertAllServicesPresent(ApplicationContext context, ProcessEngine processEngine) {
        List<Method> methods = Stream.of(ProcessEngine.class.getDeclaredMethods())
            .filter(method -> !(method.getName().equals("close") || method.getName().equals("getName"))).collect(Collectors.toList());

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
            .containsExactly("integrationGatewayProcess", "simpleTasks", "vacationRequest", "waiter");
        
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().latestVersion().processDefinitionKey("simpleTasks").singleResult();
        assertThat(processDefinition.getVersion()).isOne();
        
        processDefinition = repositoryService.createProcessDefinitionQuery().latestVersion().processDefinitionKey("integrationGatewayProcess").singleResult();
        assertThat(processDefinition.getVersion()).isOne();
        
        List<Deployment> deployments = repositoryService.createDeploymentQuery().list();

        assertThat(deployments).hasSize(3)
            .extracting(Deployment::getName)
            .contains("SpringBootAutoDeployment", "simple.bar", "vacationRequest.zip");
        
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
        assertThat(appDeployments).hasSize(2)
            .extracting(AppDeployment::getName)
            .contains("simple.bar", "vacationRequest.zip");
    }
    
    private static ProcessEngineConfiguration processEngine(AppEngine appEngine) {
        AppEngineConfiguration appEngineConfiguration = appEngine.getAppEngineConfiguration();
        return (ProcessEngineConfiguration) appEngineConfiguration.getEngineConfigurations().get(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG);
    }

    @Configuration
    static class CustomIdGeneratorConfiguration {

        @Bean
        public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> customIdGeneratorConfigurer() {
            return engineConfiguration -> engineConfiguration.setIdGenerator(new DbIdGenerator());
        }
    }
}
