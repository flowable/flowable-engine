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
package org.flowable.cmmn.test.eventregistry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.eventregistry.EventRegistry;
import org.flowable.common.engine.api.eventregistry.InboundEventChannelAdapter;
import org.flowable.common.engine.api.eventregistry.definition.EventPayloadTypes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class CmmnEventRegistryConsumerTest extends FlowableCmmnTestCase {

    protected TestInboundEventChannelAdapter inboundEventChannelAdapter;

    @Before
    public void registerEventDefinition() {
        inboundEventChannelAdapter = setupTestChannel();

        cmmnEngineConfiguration.getEventRegistry().newEventDefinition()
            .channelKey("test-channel")
            .key("myEvent")
            .correlationParameter("customerId", EventPayloadTypes.STRING)
            .correlationParameter("orderId", EventPayloadTypes.STRING)
            .payload("payload1", EventPayloadTypes.STRING)
            .payload("payload2", EventPayloadTypes.INTEGER)
            .register();
    }

    protected TestInboundEventChannelAdapter setupTestChannel() {
        TestInboundEventChannelAdapter inboundEventChannelAdapter = new TestInboundEventChannelAdapter();

        cmmnEngineConfiguration.getEventRegistry().newChannelDefinition()
            .key("test-channel")
            .channelAdapter(inboundEventChannelAdapter)
            .jsonDeserializer()
            .detectEventKeyUsingJsonField("type")
            .jsonFieldsMapDirectlyToPayload()
            .register();

        return inboundEventChannelAdapter;
    }


    @After
    public void unregisterEventDefinition() {
        cmmnEngineConfiguration.getEventRegistry().removeChannelDefinition("test-channel");
        cmmnEngineConfiguration.getEventRegistry().removeEventDefinition("myEvent");
    }

    @Test
    @CmmnDeployment
    public void testGenericEventListenerNoCorrelation() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(1);

        for (int i = 2; i < 10; i++) {
            inboundEventChannelAdapter.triggerTestEvent("test");
            assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(i);
        }
    }

    @Test
    @CmmnDeployment
    public void testGenericEventListenerWithCorrelation() {
        CaseInstance kermitCase = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("singleCorrelationCase")
            .variable("customerIdVar", "kermit")
            .start();
        CaseInstance gonzoCase = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("singleCorrelationCase")
            .variable("customerIdVar", "gonzo")
            .start();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitCase.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoCase.getId()).list()).hasSize(1);

        inboundEventChannelAdapter.triggerTestEvent("kermit");
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitCase.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoCase.getId()).list()).hasSize(1);

        inboundEventChannelAdapter.triggerTestEvent("gonzo");
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitCase.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoCase.getId()).list()).hasSize(2);

        inboundEventChannelAdapter.triggerTestEvent("fozzie");
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitCase.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoCase.getId()).list()).hasSize(2);
    }

    @Test
    @CmmnDeployment(resources = {
        "org/flowable/cmmn/test/eventregistry/CmmnEventRegistryConsumerTest.testGenericEventListenerNoCorrelation.cmmn",
        "org/flowable/cmmn/test/eventregistry/CmmnEventRegistryConsumerTest.testGenericEventListenerWithCorrelation.cmmn",
        "org/flowable/cmmn/test/eventregistry/CmmnEventRegistryConsumerTest.testGenericEventListenerWithOrderCorrelation.cmmn",
        "org/flowable/cmmn/test/eventregistry/CmmnEventRegistryConsumerTest.testGenericEventListenerWithTwoCorrelations.cmmn"
    })
    public void testGenericEventListenerWithMultipleCorrelations() {
        CaseInstance noCorrelationCase = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("myCase")
            .start();
        CaseInstance kermitOrder1Case = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("twoCorrelationsCase")
            .variable("customerIdVar", "kermit")
            .variable("orderIdVar", "order1")
            .start();
        CaseInstance kermitOrder2Case = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("twoCorrelationsCase")
            .variable("customerIdVar", "kermit")
            .variable("orderIdVar", "order2")
            .start();
        CaseInstance gonzoOrder1Case = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("twoCorrelationsCase")
            .variable("customerIdVar", "gonzo")
            .variable("orderIdVar", "order1")
            .start();
        CaseInstance gonzoCase = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("singleCorrelationCase")
            .variable("customerIdVar", "gonzo")
            .start();
        CaseInstance order1Case = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("orderCorrelationCase")
            .variable("orderIdVar", "order1")
            .start();
        CaseInstance order2Case = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("orderCorrelationCase")
            .variable("orderIdVar", "order2")
            .start();

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(noCorrelationCase.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder2Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoOrder1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoCase.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order2Case.getId()).list()).hasSize(1);

        inboundEventChannelAdapter.triggerTestEvent("kermit");
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(noCorrelationCase.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder2Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoOrder1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoCase.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order2Case.getId()).list()).hasSize(1);

        inboundEventChannelAdapter.triggerTestEvent("gonzo");
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(noCorrelationCase.getId()).list()).hasSize(3);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder2Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoOrder1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoCase.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order2Case.getId()).list()).hasSize(1);

        inboundEventChannelAdapter.triggerTestEvent("fozzie");
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(noCorrelationCase.getId()).list()).hasSize(4);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder2Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoOrder1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoCase.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order2Case.getId()).list()).hasSize(1);

        inboundEventChannelAdapter.triggerTestEvent("kermit", "order1");
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(noCorrelationCase.getId()).list()).hasSize(5);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder1Case.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder2Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoOrder1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoCase.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order1Case.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order2Case.getId()).list()).hasSize(1);

        inboundEventChannelAdapter.triggerTestEvent("gonzo", "order1");
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(noCorrelationCase.getId()).list()).hasSize(6);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder1Case.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder2Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoOrder1Case.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoCase.getId()).list()).hasSize(3);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order1Case.getId()).list()).hasSize(3);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order2Case.getId()).list()).hasSize(1);

        inboundEventChannelAdapter.triggerOrderTestEvent("order2");
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(noCorrelationCase.getId()).list()).hasSize(7);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder1Case.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder2Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoOrder1Case.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoCase.getId()).list()).hasSize(3);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order1Case.getId()).list()).hasSize(3);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order2Case.getId()).list()).hasSize(2);
    }

    private static class TestInboundEventChannelAdapter implements InboundEventChannelAdapter {

        public String channelKey;
        public EventRegistry eventRegistry;

        @Override
        public void setChannelKey(String channelKey) {
            this.channelKey = channelKey;
        }

        @Override
        public void setEventRegistry(EventRegistry eventRegistry) {
            this.eventRegistry = eventRegistry;
        }

        public void triggerTestEvent(String customerId) {
            triggerTestEvent(customerId, null);
        }

        public void triggerOrderTestEvent(String orderId) {
            triggerTestEvent(null, orderId);
        }

        public void triggerTestEvent(String customerId, String orderId) {
            ObjectMapper objectMapper = new ObjectMapper();

            ObjectNode json = objectMapper.createObjectNode();
            json.put("type", "myEvent");
            if (customerId != null) {
                json.put("customerId", customerId);
            }

            if (orderId != null) {
                json.put("orderId", orderId);
            }
            json.put("payload1", "Hello World");
            json.put("payload2", new Random().nextInt());
            try {
                eventRegistry.eventReceived(channelKey, objectMapper.writeValueAsString(json));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
