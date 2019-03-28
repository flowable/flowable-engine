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
package org.flowable.variable.service.impl.types;

import java.nio.charset.StandardCharsets;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.variable.api.types.ValueFields;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
public class LongJsonType extends SerializableType {

    public static final String TYPE_NAME = "longJson";

    protected final int minLength;
    protected ObjectMapper objectMapper;

    public LongJsonType(int minLength, ObjectMapper objectMapper) {
        this.minLength = minLength;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    @Override
    public boolean isAbleToStore(Object value) {
        if (value == null) {
            return true;
        }
        if (JsonNode.class.isAssignableFrom(value.getClass())) {
            JsonNode jsonValue = (JsonNode) value;
            return jsonValue.toString().length() >= minLength;
        }
        return false;
    }

    @Override
    public byte[] serialize(Object value, ValueFields valueFields) {
        if (value == null) {
            return null;
        }
        JsonNode valueNode = (JsonNode) value;
        try {
            return valueNode.toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new FlowableException("Error getting bytes from json variable", e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes, ValueFields valueFields) {
        JsonNode valueNode = null;
        try {
            valueNode = objectMapper.readTree(bytes);
        } catch (Exception e) {
            throw new FlowableException("Error reading json variable", e);
        }
        return valueNode;
    }
}
