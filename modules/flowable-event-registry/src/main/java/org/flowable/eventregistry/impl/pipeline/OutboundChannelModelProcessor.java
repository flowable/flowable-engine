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
package org.flowable.eventregistry.impl.pipeline;

import org.flowable.eventregistry.api.ChannelModelProcessor;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.OutboundEventProcessingPipeline;
import org.flowable.eventregistry.api.OutboundEventSerializer;
import org.flowable.eventregistry.impl.serialization.EventPayloadToJsonStringSerializer;
import org.flowable.eventregistry.impl.serialization.EventPayloadToXmlStringSerializer;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.OutboundChannelModel;

/**
 * @author Filip Hrisafov
 */
public class OutboundChannelModelProcessor implements ChannelModelProcessor {

    @Override
    public boolean canProcess(ChannelModel channelModel) {
        return channelModel instanceof OutboundChannelModel;
    }

    @Override
    public void registerChannelModel(ChannelModel channelModel, String tenantId, EventRegistry eventRegistry, 
                    EventRepositoryService eventRepositoryService, boolean fallbackToDefaultTenant) {
        
        if (channelModel instanceof OutboundChannelModel) {
            registerChannelModel((OutboundChannelModel) channelModel);
        }

    }

    protected void registerChannelModel(OutboundChannelModel inboundChannelModel) {
        if (inboundChannelModel.getOutboundEventProcessingPipeline() == null) {

            OutboundEventProcessingPipeline eventProcessingPipeline;

            if ("json".equals(inboundChannelModel.getSerializerType())) {
                OutboundEventSerializer eventSerializer = new EventPayloadToJsonStringSerializer();
                eventProcessingPipeline = new DefaultOutboundEventProcessingPipeline(eventSerializer);
                
            } else if ("xml".equals(inboundChannelModel.getSerializerType())) {
                OutboundEventSerializer eventSerializer = new EventPayloadToXmlStringSerializer();
                eventProcessingPipeline = new DefaultOutboundEventProcessingPipeline(eventSerializer);
                
            } else {
                eventProcessingPipeline = null;
            }

            if (eventProcessingPipeline != null) {
                inboundChannelModel.setOutboundEventProcessingPipeline(eventProcessingPipeline);
            }

        }
    }

    @Override
    public void unregisterChannelModel(ChannelModel channelModel, String tenantId, EventRepositoryService eventRepositoryService) {
        // nothing to do
    }
}
