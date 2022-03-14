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
import java.util.HashSet;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.eventregistry.api.InboundEventDeserializer;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RabbitMessageToJsonDeserializer implements InboundEventDeserializer<JsonNode> {

    protected Collection<String> stringContentTypes;
    protected ObjectMapper objectMapper = new ObjectMapper();

    public RabbitMessageToJsonDeserializer() {
        this.stringContentTypes = new HashSet<>();
        this.stringContentTypes.add(MessageProperties.CONTENT_TYPE_JSON);
        this.stringContentTypes.add(MessageProperties.CONTENT_TYPE_JSON_ALT);
        this.stringContentTypes.add(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
        this.stringContentTypes.add(MessageProperties.CONTENT_TYPE_XML);
    }
    
    @Override
    public JsonNode deserialize(Object rawEvent) {
        try {
            return objectMapper.readTree(convertEventToString(rawEvent));
        } catch (Exception e) {
            throw new FlowableException("Could not deserialize event to json", e);
        }
    }
    
    public String convertEventToString(Object rawEvent) throws Exception {
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
    }

    public Collection<String> getStringContentTypes() {
        return stringContentTypes;
    }

    public void setStringContentTypes(Collection<String> stringContentTypes) {
        this.stringContentTypes = stringContentTypes;
    }
}
