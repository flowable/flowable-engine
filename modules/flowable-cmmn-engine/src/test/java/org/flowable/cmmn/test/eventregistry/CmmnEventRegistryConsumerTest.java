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
import org.flowable.common.engine.impl.eventregistry.JsonFieldBasedInboundEventKeyDetector;
import org.flowable.common.engine.impl.eventregistry.deserializer.StringToJsonDeserializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public class CmmnEventRegistryConsumerTest extends FlowableCmmnTestCase {

    protected TestInboundEventChannelAdapter inboundEventChannelAdapter;

    @Before
    public void registerEventDefinition() {
        inboundEventChannelAdapter = setupTestChannel();

        cmmnEngineConfiguration.getEventRegistry().newEventDefinition()
            .channelKey("test-channel")
            .key("myEvent")
            .correlation().appliesAlways()
            .correlationParameter("customerId", EventPayloadTypes.STRING)
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
            inboundEventChannelAdapter.triggerTestEvent();
            assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(i);
        }
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

        public void triggerTestEvent() {
            ObjectMapper objectMapper = new ObjectMapper();

            ObjectNode json = objectMapper.createObjectNode();
            json.put("type", "myEvent");
            json.put("customerId", "test");
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
