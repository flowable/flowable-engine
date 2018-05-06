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
package org.flowable.spring.boot.form;

import org.flowable.app.engine.AppEngine;
import org.flowable.engine.ProcessEngine;
import org.flowable.form.api.FormManagementService;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.engine.FormEngines;
import org.flowable.form.spring.FormEngineFactoryBean;
import org.flowable.form.spring.SpringFormEngineConfiguration;
import org.flowable.spring.boot.BaseEngineConfigurationWithConfigurers;
import org.flowable.spring.boot.FlowableProperties;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.condition.ConditionalOnFormEngine;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto configuration for the form engine.
 *
 * @author Filip Hrisafov
 * @author Javier Casal
 */
@Configuration
@ConditionalOnFormEngine
@EnableConfigurationProperties({
    FlowableProperties.class,
    FlowableFormProperties.class
})
@AutoConfigureAfter({
    FormEngineAutoConfiguration.class,
    AppEngineServicesAutoConfiguration.class,
    ProcessEngineServicesAutoConfiguration.class
})
public class FormEngineServicesAutoConfiguration {


    /**
     * If a process engine is present that means that the FormEngine was created as part of it.
     * Therefore extract it from the FormEngines.
     */
    @Configuration
    @ConditionalOnMissingBean(type = {
        "org.flowable.form.engine.FormEngine",
        "org.flowable.app.engine.AppEngine"
    })
    @ConditionalOnBean(type = {
        "org.flowable.engine.ProcessEngine"
    })
    static class AlreadyInitializedFormEngineConfiguration {
        @Bean
        public FormEngine formEngine(@SuppressWarnings("unused") ProcessEngine processEngine) {
            // The process engine needs to be injected, as otherwise it won't be initialized, which means that the FormEngine is not initialized yet
            if (!FormEngines.isInitialized()) {
                throw new IllegalStateException("Form engine has not been initialized");
            }
            return FormEngines.getDefaultFormEngine();
        }
    }
    
    /**
     * If an app engine is present that means that the FormEngine was created as part of the app engine.
     * Therefore extract it from the FormEngines.
     */
    @Configuration
    @ConditionalOnMissingBean(type = {
        "org.flowable.form.engine.FormEngine"
    })
    @ConditionalOnBean(type = {
        "org.flowable.app.engine.AppEngine"
    })
    static class AlreadyInitializedAppEngineConfiguration {

        @Bean
        public FormEngine formEngine(@SuppressWarnings("unused") AppEngine appEngine) {
            // The app engine needs to be injected, as otherwise it won't be initialized, which means that the FormEngine is not initialized yet
            if (!FormEngines.isInitialized()) {
                throw new IllegalStateException("Form engine has not been initialized");
            }
            return FormEngines.getDefaultFormEngine();
        }
    }
    
    /**
     * If there is no process engine configuration, then trigger a creation of the form engine.
     */
    @Configuration
    @ConditionalOnMissingBean(type = {
        "org.flowable.form.engine.FormEngine",
        "org.flowable.engine.ProcessEngine",
        "org.flowable.app.engine.AppEngine"
    })
    static class StandaloneFormEngineConfiguration extends BaseEngineConfigurationWithConfigurers<SpringFormEngineConfiguration> {

        @Bean
        public FormEngineFactoryBean formEngine(SpringFormEngineConfiguration formEngineConfiguration) {
            FormEngineFactoryBean factory = new FormEngineFactoryBean();
            factory.setFormEngineConfiguration(formEngineConfiguration);
            
            invokeConfigurers(formEngineConfiguration);
            
            return factory;
        }
    }

    @Bean
    public FormService formService(FormEngine formEngine) {
        return formEngine.getFormService();
    }

    @Bean
    public FormRepositoryService formRepositoryService(FormEngine formEngine) {
        return formEngine.getFormRepositoryService();
    }

    @Bean
    public FormManagementService formManagementService(FormEngine formEngine) {
        return formEngine.getFormManagementService();
    }
}

