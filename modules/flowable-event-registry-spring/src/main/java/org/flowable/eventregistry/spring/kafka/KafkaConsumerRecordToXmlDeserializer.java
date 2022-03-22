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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jms.TextMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.eventregistry.api.FlowableEventInfo;
import org.flowable.eventregistry.api.InboundEventDeserializer;
import org.flowable.eventregistry.impl.FlowableEventInfoImpl;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.ObjectMapper;

public class KafkaConsumerRecordToXmlDeserializer implements InboundEventDeserializer<Document> {

    protected ObjectMapper objectMapper;
    
    public KafkaConsumerRecordToXmlDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public FlowableEventInfo<Document> deserialize(Object rawEvent) {
        Map<String, Object> headers = retrieveHeaders(rawEvent);
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            try (InputStream inputStream = new ByteArrayInputStream(convertEventToBytes(rawEvent))) {
                Document document = documentBuilder.parse(inputStream);
                return new FlowableEventInfoImpl<>(headers, document);
            }
            
        } catch (Exception e) {
            throw new FlowableException("Could not deserialize event to json", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    protected Map<String, Object> retrieveHeaders(Object rawEvent) {
        try {
            Map<String, Object> headers = new HashMap<>();
            ConsumerRecord<Object, Object> consumerRecord = (ConsumerRecord<Object, Object>) rawEvent;
            Headers consumerRecordHeaders = consumerRecord.headers();
            Iterator<Header> itConsumerRecordHeader = consumerRecordHeaders.iterator();
            while (itConsumerRecordHeader.hasNext()) {
                Header consumerRecordHeader = itConsumerRecordHeader.next();
                headers.put(consumerRecordHeader.key(), consumerRecordHeader.value());
            }
            
            return headers;
            
        } catch (Exception e) {
            throw new FlowableException("Could not get header information", e);
        }
    }
    
    protected byte[] convertEventToBytes(Object rawEvent) throws Exception {
        TextMessage textMessage = (TextMessage) rawEvent;
        return textMessage.getText().getBytes(StandardCharsets.UTF_8);
    }
}
