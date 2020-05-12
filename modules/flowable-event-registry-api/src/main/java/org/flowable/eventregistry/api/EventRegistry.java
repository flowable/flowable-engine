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

import java.util.Collection;
import java.util.Map;

import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.InboundChannelModel;

/**
 * Central registry for events that are received through external channels through a {@link InboundEventChannelAdapter}
 * and then passed through as a event registry event.
 *
 * @author Joram Barrez
 */
public interface EventRegistry {

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

    OutboundEventProcessor getSystemOutboundEventProcessor();

    /**
     * The {@link OutboundEventProcessor} is responsible for handling sending system out events.
     * The event registry will simply pass any event it needs to send to this instance.
     */
    void setSystemOutboundEventProcessor(OutboundEventProcessor outboundEventProcessor);

    /**
     * Registers a {@link EventRegistryEventConsumer} instance (a consumer of event registry events which
     * is created by any of the engines).
     */
    void registerEventRegistryEventConsumer(EventRegistryEventConsumer eventRegistryEventBusConsumer);
    
    /**
     * Removes the event consumer from the event registry
     */
    void removeFlowableEventRegistryEventConsumer(EventRegistryEventConsumer eventRegistryEventBusConsumer);

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
    void eventReceived(InboundChannelModel channelModel, String event);
    
    /**
     * Send an event to all the registered event consumers.
     */
    void sendEventToConsumers(EventRegistryEvent eventRegistryEvent);

    /**
     * Send out the {@code eventInstance} via the given system {@link OutboundEventProcessor}.
     */
    void sendSystemEventOutbound(EventInstance eventInstance);

    /**
     * Send out the {@code eventInstance} via the given {@code channelModel}(s).
     */
    void sendEventOutbound(EventInstance eventInstance, Collection<ChannelModel> channelModels);
}
