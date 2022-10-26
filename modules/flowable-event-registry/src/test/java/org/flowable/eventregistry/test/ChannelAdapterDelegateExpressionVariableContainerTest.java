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
package org.flowable.eventregistry.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRegistryEvent;
import org.flowable.eventregistry.api.InboundEvent;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.api.InboundEventProcessingPipeline;
import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.flowable.eventregistry.api.OutboundEventProcessingPipeline;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.model.DelegateExpressionInboundChannelModel;
import org.flowable.eventregistry.model.DelegateExpressionOutboundChannelModel;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Aleksandar Matic
 */
public class ChannelAdapterDelegateExpressionVariableContainerTest extends AbstractFlowableEventTest {

    protected Map<Object, Object> initialBeans;

    @BeforeEach
    void setup() {
        eventEngineConfiguration.setFallbackToDefaultTenant(true);
        initialBeans = eventEngineConfiguration.getExpressionManager().getBeans();
        eventEngineConfiguration.getExpressionManager().setBeans(new HashMap<>());
    }

    @AfterEach
    public void cleanup () {
        eventRegistryEngine.getEventRepositoryService().createDeploymentQuery().list()
            .forEach(eventDeployment -> eventRegistryEngine.getEventRepositoryService().deleteDeployment(eventDeployment.getId()));

        eventEngineConfiguration.getExpressionManager().setBeans(initialBeans);
        eventEngineConfiguration.setFallbackToDefaultTenant(false);
    }


    @Test
    public void testCustomInboundPipelineInvoked() {
        TestInboundEventProcessingPipeline testInboundEventProcessingPipeline = new TestInboundEventProcessingPipeline();
        TestInboundChannelAdapter testInboundChannelAdapterAcme = new TestInboundChannelAdapter();
        TestInboundChannelAdapter testInboundChannelAdapterMegacorp = new TestInboundChannelAdapter();
        Map<String, TestInboundChannelAdapter> adapters = new HashMap<>();
        adapters.put("acme", testInboundChannelAdapterAcme);
        adapters.put("megacorp", testInboundChannelAdapterMegacorp);
        Map<Object, Object> beans = eventEngineConfiguration.getExpressionManager().getBeans();
        beans.put("adapters", adapters);
        beans.put("testInboundEventProcessingPipeline", testInboundEventProcessingPipeline);

        eventRegistryEngine.getEventRepositoryService().createInboundChannelModelBuilder()
            .key("customTestChannel")
            .resourceName("customTest.channel")
            .channelAdapter("${adapters.get(tenantId)}")
            .eventProcessingPipeline("${testInboundEventProcessingPipeline}")
            .deploymentTenantId("acme")
            .deploy();

        eventRegistryEngine.getEventRepositoryService().createInboundChannelModelBuilder()
            .key("customTestChannel")
            .resourceName("customTest.channel")
            .channelAdapter("${adapters.get(variableContainer.tenantId)}")
            .eventProcessingPipeline("${testInboundEventProcessingPipeline}")
            .deploymentTenantId("megacorp")
            .deploy();

        DelegateExpressionInboundChannelModel channelModelAcme = (DelegateExpressionInboundChannelModel) eventRegistryEngine
                .getEventRepositoryService()
                .getChannelModelByKey("customTestChannel", "acme");

        DelegateExpressionInboundChannelModel channelModelMegacorp = (DelegateExpressionInboundChannelModel) eventRegistryEngine
                .getEventRepositoryService()
                .getChannelModelByKey("customTestChannel", "megacorp");

        assertThat(channelModelAcme.getInboundEventChannelAdapter()).isEqualTo(testInboundChannelAdapterAcme);
        assertThat(channelModelMegacorp.getInboundEventChannelAdapter()).isEqualTo(testInboundChannelAdapterMegacorp);
    }

    @Test
    public void testCustomOutboundPipelineInvoked() {
        TestOutboundEventProcessingPipeline testOutboundEventProcessingPipeline = new TestOutboundEventProcessingPipeline();
        TestOutboundChannelAdapter testOutboundChannelAdapterAcme = new TestOutboundChannelAdapter();
        TestOutboundChannelAdapter testOutboundChannelAdapterMegacorp = new TestOutboundChannelAdapter();
        Map<String, TestOutboundChannelAdapter> adapters = new HashMap<>();
        adapters.put("acme", testOutboundChannelAdapterAcme);
        adapters.put("megacorp", testOutboundChannelAdapterMegacorp);
        Map<Object, Object> beans = eventEngineConfiguration.getExpressionManager().getBeans();
        beans.put("adapters", adapters);
        beans.put("testOutboundEventProcessingPipeline", testOutboundEventProcessingPipeline);

        eventRegistryEngine.getEventRepositoryService().createOutboundChannelModelBuilder()
            .key("customTestOutboundChannel")
            .resourceName("customOutboundTest.channel")
            .channelAdapter("${adapters.get(tenantId)}")
            .eventProcessingPipeline("${testOutboundEventProcessingPipeline}")
            .deploymentTenantId("acme")
            .deploy();

        eventRegistryEngine.getEventRepositoryService().createOutboundChannelModelBuilder()
            .key("customTestOutboundChannel")
            .resourceName("customOutboundTest.channel")
            .channelAdapter("${adapters.get(variableContainer.tenantId)}")
            .eventProcessingPipeline("${testOutboundEventProcessingPipeline}")
            .deploymentTenantId("megacorp")
            .deploy();

        DelegateExpressionOutboundChannelModel channelModelAcme = (DelegateExpressionOutboundChannelModel) eventRegistryEngine
                .getEventRepositoryService()
                .getChannelModelByKey("customTestOutboundChannel", "acme");

        DelegateExpressionOutboundChannelModel channelModelMegacorp = (DelegateExpressionOutboundChannelModel) eventRegistryEngine
                .getEventRepositoryService()
                .getChannelModelByKey("customTestOutboundChannel", "megacorp");

        assertThat(channelModelAcme.getOutboundEventChannelAdapter()).isEqualTo(testOutboundChannelAdapterAcme);
        assertThat(channelModelMegacorp.getOutboundEventChannelAdapter()).isEqualTo(testOutboundChannelAdapterMegacorp);
    }

    private static class TestInboundChannelAdapter implements InboundEventChannelAdapter {

        @Override
        public void setInboundChannelModel(InboundChannelModel inboundChannelModel) {

        }

        @Override
        public void setEventRegistry(EventRegistry eventRegistry) {

        }
    }

    private static class TestOutboundChannelAdapter implements OutboundEventChannelAdapter<String> {

        @Override
        public void sendEvent(String rawEvent, Map<String, Object> headerMap) {

        }

    }

    private static class TestInboundEventProcessingPipeline implements InboundEventProcessingPipeline {

        public AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Collection<EventRegistryEvent> run(InboundChannelModel inboundChannel, InboundEvent rawEvent) {
            counter.incrementAndGet();
            return Collections.emptyList();
        }
    }

    private static class TestOutboundEventProcessingPipeline implements OutboundEventProcessingPipeline<String> {

        public AtomicInteger counter = new AtomicInteger(0);

        @Override
        public String run(EventInstance eventInstance) {
            counter.incrementAndGet();
            return "test";
        }
    }

}
