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

import org.flowable.common.engine.impl.de.odysseus.el.tree.TreeStore;
import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Builder;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleResolver;
import org.flowable.common.engine.impl.javax.el.BeanELResolver;
import org.flowable.common.engine.impl.javax.el.MethodInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TreeMethodExpressionTest extends TestCase {

    public int foo() {
        return 0;
    }

    public int bar() {
        return 0;
    }

    SimpleContext context;
    TreeStore store = new TreeStore(new Builder(Builder.Feature.METHOD_INVOCATIONS), null);

    @BeforeEach
    void setUp() {
        context = new SimpleContext(new SimpleResolver(new BeanELResolver()));
        context.getELResolver().setValue(context, null, "base", this);
    }

    @Test
    void testEqualsAndHashCode() {
        TreeMethodExpression e1, e2;
        e1 = new TreeMethodExpression(store, null, null, null, "${base.foo}", null, new Class[0]);
        e2 = new TreeMethodExpression(store, null, null, null, "${base.foo}", null, new Class[0]);
        assertThat(e2).isEqualTo(e1);

        e1 = new TreeMethodExpression(store, null, null, null, "${base.foo}", null, new Class[0]);
        e2 = new TreeMethodExpression(store, null, null, null, "${base.bar}", null, new Class[0]);
        assertThat(e2).isNotEqualTo(e1);
    }

    @Test
    void testGetExpressionString() {
        assertThat(new TreeMethodExpression(store, null, null, null, "${base.foo}", null, new Class[0]).getExpressionString()).isEqualTo("${base.foo}");
    }

    @Test
    void testIsLiteralText() {
        assertThat(new TreeMethodExpression(store, null, null, null, "${base.foo}", null, new Class[0]).isLiteralText()).isFalse();
        assertThat(new TreeMethodExpression(store, null, null, null, "base.foo", null, new Class[0]).isLiteralText()).isTrue();
    }

    @Test
    void testIsDeferred() {
        assertThat(new TreeMethodExpression(store, null, null, null, "foo", null, new Class[0]).isDeferred()).isFalse();
        assertThat(new TreeMethodExpression(store, null, null, null, "${foo}", null, new Class[0]).isDeferred()).isFalse();
        assertThat(new TreeMethodExpression(store, null, null, null, "#{foo}", null, new Class[0]).isDeferred()).isTrue();
    }

    @Test
    void testGetMethodInfo() {
        TreeMethodExpression e = new TreeMethodExpression(store, null, null, null, "${base.foo}", null, new Class[0]);
        MethodInfo info = e.getMethodInfo(context);
        assertThat(info.getName()).isEqualTo("foo");
        assertThat(info.getParamTypes()).isEmpty();
        assertThat(info.getReturnType()).isEqualTo(int.class);
    }

    @Test
    void testInvoke() {
        assertThat(new TreeMethodExpression(store, null, null, null, "${base.foo}", null, new Class[0]).invoke(context, null)).isEqualTo(0);
        assertThat(new TreeMethodExpression(store, null, null, null, "${base.foo()}", null, null).invoke(context, null)).isEqualTo(0);
    }

    @Test
    void testSerialize() throws Exception {
        TreeMethodExpression expression = new TreeMethodExpression(store, null, null, null, "${base.foo}", null, new Class[0]);
        assertThat(deserialize(serialize(expression))).isEqualTo(expression);
    }
}
