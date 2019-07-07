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
package org.flowable.spring.test.transaction;

import javax.sql.DataSource;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.spring.ProcessEngineFactoryBean;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.flowable.task.api.Task;
import org.h2.Driver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Joram Barrez
 */
@ContextConfiguration(classes = SpringTransactionAndExceptionsTest.TestConfiguration.class)
public class SpringTransactionAndExceptionsTest extends SpringFlowableTestCase {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Test
    @Deployment
    public void testExceptionDoesRollback() {
        try {
            runtimeService.createProcessInstanceBuilder().processDefinitionKey("testProcess").start();
            fail();
        } catch (Exception e) {
            // exception expected
        }

        assertEquals(taskService.createTaskQuery().count(), 0);
    }

    @Test
    @Deployment
    public void testExceptionInRegularDelegateBean() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("testProcess").start();

        // The task should be created, as the exception is try-catched
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);

        String variable = (String) runtimeService.getVariable(processInstance.getId(), "theVariable");
        assertEquals("test", variable);
    }

    /**
     * This test starts a process instance, that has a service task which is a Spring bean that throws an exception from within a command.
     *
     * Bug that was fixed: due to the transaction mgmt in Spring, this marks the whole transaction for rollback, even if it was try-catched,
     * because the transaction interceptor is used for every command, event the nested ones.
     * (See the implementation of TransactionTemplate#execute)
     */
    @Test
    @Deployment
    public void testExceptionInNestedCommandRollsbackTransaction() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("testProcess").start();

        // The task should be created, as the service task with an exception is try-catched in the delegate.
        // However, due to a bug that's now fixed this wasn't the case and the whole process instance was rolled back.
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);

        String variable = (String) runtimeService.getVariable(processInstance.getId(), "theVariable");
        assertEquals("test", variable);
    }

    @Configuration
    @EnableTransactionManagement
    static class TestConfiguration {

        @Bean
        public DataSource dataSource() {
            SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
            dataSource.setDriverClass(Driver.class);
            dataSource.setUrl("jdbc:h2:mem:flowable-spring-command-context-exception;DB_CLOSE_DELAY=1000");
            dataSource.setUsername("sa");
            dataSource.setPassword("");
            return dataSource;
        }

        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        public SpringProcessEngineConfiguration processEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager) {
            SpringProcessEngineConfiguration processEngineConfiguration = new SpringProcessEngineConfiguration();
            processEngineConfiguration.setDataSource(dataSource);
            processEngineConfiguration.setTransactionManager(transactionManager);
            processEngineConfiguration.setDatabaseSchemaUpdate("true");
            return processEngineConfiguration;
        }

        @Bean
        public ProcessEngineFactoryBean cmmnEngine(SpringProcessEngineConfiguration processEngineConfiguration) {
            ProcessEngineFactoryBean processEngineFactoryBean = new ProcessEngineFactoryBean();
            processEngineFactoryBean.setProcessEngineConfiguration(processEngineConfiguration);
            return processEngineFactoryBean;
        }

        @Bean
        public RuntimeService runtimeService(ProcessEngine processEngine) {
            return processEngine.getRuntimeService();
        }

        @Bean
        public TaskService taskService(ProcessEngine processEngine) {
            return processEngine.getTaskService();
        }

        @Bean
        public ManagementService managementService(ProcessEngine processEngine) {
            return processEngine.getManagementService();
        }

        @Bean
        public TestDelegateBean testDelegateBean(ExceptionThrowingBean exceptionThrowingBean) {
            return new TestDelegateBean(exceptionThrowingBean);
        }

        @Bean
        public ExceptionThrowingBean exceptionThrowingBean() {
            return new ExceptionThrowingBean();
        }

        @Bean
        public TestServiceTaskBean testServiceTaskBean(ManagementService managementService, ExceptionThrowingBean exceptionThrowingBean) {
            return new TestServiceTaskBean(managementService, exceptionThrowingBean);
        }

        static class TestServiceTaskBean implements JavaDelegate {

            private ManagementService managementService;

            public TestServiceTaskBean(ManagementService managementService,
                ExceptionThrowingBean exceptionThrowingBean) {
                this.managementService = managementService;
            }

            @Override
            public void execute(DelegateExecution execution) {

                // Exception gets catched, so shouldn't lead to rolling back the whole transaction
                try {
                    managementService.executeCommand(new Command<Void>() {

                        @Override
                        public Void execute(CommandContext commandContext) {
                            throw new RuntimeException("exception from service task");
                        }

                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }

                execution.setVariable("theVariable", "test");
            }

        }

    }

    static class TestDelegateBean implements JavaDelegate {

        private ExceptionThrowingBean exceptionThrowingBean;

        public TestDelegateBean(ExceptionThrowingBean exceptionThrowingBean) {
            this.exceptionThrowingBean = exceptionThrowingBean;
        }

        @Override
        public void execute(DelegateExecution execution) {
            try {
                exceptionThrowingBean.throwException();
            } catch (Exception e) {

            }
            execution.setVariable("theVariable", "test");
        }

    }

    static class ExceptionThrowingBean {

        public void throwException() {
            throw new RuntimeException("from the exception throwing bean");
        }

    }


}
