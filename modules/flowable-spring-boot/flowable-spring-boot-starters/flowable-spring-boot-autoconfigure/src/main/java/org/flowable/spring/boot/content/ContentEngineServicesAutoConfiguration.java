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
package org.flowable.spring.boot.content;

import org.flowable.app.engine.AppEngine;
import org.flowable.content.api.ContentManagementService;
import org.flowable.content.api.ContentService;
import org.flowable.content.engine.ContentEngine;
import org.flowable.content.engine.ContentEngines;
import org.flowable.content.spring.ContentEngineFactoryBean;
import org.flowable.content.spring.SpringContentEngineConfiguration;
import org.flowable.engine.ProcessEngine;
import org.flowable.spring.boot.BaseEngineConfigurationWithConfigurers;
import org.flowable.spring.boot.FlowableProperties;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.condition.ConditionalOnContentEngine;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-Configuration} for the Content Engine
 *
 * @author Filip Hrisafov
 */
@Configuration
@ConditionalOnContentEngine
@EnableConfigurationProperties({
    FlowableProperties.class,
    FlowableContentProperties.class
})
@AutoConfigureAfter({
    ContentEngineAutoConfiguration.class,
    AppEngineServicesAutoConfiguration.class,
    ProcessEngineServicesAutoConfiguration.class
})
public class ContentEngineServicesAutoConfiguration {

    /**
     * If a process engine is present that means that the ContentEngine was created as part of it.
     * Therefore extract it from the ContentEngines.
     */
    @Configuration
    @ConditionalOnMissingBean(type = {
        "org.flowable.content.engine.ContentEngine",
        "org.flowable.app.engine.AppEngine"
    })
    @ConditionalOnBean(type = {
        "org.flowable.engine.ProcessEngine"
    })
    static class AlreadyInitializedEngineConfiguration {

        @Bean
        public ContentEngine contentEngine(@SuppressWarnings("unused") ProcessEngine processEngine) {
            // The process engine needs to be injected, as otherwise it won't be initialized, which means that the ContentEngine is not initialized yet
            if (!ContentEngines.isInitialized()) {
                throw new IllegalStateException("Content engine has not been initialized");
            }
            return ContentEngines.getDefaultContentEngine();
        }
    }
    
    /**
     * If an app engine is present that means that the ContentEngine was created as part of it.
     * Therefore extract it from the ContentEngines.
     */
    @Configuration
    @ConditionalOnMissingBean(type = {
        "org.flowable.content.engine.ContentEngine",
    })
    @ConditionalOnBean(type = {
        "org.flowable.app.engine.AppEngine"
    })
    static class AlreadyInitializedAppEngineConfiguration {

        @Bean
        public ContentEngine contentEngine(@SuppressWarnings("unused") AppEngine appEngine) {
            // The app engine needs to be injected, as otherwise it won't be initialized, which means that the ContentEngine is not initialized yet
            if (!ContentEngines.isInitialized()) {
                throw new IllegalStateException("Content engine has not been initialized");
            }
            return ContentEngines.getDefaultContentEngine();
        }
    }

    /**
     * If there is no process engine configuration, then trigger a creation of the content engine.
     */
    @Configuration
    @ConditionalOnMissingBean(type = {
        "org.flowable.content.engine.ContentEngine",
        "org.flowable.engine.ProcessEngine",
        "org.flowable.app.engine.AppEngine"
    })
    static class StandaloneConfiguration extends BaseEngineConfigurationWithConfigurers<SpringContentEngineConfiguration> {

        @Bean
        public ContentEngineFactoryBean contentEngine(SpringContentEngineConfiguration contentEngineConfiguration) {
            ContentEngineFactoryBean factory = new ContentEngineFactoryBean();
            factory.setContentEngineConfiguration(contentEngineConfiguration);
            
            invokeConfigurers(contentEngineConfiguration);
            
            return factory;
        }
        
    }

    @Bean
    public ContentService contentService(ContentEngine contentEngine) {
        return contentEngine.getContentService();
    }

    @Bean
    public ContentManagementService contentManagementService(ContentEngine contentEngine) {
        return contentEngine.getContentManagementService();
    }
}

