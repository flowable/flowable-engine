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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.eventregistry.api.FlowableEventInfo;
import org.flowable.eventregistry.api.InboundEventDeserializer;
import org.flowable.eventregistry.impl.FlowableEventInfoImpl;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RabbitMessageToJsonDeserializer implements InboundEventDeserializer<JsonNode> {

    protected Collection<String> stringContentTypes;
    protected ObjectMapper objectMapper;

    public RabbitMessageToJsonDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.stringContentTypes = new HashSet<>();
        this.stringContentTypes.add(MessageProperties.CONTENT_TYPE_JSON);
        this.stringContentTypes.add(MessageProperties.CONTENT_TYPE_JSON_ALT);
        this.stringContentTypes.add(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
        this.stringContentTypes.add(MessageProperties.CONTENT_TYPE_XML);
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
    
    protected Map<String, Object> retrieveHeaders(Object rawEvent) {
        Map<String, Object> headers = new HashMap<>();
        
        if (rawEvent instanceof Message) {
            Message message = (Message) rawEvent;
            Map<String, Object> headerMap = message.getMessageProperties().getHeaders();
            for (String headerName : headerMap.keySet()) {
                headers.put(headerName, headerMap.get(headerName));
            }
        }
        
        return headers;
    }
    
    public String convertEventToString(Object rawEvent) throws Exception {
        if (rawEvent instanceof Message) {
            Message message = (Message) rawEvent;
            
            byte[] body = message.getBody();
            MessageProperties messageProperties = message.getMessageProperties();
            String contentType = messageProperties != null ? messageProperties.getContentType() : null;
            
            String bodyContent = null;
            if (stringContentTypes.contains(contentType)) {
                bodyContent = new String(body, StandardCharsets.UTF_8);
            } else {
                bodyContent = Base64.getEncoder().encodeToString(body);
            }
            return bodyContent;
            
        } else {
            return rawEvent.toString();
        }
    }

    public Collection<String> getStringContentTypes() {
        return stringContentTypes;
    }

    public void setStringContentTypes(Collection<String> stringContentTypes) {
        this.stringContentTypes = stringContentTypes;
    }
}
