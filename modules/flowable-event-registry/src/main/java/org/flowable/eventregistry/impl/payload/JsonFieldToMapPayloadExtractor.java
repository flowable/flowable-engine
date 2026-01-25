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
import org.flowable.eventregistry.api.JsonPayloadValueTransformer;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.runtime.EventPayloadInstanceImpl;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.EventPayload;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class JsonFieldToMapPayloadExtractor implements InboundEventPayloadExtractor<JsonNode> {

    protected JsonPayloadValueTransformer payloadValueTransformer;
    
    public JsonFieldToMapPayloadExtractor(JsonPayloadValueTransformer payloadValueTransformer) {
        this.payloadValueTransformer = payloadValueTransformer;
    }

    @Override
    public Collection<EventPayloadInstance> extractPayload(EventModel eventModel, JsonNode payload, String parentDeploymentId, String tenantId) {
        return eventModel.getPayload().stream()
            .filter(payloadDefinition -> payloadDefinition.isFullPayload() || payload.has(payloadDefinition.getName()))
            .map(payloadDefinition -> new EventPayloadInstanceImpl(payloadDefinition, getPayloadValue(payload, payloadDefinition, parentDeploymentId, tenantId)))
            .collect(Collectors.toList());
    }

    protected Object getPayloadValue(JsonNode event, EventPayload eventPayload, String parentDeploymentId, String tenantId) {
        if (eventPayload.isFullPayload()) {
            return event;
        }
        
        JsonNode parameterNode = event.get(eventPayload.getName());
        return payloadValueTransformer.transformValue(parameterNode, eventPayload.getType(), eventPayload, parentDeploymentId, tenantId);
    }

}
