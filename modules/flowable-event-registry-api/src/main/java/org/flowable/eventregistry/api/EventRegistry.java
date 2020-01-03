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

import org.flowable.eventregistry.api.model.InboundChannelModelBuilder;
import org.flowable.eventregistry.api.model.OutboundChannelModelBuilder;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.flowable.eventregistry.model.OutboundChannelModel;

/**
 * Central registry for events that are received through external channels through a {@link InboundEventChannelAdapter}
 * and then passed through as a event registry event.
 *
 * @author Joram Barrez
 */
public interface EventRegistry {

    /**
     * Programmatically build and register a new {@link InboundChannelModel}.
     */
    InboundChannelModelBuilder newInboundChannelModel();

    /**
     * Returns the {@link InboundChannelModel} instance associated with the given key.
     */
    InboundChannelModel getInboundChannelModel(String channelKey);
    
    /**
     * Returns all the {@link InboundChannelModel} instances.
     */
    Map<String, InboundChannelModel> getInboundChannelModels();

    /**
     * Programmatically build and register a new {@link OutboundChannelModel}.
     */
    OutboundChannelModelBuilder newOutboundChannelModel();

    /**
     * Returns the {@link OutboundChannelModel} instance associated with the given key.
     */
    OutboundChannelModel getOutboundChannelModel(String channelKey);

    /**
     * Low-level (vs the {@link InboundChannelModelBuilder}) way of registering a new {@link ChannelModel}.
     */
    void registerChannelModel(ChannelModel channelModel);

    /**
     * Removes a previously registered {@link ChannelModel}.
     */
    void removeChannelModel(String channelDefinitionKey);

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
     * Registers a {@link EventRegistryEventConsumer} instance (a consumer of event registry events which
     * is created by any of the engines).
     */
    void registerEventRegistryEventBusConsumer(EventRegistryEventConsumer eventRegistryEventBusConsumer);
    
    /**
     * Removes the event consumer from the event registry
     */
    void removeFlowableEventConsumer(EventRegistryEventConsumer eventRegistryEventBusConsumer);

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
