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

import java.util.HashMap;
import java.util.Map;

import org.flowable.eventregistry.api.ChannelProcessingPipelineManager;
import org.flowable.eventregistry.api.InboundEventContextExtractor;
import org.flowable.eventregistry.api.InboundEventDeserializer;

public class DefaultChannelProcessingPipelineManager implements ChannelProcessingPipelineManager {

    protected Map<String, InboundEventContextExtractor> eventContextExtractorMap = new HashMap<>();
    protected Map<String, Map<String, InboundEventDeserializer<?>>> eventDeserializerMap = new HashMap<>();

    @Override
    public InboundEventContextExtractor getInboundEventContextExtractor(String channelType) {
        return eventContextExtractorMap.get(channelType);
    }
    
    @Override
    public void registerInboundEventContextExtractor(String channelType, InboundEventContextExtractor inboundEventContextExtractor) {
        eventContextExtractorMap.put(channelType, inboundEventContextExtractor);
    }
    
    @Override
    public InboundEventDeserializer<?> getInboundEventDeserializer(String channelType, String deserializerType) {
        Map<String, InboundEventDeserializer<?>> channelDeserializerMap = eventDeserializerMap.get(channelType);
        if (channelDeserializerMap == null) {
            channelDeserializerMap = eventDeserializerMap.get(CHANNEL_DEFAULT_TYPE);
            if (channelDeserializerMap == null) {
                return null;
            }
        }
        
        return channelDeserializerMap.get(deserializerType);
    }

    @Override
    public InboundEventDeserializer<?> getDefaultInboundEventDeserializer(String deserializerType) {
        Map<String, InboundEventDeserializer<?>> channelDeserializerMap = eventDeserializerMap.get(CHANNEL_DEFAULT_TYPE);
        if (channelDeserializerMap == null) {
            return null;
        }
        
        return channelDeserializerMap.get(deserializerType);
    }

    @Override
    public void registerInboundEventDeserializer(String channelType, String deserializerType, InboundEventDeserializer<?> inboundEventDeserializer) {
        Map<String, InboundEventDeserializer<?>> channelDeserializerMap = null;
        if (eventDeserializerMap.containsKey(channelType)) {
            channelDeserializerMap = eventDeserializerMap.get(channelType);
        } else {
            channelDeserializerMap = new HashMap<>();
        }
        
        channelDeserializerMap.put(deserializerType, inboundEventDeserializer);
        eventDeserializerMap.put(channelType, channelDeserializerMap);
    }
}
