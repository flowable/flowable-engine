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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.flowable.common.engine.impl.javax.el.ELBaseTest;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ExpressionFactory;
import org.flowable.common.engine.impl.javax.el.ValueExpression;
import org.flowable.common.engine.impl.javax.el.ValueReference;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ValueExpressionTest extends ELBaseTest {

    @Test
    void testGetValueReference() {
        ExpressionFactory factory = createExpressionFactory();
        ELContext context = new SimpleContext();

        TesterBeanB beanB = new TesterBeanB();
        beanB.setName("Tomcat");
        ValueExpression var = factory.createValueExpression(beanB, TesterBeanB.class);
        context.getVariableMapper().setVariable("beanB", var);

        ValueExpression ve = factory.createValueExpression(context, "${beanB.name}", String.class);

        // First check the basics work
        String result = (String) ve.getValue(context);
        assertThat(result).isEqualTo("Tomcat");

        // Now check the value reference
        ValueReference vr = ve.getValueReference(context);
        assertThat(vr).isNotNull();

        assertThat(vr.getBase()).isEqualTo(beanB);
        assertThat(vr.getProperty()).isEqualTo("name");
    }

    @Test
    void testGetValueReferenceVariable() {
        ExpressionFactory factory = createExpressionFactory();
        ELContext context = new SimpleContext();

        TesterBeanB beanB = new TesterBeanB();
        beanB.setName("Tomcat");
        ValueExpression var = factory.createValueExpression(beanB, TesterBeanB.class);
        context.getVariableMapper().setVariable("beanB", var);

        ValueExpression var2 = factory.createValueExpression(context, "${beanB.name}", String.class);

        context.getVariableMapper().setVariable("foo", var2);

        ValueExpression ve = factory.createValueExpression(context, "${foo}", ValueExpression.class);

        // Now check the value reference
        ValueReference vr = ve.getValueReference(context);
        assertThat(vr).isNotNull();

        assertThat(vr.getBase()).isEqualTo(beanB);
        assertThat(vr.getProperty()).isEqualTo("name");
    }

    @Test
    void testBug49345() {
        ExpressionFactory factory = createExpressionFactory();
        ELContext context = new SimpleContext();

        TesterBeanA beanA = new TesterBeanA();
        TesterBeanB beanB = new TesterBeanB();
        beanB.setName("Tomcat");
        beanA.setBean(beanB);

        ValueExpression var = factory.createValueExpression(beanA, TesterBeanA.class);
        context.getVariableMapper().setVariable("beanA", var);

        ValueExpression ve = factory.createValueExpression(context, "${beanA.bean.name}", String.class);

        // First check the basics work
        String result = (String) ve.getValue(context);
        assertThat(result).isEqualTo("Tomcat");

        // Now check the value reference
        ValueReference vr = ve.getValueReference(context);
        assertThat(vr).isNotNull();

        assertThat(vr.getBase()).isEqualTo(beanB);
        assertThat(vr.getProperty()).isEqualTo("name");
    }

    @Test
    void testBug50105() {
        ExpressionFactory factory = createExpressionFactory();
        ELContext context = new SimpleContext();

        TesterEnum testEnum = TesterEnum.APPLE;

        ValueExpression var = factory.createValueExpression(testEnum, TesterEnum.class);
        context.getVariableMapper().setVariable("testEnum", var);

        // When coercing an Enum to a String, name() should always be used.
        ValueExpression ve1 = factory.createValueExpression(context, "${testEnum}", String.class);
        String result1 = (String) ve1.getValue(context);
        assertThat(result1).isEqualTo("APPLE");

        ValueExpression ve2 = factory.createValueExpression(context, "foo${testEnum}bar", String.class);
        String result2 = (String) ve2.getValue(context);
        assertThat(result2).isEqualTo("fooAPPLEbar");
    }

    @Test
    void testBug51177ObjectMap() {
        ExpressionFactory factory = createExpressionFactory();
        ELContext context = new SimpleContext();

        Object o1 = "String value";
        Object o2 = 32;

        Map<Object, Object> map = new HashMap<>();
        map.put("key1", o1);
        map.put("key2", o2);

        ValueExpression var = factory.createValueExpression(map, Map.class);
        context.getVariableMapper().setVariable("map", var);

        ValueExpression ve1 = factory.createValueExpression(context, "${map.key1}", Object.class);
        ve1.setValue(context, o2);
        assertThat(ve1.<Object>getValue(context)).isEqualTo(o2);

        ValueExpression ve2 = factory.createValueExpression(context, "${map.key2}", Object.class);
        ve2.setValue(context, o1);
        assertThat(ve2.<Object>getValue(context)).isEqualTo(o1);
    }

    @Test
    void testBug51177ObjectList() {
        ExpressionFactory factory = createExpressionFactory();
        ELContext context = new SimpleContext();

        Object o1 = "String value";
        Object o2 = 32;

        List<Object> list = new ArrayList<>();
        list.add(0, o1);
        list.add(1, o2);

        ValueExpression var = factory.createValueExpression(list, List.class);
        context.getVariableMapper().setVariable("list", var);

        ValueExpression ve1 = factory.createValueExpression(context, "${list[0]}", Object.class);
        ve1.setValue(context, o2);
        assertThat(ve1.<Object>getValue(context)).isEqualTo(o2);

        ValueExpression ve2 = factory.createValueExpression(context, "${list[1]}", Object.class);
        ve2.setValue(context, o1);
        assertThat(ve2.<Object>getValue(context)).isEqualTo(o1);
    }

    /*
     * Test returning an empty list as a bean property.
     */
    @Test
    void testBug51544Bean() {
        ExpressionFactory factory = createExpressionFactory();
        ELContext context = new SimpleContext();

        TesterBeanA beanA = new TesterBeanA();
        beanA.setValList(Collections.emptyList());

        ValueExpression var = factory.createValueExpression(beanA, TesterBeanA.class);
        context.getVariableMapper().setVariable("beanA", var);

        ValueExpression ve = factory.createValueExpression(context, "${beanA.valList.size()}", Integer.class);

        Integer result = (Integer) ve.getValue(context);
        assertThat(result).isEqualTo(Integer.valueOf(0));
    }

    /*
     * Test using list directly as variable.
     */
    @Test
    void testBug51544Direct() {
        ExpressionFactory factory = createExpressionFactory();
        ELContext context = new SimpleContext();

        List<?> list = Collections.emptyList();

        ValueExpression var = factory.createValueExpression(list, List.class);
        context.getVariableMapper().setVariable("list", var);

        ValueExpression ve = factory.createValueExpression(context, "${list.size()}", Integer.class);

        Integer result = (Integer) ve.getValue(context);
        assertThat(result).isEqualTo(Integer.valueOf(0));
    }

    @Test
    void testBug56522SetNullValue() {
        ExpressionFactory factory = createExpressionFactory();
        ELContext context = new SimpleContext();

        TesterBeanB beanB = new TesterBeanB();
        beanB.setName("Tomcat");
        ValueExpression var = factory.createValueExpression(beanB, TesterBeanB.class);
        context.getVariableMapper().setVariable("beanB", var);

        ValueExpression ve = factory.createValueExpression(context, "${beanB.name}", String.class);

        // First check the basics work
        String result = (String) ve.getValue(context);
        assertThat(result).isEqualTo("Tomcat");

        // Now set the value to null
        ve.setValue(context, null);

        // This one deviates from the Jakarta Expression Language spec due to legacy reasons within Flowable
        // In method invocations we are passing `null` instead of coercing to empty string for null
        assertThat(beanB.getName()).isNull();
    }

    @Test
    @Disabled("Not yet supported")
    void testOptional01() {
        ExpressionFactory factory = createExpressionFactory();
        ELContext context = new SimpleContext();

        final String data = "some data";

        TesterBeanJ beanJ = new TesterBeanJ();
        TesterBeanJ beanJ2 = new TesterBeanJ();
        beanJ2.setData(data);
        beanJ.setBean(beanJ2);

        ValueExpression var = factory.createValueExpression(beanJ, TesterBeanJ.class);
        context.getVariableMapper().setVariable("beanJ", var);

        ValueExpression ve = factory.createValueExpression(context, "${beanJ.optionalBean.map(b -> b.data)}",
                Optional.class);

        @SuppressWarnings("unchecked")
        Optional<String> result = (Optional<String>) ve.getValue(context);
        assertThat(result).hasValue(data);
    }

    @Test
    @Disabled("Not yet supported")
    void testOptional02() {
        ExpressionFactory factory = createExpressionFactory();
        ELContext context = new SimpleContext();

        TesterBeanJ beanJ = new TesterBeanJ();

        ValueExpression var = factory.createValueExpression(beanJ, TesterBeanJ.class);
        context.getVariableMapper().setVariable("beanJ", var);

        ValueExpression ve = factory.createValueExpression(context, "${beanJ.optionalBean.map(b -> b.data)}",
                Optional.class);

        @SuppressWarnings("unchecked")
        Optional<String> result = (Optional<String>) ve.getValue(context);
        assertThat(result).isEmpty();
    }

    @Test
    @Disabled("Not yet supported")
    void testOptional03() {
        ExpressionFactory factory = createExpressionFactory();
        ELContext context = new SimpleContext();

        final String data = "some data";

        TesterBeanJ beanJ = new TesterBeanJ();
        TesterBeanJ beanJ2 = new TesterBeanJ();
        beanJ2.setData(data);
        beanJ.setBean(beanJ2);

        ValueExpression var = factory.createValueExpression(beanJ, TesterBeanJ.class);
        context.getVariableMapper().setVariable("beanJ", var);

        ValueExpression ve = factory.createValueExpression(context, "${beanJ.optionalBean.get().data}", String.class);

        String result = (String) ve.getValue(context);
        assertThat(result).isEqualTo(data);
    }

    @Test
    @Disabled("Not yet supported")
    void testOptional04() {
        ExpressionFactory factory = createExpressionFactory();
        ELContext context = new SimpleContext();

        TesterBeanJ beanJ = new TesterBeanJ();

        ValueExpression var = factory.createValueExpression(beanJ, TesterBeanJ.class);
        context.getVariableMapper().setVariable("beanJ", var);

        ValueExpression ve = factory.createValueExpression(context,
                "${beanJ.optionalBean.map(b -> b.data).orElse(null)}", String.class);

        String result = (String) ve.getValue(context);
        // Result is null but is coerced to String which makes it ""
        assertThat(result)
                .isNotNull()
                .isEmpty();
    }

    @Test
    @Disabled("Not yet supported")
    void testArrayLength01() {
        ExpressionFactory factory = createExpressionFactory();
        ELContext context = new SimpleContext();

        TesterBeanA beanA = new TesterBeanA();
        beanA.setValArray(new String[3]);

        ValueExpression var = factory.createValueExpression(beanA, TesterBeanA.class);
        context.getVariableMapper().setVariable("beanA", var);

        ValueExpression ve = factory.createValueExpression(context, "${beanA.valArray.length}", Integer.class);

        // Check the result
        Integer result = (Integer) ve.getValue(context);
        assertThat(result).isEqualTo(Integer.valueOf(3));
    }

    @Test
    @Disabled("Not yet supported")
    void testArrayLength02() {
        ExpressionFactory factory = createExpressionFactory();
        ELContext context = new SimpleContext();

        TesterBeanA beanA = new TesterBeanA();
        beanA.setValArray(new String[0]);

        ValueExpression var = factory.createValueExpression(beanA, TesterBeanA.class);
        context.getVariableMapper().setVariable("beanA", var);

        ValueExpression ve = factory.createValueExpression(context, "${beanA.valArray.length}", Integer.class);

        // Check the result
        Integer result = (Integer) ve.getValue(context);
        assertThat(result).isEqualTo(Integer.valueOf(0));
    }

    @Test
    @Disabled("Not yet supported")
    void testArrayLength03() {
        ExpressionFactory factory = createExpressionFactory();
        ELContext context = new SimpleContext();

        TesterBeanA beanA = new TesterBeanA();

        ValueExpression var = factory.createValueExpression(beanA, TesterBeanA.class);
        context.getVariableMapper().setVariable("beanA", var);

        ValueExpression ve = factory.createValueExpression(context, "${beanA.valArray.length}", Integer.class);

        // Check the result
        Integer result = (Integer) ve.getValue(context);
        assertThat(result).isNull();
    }
}
