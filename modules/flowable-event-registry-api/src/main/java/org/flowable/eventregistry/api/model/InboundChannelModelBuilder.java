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
import org.flowable.eventregistry.api.InboundEventTenantDetector;
import org.flowable.eventregistry.api.InboundEventTransformer;
import org.flowable.eventregistry.model.InboundChannelModel;

/**
 * A builder to create a {@link InboundChannelModel} instance,
 * which represents a channel from the 'outside world' to receive events.
 *
 * An inbound channel consists of the following parts:
 * - An adapter that defines how/where the events are received, each with specific configurations.
 * - An event processing pipeline, which transforms the incoming event and extracts data and metadata:
 *     - deserialization (from the 'raw' event to something else)
 *     - event key detection: detects the 'key' which will define the {@link org.flowable.eventregistry.api.EventDefinition} to be used.
 *     - tenant detection (only relevant when using multi-tenant): detects a 'tenantId' which is used to determine
 *       the correct {@link org.flowable.eventregistry.api.EventDefinition}.
 *     - payload extraction: with the {@link org.flowable.eventregistry.api.EventDefinition} determined,
 *       the definition is used to extract the payload from the event data.
 *     - transformation: transforms the event to an internal representation,
 *       ready to be passed to the {@link org.flowable.eventregistry.api.EventRegistry}.
 *     - (Optionally) custom steps (or override any of the above)
 *
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public interface InboundChannelModelBuilder {

    /**
     * Each channel needs to have a unique key to identity it.
     */
    InboundChannelModelBuilder key(String key);

    /**
     * Sets a custom {@link InboundEventChannelAdapter} adapter instance which will receive
     * the events and pass them to the {@link org.flowable.eventregistry.api.EventRegistry}.
     */
    InboundEventProcessingPipelineBuilder channelAdapter(InboundEventChannelAdapter inboundEventChannelAdapter);

    /**
     * Configures an adapter which will receive events using JMS.
     */
    InboundJmsChannelBuilder jmsChannelAdapter(String destinationName);

    /**
     * Configures an adapter which will receive events using a RabbitMQ.
     */
    InboundRabbitChannelBuilder rabbitChannelAdapter(String queue);

    /**
     * Configures an adapter which receives events using Kafka.
     */
    InboundKafkaChannelBuilder kafkaChannelAdapter(String topic);

    /**
     * Creates the {@link InboundChannelModel} instance based on the configuration
     * and registers it with the {@link org.flowable.eventregistry.api.EventRegistry}.
     */
    InboundChannelModel register();

    /**
     * Builder to create an {@link InboundEventChannelAdapter} using JMS.
     */
    interface InboundJmsChannelBuilder {

        /**
         * Set the JMS message selector, which can be used to filter
         * out incoming events. See the JMS spec for more info.
         */
        InboundJmsChannelBuilder selector(String selector);

        /**
         * Sets the JMS subscription. See the JMS spec for more info.
         */
        InboundJmsChannelBuilder subscription(String subscription);

        /**
         * Sets the concurrency for the listener (e.g "5-10"). See the Spring JMS docs for more info.
         */
        InboundJmsChannelBuilder concurrency(String concurrency);

        /**
         * Continue building the {@link InboundChannelModel} by configuring the next parts (if any).
         */
        InboundEventProcessingPipelineBuilder eventProcessingPipeline();
    }

    /**
     * Builder to create an {@link InboundEventChannelAdapter} using RabbitMQ.
     */
    interface InboundRabbitChannelBuilder {

        /**
         * Sets whether this adapter will be the only consumer of messages of the provided queue(s).
         * See the Spring Rabbit docs for more info.
         */
        InboundRabbitChannelBuilder exclusive(boolean exclusive);

        /**
         * Sets the priority of this adapter. See the Spring Rabbit docs for more info.
         */
        InboundRabbitChannelBuilder priority(String priority);

        /**
         * Bean name of a org.springframework.amqp.rabbit.core.RabbitAdmin instance.
         * See the Spring Rabbit docs for more info.
         */
        InboundRabbitChannelBuilder admin(String admin);

        /**
         * Sets the concurrency for the listener. See the Spring JMS docs for more info.
         */
        InboundRabbitChannelBuilder concurrency(String concurrency);

        /**
         * Bean name of a TaskExecutor instance used to process incoming messages.
         */
        InboundRabbitChannelBuilder executor(String executor);

        /**
         * Sets the AckMode (e.g. NONE/MANUAL/AUTO). See the Spring JMS docs for more info.
         */
        InboundRabbitChannelBuilder ackMode(String ackMode);

        /**
         * Continue building the {@link InboundChannelModel} by configuring the next parts (if any).
         */
        InboundEventProcessingPipelineBuilder eventProcessingPipeline();
    }

    /**
     * Builder to create an {@link InboundEventChannelAdapter} using Kafka.
     */
    interface InboundKafkaChannelBuilder {

        /**
         * Sets the groupId for this Kafka adapter. See Kafka docs for more info.
         */
        InboundKafkaChannelBuilder groupId(String groupId);

        /**
         * Sets the client id prefix for this Kafka adapter. See Kafka docs for more info.
         */
        InboundKafkaChannelBuilder clientIdPrefix(String clientIdPrefix);

        /**
         * Sets the concurrency (and integer) for this Kafka adapter. See the Spring Kafka docs for more information.
         */
        InboundKafkaChannelBuilder concurrency(String concurrency);

        /**
         * Sets custom properties for this Kafka adapter. See the Spring Kafka docs for more information.
         */
        InboundKafkaChannelBuilder property(String name, String value);

        /**
         * Continue building the {@link InboundChannelModel} by configuring the next parts (if any).
         */
        InboundEventProcessingPipelineBuilder eventProcessingPipeline();
    }

    /**
     * Builder for the 'processing pipeline' part of the {@link InboundChannelModel}.
     */
    interface InboundEventProcessingPipelineBuilder {

        /**
         * Deserializes the event to JSON.
         */
        InboundEventKeyJsonDetectorBuilder jsonDeserializer();

        /**
         * Deserializes the event to XML.
         */
        InboundEventKeyXmlDetectorBuilder xmlDeserializer();

        /**
         * Deserializes the event using a custom {@link InboundEventDeserializer}.
         */
        <T> InboundEventKeyDetectorBuilder<T> deserializer(InboundEventDeserializer<T> deserializer);

        /**
         * Continue configuring the event processing pipeline.
         */
        InboundChannelModelBuilder eventProcessingPipeline(InboundEventProcessingPipeline inboundEventProcessingPipeline);

        /**
         * Finish configuring the event processing pipeline.
         */
        InboundEventProcessingPipeline build();

    }

    /**
     * Builder for the 'key detection' part of the {@link InboundChannelModel}, specifically for JSON events.
     */
    interface InboundEventKeyJsonDetectorBuilder {

        /**
         * Sets the event key to a hardcoded value. This is useful when the channel only receives one type of event.
         */
        InboundEventTenantJsonDetectorBuilder fixedEventKey(String key);

        /**
         * Determines the key of the event based on a top-level field in the JSON representation in the event.
         */
        InboundEventTenantJsonDetectorBuilder detectEventKeyUsingJsonField(String field);

        /**
         * Determines the key of the event using on a JSON Path expression to find the value.
         */
        InboundEventTenantJsonDetectorBuilder detectEventKeyUsingJsonPathExpression(String jsonPathExpression);

    }

    /**
     * Builder for the 'tenant ID detection' part of the {@link InboundChannelModel}, specifically for JSON events
     */
    interface InboundEventTenantJsonDetectorBuilder extends InboundEventPayloadJsonExtractorBuilder {  // Extends because using tenant extraction is optional

        /**
         * Sets the tenant to a hardcoded value. Useful for when the channel only receives events for a given tenant.
         */
        InboundEventPayloadJsonExtractorBuilder fixedTenantId(String tenantId);

        /**
         * Determines the tenant ID by using a JSONPath expression.
         */
        InboundEventPayloadJsonExtractorBuilder detectEventTenantUsingJsonPathExpression(String jsonPathExpression);

    }

    /**
     * Builder for the 'key detection' part of the {@link InboundChannelModel}, specifically for XML events.
     */
    interface InboundEventKeyXmlDetectorBuilder {

        /**
         * Sets the event key to a hardcoded value. This is useful when the channel only receives one type of event.
         */
        InboundEventTenantXmlDetectorBuilder fixedEventKey(String key);

        /**
         * Determines the key of the event using on a XPATH expression to find the value.
         */
        InboundEventTenantXmlDetectorBuilder detectEventKeyUsingXPathExpression(String xPathExpression);

    }

    /**
     * Builder for the 'tenant ID detection' part of the {@link InboundChannelModel}, specifically for XML events
     */
    interface InboundEventTenantXmlDetectorBuilder extends InboundEventPayloadXmlExtractorBuilder {  // Extends because using tenant extraction is optional

        /**
         * Sets the tenant to a hardcoded value. Useful for when the channel only receives events for a given tenant.
         */
        InboundEventPayloadXmlExtractorBuilder fixedTenantId(String tenantId);

        /**
         * Determines the tenant ID by using an XPath expression.
         */
        InboundEventPayloadXmlExtractorBuilder detectEventTenantUsingXPathExpression(String xPathExpression);

    }

    /**
     * Builder for the 'key detection' part of the {@link InboundChannelModel}.
     */
    interface InboundEventKeyDetectorBuilder<T> {

        /**
         * Determines the key of the event using a custom {@link InboundEventKeyDetector}.
         */
        InboundEventTenantDetectorBuilder<T> detectEventKeyUsingKeyDetector(InboundEventKeyDetector<T> inboundEventKeyDetector);

    }

    /**
     * Builder for the 'tenant ID detection' part of the {@link InboundChannelModel}.
     */
    interface InboundEventTenantDetectorBuilder<T> extends InboundEventPayloadExtractorBuilder<T> {  // Extends because using tenant extraction is optional

        /**
         * Sets the tenant to a hardcoded value. Useful for when the channel only receives events for a given tenant.
         */
        InboundEventPayloadExtractorBuilder<T> fixedTenantId(String tenantId);

        /**
         * Detect the tenant using a custom {@link InboundEventTenantDetector} instance.
         */
        InboundEventPayloadExtractorBuilder<T> detectTenantUsingTenantDetector(InboundEventTenantDetector<T>inboundEventTenantDetector);

    }

    /**
     * Builder for the 'payload extraction' part of the {@link InboundChannelModel}.
     */
    interface InboundEventPayloadJsonExtractorBuilder {

        /**
         * Extracts the payload directly from (top-level) json fields.
         */
        InboundEventTransformerBuilder jsonFieldsMapDirectlyToPayload();

    }

    /**
     * Builder for the 'payload extraction' part of the {@link InboundChannelModel}.
     */
    interface InboundEventPayloadXmlExtractorBuilder {

        /**
         * Extracts the payload directly from (top-level) xml elements.
         */
        InboundEventTransformerBuilder xmlElementsMapDirectlyToPayload();

    }

    /**
     * Builder for the 'payload extraction' part of the {@link InboundChannelModel}.
     */
    interface InboundEventPayloadExtractorBuilder<T> {

        /**
         * Extracts the payload with a custom {@link InboundEventPayloadExtractor} instance.
         */
        InboundEventTransformerBuilder payloadExtractor(InboundEventPayloadExtractor<T> inboundEventPayloadExtractor);

    }

    interface InboundEventTransformerBuilder {

        InboundChannelModelBuilder transformer(InboundEventTransformer inboundEventTransformer);

        InboundChannelModel register();

    }

}
