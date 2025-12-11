/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.common.engine.impl.el;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.flowable.common.engine.impl.javax.el.ELBaseTest;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ELException;
import org.flowable.common.engine.impl.javax.el.MethodExpression;
import org.flowable.common.engine.impl.javax.el.MethodInfo;
import org.flowable.common.engine.impl.javax.el.MethodNotFoundException;
import org.flowable.common.engine.impl.javax.el.ValueExpression;
import org.flowable.common.engine.impl.javax.el.VariableMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class MethodExpressionTest extends ELBaseTest {

    private static final String BUG53792 = "TEST_PASS";

    private ELContext context;
    private TesterBeanB beanB;

    @BeforeEach
    void setUp() {
        expressionFactory = createExpressionFactory();
        context = new SimpleContext();

        TesterBeanA beanA = new TesterBeanA();
        beanA.setName("A");
        context.getVariableMapper().setVariable("beanA", expressionFactory.createValueExpression(beanA, TesterBeanA.class));

        TesterBeanAA beanAA = new TesterBeanAA();
        beanAA.setName("AA");
        context.getVariableMapper().setVariable("beanAA", expressionFactory.createValueExpression(beanAA, TesterBeanAA.class));

        TesterBeanAAA beanAAA = new TesterBeanAAA();
        beanAAA.setName("AAA");
        context.getVariableMapper().setVariable("beanAAA", expressionFactory.createValueExpression(beanAAA, TesterBeanAAA.class));

        beanB = new TesterBeanB();
        beanB.setName("B");
        context.getVariableMapper().setVariable("beanB", expressionFactory.createValueExpression(beanB, TesterBeanB.class));

        TesterBeanBB beanBB = new TesterBeanBB();
        beanBB.setName("BB");
        context.getVariableMapper().setVariable("beanBB", expressionFactory.createValueExpression(beanBB, TesterBeanBB.class));

        TesterBeanBBB beanBBB = new TesterBeanBBB();
        beanBBB.setName("BBB");
        context.getVariableMapper().setVariable("beanBBB", expressionFactory.createValueExpression(beanBBB, TesterBeanBBB.class));

        TesterBeanC beanC = new TesterBeanC();
        context.getVariableMapper().setVariable("beanC", expressionFactory.createValueExpression(beanC, TesterBeanC.class));

        TesterBeanEnum beanEnum = new TesterBeanEnum();
        context.getVariableMapper().setVariable("beanEnum",
                expressionFactory.createValueExpression(beanEnum, TesterBeanEnum.class));
    }

    @Test
    void testIsParametersProvided() {
        MethodExpression me1 = expressionFactory.createMethodExpression(context, "${beanB.getName}", String.class,
                new Class<?>[] {});
        MethodExpression me2 = expressionFactory.createMethodExpression(context, "${beanB.sayHello('JUnit')}", String.class,
                new Class<?>[] { String.class });

        assertThat(me1.isParmetersProvided()).isFalse();
        assertThat(me2.isParmetersProvided()).isTrue();
    }

    @Test
    void testInvoke() {
        MethodExpression me1 = expressionFactory.createMethodExpression(context, "${beanB.getName}", String.class,
                new Class<?>[] {});
        MethodExpression me2 = expressionFactory.createMethodExpression(context, "${beanB.sayHello('JUnit')}", String.class,
                new Class<?>[] { String.class });
        MethodExpression me3 = expressionFactory.createMethodExpression(context, "${beanB.sayHello}", String.class,
                new Class<?>[] { String.class });

        assertThat(me1.invoke(context, null)).isEqualTo("B");
        assertThat(me2.invoke(context, null)).isEqualTo("Hello JUnit from B");
        assertThat(me2.invoke(context, new Object[] { "JUnit2" })).isEqualTo("Hello JUnit from B");
        assertThat(me3.invoke(context, new Object[] { "JUnit2" })).isEqualTo("Hello JUnit2 from B");
        assertThat(me2.invoke(context, new Object[] { null })).isEqualTo("Hello JUnit from B");
        // This one deviates from the Jakarta Expression Language spec due to legacy reasons within Flowable
        // In method invocations we are passing `null` instead of coercing to empty string for null
        assertThat(me3.invoke(context, new Object[] { null })).isEqualTo("Hello null from B");
    }

    @Test
    void testInvokeWithSuper() {
        MethodExpression me = expressionFactory.createMethodExpression(context, "${beanA.setBean(beanBB)}", null,
                new Class<?>[] { TesterBeanB.class });
        me.invoke(context, null);
        ValueExpression ve = expressionFactory.createValueExpression(context, "${beanA.bean.name}", String.class);
        Object r = ve.getValue(context);
        assertThat(r).isEqualTo("BB");
    }

    @Test
    void testInvokeWithSuperABNoReturnTypeNoParamTypes() {
        MethodExpression me2 = expressionFactory.createMethodExpression(context, "${beanC.sayHello(beanA,beanB)}", null, null);
        Object r2 = me2.invoke(context, null);
        assertThat(r2.toString()).isEqualTo("AB: Hello A from B");
    }

    @Test
    void testInvokeWithSuperABReturnTypeNoParamTypes() {
        MethodExpression me3 = expressionFactory.createMethodExpression(context, "${beanC.sayHello(beanA,beanB)}", String.class,
                null);
        Object r3 = me3.invoke(context, null);
        assertThat(r3.toString()).isEqualTo("AB: Hello A from B");
    }

    @Test
    void testInvokeWithSuperABNoReturnTypeParamTypes() {
        MethodExpression me4 = expressionFactory.createMethodExpression(context, "${beanC.sayHello(beanA,beanB)}", null,
                new Class<?>[] { TesterBeanA.class, TesterBeanB.class });
        Object r4 = me4.invoke(context, null);
        assertThat(r4.toString()).isEqualTo("AB: Hello A from B");
    }

    @Test
    void testInvokeWithSuperABReturnTypeParamTypes() {
        MethodExpression me5 = expressionFactory.createMethodExpression(context, "${beanC.sayHello(beanA,beanB)}", String.class,
                new Class<?>[] { TesterBeanA.class, TesterBeanB.class });
        Object r5 = me5.invoke(context, null);
        assertThat(r5.toString()).isEqualTo("AB: Hello A from B");
    }

    @Test
    void testInvokeWithSuperABB() {
        MethodExpression me6 = expressionFactory.createMethodExpression(context, "${beanC.sayHello(beanA,beanBB)}", null, null);
        Object r6 = me6.invoke(context, null);
        assertThat(r6.toString()).isEqualTo("ABB: Hello A from BB");
    }

    @Test
    void testInvokeWithSuperABBB() {
        MethodExpression me7 = expressionFactory.createMethodExpression(context, "${beanC.sayHello(beanA,beanBBB)}", null, null);
        Object r7 = me7.invoke(context, null);
        assertThat(r7.toString()).isEqualTo("ABB: Hello A from BBB");
    }

    @Test
    void testInvokeWithSuperAAB() {
        MethodExpression me8 = expressionFactory.createMethodExpression(context, "${beanC.sayHello(beanAA,beanB)}", null, null);
        Object r8 = me8.invoke(context, null);
        assertThat(r8.toString()).isEqualTo("AAB: Hello AA from B");
    }

    @Test
    void testInvokeWithSuperAABB() {
        MethodExpression me9 = expressionFactory.createMethodExpression(context, "${beanC.sayHello(beanAA,beanBB)}", null, null);
        assertThatThrownBy(() -> me9.invoke(context, null))
                .isInstanceOf(MethodNotFoundException.class)
                .hasMessage("Unable to find unambiguous method: class org.flowable.common.engine.impl.el.TesterBeanC.sayHello(org.flowable.common.engine.impl.el.TesterBeanAA, org.flowable.common.engine.impl.el.TesterBeanBB)");
    }

    @Test
    void testInvokeWithSuperAABBB() {
        // The Java compiler reports this as ambiguous. Using the parameter that
        // matches exactly seems reasonable to limit the scope of the method
        // search so the EL will find a match.
        MethodExpression me10 = expressionFactory.createMethodExpression(context, "${beanC.sayHello(beanAA,beanBBB)}", null,
                null);
        Object r10 = me10.invoke(context, null);
        assertThat(r10.toString()).isEqualTo("AAB: Hello AA from BBB");
    }

    @Test
    void testInvokeWithSuperAAAB() {
        MethodExpression me11 = expressionFactory.createMethodExpression(context, "${beanC.sayHello(beanAAA,beanB)}", null, null);
        Object r11 = me11.invoke(context, null);
        assertThat(r11.toString()).isEqualTo("AAB: Hello AAA from B");
    }

    @Test
    void testInvokeWithSuperAAABB() {
        // The Java compiler reports this as ambiguous. Using the parameter that
        // matches exactly seems reasonable to limit the scope of the method
        // search so the EL will find a match.
        MethodExpression me12 = expressionFactory.createMethodExpression(context, "${beanC.sayHello(beanAAA,beanBB)}", null,
                null);
        Object r12 = me12.invoke(context, null);
        assertThat(r12.toString()).isEqualTo("ABB: Hello AAA from BB");
    }

    @Test
    void testInvokeWithSuperAAABBB() {
        MethodExpression me13 = expressionFactory.createMethodExpression(context, "${beanC.sayHello(beanAAA,beanBBB)}", null, null);
        assertThatThrownBy(() -> me13.invoke(context, null))
                .isInstanceOf(MethodNotFoundException.class)
                .hasMessage("Unable to find unambiguous method: class org.flowable.common.engine.impl.el.TesterBeanC.sayHello(org.flowable.common.engine.impl.el.TesterBeanAAA, org.flowable.common.engine.impl.el.TesterBeanBBB)");
    }

    @Test
    void testInvokeWithVarArgsAB() {
        MethodExpression me1 = expressionFactory.createMethodExpression(context, "${beanC.sayHello(beanA,beanB,beanB)}", null, null);
        assertThatThrownBy(() -> me1.invoke(context, null))
                .isInstanceOf(MethodNotFoundException.class)
                .hasMessage("Method not found: class org.flowable.common.engine.impl.el.TesterBeanC.sayHello(org.flowable.common.engine.impl.el.TesterBeanA, org.flowable.common.engine.impl.el.TesterBeanB, org.flowable.common.engine.impl.el.TesterBeanB)");
    }

    @Test
    void testInvokeWithVarArgsABB() {
        MethodExpression me2 = expressionFactory.createMethodExpression(context, "${beanC.sayHello(beanA,beanBB,beanBB)}", null,
                null);
        Object r2 = me2.invoke(context, null);
        assertThat(r2.toString()).isEqualTo("ABB[]: Hello A from BB, BB");
    }

    @Test
    void testInvokeWithVarArgsABBB() {
        MethodExpression me3 = expressionFactory.createMethodExpression(context, "${beanC.sayHello(beanA,beanBBB,beanBBB)}", null,
                null);
        Object r3 = me3.invoke(context, null);
        assertThat(r3.toString()).isEqualTo("ABB[]: Hello A from BBB, BBB");
    }

    @Test
    void testInvokeWithVarArgsAAB() {
        MethodExpression me4 = expressionFactory.createMethodExpression(context, "${beanC.sayHello(beanAA,beanB,beanB)}", null,
                null);
        assertThatThrownBy(() -> me4.invoke(context, null))
                .isInstanceOf(MethodNotFoundException.class)
                .hasMessage("Method not found: class org.flowable.common.engine.impl.el.TesterBeanC.sayHello(org.flowable.common.engine.impl.el.TesterBeanAA, org.flowable.common.engine.impl.el.TesterBeanB, org.flowable.common.engine.impl.el.TesterBeanB)");
    }

    @Test
    void testInvokeWithVarArgsAABB() {
        MethodExpression me5 = expressionFactory.createMethodExpression(context, "${beanC.sayHello(beanAA,beanBB,beanBB)}", null,
                null);
        Object r5 = me5.invoke(context, null);
        assertThat(r5.toString()).isEqualTo("ABB[]: Hello AA from BB, BB");
    }

    @Test
    void testInvokeWithVarArgsAABBB() {
        MethodExpression me6 = expressionFactory.createMethodExpression(context, "${beanC.sayHello(beanAA,beanBBB,beanBBB)}",
                null, null);
        Object r6 = me6.invoke(context, null);
        assertThat(r6.toString()).isEqualTo("ABB[]: Hello AA from BBB, BBB");
    }

    @Test
    void testInvokeWithVarArgsAAAB() {
        MethodExpression me7 = expressionFactory.createMethodExpression(context, "${beanC.sayHello(beanAAA,beanB,beanB)}", null,
                null);
        assertThatThrownBy(() -> me7.invoke(context, null))
                .isInstanceOf(MethodNotFoundException.class)
                .hasMessage("Method not found: class org.flowable.common.engine.impl.el.TesterBeanC.sayHello(org.flowable.common.engine.impl.el.TesterBeanAAA, org.flowable.common.engine.impl.el.TesterBeanB, org.flowable.common.engine.impl.el.TesterBeanB)");
    }

    @Test
    void testInvokeWithVarArgsAAABB() {
        MethodExpression me8 = expressionFactory.createMethodExpression(context, "${beanC.sayHello(beanAAA,beanBB,beanBB)}", null,
                null);
        Object r8 = me8.invoke(context, null);
        assertThat(r8.toString()).isEqualTo("ABB[]: Hello AAA from BB, BB");
    }

    @Test
    void testInvokeWithVarArgsAAABBB() {
        MethodExpression me9 = expressionFactory.createMethodExpression(context, "${beanC.sayHello(beanAAA,beanBBB,beanBBB)}",
                null, null);
        Object r9 = me9.invoke(context, null);
        assertThat(r9.toString()).isEqualTo("ABB[]: Hello AAA from BBB, BBB");
    }

    /*
     * This is also tested implicitly in numerous places elsewhere in this class.
     */
    @Test
    void testBug49655() {
        // This is the call the failed
        MethodExpression me = expressionFactory.createMethodExpression(context, "#{beanA.setName('New value')}", null, null);
        // The rest is to check it worked correctly
        me.invoke(context, null);
        ValueExpression ve = expressionFactory.createValueExpression(context, "#{beanA.name}", String.class);
        assertThat(ve.<String>getValue(context)).isEqualTo("New value");
    }

    @Test
    void testBugPrimitives() {
        MethodExpression me = expressionFactory.createMethodExpression(context, "${beanA.setValLong(5)}", null, null);
        me.invoke(context, null);
        ValueExpression ve = expressionFactory.createValueExpression(context, "#{beanA.valLong}", String.class);
        assertThat(ve.<String>getValue(context)).isEqualTo("5");
    }

    @Test
    void testBug50449a() {
        MethodExpression me1 = expressionFactory.createMethodExpression(context, "${beanB.sayHello()}", null, null);
        String actual = (String) me1.invoke(context, null);
        assertThat(actual).isEqualTo("Hello from B");
    }

    @Test
    void testBug50449b() {
        MethodExpression me1 = expressionFactory.createMethodExpression(context, "${beanB.sayHello('Tomcat')}", null, null);
        String actual = (String) me1.invoke(context, null);
        assertThat(actual).isEqualTo("Hello Tomcat from B");
    }

    @Test
    void testBug50790a() {
        ValueExpression ve = expressionFactory.createValueExpression(context, "#{beanAA.name.contains(beanA.name)}", Boolean.class);
        Boolean actual = (Boolean) ve.getValue(context);
        assertThat(actual).isTrue();
    }

    @Test
    void testBug50790b() {
        ValueExpression ve = expressionFactory.createValueExpression(context, "#{beanA.name.contains(beanAA.name)}", Boolean.class);
        Boolean actual = (Boolean) ve.getValue(context);
        assertThat(actual).isFalse();
    }

    @Test
    void testBug52445a() {
        MethodExpression me = expressionFactory.createMethodExpression(context, "${beanA.setBean(beanBB)}", null,
                new Class<?>[] { TesterBeanB.class });
        me.invoke(context, null);

        MethodExpression me1 = expressionFactory.createMethodExpression(context, "${beanA.bean.sayHello()}", null, null);
        String actual = (String) me1.invoke(context, null);
        assertThat(actual).isEqualTo("Hello from BB");
    }

    @Test
    void testBug52970() {
        MethodExpression me = expressionFactory.createMethodExpression(context, "${beanEnum.submit('APPLE')}", null,
                new Class<?>[] { TesterBeanEnum.class });
        me.invoke(context, null);

        ValueExpression ve = expressionFactory.createValueExpression(context, "#{beanEnum.lastSubmitted}", TesterEnum.class);
        TesterEnum actual = (TesterEnum) ve.getValue(context);
        assertThat(actual).isEqualTo(TesterEnum.APPLE);

    }

    @Test
    void testBug53792a() {
        MethodExpression me = expressionFactory.createMethodExpression(context, "${beanA.setBean(beanB)}", null,
                new Class<?>[] { TesterBeanB.class });
        me.invoke(context, null);
        me = expressionFactory.createMethodExpression(context, "${beanB.setName('" + BUG53792 + "')}", null,
                new Class<?>[] { TesterBeanB.class });
        me.invoke(context, null);

        ValueExpression ve = expressionFactory.createValueExpression(context, "#{beanA.getBean().name}", String.class);
        String actual = (String) ve.getValue(context);
        assertThat(actual).isEqualTo(BUG53792);
    }

    @Test
    void testBug53792b() {
        MethodExpression me = expressionFactory.createMethodExpression(context, "${beanA.setBean(beanB)}", null,
                new Class<?>[] { TesterBeanB.class });
        me.invoke(context, null);
        me = expressionFactory.createMethodExpression(context, "${beanB.setName('" + BUG53792 + "')}", null,
                new Class<?>[] { TesterBeanB.class });
        me.invoke(context, null);

        ValueExpression ve = expressionFactory.createValueExpression(context, "#{beanA.getBean().name.length()}", Integer.class);
        Integer actual = (Integer) ve.getValue(context);
        assertThat(actual).isEqualTo(BUG53792.length());
    }

    @Test
    void testBug53792c() {
        MethodExpression me = expressionFactory.createMethodExpression(context, "#{beanB.sayHello().length()}", null,
                new Class<?>[] {});
        Integer result = (Integer) me.invoke(context, null);
        assertThat(result).isEqualTo(beanB.sayHello().length());
    }

    @Test
    void testBug53792d() {
        MethodExpression me = expressionFactory.createMethodExpression(context, "#{beanB.sayHello().length()}", null,
                new Class<?>[] {});
        Integer result = (Integer) me.invoke(context, new Object[] { "foo" });
        assertThat(result).isEqualTo(beanB.sayHello().length());
    }

    @Test
    void testBug56797a() {
        MethodExpression me = expressionFactory.createMethodExpression(context, "${beanAA.echo1('Hello World!')}", null, null);
        Object r = me.invoke(context, null);
        assertThat(r.toString()).isEqualTo("AA1Hello World!");
    }

    @Test
    void testBug56797b() {
        MethodExpression me = expressionFactory.createMethodExpression(context, "${beanAA.echo2('Hello World!')}", null, null);
        Object r = me.invoke(context, null);
        assertThat(r.toString()).isEqualTo("AA2Hello World!");
    }

    @Test
    void testBug57855a() {
        MethodExpression me = expressionFactory.createMethodExpression(context, "${beanAA.echo2}", null,
                new Class[] { String.class });
        assertThatThrownBy(() -> me.invoke(context, new Object[0]))
                .isInstanceOf(ELException.class)
                .hasMessage("Error invoking method 'echo2' in 'class org.flowable.common.engine.impl.el.TesterBeanAA'")
                .cause()
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Disabled("We do not use MethodExpression")
    void testBug57855b() {
        MethodExpression me = expressionFactory.createMethodExpression(context, "${beanAA.echo2}", null,
                new Class[] { String.class });
        assertThatThrownBy(() -> me.invoke(context, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("");
    }

    @Test
    @Disabled("We do not use MethodExpression")
    void testBug57855c() {
        MethodExpression me = expressionFactory.createMethodExpression(context, "${beanB.echo}", null,
                new Class[] { String.class });
        me.invoke(context, null);
    }

    @Test
    void testBug57855d() {
        MethodExpression me = expressionFactory.createMethodExpression(context, "${beanB.echo('aaa')}", null,
                new Class[] { String.class });
        Object r = me.invoke(context, null);
        assertThat(r.toString()).isEqualTo("aaa");
    }

    @Test
    void testBug57855e() {
        MethodExpression me = expressionFactory.createMethodExpression(context, "${beanB.echo}", null,
                new Class[] { String.class });
        assertThatThrownBy(() -> me.invoke(context, new String[] { "aaa", "bbb" }))
                .isInstanceOf(MethodNotFoundException.class)
                .hasMessage("Cannot find method 'echo' in 'class org.flowable.common.engine.impl.el.TesterBeanB'");
    }

    @Test
    @Disabled("We do not use MethodExpression")
    void testBug60844() {
        MethodExpression me2 = expressionFactory.createMethodExpression(context, "${beanC.sayHello}", null,
                new Class[] { TesterBeanA.class, TesterBeanB.class });
        Assertions.assertThatThrownBy(() -> me2.invoke(context, new Object[] { new Object() }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("");
    }

    @Test
    void testVarArgsBeanFEnum() {
        doTestVarArgsBeanF("beanF.doTest(apple)", (a) -> a.doTest(TesterEnum.APPLE));
    }

    @Test
    void testVarArgsBeanFEnumEnum() {
        doTestVarArgsBeanF("beanF.doTest(apple,apple)", (a) -> a.doTest(TesterEnum.APPLE, TesterEnum.APPLE));
    }

    @Test
    void testVarArgsBeanFEnumString() {
        doTestVarArgsBeanF("beanF.doTest(apple,'apple')", (a) -> a.doTest(TesterEnum.APPLE, "apple"));
    }

    @Test
    void testVarArgsBeanFEnumVEnum() {
        doTestVarArgsBeanF("beanF.doTest(apple,apple,apple)",
                (a) -> a.doTest(TesterEnum.APPLE, TesterEnum.APPLE, TesterEnum.APPLE));
    }

    @Test
    void testVarArgsBeanFEnumVString() {
        doTestVarArgsBeanF("beanF.doTest(apple,'apple','apple')", (a) -> a.doTest(TesterEnum.APPLE, "apple", "apple"));
    }

    @Test
    void testVarArgsBeanFString() {
        doTestVarArgsBeanF("beanF.doTest('apple')", (a) -> a.doTest("apple"));
    }

    @Test
    void testVarArgsBeanFStringEnum() {
        doTestVarArgsBeanF("beanF.doTest('apple',apple)", (a) -> a.doTest("apple", TesterEnum.APPLE));
    }

    @Test
    void testVarArgsBeanFStringString() {
        doTestVarArgsBeanF("beanF.doTest('apple','apple')", (a) -> a.doTest("apple", "apple"));
    }

    @Test
    void testVarArgsBeanFStringVEnum() {
        doTestVarArgsBeanF("beanF.doTest('apple',apple,apple)",
                (a) -> a.doTest("apple", TesterEnum.APPLE, TesterEnum.APPLE));
    }

    @Test
    void testVarArgsBeanFStringVString() {
        doTestVarArgsBeanF("beanF.doTest('apple','apple','apple')", (a) -> a.doTest("apple", "apple", "apple"));
    }

    private void doTestVarArgsBeanF(String expression, Function<TesterBeanF, String> func) {
        VariableMapper variableMapper = context.getVariableMapper();
        variableMapper.setVariable("apple", expressionFactory.createValueExpression(TesterEnum.APPLE, TesterEnum.class));
        variableMapper.setVariable("beanF", expressionFactory.createValueExpression(new TesterBeanF(), TesterBeanF.class));
        try {
            String elResult = (String) eval(expression, context);
            String javaResult = func.apply(new TesterBeanF());
            assertThat(elResult).isEqualTo(javaResult);
        } finally {
            variableMapper.setVariable("apple", null);
            variableMapper.setVariable("beanF", null);
        }
    }

    @Test
    void testVarArgsBeanGEnum() {
        doTestVarArgsBeanG("beanG.doTest(apple)", (a) -> a.doTest("apple"));
    }

    @Test
    void testVarArgsBeanGEnumEnum() {
        doTestVarArgsBeanG("beanG.doTest(apple,apple)", (a) -> a.doTest("apple", "apple"));
    }

    @Test
    void testVarArgsBeanGEnumString() {
        doTestVarArgsBeanG("beanG.doTest(apple,'apple')", (a) -> a.doTest("apple", "apple"));
    }

    @Test
    void testVarArgsBeanGEnumVEnum() {
        doTestVarArgsBeanG("beanG.doTest(apple,apple,apple)", (a) -> a.doTest("apple", "apple", "apple"));
    }

    @Test
    void testVarArgsBeanGEnumVString() {
        doTestVarArgsBeanG("beanG.doTest(apple,'apple','apple')", (a) -> a.doTest("apple", "apple", "apple"));
    }

    @Test
    void testVarArgsBeanGString() {
        doTestVarArgsBeanG("beanG.doTest('apple')", (a) -> a.doTest("apple"));
    }

    @Test
    void testVarArgsBeanGStringEnum() {
        doTestVarArgsBeanG("beanG.doTest('apple',apple)", (a) -> a.doTest("apple", "apple"));
    }

    @Test
    void testVarArgsBeanGStringString() {
        doTestVarArgsBeanG("beanG.doTest('apple','apple')", (a) -> a.doTest("apple", "apple"));
    }

    @Test
    void testVarArgsBeanGStringVEnum() {
        doTestVarArgsBeanG("beanG.doTest('apple',apple,apple)", (a) -> a.doTest("apple", "apple", "apple"));
    }

    @Test
    void testVarArgsBeanGStringVString() {
        doTestVarArgsBeanG("beanG.doTest('apple','apple','apple')", (a) -> a.doTest("apple", "apple", "apple"));
    }

    private void doTestVarArgsBeanG(String expression, Function<TesterBeanG, String> func) {
        VariableMapper variableMapper = context.getVariableMapper();
        variableMapper.setVariable("apple", expressionFactory.createValueExpression(TesterEnum.APPLE, TesterEnum.class));
        variableMapper.setVariable("beanG", expressionFactory.createValueExpression(new TesterBeanG(), TesterBeanG.class));
        try {
            String elResult = (String) eval(expression, context);
            String javaResult = func.apply(new TesterBeanG());
            assertThat(elResult).isEqualTo(javaResult);
        } finally {
            variableMapper.setVariable("apple", null);
            variableMapper.setVariable("beanG", null);
        }
    }

    @Test
    void testVarArgsBeanHEnum() {
        doTestVarArgsBeanH("beanH.doTest(apple)", (a) -> a.doTest("apple"));
    }

    @Test
    void testVarArgsBeanHEnumEnum() {
        doTestVarArgsBeanH("beanH.doTest(apple,apple)", (a) -> a.doTest("apple", "apple"));
    }

    @Test
    void testVarArgsBeanHEnumString() {
        doTestVarArgsBeanH("beanH.doTest(apple,'apple')", (a) -> a.doTest("apple", "apple"));
    }

    @Test
    void testVarArgsBeanHEnumVEnum() {
        doTestVarArgsBeanH("beanH.doTest(apple,apple,apple)", (a) -> a.doTest("apple", "apple", "apple"));
    }

    @Test
    void testVarArgsBeanHEnumVString() {
        doTestVarArgsBeanH("beanH.doTest(apple,'apple','apple')", (a) -> a.doTest("apple", "apple", "apple"));
    }

    @Test
    void testVarArgsBeanHString() {
        doTestVarArgsBeanH("beanH.doTest('apple')", (a) -> a.doTest("apple"));
    }

    @Test
    void testVarArgsBeanHStringEnum() {
        doTestVarArgsBeanH("beanH.doTest('apple',apple)", (a) -> a.doTest("apple", "apple"));
    }

    @Test
    void testVarArgsBeanHStringString() {
        doTestVarArgsBeanH("beanH.doTest('apple','apple')", (a) -> a.doTest("apple", "apple"));
    }

    @Test
    void testVarArgsBeanHStringVEnum() {
        doTestVarArgsBeanH("beanH.doTest('apple',apple,apple)", (a) -> a.doTest("apple", "apple", "apple"));
    }

    @Test
    void testVarArgsBeanHStringVString() {
        doTestVarArgsBeanH("beanH.doTest('apple','apple','apple')", (a) -> a.doTest("apple", "apple", "apple"));
    }

    private void doTestVarArgsBeanH(String expression, Function<TesterBeanH, String> func) {
        VariableMapper variableMapper = context.getVariableMapper();
        variableMapper.setVariable("apple", expressionFactory.createValueExpression(TesterEnum.APPLE, TesterEnum.class));
        variableMapper.setVariable("beanH", expressionFactory.createValueExpression(new TesterBeanH(), TesterBeanH.class));
        try {
            String elResult = (String) eval(expression, context);
            String javaResult = func.apply(new TesterBeanH());
            assertThat(elResult).isEqualTo(javaResult);
        } finally {
            variableMapper.setVariable("apple", null);
            variableMapper.setVariable("beanH", null);
        }
    }

    @Test
    void testPreferNoVarArgs() {
        VariableMapper variableMapper = context.getVariableMapper();
        TesterBeanAAA bean = new TesterBeanAAA();
        bean.setName("xyz");
        variableMapper.setVariable("bean2", expressionFactory.createValueExpression(bean, TesterBeanAAA.class));
        variableMapper.setVariable("bean1", expressionFactory.createValueExpression(new TesterBeanI(), TesterBeanI.class));
        try {
            String elResult = (String) eval("bean1.echo(bean2)", context);
            assertThat(elResult).isEqualTo("No varargs: xyz");
        } finally {
            variableMapper.setVariable("bean2", null);
            variableMapper.setVariable("bean1", null);
        }
    }

    @Test
    @Disabled("We don't need the method info implementation")
    void testGetMethodInfo01() {
        MethodExpression me = expressionFactory.createMethodExpression(context, "#{beanA.setName('New value')}", null, null);
        // This is the call that failed
        MethodInfo mi = me.getMethodInfo(context);
        // The rest is to check it worked correctly
        assertThat(mi.getReturnType()).isEqualTo(void.class);
        assertThat(mi.getParamTypes())
                .containsExactly(String.class);
    }

    @Test
    void testGetMethodInfo02() {
        MethodExpression me = expressionFactory.createMethodExpression(context, "#{beanA.setName}", null,
                new Class[] { String.class });
        // This is the call that failed
        MethodInfo mi = me.getMethodInfo(context);
        // The rest is to check it worked correctly
        assertThat(mi.getReturnType()).isEqualTo(void.class);
        assertThat(mi.getParamTypes())
                .containsExactly(String.class);
    }

    // Flowable Specific Test
    @Test
    void testMultipleMethodsWithInheritance() {
        VariableMapper variableMapper = context.getVariableMapper();
        variableMapper.setVariable("base", expressionFactory.createValueExpression(new BaseImpl(), BaseImpl.class));
        variableMapper.setVariable("testAmbiguousMethodSingleParameterBean",
                expressionFactory.createValueExpression(new TestAmbiguousMethodSingleParameterBean(), TestAmbiguousMethodSingleParameterBean.class));
        try {
            String elResult = (String) eval("testAmbiguousMethodSingleParameterBean.run(base)", context);
            assertThat(elResult).isEqualTo("baseB");
        } finally {
            variableMapper.setVariable("base", null);
            variableMapper.setVariable("testAmbiguousMethodSingleParameterBean", null);
        }
    }

    static class TestAmbiguousMethodSingleParameterBean {

        public String run(BaseA base) {
            return "baseA";
        }

        public String run(BaseB base) {
            return "baseB";
        }
    }

    interface BaseA {

    }

    interface BaseB extends BaseA {

    }

    public static class BaseImpl implements BaseB {

    }
}
