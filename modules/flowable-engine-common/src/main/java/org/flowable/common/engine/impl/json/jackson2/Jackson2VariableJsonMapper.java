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
package org.flowable.common.engine.impl.json.jackson2;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.flowable.common.engine.impl.json.FlowableArrayNode;
import org.flowable.common.engine.impl.json.FlowableObjectNode;
import org.flowable.common.engine.impl.json.VariableJsonMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Filip Hrisafov
 */
@Deprecated
public class Jackson2VariableJsonMapper implements VariableJsonMapper {

    protected final ObjectMapper objectMapper;

    public Jackson2VariableJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Object readTree(String textValue) {
        try {
            return objectMapper.readTree(textValue);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Failed to read text value", e);
        }
    }

    @Override
    public Object readTree(byte[] bytes) {
        try {
            return objectMapper.readTree(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read text value", e);
        }
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
        return FlowableJackson2JsonNode.asJsonNode(value, () -> objectMapper);
    }

    @Override
    public FlowableObjectNode createObjectNode() {
        return new FlowableJackson2ObjectNode(objectMapper.createObjectNode());
    }

    @Override
    public FlowableArrayNode createArrayNode() {
        return new FlowableJackson2ArrayNode(objectMapper.createArrayNode());
    }
}
