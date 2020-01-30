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
import java.util.Objects;

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
    public void sendEvent(EventInstance eventInstance) {
        Collection<String> outboundChannelKeys = eventInstance.getEventModel().getOutboundChannelKeys();
        for (String outboundChannelKey : outboundChannelKeys) {

            ChannelModel channelModel = null;
            if (Objects.equals(EventRegistryEngineConfiguration.NO_TENANT_ID, eventInstance.getTenantId())) {
                channelModel = eventRepositoryService.getChannelModelByKey(outboundChannelKey);
            } else {
                channelModel = eventRepositoryService.getChannelModelByKey(outboundChannelKey, eventInstance.getTenantId());
            }
            
            if (channelModel == null) {
                throw new FlowableException("Could not find outbound channel model for " + outboundChannelKey);
            }
            
            if (!(channelModel instanceof OutboundChannelModel)) {
                throw new FlowableException("Channel model is not an outbound channel model for " + outboundChannelKey);
            }
            
            OutboundChannelModel outboundChannelModel = (OutboundChannelModel) channelModel;

            OutboundEventProcessingPipeline outboundEventProcessingPipeline = (OutboundEventProcessingPipeline) outboundChannelModel.getOutboundEventProcessingPipeline();
            String rawEvent = outboundEventProcessingPipeline.run(eventInstance);

            OutboundEventChannelAdapter outboundEventChannelAdapter = (OutboundEventChannelAdapter) outboundChannelModel.getOutboundEventChannelAdapter();
            if (outboundEventChannelAdapter == null) {
                throw new FlowableException("Could not find an outbound channel adapter for channel " + outboundChannelKey);
            }
            
            outboundEventChannelAdapter.sendEvent(rawEvent);
        }
    }

}
