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
package org.flowable.spring.boot.dmn;

import org.flowable.app.engine.AppEngine;
import org.flowable.dmn.api.DmnHistoryService;
import org.flowable.dmn.api.DmnManagementService;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.api.DmnRuleService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.DmnEngines;
import org.flowable.dmn.spring.DmnEngineFactoryBean;
import org.flowable.dmn.spring.SpringDmnEngineConfiguration;
import org.flowable.engine.ProcessEngine;
import org.flowable.spring.boot.BaseEngineConfigurationWithConfigurers;
import org.flowable.spring.boot.FlowableProperties;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.condition.ConditionalOnDmnEngine;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration} for the DMN Engine
 *
 * @author Filip Hrisafov
 */
@Configuration
@ConditionalOnDmnEngine
@EnableConfigurationProperties({
    FlowableProperties.class,
    FlowableDmnProperties.class
})
@AutoConfigureAfter({
    DmnEngineAutoConfiguration.class,
    AppEngineServicesAutoConfiguration.class,
    ProcessEngineServicesAutoConfiguration.class
})
public class DmnEngineServicesAutoConfiguration {

    /**
     * If a process engine is present that means that the DmnEngine was created as part of it.
     * Therefore extract it from the DmnEngines.
     */
    @Configuration
    @ConditionalOnMissingBean(type = {
        "org.flowable.dmn.engine.DmnEngine",
        "org.flowable.app.engine.AppEngine"
    })
    @ConditionalOnBean(type = {
        "org.flowable.engine.ProcessEngine"
    })
    static class AlreadyInitializedEngineConfiguration {
        @Bean
        public DmnEngine dmnEngine(@SuppressWarnings("unused") ProcessEngine processEngine) {
            // The process engine needs to be injected, as otherwise it won't be initialized, which means that the DmnEngine is not initialized yet
            if (!DmnEngines.isInitialized()) {
                throw new IllegalStateException("DMN engine has not been initialized");
            }
            return DmnEngines.getDefaultDmnEngine();
        }
    }
    
    /**
     * If an app engine is present that means that the DmnEngine was created as part of the app engine.
     * Therefore extract it from the DmnEngines.
     */
    @Configuration
    @ConditionalOnMissingBean(type = {
        "org.flowable.dmn.engine.DmnEngine"
    })
    @ConditionalOnBean(type = {
        "org.flowable.app.engine.AppEngine"
    })
    static class AlreadyInitializedAppEngineConfiguration {

        @Bean
        public DmnEngine dmnEngine(@SuppressWarnings("unused") AppEngine appEngine) {
            // The app engine needs to be injected, as otherwise it won't be initialized, which means that the DmnEngine is not initialized yet
            if (!DmnEngines.isInitialized()) {
                throw new IllegalStateException("DMN engine has not been initialized");
            }
            return DmnEngines.getDefaultDmnEngine();
        }
    }

    /**
     * If there is no process engine configuration, then trigger a creation of the dmn engine.
     */
    @Configuration
    @ConditionalOnMissingBean(type = {
        "org.flowable.dmn.engine.DmnEngine",
        "org.flowable.engine.ProcessEngine",
        "org.flowable.app.engine.AppEngine"
    })
    static class StandaloneEngineConfiguration extends BaseEngineConfigurationWithConfigurers<SpringDmnEngineConfiguration> {

        @Bean
        public DmnEngineFactoryBean dmnEngine(SpringDmnEngineConfiguration dmnEngineConfiguration) {
            DmnEngineFactoryBean factory = new DmnEngineFactoryBean();
            factory.setDmnEngineConfiguration(dmnEngineConfiguration);
            
            invokeConfigurers(dmnEngineConfiguration);
            
            return factory;
        }
    }

    @Bean
    public DmnManagementService dmnManagementService(DmnEngine dmnEngine) {
        return dmnEngine.getDmnManagementService();
    }

    @Bean
    public DmnRepositoryService dmnRepositoryService(DmnEngine dmnEngine) {
        return dmnEngine.getDmnRepositoryService();
    }

    @Bean
    public DmnRuleService dmnRuleService(DmnEngine dmnEngine) {
        return dmnEngine.getDmnRuleService();
    }

    @Bean
    public DmnHistoryService dmnHistoryService(DmnEngine dmnEngine) {
        return dmnEngine.getDmnHistoryService();
    }
}
