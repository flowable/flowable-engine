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

import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.engine.ProcessEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackages = {
        "org.flowable.ui.task.extension.conf", // For custom configuration classes
        "org.flowable.ui.task.extension.bean" // For custom beans (delegates etc.)
})
public class FlowableEngineConfiguration {

    @Bean(name = "clock")
    public Clock getClock(ProcessEngine processEngine) {
        return processEngine.getProcessEngineConfiguration().getClock();
    }
}
