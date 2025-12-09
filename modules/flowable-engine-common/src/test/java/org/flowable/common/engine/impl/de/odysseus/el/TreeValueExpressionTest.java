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
package org.flowable.common.engine.impl.de.odysseus.el;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flowable.common.engine.impl.de.odysseus.el.tree.TreeStore;
import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Builder;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleResolver;
import org.flowable.common.engine.impl.javax.el.BeanELResolver;
import org.flowable.common.engine.impl.javax.el.PropertyNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TreeValueExpressionTest extends TestCase {

    public static int foo() {
        return 0;
    }

    public static int bar() {
        return 0;
    }

    int foobar;

    public void setFoobar(int value) {
        foobar = value;
    }

    SimpleContext context;
    TreeStore store = new TreeStore(new Builder(), null);

    @BeforeEach
    void setUp() throws Exception {
        context = new SimpleContext(new SimpleResolver(new BeanELResolver()));
        context.getELResolver().setValue(context, null, "base", this);

        // variables var_long_1, var_long_2
        context.setVariable("var_long_1", new TreeValueExpression(store, null, null, null, "${1}", long.class));
        context.setVariable("var_long_2", new TreeValueExpression(store, null, null, null, "${1}", long.class));
        // var_var_long_1 --> var_long_1, var_var_long_2 --> var_long_1
        context.setVariable("var_var_long_1", new TreeValueExpression(store, null, context.getVariableMapper(), null, "${var_long_1}", long.class));
        context.setVariable("var_var_long_2", new TreeValueExpression(store, null, context.getVariableMapper(), null, "${var_long_2}", long.class));

        // functions ns:f0(), ns:f1()
        context.setFunction("", "foo", getClass().getMethod("foo"));
        context.setFunction("ns", "foo_1", getClass().getMethod("foo"));
        context.setFunction("ns", "foo_2", getClass().getMethod("foo"));

        context.setVariable("var_foo_1", new TreeValueExpression(store, context.getFunctionMapper(), null, null, "${ns:foo_1()}", long.class));
        context.setVariable("var_foo_2", new TreeValueExpression(store, context.getFunctionMapper(), null, null, "${ns:foo_2()}", long.class));

        context.setVariable("var_foobar", new TreeValueExpression(store, null, context.getVariableMapper(), null, "${base.foobar}", int.class));

        context.getELResolver().setValue(context, null, "property_foo", "foo");
    }

    @Test
    void testEqualsAndHashCode() throws NoSuchMethodException {
        TreeValueExpression e1, e2;

        e1 = new TreeValueExpression(store, null, context.getVariableMapper(), null, "${1}", Object.class);
        e2 = new TreeValueExpression(store, null, context.getVariableMapper(), null, "${1}", Object.class);
        assertThat(e2).isEqualTo(e1);
        assertThat(e2.hashCode()).isEqualTo(e1.hashCode());

        e1 = new TreeValueExpression(store, null, context.getVariableMapper(), null, "${var_long_1}", Object.class);
        e2 = new TreeValueExpression(store, null, context.getVariableMapper(), null, "${var_long_2}", Object.class);
        assertThat(e2).isEqualTo(e1);
        assertThat(e2.hashCode()).isEqualTo(e1.hashCode());

        e1 = new TreeValueExpression(store, null, context.getVariableMapper(), null, "${var_var_long_1}", Object.class);
        e2 = new TreeValueExpression(store, null, context.getVariableMapper(), null, "${var_var_long_2}", Object.class);
        assertThat(e2).isEqualTo(e1);
        assertThat(e2.hashCode()).isEqualTo(e1.hashCode());

        e1 = new TreeValueExpression(store, null, context.getVariableMapper(), null, "${var_long_1}", Object.class);
        context.getVariableMapper().setVariable("var_long_1", new TreeValueExpression(store, null, context.getVariableMapper(), null, "${-1}", Object.class));
        e2 = new TreeValueExpression(store, null, context.getVariableMapper(), null, "${var_long_1}", Object.class);
        assertThat(e2).isNotEqualTo(e1);

        e1 = new TreeValueExpression(store, null, context.getVariableMapper(), null, "${var_foo_1}", Object.class);
        e2 = new TreeValueExpression(store, null, context.getVariableMapper(), null, "${var_foo_2}", Object.class);
        assertThat(e2).isEqualTo(e1);
        assertThat(e2.hashCode()).isEqualTo(e1.hashCode());

        e2 = new TreeValueExpression(store, context.getFunctionMapper(), null, null, "${ns:foo_1()}", Object.class);
        e1 = new TreeValueExpression(store, context.getFunctionMapper(), null, null, "${ns:foo_2()}", Object.class);
        assertThat(e2).isEqualTo(e1);
        assertThat(e2.hashCode()).isEqualTo(e1.hashCode());

        e2 = new TreeValueExpression(store, context.getFunctionMapper(), null, null, "${foo()}", Object.class);
        e1 = new TreeValueExpression(store, context.getFunctionMapper(), null, null, "${ns:foo_1()}", Object.class);
        assertThat(e2).isEqualTo(e1);
        assertThat(e2.hashCode()).isEqualTo(e1.hashCode());

        e2 = new TreeValueExpression(store, context.getFunctionMapper(), null, null, "${foo()}", Object.class);
        context.setFunction("", "foo", getClass().getMethod("bar"));
        e1 = new TreeValueExpression(store, context.getFunctionMapper(), null, null, "${foo()}", Object.class);
        assertThat(e2).isNotEqualTo(e1);

        e2 = new TreeValueExpression(store, context.getFunctionMapper(), null, null, "${ns:foo_1()}", Object.class);
        context.setFunction("ns", "foo_1", getClass().getMethod("bar"));
        e1 = new TreeValueExpression(store, context.getFunctionMapper(), null, null, "${ns:foo_1()}", Object.class);
        assertThat(e2).isNotEqualTo(e1);
    }

    @Test
    void testGetExpressionString() {
        assertThat(new TreeValueExpression(store, null, null, null, "foo", Object.class).getExpressionString()).isEqualTo("foo");
    }

    @Test
    void testIsLiteralText() {
        assertThat(new TreeValueExpression(store, null, null, null, "foo", Object.class).isLiteralText()).isTrue();
        assertThat(new TreeValueExpression(store, null, null, null, "${foo}", Object.class).isLiteralText()).isFalse();
    }

    @Test
    void testIsDeferred() {
        assertThat(new TreeValueExpression(store, null, null, null, "foo", Object.class).isDeferred()).isFalse();
        assertThat(new TreeValueExpression(store, null, null, null, "${foo}", Object.class).isDeferred()).isFalse();
        assertThat(new TreeValueExpression(store, null, null, null, "#{foo}", Object.class).isDeferred()).isTrue();
    }

    @Test
    void testGetExpectedType() {
        assertThat(new TreeValueExpression(store, null, null, null, "${foo}", Object.class).getExpectedType()).isEqualTo(Object.class);
        assertThat(new TreeValueExpression(store, null, null, null, "${foo}", String.class).getExpectedType()).isEqualTo(String.class);
    }

    @Test
    void testGetType() {
        assertThat(new TreeValueExpression(store, null, null, null, "${property_foo}", Object.class).isReadOnly(context)).isFalse();
    }

    @Test
    void testIsReadOnly() {
        assertThat(new TreeValueExpression(store, null, null, null, "${property_foo}", Object.class).isReadOnly(context)).isFalse();
    }

    @Test
    void testSetValue() {
        new TreeValueExpression(store, null, null, null, "${property_foo}", Object.class).setValue(context, "bar");
        assertThat(new TreeValueExpression(store, null, null, null, "${property_foo}", Object.class).getValue(context)).isEqualTo("bar");

        // Test added for bug #2748538
        new TreeValueExpression(store, null, context.getVariableMapper(), null, "${var_foobar}", Object.class).setValue(context, 123);
        assertThat(foobar).isEqualTo(123);
        assertThatThrownBy(() -> context.getELResolver().getValue(context, null, "var_foobar"))
                .isInstanceOf(PropertyNotFoundException.class);
    }

    @Test
    void testGetValue() {
        assertThat(new TreeValueExpression(store, null, null, null, "${property_foo}", Object.class).getValue(context)).isEqualTo("foo");
    }

    @Test
    void testSerialize() throws Exception {
        TreeValueExpression expression = new TreeValueExpression(store, context.getFunctionMapper(), context.getVariableMapper(), null, "${var_long_1 + foo()}",
                Object.class);
        assertThat(deserialize(serialize(expression))).isEqualTo(expression);
    }
}
