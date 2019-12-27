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

import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.flowable.eventregistry.api.OutboundEventProcessingPipeline;
import org.flowable.eventregistry.api.OutboundEventSerializer;
import org.flowable.eventregistry.model.OutboundChannelModel;

/**
 * @author Joram Barrez
 */
public interface OutboundChannelModelBuilder {

    OutboundChannelModelBuilder key(String key);

    OutboundEventProcessingPipelineBuilder channelAdapter(OutboundEventChannelAdapter outboundEventChannelAdapter);

    OutboundJmsChannelBuilder jmsChannelAdapter(String destination);

    OutboundRabbitChannelBuilder rabbitChannelAdapter(String routingKey);

    OutboundKafkaChannelBuilder kafkaChannelAdapter(String topic);

    OutboundChannelModel register();

    interface OutboundJmsChannelBuilder {

        OutboundEventProcessingPipelineBuilder eventProcessingPipeline();
    }

    interface OutboundRabbitChannelBuilder {

        OutboundRabbitChannelBuilder exchange(String exchange);

        OutboundEventProcessingPipelineBuilder eventProcessingPipeline();
    }

    interface OutboundKafkaChannelBuilder {

        OutboundKafkaChannelBuilder recordKey(String key);

        OutboundEventProcessingPipelineBuilder eventProcessingPipeline();
    }

    interface OutboundEventProcessingPipelineBuilder {

        OutboundChannelModelBuilder jsonSerializer();

        OutboundChannelModelBuilder xmlSerializer();

        OutboundChannelModelBuilder serializer(OutboundEventSerializer serializer);

        OutboundChannelModelBuilder eventProcessingPipeline(OutboundEventProcessingPipeline outboundEventProcessingPipeline);

        OutboundEventProcessingPipeline build();

    }

}
