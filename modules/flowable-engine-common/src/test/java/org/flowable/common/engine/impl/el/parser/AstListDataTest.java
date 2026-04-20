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
import java.util.Collections;
import java.util.List;

import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.flowable.common.engine.impl.el.BaseElTest;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ExpressionFactory;
import org.flowable.common.engine.impl.javax.el.ValueExpression;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("We do not support List Construction https://jakarta.ee/specifications/expression-language/6.0/jakarta-expression-language-spec-6.0#list-construction")
class AstListDataTest extends BaseElTest {

    private static final List<String> simpleList = new ArrayList<>();
    private static final List<Object> nestedList = new ArrayList<>();

    static {
        simpleList.add("a");
        simpleList.add("b");
        simpleList.add("c");
        simpleList.add("b");
        simpleList.add("c");
        nestedList.add(simpleList);
        nestedList.add(Collections.EMPTY_LIST);
        nestedList.add("d");
    }

    @Test
    void testSimple01() {
        Object result = eval("['a','b','c', 'b', 'c']", List.class);
        assertThat(result).isEqualTo(simpleList);
    }

    @Test
    void testSimple02() {
        Object result = eval("[]", List.class);
        assertThat(result).isEqualTo(Collections.EMPTY_LIST);
    }

    @Test
    void testNested01() {
        Object result = eval("[['a','b','c','b','c'],[],'d']", List.class);
        assertThat(result).isEqualTo(nestedList);
    }

    @Test
    void testGetType() {
        ELContext context = new SimpleContext();
        ExpressionFactory factory = createExpressionFactory();

        ValueExpression ve = factory.createValueExpression(
                context, "${['a','b','c','b','c']}", List.class);

        assertThat(ve.getType(context)).isEqualTo(List.class);
        assertThat(ve.<Object>getValue(context)).isEqualTo(simpleList);
    }
}
