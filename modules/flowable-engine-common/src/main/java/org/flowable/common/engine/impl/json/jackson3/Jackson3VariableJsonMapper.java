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
package org.flowable.common.engine.impl.json.jackson3;

import org.flowable.common.engine.impl.json.FlowableArrayNode;
import org.flowable.common.engine.impl.json.FlowableObjectNode;
import org.flowable.common.engine.impl.json.VariableJsonMapper;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Filip Hrisafov
 */
public class Jackson3VariableJsonMapper implements VariableJsonMapper {

    protected final ObjectMapper objectMapper;

    public Jackson3VariableJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Object readTree(String textValue) {
        return objectMapper.readTree(textValue);
    }

    @Override
    public Object readTree(byte[] bytes) {
        return objectMapper.readTree(bytes);
    }

    @Override
    public Object deepCopy(Object value) {
        return ((JsonNode) value).deepCopy();
    }

    @Override
    public boolean isJsonNode(Object value) {
        return value instanceof JsonNode;
    }

    @Override
    public Object transformToJsonNode(Object value) {
        return FlowableJackson3JsonNode.asJsonNode(value, () -> objectMapper);
    }

    @Override
    public FlowableObjectNode createObjectNode() {
        return new FlowableJackson3ObjectNode(objectMapper.createObjectNode());
    }

    @Override
    public FlowableArrayNode createArrayNode() {
        return new FlowableJackson3ArrayNode(objectMapper.createArrayNode());
    }
}
