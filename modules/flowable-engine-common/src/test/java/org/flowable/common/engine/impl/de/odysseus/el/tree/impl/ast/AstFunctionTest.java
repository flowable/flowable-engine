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
import org.flowable.common.engine.impl.de.odysseus.el.tree.Tree;
import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Builder;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.flowable.common.engine.impl.javax.el.ELException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AstFunctionTest extends TestCase {

    public static int foo() {
        return 0;
    }

    public static int bar(int op) {
        return op;
    }

    public static int foobar(int op1, int op2) {
        return op1 + op2;
    }

    public static int foovar(int... ops) {
        int sum = 0;
        for (int op : ops) {
            sum += op;
        }
        return sum;
    }

    public static int foovar2(Integer... ops) {
        int sum = 0;
        for (Integer op : ops) {
            if (op != null) {
                sum += op;
            }
        }
        return sum;
    }

    AstFunction parseNode(String expression) {
        return getNode(parse(expression));
    }

    AstFunction getNode(Tree tree) {
        return (AstFunction) tree.getRoot().getChild(0);
    }

    SimpleContext context;

    @BeforeEach
    void setUp() throws Exception {
        context = new SimpleContext();

        // functions ns:f0(), ns:f1(int), ns:f2(int)
        context.setFunction("ns", "f0", getClass().getMethod("foo"));
        context.setFunction("ns", "f1", getClass().getMethod("bar", new Class[] { int.class }));
        context.setFunction("ns", "f2", getClass().getMethod("foobar", new Class[] { int.class, int.class }));

        // functions g0(), g1(int), g2(int,int)
        context.setFunction("", "g0", getClass().getMethod("foo"));
        context.setFunction("", "g1", getClass().getMethod("bar", new Class[] { int.class }));
        context.setFunction("", "g2", getClass().getMethod("foobar", new Class[] { int.class, int.class }));

        context.setFunction("vararg", "f", getClass().getMethod("foovar", new Class[] { int[].class }));
        context.getELResolver().setValue(context, null, "var111", new int[] { 1, 1, 1 });
        context.getELResolver().setValue(context, null, "var111s", new String[] { "1", "1", "1" });
    }

    @Test
    void testVarargs() {
        Builder builder = new Builder(Builder.Feature.VARARGS);
        Tree tree = null;

        tree = builder.build("${vararg:f()}");
        assertThat(getNode(tree).eval(tree.bind(context.getFunctionMapper(), null), null)).isEqualTo(foovar());

        tree = builder.build("${vararg:f(1)}");
        assertThat(getNode(tree).eval(tree.bind(context.getFunctionMapper(), null), null)).isEqualTo(foovar(1));

        tree = builder.build("${vararg:f(1,1)}");
        assertThat(getNode(tree).eval(tree.bind(context.getFunctionMapper(), null), null)).isEqualTo(foovar(1, 1));

        tree = builder.build("${vararg:f(null)}");
        assertThat(getNode(tree).eval(tree.bind(context.getFunctionMapper(), null), null)).isEqualTo(foovar(0));

        tree = builder.build("${vararg:f(var111)}");
        assertThat(getNode(tree).eval(tree.bind(context.getFunctionMapper(), null), context)).isEqualTo(foovar(1, 1, 1));

        tree = builder.build("${vararg:f(var111s)}");
        assertThat(getNode(tree).eval(tree.bind(context.getFunctionMapper(), null), context)).isEqualTo(foovar(1, 1, 1));
    }

    @Test
    void testEval() {
        Tree tree = null;

        tree = parse("${ns:f0()}");
        assertThat(getNode(tree).eval(tree.bind(context.getFunctionMapper(), null), null)).isEqualTo(foo());

        tree = parse("${ns:f1(42)}");
        assertThat(getNode(tree).eval(tree.bind(context.getFunctionMapper(), null), null)).isEqualTo(bar(42));

        tree = parse("${ns:f2(21,21)}");
        assertThat(getNode(tree).eval(tree.bind(context.getFunctionMapper(), null), null)).isEqualTo(foobar(21, 21));

        tree = parse("${g0()}");
        assertThat(getNode(tree).eval(tree.bind(context.getFunctionMapper(), null), null)).isEqualTo(foo());

        tree = parse("${g1(42)}");
        assertThat(getNode(tree).eval(tree.bind(context.getFunctionMapper(), null), null)).isEqualTo(bar(42));

        tree = parse("${g2(21,21)}");
        assertThat(getNode(tree).eval(tree.bind(context.getFunctionMapper(), null), null)).isEqualTo(foobar(21, 21));
    }

    @Test
    void testAppendStructure() {
        StringBuilder s = null;

        Bindings bindings = new Bindings(null, null);

        s = new StringBuilder();
        parseNode("${f()}").appendStructure(s, bindings);
        parseNode("${f(x)}").appendStructure(s, bindings);
        parseNode("${f(x,y)}").appendStructure(s, bindings);
        assertThat(s.toString()).isEqualTo("f()f(x)f(x, y)");

        s = new StringBuilder();
        parseNode("${p:f()}").appendStructure(s, bindings);
        parseNode("${p:f(x)}").appendStructure(s, bindings);
        parseNode("${p:f(x,y)}").appendStructure(s, bindings);
        assertThat(s.toString()).isEqualTo("p:f()p:f(x)p:f(x, y)");
    }

    @Test
    void testIsLiteralText() {
        assertThat(parseNode("${f()}").isLiteralText()).isFalse();
    }

    @Test
    void testIsLeftValue() {
        assertThat(parseNode("${f()}").isLeftValue()).isFalse();
    }

    @Test
    void testGetType() {
        assertThat(parseNode("${f()}").getType(null, null)).isNull();
    }

    @Test
    void testIsReadOnly() {
        assertThat(parseNode("${f()}").isReadOnly(null, null)).isTrue();
    }

    @Test
    void testSetValue() {
        assertThatThrownBy(() -> parseNode("${f()}").setValue(null, null, null))
                .isInstanceOf(ELException.class);
    }

    @Test
    void testGetValue() {
        Tree tree = null;

        tree = parse("${ns:f0()}");

        assertThat(getNode(tree).getValue(tree.bind(context.getFunctionMapper(), null), null, null)).isEqualTo(foo());
        assertThat(getNode(tree).getValue(tree.bind(context.getFunctionMapper(), null), null, String.class)).isEqualTo("" + foo());
    }

    @Test
    void testGetValueReference() {
        assertThat(parseNode("${ns:f0()}").getValueReference(null, null)).isNull();
    }
}
