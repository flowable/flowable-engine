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
package org.flowable.eventregistry.impl;

import org.flowable.eventregistry.api.InboundEventDeserializer;

public interface ChannelProcessingPipelineManager {
    
    final String CHANNEL_DEFAULT_TYPE = "default";
    final String CHANNEL_JMS_TYPE = "jms";
    final String CHANNEL_KAFKA_TYPE = "kafka";
    final String CHANNEL_RABBIT_TYPE = "rabbit";
    
    final String DESERIALIZER_JSON_TYPE = "deserializerjson";
    final String DESERIALIZER_XML_TYPE = "deserializerxml";

    InboundEventDeserializer<?> getInboundEventDeserializer(String channelType, String deserializerType);

    void registerInboundEventDeserializer(String channelType, String deserializerType, InboundEventDeserializer<?> inboundEventDeserializer);
   
}
