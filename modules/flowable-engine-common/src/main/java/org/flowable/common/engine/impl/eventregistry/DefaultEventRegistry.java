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
package org.flowable.common.engine.impl.eventregistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.eventbus.FlowableEventBus;
import org.flowable.common.engine.api.eventregistry.CorrelationKeyGenerator;
import org.flowable.common.engine.api.eventregistry.EventRegistry;
import org.flowable.common.engine.api.eventregistry.EventRegistryEventBusConsumer;
import org.flowable.common.engine.api.eventregistry.InboundEventProcessor;
import org.flowable.common.engine.api.eventregistry.definition.ChannelDefinition;
import org.flowable.common.engine.api.eventregistry.definition.ChannelDefinitionBuilder;
import org.flowable.common.engine.api.eventregistry.definition.EventDefinition;
import org.flowable.common.engine.api.eventregistry.definition.EventDefinitionBuilder;
import org.flowable.common.engine.impl.eventregistry.definition.ChannelDefinitionBuilderImpl;
import org.flowable.common.engine.impl.eventregistry.definition.EventDefinitionBuilderImpl;

/**
 * @author Joram Barrez
 */
public class DefaultEventRegistry implements EventRegistry {

    protected FlowableEventBus eventBus;
    protected Map<String, ChannelDefinition> channelDefinitions = new HashMap<>();

    protected Map<String, EventDefinition> eventDefinitionsByKey = new HashMap<>();

    protected List<EventRegistryEventBusConsumer> eventRegistryEventBusConsumers = new ArrayList<>();

    protected InboundEventProcessor inboundEventProcessor;
    protected CorrelationKeyGenerator<Map<String, Object>> correlationKeyGenerator;

    public DefaultEventRegistry(FlowableEventBus eventBus) {
        this.eventBus = eventBus;
        this.correlationKeyGenerator = new DefaultCorrelationKeyGenerator();
    }

    @Override
    public ChannelDefinitionBuilder newChannelDefinition() {
        return new ChannelDefinitionBuilderImpl(this);
    }

    @Override
    public void registerChannelDefinition(ChannelDefinition channelDefinition) {
        if (StringUtils.isEmpty(channelDefinition.getKey())) {
            throw new FlowableIllegalArgumentException("No key set for channel definition");
        }

        if ( (channelDefinition.getInboundEventChannelAdapter() != null && channelDefinition.getInboundEventProcessingPipeline() == null)
            || (channelDefinition.getInboundEventChannelAdapter() == null && channelDefinition.getInboundEventProcessingPipeline() != null)) {
            throw new FlowableIllegalArgumentException("Need to set both inbound channel adapter and inbound event key detector when one of both is set");
        }

        channelDefinitions.put(channelDefinition.getKey(), channelDefinition);

        if (channelDefinition.getInboundEventChannelAdapter() != null) {
            channelDefinition.getInboundEventChannelAdapter().setEventRegistry(this);
            channelDefinition.getInboundEventChannelAdapter().setChannelKey(channelDefinition.getKey());
        }
    }

    @Override
    public void removeChannelDefinition(String channelDefinitionKey) {
        channelDefinitions.remove(channelDefinitionKey);
    }

    @Override
    public ChannelDefinition getChannelDefinition(String channelKey) {
        return channelDefinitions.get(channelKey);
    }

    @Override
    public EventDefinitionBuilder newEventDefinition() {
        return new EventDefinitionBuilderImpl(this);
    }

    @Override
    public void registerEventDefinition(EventDefinition eventDefinition) {
        eventDefinitionsByKey.put(eventDefinition.getKey(), eventDefinition);

        // The eventRegistryEventBusConsumers contains the engine-specific listeners for events
        // related to event definitions registered with this event registry.
        // When a new event definition is added, they need to be reregistered, as the eventBus implementation
        // captures and stores the types at registration time and not on event receiving (for performance).
        for (EventRegistryEventBusConsumer eventRegistryEventBusConsumer : eventRegistryEventBusConsumers) {
            eventBus.removeFlowableEventConsumer(eventRegistryEventBusConsumer);

            if (!eventRegistryEventBusConsumer.getSupportedTypes().contains(eventDefinition.getKey())) {
                eventRegistryEventBusConsumer.getSupportedTypes().add(eventDefinition.getKey());
            }
            eventBus.addFlowableEventConsumer(eventRegistryEventBusConsumer);
        }
    }

    @Override
    public void removeEventDefinition(String eventDefinitionKey) {
        eventDefinitionsByKey.remove(eventDefinitionKey);

        // Similar as for the addition (see the comment there)
        for (EventRegistryEventBusConsumer eventRegistryEventBusConsumer : eventRegistryEventBusConsumers) {
            eventBus.removeFlowableEventConsumer(eventRegistryEventBusConsumer);

            eventRegistryEventBusConsumer.getSupportedTypes().remove(eventDefinitionKey);
            eventBus.addFlowableEventConsumer(eventRegistryEventBusConsumer);
        }
    }

    @Override
    public Collection<EventDefinition> getAllEventDefinitions() {
        return eventDefinitionsByKey.values();
    }

    @Override
    public EventDefinition getEventDefinition(String eventDefinitionKey) {
        return eventDefinitionsByKey.get(eventDefinitionKey);
    }

    @Override
    public void setInboundEventProcessor(InboundEventProcessor inboundEventProcessor) {
        this.inboundEventProcessor = inboundEventProcessor;
    }

    @Override
    public void eventReceived(String channelKey, String event) {
        inboundEventProcessor.eventReceived(channelKey, event);
    }

    @Override
    public void registerEventRegistryEventBusConsumer(EventRegistryEventBusConsumer eventRegistryEventBusConsumer) {
        eventRegistryEventBusConsumers.add(eventRegistryEventBusConsumer);
    }

    @Override
    public String generateKey(Map<String, Object> data) {
        return correlationKeyGenerator.generateKey(data);
    }
}
