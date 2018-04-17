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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManagerFactory;

import org.flowable.common.engine.impl.persistence.StrongUuidGenerator;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.impl.db.DbIdGenerator;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.flowable.spring.boot.FlowableTransactionAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
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
            ProcessEngineAutoConfiguration.class,
            FlowableTransactionAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            TransactionAutoConfiguration.class
        ))
        .withClassLoader(new FilteredClassLoader(EntityManagerFactory.class));

    @Test
    public void standaloneProcessEngineWithBasicDatasource() {
        contextRunner.run(context -> {
            assertThat(context).as("Process engine").hasSingleBean(ProcessEngine.class);

            ProcessEngine processEngine = context.getBean(ProcessEngine.class);

            assertThat(processEngine.getProcessEngineConfiguration().getIdGenerator()).isInstanceOf(StrongUuidGenerator.class);

            assertAllServicesPresent(context, processEngine);
            assertAutoDeployment(context);
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

    @Configuration
    static class CustomIdGeneratorConfiguration {

        @Bean
        public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> customIdGeneratorConfigurer() {
            return engineConfiguration -> engineConfiguration.setIdGenerator(new DbIdGenerator());
        }
    }
}
