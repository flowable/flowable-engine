/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.common.engine.impl.el.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.flowable.common.engine.impl.el.BaseElTest;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ExpressionFactory;
import org.flowable.common.engine.impl.javax.el.ValueExpression;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("We do not support concatenation")
class AstConcatenationTest extends BaseElTest {

    private static final List<String> simpleList = new ArrayList<>();
    private static final Map<String, String> simpleMap = new HashMap<>();
    private static final Set<String> simpleSet = new HashSet<>();

    static {
        simpleList.add("a");
        simpleList.add("b");
        simpleList.add("c");
        simpleList.add("b");
        simpleList.add("c");

        simpleMap.put("a", "1");
        simpleMap.put("b", "2");
        simpleMap.put("c", "3");

        simpleSet.add("a");
        simpleSet.add("b");
        simpleSet.add("c");
    }

    @Test
    void testNullConcatNull() {
        Object result = eval("null += null", Integer.class);
        assertThat(result).isEqualTo(0);
    }

    @Test
    void testMapConcatMapNoConflicts() {
        Object result = eval("{'a':'1','b':'2'} += {'c':'3'}", Map.class);
        assertThat(result).isEqualTo(simpleMap);
    }

    @Test
    void testMapConcatMapConflicts() {
        Object result = eval("{'a':'1','b':'3'} += {'b':'2','c':'3'}", Map.class);
        assertThat(result).isEqualTo(simpleMap);
    }

    @Test
    void testSetConcatSetNoConflicts() {
        Object result = eval("{'a','b'} += {'c'}", Set.class);
        assertThat(result).isEqualTo(simpleSet);
    }

    @Test
    void testSetConcatSetConflicts() {
        Object result = eval("{'a','b'} += {'b','c'}", Set.class);
        assertThat(result).isEqualTo(simpleSet);
    }

    @Test
    void testSetConcatListNoConflicts() {
        Object result = eval("{'a','b'} += ['c']", Set.class);
        assertThat(result).isEqualTo(simpleSet);
    }

    @Test
    void testSetConcatListConflicts() {
        Object result = eval("{'a','b'} += ['b','c','c']", Set.class);
        assertThat(result).isEqualTo(simpleSet);
    }

    @Test
    void testListConcatList() {
        Object result = eval("['a','b','c'] += ['b','c']", List.class);
        assertThat(result).isEqualTo(simpleList);
    }

    @Test
    void testListConcatSet() {
        Object result = eval("['a','b','c'] += {'b','c'}", List.class);
        assertThat(result).isEqualTo(simpleList);
    }

    /**
     * Test string concatenation.
     */
    @Test
    void testConcatenation01() {
        Object result = eval("'a' += 'b'", String.class);
        assertThat(result).isEqualTo("ab");
    }

    /**
     * Test coercion to string then concatenation.
     */
    @Test
    void testConcatenation02() {
        Object result = eval("1 += 2", String.class);
        assertThat(result).isEqualTo("12");
    }

    /**
     * Test string concatenation with whitespace.
     */
    @Test
    void testConcatenation03() {
        Object result = eval("' a' += ' b '", String.class);
        assertThat(result).isEqualTo(" a b ");
    }

    /**
     * Test string concatenation with mixed types.
     */
    @Test
    void testConcatenation04() {
        Object result = eval("'a' += 3", String.class);
        assertThat(result).isEqualTo("a3");
    }

    /**
     * Test operator precedence (+ before +=).
     */
    @Test
    void testPrecedence01() {
        Object result = eval("1 + 2 += 3", String.class);
        assertThat(result).isEqualTo("33");
    }

    /**
     * Test operator precedence (+ before +=).
     */
    @Test
    void testPrecedence02() {
        Object result = eval("1 += 2 + 3", String.class);
        assertThat(result).isEqualTo("15");
    }

    /**
     * Test operator precedence (+= before >).
     */
    @Test
    void testPrecedence03() {
        Object result = eval("10 > 2 += 3", String.class);
        assertThat(result).isEqualTo("false");
    }

    /**
     * Test operator precedence (+= before >).
     */
    @Test
    void testPrecedence04() {
        Object result = eval("1 += 2 > 3", String.class);
        assertThat(result).isEqualTo("true");
    }

    @Test
    void testGetType() {
        ELContext context = new SimpleContext();
        ExpressionFactory factory = createExpressionFactory();

        ValueExpression ve = factory.createValueExpression(
                context, "${'a' += 3}", String.class);

        assertThat(ve.getType(context)).isEqualTo(String.class);
        assertThat(ve.<String>getValue(context)).isEqualTo("a3");
    }
}
