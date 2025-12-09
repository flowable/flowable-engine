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
package org.flowable.common.engine.impl.el.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.flowable.common.engine.impl.javax.el.ELBaseTest;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ELException;
import org.flowable.common.engine.impl.javax.el.ExpressionFactory;
import org.flowable.common.engine.impl.javax.el.ValueExpression;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ELParserTest extends ELBaseTest {

    @Test
    void testBug49081() {
        // OP's report
        testExpression("#${1+1}", "#2");

        // Variations on a theme
        testExpression("#", "#");
        testExpression("##", "##");
        testExpression("###", "###");
        testExpression("$", "$");
        testExpression("$$", "$$");
        testExpression("$$$", "$$$");
        testExpression("#$", "#$");
        testExpression("#$#", "#$#");
        testExpression("$#", "$#");
        testExpression("$#$", "$#$");

        testExpression("#{1+1}", "2");
        testExpression("##{1+1}", "#2");
        testExpression("###{1+1}", "##2");
        testExpression("${1+1}", "2");
        testExpression("$${1+1}", "$2");
        testExpression("$$${1+1}", "$$2");
        testExpression("#${1+1}", "#2");
        testExpression("#$#{1+1}", "#$2");
        testExpression("$#{1+1}", "$2");
        testExpression("$#${1+1}", "$#2");
    }

    @Test
    @Disabled("We do not have a limit on where you can use 'int'")
    void testJavaKeyWordSuffix() {
        ExpressionFactory factory = createExpressionFactory();
        ELContext context = new SimpleContext();

        TesterBeanA beanA = new TesterBeanA();
        beanA.setInt("five");
        ValueExpression var = factory.createValueExpression(beanA, TesterBeanA.class);
        context.getVariableMapper().setVariable("beanA", var);

        // Should fail
        assertThatThrownBy(() -> factory.createValueExpression(context, "${beanA.int}", String.class))
                .isInstanceOf(ELException.class);
    }

    @Test
    @Disabled("We do not have a limit on where you can use 'this'")
    void testJavaKeyWordIdentifier() {
        ExpressionFactory factory = createExpressionFactory();
        ELContext context = new SimpleContext();

        TesterBeanA beanA = new TesterBeanA();
        beanA.setInt("five");
        ValueExpression var = factory.createValueExpression(beanA, TesterBeanA.class);
        context.getVariableMapper().setVariable("this", var);

        // Should fail
        assertThatThrownBy(() -> factory.createValueExpression(context, "${this}", String.class))
                .isInstanceOf(ELException.class);
    }

    @Test
    void bug56179a() {
        doTestBug56179(0, "test == true");
    }

    @Test
    void bug56179b() {
        doTestBug56179(1, "test == true");
    }

    @Test
    void bug56179c() {
        doTestBug56179(2, "test == true");
    }

    @Test
    void bug56179d() {
        doTestBug56179(3, "test == true");
    }

    @Test
    void bug56179e() {
        doTestBug56179(4, "test == true");
    }

    @Test
    void bug56179f() {
        doTestBug56179(5, "test == true");
    }

    @Test
    void bug56179g() {
        doTestBug56179(0, "(test) == true");
    }

    @Test
    void bug56179h() {
        doTestBug56179(1, "(test) == true");
    }

    @Test
    void bug56179i() {
        doTestBug56179(2, "(test) == true");
    }

    @Test
    void bug56179j() {
        doTestBug56179(3, "(test) == true");
    }

    @Test
    void bug56179k() {
        doTestBug56179(4, "(test) == true");
    }

    @Test
    void bug56179l() {
        doTestBug56179(5, "(test) == true");
    }

    @Test
    void bug56179m() {
        doTestBug56179(5, "((test)) == true");
    }

    @Test
    void bug56179n() {
        doTestBug56179(5, "(((test))) == true");
    }

    private void doTestBug56179(int parenthesesCount, String innerExpr) {
        ExpressionFactory factory = createExpressionFactory();
        ELContext context = new SimpleContext();

        ValueExpression var = factory.createValueExpression(Boolean.TRUE, Boolean.class);
        context.getVariableMapper().setVariable("test", var);

        String expr = "${"
                + "(".repeat(Math.max(0, parenthesesCount))
                + innerExpr
                + ")".repeat(Math.max(0, parenthesesCount))
                + '}';
        ValueExpression ve = factory.createValueExpression(context, expr, String.class);

        String result = (String) ve.getValue(context);
        assertThat(result).isEqualTo("true");
    }

    @Test
    void bug56185() {
        ExpressionFactory factory = createExpressionFactory();
        ELContext context = new SimpleContext();

        TesterBeanC beanC = new TesterBeanC();
        ValueExpression var = factory.createValueExpression(beanC, TesterBeanC.class);
        context.getVariableMapper().setVariable("myBean", var);

        ValueExpression ve = factory.createValueExpression(context,
                "${(myBean.int1 > 1 and myBean.myBool) or " +
                        "((myBean.myBool or myBean.myBool1) and myBean.int1 > 1)}",
                Boolean.class);
        assertThat(ve.<Object>getValue(context)).isEqualTo(Boolean.FALSE);
        beanC.setInt1(2);
        beanC.setMyBool1(true);
        assertThat(ve.<Object>getValue(context)).isEqualTo(Boolean.TRUE);
    }

    private void testExpression(String expression, String expected) {
        ExpressionFactory factory = createExpressionFactory();
        ELContext context = new SimpleContext();

        ValueExpression ve = factory.createValueExpression(
                context, expression, String.class);

        assertThat(ve.<String>getValue(context)).isEqualTo(expected);
    }
}
