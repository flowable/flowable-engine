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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
public interface VariableJsonMapperTests {

    VariableJsonMapper getVariableJsonMapper();

    Class<?> getImplementationJsonNodeClass();

    Object createOtherTypeJsonNode(String json);

    @Test
    default void readTreeFromString() {
        VariableJsonMapper mapper = getVariableJsonMapper();
        String json = """
                {
                  "name": "John",
                  "age": 30,
                  "active": true
                }
                """;

        Object jsonNode = mapper.readTree(json);

        assertThat(jsonNode).isNotNull();
        assertThat(jsonNode).isInstanceOf(getImplementationJsonNodeClass());
        assertThatJson(jsonNode)
                .isEqualTo("""
                        {
                          name: 'John',
                          age: 30,
                          active: true
                        }
                        """);
    }

    @Test
    default void readTreeFromStringWithArray() {
        VariableJsonMapper mapper = getVariableJsonMapper();
        String json = """
                [1, 2, 3, "test", true]
                """;

        Object jsonNode = mapper.readTree(json);

        assertThat(jsonNode).isNotNull();
        assertThat(jsonNode).isInstanceOf(getImplementationJsonNodeClass());
        assertThatJson(jsonNode)
                .isEqualTo("""
                        [
                          1, 2, 3, "test", true
                        ]
                        """);
    }

    @Test
    default void readTreeFromStringWithNull() {
        VariableJsonMapper mapper = getVariableJsonMapper();
        String json = "null";

        Object jsonNode = mapper.readTree(json);

        assertThat(jsonNode).isNotNull();
        assertThat(jsonNode).isInstanceOf(getImplementationJsonNodeClass());
    }

    @Test
    default void readTreeFromStringWithInvalidJson() {
        VariableJsonMapper mapper = getVariableJsonMapper();
        String json = "{ invalid json }";

        assertThatThrownBy(() -> mapper.readTree(json));
    }

    @Test
    default void readTreeFromBytes() {
        VariableJsonMapper mapper = getVariableJsonMapper();
        String json = """
                {
                  "name": "John",
                  "age": 30
                }
                """;
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        Object jsonNode = mapper.readTree(bytes);

        assertThat(jsonNode).isNotNull();
        assertThat(jsonNode).isInstanceOf(getImplementationJsonNodeClass());
        assertThatJson(jsonNode)
                .isEqualTo("""
                        {
                          name: 'John',
                          age: 30
                        }
                        """);
    }

    @Test
    default void readTreeFromBytesWithArray() {
        VariableJsonMapper mapper = getVariableJsonMapper();
        String json = """
                ["value1", "value2"]
                """;
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        Object jsonNode = mapper.readTree(bytes);

        assertThat(jsonNode).isNotNull();
        assertThat(jsonNode).isInstanceOf(getImplementationJsonNodeClass());
        assertThatJson(jsonNode)
                .isEqualTo("""
                        [
                          "value1",
                          "value2"
                        ]
                        """);
    }

    @Test
    default void readTreeFromBytesWithInvalidJson() {
        VariableJsonMapper mapper = getVariableJsonMapper();
        byte[] bytes = "{ invalid }".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> mapper.readTree(bytes));
    }

    @Test
    default void deepCopyObject() {
        VariableJsonMapper mapper = getVariableJsonMapper();
        String json = """
                {
                  "name": "John",
                  "age": 30
                }
                """;
        Object original = mapper.readTree(json);

        Object copy = mapper.deepCopy(original);

        assertThat(copy).isNotNull();
        assertThat(copy).isNotSameAs(original);
        assertThat(copy).isInstanceOf(getImplementationJsonNodeClass());
    }

    @Test
    default void deepCopyArray() {
        VariableJsonMapper mapper = getVariableJsonMapper();
        String json = """
                [1, 2, 3]
                """;
        Object original = mapper.readTree(json);

        Object copy = mapper.deepCopy(original);

        assertThat(copy).isNotNull();
        assertThat(copy).isNotSameAs(original);
        assertThat(copy).isInstanceOf(getImplementationJsonNodeClass());
    }

    @Test
    default void isJsonNodeWithJsonNode() {
        VariableJsonMapper mapper = getVariableJsonMapper();
        Object jsonNode = mapper.readTree("{}");

        boolean result = mapper.isJsonNode(jsonNode);

        assertThat(result).isTrue();
    }

    @Test
    default void isJsonNodeWithNonJsonNode() {
        VariableJsonMapper mapper = getVariableJsonMapper();

        boolean result = mapper.isJsonNode("not a json node");

        assertThat(result).isFalse();
    }

    @Test
    default void isJsonNodeWithNull() {
        VariableJsonMapper mapper = getVariableJsonMapper();

        boolean result = mapper.isJsonNode(null);

        assertThat(result).isFalse();
    }

    @Test
    default void createObjectNode() {
        VariableJsonMapper mapper = getVariableJsonMapper();

        FlowableObjectNode objectNode = mapper.createObjectNode();

        assertThat(objectNode).isNotNull();
        assertThat(objectNode.getImplementationValue()).isInstanceOf(getImplementationJsonNodeClass());
    }

    @Test
    default void createObjectNodeAndAddProperties() {
        VariableJsonMapper mapper = getVariableJsonMapper();

        FlowableObjectNode objectNode = mapper.createObjectNode();
        objectNode.put("stringProp", "value");
        objectNode.put("intProp", 42);
        objectNode.put("longProp", 123L);
        objectNode.put("doubleProp", 3.14);
        objectNode.put("boolProp", true);
        objectNode.put("shortProp", (short) 10);
        objectNode.put("bigDecimalProp", new BigDecimal("123.456"));
        objectNode.put("bigIntegerProp", new BigInteger("9876543210"));
        objectNode.putNull("nullProp");

        assertThatJson(objectNode.getImplementationValue())
                .isEqualTo("""
                        {
                          stringProp: 'value',
                          intProp: 42,
                          longProp: 123,
                          doubleProp: 3.14,
                          boolProp: true,
                          shortProp: 10,
                          bigDecimalProp: 123.456,
                          bigIntegerProp: 9876543210,
                          nullProp: null
                        }
                        """);
    }

    @Test
    default void createObjectNodeWithNestedObject() {
        VariableJsonMapper mapper = getVariableJsonMapper();

        FlowableObjectNode objectNode = mapper.createObjectNode();
        FlowableObjectNode nestedObject = objectNode.putObject("nested");
        nestedObject.put("name", "John");
        nestedObject.put("age", 30);

        assertThatJson(objectNode.getImplementationValue())
                .isEqualTo("""
                        {
                          nested: {
                            name: 'John',
                            age: 30
                          }
                        }
                        """);
    }

    @Test
    default void createArrayNode() {
        VariableJsonMapper mapper = getVariableJsonMapper();

        FlowableArrayNode arrayNode = mapper.createArrayNode();

        assertThat(arrayNode).isNotNull();
        assertThat(arrayNode.getImplementationValue()).isInstanceOf(getImplementationJsonNodeClass());
    }

    @Test
    default void readTreeAndDeepCopyComplexObject() {
        VariableJsonMapper mapper = getVariableJsonMapper();
        String json = """
                {
                  "person": {
                    "name": "John",
                    "age": 30,
                    "addresses": [
                      {
                        "street": "Main St",
                        "city": "New York"
                      },
                      {
                        "street": "Second St",
                        "city": "Boston"
                      }
                    ]
                  },
                  "active": true
                }
                """;
        Object original = mapper.readTree(json);

        Object copy = mapper.deepCopy(original);

        assertThat(copy).isNotNull();
        assertThat(copy).isNotSameAs(original);
        assertThat(mapper.isJsonNode(copy)).isTrue();
    }

    @Test
    default void transformToJsonNodeWhenUsingOtherTypeJson() {
        VariableJsonMapper mapper = getVariableJsonMapper();

        Object original = createOtherTypeJsonNode("""
                {
                  "person": {
                    "name": "John",
                    "age": 30
                  },
                  "active": true
                }
                """);
        assertThat(original).isNotInstanceOf(getImplementationJsonNodeClass());

        Object transformed = mapper.transformToJsonNode(original);

        assertThat(transformed).isInstanceOf(getImplementationJsonNodeClass());
        assertThat(transformed).isNotSameAs(original);
    }

    @Test
    default void transformToJsonNodeWhenUsingSameTypeJson() {
        VariableJsonMapper mapper = getVariableJsonMapper();

        Object original = mapper.readTree("""
                {
                  "person": {
                    "name": "John",
                    "age": 30
                  },
                  "active": true
                }
                """);

        Object transformed = mapper.transformToJsonNode(original);

        assertThat(transformed).isSameAs(original);
    }
}
