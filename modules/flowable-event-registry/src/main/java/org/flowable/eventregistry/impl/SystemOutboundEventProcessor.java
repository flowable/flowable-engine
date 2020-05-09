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

import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.flowable.eventregistry.api.OutboundEventProcessingPipeline;
import org.flowable.eventregistry.api.OutboundEventProcessor;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.model.ChannelModel;

/**
 * @author Filip Hrisafov
 */
public class SystemOutboundEventProcessor<T> implements OutboundEventProcessor {

    protected OutboundEventChannelAdapter<T> outboundEventChannelAdapter;
    protected OutboundEventProcessingPipeline<T> outboundEventProcessingPipeline;

    public SystemOutboundEventProcessor(OutboundEventChannelAdapter<T> outboundEventChannelAdapter,
            OutboundEventProcessingPipeline<T> outboundEventProcessingPipeline) {
        this.outboundEventChannelAdapter = outboundEventChannelAdapter;
        this.outboundEventProcessingPipeline = outboundEventProcessingPipeline;
    }

    @Override
    public void sendEvent(EventInstance eventInstance, Collection<ChannelModel> channelModels) {
        T rawEvent = outboundEventProcessingPipeline.run(eventInstance);
        outboundEventChannelAdapter.sendEvent(rawEvent);
    }

    public OutboundEventChannelAdapter<T> getOutboundEventChannelAdapter() {
        return outboundEventChannelAdapter;
    }

    public void setOutboundEventChannelAdapter(OutboundEventChannelAdapter<T> outboundEventChannelAdapter) {
        this.outboundEventChannelAdapter = outboundEventChannelAdapter;
    }

    public OutboundEventProcessingPipeline<T> getOutboundEventProcessingPipeline() {
        return outboundEventProcessingPipeline;
    }

    public void setOutboundEventProcessingPipeline(OutboundEventProcessingPipeline<T> outboundEventProcessingPipeline) {
        this.outboundEventProcessingPipeline = outboundEventProcessingPipeline;
    }
}
