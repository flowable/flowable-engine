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
import java.util.Collections;

import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.eventregistry.api.EventRegistryEvent;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.FlowableEventInfo;
import org.flowable.eventregistry.api.InboundEvent;
import org.flowable.eventregistry.api.InboundEventDeserializer;
import org.flowable.eventregistry.api.InboundEventFilter;
import org.flowable.eventregistry.api.InboundEventKeyDetector;
import org.flowable.eventregistry.api.InboundEventPayloadExtractor;
import org.flowable.eventregistry.api.InboundEventProcessingPipeline;
import org.flowable.eventregistry.api.InboundEventTenantDetector;
import org.flowable.eventregistry.api.InboundEventTransformer;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.FlowableEventInfoImpl;
import org.flowable.eventregistry.impl.runtime.EventInstanceImpl;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class DefaultInboundEventProcessingPipeline<T> implements InboundEventProcessingPipeline {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected EventRepositoryService eventRepositoryService;
    protected InboundEventDeserializer<T> inboundEventDeserializer;
    protected InboundEventFilter<T> inboundEventFilter;
    protected InboundEventKeyDetector<T> inboundEventKeyDetector;
    protected InboundEventTenantDetector<T> inboundEventTenantDetector;
    protected InboundEventPayloadExtractor<T> inboundEventPayloadExtractor;
    protected InboundEventTransformer inboundEventTransformer;

    public DefaultInboundEventProcessingPipeline(EventRepositoryService eventRepositoryService,
            InboundEventDeserializer<T> inboundEventDeserializer,
            InboundEventFilter<T> inboundEventFilter,
            InboundEventKeyDetector<T> inboundEventKeyDetector,
            InboundEventTenantDetector<T> inboundEventTenantDetector,
            InboundEventPayloadExtractor<T> inboundEventPayloadExtractor,
            InboundEventTransformer inboundEventTransformer) {
        
        this.eventRepositoryService = eventRepositoryService;
        this.inboundEventDeserializer = inboundEventDeserializer;
        this.inboundEventFilter = inboundEventFilter;
        this.inboundEventKeyDetector = inboundEventKeyDetector;
        this.inboundEventTenantDetector = inboundEventTenantDetector;
        this.inboundEventPayloadExtractor = inboundEventPayloadExtractor;
        this.inboundEventTransformer = inboundEventTransformer;
    }

    @Override
    public Collection<EventRegistryEvent> run(InboundChannelModel inboundChannel, InboundEvent inboundEvent) {

        boolean debugLoggingEnabled = logger.isDebugEnabled();
        if (debugLoggingEnabled) {
            logger.debug("Running inbound pipeline for inbound {} channel {}. Inbound event: {}", inboundChannel.getChannelType(), inboundChannel.getKey(), inboundEvent);
        }

        T deserializedBody = deserialize(inboundEvent.getBody());

        FlowableEventInfo<T> event = new FlowableEventInfoImpl<>(inboundEvent, deserializedBody, inboundChannel);

        String eventKey = detectEventDefinitionKey(event);

        // if there is a custom filter in place, invoke it to retain only the events that are wanted or to abort the pipeline
        if (inboundEventFilter != null) {
            if (!inboundEventFilter.retain(eventKey, event)) {
                if (debugLoggingEnabled) {
                    logger.debug("Inbound event {} on inbound {} channel {} was filtered out.", inboundEvent, inboundChannel.getChannelType(), inboundChannel.getKey());
                }
                return Collections.emptyList();
            }
        }

        boolean multiTenant = false;
        String tenantId = AbstractEngineConfiguration.NO_TENANT_ID;
        if (inboundEventTenantDetector != null) {
            tenantId = inboundEventTenantDetector.detectTenantId(event);
            multiTenant = true;
        }

        if (debugLoggingEnabled) {
            logger.debug("Detected event {} and tenant {} for inbound {} channel {}. Inbound event: {}", eventKey, tenantId, inboundChannel.getChannelType(),
                    inboundChannel.getKey(), inboundEvent);
        }

        EventModel eventModel = multiTenant ? eventRepositoryService.getEventModelByKey(eventKey, tenantId) : eventRepositoryService.getEventModelByKey(eventKey);
        
        EventInstanceImpl eventInstance = new EventInstanceImpl(
            eventModel.getKey(),
            extractPayload(eventModel, event),
            tenantId
        );

        if (debugLoggingEnabled) {
            logger.debug("Transforming {} for inbound {} channel {}. Inbound event: {}", eventInstance, inboundChannel.getChannelType(),
                    inboundChannel.getKey(), inboundEvent);
        }
        Collection<EventRegistryEvent> registryEvents = transform(eventInstance);

        if (debugLoggingEnabled) {
            logger.debug("Transformed {} to {} for inbound {} channel {}. Inbound event: {}", eventInstance, registryEvents, inboundChannel.getChannelType(),
                    inboundChannel.getKey(), inboundEvent);
        }

        return registryEvents;
    }

    public T deserialize(Object rawEvent) {
        return inboundEventDeserializer.deserialize(rawEvent);
    }

    public String detectEventDefinitionKey(FlowableEventInfo<T> event) {
        return inboundEventKeyDetector.detectEventDefinitionKey(event);
    }

    public Collection<EventPayloadInstance> extractPayload(EventModel eventDefinition, FlowableEventInfo<T> event) {
        return inboundEventPayloadExtractor.extractPayload(eventDefinition, event);
    }

    public Collection<EventRegistryEvent> transform(EventInstance eventInstance) {
        return inboundEventTransformer.transform(eventInstance);
    }
    
    public InboundEventDeserializer<T> getInboundEventDeserializer() {
        return inboundEventDeserializer;
    }
    
    public void setInboundEventDeserializer(InboundEventDeserializer<T> inboundEventDeserializer) {
        this.inboundEventDeserializer = inboundEventDeserializer;
    }
    
    public InboundEventKeyDetector<T> getInboundEventKeyDetector() {
        return inboundEventKeyDetector;
    }
    
    public void setInboundEventKeyDetector(InboundEventKeyDetector<T> inboundEventKeyDetector) {
        this.inboundEventKeyDetector = inboundEventKeyDetector;
    }
    
    public InboundEventTenantDetector<T> getInboundEventTenantDetector() {
        return inboundEventTenantDetector;
    }
    
    public void setInboundEventTenantDetector(InboundEventTenantDetector<T> inboundEventTenantDetector) {
        this.inboundEventTenantDetector = inboundEventTenantDetector;
    }
    
    public InboundEventPayloadExtractor<T> getInboundEventPayloadExtractor() {
        return inboundEventPayloadExtractor;
    }
    
    public void setInboundEventPayloadExtractor(InboundEventPayloadExtractor<T> inboundEventPayloadExtractor) {
        this.inboundEventPayloadExtractor = inboundEventPayloadExtractor;
    }

    public InboundEventTransformer getInboundEventTransformer() {
        return inboundEventTransformer;
    }
    
    public void setInboundEventTransformer(InboundEventTransformer inboundEventTransformer) {
        this.inboundEventTransformer = inboundEventTransformer;
    }
}
