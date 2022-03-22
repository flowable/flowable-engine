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
package org.flowable.eventregistry.spring.jms;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Message;
import javax.jms.TextMessage;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.eventregistry.api.FlowableEventInfo;
import org.flowable.eventregistry.api.InboundEventDeserializer;
import org.flowable.eventregistry.impl.FlowableEventInfoImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JmsMessageToJsonDeserializer implements InboundEventDeserializer<JsonNode> {

    protected ObjectMapper objectMapper;

    public JmsMessageToJsonDeserializer(ObjectMapper objectMapper) {
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
            Message message = (Message) rawEvent;
            Enumeration<String> headerNames = message.getPropertyNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headers.put(headerName, message.getObjectProperty(headerName));
            }
            
            return headers;
            
        } catch (Exception e) {
            throw new FlowableException("Could not get header information", e);
        }
    }
    
    protected String convertEventToString(Object rawEvent) throws Exception {
        TextMessage textMessage = (TextMessage) rawEvent;
        return textMessage.getText();
    }

}
