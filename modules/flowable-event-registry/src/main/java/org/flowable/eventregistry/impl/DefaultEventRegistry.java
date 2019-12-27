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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.eventregistry.api.ChannelModelProcessor;
import org.flowable.eventregistry.api.CorrelationKeyGenerator;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRegistryEvent;
import org.flowable.eventregistry.api.EventRegistryEventBusConsumer;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.api.InboundEventProcessor;
import org.flowable.eventregistry.api.OutboundEventProcessor;
import org.flowable.eventregistry.api.model.InboundChannelModelBuilder;
import org.flowable.eventregistry.api.model.OutboundChannelModelBuilder;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.impl.model.InboundChannelDefinitionBuilderImpl;
import org.flowable.eventregistry.impl.model.OutboundChannelDefinitionBuilderImpl;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.flowable.eventregistry.model.OutboundChannelModel;

/**
 * @author Joram Barrez
 */
public class DefaultEventRegistry implements EventRegistry {

    protected EventRegistryEngineConfiguration engineConfiguration;
    
    protected Map<String, InboundChannelModel> inboundChannelModels = new ConcurrentHashMap<>();
    protected Map<String, OutboundChannelModel> outboundChannelModels = new ConcurrentHashMap<>();

    protected List<EventRegistryEventBusConsumer> eventRegistryEventBusConsumers = new ArrayList<>();
    protected CorrelationKeyGenerator<Map<String, Object>> correlationKeyGenerator;

    protected InboundEventProcessor inboundEventProcessor;
    protected OutboundEventProcessor outboundEventProcessor;

    public DefaultEventRegistry(EventRegistryEngineConfiguration engineConfiguration) {
        this.engineConfiguration = engineConfiguration;
        this.correlationKeyGenerator = new DefaultCorrelationKeyGenerator();
    }

    @Override
    public InboundChannelModelBuilder newInboundChannelModel() {
        return new InboundChannelDefinitionBuilderImpl(this);
    }

    @Override
    public OutboundChannelModelBuilder newOutboundChannelModel() {
        return new OutboundChannelDefinitionBuilderImpl(this);
    }

    @Override
    public void registerChannelModel(ChannelModel channelModel) {
        String channelDefinitionKey = channelModel.getKey();
        if (StringUtils.isEmpty(channelDefinitionKey)) {
            throw new FlowableIllegalArgumentException("No key set for channel model");
        }

        if (channelModel instanceof InboundChannelModel) {

            InboundChannelModel inboundChannelModel = (InboundChannelModel) channelModel;
            inboundChannelModels.put(inboundChannelModel.getKey(), inboundChannelModel);

            if (inboundChannelModel.getInboundEventChannelAdapter() != null) {
                InboundEventChannelAdapter inboundEventChannelAdapter = (InboundEventChannelAdapter) inboundChannelModel.getInboundEventChannelAdapter();
                inboundEventChannelAdapter.setEventRegistry(this);
                inboundEventChannelAdapter.setChannelKey(inboundChannelModel.getKey());
            }

        } else if (channelModel instanceof OutboundChannelModel) {

            OutboundChannelModel outboundChannelModel = (OutboundChannelModel) channelModel;
            outboundChannelModels.put(outboundChannelModel.getKey(), outboundChannelModel);

        } else {
            throw new FlowableIllegalArgumentException("Unrecognized ChannelModel class : " + channelModel.getClass());

        }

        for (ChannelModelProcessor channelDefinitionProcessor : engineConfiguration.getChannelDefinitionProcessors()) {
            if (channelDefinitionProcessor.canProcess(channelModel)) {
                channelDefinitionProcessor.registerChannelModel(channelModel, engineConfiguration.getEventRegistry());
            }
        }

    }

    @Override
    public void removeChannelModel(String channelDefinitionKey) {
        // keys are unique over the two maps
        InboundChannelModel inboundChannelDefinition = inboundChannelModels.remove(channelDefinitionKey);
        if (inboundChannelDefinition != null) {
            for (ChannelModelProcessor channelDefinitionProcessor : engineConfiguration.getChannelDefinitionProcessors()) {
                if (channelDefinitionProcessor.canProcess(inboundChannelDefinition)) {
                    channelDefinitionProcessor.unregisterChannelModel(inboundChannelDefinition, engineConfiguration.getEventRegistry());
                }
            }
        }
        OutboundChannelModel outboundChannelDefinition = outboundChannelModels.remove(channelDefinitionKey);
        if (outboundChannelDefinition != null) {
            for (ChannelModelProcessor channelDefinitionProcessor : engineConfiguration.getChannelDefinitionProcessors()) {
                if (channelDefinitionProcessor.canProcess(outboundChannelDefinition)) {
                    channelDefinitionProcessor.unregisterChannelModel(outboundChannelDefinition, engineConfiguration.getEventRegistry());
                }
            }
        }

    }

    @Override
    public InboundChannelModel getInboundChannelModel(String channelKey) {
        return inboundChannelModels.get(channelKey);
    }
    
    @Override
    public Map<String, InboundChannelModel> getInboundChannelModels() {
        return inboundChannelModels;
    }

    @Override
    public OutboundChannelModel getOutboundChannelModel(String channelKey) {
        return outboundChannelModels.get(channelKey);
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
