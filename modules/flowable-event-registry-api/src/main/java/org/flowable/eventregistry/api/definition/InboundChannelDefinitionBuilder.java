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
package org.flowable.eventregistry.api.definition;

import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.api.InboundEventDeserializer;
import org.flowable.eventregistry.api.InboundEventKeyDetector;
import org.flowable.eventregistry.api.InboundEventPayloadExtractor;
import org.flowable.eventregistry.api.InboundEventProcessingPipeline;
import org.flowable.eventregistry.api.InboundEventTransformer;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public interface InboundChannelDefinitionBuilder {

    InboundChannelDefinitionBuilder key(String key);

    InboundEventProcessingPipelineBuilder channelAdapter(InboundEventChannelAdapter inboundEventChannelAdapter);

    InboundChannelDefinition register();


    interface InboundEventProcessingPipelineBuilder {

        InboundEventKeyJsonDetectorBuilder jsonDeserializer();

        <T> InboundEventKeyDetectorBuilder<T> deserializer(InboundEventDeserializer<T> deserializer);

        InboundChannelDefinitionBuilder eventProcessingPipeline(InboundEventProcessingPipeline inboundEventProcessingPipeline);

        InboundEventProcessingPipeline build();

    }

    interface InboundEventKeyJsonDetectorBuilder {

        InboundEventPayloadJsonExtractorBuilder detectEventKeyUsingJsonField(String field);

        InboundEventPayloadJsonExtractorBuilder detectEventKeyUsingJsonPathExpression(String jsonPathExpression);

    }

    interface InboundEventKeyDetectorBuilder<T> {

        InboundEventPayloadExtractorBuilder<T> detectEventKeyUsingKeyDetector(InboundEventKeyDetector<T> inboundEventKeyDetector);

    }

    interface InboundEventPayloadJsonExtractorBuilder {

        InboundEventTransformerBuilder jsonFieldsMapDirectlyToPayload();

    }

    interface InboundEventPayloadExtractorBuilder<T> {

        InboundEventTransformerBuilder payloadExtractor(InboundEventPayloadExtractor<T> inboundEventPayloadExtractor);

    }

    interface InboundEventTransformerBuilder {

        InboundChannelDefinitionBuilder transformer(InboundEventTransformer inboundEventTransformer);

        InboundChannelDefinition register();

    }

}
