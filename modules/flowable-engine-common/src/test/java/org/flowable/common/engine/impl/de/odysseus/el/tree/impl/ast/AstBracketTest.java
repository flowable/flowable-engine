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

import java.util.HashMap;

import org.flowable.common.engine.impl.de.odysseus.el.TestCase;
import org.flowable.common.engine.impl.de.odysseus.el.TestClass;
import org.flowable.common.engine.impl.de.odysseus.el.tree.Bindings;
import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Builder;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleResolver;
import org.flowable.common.engine.impl.javax.el.ELException;
import org.flowable.common.engine.impl.javax.el.MethodInfo;
import org.flowable.common.engine.impl.javax.el.PropertyNotFoundException;
import org.flowable.common.engine.impl.javax.el.ValueExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AstBracketTest extends TestCase {

    AstBracket parseNode(String expression) {
        return (AstBracket) parse(expression).getRoot().getChild(0);
    }

    SimpleContext context;
    Bindings bindings;

    long foo = 1L;

    public long getFoo() {
        return foo;
    }

    public void setFoo(long value) {
        foo = value;
    }

    public long bar() {
        return 1L;
    }

    public long bar(long value) {
        return value;
    }

    public TestClass getTestClass() {
        return new TestClass();
    }

    public Object getNullObject() {
        return null;
    }

    @BeforeEach
    void setUp() {
        context = new SimpleContext(new SimpleResolver());
        context.getELResolver().setValue(context, null, "base", this);

        HashMap<Object, String> nullmap = new HashMap<>();
        nullmap.put(null, "foo");
        context.getELResolver().setValue(context, null, "nullmap", nullmap);

        bindings = new Bindings(null, new ValueExpression[2]);
    }

    @Test
    void testEval() {
        assertThatThrownBy(() -> parseNode("${base[bad]}").eval(bindings, context))
                .isInstanceOf(ELException.class);
        assertThat(parseNode("${base['foo']}").eval(bindings, context)).isEqualTo(1L);
    }

    @Test
    void testAppendStructure() {
        StringBuilder s = new StringBuilder();
        parseNode("${foo[bar]}").appendStructure(s, new Bindings(null, null));
        assertThat(s.toString()).isEqualTo("foo[bar]");
    }

    @Test
    void testIsLiteralText() {
        assertThat(parseNode("${foo[bar]}").isLiteralText()).isFalse();
    }

    @Test
    void testIsLeftValue() {
        assertThat(parseNode("${'foo'[bar]}").isLeftValue()).isFalse();
        assertThat(parseNode("${foo[bar]}").isLeftValue()).isTrue();
    }

    @Test
    void testGetType() {
        assertThatThrownBy(() -> parseNode("${base[bad]}").getType(bindings, context))
                .isInstanceOf(ELException.class);
        assertThat(parseNode("${base['foo']}").getType(bindings, context)).isEqualTo(long.class);
        assertThat(parseNode("${'base'['foo']}").getType(bindings, context)).isNull();
        if (BUILDER.isEnabled(Builder.Feature.NULL_PROPERTIES)) {
            assertThat(parseNode("${nullmap[null]}").getType(bindings, context)).isEqualTo(Object.class);
        } else {
            assertThatThrownBy(() -> parseNode("${nullmap[null]}").getType(bindings, context))
                    .isInstanceOf(ELException.class);
        }
    }

    @Test
    void testIsReadOnly() {
        assertThat(parseNode("${base['foo']}").isReadOnly(bindings, context)).isFalse();
        assertThat(parseNode("${'base'['foo']}").isReadOnly(bindings, context)).isTrue();
        if (BUILDER.isEnabled(Builder.Feature.NULL_PROPERTIES)) {
            assertThat(parseNode("${nullmap[null]}").isReadOnly(bindings, context)).isFalse();
        } else {
            assertThatThrownBy(() -> parseNode("${nullmap[null]}").isReadOnly(bindings, context))
                    .isInstanceOf(ELException.class);
        }
    }

    @Test
    void testSetValue() {
        assertThatThrownBy(() -> parseNode("${base[bad]}").setValue(bindings, context, "good"))
                .isInstanceOf(ELException.class);
        parseNode("${base['foo']}").setValue(bindings, context, 2L);
        assertThat(getFoo()).isEqualTo(2L);
        parseNode("${base['foo']}").setValue(bindings, context, "3");
        assertThat(getFoo()).isEqualTo(3L);
        if (BUILDER.isEnabled(Builder.Feature.NULL_PROPERTIES)) {
            parseNode("${nullmap[null]}").setValue(bindings, context, "bar");
            assertThat(parseNode("${nullmap[null]}").eval(bindings, context)).isEqualTo("bar");
            parseNode("${nullmap[null]}").setValue(bindings, context, "foo");
        } else {
            assertThatThrownBy(() -> parseNode("${nullmap[null]}").setValue(bindings, context, "bar"))
                    .isInstanceOf(ELException.class);
        }
    }

    @Test
    void testGetValue() {
        assertThat(eval("${base['foo']}", context)).isEqualTo(1L);
        assertThat(eval("${base['foo']}", context, String.class)).isEqualTo("1");
        assertThat(eval("${base.nullObject['class']}", context, Object.class)).isNull();
        if (BUILDER.isEnabled(Builder.Feature.NULL_PROPERTIES)) {
            assertThat(eval("${nullmap[null]}", context)).isEqualTo("foo");
        } else {
            assertThat(eval("${nullmap[null]}", context)).isNull();
        }
    }

    @Test
    void testGetValueReference() {
        assertThat(parseNode("${base['foo']}").getValueReference(bindings, context).getBase()).isEqualTo(this);
        assertThat(parseNode("${base['foo']}").getValueReference(bindings, context).getProperty()).isEqualTo("foo");
    }

    @Test
    void testInvoke() {
        assertThat(parseNode("${base['bar']}").invoke(bindings, context, long.class, new Class[0], null)).isEqualTo(1L);
        assertThat(parseNode("${base['bar']}").invoke(bindings, context, null, new Class[] { long.class }, new Object[] { 2L })).isEqualTo(2L);

        assertThat(parseNode("${base.testClass.anonymousTestInterface['fourtyTwo']}").invoke(bindings, context, null, new Class[0], null)).isEqualTo(42);
        assertThat(parseNode("${base.testClass.nestedTestInterface['fourtyTwo']}").invoke(bindings, context, null, new Class[0], null)).isEqualTo(42);

        assertThatThrownBy(() -> parseNode("${base.nullObject['class']}").invoke(bindings, context, null, null, new Object[0]))
                .isInstanceOf(PropertyNotFoundException.class);
    }

    @Test
    void testGetMethodInfo() {
        MethodInfo info = null;

        // long bar()
        info = parseNode("${base['bar']}").getMethodInfo(bindings, context, long.class, new Class[0]);
        assertThat(info.getName()).isEqualTo("bar");
        assertThat(info.getParamTypes()).isEmpty();
        assertThat(info.getReturnType()).isEqualTo(long.class);

        // long bar(long)
        info = parseNode("${base['bar']}").getMethodInfo(bindings, context, null, new Class[] { long.class });
        assertThat(info.getName()).isEqualTo("bar");
        assertThat(info.getParamTypes()).containsExactly(long.class);
        assertThat(info.getReturnType()).isEqualTo(long.class);

        // bad arg type
        assertThatThrownBy(() -> parseNode("${base['bar']}").getMethodInfo(bindings, context, null, new Class[] { String.class }))
                .isInstanceOf(ELException.class);
        // bad return type
        assertThatThrownBy(() -> parseNode("${base['bar']}").getMethodInfo(bindings, context, String.class, new Class[0]))
                .isInstanceOf(ELException.class);
    }
}
