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

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleResolver;
import org.flowable.common.engine.impl.el.ReadOnlyMapELResolver;
import org.flowable.common.engine.impl.javax.el.CompositeELResolver;
import org.flowable.common.engine.impl.javax.el.ELBaseTest;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ExpressionFactory;
import org.flowable.common.engine.impl.javax.el.ValueExpression;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("We do not support assignment https://jakarta.ee/specifications/expression-language/6.0/jakarta-expression-language-spec-6.0#assignment-operator-a-b")
class AstAssignTest extends ELBaseTest {

    @Test
    void testGetValue01() {
        TesterBeanB testerBeanB = new TesterBeanB();
        assertThat(testerBeanB.getText()).isNull();
        CompositeELResolver resolver = new CompositeELResolver(List.of(
                new ReadOnlyMapELResolver(Map.of(
                        "bean01", testerBeanB
                )),
                new SimpleResolver()
        ));
        ELContext context = new SimpleContext(resolver);

        String result = eval("bean01.text = 'hello'", context, String.class);
        assertThat(result).isEqualTo("hello");
        assertThat(result).isEqualTo("hello");
    }

    @Test
    void testGetValue02() {
        TesterBeanB testerBeanB = new TesterBeanB();
        assertThat(testerBeanB.getText()).isNull();
        CompositeELResolver resolver = new CompositeELResolver(List.of(
                new ReadOnlyMapELResolver(Map.of(
                        "bean01", testerBeanB
                )),
                new SimpleResolver()
        ));
        ELContext context = new SimpleContext(resolver);

        String result = eval("bean01.text = 'hello'", context, String.class);
        assertThat(result).isEqualTo("hello");
        assertThat(result).isEqualTo("hello");
    }

    @Test
    void testGetType01() {
        TesterBeanB testerBeanB = new TesterBeanB();
        assertThat(testerBeanB.getText()).isNull();
        CompositeELResolver resolver = new CompositeELResolver(List.of(
                new ReadOnlyMapELResolver(Map.of(
                        "bean01", testerBeanB
                )),
                new SimpleResolver()
        ));
        ELContext context = new SimpleContext(resolver);
        ExpressionFactory factory = createExpressionFactory();

        ValueExpression ve = factory.createValueExpression(
                context, "${bean01.text = 'hello'}", String.class);

        assertThat(ve.getType(context)).isEqualTo(String.class);
        assertThat(ve.<String>getValue(context)).isEqualTo("hello");
        assertThat(testerBeanB.getText()).isEqualTo("hello");
    }

    @Test
    void testGetType02() {
        TesterBeanB testerBeanB = new TesterBeanB();
        assertThat(testerBeanB.getText()).isNull();
        CompositeELResolver resolver = new CompositeELResolver(List.of(
                new ReadOnlyMapELResolver(Map.of(
                        "bean01", testerBeanB
                )),
                new SimpleResolver()
        ));
        ELContext context = new SimpleContext(resolver);
        ExpressionFactory factory = createExpressionFactory();

        ValueExpression ve = factory.createValueExpression(
                context, "${bean01.text = 'hello'; bean01.text}", String.class);

        assertThat(ve.getType(context)).isEqualTo(String.class);
        assertThat(ve.<String>getValue(context)).isEqualTo("hello");
        assertThat(testerBeanB.getText()).isEqualTo("hello");
    }
}
