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
package org.flowable.eventregistry.impl;

import org.flowable.eventregistry.api.JsonPayloadValueTransformer;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventregistry.model.EventPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

public class DefaultJsonPayloadValueTransformer implements JsonPayloadValueTransformer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultJsonPayloadValueTransformer.class);

    public Object transformValue(JsonNode parameterNode, String definitionType, EventPayload eventPayload, String parentDeploymentId, String tenantId) {
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
