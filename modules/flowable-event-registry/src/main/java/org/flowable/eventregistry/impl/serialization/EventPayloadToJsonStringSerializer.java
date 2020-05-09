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
package org.flowable.eventregistry.impl.serialization;

import java.util.Collection;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.eventregistry.api.OutboundEventSerializer;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Simple {@link EventInstance} serialization that maps all {@link org.flowable.eventregistry.api.runtime.EventPayloadInstance}'s
 * to a json which gets transformed to a String.
 *
 * @author Joram Barrez
 */
public class EventPayloadToJsonStringSerializer implements OutboundEventSerializer {

    protected ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String serialize(EventInstance eventInstance) {
        ObjectNode objectNode = objectMapper.createObjectNode();

        Collection<EventPayloadInstance> payloadInstances = eventInstance.getPayloadInstances();
        for (EventPayloadInstance payloadInstance : payloadInstances) {

            String definitionType = payloadInstance.getDefinitionType();
            Object payloadInstanceValue = payloadInstance.getValue();

            if (payloadInstanceValue != null) {

                if (EventPayloadTypes.STRING.equals(definitionType)) {
                    objectNode.put(payloadInstance.getDefinitionName(), payloadInstanceValue.toString());

                } else if (EventPayloadTypes.DOUBLE.equals(definitionType)) {

                    if (payloadInstanceValue instanceof Number) {
                        objectNode.put(payloadInstance.getDefinitionName(), ((Number) payloadInstanceValue).doubleValue());
                    } else if (payloadInstanceValue instanceof String) {
                        objectNode.put(payloadInstance.getDefinitionName(), Double.valueOf((String) payloadInstanceValue));
                    } else {
                        throw new FlowableIllegalArgumentException("Cannot convert event payload " + payloadInstanceValue + " to type 'double'");
                    }

                } else if (EventPayloadTypes.INTEGER.equals(definitionType)) {

                    if (payloadInstanceValue instanceof Number) {
                        objectNode.put(payloadInstance.getDefinitionName(), ((Number) payloadInstanceValue).intValue());
                    } else if (payloadInstanceValue instanceof String) {
                        objectNode.put(payloadInstance.getDefinitionName(), Integer.valueOf((String) payloadInstanceValue));
                    } else {
                        throw new FlowableIllegalArgumentException("Cannot convert event payload " + payloadInstanceValue + " to type 'integer'");
                    }

                } else if (EventPayloadTypes.LONG.equals(definitionType)) {

                    if (payloadInstanceValue instanceof Number) {
                        objectNode.put(payloadInstance.getDefinitionName(), ((Number) payloadInstanceValue).longValue());
                    } else if (payloadInstanceValue instanceof String) {
                        objectNode.put(payloadInstance.getDefinitionName(), Long.valueOf((String) payloadInstanceValue));
                    } else {
                        throw new FlowableIllegalArgumentException("Cannot convert event payload " + payloadInstanceValue + " to type 'long'");
                    }

                } else if (EventPayloadTypes.BOOLEAN.equals(definitionType)) {

                    if (payloadInstanceValue instanceof Boolean) {
                        objectNode.put(payloadInstance.getDefinitionName(), (Boolean) payloadInstanceValue);
                    } else if (payloadInstanceValue instanceof String) {
                        objectNode.put(payloadInstance.getDefinitionName(), Boolean.valueOf((String) payloadInstanceValue));
                    }  else {
                        throw new FlowableIllegalArgumentException("Cannot convert event payload " + payloadInstanceValue + " to type 'boolean'");
                    }

                } else if (EventPayloadTypes.JSON.equals(definitionType)) {

                    if (payloadInstanceValue instanceof JsonNode) {
                        objectNode.set(payloadInstance.getDefinitionName(), (JsonNode) payloadInstanceValue);
                    } else if (payloadInstanceValue instanceof String) {
                        JsonNode jsonNode = null;
                        try {
                            jsonNode = objectMapper.readTree((String) payloadInstanceValue);
                        } catch (JsonProcessingException e) {
                            throw new FlowableIllegalArgumentException("Could not read json event payload", e);
                        }
                        objectNode.set(payloadInstance.getDefinitionName(), jsonNode);
                    }  else {
                        throw new FlowableIllegalArgumentException("Cannot convert event payload " + payloadInstanceValue + " to type 'json'");
                    }

                } else {
                    throw new FlowableIllegalArgumentException("Unsupported event payload instance type: " + definitionType);
                }

            } else {
                objectNode.putNull(payloadInstance.getDefinitionName());

            }

        }

        try {
            return objectMapper.writeValueAsString(objectNode);
        } catch (JsonProcessingException e) {
            throw new FlowableException("Could not serialize event to json string", e);
        }
    }

}
