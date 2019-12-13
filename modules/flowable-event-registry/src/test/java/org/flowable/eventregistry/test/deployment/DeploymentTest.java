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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Iterator;
import java.util.List;

import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.model.EventCorrelationParameterDefinition;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.EventPayloadDefinition;
import org.flowable.eventregistry.test.AbstractFlowableEventTest;
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
        EventCorrelationParameterDefinition correlationParameter = eventModel.getCorrelationParameters().iterator().next();
        assertEquals("customerId", correlationParameter.getName());
        assertEquals("string", correlationParameter.getType());
        
        assertEquals(2, eventModel.getPayload().size());
        Iterator<EventPayloadDefinition> itPayload = eventModel.getPayload().iterator();
        EventPayloadDefinition payloadDefinition = itPayload.next();
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
    @EventDeploymentAnnotation(resources = { "org/flowable/eventregistry/test/deployment/simpleEvent.event",
            "org/flowable/eventregistry/test/deployment/orderEvent.event" })
    public void deploy2EventDefinitions() {
        List<EventDefinition> eventDefinitions = repositoryService.createEventDefinitionQuery().orderByEventDefinitionName().asc().list();
        assertEquals(2, eventDefinitions.size());

        assertEquals("My event", eventDefinitions.get(0).getName());
        assertEquals("My order event", eventDefinitions.get(1).getName());
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
}
