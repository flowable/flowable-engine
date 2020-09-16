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
package org.flowable.eventregistry.impl.payload;

import java.util.Collection;
import java.util.stream.Collectors;

import org.flowable.eventregistry.api.InboundEventPayloadExtractor;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.runtime.EventPayloadInstanceImpl;
import org.flowable.eventregistry.model.EventModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class JsonFieldToMapPayloadExtractor implements InboundEventPayloadExtractor<JsonNode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonFieldToMapPayloadExtractor.class);

    @Override
    public Collection<EventPayloadInstance> extractPayload(EventModel eventDefinition, JsonNode event) {
        return eventDefinition.getPayload().stream()
            .filter(payloadDefinition -> event.has(payloadDefinition.getName()))
            .map(payloadDefinition -> new EventPayloadInstanceImpl(payloadDefinition, getPayloadValue(event, payloadDefinition.getName(), payloadDefinition.getType())))
            .collect(Collectors.toList());
    }

    protected Object getPayloadValue(JsonNode event, String definitionName, String definitionType) {
        JsonNode parameterNode = event.get(definitionName);
        Object value = null;

        if (EventPayloadTypes.STRING.equals(definitionType)) {
            value = parameterNode.asText();

        } else if (EventPayloadTypes.BOOLEAN.equals(definitionType)) {
            value = parameterNode.booleanValue();

        } else if (EventPayloadTypes.INTEGER.equals(definitionType)) {
            value = parameterNode.intValue();

        } else if (EventPayloadTypes.DOUBLE.equals(definitionType)) {
            value = parameterNode.doubleValue();

        } else if (EventPayloadTypes.LONG.equals(definitionType)) {
          value = parameterNode.longValue();

        } else if (EventPayloadTypes.JSON.equals(definitionType)) {
            value = parameterNode;

        } else {
            LOGGER.warn("Unsupported payload type: {} ", definitionType);
            value = parameterNode.asText();

        }

        return value;
    }

}
