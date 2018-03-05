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
package org.flowable.spring.boot.cmmn;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.CmmnEngines;
import org.flowable.cmmn.spring.CmmnEngineFactoryBean;
import org.flowable.engine.ProcessEngine;
import org.flowable.spring.boot.FlowableProperties;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.condition.ConditionalOnCmmnEngine;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration} for the CMMN Engine
 *
 * @author Filip Hrisafov
 */
@Configuration
@ConditionalOnCmmnEngine
@EnableConfigurationProperties({
    FlowableProperties.class,
    FlowableCmmnProperties.class
})
@AutoConfigureAfter({
    CmmnEngineAutoConfiguration.class,
    ProcessEngineAutoConfiguration.class
})
public class CmmnEngineServicesAutoConfiguration {

    /**
     * If a process engine is present that means that the CmmnEngine was created as part of it.
     * Therefore extract it from the CmmnEngines.
     */
    @Configuration
    @ConditionalOnMissingBean({
        CmmnEngine.class
    })
    @ConditionalOnBean({
        ProcessEngine.class
    })
    static class AlreadyInitializedEngineConfiguration {

        @Bean
        public CmmnEngine cmmnEngine() {
            if (!CmmnEngines.isInitialized()) {
                throw new IllegalStateException("CMMN engine has not been initialized");
            }
            return CmmnEngines.getDefaultCmmnEngine();
        }
    }

    /**
     * If there is no process engine configuration, then trigger a creation of the cmmn engine.
     */
    @Configuration
    @ConditionalOnMissingBean({
        CmmnEngine.class,
        ProcessEngine.class
    })
    static class StandaloneEngineConfiguration {

        @Bean
        public CmmnEngineFactoryBean cmmnEngine(CmmnEngineConfiguration cmmnEngineConfiguration) {
            CmmnEngineFactoryBean factory = new CmmnEngineFactoryBean();
            factory.setCmmnEngineConfiguration(cmmnEngineConfiguration);
            return factory;
        }
    }

    @Bean
    public CmmnRuntimeService cmmnRuntimeService(CmmnEngine cmmnEngine) {
        return cmmnEngine.getCmmnRuntimeService();
    }

    @Bean
    public CmmnTaskService cmmnTaskService(CmmnEngine cmmnEngine) {
        return cmmnEngine.getCmmnTaskService();
    }

    @Bean
    public CmmnManagementService cmmnManagementService(CmmnEngine cmmnEngine) {
        return cmmnEngine.getCmmnManagementService();
    }

    @Bean
    public CmmnRepositoryService cmmnRepositoryService(CmmnEngine cmmnEngine) {
        return cmmnEngine.getCmmnRepositoryService();
    }

    @Bean
    public CmmnHistoryService cmmnHistoryService(CmmnEngine cmmnEngine) {
        return cmmnEngine.getCmmnHistoryService();
    }
}
