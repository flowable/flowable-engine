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

import java.math.BigDecimal;
import java.math.BigInteger;

import org.flowable.common.engine.impl.json.jackson2.FlowableJackson2JsonNode;
import org.flowable.common.engine.impl.json.jackson2.Jackson2VariableJsonMapper;
import org.flowable.common.engine.impl.json.jackson3.FlowableJackson3JsonNode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ShortNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * @author Filip Hrisafov
 */
public class FlowableJackson2Test implements FlowableJsonNodeTests, FlowableArrayNodeTests, FlowableObjectNodeTests, VariableJsonMapperTests {

    @Override
    public VariableJsonMapper getVariableJsonMapper() {
        return new Jackson2VariableJsonMapper(new ObjectMapper());
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
        return FlowableJackson2JsonNode.wrap(value != null ? TextNode.valueOf(value) : NullNode.getInstance());
    }

    @Override
    public FlowableJsonNode create(Long value) {
        return FlowableJackson2JsonNode.wrap(value != null ? LongNode.valueOf(value) : NullNode.getInstance());
    }

    @Override
    public FlowableJsonNode create(Integer value) {
        return FlowableJackson2JsonNode.wrap(value != null ? IntNode.valueOf(value) : NullNode.getInstance());
    }

    @Override
    public FlowableJsonNode create(Double value) {
        return FlowableJackson2JsonNode.wrap(value != null ? DoubleNode.valueOf(value) : NullNode.getInstance());
    }

    @Override
    public FlowableJsonNode create(Boolean value) {
        return FlowableJackson2JsonNode.wrap(value != null ? BooleanNode.valueOf(value) : NullNode.getInstance());
    }

    @Override
    public FlowableJsonNode create(Short value) {
        return FlowableJackson2JsonNode.wrap(value != null ? ShortNode.valueOf(value) : NullNode.getInstance());
    }

    @Override
    public FlowableJsonNode create(Float value) {
        return FlowableJackson2JsonNode.wrap(value != null ? FloatNode.valueOf(value) : NullNode.getInstance());
    }

    @Override
    public FlowableJsonNode create(BigDecimal value) {
        return FlowableJackson2JsonNode.wrap(value != null ? DecimalNode.valueOf(value) : NullNode.getInstance());
    }

    @Override
    public FlowableJsonNode create(BigInteger value) {
        return FlowableJackson2JsonNode.wrap(value != null ? BigIntegerNode.valueOf(value) : NullNode.getInstance());
    }

    @Override
    public FlowableJsonNode createNull() {
        return FlowableJackson2JsonNode.wrap(NullNode.getInstance());
    }

    @Override
    public FlowableJsonNode createMissing() {
        return FlowableJackson2JsonNode.wrap(MissingNode.getInstance());
    }

    @Override
    public FlowableJsonNode wrapNull() {
        return FlowableJackson2JsonNode.wrap(null);
    }

    @Override
    public FlowableObjectNode createObjectNode(String json) {
        try {
            JsonNode jsonNode = new ObjectMapper().readTree(json);
            return (FlowableObjectNode) FlowableJackson2JsonNode.wrap(jsonNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    @Override
    public FlowableArrayNode createArrayNode(String json) {
        try {
            JsonNode jsonNode = new ObjectMapper().readTree(json);
            return (FlowableArrayNode) FlowableJackson2JsonNode.wrap(jsonNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    @Override
    public FlowableJsonNode createOtherTypeJson(String json) {
        return FlowableJackson3JsonNode.wrap(tools.jackson.databind.json.JsonMapper.shared().readTree(json));
    }
}
