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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.eventregistry.definition.ChannelDefinition;
import org.flowable.common.engine.api.eventregistry.definition.ChannelDefinitionBuilder;
import org.flowable.common.engine.api.eventregistry.definition.EventDefinition;
import org.flowable.common.engine.api.eventregistry.definition.EventDefinitionBuilder;
import org.flowable.common.engine.api.eventregistry.EventRegistry;
import org.flowable.common.engine.api.eventregistry.InboundEventProcessor;
import org.flowable.common.engine.impl.eventregistry.definition.ChannelDefinitionBuilderImpl;
import org.flowable.common.engine.impl.eventregistry.definition.EventDefinitionBuilderImpl;

/**
 * @author Joram Barrez
 */
public class DefaultEventRegistry implements EventRegistry {

    protected Map<String, ChannelDefinition> channelDefinitions = new HashMap<>();

    protected Map<String, EventDefinition> eventDefinitionsByKey = new HashMap<>();
    protected Map<String, EventDefinition> eventDefinitionsWithoutchannelKey = new HashMap<>();
    protected Map<String, Map<String, EventDefinition>> eventDefinitionsByChannelKey = new HashMap<>();

    protected InboundEventProcessor inboundEventProcessor;

    @Override
    public ChannelDefinitionBuilder newChannelDefinition() {
        return new ChannelDefinitionBuilderImpl(this);
    }

    @Override
    public void registerChannelDefinition(ChannelDefinition channelDefinition) {
        if (StringUtils.isEmpty(channelDefinition.getKey())) {
            throw new FlowableIllegalArgumentException("No key set for channel definition");
        }

        if ( (channelDefinition.getInboundEventChannelAdapter() != null && channelDefinition.getInboundEventKeyDetector() == null)
            || (channelDefinition.getInboundEventChannelAdapter() == null && channelDefinition.getInboundEventKeyDetector() != null)) {
            throw new FlowableIllegalArgumentException("Need to set both inbound channel adapter and inbound event key detector when one of both is set");
        }

        channelDefinitions.put(channelDefinition.getKey(), channelDefinition);

        if (channelDefinition.getInboundEventChannelAdapter() != null) {
            channelDefinition.getInboundEventChannelAdapter().setEventRegistry(this);
            channelDefinition.getInboundEventChannelAdapter().setChannelKey(channelDefinition.getKey());
        }
    }

    @Override
    public ChannelDefinition getChannelDefinition(String channelKey) {
        return channelDefinitions.get(channelKey);
    }

    @Override
    public EventDefinition detectEventDefinitionForEvent(String channelKey, String event) {
        ChannelDefinition channelDefinition = getChannelDefinition(channelKey);
        if (channelDefinition == null) {
            throw new FlowableException("No channel definition found for key " + channelKey);
        }

        String eventDefinitionKey = channelDefinition.getInboundEventKeyDetector().detectEventDefinitionKey(event);
        if (eventDefinitionKey == null) {
            throw new FlowableException("No event definition key could be detected for event " + event);
        }

        return getEventDefinition(channelKey, eventDefinitionKey);
    }

    @Override
    public EventDefinitionBuilder newEventDefinition() {
        return new EventDefinitionBuilderImpl(this);
    }

    @Override
    public void registerEventDefinition(EventDefinition eventDefinition) {
        eventDefinitionsByKey.put(eventDefinition.getKey(), eventDefinition);

        if (eventDefinition.getChannelKeys() != null) {
            for (String channelKey : eventDefinition.getChannelKeys()) {
                if (!eventDefinitionsByChannelKey.containsKey(channelKey)) {
                    eventDefinitionsByChannelKey.put(channelKey, new HashMap<>());
                }
                eventDefinitionsByChannelKey.get(channelKey).put(eventDefinition.getKey(), eventDefinition);
            }

        } else {
            eventDefinitionsWithoutchannelKey.put(eventDefinition.getKey(), eventDefinition);

        }
    }

    @Override
    public EventDefinition getEventDefinition(String channelKey, String eventDefinitionKey) {
        EventDefinition eventDefinition = null;
        if (eventDefinitionsByChannelKey.containsKey(channelKey)) {
            eventDefinition = eventDefinitionsByChannelKey.get(channelKey).get(eventDefinitionKey);
        }

        if (eventDefinition == null) {
            eventDefinition = eventDefinitionsWithoutchannelKey.get(eventDefinitionKey);
        }

        return eventDefinition;
    }

    @Override
    public void setInboundEventProcessor(InboundEventProcessor inboundEventProcessor) {
        this.inboundEventProcessor = inboundEventProcessor;
    }

    @Override
    public InboundEventProcessor getInboundEventProcessor() {
        return inboundEventProcessor;
    }

}
