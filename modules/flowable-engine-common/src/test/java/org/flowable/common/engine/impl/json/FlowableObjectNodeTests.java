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

import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
public interface FlowableObjectNodeTests extends TestJsonProvider {

    @Test
    default void objectNode() {
        FlowableJsonNode test = createObjectNode("""
                {
                  "name": "John",
                  "age": 30,
                  "active": true
                }
                """);

        assertThat(test.isValueNode()).isFalse();
        assertThat(test.isContainer()).isTrue();
        assertThat(test.getNodeType()).isEqualTo("OBJECT");
        assertThat(test.propertyNames()).containsExactlyInAnyOrder("name", "age", "active");
    }

    @Test
    default void objectNodeGetProperty() {
        FlowableJsonNode test = createObjectNode("""
                {
                  "name": "John",
                  "age": 30,
                  "active": true
                }
                """);

        assertThat(test.isValueNode()).isFalse();
        assertThat(test.size()).isEqualTo(3);

        FlowableJsonNode name = test.get("name");
        assertThat(name).isNotNull();
        assertThat(name.isString()).isTrue();
        assertThat(name.asString()).isEqualTo("John");

        FlowableJsonNode age = test.get("age");
        assertThat(age).isNotNull();
        assertThat(age.isInt()).isTrue();
        assertThat(age.intValue()).isEqualTo(30);

        FlowableJsonNode active = test.get("active");
        assertThat(active).isNotNull();
        assertThat(active.isBoolean()).isTrue();
        assertThat(active.booleanValue()).isTrue();
    }

    @Test
    default void objectNodeGetNonExistentProperty() {
        FlowableJsonNode test = createObjectNode("""
                { "name": "John" }
                """);

        FlowableJsonNode missing = test.get("nonExistent");
        assertThat(missing).isNull();
    }

    @Test
    default void nestedObjectInObject() {
        FlowableJsonNode test = createObjectNode("""
                {
                  "person": {
                    "name": "John",
                    "age":30
                  }
                }
                """);

        FlowableJsonNode person = test.get("person");
        assertThat(person).isNotNull();
        assertThat(person.isValueNode()).isFalse();
        assertThat(person.size()).isEqualTo(2);

        FlowableJsonNode name = person.get("name");
        assertThat(name).isNotNull();
        assertThat(name.asString()).isEqualTo("John");

        FlowableJsonNode age = person.get("age");
        assertThat(age).isNotNull();
        assertThat(age.intValue()).isEqualTo(30);
    }

    @Test
    default void nestedArrayInObject() {
        FlowableJsonNode test = createObjectNode("""
                {
                  "numbers": [1, 2, 3]
                }
                """);

        FlowableJsonNode numbers = test.get("numbers");
        assertThat(numbers).isNotNull();
        assertThat(numbers.isValueNode()).isFalse();
        assertThat(numbers.size()).isEqualTo(3);

        FlowableJsonNode first = numbers.get(0);
        assertThat(first).isNotNull();
        assertThat(first.intValue()).isEqualTo(1);
    }

    @Test
    default void emptyObjectSize() {
        FlowableJsonNode test = createObjectNode("{}");
        assertThat(test.size()).isEqualTo(0);
        assertThat(test.isValueNode()).isFalse();
    }

    @Test
    default void objectWithNullProperty() {
        FlowableJsonNode test = createObjectNode("""
                {
                  "name": "John",
                  "middleName":null
                }
                """);

        FlowableJsonNode middleName = test.get("middleName");
        assertThat(middleName).isNotNull();
        assertThat(middleName.isNull()).isTrue();
        assertThat(middleName.asString()).isEqualTo("null");
    }

    @Test
    default void putStringValue() {
        FlowableObjectNode test = createObjectNode("""
                {
                  "name": "John",
                  "age":30
                }
                """);

        test.put("name", "Jane");
        test.put("city", "New York");

        FlowableJsonNode name = test.get("name");
        assertThat(name).isNotNull();
        assertThat(name.isString()).isTrue();
        assertThat(name.asString()).isEqualTo("Jane");

        FlowableJsonNode city = test.get("city");
        assertThat(city).isNotNull();
        assertThat(city.isString()).isTrue();
        assertThat(city.asString()).isEqualTo("New York");

        // Verify other properties unchanged
        assertThat(test.get("age").intValue()).isEqualTo(30);
    }

    @Test
    default void putBooleanValue() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        test.put("active", true);
        test.put("verified", false);

        FlowableJsonNode active = test.get("active");
        assertThat(active).isNotNull();
        assertThat(active.isBoolean()).isTrue();
        assertThat(active.booleanValue()).isTrue();

        FlowableJsonNode verified = test.get("verified");
        assertThat(verified).isNotNull();
        assertThat(verified.isBoolean()).isTrue();
        assertThat(verified.booleanValue()).isFalse();
    }

    @Test
    default void putIntegerValue() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        test.put("age", 30);
        test.put("count", 42);

        FlowableJsonNode age = test.get("age");
        assertThat(age).isNotNull();
        assertThat(age.isInt()).isTrue();
        assertThat(age.intValue()).isEqualTo(30);

        FlowableJsonNode count = test.get("count");
        assertThat(count).isNotNull();
        assertThat(count.isInt()).isTrue();
        assertThat(count.intValue()).isEqualTo(42);
    }

    @Test
    default void putShortValue() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        test.put("value", (short) 100);

        FlowableJsonNode value = test.get("value");
        assertThat(value).isNotNull();
        assertThat(value.isShort()).isTrue();
        assertThat(value.intValue()).isEqualTo(100);
    }

    @Test
    default void putLongValue() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        test.put("timestamp", 9223372036854775807L);

        FlowableJsonNode timestamp = test.get("timestamp");
        assertThat(timestamp).isNotNull();
        assertThat(timestamp.isLong()).isTrue();
        assertThat(timestamp.longValue()).isEqualTo(9223372036854775807L);
    }

    @Test
    default void putDoubleValue() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        test.put("price", 99.99);
        test.put("pi", 3.14159);

        FlowableJsonNode price = test.get("price");
        assertThat(price).isNotNull();
        assertThat(price.isDouble()).isTrue();
        assertThat(price.doubleValue()).isEqualTo(99.99);

        FlowableJsonNode pi = test.get("pi");
        assertThat(pi).isNotNull();
        assertThat(pi.isDouble()).isTrue();
        assertThat(pi.doubleValue()).isEqualTo(3.14159);
    }

    @Test
    default void putFloatValue() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        test.put("price", (float) 99.99);
        test.put("pi", (float) 3.14159);

        FlowableJsonNode price = test.get("price");
        assertThat(price).isNotNull();
        assertThat(price.isDouble()).isFalse();
        assertThat(price.isFloat()).isTrue();

        FlowableJsonNode pi = test.get("pi");
        assertThat(pi).isNotNull();
        assertThat(pi.isDouble()).isFalse();
        assertThat(pi.isFloat()).isTrue();

    }

    @Test
    default void putBigDecimalValue() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        BigDecimal bigDecimal = new BigDecimal("123456789.987654321");
        test.put("amount", bigDecimal);

        FlowableJsonNode amount = test.get("amount");
        assertThat(amount).isNotNull();
        assertThat(amount.isBigDecimal()).isTrue();
        assertThat(amount.asString()).isEqualTo("123456789.987654321");
    }

    @Test
    default void putBigIntegerValue() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        BigInteger bigInteger = new BigInteger("12345678901234567890");
        test.put("bigNumber", bigInteger);

        FlowableJsonNode bigNumber = test.get("bigNumber");
        assertThat(bigNumber).isNotNull();
        assertThat(bigNumber.isNumber()).isTrue();
        assertThat(bigNumber.asString()).isEqualTo("12345678901234567890");
    }

    @Test
    default void putByteArrayValue() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        byte[] data = new byte[] { 1, 2, 3, 4, 5 };
        test.put("data", data);

        FlowableJsonNode dataNode = test.get("data");
        assertThat(dataNode).isNotNull();
        // Byte arrays are typically encoded as base64 strings in JSON
        assertThat(dataNode).isNotNull();
    }

    @Test
    default void putNullValue() {
        FlowableObjectNode test = createObjectNode("""
                {
                  "name": "John",
                  "age": 30
                }
                """);

        test.putNull("middleName");
        test.putNull("age");

        FlowableJsonNode middleName = test.get("middleName");
        assertThat(middleName).isNotNull();
        assertThat(middleName.isNull()).isTrue();
        assertThat(middleName.asString()).isEqualTo("null");

        FlowableJsonNode age = test.get("age");
        assertThat(age).isNotNull();
        assertThat(age.isNull()).isTrue();

        // Verify other properties unchanged
        assertThat(test.get("name").asString()).isEqualTo("John");
    }

    @Test
    default void putNullStringValue() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        test.put("middleName", (String) null);

        FlowableJsonNode middleName = test.get("middleName");
        assertThat(middleName).isNotNull();
        assertThat(middleName.isNull()).isTrue();
    }

    @Test
    default void putNullBooleanValue() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        test.put("active", (Boolean) null);

        FlowableJsonNode active = test.get("active");
        assertThat(active).isNotNull();
        assertThat(active.isNull()).isTrue();
    }

    @Test
    default void putNullIntegerValue() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        test.put("age", (Integer) null);

        FlowableJsonNode age = test.get("age");
        assertThat(age).isNotNull();
        assertThat(age.isNull()).isTrue();
    }

    @Test
    default void putNullShortValue() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        test.put("value", (Short) null);

        FlowableJsonNode value = test.get("value");
        assertThat(value).isNotNull();
        assertThat(value.isNull()).isTrue();
    }

    @Test
    default void putNullLongValue() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        test.put("timestamp", (Long) null);

        FlowableJsonNode timestamp = test.get("timestamp");
        assertThat(timestamp).isNotNull();
        assertThat(timestamp.isNull()).isTrue();
    }

    @Test
    default void putNullDoubleValue() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        test.put("price", (Double) null);

        FlowableJsonNode price = test.get("price");
        assertThat(price).isNotNull();
        assertThat(price.isNull()).isTrue();
    }

    @Test
    default void putNullBigDecimalValue() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        test.put("amount", (BigDecimal) null);

        FlowableJsonNode amount = test.get("amount");
        assertThat(amount).isNotNull();
        assertThat(amount.isNull()).isTrue();
    }

    @Test
    default void putNullBigIntegerValue() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        test.put("bigNumber", (BigInteger) null);

        FlowableJsonNode bigNumber = test.get("bigNumber");
        assertThat(bigNumber).isNotNull();
        assertThat(bigNumber.isNull()).isTrue();
    }

    @Test
    default void setJsonNodeOnObjectWithString() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        test.set("city", create("Boston"));

        FlowableJsonNode city = test.get("city");
        assertThat(city).isNotNull();
        assertThat(city.isString()).isTrue();
        assertThat(city.asString()).isEqualTo("Boston");
    }

    @Test
    default void setJsonNodeOnObjectWithOtherJacksonNode() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        test.set("address", createOtherTypeJson("""
                {
                  "city": "Boston"
                }
                """));

        FlowableJsonNode address = test.get("address");
        assertThat(address).isNotNull();
        FlowableJsonNode city = address.get("city");
        assertThat(city.isString()).isTrue();
        assertThat(city.asString()).isEqualTo("Boston");
    }

    @Test
    default void setJsonNodeOnObjectWithNumber() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        test.set("count", create(999));

        FlowableJsonNode count = test.get("count");
        assertThat(count).isNotNull();
        assertThat(count.isInt()).isTrue();
        assertThat(count.intValue()).isEqualTo(999);
    }

    @Test
    default void setJsonNodeOnObjectWithNullNode() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        test.set("middleName", createNull());

        FlowableJsonNode middleName = test.get("middleName");
        assertThat(middleName).isNotNull();
        assertThat(middleName.isNull()).isTrue();
    }

    @Test
    default void setJsonNodeOnObjectWithNull() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        test.set("middleName", null);

        assertThat(test.size()).isEqualTo(2);
        FlowableJsonNode node = test.get("middleName");
        assertThat(node).isNotNull();
        assertThat(node.isNull()).isTrue();
    }

    @Test
    default void setJsonNodeWithObject() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        FlowableObjectNode address = createObjectNode("""
                {
                  "city": "Boston",
                  "zip": "02101"
                }
                """);
        test.set("address", address);

        FlowableJsonNode addressNode = test.get("address");
        assertThat(addressNode).isNotNull();
        assertThat(addressNode.isValueNode()).isFalse();
        assertThat(addressNode.get("city").asString()).isEqualTo("Boston");
        assertThat(addressNode.get("zip").asString()).isEqualTo("02101");
    }

    @Test
    default void setJsonNodeWithArray() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        FlowableArrayNode tags = createArrayNode("[\"developer\",\"java\",\"flowable\"]");
        test.set("tags", tags);

        FlowableJsonNode tagsNode = test.get("tags");
        assertThat(tagsNode).isNotNull();
        assertThat(tagsNode.isValueNode()).isFalse();
        assertThat(tagsNode.size()).isEqualTo(3);
        assertThat(tagsNode.get(0).asString()).isEqualTo("developer");
    }

    @Test
    default void putArray() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        assertThat(test.propertyNames()).containsExactlyInAnyOrder("name");
        FlowableArrayNode tags = test.putArray("tags");
        assertThat(tags.size()).isZero();
        assertThat(test.propertyNames()).containsExactlyInAnyOrder("name", "tags");

        tags.add("developer");
        tags.add("java");
        tags.add("flowable");

        FlowableJsonNode tagsNode = test.get("tags");
        assertThat(tagsNode).isNotNull();
        assertThat(tagsNode.isValueNode()).isFalse();
        assertThat(tagsNode.size()).isEqualTo(3);
        assertThat(tagsNode.get(0).asString()).isEqualTo("developer");
        assertThat(tagsNode.get(1).asString()).isEqualTo("java");
        assertThat(tagsNode.get(2).asString()).isEqualTo("flowable");
    }

    @Test
    default void putObjectCreatesNestedObject() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        FlowableObjectNode address = test.putObject("address");
        assertThat(address).isNotNull();

        address.put("city", "Boston");
        address.put("zip", "02101");

        FlowableJsonNode addressNode = test.get("address");
        assertThat(addressNode).isNotNull();
        assertThat(addressNode.isValueNode()).isFalse();
        assertThat(addressNode.get("city").asString()).isEqualTo("Boston");
        assertThat(addressNode.get("zip").asString()).isEqualTo("02101");
    }

    @Test
    default void putMultipleTypesInObject() {
        FlowableObjectNode test = createObjectNode("{}");

        test.put("name", "John");
        test.put("age", 30);
        test.put("active", true);
        test.put("salary", 50000.50);
        test.putNull("middleName");

        assertThat(test.get("name").asString()).isEqualTo("John");
        assertThat(test.get("age").intValue()).isEqualTo(30);
        assertThat(test.get("active").booleanValue()).isTrue();
        assertThat(test.get("salary").doubleValue()).isEqualTo(50000.50);
        assertThat(test.get("middleName").isNull()).isTrue();
    }

    @Test
    default void overwritePropertyWithDifferentType() {
        FlowableObjectNode test = createObjectNode("""
                { "value": "text" }
                """);

        FlowableJsonNode original = test.get("value");
        assertThat(original.isString()).isTrue();
        assertThat(original.asString()).isEqualTo("text");

        test.put("value", 42);

        FlowableJsonNode modified = test.get("value");
        assertThat(modified.isInt()).isTrue();
        assertThat(modified.intValue()).isEqualTo(42);
    }

    @Test
    default void overwritePropertyWithSameType() {
        FlowableObjectNode test = createObjectNode("""
                { "name": "John" }
                """);

        test.put("name", "Jane");
        test.put("name", "Bob");

        FlowableJsonNode name = test.get("name");
        assertThat(name.asString()).isEqualTo("Bob");
    }

    @Test
    default void buildObjectFromScratch() {
        FlowableObjectNode test = createObjectNode("{}");

        assertThat(test.size()).isEqualTo(0);

        test.put("firstName", "John");
        test.put("lastName", "Doe");
        test.put("age", 30);
        test.put("active", true);

        assertThat(test.size()).isEqualTo(4);
        assertThat(test.get("firstName").asString()).isEqualTo("John");
        assertThat(test.get("lastName").asString()).isEqualTo("Doe");
        assertThat(test.get("age").intValue()).isEqualTo(30);
        assertThat(test.get("active").booleanValue()).isTrue();
    }

    @Test
    default void createDeeplyNestedObjects() {
        FlowableObjectNode test = createObjectNode("{}");

        FlowableObjectNode person = test.putObject("person");
        person.put("name", "John");

        FlowableObjectNode address = person.putObject("address");
        address.put("street", "123 Main St");

        FlowableObjectNode location = address.putObject("location");
        location.put("city", "Boston");
        location.put("state", "MA");

        FlowableJsonNode city = test.get("person").get("address").get("location").get("city");
        assertThat(city).isNotNull();
        assertThat(city.asString()).isEqualTo("Boston");
    }

    @Test
    default void modifyObjectPreservesOtherProperties() {
        FlowableObjectNode test = createObjectNode("""
                {
                  "name": "John",
                  "age": 30,
                  "city": "Boston"
                }
                """);

        int originalSize = test.size();

        test.put("age", 31);

        assertThat(test.size()).isEqualTo(originalSize);
        assertThat(test.get("name").asString()).isEqualTo("John");
        assertThat(test.get("age").intValue()).isEqualTo(31);
        assertThat(test.get("city").asString()).isEqualTo("Boston");
    }

}
