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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.spring.AutoDeploymentStrategy;
import org.flowable.common.spring.CommonAutoDeploymentProperties;
import org.flowable.eventregistry.impl.EventRegistryEngine;
import org.flowable.eventregistry.impl.configurator.EventRegistryEngineConfigurator;
import org.flowable.eventregistry.spring.SpringEventRegistryEngineConfiguration;
import org.flowable.eventregistry.spring.autodeployment.DefaultAutoDeploymentStrategy;
import org.flowable.eventregistry.spring.autodeployment.ResourceParentFolderAutoDeploymentStrategy;
import org.flowable.eventregistry.spring.autodeployment.SingleResourceAutoDeploymentStrategy;
import org.flowable.eventregistry.spring.configurator.SpringEventRegistryConfigurator;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.AbstractSpringEngineAutoConfiguration;
import org.flowable.spring.boot.BaseEngineConfigurationWithConfigurers;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.flowable.spring.boot.FlowableAutoDeploymentProperties;
import org.flowable.spring.boot.FlowableProperties;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.condition.ConditionalOnEventRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Auto configuration for the event registry.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnEventRegistry
@EnableConfigurationProperties({
    FlowableProperties.class,
    FlowableAutoDeploymentProperties.class,
    FlowableEventProperties.class
})
@AutoConfigureAfter({
    AppEngineAutoConfiguration.class,
    ProcessEngineAutoConfiguration.class,
})
@AutoConfigureBefore({
    AppEngineServicesAutoConfiguration.class,
    ProcessEngineServicesAutoConfiguration.class,
})
public class EventRegistryAutoConfiguration extends AbstractSpringEngineAutoConfiguration {

    protected final FlowableEventProperties eventProperties;
    protected final FlowableAutoDeploymentProperties autoDeploymentProperties;

    public EventRegistryAutoConfiguration(FlowableProperties flowableProperties, FlowableEventProperties eventProperties,
                    FlowableAutoDeploymentProperties autoDeploymentProperties) {
        
        super(flowableProperties);
        this.eventProperties = eventProperties;
        this.autoDeploymentProperties = autoDeploymentProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringEventRegistryEngineConfiguration eventEngineConfiguration(
        DataSource dataSource,
        PlatformTransactionManager platformTransactionManager,
        ObjectProvider<List<AutoDeploymentStrategy<EventRegistryEngine>>> eventAutoDeploymentStrategies
    ) throws IOException {
        
        SpringEventRegistryEngineConfiguration configuration = new SpringEventRegistryEngineConfiguration();

        List<Resource> resources = this.discoverDeploymentResources(
                eventProperties.getResourceLocation(),
                eventProperties.getResourceSuffixes(),
                eventProperties.isDeployResources()
        );

        if (resources != null && !resources.isEmpty()) {
            configuration.setDeploymentResources(resources.toArray(new Resource[0]));
            configuration.setDeploymentName(eventProperties.getDeploymentName());
        }

        configureSpringEngine(configuration, platformTransactionManager);
        configureEngine(configuration, dataSource);

        // We cannot use orderedStream since we want to support Boot 1.5 which is on pre 5.x Spring
        List<AutoDeploymentStrategy<EventRegistryEngine>> deploymentStrategies = eventAutoDeploymentStrategies.getIfAvailable();
        if (deploymentStrategies == null) {
            deploymentStrategies = new ArrayList<>();
        }

        CommonAutoDeploymentProperties deploymentProperties = this.autoDeploymentProperties.deploymentPropertiesForEngine(ScopeTypes.EVENT_REGISTRY);
        // Always add the out of the box auto deployment strategies as last
        deploymentStrategies.add(new DefaultAutoDeploymentStrategy(deploymentProperties));
        deploymentStrategies.add(new SingleResourceAutoDeploymentStrategy(deploymentProperties));
        deploymentStrategies.add(new ResourceParentFolderAutoDeploymentStrategy(deploymentProperties));
        configuration.setDeploymentStrategies(deploymentStrategies);

        return configuration;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(type = {
        "org.flowable.spring.SpringProcessEngineConfiguration"
    })
    @ConditionalOnMissingBean(type = {
        "org.flowable.app.spring.SpringAppEngineConfiguration"
    })
    public static class EventRegistryProcessConfiguration extends BaseEngineConfigurationWithConfigurers<SpringEventRegistryEngineConfiguration> {

        @Bean
        @ConditionalOnMissingBean(name = "eventProcessEngineConfigurationConfigurer")
        public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> eventProcessEngineConfigurationConfigurer(
                        EventRegistryEngineConfigurator eventRegistryEngineConfigurator) {
            
            return processEngineConfiguration -> processEngineConfiguration.setEventRegistryConfigurator(eventRegistryEngineConfigurator);
        }

        @Bean
        @ConditionalOnMissingBean
        public SpringEventRegistryConfigurator eventEngineConfigurator(SpringEventRegistryEngineConfiguration configuration) {
            SpringEventRegistryConfigurator eventEngineConfigurator = new SpringEventRegistryConfigurator();
            eventEngineConfigurator.setEventEngineConfiguration(configuration);
            invokeConfigurers(configuration);
            
            return eventEngineConfigurator;
        }
    }
    
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(type = {
        "org.flowable.app.spring.SpringAppEngineConfiguration"
    })
    public static class EventRegistryAppEngineConfiguration extends BaseEngineConfigurationWithConfigurers<SpringEventRegistryEngineConfiguration> {

        @Bean
        @ConditionalOnMissingBean(name = "eventAppEngineConfigurationConfigurer")
        public EngineConfigurationConfigurer<SpringAppEngineConfiguration> eventAppEngineConfigurationConfigurer(
                        EventRegistryEngineConfigurator eventRegistryEngineConfigurator) {
            
            return appEngineConfiguration -> appEngineConfiguration.setEventRegistryConfigurator(eventRegistryEngineConfigurator);
        }

        @Bean
        @ConditionalOnMissingBean
        public SpringEventRegistryConfigurator eventEngineConfigurator(SpringEventRegistryEngineConfiguration configuration) {
            SpringEventRegistryConfigurator eventEngineConfigurator = new SpringEventRegistryConfigurator();
            eventEngineConfigurator.setEventEngineConfiguration(configuration);

            invokeConfigurers(configuration);
            
            return eventEngineConfigurator;
        }
    }
}

