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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.api.ChannelModelProcessor;
import org.flowable.eventregistry.api.CorrelationKeyGenerator;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRegistryEvent;
import org.flowable.eventregistry.api.EventRegistryEventConsumer;
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

    // The channel models in this map have been deployed,
    // meaning that any adapter is also running and listening if found in this map
    protected ChannelManager<InboundChannelModel> inboundChannelManager = new ChannelManager<>();
    protected ChannelManager<OutboundChannelModel> outboundChannelManager = new ChannelManager<>();

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
    public void registerChannelModel(ChannelModel channelModel, ChannelDefinition channelDefinition) {
        String channelDefinitionKey = channelModel.getKey();
        if (StringUtils.isEmpty(channelDefinitionKey)) {
            throw new FlowableIllegalArgumentException("No key set for channel model");
        }

        if (channelModel instanceof InboundChannelModel) {

            InboundChannelModel inboundChannelModel = (InboundChannelModel) channelModel;
            inboundChannelManager.addChannelData(inboundChannelModel, channelDefinition);

            if (inboundChannelModel.getInboundEventChannelAdapter() != null) {
                InboundEventChannelAdapter inboundEventChannelAdapter = (InboundEventChannelAdapter) inboundChannelModel.getInboundEventChannelAdapter();
                inboundEventChannelAdapter.setEventRegistry(this);
                inboundEventChannelAdapter.setChannelKey(inboundChannelModel.getKey());
            }

        } else if (channelModel instanceof OutboundChannelModel) {

            OutboundChannelModel outboundChannelModel = (OutboundChannelModel) channelModel;
            outboundChannelManager.addChannelData(outboundChannelModel, channelDefinition);

        } else {
            throw new FlowableIllegalArgumentException("Unrecognized ChannelModel class : " + channelModel.getClass());

        }

        for (ChannelModelProcessor channelDefinitionProcessor : engineConfiguration.getChannelDefinitionProcessors()) {
            if (channelDefinitionProcessor.canProcess(channelModel)) {
                channelDefinitionProcessor.unregisterChannelModel(channelModel, engineConfiguration.getEventRegistry());
                channelDefinitionProcessor.registerChannelModel(channelModel, engineConfiguration.getEventRegistry());
            }
        }

    }

    @Override
    public void removeChannelModel(String channelKey) {
        // keys are unique over the two maps
        InboundChannelModel inboundChannelModel = inboundChannelManager.getChannelModel(channelKey);
        if (inboundChannelModel != null) {
            for (ChannelModelProcessor channelDefinitionProcessor : engineConfiguration.getChannelDefinitionProcessors()) {
                if (channelDefinitionProcessor.canProcess(inboundChannelModel)) {
                    channelDefinitionProcessor.unregisterChannelModel(inboundChannelModel, engineConfiguration.getEventRegistry());
                }
            }
            inboundChannelManager.removeChannelDate(channelKey);
        }

        OutboundChannelModel outboundChannelDefinition = outboundChannelManager.getChannelModel(channelKey);
        if (outboundChannelDefinition != null) {
            for (ChannelModelProcessor channelDefinitionProcessor : engineConfiguration.getChannelDefinitionProcessors()) {
                if (channelDefinitionProcessor.canProcess(outboundChannelDefinition)) {
                    channelDefinitionProcessor.unregisterChannelModel(outboundChannelDefinition, engineConfiguration.getEventRegistry());
                }
            }
            outboundChannelManager.removeChannelDate(channelKey);
        }

    }

    @Override
    public InboundChannelModel getInboundChannelModel(String channelKey) {
        return inboundChannelManager.getChannelModel(channelKey);
    }
    
    @Override
    public Map<String, InboundChannelModel> getInboundChannelModels() {
        return inboundChannelManager.getAllChannelModels();
    }

    @Override
    public Map<String, OutboundChannelModel> getOutboundChannelModels() {
        return outboundChannelManager.getAllChannelModels();
    }

    @Override
    public OutboundChannelModel getOutboundChannelModel(String channelKey) {
        return outboundChannelManager.getChannelModel(channelKey);
    }

    @Override
    public EventModel getEventModel(String eventDefinitionKey) {
        return getEventRepositoryService().getEventModelByKey(eventDefinitionKey);
    }

    @Override
    public EventModel getEventModel(String eventDefinitionKey, String tenantId) {
        return getEventRepositoryService().getEventModelByKey(eventDefinitionKey, tenantId, true);
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
        Collection<EventRegistryEventConsumer> engineEventRegistryEventConsumers = engineConfiguration.getEventRegistryEventConsumers().values();
        for (EventRegistryEventConsumer eventConsumer : engineEventRegistryEventConsumers) {
            eventConsumer.eventReceived(eventRegistryEvent);
        }
    }

    @Override
    public void sendEventOutbound(EventInstance eventInstance) {
        outboundEventProcessor.sendEvent(eventInstance);
    }

    @Override
    public void registerEventRegistryEventConsumer(EventRegistryEventConsumer eventRegistryEventBusConsumer) {
        engineConfiguration.getEventRegistryEventConsumers().put(eventRegistryEventBusConsumer.getConsumerKey(), eventRegistryEventBusConsumer);
    }
    
    @Override
    public void removeFlowableEventRegistryEventConsumer(EventRegistryEventConsumer eventRegistryEventBusConsumer) {
        engineConfiguration.getEventRegistryEventConsumers().remove(eventRegistryEventBusConsumer.getConsumerKey());
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

    /**
     * A helper class that wraps a map of {@link ChannelModel} and {@link ChannelDefinition} instances.
     */
    public static class ChannelManager<T extends ChannelModel> {

        protected Map<String, ChannelData<T>> channelData = new ConcurrentHashMap<>();

        public void addChannelData(T channelModel, ChannelDefinition channelDefinition) {
            channelData.put(channelModel.getKey(), new ChannelData<>(channelModel, channelDefinition));
        }

        public void removeChannelDate(String key) {
            channelData.remove(key);
        }

        public T getChannelModel(String key) {
            ChannelData<T> channelData = this.channelData.get(key);
            if (channelData != null) {
                return channelData.getChannelModel();
            }
            return null;
        }

        public ChannelDefinition getChannelDefinition(String key) {
            return channelData.get(key).getChannelDefinition();
        }

        public Map<String, T> getAllChannelModels() {
            return channelData.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getChannelModel()));
        }

        public static class ChannelData<T extends ChannelModel> {

            protected T channelModel;
            protected ChannelDefinition channelDefinition;

            public ChannelData(T channelModel, ChannelDefinition channelDefinition) {
                this.channelModel = channelModel;
                this.channelDefinition = channelDefinition;
            }

            public T getChannelModel() {
                return channelModel;
            }
            public void setChannelModel(T channelModel) {
                this.channelModel = channelModel;
            }
            public ChannelDefinition getChannelDefinition() {
                return channelDefinition;
            }
            public void setChannelDefinition(ChannelDefinition channelDefinition) {
                this.channelDefinition = channelDefinition;
            }
        }

    }

}
