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
package org.flowable.common.engine.impl.de.odysseus.el.tree;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.flowable.common.engine.impl.de.odysseus.el.ObjectValueExpression;
import org.flowable.common.engine.impl.de.odysseus.el.TestCase;
import org.flowable.common.engine.impl.de.odysseus.el.misc.TypeConverter;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.flowable.common.engine.impl.javax.el.ValueExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BindingsTest extends TestCase {

    public static int foo() {
        return 0;
    }

    public static int bar(int i) {
        return i;
    }

    private SimpleContext context;

    @BeforeEach
    void setUp() throws Exception {
        context = new SimpleContext();

        // function ns:f()
        context.setFunction("ns", "f", BindingsTest.class.getMethod("foo"));

        // function g()
        context.setFunction("", "g", BindingsTest.class.getMethod("bar", int.class));

        // variable v
        context.setVariable("v", new ObjectValueExpression(TypeConverter.DEFAULT, 0L, long.class));
    }

    @Test
    void testSerialize() throws Exception {
        Bindings bindings = null;

        bindings = new Bindings(null, null);
        assertThat(deserialize(serialize(bindings))).isEqualTo(bindings);

        bindings = parse("${ns:f()+v+g(1)+x}").bind(context.getFunctionMapper(), context.getVariableMapper());
        assertThat(deserialize(serialize(bindings))).isEqualTo(bindings);
    }

    @Test
    void testEqualsAndHashcode() {
        Bindings bindings1 = null;
        Bindings bindings2 = null;

        bindings1 = new Bindings(null, null);
        bindings2 = new Bindings(null, null);
        assertThat(bindings2).isEqualTo(bindings1);
        assertThat(bindings2.hashCode()).isEqualTo(bindings1.hashCode());

        bindings1 = new Bindings(new Method[0], new ValueExpression[0]);
        bindings2 = new Bindings(null, null);
        assertThat(bindings2).isEqualTo(bindings1);
        assertThat(bindings2.hashCode()).isEqualTo(bindings1.hashCode());

        Tree tree = parse("${ns:f()+v+g(1)}+x");
        bindings1 = tree.bind(context.getFunctionMapper(), context.getVariableMapper());
        bindings2 = tree.bind(context.getFunctionMapper(), context.getVariableMapper());
        assertThat(bindings2).isEqualTo(bindings1);
        assertThat(bindings2.hashCode()).isEqualTo(bindings1.hashCode());
    }
}
