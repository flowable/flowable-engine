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
package org.flowable.eventregistry.api;

import java.util.Map;

import org.flowable.eventregistry.api.model.InboundChannelDefinitionBuilder;
import org.flowable.eventregistry.api.model.OutboundChannelDefinitionBuilder;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.model.ChannelDefinition;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.InboundChannelDefinition;
import org.flowable.eventregistry.model.OutboundChannelDefinition;

/**
 * Central registry for events that are received through external channels through a {@link InboundEventChannelAdapter}
 * and then passed through as a event registry event.
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
     * Returns all the {@link InboundChannelDefinition} instances.
     */
    Map<String, InboundChannelDefinition> getInboundChannelDefinitions();

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
     * Retrieves the {@link EventModel} for the given eventDefinitionKey.
     */
    EventModel getEventModel(String eventDefinitionKey);

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
     * Removes the event consumer from the event registry
     */
    void removeFlowableEventConsumer(EventRegistryEventBusConsumer eventRegistryEventBusConsumer);

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
     * Send an event to all the registered event consumers.
     */
    void sendEventToConsumers(EventRegistryEvent eventRegistryEvent);

    /**
     * Send out an event. The corresponding {@link EventModel} will be used to
     * decide which channel (and pipeline) will be used
     */
    void sendEventOutbound(EventInstance eventInstance);
}
