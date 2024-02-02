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
package org.flowable.external.job.rest.conf;

import java.util.function.Consumer;

import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.CmmnEngines;
import org.flowable.cmmn.spring.configurator.SpringCmmnEngineConfigurator;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * @author Filip Hrisafov
 */
@TestConfiguration
public class CmmnEngineWithBpmnConfiguration {

    @Bean
    public CmmnEngine cmmnEngine(@SuppressWarnings("unused") ProcessEngine processEngine) {
        // The process engine needs to be injected, as otherwise it won't be initialized, which means that the CmmnEngine is not initialized yet
        if (!CmmnEngines.isInitialized()) {
            throw new IllegalStateException("cmmn engine has not been initialized");
        }
        return CmmnEngines.getDefaultCmmnEngine();
    }

    @Bean
    public SpringCmmnEngineConfigurator cmmnEngineConfigurator(CmmnEngineConfiguration cmmnEngineConfiguration) {
        SpringCmmnEngineConfigurator engineConfigurator = new SpringCmmnEngineConfigurator();
        engineConfigurator.setCmmnEngineConfiguration(cmmnEngineConfiguration);
        return engineConfigurator;
    }

    @Bean
    public Consumer<ProcessEngineConfiguration> cmmnProcessEngineConfigurator(SpringCmmnEngineConfigurator cmmnEngineConfigurator) {
        return processEngineConfiguration -> {
            processEngineConfiguration.addConfigurator(cmmnEngineConfigurator);
        };
    }

}
