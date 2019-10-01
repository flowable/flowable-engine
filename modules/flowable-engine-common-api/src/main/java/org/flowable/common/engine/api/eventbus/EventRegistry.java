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
package org.flowable.common.engine.api.eventbus;

/**
 * Central registry for events that are received through external channels through a {@link InboundEventChannelAdapter}
 * and then passed through to the {@link FlowableEventBusEvent}.
 *
 * @author Joram Barrez
 */
public interface EventRegistry {

    /**
     * Programmatically build and register a new {@link ChannelDefinition}.
     */
    ChannelDefinitionBuilder newChannelDefinition();

    /**
     * Low-level (vs the {@link ChannelDefinitionBuilder}) way of registering a new {@link ChannelDefinition}.
     */
    void registerChannelDefinition(ChannelDefinition channelDefinition);

    /**
     * Returns the {@link ChannelDefinition} instance associated with the given key.
     */
    ChannelDefinition getChannelDefinition(String channelKey);

    /**
     * Retrieves the {@link ChannelDefinition} for the given key and
     * uses its {@link InboundEventKeyDetector} to return the event definition for the passed event.
     */
    EventDefinition detectEventDefinitionForEvent(String channelKey, String event);

    /**
     * Programmatically build and register a new {@link EventDefinition}.
     */
    EventDefinitionBuilder newEventDefinition();

    /**
     * Low-level (vs the {@link EventDefinitionBuilder}) way of registering a new {@link EventDefinition}.
     */
    void registerEventDefinition(EventDefinition eventDefinition);

    /**
     * Retrieves the {@link EventDefinition} for the given eventDefinitionKey.
     * First, it is checked if a channel is registered with the event definition.
     * Otherwise, the event definitions not bound to a channel are checked.
     */
    EventDefinition getEventDefinition(String channelKey, String eventDefinitionKey);

    /**
     * See {@link #getInboundEventProcessor()}.
     */
    void setInboundEventProcessor(InboundEventProcessor inboundEventProcessor);

    /**
     * Gets the processor of inbound events. All {@link InboundEventChannelAdapter} should pass received events to this processor.
     */
    InboundEventProcessor getInboundEventProcessor();

}
