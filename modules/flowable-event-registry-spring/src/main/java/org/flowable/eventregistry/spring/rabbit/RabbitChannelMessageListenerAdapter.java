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

import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;

/**
 * @author Filip Hrisafov
 */
public class RabbitChannelMessageListenerAdapter implements MessageListener {

    protected Collection<String> stringContentTypes;

    protected EventRegistry eventRegistry;
    protected InboundChannelModel inboundChannelModel;

    public RabbitChannelMessageListenerAdapter(EventRegistry eventRegistry, InboundChannelModel inboundChannelModel) {
        this.eventRegistry = eventRegistry;
        this.inboundChannelModel = inboundChannelModel;
        this.stringContentTypes = new HashSet<>();
        this.stringContentTypes.add(MessageProperties.CONTENT_TYPE_JSON);
        this.stringContentTypes.add(MessageProperties.CONTENT_TYPE_JSON_ALT);
        this.stringContentTypes.add(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
        this.stringContentTypes.add(MessageProperties.CONTENT_TYPE_XML);
    }

    @Override
    public void onMessage(Message message) {
        byte[] body = message.getBody();
        MessageProperties messageProperties = message.getMessageProperties();
        String contentType = messageProperties != null ? messageProperties.getContentType() : null;

        String rawEvent;
        if (body == null) {
            rawEvent = null;
        } else if (stringContentTypes.contains(contentType)) {
            rawEvent = new String(body, StandardCharsets.UTF_8);
        } else {
            rawEvent = Base64.getEncoder().encodeToString(body);
        }

        eventRegistry.eventReceived(inboundChannelModel, rawEvent);
    }

    public EventRegistry getEventRegistry() {
        return eventRegistry;
    }

    public void setEventRegistry(EventRegistry eventRegistry) {
        this.eventRegistry = eventRegistry;
    }

    public InboundChannelModel getInboundChannelModel() {
        return inboundChannelModel;
    }

    public void setInboundChannelModel(InboundChannelModel inboundChannelModel) {
        this.inboundChannelModel = inboundChannelModel;
    }

    public Collection<String> getStringContentTypes() {
        return stringContentTypes;
    }

    public void setStringContentTypes(Collection<String> stringContentTypes) {
        this.stringContentTypes = stringContentTypes;
    }
}
