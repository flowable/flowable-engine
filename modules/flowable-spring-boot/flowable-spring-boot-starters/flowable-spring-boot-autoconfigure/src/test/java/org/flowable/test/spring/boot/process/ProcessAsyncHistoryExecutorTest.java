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

import javax.persistence.EntityManagerFactory;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.job.service.SpringAsyncExecutor;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.TaskExecutor;

/**
 * @author Joram Barrez
 */
public class ProcessAsyncHistoryExecutorTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            ProcessEngineServicesAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            TransactionAutoConfiguration.class
        ))
        .withClassLoader(new FilteredClassLoader(EntityManagerFactory.class));

    @Test
    public void asyncHistoryExecutorBeanAvailable() {
        contextRunner.withPropertyValues("flowable.process.async-history.enable=true").run((context -> {
            assertThat(context).hasSingleBean(ProcessEngine.class);
            assertThat(context).hasBean("taskExecutor");
            assertThat(context).hasBean("processAsyncExecutor");
            assertThat(context).hasBean("asyncHistoryExecutor");
            
            AsyncExecutor processAsyncExecutor = context.getBean(ProcessEngine.class).getProcessEngineConfiguration().getAsyncExecutor();
            assertThat(processAsyncExecutor).isNotNull();
            AsyncExecutor processAsyncHistoryExecutor = context.getBean(ProcessEngine.class).getProcessEngineConfiguration().getAsyncHistoryExecutor();
            assertThat(processAsyncHistoryExecutor).isNotNull();

            assertThat(processAsyncExecutor).isNotSameAs(processAsyncHistoryExecutor);

            TaskExecutor taskExecutorBean = context.getBean("taskExecutor", TaskExecutor.class);

            assertThat(((SpringAsyncExecutor) processAsyncExecutor).getTaskExecutor()).isSameAs(taskExecutorBean);
            assertThat(((SpringAsyncExecutor) processAsyncHistoryExecutor).getTaskExecutor()).isSameAs(taskExecutorBean);
            
            assertThat(context.getBean(ProcessEngine.class).getProcessEngineConfiguration().isAsyncExecutorActivate()).isTrue();
            assertThat(context.getBean(ProcessEngine.class).getProcessEngineConfiguration().isAsyncHistoryExecutorActivate()).isTrue();
            
            assertThat(((ProcessEngineConfigurationImpl) context.getBean(ProcessEngine.class).getProcessEngineConfiguration()).isAsyncHistoryEnabled()).isTrue();
            
        }));
    }

    @Test
    public void asyncHistoryExecutorBeanDisabled() {
        contextRunner
            .withPropertyValues("flowable.process.async-history.enable=false")
            .run((context -> {
                assertThat(context).hasSingleBean(ProcessEngine.class);
                assertThat(context).hasBean("taskExecutor");
                assertThat(context).hasBean("processAsyncExecutor");
                assertThat(context).doesNotHaveBean("asyncHistoryExecutor");

                AsyncExecutor processAsyncExecutor = context.getBean(ProcessEngine.class).getProcessEngineConfiguration().getAsyncExecutor();
                assertThat(processAsyncExecutor).isNotNull();
                AsyncExecutor processAsyncHistoryExecutor = context.getBean(ProcessEngine.class).getProcessEngineConfiguration().getAsyncHistoryExecutor();
                assertThat(processAsyncHistoryExecutor).isNull();

                assertThat(processAsyncExecutor).isNotSameAs(processAsyncHistoryExecutor);

                TaskExecutor taskExecutorBean = context.getBean("taskExecutor", TaskExecutor.class);

                assertThat(((SpringAsyncExecutor) processAsyncExecutor).getTaskExecutor()).isSameAs(taskExecutorBean);

                assertThat(context.getBean(ProcessEngine.class).getProcessEngineConfiguration().isAsyncExecutorActivate()).isTrue();
                assertThat(context.getBean(ProcessEngine.class).getProcessEngineConfiguration().isAsyncHistoryExecutorActivate()).isTrue();

                assertThat(((ProcessEngineConfigurationImpl) context.getBean(ProcessEngine.class).getProcessEngineConfiguration()).isAsyncHistoryEnabled())
                    .isFalse();

            }));
    }

    @Test
    public void asyncHistoryExecutorBeanWithDefault() {
        contextRunner
            .run((context -> {
                assertThat(context).doesNotHaveBean("asyncHistoryExecutor");

                AsyncExecutor processAsyncHistoryExecutor = context.getBean(ProcessEngine.class).getProcessEngineConfiguration().getAsyncHistoryExecutor();
                assertThat(processAsyncHistoryExecutor).isNull();

                assertThat(context.getBean(ProcessEngine.class).getProcessEngineConfiguration().isAsyncHistoryExecutorActivate()).isTrue();

                assertThat(((ProcessEngineConfigurationImpl) context.getBean(ProcessEngine.class).getProcessEngineConfiguration()).isAsyncHistoryEnabled())
                    .isFalse();

            }));
    }

}
