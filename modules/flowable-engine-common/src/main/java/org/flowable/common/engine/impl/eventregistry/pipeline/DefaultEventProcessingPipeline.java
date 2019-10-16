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
package org.flowable.common.engine.impl.eventregistry.pipeline;

import java.util.Collection;
import java.util.List;

import org.flowable.common.engine.api.eventbus.FlowableEventBusEvent;
import org.flowable.common.engine.api.eventregistry.EventProcessingContext;
import org.flowable.common.engine.api.eventregistry.EventRegistry;
import org.flowable.common.engine.api.eventregistry.InboundEventDeserializer;
import org.flowable.common.engine.api.eventregistry.InboundEventKeyDetector;
import org.flowable.common.engine.api.eventregistry.InboundEventPayloadExtractor;
import org.flowable.common.engine.api.eventregistry.InboundEventProcessingPipeline;
import org.flowable.common.engine.api.eventregistry.InboundEventTransformer;
import org.flowable.common.engine.api.eventregistry.definition.EventDefinition;
import org.flowable.common.engine.api.eventregistry.runtime.EventCorrelationParameterInstance;
import org.flowable.common.engine.api.eventregistry.runtime.EventPayloadInstance;
import org.flowable.common.engine.impl.eventregistry.runtime.EventInstanceImpl;

/**
 * @author Joram Barrez
 */
public class DefaultEventProcessingPipeline implements InboundEventProcessingPipeline {

    protected EventRegistry eventRegistry;
    protected InboundEventDeserializer inboundEventDeserializer;
    protected InboundEventKeyDetector inboundEventKeyDetector;
    protected InboundEventPayloadExtractor inboundEventPayloadExtractor;
    protected InboundEventTransformer inboundEventTransformer;

    public DefaultEventProcessingPipeline(EventRegistry eventRegistry,
            InboundEventDeserializer inboundEventDeserializer,
            InboundEventKeyDetector inboundEventKeyDetector,
            InboundEventPayloadExtractor inboundEventPayloadExtractor,
            InboundEventTransformer inboundEventTransformer) {
        this.eventRegistry = eventRegistry;
        this.inboundEventDeserializer = inboundEventDeserializer;
        this.inboundEventKeyDetector = inboundEventKeyDetector;
        this.inboundEventPayloadExtractor = inboundEventPayloadExtractor;
        this.inboundEventTransformer = inboundEventTransformer;
    }

    @Override
    public List<FlowableEventBusEvent> run(EventProcessingContext eventProcessingContext) {
        deserialize(eventProcessingContext.getRawEvent(), eventProcessingContext);
        String eventKey = detectEventDefinitionKey(eventProcessingContext);

        EventDefinition eventDefinition = eventRegistry.getEventDefinition(eventKey);
        eventProcessingContext.setEventDefinition(eventDefinition);

        EventInstanceImpl eventInstance = new EventInstanceImpl(
            eventDefinition,
            extractCorrelationParameters(eventProcessingContext),
            extractPayload(eventProcessingContext)
        );
        eventProcessingContext.getEventInstances().add(eventInstance);

        // TODO: change transform() to EventInstance instead of eventBusEvent
        return transform(eventProcessingContext);
    }

    public void deserialize(String rawEvent, EventProcessingContext eventProcessingContext) {
        inboundEventDeserializer.deserialize(rawEvent, eventProcessingContext);
    }

    public String detectEventDefinitionKey(EventProcessingContext eventProcessingContext) {
        return inboundEventKeyDetector.detectEventDefinitionKey(eventProcessingContext);
    }

    public Collection<EventCorrelationParameterInstance> extractCorrelationParameters(EventProcessingContext eventProcessingContext) {
        return inboundEventPayloadExtractor.extractCorrelationParameters(eventProcessingContext);
    }

    public Collection<EventPayloadInstance> extractPayload(EventProcessingContext eventProcessingContext) {
        return inboundEventPayloadExtractor.extractPayload(eventProcessingContext);
    }

    public List<FlowableEventBusEvent> transform(EventProcessingContext eventProcessingContext) {
        return inboundEventTransformer.transform(eventProcessingContext);
    }

}
