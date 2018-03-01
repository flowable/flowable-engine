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

import org.flowable.content.api.ContentManagementService;
import org.flowable.content.api.ContentService;
import org.flowable.content.engine.ContentEngine;
import org.flowable.content.engine.ContentEngineConfiguration;
import org.flowable.content.engine.ContentEngines;
import org.flowable.content.spring.ContentEngineFactoryBean;
import org.flowable.engine.ProcessEngine;
import org.flowable.spring.boot.FlowableProperties;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
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
    ProcessEngineAutoConfiguration.class
})
public class ContentEngineServicesAutoConfiguration {

    /**
     * If a process engine is present that means that the ContentEngine was created as part of it.
     * Therefore extract it from the ContentEngines.
     */
    @Configuration
    @ConditionalOnMissingBean({
        ContentEngine.class
    })
    @ConditionalOnBean({
        ProcessEngine.class
    })
    static class AlreadyInitializedConfiguration {

        @Bean
        public ContentEngine contentEngine() {
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
    @ConditionalOnMissingBean({
        ContentEngine.class,
        ProcessEngine.class
    })
    static class StandaloneConfiguration {

        @Bean
        public ContentEngineFactoryBean contentEngine(ContentEngineConfiguration contentEngineConfiguration) {
            ContentEngineFactoryBean factory = new ContentEngineFactoryBean();
            factory.setContentEngineConfiguration(contentEngineConfiguration);
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

