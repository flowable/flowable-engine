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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Iterator;
import java.util.List;

import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.impl.pipeline.DefaultInboundEventProcessingPipeline;
import org.flowable.eventregistry.impl.tenantdetector.InboundEventStaticTenantDetector;
import org.flowable.eventregistry.impl.tenantdetector.JsonPathBasedInboundEventTenantDetector;
import org.flowable.eventregistry.impl.tenantdetector.XpathBasedInboundEventTenantDetector;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.EventCorrelationParameter;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.EventPayload;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.flowable.eventregistry.model.JmsInboundChannelModel;
import org.flowable.eventregistry.test.AbstractFlowableEventTest;
import org.flowable.eventregistry.test.ChannelDeploymentAnnotation;
import org.flowable.eventregistry.test.EventDeploymentAnnotation;
import org.junit.jupiter.api.Test;

public class DeploymentTest extends AbstractFlowableEventTest {

    @Test
    @EventDeploymentAnnotation(resources = "org/flowable/eventregistry/test/deployment/simpleEvent.event")
    public void deploySingleEventDefinition() {
        EventDefinition eventDefinition = repositoryService.createEventDefinitionQuery()
                .eventDefinitionKey("myEvent")
                .latestVersion()
                .singleResult();
        assertNotNull(eventDefinition);
        assertEquals("myEvent", eventDefinition.getKey());
        assertEquals(1, eventDefinition.getVersion());
    }

    @Test
    @ChannelDeploymentAnnotation(resources = "org/flowable/eventregistry/test/deployment/simpleChannel.channel")
    public void deploySingleChannelDefinition() {
        ChannelDefinition channelDefinition = repositoryService.createChannelDefinitionQuery()
                .channelDefinitionKey("myChannel")
                .latestVersion()
                .singleResult();
        assertNotNull(channelDefinition);
        assertEquals("myChannel", channelDefinition.getKey());
        assertEquals(1, channelDefinition.getVersion());
    }

    @Test
    @ChannelDeploymentAnnotation(resources = {
        "org/flowable/eventregistry/test/deployment/simpleChannelWithFixedTenant.channel",
        "org/flowable/eventregistry/test/deployment/simpleChannelWithJsonPathTenant.channel",
        "org/flowable/eventregistry/test/deployment/simpleChannelWithXPathTenant.channel"
        }
    )
    public void deployChannelsWithTenantDetection() {
        InboundChannelModel channel1 = (InboundChannelModel) eventRegistryEngine.getEventRepositoryService().getChannelModelByKey("channel1");
        assertThat(((DefaultInboundEventProcessingPipeline) channel1.getInboundEventProcessingPipeline()).getInboundEventTenantDetector())
            .isInstanceOf(InboundEventStaticTenantDetector.class);

        InboundChannelModel channel2 = (InboundChannelModel) eventRegistryEngine.getEventRepositoryService().getChannelModelByKey("channel2");
        DefaultInboundEventProcessingPipeline inboundEventProcessingPipeline = (DefaultInboundEventProcessingPipeline) channel2.getInboundEventProcessingPipeline();
        assertThat(inboundEventProcessingPipeline.getInboundEventTenantDetector()).isInstanceOf(JsonPathBasedInboundEventTenantDetector.class);
        assertThat(((JsonPathBasedInboundEventTenantDetector) inboundEventProcessingPipeline.getInboundEventTenantDetector()).getJsonPathExpression()).isEqualTo("/tenantId");

        InboundChannelModel channel3 = (InboundChannelModel) eventRegistryEngine.getEventRepositoryService().getChannelModelByKey("channel3");
        inboundEventProcessingPipeline = (DefaultInboundEventProcessingPipeline) channel3.getInboundEventProcessingPipeline();
        assertThat(inboundEventProcessingPipeline.getInboundEventTenantDetector()).isInstanceOf(XpathBasedInboundEventTenantDetector.class);
        assertThat(((XpathBasedInboundEventTenantDetector) inboundEventProcessingPipeline.getInboundEventTenantDetector()).getXpathExpression()).isEqualTo("/data/tenantId");
    }


    @Test
    @EventDeploymentAnnotation(resources = "org/flowable/eventregistry/test/deployment/simpleEvent.event")
    @ChannelDeploymentAnnotation(resources = "org/flowable/eventregistry/test/deployment/simpleChannel.channel")
    public void deployEventAndChannelDefinition() {
        EventDefinition eventDefinition = repositoryService.createEventDefinitionQuery()
                .eventDefinitionKey("myEvent")
                .latestVersion()
                .singleResult();
        assertNotNull(eventDefinition);
        assertEquals("myEvent", eventDefinition.getKey());
        assertEquals(1, eventDefinition.getVersion());
        
        ChannelDefinition channelDefinition = repositoryService.createChannelDefinitionQuery()
                .channelDefinitionKey("myChannel")
                .latestVersion()
                .singleResult();
        assertNotNull(channelDefinition);
        assertEquals("myChannel", channelDefinition.getKey());
        assertEquals(1, channelDefinition.getVersion());
    }

    @Test
    @EventDeploymentAnnotation(resources = "org/flowable/eventregistry/test/deployment/simpleEvent.event")
    public void redeploySingleEventDefinition() {
        EventDefinition eventDefinition = repositoryService.createEventDefinitionQuery()
                .eventDefinitionKey("myEvent")
                .latestVersion()
                .singleResult();
        assertNotNull(eventDefinition);
        assertEquals("myEvent", eventDefinition.getKey());
        assertEquals(1, eventDefinition.getVersion());

        EventModel eventModel = repositoryService.getEventModelById(eventDefinition.getId());
        assertEquals("myEvent", eventModel.getKey());
        
        assertEquals(1, eventModel.getInboundChannelKeys().size());
        assertEquals("test-channel", eventModel.getInboundChannelKeys().iterator().next());
        
        assertEquals(1, eventModel.getCorrelationParameters().size());
        EventCorrelationParameter correlationParameter = eventModel.getCorrelationParameters().iterator().next();
        assertEquals("customerId", correlationParameter.getName());
        assertEquals("string", correlationParameter.getType());
        
        assertEquals(2, eventModel.getPayload().size());
        Iterator<EventPayload> itPayload = eventModel.getPayload().iterator();
        EventPayload payloadDefinition = itPayload.next();
        assertEquals("payload1", payloadDefinition.getName());
        assertEquals("string", payloadDefinition.getType());
        
        payloadDefinition = itPayload.next();
        assertEquals("payload2", payloadDefinition.getName());
        assertEquals("integer", payloadDefinition.getType());

        EventDeployment redeployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/test/deployment/simpleEvent2.event")
                .deploy();

        eventDefinition = repositoryService.createEventDefinitionQuery()
                .eventDefinitionKey("myEvent")
                .latestVersion()
                .singleResult();
        assertNotNull(eventDefinition);
        assertEquals("myEvent", eventDefinition.getKey());
        assertEquals(2, eventDefinition.getVersion());

        eventModel = repositoryService.getEventModelById(eventDefinition.getId());
        assertEquals("myEvent", eventModel.getKey());
        
        assertEquals(1, eventModel.getInboundChannelKeys().size());
        assertEquals("test-channel2", eventModel.getInboundChannelKeys().iterator().next());
        
        assertEquals(1, eventModel.getCorrelationParameters().size());
        correlationParameter = eventModel.getCorrelationParameters().iterator().next();
        assertEquals("customerId2", correlationParameter.getName());
        assertEquals("string", correlationParameter.getType());
        
        assertEquals(2, eventModel.getPayload().size());
        itPayload = eventModel.getPayload().iterator();
        payloadDefinition = itPayload.next();
        assertEquals("payload3", payloadDefinition.getName());
        assertEquals("string", payloadDefinition.getType());
        
        payloadDefinition = itPayload.next();
        assertEquals("payload4", payloadDefinition.getName());
        assertEquals("integer", payloadDefinition.getType());

        repositoryService.deleteDeployment(redeployment.getId());
    }
    
    @Test
    @EventDeploymentAnnotation(resources = "org/flowable/eventregistry/test/deployment/simpleChannel.channel")
    public void redeploySingleChannelDefinition() {
        ChannelDefinition channelDefinition = repositoryService.createChannelDefinitionQuery()
                .channelDefinitionKey("myChannel")
                .latestVersion()
                .singleResult();
        assertNotNull(channelDefinition);
        assertEquals("myChannel", channelDefinition.getKey());
        assertEquals(1, channelDefinition.getVersion());

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
        assertNotNull(channelDefinition);
        assertEquals("myChannel", channelDefinition.getKey());
        assertEquals(2, channelDefinition.getVersion());

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
    @EventDeploymentAnnotation(resources = { "org/flowable/eventregistry/test/deployment/simpleEvent.event",
            "org/flowable/eventregistry/test/deployment/orderEvent.event" })
    public void deploy2EventDefinitions() {
        List<EventDefinition> eventDefinitions = repositoryService.createEventDefinitionQuery().orderByEventDefinitionName().asc().list();
        assertEquals(2, eventDefinitions.size());

        assertEquals("My event", eventDefinitions.get(0).getName());
        assertEquals("My order event", eventDefinitions.get(1).getName());
    }
    
    @Test
    @ChannelDeploymentAnnotation(resources = { "org/flowable/eventregistry/test/deployment/simpleChannel.channel",
            "org/flowable/eventregistry/test/deployment/orderChannel.channel" })
    public void deploy2ChannelDefinitions() {
        List<ChannelDefinition> channelDefinitions = repositoryService.createChannelDefinitionQuery().orderByChannelDefinitionName().asc().list();
        assertEquals(2, channelDefinitions.size());

        assertEquals("My channel", channelDefinitions.get(0).getName());
        assertEquals("Order channel", channelDefinitions.get(1).getName());
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
            assertNotNull(definition);
            assertEquals("myEvent", definition.getKey());
            assertEquals(1, definition.getVersion());
            
            EventDefinition newDefinition = repositoryService.createEventDefinitionQuery().deploymentId(newDeployment.getId()).singleResult();
            assertNotNull(newDefinition);
            assertEquals("myEvent", newDefinition.getKey());
            assertEquals(2, newDefinition.getVersion());
            
            EventModel eventModel = repositoryService.getEventModelByKeyAndParentDeploymentId("myEvent", "someDeploymentId");
            assertEquals("myEvent", eventModel.getKey());
            assertEquals("My event", eventModel.getName());
            
            eventEngineConfiguration.setAlwaysLookupLatestDefinitionVersion(true);
            eventModel = repositoryService.getEventModelByKeyAndParentDeploymentId("myEvent", "someDeploymentId");
            assertEquals("myEvent", eventModel.getKey());
            assertEquals("My event2", eventModel.getName());
        
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
            assertNotNull(definition);
            assertEquals("myChannel", definition.getKey());
            assertEquals(1, definition.getVersion());
            
            ChannelDefinition newDefinition = repositoryService.createChannelDefinitionQuery().deploymentId(newDeployment.getId()).singleResult();
            assertNotNull(newDefinition);
            assertEquals("myChannel", newDefinition.getKey());
            assertEquals(2, newDefinition.getVersion());
            
            ChannelModel channelModel = repositoryService.getChannelModelByKeyAndParentDeploymentId("myChannel", "someDeploymentId");
            assertEquals("myChannel", channelModel.getKey());
            assertEquals("My channel", channelModel.getName());
            
            eventEngineConfiguration.setAlwaysLookupLatestDefinitionVersion(true);
            channelModel = repositoryService.getChannelModelByKeyAndParentDeploymentId("myChannel", "someDeploymentId");
            assertEquals("myChannel", channelModel.getKey());
            assertEquals("My channel2", channelModel.getName());
        
        } finally {
            eventEngineConfiguration.setAlwaysLookupLatestDefinitionVersion(false);
            repositoryService.deleteDeployment(deployment.getId());
            repositoryService.deleteDeployment(newDeployment.getId());
        }
    }
}
