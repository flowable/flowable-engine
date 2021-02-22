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
package org.flowable.eventregistry.json.converter;

import java.util.Collection;

import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.EventPayload;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 */
public class EventJsonConverter {

    protected ObjectMapper objectMapper = new ObjectMapper();

    public EventModel convertToEventModel(String modelJson) {
        try {
            JsonNode modelNode = objectMapper.readTree(modelJson);
            EventModel eventModel = new EventModel();

            eventModel.setKey(modelNode.path("key").asText(null));
            eventModel.setName(modelNode.path("name").asText(null));

            JsonNode payloadNode = modelNode.path("payload");

            if (payloadNode.isArray()) {
                for (JsonNode node : payloadNode) {
                    String name = node.path("name").asText(null);
                    String type = node.path("type").asText(null);
                    boolean correlationParameter = node.path("correlationParameter").asBoolean(false);
                    if (correlationParameter) {
                        eventModel.addCorrelation(name, type);
                    } else {
                        eventModel.addPayload(name, type);
                    }
                }
            }

            JsonNode correlationParameters = modelNode.path("correlationParameters");
            if (correlationParameters.isArray()) {
                for (JsonNode correlationPayloadNode : correlationParameters) {
                    String name = correlationPayloadNode.path("name").asText(null);
                    String type = correlationPayloadNode.path("type").asText(null);
                    eventModel.addCorrelation(name, type);
                }
            }

            return eventModel;
        } catch (Exception e) {
            throw new FlowableEventJsonException("Error reading event json", e);
        }
    }

    public String convertToJson(EventModel definition) {
        ObjectNode modelNode = objectMapper.createObjectNode();

        if (definition.getKey() != null) {
            modelNode.put("key", definition.getKey());
        }

        if (definition.getName() != null) {
            modelNode.put("name", definition.getName());
        }

        Collection<EventPayload> payload = definition.getPayload();
        if (!payload.isEmpty()) {
            ArrayNode payloadNode = modelNode.putArray("payload");
            for (EventPayload eventPayload : payload) {
                ObjectNode eventPayloadNode = payloadNode.addObject();
                if (eventPayload.getName() != null) {
                    eventPayloadNode.put("name", eventPayload.getName());
                }

                if (eventPayload.getType() != null) {
                    eventPayloadNode.put("type", eventPayload.getType());
                }

                if (eventPayload.isCorrelationParameter()) {
                    eventPayloadNode.put("correlationParameter", true);
                }
            }
        }

        try {
            return objectMapper.writeValueAsString(modelNode);
        } catch (Exception e) {
            throw new FlowableEventJsonException("Error writing event json", e);
        }
    }
}