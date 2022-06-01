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
package org.flowable.eventregistry.test.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.api.ChannelModelProcessor;
import org.flowable.eventregistry.api.ChannelProcessingPipelineManager;
import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.InboundChannelModelCacheManager;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.deployer.DefaultInboundChannelModelCacheManager;
import org.flowable.eventregistry.impl.persistence.deploy.ChannelDefinitionCacheEntry;
import org.flowable.eventregistry.impl.pipeline.DefaultInboundEventProcessingPipeline;
import org.flowable.eventregistry.impl.pipeline.InboundChannelModelProcessor;
import org.flowable.eventregistry.impl.tenantdetector.InboundEventStaticTenantDetector;
import org.flowable.eventregistry.impl.tenantdetector.JsonPointerBasedInboundEventTenantDetector;
import org.flowable.eventregistry.impl.tenantdetector.XpathBasedInboundEventTenantDetector;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.EventPayload;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.flowable.eventregistry.model.JmsInboundChannelModel;
import org.flowable.eventregistry.test.AbstractFlowableEventTest;
import org.flowable.eventregistry.test.ChannelDeploymentAnnotation;
import org.flowable.eventregistry.test.EventDeploymentAnnotation;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DeploymentTest extends AbstractFlowableEventTest {

    @Test
    @EventDeploymentAnnotation(resources = "org/flowable/eventregistry/test/deployment/simpleEvent.event")
    public void deploySingleEventDefinition() {
        EventDefinition eventDefinition = repositoryService.createEventDefinitionQuery()
                .eventDefinitionKey("myEvent")
                .latestVersion()
                .singleResult();
        assertThat(eventDefinition).isNotNull();
        assertThat(eventDefinition.getKey()).isEqualTo("myEvent");
        assertThat(eventDefinition.getVersion()).isEqualTo(1);
    }

    @Test
    @ChannelDeploymentAnnotation(resources = "org/flowable/eventregistry/test/deployment/simpleChannel.channel")
    public void deploySingleChannelDefinition() {
        ChannelDefinition channelDefinition = repositoryService.createChannelDefinitionQuery()
                .channelDefinitionKey("myChannel")
                .latestVersion()
                .singleResult();
        assertThat(channelDefinition).isNotNull();
        assertThat(channelDefinition.getKey()).isEqualTo("myChannel");
        assertThat(channelDefinition.getVersion()).isEqualTo(1);
    }

    @Test
    @ChannelDeploymentAnnotation(resources = {
            "org/flowable/eventregistry/test/deployment/simpleChannelWithFixedTenant.channel",
            "org/flowable/eventregistry/test/deployment/simpleChannelWithJsonPointerTenant.channel",
            "org/flowable/eventregistry/test/deployment/simpleChannelWithXPathTenant.channel"
    })
    public void deployChannelsWithTenantDetection() {
        InboundChannelModel channel1 = (InboundChannelModel) eventRegistryEngine.getEventRepositoryService().getChannelModelByKey("channel1");
        assertThat(((DefaultInboundEventProcessingPipeline) channel1.getInboundEventProcessingPipeline()).getInboundEventTenantDetector())
                .isInstanceOf(InboundEventStaticTenantDetector.class);

        InboundChannelModel channel2 = (InboundChannelModel) eventRegistryEngine.getEventRepositoryService().getChannelModelByKey("channel2");
        DefaultInboundEventProcessingPipeline inboundEventProcessingPipeline = (DefaultInboundEventProcessingPipeline) channel2
                .getInboundEventProcessingPipeline();
        assertThat(inboundEventProcessingPipeline.getInboundEventTenantDetector()).isInstanceOf(JsonPointerBasedInboundEventTenantDetector.class);
        assertThat(((JsonPointerBasedInboundEventTenantDetector) inboundEventProcessingPipeline.getInboundEventTenantDetector()).getJsonPointerExpression())
                .isEqualTo("/tenantId");

        InboundChannelModel channel3 = (InboundChannelModel) eventRegistryEngine.getEventRepositoryService().getChannelModelByKey("channel3");
        inboundEventProcessingPipeline = (DefaultInboundEventProcessingPipeline) channel3.getInboundEventProcessingPipeline();
        assertThat(inboundEventProcessingPipeline.getInboundEventTenantDetector()).isInstanceOf(XpathBasedInboundEventTenantDetector.class);
        assertThat(((XpathBasedInboundEventTenantDetector) inboundEventProcessingPipeline.getInboundEventTenantDetector()).getXpathExpression())
                .isEqualTo("/data/tenantId");
    }

    @Test
    @EventDeploymentAnnotation(resources = "org/flowable/eventregistry/test/deployment/simpleEvent.event")
    @ChannelDeploymentAnnotation(resources = "org/flowable/eventregistry/test/deployment/simpleChannel.channel")
    public void deployEventAndChannelDefinition() {
        EventDefinition eventDefinition = repositoryService.createEventDefinitionQuery()
                .eventDefinitionKey("myEvent")
                .latestVersion()
                .singleResult();
        assertThat(eventDefinition).isNotNull();
        assertThat(eventDefinition.getKey()).isEqualTo("myEvent");
        assertThat(eventDefinition.getVersion()).isEqualTo(1);

        ChannelDefinition channelDefinition = repositoryService.createChannelDefinitionQuery()
                .channelDefinitionKey("myChannel")
                .latestVersion()
                .singleResult();
        assertThat(channelDefinition).isNotNull();
        assertThat(channelDefinition.getKey()).isEqualTo("myChannel");
        assertThat(channelDefinition.getVersion()).isEqualTo(1);
    }

    @Test
    @EventDeploymentAnnotation(resources = "org/flowable/eventregistry/test/deployment/simpleEvent.event")
    public void redeploySingleEventDefinition() {
        EventDefinition eventDefinition = repositoryService.createEventDefinitionQuery()
                .eventDefinitionKey("myEvent")
                .latestVersion()
                .singleResult();
        assertThat(eventDefinition).isNotNull();
        assertThat(eventDefinition.getKey()).isEqualTo("myEvent");
        assertThat(eventDefinition.getVersion()).isEqualTo(1);

        EventModel eventModel = repositoryService.getEventModelById(eventDefinition.getId());
        assertThat(eventModel.getKey()).isEqualTo("myEvent");

        assertThat(eventModel.getCorrelationParameters())
                .extracting(EventPayload::getName, EventPayload::getType)
                .containsExactly(tuple("customerId", "string"));

        assertThat(eventModel.getPayload())
                .extracting(EventPayload::getName, EventPayload::getType)
                .containsExactly(
                        tuple("payload1", "string"),
                        tuple("payload2", "integer"),
                        tuple("customerId", "string")
                );

        EventDeployment redeployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/test/deployment/simpleEvent2.event")
                .deploy();

        eventDefinition = repositoryService.createEventDefinitionQuery()
                .eventDefinitionKey("myEvent")
                .latestVersion()
                .singleResult();
        assertThat(eventDefinition).isNotNull();
        assertThat(eventDefinition.getKey()).isEqualTo("myEvent");
        assertThat(eventDefinition.getVersion()).isEqualTo(2);

        eventModel = repositoryService.getEventModelById(eventDefinition.getId());
        assertThat(eventModel.getKey()).isEqualTo("myEvent");

        assertThat(eventModel.getCorrelationParameters())
                .extracting(EventPayload::getName, EventPayload::getType)
                .containsExactly(tuple("customerId2", "string"));

        assertThat(eventModel.getPayload())
                .extracting(EventPayload::getName, EventPayload::getType)
                .containsExactly(
                        tuple("payload3", "string"),
                        tuple("payload4", "integer"),
                        tuple("customerId2", "string")
                );

        repositoryService.deleteDeployment(redeployment.getId());
    }

    @Test
    @EventDeploymentAnnotation(resources = "org/flowable/eventregistry/test/deployment/simpleChannel.channel")
    public void redeploySingleChannelDefinition() {
        ChannelDefinition channelDefinition = repositoryService.createChannelDefinitionQuery()
                .channelDefinitionKey("myChannel")
                .latestVersion()
                .singleResult();
        assertThat(channelDefinition).isNotNull();
        assertThat(channelDefinition.getKey()).isEqualTo("myChannel");
        assertThat(channelDefinition.getVersion()).isEqualTo(1);

        ChannelModel channelModel = repositoryService.getChannelModelById(channelDefinition.getId());
        assertThat(channelModel)
                .isInstanceOfSatisfying(JmsInboundChannelModel.class, channel -> {
                    assertThat(channel.getKey()).isEqualTo("myChannel");
                    assertThat(channel.getChannelType()).isEqualTo("inbound");
                    assertThat(channel.getType()).isEqualTo("jms");

                    assertThat(channel.getDestination()).isEqualTo("testQueue");
                    assertThat(channel.getDeserializerType()).isEqualTo("json");
                    assertThat(channel.getChannelEventKeyDetection().getFixedValue()).isEqualTo("myEvent");
                });

        EventDeployment redeployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/test/deployment/simpleChannel2.channel")
                .deploy();

        channelDefinition = repositoryService.createChannelDefinitionQuery()
                .channelDefinitionKey("myChannel")
                .latestVersion()
                .singleResult();
        assertThat(channelDefinition).isNotNull();
        assertThat(channelDefinition.getKey()).isEqualTo("myChannel");
        assertThat(channelDefinition.getVersion()).isEqualTo(2);

        channelModel = repositoryService.getChannelModelById(channelDefinition.getId());
        assertThat(channelModel)
                .isInstanceOfSatisfying(JmsInboundChannelModel.class, channel -> {
                    assertThat(channel.getChannelType()).isEqualTo("inbound");
                    assertThat(channel.getType()).isEqualTo("jms");
                    assertThat(channel.getDestination()).isEqualTo("testQueue2");

                    assertThat(channel.getDeserializerType()).isEqualTo("json");
                    assertThat(channel.getChannelEventKeyDetection().getFixedValue()).isEqualTo("myEvent2");
                });

        repositoryService.deleteDeployment(redeployment.getId());
    }

    @Test
    @EventDeploymentAnnotation(resources = "org/flowable/eventregistry/test/deployment/simpleChannel.channel", tenantId = "tenant1")
    public void redeploySingleChannelDefinitionInMultipleTenants() {
        ChannelDefinition channelDefinition = repositoryService.createChannelDefinitionQuery()
                .channelDefinitionKey("myChannel")
                .latestVersion()
                .singleResult();
        assertThat(channelDefinition).isNotNull();
        assertThat(channelDefinition.getKey()).isEqualTo("myChannel");
        assertThat(channelDefinition.getTenantId()).isEqualTo("tenant1");
        assertThat(channelDefinition.getVersion()).isEqualTo(1);

        ChannelModel channelModel = repositoryService.getChannelModelById(channelDefinition.getId());
        assertThat(channelModel)
                .isInstanceOfSatisfying(JmsInboundChannelModel.class, channel -> {
                    assertThat(channel.getKey()).isEqualTo("myChannel");
                    assertThat(channel.getChannelType()).isEqualTo("inbound");
                    assertThat(channel.getType()).isEqualTo("jms");

                    assertThat(channel.getDestination()).isEqualTo("testQueue");
                    assertThat(channel.getDeserializerType()).isEqualTo("json");
                    assertThat(channel.getChannelEventKeyDetection().getFixedValue()).isEqualTo("myEvent");
                });

        EventDeployment redeployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/test/deployment/simpleChannel2.channel")
                .tenantId("tenant2")
                .deploy();

        channelDefinition = repositoryService.createChannelDefinitionQuery()
                .channelDefinitionKey("myChannel")
                .tenantId("tenant2")
                .latestVersion()
                .singleResult();
        assertThat(channelDefinition).isNotNull();
        assertThat(channelDefinition.getKey()).isEqualTo("myChannel");
        assertThat(channelDefinition.getTenantId()).isEqualTo("tenant2");
        assertThat(channelDefinition.getVersion()).isEqualTo(1);

        channelModel = repositoryService.getChannelModelById(channelDefinition.getId());
        assertThat(channelModel)
                .isInstanceOfSatisfying(JmsInboundChannelModel.class, channel -> {
                    assertThat(channel.getChannelType()).isEqualTo("inbound");
                    assertThat(channel.getType()).isEqualTo("jms");
                    assertThat(channel.getDestination()).isEqualTo("testQueue2");

                    assertThat(channel.getDeserializerType()).isEqualTo("json");
                    assertThat(channel.getChannelEventKeyDetection().getFixedValue()).isEqualTo("myEvent2");
                });

        List<ChannelDefinition> channelDefinitions = repositoryService.createChannelDefinitionQuery()
                .channelDefinitionKey("myChannel")
                .latestVersion()
                .list();

        assertThat(channelDefinitions)
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getTenantId, ChannelDefinition::getVersion)
                .containsExactlyInAnyOrder(
                        tuple("myChannel", "tenant1", 1),
                        tuple("myChannel", "tenant2", 1)
                );

        EventDeployment redeployment2 = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/test/deployment/simpleChannel2.channel")
                .tenantId("tenant1")
                .deploy();

        channelDefinitions = repositoryService.createChannelDefinitionQuery()
                .channelDefinitionKey("myChannel")
                .latestVersion()
                .list();

        assertThat(channelDefinitions)
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getTenantId, ChannelDefinition::getVersion)
                .containsExactlyInAnyOrder(
                        tuple("myChannel", "tenant1", 2),
                        tuple("myChannel", "tenant2", 1)
                );

        repositoryService.deleteDeployment(redeployment.getId());
        repositoryService.deleteDeployment(redeployment2.getId());
    }

    @Test
    @EventDeploymentAnnotation(resources = { "org/flowable/eventregistry/test/deployment/simpleEvent.event",
            "org/flowable/eventregistry/test/deployment/orderEvent.event" })
    public void deploy2EventDefinitions() {
        List<EventDefinition> eventDefinitions = repositoryService.createEventDefinitionQuery().orderByEventDefinitionName().asc().list();
        assertThat(eventDefinitions)
                .extracting(EventDefinition::getName)
                .containsExactly("My event", "My order event");
    }

    @Test
    @ChannelDeploymentAnnotation(resources = { "org/flowable/eventregistry/test/deployment/simpleChannel.channel",
            "org/flowable/eventregistry/test/deployment/orderChannel.channel" })
    public void deploy2ChannelDefinitions() {
        List<ChannelDefinition> channelDefinitions = repositoryService.createChannelDefinitionQuery().orderByChannelDefinitionName().asc().list();
        assertThat(channelDefinitions)
                .extracting(ChannelDefinition::getName)
                .containsExactly("My channel", "Order channel");
    }
    
    @Test
    public void verifyInboundChannelModelCacheManager() {
        InboundChannelModelCacheManager inboundChannelModelCacheManager = eventEngineConfiguration.getInboundChannelModelCacheManager();
        try {
            TestInboundChannelModelCacheManager testInboundChannelModelCacheManager = new TestInboundChannelModelCacheManager(eventEngineConfiguration);
            eventEngineConfiguration.setInboundChannelModelCacheManager(testInboundChannelModelCacheManager);
            repositoryService.createDeployment().addClasspathResource("org/flowable/eventregistry/test/deployment/simpleChannel.channel").deploy();
            
            assertThat(testInboundChannelModelCacheManager.getCache()).hasSize(1);
            
            assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").list()).hasSize(1);
            assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").latestVersion().singleResult().getVersion()).isEqualTo(1);
            
            repositoryService.createDeployment().addClasspathResource("org/flowable/eventregistry/test/deployment/simpleChannel.channel").deploy();
            
            assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").list()).hasSize(2);
            assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").latestVersion().singleResult().getVersion()).isEqualTo(2);
            
            assertThat(testInboundChannelModelCacheManager.getCache()).hasSize(1);
            
        } finally {
            eventEngineConfiguration.setInboundChannelModelCacheManager(inboundChannelModelCacheManager);
            repositoryService.createDeploymentQuery().list().forEach(deployment -> {
                repositoryService.deleteDeployment(deployment.getId());
            });
        }
    }
    
    @Test
    public void verifyInboundChannelCacheWithCustomProcessor() {
        List<ChannelModelProcessor> channelModelProcessors = (List<ChannelModelProcessor>) eventEngineConfiguration.getChannelModelProcessors();
        TestChannelModelProcessor testChannelModelProcessor = new TestChannelModelProcessor(eventEngineConfiguration.getObjectMapper());
        for (int i = 0; i < channelModelProcessors.size(); i++) {
            ChannelModelProcessor channelModelProcessor = channelModelProcessors.get(i);
            if (channelModelProcessor instanceof InboundChannelModelProcessor) {
                channelModelProcessors.set(i, testChannelModelProcessor);
            }
        }
        
        eventEngineConfiguration.getInboundChannelModelCacheManager().cleanChannelModels();
        
        try {
            repositoryService.createDeployment().addClasspathResource("org/flowable/eventregistry/test/deployment/simpleChannel.channel").deploy();
            
            assertThat(testChannelModelProcessor.registeredChannelModels).hasSize(1);
            assertThat(testChannelModelProcessor.unregisteredChannelModels).hasSize(1);
            
            assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").list()).hasSize(1);
            assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").latestVersion().singleResult().getVersion()).isEqualTo(1);
            
            repositoryService.createDeployment().addClasspathResource("org/flowable/eventregistry/test/deployment/simpleChannel.channel").deploy();
            
            assertThat(testChannelModelProcessor.registeredChannelModels).hasSize(1);
            assertThat(testChannelModelProcessor.unregisteredChannelModels).hasSize(1);
            
            assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").list()).hasSize(2);
            assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").latestVersion().singleResult().getVersion()).isEqualTo(2);
            
            repositoryService.createDeployment().addClasspathResource("org/flowable/eventregistry/test/deployment/simpleChannel2.channel").deploy();
            
            assertThat(testChannelModelProcessor.registeredChannelModels).hasSize(2);
            assertThat(testChannelModelProcessor.unregisteredChannelModels).hasSize(2);
            
            assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").list()).hasSize(3);
            assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").latestVersion().singleResult().getVersion()).isEqualTo(3);
            
        } finally {
            for (int i = 0; i < channelModelProcessors.size(); i++) {
                ChannelModelProcessor channelModelProcessor = channelModelProcessors.get(i);
                if (channelModelProcessor instanceof TestChannelModelProcessor) {
                    channelModelProcessors.set(i, new InboundChannelModelProcessor(eventEngineConfiguration.getObjectMapper()));
                }
            }
            
            repositoryService.createDeploymentQuery().list().forEach(deployment -> {
                repositoryService.deleteDeployment(deployment.getId());
            });
        }
    }
    
    @Test
    public void verifyChangeDetectionWithCustomProcessor() {
        List<ChannelModelProcessor> channelModelProcessors = (List<ChannelModelProcessor>) eventEngineConfiguration.getChannelModelProcessors();
        TestChannelModelProcessor testChannelModelProcessor = new TestChannelModelProcessor(eventEngineConfiguration.getObjectMapper());
        for (int i = 0; i < channelModelProcessors.size(); i++) {
            ChannelModelProcessor channelModelProcessor = channelModelProcessors.get(i);
            if (channelModelProcessor instanceof InboundChannelModelProcessor) {
                channelModelProcessors.set(i, testChannelModelProcessor);
            }
        }
        
        eventEngineConfiguration.getInboundChannelModelCacheManager().cleanChannelModels();
        
        try {
            repositoryService.createDeployment().addClasspathResource("org/flowable/eventregistry/test/deployment/simpleChannel.channel").deploy();
            
            assertThat(testChannelModelProcessor.registeredChannelModels).hasSize(1);
            assertThat(testChannelModelProcessor.unregisteredChannelModels).hasSize(1);
            
            assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").list()).hasSize(1);
            assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").latestVersion().singleResult().getVersion()).isEqualTo(1);
            
            eventEngineConfiguration.getEventRegistryChangeDetectionManager().detectChanges();
            
            assertThat(testChannelModelProcessor.registeredChannelModels).hasSize(1);
            assertThat(testChannelModelProcessor.unregisteredChannelModels).hasSize(1);
            
            repositoryService.createDeployment().addClasspathResource("org/flowable/eventregistry/test/deployment/simpleChannel2.channel").deploy();
            
            assertThat(testChannelModelProcessor.registeredChannelModels).hasSize(2);
            assertThat(testChannelModelProcessor.unregisteredChannelModels).hasSize(2);
            
            assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").list()).hasSize(2);
            assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").latestVersion().singleResult().getVersion()).isEqualTo(2);
            
            eventEngineConfiguration.getEventRegistryChangeDetectionManager().detectChanges();
            
            assertThat(testChannelModelProcessor.registeredChannelModels).hasSize(2);
            assertThat(testChannelModelProcessor.unregisteredChannelModels).hasSize(2);
            
            eventEngineConfiguration.getInboundChannelModelCacheManager().cleanChannelModels();
            
            ChannelDefinition channelDefinition = repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").latestVersion().singleResult();
            eventEngineConfiguration.getDeploymentManager().removeChannelDefinitionFromCache(channelDefinition);
            
            assertThat(testChannelModelProcessor.registeredChannelModels).hasSize(2);
            assertThat(testChannelModelProcessor.unregisteredChannelModels).hasSize(3);

            eventEngineConfiguration.getEventRegistryChangeDetectionManager().detectChanges();
            
            assertThat(testChannelModelProcessor.registeredChannelModels).hasSize(3);
            assertThat(testChannelModelProcessor.unregisteredChannelModels).hasSize(4);
            
        } finally {
            for (int i = 0; i < channelModelProcessors.size(); i++) {
                ChannelModelProcessor channelModelProcessor = channelModelProcessors.get(i);
                if (channelModelProcessor instanceof TestChannelModelProcessor) {
                    channelModelProcessors.set(i, new InboundChannelModelProcessor(eventEngineConfiguration.getObjectMapper()));
                }
            }
            
            repositoryService.createDeploymentQuery().list().forEach(deployment -> {
                repositoryService.deleteDeployment(deployment.getId());
            });
        }
    }
    
    @Test
    public void verifyMTInboundChannelCacheWithCustomProcessor() {
        List<ChannelModelProcessor> channelModelProcessors = (List<ChannelModelProcessor>) eventEngineConfiguration.getChannelModelProcessors();
        TestChannelModelProcessor testChannelModelProcessor = new TestChannelModelProcessor(eventEngineConfiguration.getObjectMapper());
        for (int i = 0; i < channelModelProcessors.size(); i++) {
            ChannelModelProcessor channelModelProcessor = channelModelProcessors.get(i);
            if (channelModelProcessor instanceof InboundChannelModelProcessor) {
                channelModelProcessors.set(i, testChannelModelProcessor);
            }
        }
        
        eventEngineConfiguration.getInboundChannelModelCacheManager().cleanChannelModels();
        
        try {
            repositoryService.createDeployment().addClasspathResource("org/flowable/eventregistry/test/deployment/simpleChannel.channel")
                    .tenantId("tenantA")
                    .deploy();
            
            assertThat(testChannelModelProcessor.registeredChannelModels).hasSize(1);
            assertThat(testChannelModelProcessor.unregisteredChannelModels).hasSize(1);
            
            assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").tenantId("tenantA").list()).hasSize(1);
            assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").tenantId("tenantA").latestVersion().singleResult().getVersion()).isEqualTo(1);
            
            repositoryService.createDeployment().addClasspathResource("org/flowable/eventregistry/test/deployment/simpleChannel.channel")
                    .tenantId("tenantA")
                    .deploy();
            
            assertThat(testChannelModelProcessor.registeredChannelModels).hasSize(1);
            assertThat(testChannelModelProcessor.unregisteredChannelModels).hasSize(1);
            
            assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").tenantId("tenantA").list()).hasSize(2);
            assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").tenantId("tenantA").latestVersion().singleResult().getVersion()).isEqualTo(2);
            
            repositoryService.createDeployment().addClasspathResource("org/flowable/eventregistry/test/deployment/simpleChannel.channel")
                    .tenantId("tenantB")
                    .deploy();
    
            assertThat(testChannelModelProcessor.registeredChannelModels).hasSize(2);
            assertThat(testChannelModelProcessor.unregisteredChannelModels).hasSize(2);
            
            assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").tenantId("tenantB").list()).hasSize(1);
            assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").tenantId("tenantB").latestVersion().singleResult().getVersion()).isEqualTo(1);
            
            repositoryService.createDeployment().addClasspathResource("org/flowable/eventregistry/test/deployment/simpleChannel2.channel")
                    .tenantId("tenantA")
                    .deploy();
            
            assertThat(testChannelModelProcessor.registeredChannelModels).hasSize(3);
            assertThat(testChannelModelProcessor.unregisteredChannelModels).hasSize(3);
            
            assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").tenantId("tenantA").list()).hasSize(3);
            assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").tenantId("tenantA").latestVersion().singleResult().getVersion()).isEqualTo(3);
            
        } finally {
            for (int i = 0; i < channelModelProcessors.size(); i++) {
                ChannelModelProcessor channelModelProcessor = channelModelProcessors.get(i);
                if (channelModelProcessor instanceof TestChannelModelProcessor) {
                    channelModelProcessors.set(i, new InboundChannelModelProcessor(eventEngineConfiguration.getObjectMapper()));
                }
            }
            
            repositoryService.createDeploymentQuery().list().forEach(deployment -> {
                repositoryService.deleteDeployment(deployment.getId());
            });
        }
    }

    @Test
    public void deployNewChannelVersion() {
        DeploymentCache<ChannelDefinitionCacheEntry> channelDefinitionCache = eventEngineConfiguration.getChannelDefinitionCache();

        repositoryService.createDeployment().addClasspathResource("org/flowable/eventregistry/test/deployment/simpleChannel.channel").deploy();
        ChannelDefinition channelDefinition1 = repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").latestVersion()
                .singleResult();
        assertThat(channelDefinition1.getName()).isEqualTo("My channel");

        assertThat(channelDefinitionCache.size()).isEqualTo(1);
        assertThat(channelDefinitionCache.get(channelDefinition1.getId())).isNotNull();

        repositoryService.createDeployment().addClasspathResource("org/flowable/eventregistry/test/deployment/simpleChannel2.channel").deploy();

        ChannelDefinition channelDefinition2 = repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").latestVersion()
                .singleResult();
        assertThat(channelDefinition2.getName()).isEqualTo("My channel2");

        assertThat(channelDefinitionCache.size()).isEqualTo(1);
        assertThat(channelDefinitionCache.get(channelDefinition1.getId())).isNull();
        assertThat(channelDefinitionCache.get(channelDefinition2.getId())).isNotNull();

        repositoryService.deleteDeployment(channelDefinition1.getDeploymentId());
        repositoryService.deleteDeployment(channelDefinition2.getDeploymentId());
    }

    @Test
    public void deploySingleEventDefinitionWithParentDeploymentId() {
        EventDeployment deployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/test/deployment/simpleEvent.event")
                .parentDeploymentId("someDeploymentId")
                .deploy();

        EventDeployment newDeployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/test/deployment/simpleEvent2.event")
                .deploy();

        try {
            EventDefinition definition = repositoryService.createEventDefinitionQuery().deploymentId(deployment.getId()).singleResult();
            assertThat(definition).isNotNull();
            assertThat(definition.getKey()).isEqualTo("myEvent");
            assertThat(definition.getVersion()).isEqualTo(1);

            EventDefinition newDefinition = repositoryService.createEventDefinitionQuery().deploymentId(newDeployment.getId()).singleResult();
            assertThat(newDefinition).isNotNull();
            assertThat(newDefinition.getKey()).isEqualTo("myEvent");
            assertThat(newDefinition.getVersion()).isEqualTo(2);

            EventModel eventModel = repositoryService.getEventModelByKeyAndParentDeploymentId("myEvent", "someDeploymentId");
            assertThat(eventModel.getKey()).isEqualTo("myEvent");
            assertThat(eventModel.getName()).isEqualTo("My event");

            eventEngineConfiguration.setAlwaysLookupLatestDefinitionVersion(true);
            eventModel = repositoryService.getEventModelByKeyAndParentDeploymentId("myEvent", "someDeploymentId");
            assertThat(eventModel.getKey()).isEqualTo("myEvent");
            assertThat(eventModel.getName()).isEqualTo("My event2");

        } finally {
            eventEngineConfiguration.setAlwaysLookupLatestDefinitionVersion(false);
            repositoryService.deleteDeployment(deployment.getId());
            repositoryService.deleteDeployment(newDeployment.getId());
        }
    }

    @Test
    public void deploySingleChannelDefinitionWithParentDeploymentId() {
        EventDeployment deployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/test/deployment/simpleChannel.channel")
                .parentDeploymentId("someDeploymentId")
                .deploy();

        EventDeployment newDeployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/test/deployment/simpleChannel2.channel")
                .deploy();

        try {
            ChannelDefinition definition = repositoryService.createChannelDefinitionQuery().deploymentId(deployment.getId()).singleResult();
            assertThat(definition).isNotNull();
            assertThat(definition.getKey()).isEqualTo("myChannel");
            assertThat(definition.getVersion()).isEqualTo(1);

            ChannelDefinition newDefinition = repositoryService.createChannelDefinitionQuery().deploymentId(newDeployment.getId()).singleResult();
            assertThat(newDefinition).isNotNull();
            assertThat(newDefinition.getKey()).isEqualTo("myChannel");
            assertThat(newDefinition.getVersion()).isEqualTo(2);

            ChannelModel channelModel = repositoryService.getChannelModelByKeyAndParentDeploymentId("myChannel", "someDeploymentId");
            assertThat(channelModel.getKey()).isEqualTo("myChannel");
            assertThat(channelModel.getName()).isEqualTo("My channel");

            eventEngineConfiguration.setAlwaysLookupLatestDefinitionVersion(true);
            channelModel = repositoryService.getChannelModelByKeyAndParentDeploymentId("myChannel", "someDeploymentId");
            assertThat(channelModel.getKey()).isEqualTo("myChannel");
            assertThat(channelModel.getName()).isEqualTo("My channel2");

        } finally {
            eventEngineConfiguration.setAlwaysLookupLatestDefinitionVersion(false);
            repositoryService.deleteDeployment(deployment.getId());
            repositoryService.deleteDeployment(newDeployment.getId());
        }
    }

    @Test
    @EventDeploymentAnnotation(resources = "org/flowable/eventregistry/test/deployment/eventWithChannelKeys.event")
    public void deployEventWithChannelKeys() {

        // In 6.5.0, event models had channel keys (inbound/outbound).
        // This test validates that they still can be deployed.

        EventDefinition eventDefinition = repositoryService.createEventDefinitionQuery()
                .eventDefinitionKey("myOrderEvent")
                .latestVersion()
                .singleResult();
        assertThat(eventDefinition).isNotNull();
    }
    
    protected static class TestInboundChannelModelCacheManager extends DefaultInboundChannelModelCacheManager {

        protected TestInboundChannelModelCacheManager(EventRegistryEngineConfiguration engineConfiguration) {
            super(engineConfiguration);
        }

        protected Map<CacheKey, String> getCache() {
            return cache;
        }
    }
    
    protected class TestChannelModelProcessor extends InboundChannelModelProcessor {
        
        protected List<InboundChannelModel> unregisteredChannelModels = new ArrayList<>();
        protected List<ChannelModel> registeredChannelModels = new ArrayList<>();
        
        public TestChannelModelProcessor(ObjectMapper objectMapper) {
            super(objectMapper);
        }

        @Override
        protected void registerChannelModel(InboundChannelModel inboundChannelModel,
                EventRepositoryService eventRepositoryService, ChannelProcessingPipelineManager eventSerializerManager,
                ObjectMapper objectMapper, boolean fallbackToDefaultTenant) {

            registeredChannelModels.add(inboundChannelModel);
            super.registerChannelModel(inboundChannelModel, eventRepositoryService, eventSerializerManager, objectMapper, fallbackToDefaultTenant);
        }

        @Override
        public void unregisterChannelModel(ChannelModel channelModel, String tenantId, EventRepositoryService eventRepositoryService) {
            InboundChannelModel inboundChannelModel = (InboundChannelModel) channelModel;
            unregisteredChannelModels.add(inboundChannelModel);
            super.unregisterChannelModel(channelModel, tenantId, eventRepositoryService);
        }
    }
}
