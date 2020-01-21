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

import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.flowable.eventregistry.api.OutboundEventProcessingPipeline;
import org.flowable.eventregistry.api.OutboundEventSerializer;
import org.flowable.eventregistry.model.OutboundChannelModel;

/**
 * A builder to create an {@link OutboundChannelModel} instance.
 *  which represents a channel to send events to the 'outside world'.
 *
 * An {@link OutboundChannelModel} consists of the following parts:
 * - An adapter that defines how/where the events are sent, each with specific configurations.
 * - An event processing pipeline that
 *      - allows for serialization to the proper format
 *      - (Optionally) custom steps (or override any of the above)
 *
 * @author Joram Barrez
 */
public interface OutboundChannelModelBuilder {

    /**
     * Each channel needs to have a unique key to identity it.
     */
    OutboundChannelModelBuilder key(String key);
    
    /**
     * Set the name for the channel deployment.
     */
    OutboundChannelModelBuilder deploymentName(String deploymentName);
    
    /**
     * Set the resource name for the channel model.
     */
    OutboundChannelModelBuilder resourceName(String resourceName);
    
    /**
     * Set the category for the channel deployment.
     */
    OutboundChannelModelBuilder category(String category);
    
    /**
     * Set the tenant id for the channel deployment.
     */
    OutboundChannelModelBuilder deploymentTenantId(String deploymentTenantId);
    
    /**
     * Set the parent deployment id for the channel deployment.
     */
    OutboundChannelModelBuilder parentDeploymentId(String parentDeploymentId);

    /**
     * Sets a custom {@link OutboundEventChannelAdapter} via a delegate expression.
     */
    OutboundEventProcessingPipelineBuilder channelAdapter(String delegateExpression);

    /**
     * Configures an adapter which will send events using JMS.
     */
    OutboundJmsChannelBuilder jmsChannelAdapter(String destination);

    /**
     * Configures an adapter which will send events using RabbitMQ.
     */
    OutboundRabbitChannelBuilder rabbitChannelAdapter(String routingKey);

    /**
     * Configures an adapter which will send events using Kafka.
     */
    OutboundKafkaChannelBuilder kafkaChannelAdapter(String topic);

    /**
     * Creates the {@link OutboundChannelModel} instance based on the configuration
     * and registers it with the {@link org.flowable.eventregistry.api.EventRegistry}.
     */
    EventDeployment deploy();

    /**
     * Builder to create an {@link OutboundEventChannelAdapter} using JMS.
     */
    interface OutboundJmsChannelBuilder {

        OutboundEventProcessingPipelineBuilder eventProcessingPipeline();
    }

    /**
     * Builder to create an {@link OutboundEventChannelAdapter} using RabbitMQ.
     */
    interface OutboundRabbitChannelBuilder {

        /**
         * Sets the exchange to which events need to be sent.
         */
        OutboundRabbitChannelBuilder exchange(String exchange);

        OutboundEventProcessingPipelineBuilder eventProcessingPipeline();
    }

    /**
     * Builder to create an {@link OutboundEventChannelAdapter} using Kafka.
     */
    interface OutboundKafkaChannelBuilder {

        /**
         * Sets the record key for the outgoing message.
         */
        OutboundKafkaChannelBuilder recordKey(String key);

        OutboundEventProcessingPipelineBuilder eventProcessingPipeline();
    }

    /**
     * Builder for the 'event processing' pipeline which gets invoked before sending out the event.
     */
    interface OutboundEventProcessingPipelineBuilder {

        /**
         * Serializes the event to JSON.
         */
        OutboundChannelModelBuilder jsonSerializer();

        /**
         * Serializes the event to XML.
         */
        OutboundChannelModelBuilder xmlSerializer();

        /**
         * Uses a delegate expression to serialize the event.
         * The expression should resolve to an instance of {@link OutboundEventSerializer}.
         */
        OutboundChannelModelBuilder delegateExpressionSerializer(String delegateExpression);

        /**
         * Uses a delegate expression to determine the custom {@link OutboundEventProcessingPipeline} instance.
         */
        OutboundChannelModelBuilder eventProcessingPipeline(String delegateExpression);

    }

}
