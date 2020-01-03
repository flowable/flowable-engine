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
package org.flowable.spring.boot.eventregistry;

import org.flowable.app.engine.AppEngine;
import org.flowable.engine.ProcessEngine;
import org.flowable.eventregistry.api.EventManagementService;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngine;
import org.flowable.eventregistry.impl.EventRegistryEngines;
import org.flowable.eventregistry.spring.EventRegistryFactoryBean;
import org.flowable.eventregistry.spring.SpringEventRegistryEngineConfiguration;
import org.flowable.spring.boot.BaseEngineConfigurationWithConfigurers;
import org.flowable.spring.boot.FlowableProperties;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.condition.ConditionalOnEventRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto configuration for the event registry.
 *
 * @author Filip Hrisafov
 * @author Javier Casal
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnEventRegistry
@EnableConfigurationProperties({
    FlowableProperties.class,
    FlowableEventRegistryProperties.class
})
@AutoConfigureAfter({
    EventRegistryAutoConfiguration.class,
    AppEngineServicesAutoConfiguration.class,
    ProcessEngineServicesAutoConfiguration.class
})
public class EventRegistryServicesAutoConfiguration {


    /**
     * If a process engine is present that means that the EventRegistryEngine was created as part of it.
     * Therefore extract it from the EventRegistryEngines.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(type = {
        "org.flowable.eventregistry.impl.EventRegistryEngine",
        "org.flowable.app.engine.AppEngine"
    })
    @ConditionalOnBean(type = {
        "org.flowable.engine.ProcessEngine"
    })
    static class AlreadyInitializedEventRegistryConfiguration {
        @Bean
        public EventRegistryEngine eventRegistryEngine(@SuppressWarnings("unused") ProcessEngine processEngine) {
            // The process engine needs to be injected, as otherwise it won't be initialized, which means that the EventRegistryEngine is not initialized yet
            if (!EventRegistryEngines.isInitialized()) {
                throw new IllegalStateException("Event registry has not been initialized");
            }
            return EventRegistryEngines.getDefaultEventRegistryEngine();
        }
    }
    
    /**
     * If an app engine is present that means that the EventRegistryEngine was created as part of the app engine.
     * Therefore extract it from the EventRegistryEngines.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(type = {
        "org.flowable.eventregistry.impl.EventRegistryEngine"
    })
    @ConditionalOnBean(type = {
        "org.flowable.app.engine.AppEngine"
    })
    static class AlreadyInitializedAppEngineConfiguration {

        @Bean
        public EventRegistryEngine eventRegistryEngine(@SuppressWarnings("unused") AppEngine appEngine) {
            // The app engine needs to be injected, as otherwise it won't be initialized, which means that the EventRegistryEngine is not initialized yet
            if (!EventRegistryEngines.isInitialized()) {
                throw new IllegalStateException("Event registry has not been initialized");
            }
            return EventRegistryEngines.getDefaultEventRegistryEngine();
        }
    }
    
    /**
     * If there is no process engine configuration, then trigger a creation of the event registry.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(type = {
        "org.flowable.eventregistry.impl.EventRegistryEngine",
        "org.flowable.engine.ProcessEngine",
        "org.flowable.app.engine.AppEngine"
    })
    static class StandaloneEventRegistryConfiguration extends BaseEngineConfigurationWithConfigurers<SpringEventRegistryEngineConfiguration> {

        @Bean
        public EventRegistryFactoryBean formEngine(SpringEventRegistryEngineConfiguration eventEngineConfiguration) {
            EventRegistryFactoryBean factory = new EventRegistryFactoryBean();
            factory.setEventEngineConfiguration(eventEngineConfiguration);
            
            invokeConfigurers(eventEngineConfiguration);
            
            return factory;
        }
    }

    @Bean
    public EventRepositoryService eventRepositoryService(EventRegistryEngine eventRegistryEngine) {
        return eventRegistryEngine.getEventRepositoryService();
    }
    
    @Bean
    public EventManagementService eventManagementService(EventRegistryEngine eventRegistryEngine) {
        return eventRegistryEngine.getEventManagementService();
    }

    @Bean
    public EventRegistry eventRegistry(EventRegistryEngine eventRegistryEngine) {
        return eventRegistryEngine.getEventRegistry();
    }
}

