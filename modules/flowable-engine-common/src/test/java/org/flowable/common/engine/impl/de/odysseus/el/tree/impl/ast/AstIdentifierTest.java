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
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Method;

import org.flowable.common.engine.impl.de.odysseus.el.ObjectValueExpression;
import org.flowable.common.engine.impl.de.odysseus.el.TestCase;
import org.flowable.common.engine.impl.de.odysseus.el.TreeValueExpression;
import org.flowable.common.engine.impl.de.odysseus.el.misc.TypeConverter;
import org.flowable.common.engine.impl.de.odysseus.el.tree.Bindings;
import org.flowable.common.engine.impl.de.odysseus.el.tree.Tree;
import org.flowable.common.engine.impl.de.odysseus.el.tree.TreeStore;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleResolver;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ELException;
import org.flowable.common.engine.impl.javax.el.MethodExpression;
import org.flowable.common.engine.impl.javax.el.MethodInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AstIdentifierTest extends TestCase {

    public static long method_1() {
        return 1L;
    }

    class TestMethodExpression extends MethodExpression {

        final Method method;

        TestMethodExpression(Method method) {
            this.method = method;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }

        @Override
        public String getExpressionString() {
            return method.getName();
        }

        @Override
        public MethodInfo getMethodInfo(ELContext context) {
            return new MethodInfo(method.getName(), method.getReturnType(), method.getParameterTypes());
        }

        @Override
        public Object invoke(ELContext context, Object[] params) {
            try {
                return method.invoke(null, params);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean isLiteralText() {
            return false;
        }
    }

    AstIdentifier parseNode(String expression) {
        return getNode(parse(expression));
    }

    AstIdentifier getNode(Tree tree) {
        return (AstIdentifier) tree.getRoot().getChild(0);
    }

    SimpleContext context;

    @BeforeEach
    void setUp() throws Exception {
        context = new SimpleContext(new SimpleResolver());

        TypeConverter converter = TypeConverter.DEFAULT;

        // variables var_long_1, indentifier_string
        context.setVariable("var_long_1", new ObjectValueExpression(converter, 1L, long.class));
        context.setVariable("indentifier_string", new ObjectValueExpression(converter, "foo", String.class));
        context.setVariable("var_method_1", new ObjectValueExpression(converter, getClass().getMethod("method_1"), Method.class));
        context.setVariable("var_method_1_expr",
                new ObjectValueExpression(converter, new TestMethodExpression(getClass().getMethod("method_1")), MethodExpression.class));

        // properties property_long_1, indentifier_string
        context.getELResolver().setValue(context, null, "property_long_1", 1L);
        context.getELResolver().setValue(context, null, "indentifier_string", "bar"); // shadowed by variable indentifier_string
        context.getELResolver().setValue(context, null, "property_method_1", getClass().getMethod("method_1"));
        context.getELResolver().setValue(context, null, "property_method_1_expr", new TestMethodExpression(getClass().getMethod("method_1")));

        // var_var_long_1 --> var_long_1, var_property_long_1 --> property_long_1
        context.setVariable("var_var_long_1",
                new TreeValueExpression(new TreeStore(BUILDER, null), null, context.getVariableMapper(), null, "${var_long_1}", long.class));
        context.setVariable("var_property_long_1",
                new TreeValueExpression(new TreeStore(BUILDER, null), null, context.getVariableMapper(), null, "${property_long_1}", long.class));
    }

    @Test
    void testEval() {
        assertThatThrownBy(() -> eval("${bad}", context)).isInstanceOf(ELException.class);

        assertThat(eval("${var_long_1}", context)).isEqualTo(1L);

        assertThat(eval("${property_long_1}", context)).isEqualTo(1L);

        assertThat(eval("${indentifier_string}", context)).isEqualTo("foo");

        assertThat(eval("${var_var_long_1}", context)).isEqualTo(1L);

        assertThat(eval("${var_property_long_1}", context)).isEqualTo(1L);
    }

    @Test
    void testAppendStructure() {
        StringBuilder s = new StringBuilder();
        parseNode("${foo}").appendStructure(s, null);
        assertThat(s.toString()).isEqualTo("foo");
    }

    @Test
    void testIsLiteralText() {
        assertThat(parseNode("${foo}").isLiteralText()).isFalse();
    }

    @Test
    void testIsLeftValue() {
        assertThat(parseNode("${foo}").isLeftValue()).isTrue();
    }

    @Test
    void testGetType() {
        Tree tree = null;
        Bindings bindings = null;

        tree = parse("${var_long_1}");
        bindings = tree.bind(null, context.getVariableMapper());
        assertThat(getNode(tree).getType(bindings, context)).isEqualTo(null);

        tree = parse("${property_long_1}");
        bindings = tree.bind(null, context.getVariableMapper());
        assertThat(getNode(tree).getType(bindings, context)).isEqualTo(Object.class);

        tree = parse("${var_var_long_1}");
        bindings = tree.bind(null, context.getVariableMapper());
        assertThat(getNode(tree).getType(bindings, context)).isNull();

        tree = parse("${var_property_long_1}");
        bindings = tree.bind(null, context.getVariableMapper());
        assertThat(getNode(tree).getType(bindings, context)).isEqualTo(Object.class);

        tree = parse("${indentifier_string}");
        bindings = tree.bind(null, context.getVariableMapper());
        assertThat(getNode(tree).getType(bindings, context)).isNull();
    }

    @Test
    void testIsReadOnly() {
        Tree tree = null;
        Bindings bindings = null;

        tree = parse("${var_long_1}");
        bindings = tree.bind(null, context.getVariableMapper());
        assertThat(getNode(tree).isReadOnly(bindings, context)).isTrue();

        tree = parse("${property_long_1}");
        bindings = tree.bind(null, context.getVariableMapper());
        assertThat(getNode(tree).isReadOnly(bindings, context)).isFalse();

        tree = parse("${var_var_long_1}");
        bindings = tree.bind(null, context.getVariableMapper());
        assertThat(getNode(tree).isReadOnly(bindings, context)).isTrue();

        tree = parse("${var_property_long_1}");
        bindings = tree.bind(null, context.getVariableMapper());
        assertThat(getNode(tree).isReadOnly(bindings, context)).isFalse();

        tree = parse("${indentifier_string}");
        bindings = tree.bind(null, context.getVariableMapper());
        assertThat(getNode(tree).isReadOnly(bindings, context)).isTrue();
    }

    @Test
    void testSetValue() {
        Tree tree = null;
        Bindings bindings = null;

        tree = parse("${bad}");
        bindings = tree.bind(null, context.getVariableMapper());
        getNode(tree).setValue(bindings, context, "good");
        assertThat(getNode(tree).getValue(bindings, context, null)).isEqualTo("good");

        tree = parse("${var_long_1}");
        bindings = tree.bind(null, context.getVariableMapper());
        try {
            getNode(tree).setValue(bindings, context, 2L);
            fail();
        } catch (ELException ignored) {
        }

        tree = parse("${property_long_1}");
        bindings = tree.bind(null, context.getVariableMapper());
        assertThat(getNode(tree).getValue(bindings, context, null)).isEqualTo(1L);
        getNode(tree).setValue(bindings, context, 2L);
        assertThat(getNode(tree).getValue(bindings, context, null)).isEqualTo(2L);

        tree = parse("${var_var_long_1}");
        bindings = tree.bind(null, context.getVariableMapper());
        try {
            getNode(tree).setValue(bindings, context, 2L);
            fail();
        } catch (ELException ignored) {
        }

        tree = parse("${var_property_long_1}");
        bindings = tree.bind(null, context.getVariableMapper());
        assertThat(getNode(tree).getValue(bindings, context, null)).isEqualTo(2L);
        getNode(tree).setValue(bindings, context, 1L);
        assertThat(getNode(tree).getValue(bindings, context, null)).isEqualTo(1L);

        tree = parse("${indentifier_string}");
        bindings = tree.bind(null, context.getVariableMapper());
        try {
            getNode(tree).setValue(bindings, context, "bar");
            fail();
        } catch (ELException ignored) {
        }
    }

    @Test
    void testGetValue() {
        assertThatThrownBy(() -> eval("${bad}", context)).isInstanceOf(ELException.class);

        assertThat(eval("${var_long_1}", context)).isEqualTo(1L);
        assertThat(eval("${var_long_1}", context, String.class)).isEqualTo("1");
    }

    @Test
    void testGetValueReference() {
        Tree tree = null;
        Bindings bindings = null;

        tree = parse("${var_long_1}");
        bindings = tree.bind(null, context.getVariableMapper());
        assertThat(getNode(tree).getValueReference(bindings, context)).isNull();

        tree = parse("${property_long_1}");
        bindings = tree.bind(null, context.getVariableMapper());
        assertThat(getNode(tree).getValueReference(bindings, context)).isNotNull();
    }

    @Test
    void testInvoke() {
        Tree tree = null;
        Bindings bindings = null;

        tree = parse("${bad}");
        bindings = tree.bind(null, context.getVariableMapper());
        try {
            getNode(tree).invoke(bindings, context, long.class, new Class[0], null);
            fail();
        } catch (ELException ignored) {
        }

        tree = parse("${var_method_1}");
        bindings = tree.bind(null, context.getVariableMapper());
        assertThat(getNode(tree).invoke(bindings, context, long.class, new Class[0], null)).isEqualTo(1L);

        tree = parse("${property_method_1}");
        bindings = tree.bind(null, context.getVariableMapper());
        assertThat(getNode(tree).invoke(bindings, context, null, new Class[0], null)).isEqualTo(1L);

        // no return type - ok
        assertThat(getNode(tree).invoke(bindings, context, long.class, new Class[0], null)).isEqualTo(1L);
        // bad return type
        try {
            getNode(tree).invoke(bindings, context, int.class, new Class[0], null);
            fail();
        } catch (ELException ignored) {
        }
        // bad arg types
        try {
            getNode(tree).invoke(bindings, context, long.class, new Class[] { String.class }, null);
            fail();
        } catch (ELException ignored) {
        }
        // bad args
        try {
            getNode(tree).invoke(bindings, context, long.class, new Class[0], new Object[] { "" });
            fail();
        } catch (ELException ignored) {
        }

        tree = parse("${var_method_1_expr}");
        bindings = tree.bind(null, context.getVariableMapper());
        assertThat(getNode(tree).invoke(bindings, context, long.class, new Class[0], null)).isEqualTo(1L);

        tree = parse("${property_method_1_expr}");
        bindings = tree.bind(null, context.getVariableMapper());
        assertThat(getNode(tree).invoke(bindings, context, null, new Class[0], null)).isEqualTo(1L);

    }

    @Test
    void testGetMethodInfo() {
        Tree tree = null;
        Bindings bindings = null;
        MethodInfo info = null;

        tree = parse("${bad}");
        bindings = tree.bind(null, context.getVariableMapper());
        try {
            getNode(tree).getMethodInfo(bindings, context, long.class, new Class[0]);
            fail();
        } catch (ELException ignored) {
        }

        tree = parse("${var_method_1}");
        bindings = tree.bind(null, context.getVariableMapper());
        info = getNode(tree).getMethodInfo(bindings, context, long.class, new Class[0]);
        assertThat(info.getName()).isEqualTo("method_1");
        assertThat(info.getParamTypes()).isEmpty();
        assertThat(info.getReturnType()).isEqualTo(long.class);

        tree = parse("${property_method_1}");
        bindings = tree.bind(null, context.getVariableMapper());
        info = getNode(tree).getMethodInfo(bindings, context, long.class, new Class[0]);
        assertThat(info.getName()).isEqualTo("method_1");
        assertThat(info.getParamTypes()).isEmpty();
        assertThat(info.getReturnType()).isEqualTo(long.class);

        // no return type - ok
        info = getNode(tree).getMethodInfo(bindings, context, null, new Class[0]);
        assertThat(info.getName()).isEqualTo("method_1");
        assertThat(info.getParamTypes()).isEmpty();
        assertThat(info.getReturnType()).isEqualTo(long.class);
        // bad return type
        try {
            getNode(tree).getMethodInfo(bindings, context, int.class, new Class[0]);
            fail();
        } catch (ELException ignored) {
        }
        // bad arg types
        try {
            getNode(tree).getMethodInfo(bindings, context, long.class, new Class[] { String.class });
            fail();
        } catch (ELException ignored) {
        }
    }
}
