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
package org.flowable.common.engine.api.eventbus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Joram Barrez
 */
public class DefaultEventRegistry implements EventRegistry {

    protected Map<String, InboundEventChannelAdapter> inboundChannelAdapters = new HashMap<>();
    protected Map<String, OutboundEventChannelAdapter> outboundChannelAdapters = new HashMap<>();

    protected List<InboundEventTransformer> inboundEventTransformers = new ArrayList<>();

    protected InboundEventProcessor inboundEventProcessor;

    @Override
    public void registerChannel(String channelKey, InboundEventChannelAdapter inboundAdapter, OutboundEventChannelAdapter outboundAdaper) {
        inboundChannelAdapters.put(channelKey, inboundAdapter);
        outboundChannelAdapters.put(channelKey, outboundAdaper);
    }

    @Override
    public void registerInboundEventTransformer(InboundEventTransformer inboundEventTransformer) {
        inboundEventTransformers.add(inboundEventTransformer);
    }

    @Override
    public Collection<InboundEventTransformer> getInboundEventTransformers() {
        return inboundEventTransformers;
    }

    @Override
    public void registerInboudEventProcessor(InboundEventProcessor inboundEventProcessor) {
        this.inboundEventProcessor = inboundEventProcessor;
    }

    @Override
    public InboundEventProcessor getInboundEventProcessor() {
        return inboundEventProcessor;
    }

}
