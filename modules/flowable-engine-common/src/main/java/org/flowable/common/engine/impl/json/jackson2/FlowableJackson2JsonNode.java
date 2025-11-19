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

import java.util.Collection;
import java.util.List;
import java.util.Spliterators;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.json.FlowableJsonNode;
import org.flowable.common.engine.impl.util.JsonUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
public class FlowableJackson2JsonNode<T extends JsonNode> implements FlowableJsonNode {

    protected final T jsonNode;

    protected FlowableJackson2JsonNode(T jsonNode) {
        this.jsonNode = jsonNode;
    }

    @Override
    public T getImplementationValue() {
        return jsonNode;
    }

    @Override
    public String asString() {
        return jsonNode.asText();
    }

    @Override
    public String asString(String defaultValue) {
        return jsonNode.asText(defaultValue);
    }

    @Override
    public boolean isValueNode() {
        return jsonNode.isValueNode();
    }

    @Override
    public boolean isNull() {
        return jsonNode.isNull();
    }

    @Override
    public boolean isMissingNode() {
        return jsonNode.isMissingNode();
    }

    @Override
    public boolean isContainer() {
        return jsonNode.isContainerNode();
    }

    @Override
    public boolean isString() {
        return jsonNode.isTextual();
    }

    @Override
    public boolean isLong() {
        return jsonNode.isLong();
    }

    @Override
    public boolean isDouble() {
        return jsonNode.isDouble();
    }

    @Override
    public boolean isFloat() {
        return jsonNode.isFloat();
    }

    @Override
    public boolean isInt() {
        return jsonNode.isInt();
    }

    @Override
    public boolean isShort() {
        return jsonNode.isShort();
    }

    @Override
    public boolean isBoolean() {
        return jsonNode.isBoolean();
    }

    @Override
    public boolean isNumber() {
        return jsonNode.isNumber();
    }

    @Override
    public boolean isBigDecimal() {
        return jsonNode.isBigDecimal();
    }

    @Override
    public boolean isBigInteger() {
        return jsonNode.isBigInteger();
    }

    @Override
    public long longValue() {
        return jsonNode.longValue();
    }

    @Override
    public double doubleValue() {
        return jsonNode.doubleValue();
    }

    @Override
    public int intValue() {
        return jsonNode.intValue();
    }

    @Override
    public boolean booleanValue() {
        return jsonNode.booleanValue();
    }

    @Override
    public Number numberValue() {
        return jsonNode.numberValue();
    }

    @Override
    public boolean has(String propertyName) {
        return jsonNode.has(propertyName);
    }

    @Override
    public FlowableJsonNode get(String propertyName) {
        return wrap(jsonNode.get(propertyName));
    }

    @Override
    public FlowableJsonNode get(int index) {
        return wrap(jsonNode.get(index));
    }

    @Override
    public FlowableJsonNode path(int index) {
        return wrap(jsonNode.path(index));
    }

    @Override
    public FlowableJsonNode path(String propertyName) {
        return wrap(jsonNode.path(propertyName));
    }

    @Override
    public int size() {
        return jsonNode.size();
    }

    @Override
    public Collection<String> propertyNames() {
        return StreamSupport.stream(Spliterators.spliterator(jsonNode.fieldNames(), jsonNode.size(), 0), false).collect(Collectors.toSet());
    }

    @Override
    public String getNodeType() {
        return jsonNode.getNodeType().name();
    }

    protected static JsonNode asJsonNode(FlowableJsonNode value) {
        if (value == null) {
            return NullNode.getInstance();
        }
        return asJsonNode(value.getImplementationValue(), ObjectMapper::new);
    }

    protected static JsonNode asJsonNode(Object value, Supplier<ObjectMapper> objectMapperSupplier) {
        if (value instanceof JsonNode) {
            return (JsonNode) value;
        } else if (JsonUtil.isJsonNode(value)) {
            // This means it is a Jackson 3 node
            try {
                return objectMapperSupplier.get().readTree(value.toString());
            } catch (JsonProcessingException e) {
                throw new FlowableException("Failed to parse jase", e);
            }
        } else {
            throw new FlowableException("Unsupported value type " + (value == null ? "null" : value.getClass().getName()));
        }
    }

    public static FlowableJsonNode wrap(JsonNode jsonNode) {
        if (jsonNode instanceof ArrayNode arrayNode) {
            return new FlowableJackson2ArrayNode(arrayNode);
        } else if (jsonNode instanceof ObjectNode objectNode) {
            return new FlowableJackson2ObjectNode(objectNode);
        } else if (jsonNode != null) {
            return new FlowableJackson2JsonNode<>(jsonNode);
        }
        return null;
    }
}
