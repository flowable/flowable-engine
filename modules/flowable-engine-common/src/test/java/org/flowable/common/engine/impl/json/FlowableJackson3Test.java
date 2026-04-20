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
package org.flowable.common.engine.impl.json;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.flowable.common.engine.impl.json.jackson2.FlowableJackson2JsonNode;
import org.flowable.common.engine.impl.json.jackson3.FlowableJackson3JsonNode;
import org.flowable.common.engine.impl.json.jackson3.Jackson3VariableJsonMapper;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.BigIntegerNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.DecimalNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.FloatNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.LongNode;
import tools.jackson.databind.node.MissingNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ShortNode;
import tools.jackson.databind.node.StringNode;

/**
 * @author Filip Hrisafov
 */
public class FlowableJackson3Test implements FlowableJsonNodeTests, FlowableArrayNodeTests, FlowableObjectNodeTests, VariableJsonMapperTests {

    @Override
    public VariableJsonMapper getVariableJsonMapper() {
        return new Jackson3VariableJsonMapper(JsonMapper.shared());
    }

    @Override
    public Class<?> getImplementationJsonNodeClass() {
        return JsonNode.class;
    }

    @Override
    public Object createOtherTypeJsonNode(String json) {
        return createOtherTypeJson(json).getImplementationValue();
    }

    @Override
    public FlowableJsonNode create(String value) {
        return FlowableJackson3JsonNode.wrap(value != null ? StringNode.valueOf(value) : NullNode.getInstance());
    }

    @Override
    public FlowableJsonNode create(Long value) {
        return FlowableJackson3JsonNode.wrap(value != null ? LongNode.valueOf(value) : NullNode.getInstance());
    }

    @Override
    public FlowableJsonNode create(Integer value) {
        return FlowableJackson3JsonNode.wrap(value != null ? IntNode.valueOf(value) : NullNode.getInstance());
    }

    @Override
    public FlowableJsonNode create(Double value) {
        return FlowableJackson3JsonNode.wrap(value != null ? DoubleNode.valueOf(value) : NullNode.getInstance());
    }

    @Override
    public FlowableJsonNode create(Boolean value) {
        return FlowableJackson3JsonNode.wrap(value != null ? BooleanNode.valueOf(value) : NullNode.getInstance());
    }

    @Override
    public FlowableJsonNode create(Short value) {
        return FlowableJackson3JsonNode.wrap(value != null ? ShortNode.valueOf(value) : NullNode.getInstance());
    }

    @Override
    public FlowableJsonNode create(Float value) {
        return FlowableJackson3JsonNode.wrap(value != null ? FloatNode.valueOf(value) : NullNode.getInstance());
    }

    @Override
    public FlowableJsonNode create(BigDecimal value) {
        return FlowableJackson3JsonNode.wrap(value != null ? DecimalNode.valueOf(value) : NullNode.getInstance());
    }

    @Override
    public FlowableJsonNode create(BigInteger value) {
        return FlowableJackson3JsonNode.wrap(value != null ? BigIntegerNode.valueOf(value) : NullNode.getInstance());
    }

    @Override
    public FlowableJsonNode createNull() {
        return FlowableJackson3JsonNode.wrap(NullNode.getInstance());
    }

    @Override
    public FlowableJsonNode createMissing() {
        return FlowableJackson3JsonNode.wrap(MissingNode.getInstance());
    }

    @Override
    public FlowableJsonNode wrapNull() {
        return FlowableJackson3JsonNode.wrap(null);
    }

    @Override
    public FlowableObjectNode createObjectNode(String json) {
        JsonNode jsonNode = JsonMapper.shared().readTree(json);
        return (FlowableObjectNode) FlowableJackson3JsonNode.wrap(jsonNode);
    }

    @Override
    public FlowableArrayNode createArrayNode(String json) {
        JsonNode jsonNode = JsonMapper.shared().readTree(json);
        return (FlowableArrayNode) FlowableJackson3JsonNode.wrap(jsonNode);
    }

    @Override
    public FlowableJsonNode createOtherTypeJson(String json) {
        try {
            return FlowableJackson2JsonNode.wrap(new com.fasterxml.jackson.databind.ObjectMapper().readTree(json));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read json", e);
        }
    }
}
