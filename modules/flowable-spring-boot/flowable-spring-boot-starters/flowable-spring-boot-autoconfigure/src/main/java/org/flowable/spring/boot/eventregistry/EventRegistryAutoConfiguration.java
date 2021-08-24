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
import org.flowable.eventregistry.api.ChannelModelProcessor;
import org.flowable.eventregistry.api.management.EventRegistryChangeDetectionExecutor;
import org.flowable.eventregistry.impl.EventRegistryEngine;
import org.flowable.eventregistry.impl.configurator.EventRegistryEngineConfigurator;
import org.flowable.eventregistry.spring.SpringEventRegistryEngineConfiguration;
import org.flowable.eventregistry.spring.autodeployment.DefaultAutoDeploymentStrategy;
import org.flowable.eventregistry.spring.autodeployment.ResourceParentFolderAutoDeploymentStrategy;
import org.flowable.eventregistry.spring.autodeployment.SingleResourceAutoDeploymentStrategy;
import org.flowable.eventregistry.spring.configurator.SpringEventRegistryConfigurator;
import org.flowable.eventregistry.spring.jms.JmsChannelModelProcessor;
import org.flowable.eventregistry.spring.kafka.KafkaChannelDefinitionProcessor;
import org.flowable.eventregistry.spring.management.DefaultSpringEventRegistryChangeDetectionExecutor;
import org.flowable.eventregistry.spring.rabbit.RabbitChannelDefinitionProcessor;
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
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.core.JmsOperations;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Auto configuration for the event registry.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnEventRegistry
@EnableConfigurationProperties({
    FlowableProperties.class,
    FlowableAutoDeploymentProperties.class,
    FlowableEventRegistryProperties.class
})
@AutoConfigureAfter({
    AppEngineAutoConfiguration.class,
    ProcessEngineAutoConfiguration.class,
    KafkaAutoConfiguration.class,
    JmsAutoConfiguration.class,
    RabbitAutoConfiguration.class,
})
@AutoConfigureBefore({
    AppEngineServicesAutoConfiguration.class,
    ProcessEngineServicesAutoConfiguration.class,
})
public class EventRegistryAutoConfiguration extends AbstractSpringEngineAutoConfiguration {

    protected final FlowableEventRegistryProperties eventProperties;
    protected final FlowableAutoDeploymentProperties autoDeploymentProperties;

    public EventRegistryAutoConfiguration(FlowableProperties flowableProperties, FlowableEventRegistryProperties eventProperties,
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
        ObjectProvider<ObjectMapper> objectMapperProvider,
        ObjectProvider<List<ChannelModelProcessor>> channelModelProcessors,
        ObjectProvider<List<AutoDeploymentStrategy<EventRegistryEngine>>> eventAutoDeploymentStrategies,
        ObjectProvider<TaskScheduler> taskScheduler,
        ObjectProvider<EventRegistryChangeDetectionExecutor> eventRegistryChangeDetectionExecutor
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
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable();
        if (objectMapper != null) {
            configuration.setObjectMapper(objectMapper);
        }

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

        // We cannot use orderedStream since we want to support Boot 1.5 which is on pre 5.x Spring
        List<ChannelModelProcessor> channelProcessors = channelModelProcessors.getIfAvailable();
        if (channelProcessors != null && channelProcessors.size() > 0) {
            for (ChannelModelProcessor channelModelProcessor : channelProcessors) {
                configuration.addChannelModelProcessor(channelModelProcessor);
            }
        }

        configuration.setEnableEventRegistryChangeDetection(eventProperties.isEnableChangeDetection());
        EventRegistryChangeDetectionExecutor changeDetectionExecutor = eventRegistryChangeDetectionExecutor.getIfAvailable();
        if (changeDetectionExecutor != null) {
            configuration.setEventRegistryChangeDetectionExecutor(changeDetectionExecutor);
        }

        return configuration;
    }

    @Bean
    @ConditionalOnProperty(prefix = "flowable.eventregistry", name = "enable-change-detection")
    @ConditionalOnMissingBean
    public EventRegistryChangeDetectionExecutor eventRegistryChangeDetectionExecutor(ObjectProvider<TaskScheduler> taskScheduler) {
        return new DefaultSpringEventRegistryChangeDetectionExecutor(eventProperties.getChangeDetectionInitialDelay().toMillis(),
            eventProperties.getChangeDetectionDelay().toMillis(), taskScheduler.getIfAvailable());
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
        public EventRegistryEngineConfigurator eventEngineConfigurator(SpringEventRegistryEngineConfiguration configuration) {
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
        public EventRegistryEngineConfigurator eventEngineConfigurator(SpringEventRegistryEngineConfiguration configuration) {
            SpringEventRegistryConfigurator eventEngineConfigurator = new SpringEventRegistryConfigurator();
            eventEngineConfigurator.setEventEngineConfiguration(configuration);

            invokeConfigurers(configuration);
            
            return eventEngineConfigurator;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(JmsOperations.class)
    public static class EventRegistryJmsConfiguration {

        @Bean("jmsChannelDefinitionProcessor")
        @ConditionalOnMissingBean(name = "jmsChannelDefinitionProcessor")
        public JmsChannelModelProcessor jmsChannelDefinitionProcessor(JmsListenerEndpointRegistry endpointRegistry, JmsOperations jmsOperations) {
            JmsChannelModelProcessor jmsChannelDefinitionProcessor = new JmsChannelModelProcessor();
            jmsChannelDefinitionProcessor.setEndpointRegistry(endpointRegistry);
            jmsChannelDefinitionProcessor.setJmsOperations(jmsOperations);

            return jmsChannelDefinitionProcessor;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(RabbitOperations.class)
    public static class EventRegistryRabbitConfiguration {

        @Bean("rabbitChannelDefinitionProcessor")
        @ConditionalOnMissingBean(name = "rabbitChannelDefinitionProcessor")
        public RabbitChannelDefinitionProcessor rabbitChannelDefinitionProcessor(RabbitListenerEndpointRegistry endpointRegistry, RabbitOperations rabbitOperations) {
            RabbitChannelDefinitionProcessor rabbitChannelDefinitionProcessor = new RabbitChannelDefinitionProcessor();
            rabbitChannelDefinitionProcessor.setEndpointRegistry(endpointRegistry);
            rabbitChannelDefinitionProcessor.setRabbitOperations(rabbitOperations);

            return rabbitChannelDefinitionProcessor;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(KafkaOperations.class)
    public static class EventRegistryKafkaConfiguration {

        @Bean("kafkaChannelDefinitionProcessor")
        @ConditionalOnMissingBean(name = "kafkaChannelDefinitionProcessor")
        public KafkaChannelDefinitionProcessor kafkaChannelDefinitionProcessor(KafkaListenerEndpointRegistry endpointRegistry,
            KafkaOperations<Object, Object> kafkaOperations) {
            KafkaChannelDefinitionProcessor kafkaChannelDefinitionProcessor = new KafkaChannelDefinitionProcessor();
            kafkaChannelDefinitionProcessor.setEndpointRegistry(endpointRegistry);
            kafkaChannelDefinitionProcessor.setKafkaOperations(kafkaOperations);

            return kafkaChannelDefinitionProcessor;
        }
    }
}

