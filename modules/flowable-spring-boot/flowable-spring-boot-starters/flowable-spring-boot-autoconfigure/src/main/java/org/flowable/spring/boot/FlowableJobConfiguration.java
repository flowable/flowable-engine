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
package org.flowable.spring.boot;

import org.flowable.common.engine.api.async.AsyncTaskExecutor;
import org.flowable.common.engine.impl.async.AsyncTaskExecutorConfiguration;
import org.flowable.common.engine.impl.async.DefaultAsyncTaskExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Common configuration for engines that need the job executions setup.
 *
 * @author Filip Hrisafov
 * @author Joram Barrez
 */
@Configuration(proxyBeanMethods = false)
public class FlowableJobConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("flowable-task-Executor-");
        executor.setAwaitTerminationSeconds(30);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        return executor;
    }

    @Bean
    @ConfigurationProperties(prefix = "flowable.task-invoker")
    public AsyncTaskExecutorConfiguration flowableAsyncTaskInvokerTaskExecutorConfiguration() {
        AsyncTaskExecutorConfiguration configuration = new AsyncTaskExecutorConfiguration();
        configuration.setQueueSize(100);
        configuration.setThreadPoolNamingPattern("flowable-async-task-invoker-%d");
        return configuration;
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "flowableAsyncTaskInvokerTaskExecutor")
    public AsyncTaskExecutor flowableAsyncTaskInvokerTaskExecutor(
            @Qualifier("flowableAsyncTaskInvokerTaskExecutorConfiguration") AsyncTaskExecutorConfiguration executorConfiguration
    ) {
        return new DefaultAsyncTaskExecutor(executorConfiguration);
    }

}