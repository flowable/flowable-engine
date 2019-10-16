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
package org.flowable.common.engine.impl.eventregistry;

import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.eventbus.FlowableEventBus;
import org.flowable.common.engine.api.eventbus.FlowableEventBusEvent;
import org.flowable.common.engine.api.eventregistry.EventRegistry;
import org.flowable.common.engine.api.eventregistry.InboundEventProcessingPipeline;
import org.flowable.common.engine.api.eventregistry.InboundEventProcessor;
import org.flowable.common.engine.api.eventregistry.definition.ChannelDefinition;

/**
 * @author Joram Barrez
 */
public class DefaultInboundEventProcessor implements InboundEventProcessor {

    protected EventRegistry eventRegistry;
    protected FlowableEventBus flowableEventBus;

    public DefaultInboundEventProcessor(EventRegistry eventRegistry, FlowableEventBus flowableEventBus) {
        this.eventRegistry = eventRegistry;
        this.flowableEventBus = flowableEventBus;
    }

    @Override
    public void eventReceived(String channelKey, String event) {

        ChannelDefinition channelDefinition = eventRegistry.getChannelDefinition(channelKey);
        if (channelDefinition == null) {
            throw new FlowableException("No channel definition found for key " + channelKey);
        }

        EventProcessingContextImpl eventProcessingContext = new EventProcessingContextImpl(channelKey, event);
        InboundEventProcessingPipeline inboundEventProcessingPipeline = channelDefinition.getInboundEventProcessingPipeline();
        List<FlowableEventBusEvent> eventBusEvents = inboundEventProcessingPipeline.run(eventProcessingContext);

        // TODO: change transform() to EventInstance instead of eventBusEvent
        for (FlowableEventBusEvent flowableEventBusEvent : eventBusEvents) {
            flowableEventBus.sendEvent(flowableEventBusEvent);
        }

    }

}
