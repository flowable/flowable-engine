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
import java.util.Collections;
import java.util.Map;

import org.flowable.eventregistry.api.CorrelationKeyGenerator;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRegistryEvent;
import org.flowable.eventregistry.api.EventRegistryEventConsumer;
import org.flowable.eventregistry.api.EventRegistryProcessingInfo;
import org.flowable.eventregistry.api.InboundEventProcessor;
import org.flowable.eventregistry.api.OutboundEventProcessor;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.InboundChannelModel;

/**
 * @author Joram Barrez
 */
public class DefaultEventRegistry implements EventRegistry {

    protected EventRegistryEngineConfiguration engineConfiguration;

    protected CorrelationKeyGenerator<Map<String, Object>> correlationKeyGenerator;

    protected InboundEventProcessor inboundEventProcessor;
    protected OutboundEventProcessor outboundEventProcessor;
    protected OutboundEventProcessor systemOutboundEventProcessor;

    public DefaultEventRegistry(EventRegistryEngineConfiguration engineConfiguration) {
        this.engineConfiguration = engineConfiguration;
        this.correlationKeyGenerator = new DefaultCorrelationKeyGenerator();
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
    public OutboundEventProcessor getSystemOutboundEventProcessor() {
        return systemOutboundEventProcessor;
    }

    @Override
    public void setSystemOutboundEventProcessor(OutboundEventProcessor systemOutboundEventProcessor) {
        this.systemOutboundEventProcessor = systemOutboundEventProcessor;
    }

    @Override
    public void eventReceived(InboundChannelModel channelModel, String event) {
        inboundEventProcessor.eventReceived(channelModel, event);
    }
    
    @Override
    public void sendEventToConsumers(EventRegistryEvent eventRegistryEvent) {
        Collection<EventRegistryEventConsumer> engineEventRegistryEventConsumers = engineConfiguration.getEventRegistryEventConsumers().values();
        EventRegistryProcessingInfo eventRegistryProcessingInfo = null;
        for (EventRegistryEventConsumer eventConsumer : engineEventRegistryEventConsumers) {
            EventRegistryProcessingInfo processingInfo = eventConsumer.eventReceived(eventRegistryEvent);
            if (processingInfo != null && processingInfo.getEventConsumerInfos() != null && !processingInfo.getEventConsumerInfos().isEmpty()) {
                if (eventRegistryProcessingInfo == null) {
                    eventRegistryProcessingInfo = new EventRegistryProcessingInfo();
                }
                eventRegistryProcessingInfo.setEventConsumerInfos(processingInfo.getEventConsumerInfos());
            }
        }
        
        if ((eventRegistryProcessingInfo == null || !eventRegistryProcessingInfo.eventHandled()) && 
                engineConfiguration.getNonMatchingEventConsumer() != null) {
            
            engineConfiguration.getNonMatchingEventConsumer().handleNonMatchingEvent(eventRegistryEvent, eventRegistryProcessingInfo);
        }
    }

    @Override
    public void sendSystemEventOutbound(EventInstance eventInstance) {
        systemOutboundEventProcessor.sendEvent(eventInstance, Collections.emptyList());
    }

    @Override
    public void sendEventOutbound(EventInstance eventInstance, Collection<ChannelModel> channelModels) {
        outboundEventProcessor.sendEvent(eventInstance, channelModels);
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

}
