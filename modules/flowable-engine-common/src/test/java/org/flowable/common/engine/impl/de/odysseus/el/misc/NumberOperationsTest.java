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

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

class NumberOperationsTest {

    private final TypeConverter converter = TypeConverter.DEFAULT;

    /*
     * Test method for 'de.odysseus.el.lang.NumberOperations.add(Object, Object)'
     */
    @Test
    void testAdd() {
        assertThat(NumberOperations.add(converter, null, null)).isEqualTo(0L);

        BigDecimal bd1 = new BigDecimal(1);
        Integer i1 = 1;
        Long l1 = 1L;
        Float f1 = 1F;
        Double d1 = 1.0;
        String e1 = "1e0";
        String s1 = "1";
        BigInteger bi1 = new BigInteger("1");

        Long l2 = 2L;
        BigDecimal bd2 = new BigDecimal(2);
        Double d2 = 2.0;
        BigInteger bi2 = new BigInteger("2");

        assertThat(NumberOperations.add(converter, l1, bd1)).isEqualTo(bd2);
        assertThat(NumberOperations.add(converter, bd1, l1)).isEqualTo(bd2);

        assertThat(NumberOperations.add(converter, f1, bi1)).isEqualTo(bd2);
        assertThat(NumberOperations.add(converter, bi1, f1)).isEqualTo(bd2);

        assertThat(NumberOperations.add(converter, f1, l1)).isEqualTo(d2);
        assertThat(NumberOperations.add(converter, l1, f1)).isEqualTo(d2);

        assertThat(NumberOperations.add(converter, d1, bi1)).isEqualTo(bd2);
        assertThat(NumberOperations.add(converter, bi1, d1)).isEqualTo(bd2);

        assertThat(NumberOperations.add(converter, d1, l1)).isEqualTo(d2);
        assertThat(NumberOperations.add(converter, l1, d1)).isEqualTo(d2);

        assertThat(NumberOperations.add(converter, e1, bi1)).isEqualTo(bd2);
        assertThat(NumberOperations.add(converter, bi1, e1)).isEqualTo(bd2);

        assertThat(NumberOperations.add(converter, e1, l1)).isEqualTo(d2);
        assertThat(NumberOperations.add(converter, l1, e1)).isEqualTo(d2);

        assertThat(NumberOperations.add(converter, l1, bi1)).isEqualTo(bi2);
        assertThat(NumberOperations.add(converter, bi1, l1)).isEqualTo(bi2);

        assertThat(NumberOperations.add(converter, i1, l1)).isEqualTo(l2);
        assertThat(NumberOperations.add(converter, l1, i1)).isEqualTo(l2);

        assertThat(NumberOperations.add(converter, i1, s1)).isEqualTo(l2);
        assertThat(NumberOperations.add(converter, s1, i1)).isEqualTo(l2);
    }

    /*
     * Test method for 'de.odysseus.el.lang.NumberOperations.sub(Object, Object)'
     */
    @Test
    void testSub() {
        assertThat(NumberOperations.sub(converter, null, null)).isEqualTo(0L);

        BigDecimal bd1 = new BigDecimal(1);
        Integer i1 = 1;
        Long l1 = 1L;
        Float f1 = 1F;
        Double d1 = 1.0;
        String e1 = "1e0";
        String s1 = "1";
        BigInteger bi1 = new BigInteger("1");

        Long l2 = 0L;
        BigDecimal bd2 = new BigDecimal(0);
        Double d2 = (double) 0;
        BigInteger bi2 = new BigInteger("0");

        assertThat(NumberOperations.sub(converter, l1, bd1)).isEqualTo(bd2);
        assertThat(NumberOperations.sub(converter, bd1, l1)).isEqualTo(bd2);

        assertThat(NumberOperations.sub(converter, f1, bi1)).isEqualTo(bd2);
        assertThat(NumberOperations.sub(converter, bi1, f1)).isEqualTo(bd2);

        assertThat(NumberOperations.sub(converter, f1, l1)).isEqualTo(d2);
        assertThat(NumberOperations.sub(converter, l1, f1)).isEqualTo(d2);

        assertThat(NumberOperations.sub(converter, d1, bi1)).isEqualTo(bd2);
        assertThat(NumberOperations.sub(converter, bi1, d1)).isEqualTo(bd2);

        assertThat(NumberOperations.sub(converter, d1, l1)).isEqualTo(d2);
        assertThat(NumberOperations.sub(converter, l1, d1)).isEqualTo(d2);

        assertThat(NumberOperations.sub(converter, e1, bi1)).isEqualTo(bd2);
        assertThat(NumberOperations.sub(converter, bi1, e1)).isEqualTo(bd2);

        assertThat(NumberOperations.sub(converter, e1, l1)).isEqualTo(d2);
        assertThat(NumberOperations.sub(converter, l1, e1)).isEqualTo(d2);

        assertThat(NumberOperations.sub(converter, l1, bi1)).isEqualTo(bi2);
        assertThat(NumberOperations.sub(converter, bi1, l1)).isEqualTo(bi2);

        assertThat(NumberOperations.sub(converter, i1, l1)).isEqualTo(l2);
        assertThat(NumberOperations.sub(converter, l1, i1)).isEqualTo(l2);

        assertThat(NumberOperations.sub(converter, i1, s1)).isEqualTo(l2);
        assertThat(NumberOperations.sub(converter, s1, i1)).isEqualTo(l2);
    }

    /*
     * Test method for 'de.odysseus.el.lang.NumberOperations.mul(Object, Object)'
     */
    @Test
    void testMul() {
        assertThat(NumberOperations.mul(converter, null, null)).isEqualTo(0L);

        BigDecimal bd1 = new BigDecimal(1);
        Integer i1 = 1;
        Long l1 = 1L;
        Float f1 = 1F;
        Double d1 = 1.0;
        String e1 = "1e0";
        String s1 = "1";
        BigInteger bi1 = new BigInteger("1");

        Long l2 = 1L;
        BigDecimal bd2 = new BigDecimal(1);
        Double d2 = 1.0;
        BigInteger bi2 = new BigInteger("1");

        assertThat(NumberOperations.mul(converter, l1, bd1)).isEqualTo(bd2);
        assertThat(NumberOperations.mul(converter, bd1, l1)).isEqualTo(bd2);

        assertThat(NumberOperations.mul(converter, f1, bi1)).isEqualTo(bd2);
        assertThat(NumberOperations.mul(converter, bi1, f1)).isEqualTo(bd2);

        assertThat(NumberOperations.mul(converter, f1, l1)).isEqualTo(d2);
        assertThat(NumberOperations.mul(converter, l1, f1)).isEqualTo(d2);

        assertThat(NumberOperations.mul(converter, d1, bi1)).isEqualTo(bd2);
        assertThat(NumberOperations.mul(converter, bi1, d1)).isEqualTo(bd2);

        assertThat(NumberOperations.mul(converter, d1, l1)).isEqualTo(d2);
        assertThat(NumberOperations.mul(converter, l1, d1)).isEqualTo(d2);

        assertThat(NumberOperations.mul(converter, e1, bi1)).isEqualTo(bd2);
        assertThat(NumberOperations.mul(converter, bi1, e1)).isEqualTo(bd2);

        assertThat(NumberOperations.mul(converter, e1, l1)).isEqualTo(d2);
        assertThat(NumberOperations.mul(converter, l1, e1)).isEqualTo(d2);

        assertThat(NumberOperations.mul(converter, l1, bi1)).isEqualTo(bi2);
        assertThat(NumberOperations.mul(converter, bi1, l1)).isEqualTo(bi2);

        assertThat(NumberOperations.mul(converter, i1, l1)).isEqualTo(l2);
        assertThat(NumberOperations.mul(converter, l1, i1)).isEqualTo(l2);

        assertThat(NumberOperations.mul(converter, i1, s1)).isEqualTo(l2);
        assertThat(NumberOperations.mul(converter, s1, i1)).isEqualTo(l2);
    }

    /*
     * Test method for 'de.odysseus.el.lang.NumberOperations.div(Object, Object)'
     */
    @Test
    void testDiv() {
        assertThat(NumberOperations.div(converter, null, null)).isEqualTo(0L);

        BigDecimal bd1 = new BigDecimal(1);
        Integer i1 = 1;
        Long l1 = 1L;
        Float f1 = 1F;
        Double d1 = 1.0;
        String e1 = "1e0";
        String s1 = "1";
        BigInteger bi1 = new BigInteger("1");

        BigDecimal bd2 = new BigDecimal(1);
        Double d2 = 1.0;

        assertThat(NumberOperations.div(converter, l1, bd1)).isEqualTo(bd2);
        assertThat(NumberOperations.div(converter, bd1, l1)).isEqualTo(bd2);

        assertThat(NumberOperations.div(converter, f1, bi1)).isEqualTo(bd2);
        assertThat(NumberOperations.div(converter, bi1, f1)).isEqualTo(bd2);

        assertThat(NumberOperations.div(converter, f1, l1)).isEqualTo(d2);
        assertThat(NumberOperations.div(converter, l1, f1)).isEqualTo(d2);

        assertThat(NumberOperations.div(converter, d1, l1)).isEqualTo(d2);
        assertThat(NumberOperations.div(converter, l1, d1)).isEqualTo(d2);

        assertThat(NumberOperations.div(converter, e1, l1)).isEqualTo(d2);
        assertThat(NumberOperations.div(converter, l1, e1)).isEqualTo(d2);

        assertThat(NumberOperations.div(converter, i1, l1)).isEqualTo(d2);
        assertThat(NumberOperations.div(converter, l1, i1)).isEqualTo(d2);

        assertThat(NumberOperations.div(converter, i1, s1)).isEqualTo(d2);
        assertThat(NumberOperations.div(converter, s1, i1)).isEqualTo(d2);
    }

    /*
     * Test method for 'de.odysseus.el.lang.NumberOperations.mod(Object, Object)'
     */
    @Test
    void testMod() {
        assertThat(NumberOperations.mod(converter, null, null)).isEqualTo(0L);

        BigDecimal bd1 = new BigDecimal(1);
        Integer i1 = 1;
        Long l1 = 1L;
        Float f1 = 1F;
        Double d1 = 1.0;
        String e1 = "1e0";
        String s1 = "1";
        BigInteger bi1 = new BigInteger("1");

        Long l2 = 0L;
        Double d2 = (double) 0;
        BigInteger bi2 = new BigInteger("0");

        assertThat(NumberOperations.mod(converter, l1, bd1)).isEqualTo(d2);
        assertThat(NumberOperations.mod(converter, bd1, l1)).isEqualTo(d2);

        assertThat(NumberOperations.mod(converter, f1, bi1)).isEqualTo(d2);
        assertThat(NumberOperations.mod(converter, bi1, f1)).isEqualTo(d2);

        assertThat(NumberOperations.mod(converter, f1, l1)).isEqualTo(d2);
        assertThat(NumberOperations.mod(converter, l1, f1)).isEqualTo(d2);

        assertThat(NumberOperations.mod(converter, d1, l1)).isEqualTo(d2);
        assertThat(NumberOperations.mod(converter, l1, d1)).isEqualTo(d2);

        assertThat(NumberOperations.mod(converter, d1, bi1)).isEqualTo(d2);
        assertThat(NumberOperations.mod(converter, bi1, d1)).isEqualTo(d2);

        assertThat(NumberOperations.mod(converter, e1, bi1)).isEqualTo(d2);
        assertThat(NumberOperations.mod(converter, bi1, e1)).isEqualTo(d2);

        assertThat(NumberOperations.mod(converter, e1, l1)).isEqualTo(d2);
        assertThat(NumberOperations.mod(converter, l1, e1)).isEqualTo(d2);

        assertThat(NumberOperations.mod(converter, l1, bi1)).isEqualTo(bi2);
        assertThat(NumberOperations.mod(converter, bi1, l1)).isEqualTo(bi2);

        assertThat(NumberOperations.mod(converter, i1, l1)).isEqualTo(l2);
        assertThat(NumberOperations.mod(converter, l1, i1)).isEqualTo(l2);

        assertThat(NumberOperations.mod(converter, i1, s1)).isEqualTo(l2);
        assertThat(NumberOperations.mod(converter, s1, i1)).isEqualTo(l2);
    }

    /*
     * Test method for 'de.odysseus.el.lang.NumberOperations.neg(Object)'
     */
    @Test
    void testNeg() {
        assertThat(NumberOperations.neg(converter, null)).isEqualTo(0L);

        BigDecimal bd1 = new BigDecimal(1);
        Integer i1 = 1;
        Long l1 = 1L;
        Float f1 = 1F;
        Double d1 = 1.0;
        String e1 = "1e0";
        String s1 = "1";
        BigInteger bi1 = new BigInteger("1");

        BigDecimal bd2 = new BigDecimal(-1);
        Integer i2 = -1;
        Long l2 = (long) -1;
        Float f2 = (float) -1;
        Double d2 = (double) -1;
        BigInteger bi2 = new BigInteger("-1");

        assertThat(NumberOperations.neg(converter, bd1)).isEqualTo(bd2);
        assertThat(NumberOperations.neg(converter, bi1)).isEqualTo(bi2);
        assertThat(NumberOperations.neg(converter, e1)).isEqualTo(d2);
        assertThat(NumberOperations.neg(converter, s1)).isEqualTo(l2);
        assertThat(NumberOperations.neg(converter, i1)).isEqualTo(i2);
        assertThat(NumberOperations.neg(converter, l1)).isEqualTo(l2);
        assertThat(NumberOperations.neg(converter, d1)).isEqualTo(d2);
        assertThat(NumberOperations.neg(converter, f1)).isEqualTo(f2);
    }
}
