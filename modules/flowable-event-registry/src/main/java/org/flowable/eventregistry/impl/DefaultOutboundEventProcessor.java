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
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.flowable.eventregistry.api.OutboundEventProcessingPipeline;
import org.flowable.eventregistry.api.OutboundEventProcessor;
import org.flowable.eventregistry.api.definition.OutboundChannelDefinition;
import org.flowable.eventregistry.api.runtime.EventInstance;

/**
 * @author Joram Barrez
 */
public class DefaultOutboundEventProcessor implements OutboundEventProcessor {

    protected EventRegistry eventRegistry;

    public DefaultOutboundEventProcessor(EventRegistry eventRegistry) {
        this.eventRegistry = eventRegistry;
    }
    
    @Override
    public void sendEvent(EventInstance eventInstance) {
        Collection<String> outboundChannelKeys = eventInstance.getEventDefinition().getOutboundChannelKeys();
        for (String outboundChannelKey : outboundChannelKeys) {

            OutboundChannelDefinition outboundChannelDefinition = eventRegistry.getOutboundChannelDefinition(outboundChannelKey);
            if (outboundChannelDefinition == null) {
                throw new FlowableException("Could not find outbound channel definition for " + outboundChannelKey);
            }

            OutboundEventProcessingPipeline outboundEventProcessingPipeline = outboundChannelDefinition.getOutboundEventProcessingPipeline();
            String rawEvent = outboundEventProcessingPipeline.run(eventInstance);

            OutboundEventChannelAdapter outboundEventChannelAdapter = outboundChannelDefinition.getOutboundEventChannelAdapter();
            if (outboundEventChannelAdapter == null) {
                throw new FlowableException("Could not find an outbound channel adapter for channel " + outboundChannelKey);
            }
            outboundEventChannelAdapter.sendEvent(rawEvent);

        }
    }

}
