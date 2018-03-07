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

import org.flowable.spring.SpringAsyncExecutor;
import org.flowable.spring.SpringCallerRunsRejectedJobsHandler;
import org.flowable.spring.SpringRejectedJobsHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * Common configuration for engines that need the job executions setup.
 *
 * @author Filip Hrisafov
 */
@Configuration
public class FlowableJobConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor();
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringAsyncExecutor springAsyncExecutor(TaskExecutor taskExecutor) {
        return new SpringAsyncExecutor(taskExecutor, springRejectedJobsHandler());
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringRejectedJobsHandler springRejectedJobsHandler() {
        return new SpringCallerRunsRejectedJobsHandler();
    }
}
