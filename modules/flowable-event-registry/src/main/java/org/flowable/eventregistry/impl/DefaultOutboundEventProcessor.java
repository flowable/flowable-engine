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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.flowable.eventregistry.api.OutboundEventProcessingPipeline;
import org.flowable.eventregistry.api.OutboundEventProcessor;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.OutboundChannelModel;

/**
 * @author Joram Barrez
 */
public class DefaultOutboundEventProcessor implements OutboundEventProcessor {

    protected EventRepositoryService eventRepositoryService;
    protected boolean fallbackToDefaultTenant;

    public DefaultOutboundEventProcessor(EventRepositoryService eventRepositoryService, boolean fallbackToDefaultTenant) {
        this.eventRepositoryService = eventRepositoryService;
        this.fallbackToDefaultTenant =fallbackToDefaultTenant;
    }
    
    @Override
    public void sendEvent(EventInstance eventInstance, Collection<ChannelModel> channelModels) {
        if (channelModels == null || channelModels.isEmpty()) {
            throw new FlowableException("No channel model set for outgoing event " + eventInstance.getEventKey());
        }

        for (ChannelModel channelModel : channelModels) {

            OutboundChannelModel outboundChannelModel = (OutboundChannelModel) channelModel;

            OutboundEventProcessingPipeline<?> outboundEventProcessingPipeline = (OutboundEventProcessingPipeline<?>) outboundChannelModel.getOutboundEventProcessingPipeline();
            Object rawEvent = outboundEventProcessingPipeline.run(eventInstance);

            OutboundEventChannelAdapter outboundEventChannelAdapter = (OutboundEventChannelAdapter<?>) outboundChannelModel.getOutboundEventChannelAdapter();
            if (outboundEventChannelAdapter == null) {
                throw new FlowableException("Could not find an outbound channel adapter for channel " + channelModel.getKey());
            }
            
            outboundEventChannelAdapter.sendEvent(rawEvent);
        }
    }

}
