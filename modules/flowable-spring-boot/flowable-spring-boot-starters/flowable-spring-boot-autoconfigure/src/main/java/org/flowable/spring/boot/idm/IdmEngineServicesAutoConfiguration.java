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
package org.flowable.spring.boot.idm;

import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.IdmManagementService;
import org.flowable.idm.engine.IdmEngine;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.IdmEngines;
import org.flowable.idm.spring.IdmEngineFactoryBean;
import org.flowable.spring.boot.FlowableProperties;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.condition.ConditionalOnIdmEngine;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration} for the Idm Engine
 *
 * @author Filip Hrisafov
 */
@Configuration
@ConditionalOnIdmEngine
@EnableConfigurationProperties({
    FlowableProperties.class,
    FlowableIdmProperties.class
})
@AutoConfigureAfter({
    IdmEngineAutoConfiguration.class,
    ProcessEngineAutoConfiguration.class
})
public class IdmEngineServicesAutoConfiguration {

    /**
     * If a process engine is present that means that the IdmEngine was created as part of it.
     * Therefore extract it from the IdmEngines.
     */
    @Configuration
    @ConditionalOnMissingBean(type = {
        "org.flowable.idm.engine.IdmEngine"
    })
    @ConditionalOnBean(type = {
        "org.flowable.engine.ProcessEngine"
    })
    static class AlreadyInitializedEngineConfiguration {

        @Bean
        public IdmEngine idmEngine() {
            if (!IdmEngines.isInitialized()) {
                throw new IllegalStateException("Idm engine has not been initialized");
            }
            return IdmEngines.getDefaultIdmEngine();
        }
    }

    /**
     * If there is no process engine configuration, then trigger a creation of the idm engine.
     */
    @Configuration
    @ConditionalOnMissingBean(type = {
        "org.flowable.idm.engine.IdmEngine",
        "org.flowable.engine.ProcessEngine"
    })
    static class StandaloneEngineConfiguration {

        @Bean
        public IdmEngineFactoryBean idmEngine(IdmEngineConfiguration idmEngineConfiguration) {
            IdmEngineFactoryBean factory = new IdmEngineFactoryBean();
            factory.setIdmEngineConfiguration(idmEngineConfiguration);
            return factory;
        }
    }

    @Bean
    public IdmManagementService idmManagementService(IdmEngine idmEngine) {
        return idmEngine.getIdmManagementService();
    }

    @Bean
    public IdmIdentityService idmIdentityService(IdmEngine idmEngine) {
        return idmEngine.getIdmIdentityService();
    }
}
