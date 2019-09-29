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
import java.util.List;
import java.util.stream.Collectors;

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

        // TODO: split into methods

        Collection<InboundEventTransformer> inboundEventTransformers = eventRegistry.getInboundEventTransformers();

        if (inboundEventTransformers.isEmpty()) {
            // TODO: log, throw exception or ...?
        }

        EventProcessingContext eventProcessingContext = new EventProcessingContextImpl(channelKey, event);
        List<InboundEventTransformer> matchingInboundEventTransformers = inboundEventTransformers.stream()
            .filter(inboundEventTransformer -> inboundEventTransformer.accepts(eventProcessingContext))
            .collect(Collectors.toList());

        List<FlowableEventBusEvent> events = new ArrayList<>();
        for (InboundEventTransformer inboundEventTransformer : matchingInboundEventTransformers) {
            events.add(inboundEventTransformer.transform(eventProcessingContext));
        }

        for (FlowableEventBusEvent flowableEventBusEvent : events) {
            flowableEventBus.sendEvent(flowableEventBusEvent);
        }

    }

}
