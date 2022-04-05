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
package org.flowable.eventregistry.spring.kafka;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.eventregistry.api.FlowableEventInfo;
import org.flowable.eventregistry.api.InboundEventDeserializer;
import org.flowable.eventregistry.impl.FlowableEventInfoImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class KafkaConsumerRecordToJsonDeserializer implements InboundEventDeserializer<JsonNode> {

    protected ObjectMapper objectMapper;
    
    public KafkaConsumerRecordToJsonDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public FlowableEventInfo<JsonNode> deserialize(Object rawEvent) {
        Map<String, Object> headers = retrieveHeaders(rawEvent);
        try {
            JsonNode eventNode = objectMapper.readTree(convertEventToString(rawEvent));
            return new FlowableEventInfoImpl<>(headers, eventNode);
            
        } catch (Exception e) {
            throw new FlowableException("Could not deserialize event to json", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    protected Map<String, Object> retrieveHeaders(Object rawEvent) {
        try {
            Map<String, Object> headers = new HashMap<>();
            if (rawEvent instanceof ConsumerRecord) {
                ConsumerRecord<Object, Object> consumerRecord = (ConsumerRecord<Object, Object>) rawEvent;
                Headers consumerRecordHeaders = consumerRecord.headers();
                Iterator<Header> itConsumerRecordHeader = consumerRecordHeaders.iterator();
                while (itConsumerRecordHeader.hasNext()) {
                    Header consumerRecordHeader = itConsumerRecordHeader.next();
                    headers.put(consumerRecordHeader.key(), consumerRecordHeader.value());
                }
            }
            
            return headers;
            
        } catch (Exception e) {
            throw new FlowableException("Could not get header information", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    protected String convertEventToString(Object rawEvent) throws Exception {
        if (rawEvent instanceof ConsumerRecord) {
            ConsumerRecord<Object, Object> consumerRecord = (ConsumerRecord<Object, Object>) rawEvent;
            return consumerRecord.value().toString();
        } else {
            return rawEvent.toString();
        }
    }
}
