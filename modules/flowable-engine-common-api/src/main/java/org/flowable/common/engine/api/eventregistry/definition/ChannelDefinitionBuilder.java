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
package org.flowable.common.engine.api.eventregistry.definition;

import org.flowable.common.engine.api.eventregistry.InboundEventChannelAdapter;
import org.flowable.common.engine.api.eventregistry.InboundEventDeserializer;
import org.flowable.common.engine.api.eventregistry.InboundEventKeyDetector;
import org.flowable.common.engine.api.eventregistry.InboundEventPayloadExtractor;
import org.flowable.common.engine.api.eventregistry.InboundEventProcessingPipeline;
import org.flowable.common.engine.api.eventregistry.InboundEventTransformer;

/**
 * @author Joram Barrez
 */
public interface ChannelDefinitionBuilder {

    ChannelDefinitionBuilder key(String key);

    InboundEventProcessingPipelineBuilder channelAdapter(InboundEventChannelAdapter inboundEventChannelAdapter);

    ChannelDefinition register();


    interface InboundEventProcessingPipelineBuilder {

        InboundEventKeyDetectorBuilder deserializeToJson();

        InboundEventKeyDetectorBuilder customDeserializer(InboundEventDeserializer deserializer);

        ChannelDefinitionBuilder customEventProcessingPipeline(InboundEventProcessingPipeline inboundEventProcessingPipeline);

        InboundEventProcessingPipeline build();

    }

    interface InboundEventKeyDetectorBuilder {

        InboundEventPayloadExtractorBuilder detectEventKeyUsingJsonField(String field);

        InboundEventPayloadExtractorBuilder detectEventKeyUsingJsonPathExpression(String jsonPathExpression);

        InboundEventPayloadExtractorBuilder detectEventKeyUsingCustomKeyDetector(InboundEventKeyDetector inboundEventKeyDetector);

    }

    interface InboundEventPayloadExtractorBuilder{

        InboundEventTransformerBuilder jsonFieldsMapDirectlyToPayload();

        InboundEventTransformerBuilder customPayloadExtractor(InboundEventPayloadExtractor inboundEventPayloadExtractor);

    }

    interface InboundEventTransformerBuilder {

        ChannelDefinitionBuilder customTransformer(InboundEventTransformer inboundEventTransformer);

        ChannelDefinition register();

    }

}
