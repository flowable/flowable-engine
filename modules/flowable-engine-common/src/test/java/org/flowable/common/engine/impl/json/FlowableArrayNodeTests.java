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

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
public interface FlowableArrayNodeTests extends TestJsonProvider {

    @Test
    default void arrayNode() {
        FlowableJsonNode test = createArrayNode("""
                [ "first", "second", "third" ]
                """);

        assertThat(test.isValueNode()).isFalse();
        assertThat(test.isContainer()).isTrue();
        assertThat(test.getNodeType()).isEqualTo("ARRAY");
        assertThat(test.propertyNames()).isEmpty();
    }

    @Test
    default void arrayNodeGetByIndex() {
        FlowableJsonNode test = createArrayNode("""
                [ "first", "second", "third" ]
                """);

        assertThat(test.isValueNode()).isFalse();
        assertThat(test.size()).isEqualTo(3);

        FlowableJsonNode first = test.get(0);
        assertThat(first).isNotNull();
        assertThat(first.isString()).isTrue();
        assertThat(first.asString()).isEqualTo("first");

        FlowableJsonNode second = test.get(1);
        assertThat(second).isNotNull();
        assertThat(second.asString()).isEqualTo("second");

        FlowableJsonNode third = test.get(2);
        assertThat(third).isNotNull();
        assertThat(third.asString()).isEqualTo("third");
    }

    @Test
    default void arrayNodeGetOutOfBounds() {
        FlowableJsonNode test = createArrayNode("""
                [ "first", "second" ]
                """);

        FlowableJsonNode missing = test.get(5);
        assertThat(missing).isNull();
    }

    @Test
    default void arrayNodePath() {
        FlowableJsonNode test = createArrayNode("""
                [ "first", "second" ]
                """);

        // Valid index
        FlowableJsonNode first = test.path(0);
        assertThat(first).isNotNull();
        assertThat(first.asString()).isEqualTo("first");

        // Out of bounds - path returns MissingNode instead of null
        FlowableJsonNode missing = test.path(5);
        assertThat(missing).isNotNull();
        assertThat(missing.isValueNode()).isFalse();
        assertThat(missing.asString()).isEqualTo("");
    }

    @Test
    default void arrayNodeWithNumbers() {
        FlowableJsonNode test = createArrayNode("[ 10, 20, 30 ]");

        assertThat(test.size()).isEqualTo(3);

        FlowableJsonNode first = test.get(0);
        assertThat(first).isNotNull();
        assertThat(first.isInt()).isTrue();
        assertThat(first.intValue()).isEqualTo(10);

        FlowableJsonNode second = test.get(1);
        assertThat(second.intValue()).isEqualTo(20);

        FlowableJsonNode third = test.get(2);
        assertThat(third.intValue()).isEqualTo(30);
    }

    @Test
    default void nestedObjectInArray() {
        FlowableJsonNode test = createArrayNode("""
                [
                  { "name": "John" },
                  { "name": "Jane" }
                ]
                """);

        assertThat(test.size()).isEqualTo(2);

        FlowableJsonNode first = test.get(0);
        assertThat(first).isNotNull();
        assertThat(first.isValueNode()).isFalse();

        FlowableJsonNode firstName = first.get("name");
        assertThat(firstName).isNotNull();
        assertThat(firstName.asString()).isEqualTo("John");

        FlowableJsonNode second = test.get(1);
        FlowableJsonNode secondName = second.get("name");
        assertThat(secondName.asString()).isEqualTo("Jane");
    }

    @Test
    default void emptyArraySize() {
        FlowableJsonNode test = createArrayNode("[]");
        assertThat(test.size()).isEqualTo(0);
        assertThat(test.isValueNode()).isFalse();

    }

    @Test
    default void arrayWithNullElement() {
        FlowableJsonNode test = createArrayNode("""
                [ "first", null, "third" ]
                """);

        FlowableJsonNode nullElement = test.get(1);
        assertThat(nullElement).isNotNull();
        assertThat(nullElement.isNull()).isTrue();
        assertThat(nullElement.asString()).isEqualTo("null");
    }

    @Test
    default void setStringValue() {
        FlowableArrayNode test = createArrayNode("""
                [ "first", "second", "third" ]
                """);

        test.set(1, "modified");

        FlowableJsonNode modified = test.get(1);
        assertThat(modified).isNotNull();
        assertThat(modified.isString()).isTrue();
        assertThat(modified.asString()).isEqualTo("modified");

        // Verify other elements unchanged
        assertThat(test.get(0).asString()).isEqualTo("first");
        assertThat(test.get(2).asString()).isEqualTo("third");
    }

    @Test
    default void setBooleanValue() {
        FlowableArrayNode test = createArrayNode("""
                [ "first", "second", "third" ]
                """);

        test.set(0, true);
        test.set(2, false);

        FlowableJsonNode first = test.get(0);
        assertThat(first).isNotNull();
        assertThat(first.isBoolean()).isTrue();
        assertThat(first.booleanValue()).isTrue();

        FlowableJsonNode third = test.get(2);
        assertThat(third).isNotNull();
        assertThat(third.isBoolean()).isTrue();
        assertThat(third.booleanValue()).isFalse();
    }

    @Test
    default void setIntegerValue() {
        FlowableArrayNode test = createArrayNode("[ 1, 2, 3 ]");

        test.set(1, 42);

        FlowableJsonNode modified = test.get(1);
        assertThat(modified).isNotNull();
        assertThat(modified.isInt()).isTrue();
        assertThat(modified.intValue()).isEqualTo(42);
    }

    @Test
    default void setShortValue() {
        FlowableArrayNode test = createArrayNode("[ 1, 2, 3 ]");

        test.set(0, (short) 100);

        FlowableJsonNode modified = test.get(0);
        assertThat(modified).isNotNull();
        assertThat(modified.isShort()).isTrue();
        assertThat(modified.intValue()).isEqualTo(100);
    }

    @Test
    default void setLongValue() {
        FlowableArrayNode test = createArrayNode("[ 1, 2, 3 ]");

        test.set(2, 9223372036854775807L);

        FlowableJsonNode modified = test.get(2);
        assertThat(modified).isNotNull();
        assertThat(modified.isLong()).isTrue();
        assertThat(modified.longValue()).isEqualTo(9223372036854775807L);
    }

    @Test
    default void setDoubleValue() {
        FlowableArrayNode test = createArrayNode("[ 1.0, 2.0, 3.0 ]");

        test.set(1, 3.14159);

        FlowableJsonNode modified = test.get(1);
        assertThat(modified).isNotNull();
        assertThat(modified.isDouble()).isTrue();
        assertThat(modified.doubleValue()).isEqualTo(3.14159);
    }

    @Test
    default void setBigDecimalValue() {
        FlowableArrayNode test = createArrayNode("[ 1, 2, 3 ]");

        BigDecimal bigDecimal = new BigDecimal("123456789.987654321");
        test.set(0, bigDecimal);

        FlowableJsonNode modified = test.get(0);
        assertThat(modified).isNotNull();
        assertThat(modified.isBigDecimal()).isTrue();
        assertThat(modified.asString()).isEqualTo("123456789.987654321");
    }

    @Test
    default void setBigIntegerValue() {
        FlowableArrayNode test = createArrayNode("[ 1, 2, 3 ]");

        BigInteger bigInteger = new BigInteger("12345678901234567890");
        test.set(1, bigInteger);

        FlowableJsonNode modified = test.get(1);
        assertThat(modified).isNotNull();
        assertThat(modified.isNumber()).isTrue();
        assertThat(modified.asString()).isEqualTo("12345678901234567890");
    }

    @Test
    default void setNullValue() {
        FlowableArrayNode test = createArrayNode("""
                [ "first", "second", "third" ]
                """);

        test.setNull(1);

        FlowableJsonNode modified = test.get(1);
        assertThat(modified).isNotNull();
        assertThat(modified.isNull()).isTrue();
        assertThat(modified.asString()).isEqualTo("null");

        // Verify other elements unchanged
        assertThat(test.get(0).asString()).isEqualTo("first");
        assertThat(test.get(2).asString()).isEqualTo("third");
    }

    @Test
    default void setNullValueOnNullBoolean() {
        FlowableArrayNode test = createArrayNode("""
                [ "first", "second", "third" ]
                """);

        test.set(1, (Boolean) null);

        FlowableJsonNode modified = test.get(1);
        assertThat(modified).isNotNull();
        assertThat(modified.isNull()).isTrue();
    }

    @Test
    default void setNullValueOnNullInteger() {
        FlowableArrayNode test = createArrayNode("[ 1, 2, 3 ]");

        test.set(0, (Integer) null);

        FlowableJsonNode modified = test.get(0);
        assertThat(modified).isNotNull();
        assertThat(modified.isNull()).isTrue();
    }

    @Test
    default void setJsonNodeWithString() {
        FlowableArrayNode test = createArrayNode("[ 1, 2, 3 ]");

        test.set(1, create("text value"));

        FlowableJsonNode modified = test.get(1);
        assertThat(modified).isNotNull();
        assertThat(modified.asString()).isEqualTo("text value");
    }

    @Test
    default void setJsonNodeWithNumber() {
        FlowableArrayNode test = createArrayNode("""
                [ "a", "b", "c" ]
                """);

        test.set(0, create(999));

        FlowableJsonNode modified = test.get(0);
        assertThat(modified).isNotNull();
        assertThat(modified.isInt()).isTrue();
        assertThat(modified.intValue()).isEqualTo(999);
    }

    @Test
    default void setJsonNodeWithNull() {
        FlowableArrayNode test = createArrayNode("[ 1, 2, 3 ]");

        test.set(2, createNull());

        FlowableJsonNode modified = test.get(2);
        assertThat(modified).isNotNull();
        assertThat(modified.isNull()).isTrue();
    }

    @Test
    default void addJsonNodeToArray() {
        FlowableArrayNode test = createArrayNode("[ 1, 2, 3 ]");

        assertThat(test.size()).isEqualTo(3);

        test.add(create("fourth"));
        assertThat(test.size()).isEqualTo(4);

        FlowableJsonNode added = test.get(3);
        assertThat(added).isNotNull();
        assertThat(added.isString()).isTrue();
        assertThat(added.asString()).isEqualTo("fourth");
    }

    @Test
    default void addOtherJsonNodeToArray() {
        FlowableArrayNode test = createArrayNode("[]");

        assertThat(test.size()).isEqualTo(0);

        test.add(createOtherTypeJson("""
                {
                  "name": "John"
                }
                """));
        assertThat(test.size()).isEqualTo(1);

        FlowableJsonNode added = test.get(0);
        assertThat(added).isNotNull();
        assertThat(added.get("name").asString()).isEqualTo("John");
    }

    @Test
    default void addNullToArray() {
        FlowableArrayNode test = createArrayNode("[ 1, 2, 3 ]");

        assertThat(test.size()).isEqualTo(3);
        test.add((FlowableJsonNode) null);
        assertThat(test.size()).isEqualTo(4);
        FlowableJsonNode element = test.get(3);
        assertThat(element).isNotNull();
        assertThat(element.isNull()).isTrue();
    }

    @Test
    default void addNull() {
        FlowableArrayNode test = createArrayNode("[ 1, 2, 3 ]");

        assertThat(test.size()).isEqualTo(3);
        test.addNull();
        assertThat(test.size()).isEqualTo(4);
        FlowableJsonNode element = test.get(3);
        assertThat(element).isNotNull();
        assertThat(element.isNull()).isTrue();
    }

    @Test
    default void addShort() {
        FlowableArrayNode test = createArrayNode("[ 1, 2, 3 ]");

        assertThat(test.size()).isEqualTo(3);
        test.add((short) 10);
        assertThat(test.size()).isEqualTo(4);
        FlowableJsonNode element = test.get(3);
        assertThat(element).isNotNull();
        assertThat(element.isShort()).isTrue();
    }

    @Test
    default void addInteger() {
        FlowableArrayNode test = createArrayNode("[ 1, 2, 3 ]");

        assertThat(test.size()).isEqualTo(3);
        test.add(10);
        assertThat(test.size()).isEqualTo(4);
        FlowableJsonNode element = test.get(3);
        assertThat(element).isNotNull();
        assertThat(element.isInt()).isTrue();
    }

    @Test
    default void addLong() {
        FlowableArrayNode test = createArrayNode("[ 1, 2, 3 ]");

        assertThat(test.size()).isEqualTo(3);
        test.add(10L);
        assertThat(test.size()).isEqualTo(4);
        FlowableJsonNode element = test.get(3);
        assertThat(element).isNotNull();
        assertThat(element.isLong()).isTrue();
    }

    @Test
    default void addFloat() {
        FlowableArrayNode test = createArrayNode("[ 1, 2, 3 ]");

        assertThat(test.size()).isEqualTo(3);
        test.add((float) 10.42);
        assertThat(test.size()).isEqualTo(4);
        FlowableJsonNode element = test.get(3);
        assertThat(element).isNotNull();
        assertThat(element.isFloat()).isTrue();
    }

    @Test
    default void addDouble() {
        FlowableArrayNode test = createArrayNode("[ 1, 2, 3 ]");

        assertThat(test.size()).isEqualTo(3);
        test.add(10.42d);
        assertThat(test.size()).isEqualTo(4);
        FlowableJsonNode element = test.get(3);
        assertThat(element).isNotNull();
        assertThat(element.isDouble()).isTrue();
    }

    @Test
    default void addString() {
        FlowableArrayNode test = createArrayNode("[ 1, 2, 3 ]");

        assertThat(test.size()).isEqualTo(3);
        test.add("test");
        assertThat(test.size()).isEqualTo(4);
        FlowableJsonNode element = test.get(3);
        assertThat(element).isNotNull();
        assertThat(element.isString()).isTrue();
    }

    @Test
    default void addBoolean() {
        FlowableArrayNode test = createArrayNode("[ 1, 2, 3 ]");

        assertThat(test.size()).isEqualTo(3);
        test.add(true);
        assertThat(test.size()).isEqualTo(4);
        FlowableJsonNode element = test.get(3);
        assertThat(element).isNotNull();
        assertThat(element.isBoolean()).isTrue();
    }

    @Test
    default void addBigInteger() {
        FlowableArrayNode test = createArrayNode("[ 1, 2, 3 ]");

        assertThat(test.size()).isEqualTo(3);
        test.add(BigInteger.valueOf(10045));
        assertThat(test.size()).isEqualTo(4);
        FlowableJsonNode element = test.get(3);
        assertThat(element).isNotNull();
        assertThat(element.isBigInteger()).isTrue();
    }

    @Test
    default void addBigDecimal() {
        FlowableArrayNode test = createArrayNode("[ 1, 2, 3 ]");

        assertThat(test.size()).isEqualTo(3);
        test.add(BigDecimal.valueOf(100.42));
        assertThat(test.size()).isEqualTo(4);
        FlowableJsonNode element = test.get(3);
        assertThat(element).isNotNull();
        assertThat(element.isBigDecimal()).isTrue();
    }

    @Test
    default void addMultipleJsonNodesToArray() {
        FlowableArrayNode test = createArrayNode("[]");

        assertThat(test.size()).isEqualTo(0);

        test.add(create("first"));
        test.add(create(42));
        test.add(create(true));
        test.add(createNull());

        assertThat(test.size()).isEqualTo(4);

        assertThat(test.get(0).asString()).isEqualTo("first");
        assertThat(test.get(1).intValue()).isEqualTo(42);
        assertThat(test.get(2).booleanValue()).isTrue();
        assertThat(test.get(3).isNull()).isTrue();
    }

    @Test
    default void setMultipleTypesInArray() {
        FlowableArrayNode test = createArrayNode("[ null, null, null, null, null ]");

        test.set(0, "string");
        test.set(1, 123);
        test.set(2, true);
        test.set(3, 45.67);
        test.setNull(4);

        assertThat(test.get(0).asString()).isEqualTo("string");
        assertThat(test.get(1).intValue()).isEqualTo(123);
        assertThat(test.get(2).booleanValue()).isTrue();
        assertThat(test.get(3).doubleValue()).isEqualTo(45.67);
        assertThat(test.get(4).isNull()).isTrue();
    }

    @Test
    default void modifyArrayPreservesSize() {
        FlowableArrayNode test = createArrayNode("[ 1, 2, 3, 4, 5 ]");

        int originalSize = test.size();

        test.set(0, 100);
        test.set(4, 500);

        assertThat(test.size()).isEqualTo(originalSize);
        assertThat(test.get(0).intValue()).isEqualTo(100);
        assertThat(test.get(4).intValue()).isEqualTo(500);
    }

    @Test
    default void iterateArray() {
        FlowableArrayNode test = createArrayNode("""
                [
                  "first",
                  "second"
                ]
                """);

        List<String> values = new ArrayList<>(test.size());
        for (FlowableJsonNode node : test) {
            values.add(node.asString());
        }

        assertThat(values).containsExactly("first", "second");
    }

    @Test
    default void removeFromArrayIterator() {
        FlowableArrayNode test = createArrayNode("""
                [
                  "first",
                  "second"
                ]
                """);

        Iterator<FlowableJsonNode> iterator = test.iterator();
        iterator.next();
        iterator.remove();
        assertThat(test.size()).isEqualTo(1);
        assertThat(test.get(0).asString()).isEqualTo("second");
    }

}
