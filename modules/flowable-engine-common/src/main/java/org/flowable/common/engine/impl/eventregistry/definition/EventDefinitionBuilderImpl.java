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
package org.flowable.common.engine.impl.eventregistry.definition;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.eventregistry.EventRegistry;
import org.flowable.common.engine.api.eventregistry.definition.EventCorrelationParameterDefinition;
import org.flowable.common.engine.api.eventregistry.definition.EventDefinition;
import org.flowable.common.engine.api.eventregistry.definition.EventDefinitionBuilder;
import org.flowable.common.engine.api.eventregistry.definition.EventPayloadDefinition;

/**
 * @author Joram Barrez
 */
public class EventDefinitionBuilderImpl implements EventDefinitionBuilder {

    protected EventRegistry eventRegistry;

    protected String key;
    protected Collection<String> inboundChannelKeys;
    protected Collection<String> outboundChannelKeys;
    protected Map<String, EventCorrelationParameterDefinition> correlationParameterDefinitions = new LinkedHashMap<>();
    protected Map<String, EventPayloadDefinition> eventPayloadDefinitions = new LinkedHashMap<>();

    public EventDefinitionBuilderImpl(EventRegistry eventRegistry) {
        this.eventRegistry = eventRegistry;
    }

    @Override
    public EventDefinitionBuilder key(String key) {
        this.key = key;
        return this;
    }

    @Override
    public EventDefinitionBuilder inboundChannelKey(String channelKey) {
        if (inboundChannelKeys == null) {
            inboundChannelKeys = new HashSet<>();
        }
        inboundChannelKeys.add(channelKey);
        return this;
    }

    @Override
    public EventDefinitionBuilder inboundChannelKeys(Collection<String> channelKeys) {
        channelKeys.forEach(this::inboundChannelKey);
        return this;
    }

    @Override
    public EventDefinitionBuilder outboundChannelKey(String channelKey) {
        if (outboundChannelKeys == null) {
            outboundChannelKeys = new HashSet<>();
        }
        outboundChannelKeys.add(channelKey);
        return this;
    }

    @Override
    public EventDefinitionBuilder outboundChannelKeys(Collection<String> channelKeys) {
        channelKeys.forEach(this::outboundChannelKey);
        return this;
    }

    @Override
    public EventDefinitionBuilder correlationParameter(String name, String type) {
        correlationParameterDefinitions.put(name, new CorrelationParameterDefinitionImpl(name, type));
        payload(name, type);
        return this;
    }

    @Override
    public EventDefinitionBuilder payload(String name, String type) {
        eventPayloadDefinitions.put(name, new EventPayloadDefinitionImpl(name, type));
        return this;
    }

    @Override
    public EventDefinition register() {
        EventDefinitionImpl eventDefinition = new EventDefinitionImpl();

        if (StringUtils.isNotEmpty(key)) {
            eventDefinition.setKey(key);
        } else {
            throw new FlowableIllegalArgumentException("An event definition key is mandatory");
        }

        if (inboundChannelKeys != null) {
            eventDefinition.setInboundChannelKeys(inboundChannelKeys);
        }

        if (outboundChannelKeys != null) {
            eventDefinition.setOutboundChannelKeys(outboundChannelKeys);
        }

        eventDefinition.getCorrelationParameterDefinitions().addAll(correlationParameterDefinitions.values());
        eventDefinition.getEventPayloadDefinitions().addAll(eventPayloadDefinitions.values());

        eventRegistry.registerEventDefinition(eventDefinition);

        return eventDefinition;
    }

}
