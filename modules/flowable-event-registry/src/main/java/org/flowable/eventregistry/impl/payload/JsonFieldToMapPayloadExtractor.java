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
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.flowable.eventregistry.api.FlowableEventInfo;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.runtime.EventPayloadInstanceImpl;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.EventPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class JsonFieldToMapPayloadExtractor extends BaseMapPayloadExtractor<JsonNode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonFieldToMapPayloadExtractor.class);

    @Override
    public Collection<EventPayloadInstance> extractPayload(EventModel eventModel, FlowableEventInfo<JsonNode> event) {
        Map<String, Object> filteredHeaders = convertHeaderValues(event, eventModel);
        Collection<EventPayloadInstance> headerInstances = eventModel.getHeaders().stream()
                .filter(headerDefinition -> filteredHeaders.containsKey(headerDefinition.getName()))
                .map(headerDefinition -> new EventPayloadInstanceImpl(headerDefinition, filteredHeaders.get(headerDefinition.getName())))
                .collect(Collectors.toList());
        
        Collection<EventPayloadInstance> payloadInstances = eventModel.getPayload().stream()
            .filter(payloadDefinition -> event.getPayload().has(payloadDefinition.getName()))
            .map(payloadDefinition -> new EventPayloadInstanceImpl(payloadDefinition, getPayloadValue(event.getPayload(), 
                    payloadDefinition.getName(), payloadDefinition.getType())))
            .collect(Collectors.toList());
        
        if (StringUtils.isNotEmpty(eventModel.getFullPayloadPropertyName())) {
            EventPayload fullEventPayloadDefinition = new EventPayload(eventModel.getFullPayloadPropertyName(), EventPayloadTypes.JSON);
            payloadInstances.add(new EventPayloadInstanceImpl(fullEventPayloadDefinition, event.getPayload()));
        }
        
        payloadInstances.addAll(headerInstances);
        return payloadInstances;
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
