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
import static org.assertj.core.api.Assertions.fail;

import org.flowable.common.engine.impl.de.odysseus.el.ObjectValueExpression;
import org.flowable.common.engine.impl.de.odysseus.el.TestCase;
import org.flowable.common.engine.impl.de.odysseus.el.misc.TypeConverter;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TreeTest extends TestCase {

    public static int foo() {
        return 0;
    }

    public static int bar(int op) {
        return op;
    }

    private SimpleContext context;

    @BeforeEach
    void setUp() throws Exception {
        context = new SimpleContext();

        // functions ns:f0(), ns:f1(int)
        context.setFunction("ns", "f0", TreeTest.class.getMethod("foo"));
        context.setFunction("ns", "f1", TreeTest.class.getMethod("bar", new Class[] { int.class }));

        // functions g0(), g1(int)
        context.setFunction("", "g0", TreeTest.class.getMethod("foo"));
        context.setFunction("", "g1", TreeTest.class.getMethod("bar", new Class[] { int.class }));

        // variables v0, v1
        context.setVariable("v0", new ObjectValueExpression(TypeConverter.DEFAULT, 0, long.class));
        context.setVariable("v1", new ObjectValueExpression(TypeConverter.DEFAULT, 1, long.class));
    }

    @Test
    void testBindFunctions() {
        Bindings bindings;

        bindings = parse("${ns:f0()}").bind(context.getFunctionMapper(), null);
        assertThat(bindings.getFunction(0)).isSameAs(context.getFunctionMapper().resolveFunction("ns", "f0"));
        try {
            bindings.getFunction(1);
            fail();
        } catch (Exception ignored) {
        }

        bindings = parse("${ns:f1(1)}").bind(context.getFunctionMapper(), null);
        assertThat(bindings.getFunction(0)).isSameAs(context.getFunctionMapper().resolveFunction("ns", "f1"));
        try {
            bindings.getFunction(1);
            fail();
        } catch (Exception ignored) {
        }

        bindings = parse("${ns:f0()+ns:f1(1)}").bind(context.getFunctionMapper(), null);
        assertThat(bindings.getFunction(0)).isSameAs(context.getFunctionMapper().resolveFunction("ns", "f0"));
        assertThat(bindings.getFunction(1)).isSameAs(context.getFunctionMapper().resolveFunction("ns", "f1"));
        try {
            bindings.getFunction(2);
            fail();
        } catch (Exception ignored) {
        }

        // the same for default namespace functions g0(), g1()
        bindings = parse("${g0()}").bind(context.getFunctionMapper(), null);
        assertThat(bindings.getFunction(0)).isSameAs(context.getFunctionMapper().resolveFunction("", "g0"));
        try {
            bindings.getFunction(1);
            fail();
        } catch (Exception ignored) {
        }

        bindings = parse("${g1(1)}").bind(context.getFunctionMapper(), null);
        assertThat(bindings.getFunction(0)).isSameAs(context.getFunctionMapper().resolveFunction("", "g1"));
        try {
            bindings.getFunction(1);
            fail();
        } catch (Exception ignored) {
        }

        bindings = parse("${g0()+g1(1)}").bind(context.getFunctionMapper(), null);
        assertThat(bindings.getFunction(0)).isSameAs(context.getFunctionMapper().resolveFunction("", "g0"));
        assertThat(bindings.getFunction(1)).isSameAs(context.getFunctionMapper().resolveFunction("", "g1"));
        try {
            bindings.getFunction(2);
            fail();
        } catch (Exception ignored) {
        }

        try {
            parse("${foo()}").bind(context.getFunctionMapper(), null);
            fail();
        } catch (Exception ignored) {
        }
        try {
            parse("${g1()}").bind(context.getFunctionMapper(), null);
            fail();
        } catch (Exception ignored) {
        }
        try {
            parse("${g1(1,2)}").bind(context.getFunctionMapper(), null);
            fail();
        } catch (Exception ignored) {
        }
    }

    @Test
    void testBindVariables() {
        Bindings bindings;

        bindings = parse("${v0}").bind(null, context.getVariableMapper());
        assertThat(bindings.getVariable(0)).isSameAs(context.getVariableMapper().resolveVariable("v0"));
        try {
            bindings.getVariable(1);
            fail();
        } catch (Exception ignored) {
        }

        bindings = parse("${v1}").bind(null, context.getVariableMapper());
        assertThat(bindings.getVariable(0)).isSameAs(context.getVariableMapper().resolveVariable("v1"));
        try {
            bindings.getVariable(1);
            fail();
        } catch (Exception ignored) {
        }

        bindings = parse("${v0+v1}").bind(null, context.getVariableMapper());
        assertThat(bindings.getVariable(0)).isSameAs(context.getVariableMapper().resolveVariable("v0"));
        assertThat(bindings.getVariable(1)).isSameAs(context.getVariableMapper().resolveVariable("v1"));
        try {
            bindings.getVariable(2);
            fail();
        } catch (Exception ignored) {
        }

        bindings = parse("${foo}").bind(null, context.getVariableMapper());
        assertThat(bindings.getVariable(0)).isNull();
        try {
            bindings.getVariable(1);
            fail();
        } catch (Exception ignored) {
        }
    }

    @Test
    void testBindFunctionsAndVariables() {
        Bindings bindings = parse("${ns:f0()+v0+g1(1)+v1+foo}").bind(context.getFunctionMapper(), context.getVariableMapper());
        assertThat(bindings.getFunction(0)).isSameAs(context.getFunctionMapper().resolveFunction("ns", "f0"));
        assertThat(bindings.getFunction(1)).isSameAs(context.getFunctionMapper().resolveFunction("", "g1"));
        try {
            bindings.getFunction(2);
            fail();
        } catch (Exception ignored) {
        }
        assertThat(bindings.getVariable(0)).isSameAs(context.getVariableMapper().resolveVariable("v0"));
        assertThat(bindings.getVariable(1)).isSameAs(context.getVariableMapper().resolveVariable("v1"));
        assertThat(bindings.getVariable(2)).isNull();
        try {
            bindings.getVariable(3);
            fail();
        } catch (Exception ignored) {
        }
    }
}
