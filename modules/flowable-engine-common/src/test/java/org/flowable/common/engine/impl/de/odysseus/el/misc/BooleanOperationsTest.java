/*
 * Copyright 2006-2009 Odysseus Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.common.engine.impl.de.odysseus.el.misc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

import org.flowable.common.engine.impl.javax.el.ELException;
import org.junit.jupiter.api.Test;

class BooleanOperationsTest {

    /**
     * Test enum type
     */
    enum Foo {BAR, BAZ}

    private final TypeConverter converter = TypeConverter.DEFAULT;

    /*
     * Test method for 'de.odysseus.el.lang.BooleanOperations.lt(Object, Object)'
     */
    @Test
    void testLt() {
        assertThat(BooleanOperations.lt(converter, Boolean.TRUE, Boolean.TRUE)).isFalse();
        assertThat(BooleanOperations.lt(converter, null, Boolean.TRUE)).isFalse();
        assertThat(BooleanOperations.lt(converter, Boolean.TRUE, null)).isFalse();
        assertThat(BooleanOperations.lt(converter, "1", new BigDecimal("2"))).isTrue();
        assertThat(BooleanOperations.lt(converter, new BigDecimal("1"), "1")).isFalse();
        assertThat(BooleanOperations.lt(converter, new BigDecimal("2"), "1")).isFalse();
        assertThat(BooleanOperations.lt(converter, "1", Float.valueOf("2"))).isTrue();
        assertThat(BooleanOperations.lt(converter, Float.valueOf("1"), "1")).isFalse();
        assertThat(BooleanOperations.lt(converter, Float.valueOf("2"), "1")).isFalse();
        assertThat(BooleanOperations.lt(converter, "1", Double.valueOf("2"))).isTrue();
        assertThat(BooleanOperations.lt(converter, Double.valueOf("1"), "1")).isFalse();
        assertThat(BooleanOperations.lt(converter, Double.valueOf("2"), "1")).isFalse();
        assertThat(BooleanOperations.lt(converter, "1", new BigInteger("2"))).isTrue();
        assertThat(BooleanOperations.lt(converter, new BigInteger("1"), "1")).isFalse();
        assertThat(BooleanOperations.lt(converter, new BigInteger("2"), "1")).isFalse();
        assertThat(BooleanOperations.lt(converter, "1", Byte.valueOf("2"))).isTrue();
        assertThat(BooleanOperations.lt(converter, Byte.valueOf("1"), "1")).isFalse();
        assertThat(BooleanOperations.lt(converter, Byte.valueOf("2"), "1")).isFalse();
        assertThat(BooleanOperations.lt(converter, "1", Short.valueOf("2"))).isTrue();
        assertThat(BooleanOperations.lt(converter, Short.valueOf("1"), "1")).isFalse();
        assertThat(BooleanOperations.lt(converter, Short.valueOf("2"), "1")).isFalse();
        assertThat(BooleanOperations.lt(converter, 'a', 'b')).isTrue();
        assertThat(BooleanOperations.lt(converter, 'a', 'a')).isFalse();
        assertThat(BooleanOperations.lt(converter, 'b', 'a')).isFalse();
        assertThat(BooleanOperations.lt(converter, "1", Integer.valueOf("2"))).isTrue();
        assertThat(BooleanOperations.lt(converter, Integer.valueOf("1"), "1")).isFalse();
        assertThat(BooleanOperations.lt(converter, Integer.valueOf("2"), "1")).isFalse();
        assertThat(BooleanOperations.lt(converter, "1", Long.valueOf("2"))).isTrue();
        assertThat(BooleanOperations.lt(converter, Long.valueOf("1"), "1")).isFalse();
        assertThat(BooleanOperations.lt(converter, Long.valueOf("2"), "1")).isFalse();
        assertThat(BooleanOperations.lt(converter, "a", "b")).isTrue();
        assertThat(BooleanOperations.lt(converter, "a", "a")).isFalse();
        assertThat(BooleanOperations.lt(converter, "b", "a")).isFalse();
        assertThatThrownBy(() -> BooleanOperations.lt(converter, getClass(), 'a'))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> BooleanOperations.lt(converter, 'a', getClass()))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> BooleanOperations.lt(converter, getClass(), 0L))
                .isInstanceOf(Exception.class);
    }

    /*
     * Test method for 'de.odysseus.el.lang.BooleanOperations.gt(Object, Object)'
     */
    @Test
    void testGt() {
        assertThat(BooleanOperations.gt(converter, Boolean.TRUE, Boolean.TRUE)).isFalse();
        assertThat(BooleanOperations.gt(converter, null, Boolean.TRUE)).isFalse();
        assertThat(BooleanOperations.gt(converter, Boolean.TRUE, null)).isFalse();
        assertThat(BooleanOperations.gt(converter, "1", new BigDecimal("2"))).isFalse();
        assertThat(BooleanOperations.gt(converter, new BigDecimal("1"), "1")).isFalse();
        assertThat(BooleanOperations.gt(converter, new BigDecimal("2"), "1")).isTrue();
        assertThat(BooleanOperations.gt(converter, "1", Float.valueOf("2"))).isFalse();
        assertThat(BooleanOperations.gt(converter, Float.valueOf("1"), "1")).isFalse();
        assertThat(BooleanOperations.gt(converter, Float.valueOf("2"), "1")).isTrue();
        assertThat(BooleanOperations.gt(converter, "1", Double.valueOf("2"))).isFalse();
        assertThat(BooleanOperations.gt(converter, Double.valueOf("1"), "1")).isFalse();
        assertThat(BooleanOperations.gt(converter, Double.valueOf("2"), "1")).isTrue();
        assertThat(BooleanOperations.gt(converter, "1", new BigInteger("2"))).isFalse();
        assertThat(BooleanOperations.gt(converter, new BigInteger("1"), "1")).isFalse();
        assertThat(BooleanOperations.gt(converter, new BigInteger("2"), "1")).isTrue();
        assertThat(BooleanOperations.gt(converter, "1", Byte.valueOf("2"))).isFalse();
        assertThat(BooleanOperations.gt(converter, Byte.valueOf("1"), "1")).isFalse();
        assertThat(BooleanOperations.gt(converter, Byte.valueOf("2"), "1")).isTrue();
        assertThat(BooleanOperations.gt(converter, "1", Short.valueOf("2"))).isFalse();
        assertThat(BooleanOperations.gt(converter, Short.valueOf("1"), "1")).isFalse();
        assertThat(BooleanOperations.gt(converter, Short.valueOf("2"), "1")).isTrue();
        assertThat(BooleanOperations.gt(converter, 'a', 'b')).isFalse();
        assertThat(BooleanOperations.gt(converter, 'a', 'a')).isFalse();
        assertThat(BooleanOperations.gt(converter, 'b', 'a')).isTrue();
        assertThat(BooleanOperations.gt(converter, "1", Integer.valueOf("2"))).isFalse();
        assertThat(BooleanOperations.gt(converter, Integer.valueOf("1"), "1")).isFalse();
        assertThat(BooleanOperations.gt(converter, Integer.valueOf("2"), "1")).isTrue();
        assertThat(BooleanOperations.gt(converter, "1", Long.valueOf("2"))).isFalse();
        assertThat(BooleanOperations.gt(converter, Long.valueOf("1"), "1")).isFalse();
        assertThat(BooleanOperations.gt(converter, Long.valueOf("2"), "1")).isTrue();
        assertThat(BooleanOperations.gt(converter, "a", "b")).isFalse();
        assertThat(BooleanOperations.gt(converter, "a", "a")).isFalse();
        assertThat(BooleanOperations.gt(converter, "b", "a")).isTrue();
        assertThatThrownBy(() -> BooleanOperations.gt(converter, getClass(), 'a'))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> BooleanOperations.gt(converter, 'a', getClass()))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> BooleanOperations.gt(converter, getClass(), 0L))
                .isInstanceOf(Exception.class);
    }

    /*
     * Test method for 'de.odysseus.el.lang.BooleanOperations.ge(Object, Object)'
     */
    @Test
    void testGe() {
        assertThat(BooleanOperations.ge(converter, Boolean.TRUE, Boolean.TRUE)).isTrue();
        assertThat(BooleanOperations.ge(converter, null, Boolean.TRUE)).isFalse();
        assertThat(BooleanOperations.ge(converter, Boolean.TRUE, null)).isFalse();
        assertThat(BooleanOperations.ge(converter, "1", new BigDecimal("2"))).isFalse();
        assertThat(BooleanOperations.ge(converter, new BigDecimal("1"), "1")).isTrue();
        assertThat(BooleanOperations.ge(converter, new BigDecimal("2"), "1")).isTrue();
        assertThat(BooleanOperations.ge(converter, "1", Float.valueOf("2"))).isFalse();
        assertThat(BooleanOperations.ge(converter, Float.valueOf("1"), "1")).isTrue();
        assertThat(BooleanOperations.ge(converter, Float.valueOf("2"), "1")).isTrue();
        assertThat(BooleanOperations.ge(converter, "1", Double.valueOf("2"))).isFalse();
        assertThat(BooleanOperations.ge(converter, Double.valueOf("1"), "1")).isTrue();
        assertThat(BooleanOperations.ge(converter, Double.valueOf("2"), "1")).isTrue();
        assertThat(BooleanOperations.ge(converter, "1", new BigInteger("2"))).isFalse();
        assertThat(BooleanOperations.ge(converter, new BigInteger("1"), "1")).isTrue();
        assertThat(BooleanOperations.ge(converter, new BigInteger("2"), "1")).isTrue();
        assertThat(BooleanOperations.ge(converter, "1", Byte.valueOf("2"))).isFalse();
        assertThat(BooleanOperations.ge(converter, Byte.valueOf("1"), "1")).isTrue();
        assertThat(BooleanOperations.ge(converter, Byte.valueOf("2"), "1")).isTrue();
        assertThat(BooleanOperations.ge(converter, "1", Short.valueOf("2"))).isFalse();
        assertThat(BooleanOperations.ge(converter, Short.valueOf("1"), "1")).isTrue();
        assertThat(BooleanOperations.ge(converter, Short.valueOf("2"), "1")).isTrue();
        assertThat(BooleanOperations.ge(converter, 'a', 'b')).isFalse();
        assertThat(BooleanOperations.ge(converter, 'a', 'a')).isTrue();
        assertThat(BooleanOperations.ge(converter, 'b', 'a')).isTrue();
        assertThat(BooleanOperations.ge(converter, "1", Integer.valueOf("2"))).isFalse();
        assertThat(BooleanOperations.ge(converter, Integer.valueOf("1"), "1")).isTrue();
        assertThat(BooleanOperations.ge(converter, Integer.valueOf("2"), "1")).isTrue();
        assertThat(BooleanOperations.ge(converter, "1", Long.valueOf("2"))).isFalse();
        assertThat(BooleanOperations.ge(converter, Long.valueOf("1"), "1")).isTrue();
        assertThat(BooleanOperations.ge(converter, Long.valueOf("2"), "1")).isTrue();
        assertThat(BooleanOperations.ge(converter, "a", "b")).isFalse();
        assertThat(BooleanOperations.ge(converter, "a", "a")).isTrue();
        assertThat(BooleanOperations.ge(converter, "b", "a")).isTrue();
        assertThatThrownBy(() -> BooleanOperations.ge(converter, getClass(), 'a'))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> BooleanOperations.ge(converter, 'a', getClass()))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> BooleanOperations.ge(converter, getClass(), 0L))
                .isInstanceOf(Exception.class);
    }

    /*
     * Test method for 'de.odysseus.el.lang.BooleanOperations.le(Object, Object)'
     */
    @Test
    void testLe() {
        assertThat(BooleanOperations.le(converter, Boolean.TRUE, Boolean.TRUE)).isTrue();
        assertThat(BooleanOperations.le(converter, null, Boolean.TRUE)).isFalse();
        assertThat(BooleanOperations.le(converter, Boolean.TRUE, null)).isFalse();
        assertThat(BooleanOperations.le(converter, "1", new BigDecimal("2"))).isTrue();
        assertThat(BooleanOperations.le(converter, new BigDecimal("1"), "1")).isTrue();
        assertThat(BooleanOperations.le(converter, new BigDecimal("2"), "1")).isFalse();
        assertThat(BooleanOperations.le(converter, "1", Float.valueOf("2"))).isTrue();
        assertThat(BooleanOperations.le(converter, Float.valueOf("1"), "1")).isTrue();
        assertThat(BooleanOperations.le(converter, Float.valueOf("2"), "1")).isFalse();
        assertThat(BooleanOperations.le(converter, "1", Double.valueOf("2"))).isTrue();
        assertThat(BooleanOperations.le(converter, Double.valueOf("1"), "1")).isTrue();
        assertThat(BooleanOperations.le(converter, Double.valueOf("2"), "1")).isFalse();
        assertThat(BooleanOperations.le(converter, "1", new BigInteger("2"))).isTrue();
        assertThat(BooleanOperations.le(converter, new BigInteger("1"), "1")).isTrue();
        assertThat(BooleanOperations.le(converter, new BigInteger("2"), "1")).isFalse();
        assertThat(BooleanOperations.le(converter, "1", Byte.valueOf("2"))).isTrue();
        assertThat(BooleanOperations.le(converter, Byte.valueOf("1"), "1")).isTrue();
        assertThat(BooleanOperations.le(converter, Byte.valueOf("2"), "1")).isFalse();
        assertThat(BooleanOperations.le(converter, "1", Short.valueOf("2"))).isTrue();
        assertThat(BooleanOperations.le(converter, Short.valueOf("1"), "1")).isTrue();
        assertThat(BooleanOperations.le(converter, Short.valueOf("2"), "1")).isFalse();
        assertThat(BooleanOperations.le(converter, 'a', 'b')).isTrue();
        assertThat(BooleanOperations.le(converter, 'a', 'a')).isTrue();
        assertThat(BooleanOperations.le(converter, 'b', 'a')).isFalse();
        assertThat(BooleanOperations.le(converter, "1", Integer.valueOf("2"))).isTrue();
        assertThat(BooleanOperations.le(converter, Integer.valueOf("1"), "1")).isTrue();
        assertThat(BooleanOperations.le(converter, Integer.valueOf("2"), "1")).isFalse();
        assertThat(BooleanOperations.le(converter, "1", Long.valueOf("2"))).isTrue();
        assertThat(BooleanOperations.le(converter, Long.valueOf("1"), "1")).isTrue();
        assertThat(BooleanOperations.le(converter, Long.valueOf("2"), "1")).isFalse();
        assertThat(BooleanOperations.le(converter, "a", "b")).isTrue();
        assertThat(BooleanOperations.le(converter, "a", "a")).isTrue();
        assertThat(BooleanOperations.le(converter, "b", "a")).isFalse();
        assertThatThrownBy(() -> BooleanOperations.le(converter, getClass(), 'a'))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> BooleanOperations.le(converter, 'a', getClass()))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> BooleanOperations.le(converter, getClass(), 0L))
                .isInstanceOf(Exception.class);
    }

    /*
     * Test method for 'de.odysseus.el.lang.BooleanOperations.eq(Object, Object)'
     */
    @Test
    void testEq() {
        assertThat(BooleanOperations.eq(converter, Boolean.TRUE, Boolean.TRUE)).isTrue();
        assertThat(BooleanOperations.eq(converter, null, Boolean.TRUE)).isFalse();
        assertThat(BooleanOperations.eq(converter, Boolean.TRUE, null)).isFalse();
        assertThat(BooleanOperations.eq(converter, "1", new BigDecimal("2"))).isFalse();
        assertThat(BooleanOperations.eq(converter, new BigDecimal("1"), "1")).isTrue();
        assertThat(BooleanOperations.eq(converter, new BigDecimal("2"), "1")).isFalse();
        assertThat(BooleanOperations.eq(converter, "1", Float.valueOf("2"))).isFalse();
        assertThat(BooleanOperations.eq(converter, Float.valueOf("1"), "1")).isTrue();
        assertThat(BooleanOperations.eq(converter, Float.valueOf("2"), "1")).isFalse();
        assertThat(BooleanOperations.eq(converter, "1", Double.valueOf("2"))).isFalse();
        assertThat(BooleanOperations.eq(converter, Double.valueOf("1"), "1")).isTrue();
        assertThat(BooleanOperations.eq(converter, Double.valueOf("2"), "1")).isFalse();
        assertThat(BooleanOperations.eq(converter, "1", new BigInteger("2"))).isFalse();
        assertThat(BooleanOperations.eq(converter, new BigInteger("1"), "1")).isTrue();
        assertThat(BooleanOperations.eq(converter, new BigInteger("2"), "1")).isFalse();
        assertThat(BooleanOperations.eq(converter, "1", Byte.valueOf("2"))).isFalse();
        assertThat(BooleanOperations.eq(converter, Byte.valueOf("1"), "1")).isTrue();
        assertThat(BooleanOperations.eq(converter, Byte.valueOf("2"), "1")).isFalse();
        assertThat(BooleanOperations.eq(converter, "1", Short.valueOf("2"))).isFalse();
        assertThat(BooleanOperations.eq(converter, Short.valueOf("1"), "1")).isTrue();
        assertThat(BooleanOperations.eq(converter, Short.valueOf("2"), "1")).isFalse();
        assertThat(BooleanOperations.eq(converter, 'a', 'b')).isFalse();
        assertThat(BooleanOperations.eq(converter, 'a', 'a')).isTrue();
        assertThat(BooleanOperations.eq(converter, 'b', 'a')).isFalse();
        assertThat(BooleanOperations.eq(converter, "1", Integer.valueOf("2"))).isFalse();
        assertThat(BooleanOperations.eq(converter, Integer.valueOf("1"), "1")).isTrue();
        assertThat(BooleanOperations.eq(converter, Integer.valueOf("2"), "1")).isFalse();
        assertThat(BooleanOperations.eq(converter, "1", Long.valueOf("2"))).isFalse();
        assertThat(BooleanOperations.eq(converter, Long.valueOf("1"), "1")).isTrue();
        assertThat(BooleanOperations.eq(converter, Long.valueOf("2"), "1")).isFalse();
        assertThat(BooleanOperations.eq(converter, Boolean.FALSE, Boolean.TRUE)).isFalse();
        assertThat(BooleanOperations.eq(converter, Boolean.TRUE, Boolean.TRUE)).isTrue();
        assertThat(BooleanOperations.eq(converter, Boolean.FALSE, Boolean.FALSE)).isTrue();
        assertThat(BooleanOperations.eq(converter, Foo.BAR, "BAR")).isTrue();
        assertThat(BooleanOperations.eq(converter, "BAR", Foo.BAR)).isTrue();
        assertThat(BooleanOperations.eq(converter, Foo.BAR, "BAZ")).isFalse();
        assertThatThrownBy(() -> BooleanOperations.eq(converter, Foo.BAR, "FOO"))
                .isInstanceOf(ELException.class);

        assertThat(BooleanOperations.eq(converter, "a", "b")).isFalse();
        assertThat(BooleanOperations.eq(converter, "a", "a")).isTrue();
        assertThat(BooleanOperations.eq(converter, "b", "a")).isFalse();
        assertThat(BooleanOperations.eq(converter, getClass(), 'a')).isFalse();
        assertThat(BooleanOperations.eq(converter, 'a', getClass())).isFalse();
        assertThatThrownBy(() -> BooleanOperations.eq(converter, getClass(), 0L))
                .isInstanceOf(ELException.class);
    }

    /*
     * Test method for 'de.odysseus.el.lang.BooleanOperations.ne(Object, Object)'
     */
    @Test
    void testNe() {
        assertThat(BooleanOperations.ne(converter, Boolean.TRUE, Boolean.TRUE)).isFalse();
        assertThat(BooleanOperations.ne(converter, null, Boolean.TRUE)).isTrue();
        assertThat(BooleanOperations.ne(converter, Boolean.TRUE, null)).isTrue();
        assertThat(BooleanOperations.ne(converter, "1", new BigDecimal("2"))).isTrue();
        assertThat(BooleanOperations.ne(converter, new BigDecimal("1"), "1")).isFalse();
        assertThat(BooleanOperations.ne(converter, new BigDecimal("2"), "1")).isTrue();
        assertThat(BooleanOperations.ne(converter, "1", Float.valueOf("2"))).isTrue();
        assertThat(BooleanOperations.ne(converter, Float.valueOf("1"), "1")).isFalse();
        assertThat(BooleanOperations.ne(converter, Float.valueOf("2"), "1")).isTrue();
        assertThat(BooleanOperations.ne(converter, "1", Double.valueOf("2"))).isTrue();
        assertThat(BooleanOperations.ne(converter, Double.valueOf("1"), "1")).isFalse();
        assertThat(BooleanOperations.ne(converter, Double.valueOf("2"), "1")).isTrue();
        assertThat(BooleanOperations.ne(converter, "1", new BigInteger("2"))).isTrue();
        assertThat(BooleanOperations.ne(converter, new BigInteger("1"), "1")).isFalse();
        assertThat(BooleanOperations.ne(converter, new BigInteger("2"), "1")).isTrue();
        assertThat(BooleanOperations.ne(converter, "1", Byte.valueOf("2"))).isTrue();
        assertThat(BooleanOperations.ne(converter, Byte.valueOf("1"), "1")).isFalse();
        assertThat(BooleanOperations.ne(converter, Byte.valueOf("2"), "1")).isTrue();
        assertThat(BooleanOperations.ne(converter, "1", Short.valueOf("2"))).isTrue();
        assertThat(BooleanOperations.ne(converter, Short.valueOf("1"), "1")).isFalse();
        assertThat(BooleanOperations.ne(converter, Short.valueOf("2"), "1")).isTrue();
        assertThat(BooleanOperations.ne(converter, 'a', 'b')).isTrue();
        assertThat(BooleanOperations.ne(converter, 'a', 'a')).isFalse();
        assertThat(BooleanOperations.ne(converter, 'b', 'a')).isTrue();
        assertThat(BooleanOperations.ne(converter, "1", Integer.valueOf("2"))).isTrue();
        assertThat(BooleanOperations.ne(converter, Integer.valueOf("1"), "1")).isFalse();
        assertThat(BooleanOperations.ne(converter, Integer.valueOf("2"), "1")).isTrue();
        assertThat(BooleanOperations.ne(converter, "1", Long.valueOf("2"))).isTrue();
        assertThat(BooleanOperations.ne(converter, Long.valueOf("1"), "1")).isFalse();
        assertThat(BooleanOperations.ne(converter, Long.valueOf("2"), "1")).isTrue();
        assertThat(BooleanOperations.ne(converter, Boolean.FALSE, Boolean.TRUE)).isTrue();
        assertThat(BooleanOperations.ne(converter, Boolean.TRUE, Boolean.TRUE)).isFalse();
        assertThat(BooleanOperations.ne(converter, Boolean.FALSE, Boolean.FALSE)).isFalse();
        assertThat(BooleanOperations.ne(converter, Foo.BAR, "BAR")).isFalse();
        assertThat(BooleanOperations.ne(converter, "BAR", Foo.BAR)).isFalse();
        assertThat(BooleanOperations.ne(converter, Foo.BAR, "BAZ")).isTrue();
        assertThatThrownBy(() -> BooleanOperations.ne(converter, Foo.BAR, "FOO"))
                .isInstanceOf(ELException.class);
        assertThat(BooleanOperations.ne(converter, "a", "b")).isTrue();
        assertThat(BooleanOperations.ne(converter, "a", "a")).isFalse();
        assertThat(BooleanOperations.ne(converter, "b", "a")).isTrue();
        assertThat(BooleanOperations.ne(converter, getClass(), 'a')).isTrue();
        assertThat(BooleanOperations.ne(converter, 'a', getClass())).isTrue();
        assertThatThrownBy(() -> BooleanOperations.ne(converter, getClass(), 0L))
                .isInstanceOf(ELException.class);
    }

    /*
     * Test method for 'de.odysseus.el.lang.BooleanOperations.empty(Object)'
     */
    @Test
    void testEmpty() {
        assertThat(BooleanOperations.empty(converter, null)).isTrue();
        assertThat(BooleanOperations.empty(converter, "")).isTrue();
        assertThat(BooleanOperations.empty(converter, new Object[0])).isTrue();
        assertThat(BooleanOperations.empty(converter, new HashMap<>())).isTrue();
        assertThat(BooleanOperations.empty(converter, new ArrayList<>())).isTrue();
        assertThat(BooleanOperations.empty(converter, "foo")).isFalse();
    }
}
