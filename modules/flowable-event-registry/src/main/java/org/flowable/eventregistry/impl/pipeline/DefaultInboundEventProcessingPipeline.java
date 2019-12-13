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

import java.util.Collection;

import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRegistryEvent;
import org.flowable.eventregistry.api.InboundEventDeserializer;
import org.flowable.eventregistry.api.InboundEventKeyDetector;
import org.flowable.eventregistry.api.InboundEventPayloadExtractor;
import org.flowable.eventregistry.api.InboundEventProcessingPipeline;
import org.flowable.eventregistry.api.InboundEventTransformer;
import org.flowable.eventregistry.api.runtime.EventCorrelationParameterInstance;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.runtime.EventInstanceImpl;
import org.flowable.eventregistry.model.EventModel;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class DefaultInboundEventProcessingPipeline<T> implements InboundEventProcessingPipeline {

    protected EventRegistry eventRegistry;
    protected InboundEventDeserializer<T> inboundEventDeserializer;
    protected InboundEventKeyDetector<T> inboundEventKeyDetector;
    protected InboundEventPayloadExtractor<T> inboundEventPayloadExtractor;
    protected InboundEventTransformer inboundEventTransformer;

    public DefaultInboundEventProcessingPipeline(EventRegistry eventRegistry,
            InboundEventDeserializer<T> inboundEventDeserializer,
            InboundEventKeyDetector<T> inboundEventKeyDetector,
            InboundEventPayloadExtractor<T> inboundEventPayloadExtractor,
            InboundEventTransformer inboundEventTransformer) {
        this.eventRegistry = eventRegistry;
        this.inboundEventDeserializer = inboundEventDeserializer;
        this.inboundEventKeyDetector = inboundEventKeyDetector;
        this.inboundEventPayloadExtractor = inboundEventPayloadExtractor;
        this.inboundEventTransformer = inboundEventTransformer;
    }

    @Override
    public Collection<EventRegistryEvent> run(String channelKey, String rawEvent) {
        T event = deserialize(rawEvent);
        String eventKey = detectEventDefinitionKey(event);

        EventModel eventDefinition = eventRegistry.getEventModel(eventKey);

        EventInstanceImpl eventInstance = new EventInstanceImpl(
            eventDefinition,
            extractCorrelationParameters(eventDefinition, event),
            extractPayload(eventDefinition, event)
        );

        // TODO: change transform() to EventInstance instead of eventBusEvent
        return transform(eventInstance);
    }

    public T deserialize(String rawEvent) {
        return inboundEventDeserializer.deserialize(rawEvent);
    }

    public String detectEventDefinitionKey(T event) {
        return inboundEventKeyDetector.detectEventDefinitionKey(event);
    }

    public Collection<EventCorrelationParameterInstance> extractCorrelationParameters(EventModel eventDefinition, T event) {
        return inboundEventPayloadExtractor.extractCorrelationParameters(eventDefinition, event);
    }

    public Collection<EventPayloadInstance> extractPayload(EventModel eventDefinition, T event) {
        return inboundEventPayloadExtractor.extractPayload(eventDefinition, event);
    }

    public Collection<EventRegistryEvent> transform(EventInstance eventInstance) {
        return inboundEventTransformer.transform(eventInstance);
    }
}
