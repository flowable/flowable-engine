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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.flowable.common.engine.impl.el.BaseElTest;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ExpressionFactory;
import org.flowable.common.engine.impl.javax.el.ValueExpression;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("We do not support Map Construction https://jakarta.ee/specifications/expression-language/6.0/jakarta-expression-language-spec-6.0#map-construction")
class AstMapDataTest extends BaseElTest {

    private static final Map<String, String> simpleMap = new HashMap<>();
    private static final Map<Object, Object> nestedMap = new HashMap<>();

    static {
        simpleMap.put("a", "1");
        simpleMap.put("b", "2");
        simpleMap.put("c", "3");

        nestedMap.put("simple", simpleMap);
        // {} will get parsed as an empty Set as there is nothing to hint to the
        // parser that Map is expected here.
        nestedMap.put("empty", Collections.EMPTY_SET);
        nestedMap.put("d", "4");
    }

    @Test
    void testSimple01() {
        Object result = eval("{'a':'1','b':'2','c':'3'}", Map.class);
        assertThat(result).isEqualTo(simpleMap);
    }

    @Test
    void testSimple02() {
        Object result = eval("{}", Map.class);
        assertThat(result).isEqualTo(Collections.EMPTY_MAP);
    }

    @Test
    void testNested01() {
        Object result = eval(
                "{'simple':{'a':'1','b':'2','c':'3'}," +
                        "'empty':{}," +
                        "'d':'4'}", Map.class);
        assertThat(result).isEqualTo(nestedMap);
    }

    @Test
    void testGetType() {
        ELContext context = new SimpleContext();
        ExpressionFactory factory = createExpressionFactory();

        ValueExpression ve = factory.createValueExpression(
                context, "${{'a':'1','b':'2','c':'3'}}", Map.class);

        assertThat(ve.getType(context)).isEqualTo(Map.class);
        assertThat(ve.<String>getValue(context)).isEqualTo(simpleMap);
    }

    @Test
    void testLiteralWithVariable() {
        SimpleContext context = new SimpleContext();
        ExpressionFactory factory = createExpressionFactory();

        String key = "myKey";
        String value = "myValue";
        context.setVariable("aaa", factory.createValueExpression(context, "${'" + key + "'}", String.class));
        context.setVariable("bbb", factory.createValueExpression(context, "${'" + value + "'}", String.class));

        Object result = eval("{ aaa : bbb }.get(aaa)", context);

        assertThat(result).isEqualTo(value);
    }
}
