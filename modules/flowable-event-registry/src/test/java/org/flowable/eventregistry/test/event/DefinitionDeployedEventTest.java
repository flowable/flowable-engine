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
package org.flowable.eventregistry.test.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventType;
import org.flowable.common.engine.api.repository.FlowableDefinition;
import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.flowable.eventregistry.test.AbstractFlowableEventTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefinitionDeployedEventTest extends AbstractFlowableEventTest {

    private final CapturingListener listener = new CapturingListener();
    private Map<Object, Object> initialBeans;

    @BeforeEach
    void addListener() {
        initialBeans = eventEngineConfiguration.getExpressionManager().getBeans();
        Map<Object, Object> testBeans = new HashMap<>();
        testBeans.put("inboundEventChannelAdapter", new NoOpInboundEventChannelAdapter());
        eventEngineConfiguration.getExpressionManager().setBeans(testBeans);
        eventEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @AfterEach
    void removeListener() {
        eventEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        eventEngineConfiguration.getExpressionManager().setBeans(initialBeans);
    }

    @Test
    void deployEventDefinitionDispatchesDefinitionDeployedEvent() {
        EventDeployment deployment = repositoryService.createEventModelBuilder()
                .key("definitionEvent")
                .resourceName("definitionEvent.event")
                .correlationParameter("customerId", EventPayloadTypes.STRING)
                .deploy();
        String deploymentId = deployment.getId();
        try {
            EventDefinition eventDefinition = repositoryService.createEventDefinitionQuery()
                    .deploymentId(deploymentId).singleResult();

            assertThat(listener.deployed).hasSize(1);
            FlowableDefinition deployed = (FlowableDefinition) listener.deployed.get(0).getEntity();
            assertThat(deployed.getKey()).isEqualTo(eventDefinition.getKey());
            assertThat(deployed.getVersion()).isEqualTo(eventDefinition.getVersion());
            assertThat(deployed.getDeploymentId()).isEqualTo(deploymentId);
        } finally {
            repositoryService.deleteDeployment(deploymentId);
        }
    }

    @Test
    void deleteEventDeploymentDispatchesDefinitionUndeployedEvent() {
        EventDeployment deployment = repositoryService.createEventModelBuilder()
                .key("definitionEvent")
                .resourceName("definitionEvent.event")
                .correlationParameter("customerId", EventPayloadTypes.STRING)
                .deploy();
        String deploymentId = deployment.getId();
        EventDefinition eventDefinition = repositoryService.createEventDefinitionQuery()
                .deploymentId(deploymentId).singleResult();

        repositoryService.deleteDeployment(deploymentId);

        assertThat(listener.undeployed).hasSize(1);
        FlowableDefinition undeployed = (FlowableDefinition) listener.undeployed.get(0).getEntity();
        assertThat(undeployed.getKey()).isEqualTo(eventDefinition.getKey());
        assertThat(undeployed.getVersion()).isEqualTo(eventDefinition.getVersion());
        assertThat(undeployed.getDeploymentId()).isEqualTo(deploymentId);
    }

    @Test
    void deployChannelDefinitionDispatchesDefinitionDeployedEvent() {
        EventDeployment deployment = repositoryService.createInboundChannelModelBuilder()
                .key("definitionChannel")
                .resourceName("definitionChannel.channel")
                .channelAdapter("${inboundEventChannelAdapter}")
                .jsonDeserializer()
                .detectEventKeyUsingJsonField("type")
                .jsonFieldsMapDirectlyToPayload()
                .deploy();
        String deploymentId = deployment.getId();
        try {
            assertThat(listener.deployed).hasSize(1);
            FlowableDefinition deployed = (FlowableDefinition) listener.deployed.get(0).getEntity();
            assertThat(deployed.getKey()).isEqualTo("definitionChannel");
            assertThat(deployed.getVersion()).isEqualTo(1);
            assertThat(deployed.getDeploymentId()).isEqualTo(deploymentId);
        } finally {
            repositoryService.deleteDeployment(deploymentId);
        }
    }

    static class CapturingListener extends AbstractFlowableEventListener {

        final List<FlowableEntityEvent> deployed = new ArrayList<>();
        final List<FlowableEntityEvent> undeployed = new ArrayList<>();

        @Override
        public void onEvent(FlowableEvent event) {
            if (event.getType() == FlowableEngineEventType.DEFINITION_DEPLOYED) {
                deployed.add((FlowableEntityEvent) event);
            } else if (event.getType() == FlowableEngineEventType.DEFINITION_UNDEPLOYED) {
                undeployed.add((FlowableEntityEvent) event);
            }
        }

        @Override
        public boolean isFailOnException() {
            return true;
        }

        @Override
        public Collection<? extends FlowableEventType> getTypes() {
            return List.of(FlowableEngineEventType.DEFINITION_DEPLOYED, FlowableEngineEventType.DEFINITION_UNDEPLOYED);
        }
    }

    private static class NoOpInboundEventChannelAdapter implements InboundEventChannelAdapter {

        @Override
        public void setInboundChannelModel(InboundChannelModel inboundChannelModel) {
        }

        @Override
        public void setEventRegistry(EventRegistry eventRegistry) {
        }
    }
}
