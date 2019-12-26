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
package org.flowable.eventregistry.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.eventregistry.api.CorrelationKeyGenerator;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRegistryEvent;
import org.flowable.eventregistry.api.EventRegistryEventBusConsumer;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.api.InboundEventProcessor;
import org.flowable.eventregistry.api.OutboundEventProcessor;
import org.flowable.eventregistry.api.model.InboundChannelDefinitionBuilder;
import org.flowable.eventregistry.api.model.OutboundChannelDefinitionBuilder;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.impl.model.InboundChannelDefinitionBuilderImpl;
import org.flowable.eventregistry.impl.model.OutboundChannelDefinitionBuilderImpl;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.InboundChannelDefinition;
import org.flowable.eventregistry.model.OutboundChannelDefinition;

/**
 * @author Joram Barrez
 */
public class DefaultEventRegistry implements EventRegistry {

    protected AbstractEngineConfiguration engineConfiguration;
    
    protected Map<String, InboundChannelDefinition> inboundChannelDefinitions = new HashMap<>();
    protected Map<String, OutboundChannelDefinition> outboundChannelDefinitions = new HashMap<>();

    protected List<EventRegistryEventBusConsumer> eventRegistryEventBusConsumers = new ArrayList<>();
    protected CorrelationKeyGenerator<Map<String, Object>> correlationKeyGenerator;

    protected InboundEventProcessor inboundEventProcessor;
    protected OutboundEventProcessor outboundEventProcessor;

    public DefaultEventRegistry(AbstractEngineConfiguration engineConfiguration) {
        this.engineConfiguration = engineConfiguration;
        this.correlationKeyGenerator = new DefaultCorrelationKeyGenerator();
    }

    @Override
    public InboundChannelDefinitionBuilder newInboundChannelDefinition() {
        return new InboundChannelDefinitionBuilderImpl(this);
    }

    @Override
    public OutboundChannelDefinitionBuilder newOutboundChannelDefinition() {
        return new OutboundChannelDefinitionBuilderImpl(this);
    }

    @Override
    public void registerChannelDefinition(ChannelModel channelDefinition) {
        String channelDefinitionKey = channelDefinition.getKey();
        if (StringUtils.isEmpty(channelDefinitionKey)) {
            throw new FlowableIllegalArgumentException("No key set for channel definition");
        }

        if (inboundChannelDefinitions.containsKey(channelDefinitionKey) || outboundChannelDefinitions.containsKey(channelDefinitionKey)) {
            throw new FlowableIllegalArgumentException("Channel key " + channelDefinitionKey + " is already registered");
        }

        if (channelDefinition instanceof InboundChannelDefinition) {

            InboundChannelDefinition inboundChannelDefinition = (InboundChannelDefinition) channelDefinition;
            inboundChannelDefinitions.put(inboundChannelDefinition.getKey(), inboundChannelDefinition);

            if (inboundChannelDefinition.getInboundEventChannelAdapter() != null) {
                InboundEventChannelAdapter inboundEventChannelAdapter = (InboundEventChannelAdapter) inboundChannelDefinition.getInboundEventChannelAdapter();
                inboundEventChannelAdapter.setEventRegistry(this);
                inboundEventChannelAdapter.setChannelKey(inboundChannelDefinition.getKey());
            }

        } else if (channelDefinition instanceof OutboundChannelDefinition) {

            OutboundChannelDefinition outboundChannelDefinition = (OutboundChannelDefinition) channelDefinition;
            outboundChannelDefinitions.put(outboundChannelDefinition.getKey(), outboundChannelDefinition);

        } else {
            throw new FlowableIllegalArgumentException("Unrecognized ChannelDefinition class : " + channelDefinition.getClass());

        }

    }

    @Override
    public void removeChannelDefinition(String channelDefinitionKey) {
        // keys are unique over the two maps
        inboundChannelDefinitions.remove(channelDefinitionKey);
        outboundChannelDefinitions.remove(channelDefinitionKey);
    }

    @Override
    public InboundChannelDefinition getInboundChannelDefinition(String channelKey) {
        return inboundChannelDefinitions.get(channelKey);
    }
    
    @Override
    public Map<String, InboundChannelDefinition> getInboundChannelDefinitions() {
        return inboundChannelDefinitions;
    }

    @Override
    public OutboundChannelDefinition getOutboundChannelDefinition(String channelKey) {
        return outboundChannelDefinitions.get(channelKey);
    }

    @Override
    public EventModel getEventModel(String eventDefinitionKey) {
        return getEventRepositoryService().getEventModelByKey(eventDefinitionKey);
    }

    @Override
    public void setInboundEventProcessor(InboundEventProcessor inboundEventProcessor) {
        this.inboundEventProcessor = inboundEventProcessor;
    }

    @Override
    public void setOutboundEventProcessor(OutboundEventProcessor outboundEventProcessor) {
        this.outboundEventProcessor = outboundEventProcessor;
    }

    @Override
    public void eventReceived(String channelKey, String event) {
        inboundEventProcessor.eventReceived(channelKey, event);
    }
    
    @Override
    public void sendEventToConsumers(EventRegistryEvent eventRegistryEvent) {
        for (EventRegistryEventBusConsumer eventConsumer : eventRegistryEventBusConsumers) {
            eventConsumer.eventReceived(eventRegistryEvent);
        }
    }

    @Override
    public void sendEventOutbound(EventInstance eventInstance) {
        outboundEventProcessor.sendEvent(eventInstance);
    }

    @Override
    public void registerEventRegistryEventBusConsumer(EventRegistryEventBusConsumer eventRegistryEventBusConsumer) {
        eventRegistryEventBusConsumers.add(eventRegistryEventBusConsumer);
    }
    
    @Override
    public void removeFlowableEventConsumer(EventRegistryEventBusConsumer eventRegistryEventBusConsumer) {
        eventRegistryEventBusConsumers.remove(eventRegistryEventBusConsumer);
    }

    @Override
    public String generateKey(Map<String, Object> data) {
        return correlationKeyGenerator.generateKey(data);
    }
    
    protected EventRepositoryService getEventRepositoryService() {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = (EventRegistryEngineConfiguration) engineConfiguration.getEngineConfigurations()
                        .get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
        return eventRegistryEngineConfiguration.getEventRepositoryService();
    }
}
