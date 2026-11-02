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

import java.util.Collection;
import java.util.function.Supplier;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.json.FlowableJsonNode;
import org.flowable.common.engine.impl.util.JsonUtil;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
public class FlowableJackson3JsonNode<T extends JsonNode> implements FlowableJsonNode {

    protected final T jsonNode;

    protected FlowableJackson3JsonNode(T jsonNode) {
        this.jsonNode = jsonNode;
    }

    @Override
    public Object getImplementationValue() {
        return jsonNode;
    }

    @Override
    public String asString() {
        if (jsonNode.isNull()) {
            // Jackson 2 was returning `null` as a string for `asString`
            // So we keep the same behavior here
            return "null";
        } else if (jsonNode.isMissingNode()) {
            // Jackson 2 was returning empty string for `asString`
            // So we keep the same behavior here
            return "";
        }
        return jsonNode.asString();
    }

    @Override
    public String asString(String defaultValue) {
        if (jsonNode.isNull() || jsonNode.isMissingNode()) {
            return defaultValue;
        }
        return jsonNode.asString(defaultValue);
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
        return jsonNode.isContainer();
    }

    @Override
    public boolean isString() {
        return jsonNode.isString();
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
        if (jsonNode.isFloatingPointNumber()) {
            // Jackson 3 throws a coercion exception if the value is not a long.
            // We just want to return the cut long value
            return jsonNode.numberValue().longValue();
        }
        return jsonNode.longValue();
    }

    @Override
    public double doubleValue() {
        return jsonNode.doubleValue();
    }

    @Override
    public int intValue() {
        if (jsonNode.isFloatingPointNumber()) {
            // Jackson 3 throws a coercion exception if the value is not an integer.
            // We just want to return the cut int value
            return jsonNode.numberValue().intValue();
        }
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
        return jsonNode.propertyNames();
    }

    @Override
    public String getNodeType() {
        return jsonNode.getNodeType().name();
    }

    protected static JsonNode asJsonNode(FlowableJsonNode value) {
        if (value == null) {
            return NullNode.getInstance();
        }
        return asJsonNode(value.getImplementationValue(), JsonMapper::shared);
    }

    protected static JsonNode asJsonNode(Object value, Supplier<ObjectMapper> objectMapperSupplier) {
        if (value instanceof JsonNode) {
            return (JsonNode) value;
        } else if (JsonUtil.isJsonNode(value)) {
            // This means it is a Jackson 2 node
            return objectMapperSupplier.get().readTree(value.toString());
        } else {
            throw new FlowableException("Unsupported value type " + (value == null ? "null" : value.getClass().getName()));
        }
    }

    public static FlowableJsonNode wrap(JsonNode jsonNode) {
        if (jsonNode instanceof ArrayNode arrayNode) {
            return new FlowableJackson3ArrayNode(arrayNode);
        } else if (jsonNode instanceof ObjectNode objectNode) {
            return new FlowableJackson3ObjectNode(objectNode);
        } else if (jsonNode != null) {
            return new FlowableJackson3JsonNode<>(jsonNode);
        }
        return null;
    }
}
