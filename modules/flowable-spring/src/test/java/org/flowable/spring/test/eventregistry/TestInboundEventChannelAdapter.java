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
package org.flowable.spring.test.eventregistry;

import java.util.Random;

import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.model.InboundChannelModel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestInboundEventChannelAdapter implements InboundEventChannelAdapter {

    public InboundChannelModel inboundChannelModel;
    public EventRegistry eventRegistry;

    @Override
    public void setInboundChannelModel(InboundChannelModel inboundChannelModel) {
        this.inboundChannelModel = inboundChannelModel;
    }

    @Override
    public void setEventRegistry(EventRegistry eventRegistry) {
        this.eventRegistry = eventRegistry;
    }

    public void triggerTestEvent() {
        triggerTestEvent(null);
    }

    public void triggerTestEvent(String customerId) {
        triggerTestEvent(customerId, null);
    }

    public void triggerOrderTestEvent(String orderId) {
        triggerTestEvent(null, orderId);
    }

    public void triggerTestEvent(String customerId, String orderId) {
        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode json = objectMapper.createObjectNode();
        json.put("type", "myEvent");
        if (customerId != null) {
            json.put("customerId", customerId);
        }

        if (orderId != null) {
            json.put("orderId", orderId);
        }
        json.put("payload1", "Hello World");
        json.put("payload2", new Random().nextInt());
        try {
            eventRegistry.eventReceived(inboundChannelModel, objectMapper.writeValueAsString(json));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
