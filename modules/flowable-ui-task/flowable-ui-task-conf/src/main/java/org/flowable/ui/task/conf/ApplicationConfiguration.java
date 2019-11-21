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
package org.flowable.ui.task.conf;

import org.flowable.ui.task.properties.FlowableTaskAppProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.TaskExecutorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties(FlowableTaskAppProperties.class)
@ComponentScan(basePackages = {
        "org.flowable.ui.task.conf",
        "org.flowable.ui.task.repository",
        "org.flowable.ui.task.service",
        "org.flowable.ui.task.security",
        "org.flowable.ui.task.model.component",
        "org.flowable.ui.common.conf",
        "org.flowable.ui.common.repository",
        "org.flowable.ui.common.service",
        "org.flowable.ui.common.filter",
        "org.flowable.ui.common.security" })
@EnableScheduling
@EnableAsync
public class ApplicationConfiguration {

    @Bean
    public TaskExecutorCustomizer flowableTaskTaskExecutorCustomizer() {
        return taskExecutor -> {
            // Not yet exposed via properties in Spring Boot
            taskExecutor.setAwaitTerminationSeconds(30);
            taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        };
    }
}
