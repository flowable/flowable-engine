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
package org.flowable.eventregistry.api.model;

import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.api.InboundEventDeserializer;
import org.flowable.eventregistry.api.InboundEventKeyDetector;
import org.flowable.eventregistry.api.InboundEventPayloadExtractor;
import org.flowable.eventregistry.api.InboundEventProcessingPipeline;
import org.flowable.eventregistry.api.InboundEventTransformer;
import org.flowable.eventregistry.model.InboundChannelModel;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public interface InboundChannelModelBuilder {

    InboundChannelModelBuilder key(String key);

    InboundEventProcessingPipelineBuilder channelAdapter(InboundEventChannelAdapter inboundEventChannelAdapter);

    InboundJmsChannelBuilder jmsChannelAdapter(String destinationName);

    InboundRabbitChannelBuilder rabbitChannelAdapter(String queue);

    InboundKafkaChannelBuilder kafkaChannelAdapter(String topic);

    InboundChannelModel register();

    interface InboundJmsChannelBuilder {

        InboundJmsChannelBuilder selector(String selector);

        InboundJmsChannelBuilder subscription(String subscription);

        InboundJmsChannelBuilder concurrency(String concurrency);

        InboundEventProcessingPipelineBuilder eventProcessingPipeline();
    }

    interface InboundRabbitChannelBuilder {

        InboundRabbitChannelBuilder exclusive(boolean exclusive);

        InboundRabbitChannelBuilder priority(String priority);

        InboundRabbitChannelBuilder admin(String admin);

        InboundRabbitChannelBuilder concurrency(String concurrency);

        InboundRabbitChannelBuilder executor(String executor);

        InboundRabbitChannelBuilder ackMode(String ackMode);

        InboundEventProcessingPipelineBuilder eventProcessingPipeline();
    }

    interface InboundKafkaChannelBuilder {

        InboundKafkaChannelBuilder groupId(String groupId);

        InboundKafkaChannelBuilder clientIdPrefix(String clientIdPrefix);

        InboundKafkaChannelBuilder concurrency(String concurrency);

        InboundKafkaChannelBuilder property(String name, String value);

        InboundEventProcessingPipelineBuilder eventProcessingPipeline();
    }

    interface InboundEventProcessingPipelineBuilder {

        InboundEventKeyJsonDetectorBuilder jsonDeserializer();

        InboundEventKeyXmlDetectorBuilder xmlDeserializer();

        <T> InboundEventKeyDetectorBuilder<T> deserializer(InboundEventDeserializer<T> deserializer);

        InboundChannelModelBuilder eventProcessingPipeline(InboundEventProcessingPipeline inboundEventProcessingPipeline);

        InboundEventProcessingPipeline build();

    }

    interface InboundEventKeyJsonDetectorBuilder {

        InboundEventPayloadJsonExtractorBuilder fixedEventKey(String key);

        InboundEventPayloadJsonExtractorBuilder detectEventKeyUsingJsonField(String field);

        InboundEventPayloadJsonExtractorBuilder detectEventKeyUsingJsonPathExpression(String jsonPathExpression);

    }

    interface InboundEventKeyXmlDetectorBuilder {

        InboundEventPayloadXmlExtractorBuilder fixedEventKey(String key);

        InboundEventPayloadXmlExtractorBuilder detectEventKeyUsingXPathExpression(String jsonPathExpression);

    }

    interface InboundEventKeyDetectorBuilder<T> {

        InboundEventPayloadExtractorBuilder<T> detectEventKeyUsingKeyDetector(InboundEventKeyDetector<T> inboundEventKeyDetector);

    }

    interface InboundEventPayloadJsonExtractorBuilder {

        InboundEventTransformerBuilder jsonFieldsMapDirectlyToPayload();

    }

    interface InboundEventPayloadXmlExtractorBuilder {

        InboundEventTransformerBuilder xmlElementsMapDirectlyToPayload();

    }

    interface InboundEventPayloadExtractorBuilder<T> {

        InboundEventTransformerBuilder payloadExtractor(InboundEventPayloadExtractor<T> inboundEventPayloadExtractor);

    }

    interface InboundEventTransformerBuilder {

        InboundChannelModelBuilder transformer(InboundEventTransformer inboundEventTransformer);

        InboundChannelModel register();

    }

}
