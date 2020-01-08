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

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.eventregistry.api.ChannelModelProcessor;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.InboundEventDeserializer;
import org.flowable.eventregistry.api.InboundEventKeyDetector;
import org.flowable.eventregistry.api.InboundEventPayloadExtractor;
import org.flowable.eventregistry.api.InboundEventProcessingPipeline;
import org.flowable.eventregistry.api.InboundEventTenantDetector;
import org.flowable.eventregistry.api.InboundEventTransformer;
import org.flowable.eventregistry.impl.keydetector.JsonFieldBasedInboundEventKeyDetector;
import org.flowable.eventregistry.impl.keydetector.JsonPathBasedInboundEventKeyDetector;
import org.flowable.eventregistry.impl.keydetector.StaticKeyDetector;
import org.flowable.eventregistry.impl.keydetector.XpathBasedInboundEventKeyDetector;
import org.flowable.eventregistry.impl.payload.JsonFieldToMapPayloadExtractor;
import org.flowable.eventregistry.impl.payload.XmlElementsToMapPayloadExtractor;
import org.flowable.eventregistry.impl.serialization.StringToJsonDeserializer;
import org.flowable.eventregistry.impl.serialization.StringToXmlDocumentDeserializer;
import org.flowable.eventregistry.impl.transformer.DefaultInboundEventTransformer;
import org.flowable.eventregistry.model.ChannelEventKeyDetection;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Filip Hrisafov
 */
public class InboundChannelModelProcessor implements ChannelModelProcessor {

    @Override
    public boolean canProcess(ChannelModel channelModel) {
        return channelModel instanceof InboundChannelModel;
    }

    @Override
    public void registerChannelModel(ChannelModel channelModel, EventRegistry eventRegistry) {
        if (channelModel instanceof InboundChannelModel) {
            registerChannelModel((InboundChannelModel) channelModel, eventRegistry);
        }

    }

    protected void registerChannelModel(InboundChannelModel inboundChannelModel, EventRegistry eventRegistry) {
        if (inboundChannelModel.getInboundEventProcessingPipeline() == null) {

            InboundEventProcessingPipeline eventProcessingPipeline;

            if ("json".equals(inboundChannelModel.getDeserializerType())) {
                InboundEventDeserializer<JsonNode> eventDeserializer = new StringToJsonDeserializer();
                InboundEventTenantDetector<JsonNode> eventTenantDetector = null; // By default no multi-tenancy is applied
                InboundEventPayloadExtractor<JsonNode> eventPayloadExtractor = new JsonFieldToMapPayloadExtractor();
                InboundEventTransformer eventTransformer = new DefaultInboundEventTransformer();
                InboundEventKeyDetector<JsonNode> eventKeyDetector;
                ChannelEventKeyDetection keyDetection = inboundChannelModel.getChannelEventKeyDetection();

                if (keyDetection == null) {
                    throw new FlowableException("A channel key detection value is required");
                }

                if (StringUtils.isNotEmpty(keyDetection.getFixedValue())) {
                    eventKeyDetector = new StaticKeyDetector<>(keyDetection.getFixedValue());
                } else if (StringUtils.isNotEmpty(keyDetection.getJsonField())) {
                    eventKeyDetector = new JsonFieldBasedInboundEventKeyDetector(keyDetection.getJsonField());
                } else if (StringUtils.isNotEmpty(keyDetection.getJsonPathExpression())) {
                    eventKeyDetector = new JsonPathBasedInboundEventKeyDetector(keyDetection.getJsonPathExpression());
                } else {
                    throw new FlowableException(
                        "The channel json key detection value was not found for the channel model with key " + inboundChannelModel.getKey()
                            + ". One of fixedValue, jsonField or jsonPathExpression should be set.");
                }

                eventProcessingPipeline = new DefaultInboundEventProcessingPipeline<>(eventRegistry, eventDeserializer,
                    eventKeyDetector, null, eventPayloadExtractor, eventTransformer);

            } else if ("xml".equals(inboundChannelModel.getDeserializerType())) {

                InboundEventDeserializer<Document> eventDeserializer = new StringToXmlDocumentDeserializer();
                InboundEventPayloadExtractor<Document> eventPayloadExtractor = new XmlElementsToMapPayloadExtractor();
                InboundEventTransformer eventTransformer = new DefaultInboundEventTransformer();
                InboundEventKeyDetector<Document> eventKeyDetector;

                ChannelEventKeyDetection keyDetection = inboundChannelModel.getChannelEventKeyDetection();

                if (keyDetection == null) {
                    throw new FlowableException("A channel key detection value is required");
                }

                if (StringUtils.isNotEmpty(keyDetection.getFixedValue())) {
                    eventKeyDetector = new StaticKeyDetector<>(keyDetection.getFixedValue());
                } else if (StringUtils.isNotEmpty(keyDetection.getXmlXPathExpression())) {
                    eventKeyDetector = new XpathBasedInboundEventKeyDetector(keyDetection.getXmlXPathExpression());
                } else {
                    throw new FlowableException(
                        "The channel xml key detection value was not found for the channel model with key " + inboundChannelModel.getKey()
                            + ". One of fixedValue, xmlPathExpression should be set.");
                }

                eventProcessingPipeline = new DefaultInboundEventProcessingPipeline<>(eventRegistry, eventDeserializer,
                    eventKeyDetector, null, eventPayloadExtractor, eventTransformer);

            } else {
                eventProcessingPipeline = null;
            }

            if (eventProcessingPipeline != null) {
                inboundChannelModel.setInboundEventProcessingPipeline(eventProcessingPipeline);
            }

        }
    }

    @Override
    public void unregisterChannelModel(ChannelModel channelModel, EventRegistry eventRegistry) {
        // nothing to do
    }
}
