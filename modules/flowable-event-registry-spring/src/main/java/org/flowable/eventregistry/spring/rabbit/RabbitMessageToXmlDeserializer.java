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
package org.flowable.eventregistry.spring.rabbit;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.eventregistry.api.FlowableEventInfo;
import org.flowable.eventregistry.api.InboundEventDeserializer;
import org.flowable.eventregistry.impl.FlowableEventInfoImpl;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RabbitMessageToXmlDeserializer implements InboundEventDeserializer<Document> {

    protected Collection<String> stringContentTypes;
    protected ObjectMapper objectMapper;

    public RabbitMessageToXmlDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.stringContentTypes = new HashSet<>();
        this.stringContentTypes.add(MessageProperties.CONTENT_TYPE_JSON);
        this.stringContentTypes.add(MessageProperties.CONTENT_TYPE_JSON_ALT);
        this.stringContentTypes.add(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
        this.stringContentTypes.add(MessageProperties.CONTENT_TYPE_XML);
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
    
    protected Map<String, Object> retrieveHeaders(Object rawEvent) {
        Map<String, Object> headers = new HashMap<>();
        
        Message message = (Message) rawEvent;
        Map<String, Object> headerMap = message.getMessageProperties().getHeaders();
        for (String headerName : headerMap.keySet()) {
            headers.put(headerName, headerMap.get(headerName));
        }
        
        return headers;
    }
    
    protected byte[] convertEventToBytes(Object rawEvent) throws Exception {
        Message message = (Message) rawEvent;
        return message.getBody();
    }

    public Collection<String> getStringContentTypes() {
        return stringContentTypes;
    }

    public void setStringContentTypes(Collection<String> stringContentTypes) {
        this.stringContentTypes = stringContentTypes;
    }
}
