/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.common.engine.impl.el.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.flowable.common.engine.impl.el.BaseElTest;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ELException;
import org.flowable.common.engine.impl.javax.el.ExpressionFactory;
import org.flowable.common.engine.impl.javax.el.ValueExpression;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

class AstIdentifierTest extends BaseElTest {

    @Test
    @Disabled("We do not support import handlers")
    void testImport01() {
        Object result = eval("Integer.MAX_VALUE", Integer.class);
        assertThat(result).isEqualTo(Integer.MAX_VALUE);
    }

    // We do not support import handlers yet
    //@Test
    //void testImport02() {
    //    ELProcessor processor = new ELProcessor();
    //    processor.getELManager().getELContext().getImportHandler().importStatic(
    //            "java.lang.Integer.MAX_VALUE");
    //    Object result =
    //            processor.getValue("MAX_VALUE",
    //                    Integer.class);
    //    Assert.assertEquals(Integer.valueOf(Integer.MAX_VALUE), result);
    //}

    @Test
    @DisabledForJreRange(min = JRE.JAVA_24)
    void testIdentifierStart() {
        /*
         * This test only works on Java 21 to Java 23.
         *
         * In Java 24, the definition of Java Letter and/or Java Digit has changed.
         */
        for (int i = 0; i < 0xFFFF; i++) {
            if (Character.isJavaIdentifierStart(i)) {
                testIdentifier((char) i, 'b');
            } else {
                try {
                    testIdentifier((char) i, 'b');
                } catch (ELException e) {
                    continue;
                }
                fail("Expected EL exception for [" + i + "], [" + (char) i + "]");
            }
        }
    }

    @Test
    @DisabledForJreRange(min = JRE.JAVA_24)
    void testIdentifierPart() {
        /*
         * This test only works on Java 21 to Java 23.
         *
         * In Java 24, the definition of Java Letter and/or Java Digit has changed.
         */
        for (int i = 0; i < 0xFFFF; i++) {
            if (Character.isJavaIdentifierPart(i)) {
                testIdentifier('b', (char) i);
            } else {
                try {
                    testIdentifier((char) i, 'b');
                } catch (ELException e) {
                    continue;
                }
                fail("Expected EL exception for [" + i + "], [" + (char) i + "]");
            }
        }
    }

    private void testIdentifier(char one, char two) {
        ExpressionFactory factory = createExpressionFactory();
        ELContext context = new SimpleContext();

        String s = "OK";
        ValueExpression var = factory.createValueExpression(s, String.class);

        String identifier = new String(new char[] { one, two });
        context.getVariableMapper().setVariable(identifier, var);

        ValueExpression ve = factory.createValueExpression(context, "${" + identifier + "}", String.class);

        assertThat(ve.<String>getValue(context)).isEqualTo(s);
    }
}
