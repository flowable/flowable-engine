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
public interface FlowableJsonNodeTests extends TestJsonProvider {

    @Test
    default void stringNode() {
        FlowableJsonNode test = create("test");
        assertThat(test.isString()).isTrue();
        assertThat(test.asString()).isEqualTo("test");
        assertThat(test.isValueNode()).isTrue();
        assertThat(test.getNodeType()).isEqualTo("STRING");

        assertThat(test.isNull()).isFalse();
        assertThat(test.isLong()).isFalse();
        assertThat(test.isDouble()).isFalse();
        assertThat(test.isInt()).isFalse();
        assertThat(test.isBoolean()).isFalse();
        assertThat(test.isNumber()).isFalse();
        assertThat(test.isMissingNode()).isFalse();
        assertThat(test.isContainer()).isFalse();
        assertThat(test.size()).isZero();
        assertThat(test.path(0).isMissingNode()).isTrue();
        assertThat(test.path("dummy").isMissingNode()).isTrue();
        assertThat(test.get(0)).isNull();
        assertThat(test.get("dummy")).isNull();
        assertThat(test.propertyNames()).isEmpty();
        assertThat(test.has("dummy")).isFalse();
    }

    @Test
    default void emptyStringNode() {
        FlowableJsonNode test = create("");
        assertThat(test.isString()).isTrue();
        assertThat(test.asString()).isEqualTo("");
        assertThat(test.isValueNode()).isTrue();
        assertThat(test.isMissingNode()).isFalse();
        assertThat(test.size()).isZero();
        assertThat(test.asString("default")).isEqualTo("");
    }

    @Test
    default void nullStringNode() {
        FlowableJsonNode test = create((String) null);
        assertThat(test.isString()).isFalse();
        assertThat(test.asString()).isEqualTo("null");
        assertThat(test.asString("default")).isEqualTo("default");
        assertThat(test.isNull()).isTrue();
        assertThat(test.isValueNode()).isTrue();
        assertThat(test.getNodeType()).isEqualTo("NULL");

        assertThat(test.isMissingNode()).isFalse();
        assertThat(test.size()).isZero();
    }

    @Test
    default void longNode() {
        FlowableJsonNode test = create(42L);
        assertThat(test.isNumber()).isTrue();
        assertThat(test.isLong()).isTrue();
        assertThat(test.isValueNode()).isTrue();
        assertThat(test.asString()).isEqualTo("42");
        assertThat(test.numberValue())
                .isEqualTo(42L)
                .isInstanceOf(Long.class);
        assertThat(test.getNodeType()).isEqualTo("NUMBER");

        assertThat(test.isString()).isFalse();
        assertThat(test.isDouble()).isFalse();
        assertThat(test.isInt()).isFalse();
        assertThat(test.isBoolean()).isFalse();
        assertThat(test.isNull()).isFalse();
        assertThat(test.isMissingNode()).isFalse();
        assertThat(test.isContainer()).isFalse();
        assertThat(test.size()).isZero();
        assertThat(test.path(0).isMissingNode()).isTrue();
        assertThat(test.path("dummy").isMissingNode()).isTrue();
        assertThat(test.get(0)).isNull();
        assertThat(test.get("dummy")).isNull();
        assertThat(test.propertyNames()).isEmpty();
        assertThat(test.has("dummy")).isFalse();
    }

    @Test
    default void nullLongNode() {
        FlowableJsonNode test = create((Long) null);
        assertThat(test.isNumber()).isFalse();
        assertThat(test.asString()).isEqualTo("null");
        assertThat(test.isNull()).isTrue();
        assertThat(test.isValueNode()).isTrue();
        assertThat(test.isMissingNode()).isFalse();
    }

    @Test
    default void doubleNode() {
        FlowableJsonNode test = create(65.4D);
        assertThat(test.isNumber()).isTrue();
        assertThat(test.isDouble()).isTrue();
        assertThat(test.isValueNode()).isTrue();
        assertThat(test.asString()).isEqualTo("65.4");
        assertThat(test.numberValue())
                .isEqualTo(65.4d)
                .isInstanceOf(Double.class);
        assertThat(test.getNodeType()).isEqualTo("NUMBER");

        assertThat(test.isString()).isFalse();
        assertThat(test.isLong()).isFalse();
        assertThat(test.isInt()).isFalse();
        assertThat(test.isBoolean()).isFalse();
        assertThat(test.isNull()).isFalse();
        assertThat(test.isMissingNode()).isFalse();
        assertThat(test.isContainer()).isFalse();
        assertThat(test.size()).isZero();
        assertThat(test.path(0).isMissingNode()).isTrue();
        assertThat(test.path("dummy").isMissingNode()).isTrue();
        assertThat(test.get(0)).isNull();
        assertThat(test.get("dummy")).isNull();
        assertThat(test.propertyNames()).isEmpty();
        assertThat(test.has("dummy")).isFalse();
    }

    @Test
    default void nullDoubleNode() {
        FlowableJsonNode test = create((Double) null);
        assertThat(test.isNumber()).isFalse();
        assertThat(test.asString()).isEqualTo("null");
        assertThat(test.isNull()).isTrue();
        assertThat(test.isValueNode()).isTrue();
        assertThat(test.isMissingNode()).isFalse();
    }

    @Test
    default void intNode() {
        FlowableJsonNode test = create(12);
        assertThat(test.isNumber()).isTrue();
        assertThat(test.isInt()).isTrue();
        assertThat(test.isValueNode()).isTrue();
        assertThat(test.asString()).isEqualTo("12");
        assertThat(test.numberValue())
                .isEqualTo(12)
                .isInstanceOf(Integer.class);
        assertThat(test.getNodeType()).isEqualTo("NUMBER");

        assertThat(test.isString()).isFalse();
        assertThat(test.isLong()).isFalse();
        assertThat(test.isDouble()).isFalse();
        assertThat(test.isBoolean()).isFalse();
        assertThat(test.isNull()).isFalse();
        assertThat(test.isMissingNode()).isFalse();
        assertThat(test.isContainer()).isFalse();
        assertThat(test.size()).isZero();
        assertThat(test.path(0).isMissingNode()).isTrue();
        assertThat(test.path("dummy").isMissingNode()).isTrue();
        assertThat(test.get(0)).isNull();
        assertThat(test.get("dummy")).isNull();
        assertThat(test.propertyNames()).isEmpty();
        assertThat(test.has("dummy")).isFalse();
    }

    @Test
    default void nullIntNode() {
        FlowableJsonNode test = create((Integer) null);
        assertThat(test.isNumber()).isFalse();
        assertThat(test.asString()).isEqualTo("null");
        assertThat(test.isNull()).isTrue();
        assertThat(test.isValueNode()).isTrue();
        assertThat(test.isMissingNode()).isFalse();
    }

    @Test
    default void booleanNode() {
        FlowableJsonNode test = create(true);
        assertThat(test.isBoolean()).isTrue();
        assertThat(test.isValueNode()).isTrue();
        assertThat(test.asString()).isEqualTo("true");
        assertThat(test.getNodeType()).isEqualTo("BOOLEAN");

        assertThat(test.isString()).isFalse();
        assertThat(test.isLong()).isFalse();
        assertThat(test.isDouble()).isFalse();
        assertThat(test.isInt()).isFalse();
        assertThat(test.isNull()).isFalse();
        assertThat(test.isMissingNode()).isFalse();
        assertThat(test.isContainer()).isFalse();
        assertThat(test.size()).isZero();
        assertThat(test.path(0).isMissingNode()).isTrue();
        assertThat(test.path("dummy").isMissingNode()).isTrue();
        assertThat(test.get(0)).isNull();
        assertThat(test.get("dummy")).isNull();
        assertThat(test.propertyNames()).isEmpty();
        assertThat(test.has("dummy")).isFalse();
    }

    @Test
    default void nullBooleanNode() {
        FlowableJsonNode test = create((Boolean) null);
        assertThat(test.isBoolean()).isFalse();
        assertThat(test.asString()).isEqualTo("null");
        assertThat(test.isNull()).isTrue();
        assertThat(test.isValueNode()).isTrue();
        assertThat(test.isMissingNode()).isFalse();
    }

    @Test
    default void nullNode() {
        FlowableJsonNode test = createNull();
        assertThat(test.asString()).isEqualTo("null");
        assertThat(test.isNull()).isTrue();
        assertThat(test.isValueNode()).isTrue();

        assertThat(test.isString()).isFalse();
        assertThat(test.isLong()).isFalse();
        assertThat(test.isDouble()).isFalse();
        assertThat(test.isInt()).isFalse();
        assertThat(test.isBoolean()).isFalse();
        assertThat(test.isNumber()).isFalse();
        assertThat(test.isMissingNode()).isFalse();
        assertThat(test.isContainer()).isFalse();
        assertThat(test.size()).isZero();
        assertThat(test.path(0).isMissingNode()).isTrue();
        assertThat(test.path("dummy").isMissingNode()).isTrue();
        assertThat(test.get(0)).isNull();
        assertThat(test.get("dummy")).isNull();
        assertThat(test.propertyNames()).isEmpty();
        assertThat(test.has("dummy")).isFalse();
    }

    @Test
    default void missingNode() {
        FlowableJsonNode test = createMissing();
        assertThat(test.asString()).isEqualTo("");
        assertThat(test.isMissingNode()).isTrue();
        assertThat(test.getNodeType()).isEqualTo("MISSING");

        assertThat(test.isNull()).isFalse();
        assertThat(test.isValueNode()).isFalse();
        assertThat(test.isString()).isFalse();
        assertThat(test.isLong()).isFalse();
        assertThat(test.isDouble()).isFalse();
        assertThat(test.isInt()).isFalse();
        assertThat(test.isBoolean()).isFalse();
        assertThat(test.isNumber()).isFalse();
        assertThat(test.size()).isZero();
        assertThat(test.path(0).isMissingNode()).isTrue();
        assertThat(test.path("dummy").isMissingNode()).isTrue();
        assertThat(test.get(0)).isNull();
        assertThat(test.get("dummy")).isNull();
        assertThat(test.propertyNames()).isEmpty();
        assertThat(test.has("dummy")).isFalse();
    }

    @Test
    default void wrapNullNode() {
        FlowableJsonNode test = wrapNull();
        assertThat(test).isNull();
    }

    @Test
    default void longValueExtraction() {
        FlowableJsonNode test = create(100L);
        assertThat(test.longValue()).isEqualTo(100L);
        assertThat(test.intValue()).isEqualTo(100);
        assertThat(test.doubleValue()).isEqualTo(100.0);
    }

    @Test
    default void intValueExtraction() {
        FlowableJsonNode test = create(42);
        assertThat(test.intValue()).isEqualTo(42);
        assertThat(test.longValue()).isEqualTo(42L);
        assertThat(test.doubleValue()).isEqualTo(42.0);
    }

    @Test
    default void doubleValueExtraction() {
        FlowableJsonNode test = create(3.14);
        assertThat(test.isDouble()).isTrue();
        assertThat(test.isDouble()).isTrue();
        assertThat(test.doubleValue()).isEqualTo(3.14);
        assertThat(test.intValue()).isEqualTo(3);
        assertThat(test.longValue()).isEqualTo(3L);
    }

    @Test
    default void booleanValueExtraction() {
        FlowableJsonNode trueNode = create(true);
        assertThat(trueNode.booleanValue()).isTrue();

        FlowableJsonNode falseNode = create(false);
        assertThat(falseNode.booleanValue()).isFalse();
    }

    @Test
    default void shortNode() {
        FlowableJsonNode test = create((short) 5);
        assertThat(test.isShort()).isTrue();
        assertThat(test.isNumber()).isTrue();
        assertThat(test.isValueNode()).isTrue();
        assertThat(test.intValue()).isEqualTo(5);
        assertThat(test.asString()).isEqualTo("5");
        assertThat(test.numberValue())
                .isEqualTo((short) 5)
                .isInstanceOf(Short.class);
        assertThat(test.getNodeType()).isEqualTo("NUMBER");
        assertThat(test.size()).isZero();
        assertThat(test.path(0).isMissingNode()).isTrue();
        assertThat(test.path("dummy").isMissingNode()).isTrue();
        assertThat(test.get(0)).isNull();
        assertThat(test.get("dummy")).isNull();
        assertThat(test.propertyNames()).isEmpty();
        assertThat(test.has("dummy")).isFalse();
    }

    @Test
    default void floatNode() {
        FlowableJsonNode test = create(2.5f);
        assertThat(test.isFloat()).isTrue();
        assertThat(test.isNumber()).isTrue();
        assertThat(test.isValueNode()).isTrue();
        assertThat(test.doubleValue()).isEqualTo(2.5);
        assertThat(test.asString()).isEqualTo("2.5");
        assertThat(test.numberValue())
                .isEqualTo((float) 2.5)
                .isInstanceOf(Float.class);
        assertThat(test.getNodeType()).isEqualTo("NUMBER");
        assertThat(test.size()).isZero();
        assertThat(test.path(0).isMissingNode()).isTrue();
        assertThat(test.path("dummy").isMissingNode()).isTrue();
        assertThat(test.get(0)).isNull();
        assertThat(test.get("dummy")).isNull();
        assertThat(test.propertyNames()).isEmpty();
        assertThat(test.has("dummy")).isFalse();
    }

    @Test
    default void bigDecimalNode() {
        FlowableJsonNode test = create(BigDecimal.valueOf(123.456));
        assertThat(test.isBigDecimal()).isTrue();
        assertThat(test.isNumber()).isTrue();
        assertThat(test.isValueNode()).isTrue();
        assertThat(test.asString()).isEqualTo("123.456");
        assertThat(test.numberValue())
                .isEqualTo(BigDecimal.valueOf(123.456))
                .isInstanceOf(BigDecimal.class);
        assertThat(test.getNodeType()).isEqualTo("NUMBER");
        assertThat(test.size()).isZero();
        assertThat(test.path(0).isMissingNode()).isTrue();
        assertThat(test.path("dummy").isMissingNode()).isTrue();
        assertThat(test.get(0)).isNull();
        assertThat(test.get("dummy")).isNull();
        assertThat(test.propertyNames()).isEmpty();
        assertThat(test.has("dummy")).isFalse();
    }

    @Test
    default void bigIntegerNode() {
        FlowableJsonNode test = create(BigInteger.valueOf(420));
        assertThat(test.isBigInteger()).isTrue();
        assertThat(test.isNumber()).isTrue();
        assertThat(test.isValueNode()).isTrue();
        assertThat(test.asString()).isEqualTo("420");
        assertThat(test.numberValue())
                .isEqualTo(BigInteger.valueOf(420))
                .isInstanceOf(BigInteger.class);
        assertThat(test.intValue()).isEqualTo(420);
        assertThat(test.longValue()).isEqualTo(420);
        assertThat(test.getNodeType()).isEqualTo("NUMBER");
        assertThat(test.size()).isZero();
        assertThat(test.path(0).isMissingNode()).isTrue();
        assertThat(test.path("dummy").isMissingNode()).isTrue();
        assertThat(test.get(0)).isNull();
        assertThat(test.get("dummy")).isNull();
        assertThat(test.propertyNames()).isEmpty();
        assertThat(test.has("dummy")).isFalse();
    }
}
