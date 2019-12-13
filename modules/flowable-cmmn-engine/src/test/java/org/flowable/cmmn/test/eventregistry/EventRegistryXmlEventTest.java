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

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class EventRegistryXmlEventTest extends FlowableCmmnTestCase {

    protected TestInboundEventChannelAdapter inboundEventChannelAdapter;

    @Before
    public void registerEventDefinition() {
        inboundEventChannelAdapter = setupTestChannel();

        getEventRepositoryService().createEventModelBuilder()
            .inboundChannelKey("test-channel")
            .key("myEvent")
            .resourceName("myEvent.event")
            .correlationParameter("customerId", EventPayloadTypes.STRING)
            .payload("name", EventPayloadTypes.STRING)
            .deploy();
    }

    protected TestInboundEventChannelAdapter setupTestChannel() {
        TestInboundEventChannelAdapter inboundEventChannelAdapter = new TestInboundEventChannelAdapter();

        getEventRegistry().newInboundChannelDefinition()
            .key("test-channel")
            .channelAdapter(inboundEventChannelAdapter)
            .xmlDeserializer()
            .fixedEventKey("myEvent")
            .xmlElementsMapDirectlyToPayload()
            .register();

        return inboundEventChannelAdapter;
    }


    @After
    public void unregisterEventDefinition() {
        getEventRegistry().removeChannelDefinition("test-channel");
        EventRepositoryService eventRepositoryService = getEventRepositoryService();
        List<EventDeployment> deployments = eventRepositoryService.createDeploymentQuery().list();
        for (EventDeployment eventDeployment : deployments) {
            eventRepositoryService.deleteDeployment(eventDeployment.getId());
        }
    }

    @Test
    @CmmnDeployment
    public void testGenericEventListenerNoCorrelation() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(1);

        inboundEventChannelAdapter.testSendXmlEvent();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(2);
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

        public void testSendXmlEvent() {
            String event = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<customerEvent>\n"
                + "    <customerId>12345</customerId>\n"
                + "    <name>Customer name</name>\n"
                + "</customerEvent>";

            eventRegistry.eventReceived(channelKey, event);
        }

    }

}
