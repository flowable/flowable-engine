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

import javax.persistence.EntityManagerFactory;

import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.common.spring.async.SpringAsyncTaskExecutor;
import org.flowable.engine.ProcessEngine;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.cmmn.Cmmn;
import org.flowable.spring.boot.cmmn.CmmnEngineAutoConfiguration;
import org.flowable.spring.boot.cmmn.CmmnEngineServicesAutoConfiguration;
import org.flowable.spring.boot.process.Process;
import org.flowable.spring.job.service.SpringAsyncExecutor;
import org.flowable.spring.job.service.SpringRejectedJobsHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * @author Filip Hrisafov
 */
public class ProcessAndCmmnEngineAsyncExecutorTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            ProcessEngineAutoConfiguration.class,
            ProcessEngineServicesAutoConfiguration.class,
            CmmnEngineAutoConfiguration.class,
            CmmnEngineServicesAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            TransactionAutoConfiguration.class
        ))
        .withPropertyValues("flowable.cmmn.deploy-resources=false", "flowable.check-process-definitions=false")
        .withClassLoader(new FilteredClassLoader(EntityManagerFactory.class));

    @Test
    public void cmmnAndProcessEngineShouldUseDistinctAsyncExecutorsWithDefaultConfiguration() {
        contextRunner.run((context -> {
            assertThat(context)
                    .hasSingleBean(ProcessEngine.class)
                    .hasSingleBean(CmmnEngine.class)
                    .hasBean("taskExecutor")
                    .hasBean("cmmnAsyncExecutor")
                    .hasBean("processAsyncExecutor");
            AsyncExecutor processAsyncExecutor = context.getBean(ProcessEngine.class).getProcessEngineConfiguration().getAsyncExecutor();
            AsyncExecutor cmmnAsyncExecutor = context.getBean(CmmnEngine.class).getCmmnEngineConfiguration().getAsyncExecutor();

            assertThat(processAsyncExecutor)
                .as("Process Engine Async Executor vs Cmmn Engine Async Executor")
                .isNotSameAs(cmmnAsyncExecutor);
            assertThat(processAsyncExecutor)
                .as("Process Engine Async Executor")
                .isInstanceOf(SpringAsyncExecutor.class);
            assertThat(cmmnAsyncExecutor)
                .as("Cmmn Engine Async Executor")
                .isInstanceOf(SpringAsyncExecutor.class);

            TaskExecutor taskExecutorBean = context.getBean("taskExecutor", TaskExecutor.class);

            assertThat(getSpringAsyncTaskExecutor(processAsyncExecutor).getAsyncTaskExecutor())
                .as("Process Async Task Executor")
                .isSameAs(taskExecutorBean);
            assertThat(getSpringAsyncTaskExecutor(cmmnAsyncExecutor).getAsyncTaskExecutor())
                .as("Cmmn Async Task Executor")
                .isSameAs(taskExecutorBean);
        }));
    }

    @Test
    public void cmmnAndProcessEngineShouldUseDistinctTaskExecutors() {
        contextRunner.withUserConfiguration(DedicatedTaskExecutorsConfiguration.class)
            .run((context -> {
                AsyncExecutor processAsyncExecutor = context.getBean(ProcessEngine.class).getProcessEngineConfiguration().getAsyncExecutor();
                AsyncExecutor cmmnAsyncExecutor = context.getBean(CmmnEngine.class).getCmmnEngineConfiguration().getAsyncExecutor();

                assertThat(context)
                    .doesNotHaveBean("taskExecutor")
                    .hasBean("cmmnTaskExecutor")
                    .hasBean("processTaskExecutor");
                TaskExecutor cmmnTaskExecutorBean = context.getBean("cmmnTaskExecutor", TaskExecutor.class);
                TaskExecutor processTaskExecutorBean = context.getBean("processTaskExecutor", TaskExecutor.class);

                assertThat(getSpringAsyncTaskExecutor(processAsyncExecutor).getAsyncTaskExecutor())
                    .as("Process Async Task Executor")
                    .isSameAs(processTaskExecutorBean)
                    .isNotSameAs(getSpringAsyncTaskExecutor(cmmnAsyncExecutor).getAsyncTaskExecutor());

                assertThat(getSpringAsyncTaskExecutor(cmmnAsyncExecutor).getAsyncTaskExecutor())
                    .as("Cmmn Async Task Executor")
                    .isSameAs(cmmnTaskExecutorBean);
            }));
    }

    @Test
    public void cmmnAndProcessEngineShouldUseDistinctTaskExecutorsWhenPrimaryIsPresent() {
        contextRunner.withUserConfiguration(DedicatedTaskExecutorsConfiguration.class, PrimaryTaskExecutorConfiguration.class)
            .run((context -> {
                AsyncExecutor processAsyncExecutor = context.getBean(ProcessEngine.class).getProcessEngineConfiguration().getAsyncExecutor();
                AsyncExecutor cmmnAsyncExecutor = context.getBean(CmmnEngine.class).getCmmnEngineConfiguration().getAsyncExecutor();

                assertThat(context)
                    .doesNotHaveBean("taskExecutor")
                    .hasBean("primaryTaskExecutor")
                    .hasBean("cmmnTaskExecutor")
                    .hasBean("processTaskExecutor");

                TaskExecutor primaryTaskExecutorBean = context.getBean("primaryTaskExecutor", TaskExecutor.class);
                TaskExecutor cmmnTaskExecutorBean = context.getBean("cmmnTaskExecutor", TaskExecutor.class);
                TaskExecutor processTaskExecutorBean = context.getBean("processTaskExecutor", TaskExecutor.class);

                assertThat(getSpringAsyncTaskExecutor(processAsyncExecutor).getAsyncTaskExecutor())
                    .as("Process Async Task Executor")
                    .isSameAs(processTaskExecutorBean)
                    .isNotSameAs(getSpringAsyncTaskExecutor(cmmnAsyncExecutor).getAsyncTaskExecutor())
                    .as("Process Async Task Executor with primary")
                    .isNotSameAs(primaryTaskExecutorBean);

                assertThat(getSpringAsyncTaskExecutor(cmmnAsyncExecutor).getAsyncTaskExecutor())
                    .as("Cmmn Async Task Executor")
                    .isSameAs(cmmnTaskExecutorBean)
                    .as("Cmmn Async Task Executor with primary")
                    .isNotSameAs(primaryTaskExecutorBean);
            }));
    }

    @Test
    public void cmmnShouldUsePrimaryAndProcessEngineShouldUseDedicatedTaskExecutor() {
        contextRunner.withUserConfiguration(ProcessTaskExecutorConfiguration.class, PrimaryTaskExecutorConfiguration.class)
            .run((context -> {
                AsyncExecutor processAsyncExecutor = context.getBean(ProcessEngine.class).getProcessEngineConfiguration().getAsyncExecutor();
                AsyncExecutor cmmnAsyncExecutor = context.getBean(CmmnEngine.class).getCmmnEngineConfiguration().getAsyncExecutor();

                assertThat(context)
                    .doesNotHaveBean("taskExecutor")
                    .hasBean("primaryTaskExecutor")
                    .hasBean("processTaskExecutor");

                TaskExecutor primaryTaskExecutorBean = context.getBean("primaryTaskExecutor", TaskExecutor.class);
                TaskExecutor processTaskExecutorBean = context.getBean("processTaskExecutor", TaskExecutor.class);

                assertThat(getSpringAsyncTaskExecutor(processAsyncExecutor).getAsyncTaskExecutor())
                    .as("Process Async Task Executor")
                    .isSameAs(processTaskExecutorBean)
                    .isNotSameAs(getSpringAsyncTaskExecutor(cmmnAsyncExecutor).getAsyncTaskExecutor());

                assertThat(getSpringAsyncTaskExecutor(cmmnAsyncExecutor).getAsyncTaskExecutor())
                    .as("Cmmn Async Task Executor")
                    .isSameAs(primaryTaskExecutorBean);
            }));
    }

    @Test
    public void cmmnAndProcessEngineShouldUseDistinctRejectedHandlers() {
        contextRunner.withUserConfiguration(DedicatedRejectedHandlerConfiguration.class)
            .run((context -> {
                AsyncExecutor processAsyncExecutor = context.getBean(ProcessEngine.class).getProcessEngineConfiguration().getAsyncExecutor();
                AsyncExecutor cmmnAsyncExecutor = context.getBean(CmmnEngine.class).getCmmnEngineConfiguration().getAsyncExecutor();

                assertThat(context)
                    .doesNotHaveBean("springRejectedJobsHandler")
                    .hasBean("cmmnRejectedJobsHandler")
                    .hasBean("processRejectedJobsHandler");

                SpringRejectedJobsHandler processRejectedJobsHandlerBean = context.getBean("processRejectedJobsHandler", SpringRejectedJobsHandler.class);
                SpringRejectedJobsHandler cmmnRejectedJobsHandlerBean = context.getBean("cmmnRejectedJobsHandler", SpringRejectedJobsHandler.class);

                assertThat(((SpringAsyncExecutor) processAsyncExecutor).getRejectedJobsHandler())
                    .as("Process Async Rejected Jobs Handler")
                    .isSameAs(processRejectedJobsHandlerBean)
                    .isNotSameAs(((SpringAsyncExecutor) cmmnAsyncExecutor).getRejectedJobsHandler());

                assertThat(((SpringAsyncExecutor) cmmnAsyncExecutor).getRejectedJobsHandler())
                    .as("Cmmn Async Rejected Jobs Handler")
                    .isSameAs(cmmnRejectedJobsHandlerBean);
            }));
    }

    @Test
    public void cmmnShouldUseDedicatedAndProcessEngineShouldUsePrimaryRejectedHandler() {
        contextRunner.withUserConfiguration(CmmnRejectedHandlerConfiguration.class, PrimaryRejectedHandlerConfiguration.class)
            .run((context -> {
                AsyncExecutor processAsyncExecutor = context.getBean(ProcessEngine.class).getProcessEngineConfiguration().getAsyncExecutor();
                AsyncExecutor cmmnAsyncExecutor = context.getBean(CmmnEngine.class).getCmmnEngineConfiguration().getAsyncExecutor();

                assertThat(context)
                    .doesNotHaveBean("springRejectedJobsHandler")
                    .hasBean("cmmnRejectedJobsHandler")
                    .hasBean("primaryRejectedJobsHandler");

                SpringRejectedJobsHandler primaryRejectedJobsHandlerBean = context.getBean("primaryRejectedJobsHandler", SpringRejectedJobsHandler.class);
                SpringRejectedJobsHandler cmmnRejectedJobsHandlerBean = context.getBean("cmmnRejectedJobsHandler", SpringRejectedJobsHandler.class);

                assertThat(((SpringAsyncExecutor) processAsyncExecutor).getRejectedJobsHandler())
                    .as("Process Async Rejected Jobs Handler")
                    .isSameAs(primaryRejectedJobsHandlerBean)
                    .isNotSameAs(((SpringAsyncExecutor) cmmnAsyncExecutor).getRejectedJobsHandler());

                assertThat(((SpringAsyncExecutor) cmmnAsyncExecutor).getRejectedJobsHandler())
                    .as("Cmmn Async Rejected Jobs Handler")
                    .isSameAs(cmmnRejectedJobsHandlerBean);
            }));
    }

    protected SpringAsyncTaskExecutor getSpringAsyncTaskExecutor(AsyncExecutor asyncExecutor) {
        return (SpringAsyncTaskExecutor) asyncExecutor.getTaskExecutor();
    }

    @Import({ CmmnTaskExecutorConfiguration.class, ProcessTaskExecutorConfiguration.class })
    @Configuration(proxyBeanMethods = false)
    static class DedicatedTaskExecutorsConfiguration {

    }

    @Configuration(proxyBeanMethods = false)
    static class CmmnTaskExecutorConfiguration {

        @Cmmn
        @Bean
        public AsyncListenableTaskExecutor cmmnTaskExecutor() {
            return new SimpleAsyncTaskExecutor();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ProcessTaskExecutorConfiguration {

        @Process
        @Bean
        public AsyncListenableTaskExecutor processTaskExecutor() {
            return new SimpleAsyncTaskExecutor();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class PrimaryTaskExecutorConfiguration {

        @Primary
        @Bean
        public AsyncListenableTaskExecutor primaryTaskExecutor() {
            return new SimpleAsyncTaskExecutor();
        }
    }

    @Import({ CmmnRejectedHandlerConfiguration.class, ProcessRejectedHandlerConfiguration.class })
    @Configuration(proxyBeanMethods = false)
    static class DedicatedRejectedHandlerConfiguration {

    }

    @Configuration(proxyBeanMethods = false)
    static class CmmnRejectedHandlerConfiguration {

        @Cmmn
        @Bean
        public SpringRejectedJobsHandler cmmnRejectedJobsHandler() {
            return (asyncExecutor, job) -> {
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ProcessRejectedHandlerConfiguration {

        @Process
        @Bean
        public SpringRejectedJobsHandler processRejectedJobsHandler() {
            return (asyncExecutor, job) -> {
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class PrimaryRejectedHandlerConfiguration {

        @Primary
        @Bean
        public SpringRejectedJobsHandler primaryRejectedJobsHandler() {
            return (asyncExecutor, job) -> {
            };
        }
    }

}
