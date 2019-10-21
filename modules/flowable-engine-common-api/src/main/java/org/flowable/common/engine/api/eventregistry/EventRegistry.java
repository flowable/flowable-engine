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
package org.flowable.common.engine.api.eventregistry;

import java.util.Collection;
import java.util.Map;

import org.flowable.common.engine.api.eventbus.FlowableEventBusEvent;
import org.flowable.common.engine.api.eventregistry.definition.ChannelDefinition;
import org.flowable.common.engine.api.eventregistry.definition.EventDefinition;
import org.flowable.common.engine.api.eventregistry.definition.EventDefinitionBuilder;
import org.flowable.common.engine.api.eventregistry.definition.InboundChannelDefinition;
import org.flowable.common.engine.api.eventregistry.definition.InboundChannelDefinitionBuilder;
import org.flowable.common.engine.api.eventregistry.definition.OutboundChannelDefinition;
import org.flowable.common.engine.api.eventregistry.definition.OutboundChannelDefinitionBuilder;
import org.flowable.common.engine.api.eventregistry.runtime.EventInstance;

/**
 * Central registry for events that are received through external channels through a {@link InboundEventChannelAdapter}
 * and then passed through to the {@link FlowableEventBusEvent}.
 *
 * @author Joram Barrez
 */
public interface EventRegistry {

    /**
     * Programmatically build and register a new {@link InboundChannelDefinition}.
     */
    InboundChannelDefinitionBuilder newInboundChannelDefinition();

    /**
     * Returns the {@link InboundChannelDefinition} instance associated with the given key.
     */
    InboundChannelDefinition getInboundChannelDefinition(String channelKey);

    /**
     * Programmatically build and register a new {@link OutboundChannelDefinition}.
     */
    OutboundChannelDefinitionBuilder newOutboundChannelDefinition();

    /**
     * Returns the {@link OutboundChannelDefinition} instance associated with the given key.
     */
    OutboundChannelDefinition getOutboundChannelDefinition(String channelKey);

    /**
     * Low-level (vs the {@link InboundChannelDefinitionBuilder}) way of registering a new {@link ChannelDefinition}.
     */
    void registerChannelDefinition(ChannelDefinition channelDefinition);

    /**
     * Removes a previously registered {@link ChannelDefinition}.
     */
    void removeChannelDefinition(String channelDefinitionKey);

    /**
     * Programmatically build and register a new {@link EventDefinition}.
     */
    EventDefinitionBuilder newEventDefinition();

    /**
     * Low-level (vs the {@link EventDefinitionBuilder}) way of registering a new {@link EventDefinition}.
     */
    void registerEventDefinition(EventDefinition eventDefinition);

    /**
     * Removes a previously registered {@link EventDefinition}.
     */
    void removeEventDefinition(String eventDefinitionKey);

    /**
     * Returns all currently configured {@link EventDefinition} instances registered in this registry.
     */
    Collection<EventDefinition> getAllEventDefinitions();

    /**
     * Retrieves the {@link EventDefinition} for the given eventDefinitionKey.
     */
    EventDefinition getEventDefinition(String eventDefinitionKey);

    /**
     * The {@link InboundEventProcessor} is responsible for handling any new event.
     * The event registry will simply pass any event it receives to this instance.
     */
    void setInboundEventProcessor(InboundEventProcessor inboundEventProcessor);

    /**
     * The {@link OutboundEventProcessor} is responsible for handling sending out events.
     * The event registry will simply pass any event it needs to send to this instance.
     */
    void setOutboundEventProcessor(OutboundEventProcessor outboundEventProcessor);

    /**
     * Registers a {@link EventRegistryEventBusConsumer} instance (a consumer of event registry events which
     * is created by any of the engines).
     */
    void registerEventRegistryEventBusConsumer(EventRegistryEventBusConsumer eventRegistryEventBusConsumer);

    /**
     * Method to generate the unique key used to correlate an event.
     *
     * @param data data information used to generate the key (must not be {@code null})
     * @return a unique string correlating the event based on the information supplied
     */
    String generateKey(Map<String, Object> data);

    /**
     * Events received in adapters should call this method to process events.
     */
    void eventReceived(String channelKey, String event);

    /**
     * Send out an event. The corresponding {@link EventDefinition} will be used to
     * decide which channel (and pipeline) will be used
     */
    void sendEvent(EventInstance eventInstance);
}
