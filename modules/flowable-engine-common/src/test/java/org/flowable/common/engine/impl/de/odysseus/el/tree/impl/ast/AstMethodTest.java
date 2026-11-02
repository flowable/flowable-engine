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
package org.flowable.common.engine.impl.de.odysseus.el.tree.impl.ast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flowable.common.engine.impl.de.odysseus.el.TestCase;
import org.flowable.common.engine.impl.de.odysseus.el.tree.Bindings;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleResolver;
import org.flowable.common.engine.impl.javax.el.BeanELResolver;
import org.flowable.common.engine.impl.javax.el.ELException;
import org.flowable.common.engine.impl.javax.el.MethodNotFoundException;
import org.flowable.common.engine.impl.javax.el.PropertyNotFoundException;
import org.flowable.common.engine.impl.javax.el.ValueExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AstMethodTest extends TestCase {

    AstMethod parseNode(String expression) {
        return (AstMethod) parse(expression).getRoot().getChild(0);
    }

    SimpleContext context;
    Bindings bindings;

    long foo = 1L;

    public long getFoo() {
        return foo;
    }

    void setFoo(long value) {
        foo = value;
    }

    public long bar() {
        return 1L;
    }

    public long bar(long value) {
        return value;
    }

    public Object getNullObject() {
        return null;
    }

    @BeforeEach
    void setUp() {
        context = new SimpleContext(new SimpleResolver(new BeanELResolver()));
        context.getELResolver().setValue(context, null, "base", this);

        bindings = new Bindings(null, new ValueExpression[1]);
    }

    @Test
    void testEval() {
        assertThatThrownBy(() -> eval("${base.bad()}", context))
                .isInstanceOf(MethodNotFoundException.class);
        assertThat(eval("${base.bar()}", context)).isEqualTo(1L);
        assertThat(eval("${base.bar(3)}", context)).isEqualTo(3L);
    }

    @Test
    void testAppendStructure() {
        StringBuilder s = new StringBuilder();
        parseNode("${foo.bar(1)}").appendStructure(s, new Bindings(null, null, null));
        assertThat(s.toString()).isEqualTo("foo.bar(1)");
    }

    @Test
    void testIsLiteralText() {
        assertThat(parseNode("${foo.bar()}").isLiteralText()).isFalse();
    }

    @Test
    void testIsLeftValue() {
        assertThat(parseNode("${foo.bar()}").isLeftValue()).isFalse();
    }

    @Test
    void testGetType() {
        assertThat(parseNode("${base.foo()}").getType(bindings, context)).isNull();
    }

    @Test
    void testIsReadOnly() {
        assertThat(parseNode("${base.foo()}").isReadOnly(bindings, context)).isTrue();
    }

    @Test
    void testSetValue() {
        assertThatThrownBy(() -> parseNode("${base.foo()}").setValue(bindings, context, 0))
                .isInstanceOf(ELException.class);
    }

    @Test
    void testGetValue() {
        assertThat(eval("${base.bar()}", context, String.class)).isEqualTo("1");
        assertThat(eval("${base.bar(3)}", context, String.class)).isEqualTo("3");

        assertThat(eval("${base.nullObject.toString()}", context, Object.class)).isNull();
    }

    @Test
    void testGetValueReference() {
        assertThat(parseNode("${base.bar()}").getValueReference(bindings, context)).isNull();
    }

    @Test
    void testInvoke() {
        assertThat(parseNode("${base.bar()}").invoke(bindings, context, null, null, new Object[] { 999L })).isEqualTo(1L);
        assertThat(parseNode("${base.bar(3)}").invoke(bindings, context, null, new Class[] { long.class }, new Object[] { 999L })).isEqualTo(3L);

        assertThatThrownBy(() -> parseNode("${base.nullObject.toString()}").invoke(bindings, context, null, null, new Object[0]))
                .isInstanceOf(PropertyNotFoundException.class);
    }

    @Test
    void testGetMethodInfo() {
        assertThat(parseNode("${base.bar()}").getMethodInfo(bindings, context, null, new Class[] { long.class })).isNull();
    }
}
