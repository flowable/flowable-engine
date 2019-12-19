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
import org.flowable.eventregistry.api.runtime.EventCorrelationParameterInstance;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.runtime.EventCorrelationParameterInstanceImpl;
import org.flowable.eventregistry.impl.runtime.EventPayloadInstanceImpl;
import org.flowable.eventregistry.model.EventModel;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class JsonFieldToMapPayloadExtractor implements InboundEventPayloadExtractor<JsonNode> {

    @Override
    public Collection<EventCorrelationParameterInstance> extractCorrelationParameters(EventModel eventDefinition, JsonNode event) {
        return eventDefinition.getCorrelationParameters().stream()
            .filter(parameterDefinition -> event.has(parameterDefinition.getName()))
            .map(parameterDefinition -> new EventCorrelationParameterInstanceImpl(parameterDefinition, getPayloadValue(event, parameterDefinition.getName(), parameterDefinition.getType())))
            .collect(Collectors.toList());
    }

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

        } else {
            // TODO: handle type not matching

        }

        return value;
    }

}
