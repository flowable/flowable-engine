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

import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleResolver;
import org.flowable.common.engine.impl.javax.el.ELException;
import org.flowable.common.engine.impl.javax.el.ExpressionFactory;
import org.flowable.common.engine.impl.javax.el.ValueExpression;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ELEvaluationTest extends BaseElTest {

    /**
     * Test use of spaces in ternary expressions. This was primarily an EL parser bug.
     */
    @Test
    void testBug42565() {
        assertThat(evaluateExpression("${false?true:false}")).isEqualTo("false");
        assertThat(evaluateExpression("${false?true: false}")).isEqualTo("false");
        assertThat(evaluateExpression("${false?true :false}")).isEqualTo("false");
        assertThat(evaluateExpression("${false?true : false}")).isEqualTo("false");
        assertThat(evaluateExpression("${false? true:false}")).isEqualTo("false");
        assertThat(evaluateExpression("${false? true: false}")).isEqualTo("false");
        assertThat(evaluateExpression("${false? true :false}")).isEqualTo("false");
        assertThat(evaluateExpression("${false? true : false}")).isEqualTo("false");
        assertThat(evaluateExpression("${false ?true:false}")).isEqualTo("false");
        assertThat(evaluateExpression("${false ?true: false}")).isEqualTo("false");
        assertThat(evaluateExpression("${false ?true :false}")).isEqualTo("false");
        assertThat(evaluateExpression("${false ?true : false}")).isEqualTo("false");
        assertThat(evaluateExpression("${false ? true:false}")).isEqualTo("false");
        assertThat(evaluateExpression("${false ? true: false}")).isEqualTo("false");
        assertThat(evaluateExpression("${false ? true :false}")).isEqualTo("false");
        assertThat(evaluateExpression("${false ? true : false}")).isEqualTo("false");
    }

    /**
     * Test use nested ternary expressions. This was primarily an EL parser bug.
     */
    @Test
    void testBug44994() {
        assertThat(evaluateExpression("${0 lt 0 ? 1 lt 0 ? 'many': 'one': 'none'}")).isEqualTo("none");
        assertThat(evaluateExpression("${0 lt 1 ? 1 lt 1 ? 'many': 'one': 'none'}")).isEqualTo("one");
        assertThat(evaluateExpression("${0 lt 2 ? 1 lt 2 ? 'many': 'one': 'none'}")).isEqualTo("many");
    }

    @Test
    void testParserBug45511() {
        // Test cases provided by OP
        assertThat(evaluateExpression("${empty ('')}")).isEqualTo("true");
        assertThat(evaluateExpression("${empty('')}")).isEqualTo("true");
        assertThat(evaluateExpression("${(true) and (false)}")).isEqualTo("false");
        assertThat(evaluateExpression("${(true)and(false)}")).isEqualTo("false");
    }

    @Test
    void testBug48112() {
        // bug 48112
        assertThat(evaluateExpression("${fn:trim('{world}')}")).isEqualTo("{world}");
    }

    @Test
    void testParserLiteralExpression() {
        // Inspired by work on bug 45451, comments from kkolinko on the dev
        // list and looking at the spec to find some edge cases

        // '\' is only an escape character inside a StringLiteral
        assertThat(evaluateExpression("\\\\")).isEqualTo("\\\\");

        /*
         * LiteralExpressions can only contain ${ or #{ if escaped with \ \ is not an escape character in any other
         * circumstances including \\
         */
        assertThat(evaluateExpression("\\")).isEqualTo("\\");
        assertThat(evaluateExpression("$")).isEqualTo("$");
        assertThat(evaluateExpression("#")).isEqualTo("#");
        assertThat(evaluateExpression("\\$")).isEqualTo("\\$");
        assertThat(evaluateExpression("\\#")).isEqualTo("\\#");
        assertThat(evaluateExpression("\\\\$")).isEqualTo("\\\\$");
        assertThat(evaluateExpression("\\\\#")).isEqualTo("\\\\#");
        assertThat(evaluateExpression("\\${")).isEqualTo("${");
        assertThat(evaluateExpression("\\#{")).isEqualTo("#{");
        assertThat(evaluateExpression("\\\\${")).isEqualTo("\\${");
        assertThat(evaluateExpression("\\\\#{")).isEqualTo("\\#{");

        // '\' is only an escape for '${' and '#{'.
        assertThat(evaluateExpression("\\$")).isEqualTo("\\$");
        assertThat(evaluateExpression("\\${")).isEqualTo("${");
        assertThat(evaluateExpression("\\$a")).isEqualTo("\\$a");
        assertThat(evaluateExpression("\\a")).isEqualTo("\\a");
        assertThat(evaluateExpression("\\\\")).isEqualTo("\\\\");
    }

    @Test
    void testParserStringLiteral() {
        // Inspired by work on bug 45451, comments from kkolinko on the dev
        // list and looking at the spec to find some edge cases

        // The only characters that can be escaped inside a String literal
        // are \ " and '. # and $ are not escaped inside a String literal.
        assertThat(evaluateExpression("${'\\\\'}")).isEqualTo("\\");
        assertThat(evaluateExpression("${\"\\\\\"}")).isEqualTo("\\");
        assertThat(evaluateExpression("${'\\\\\\\"\\'$#'}")).isEqualTo("\\\"'$#");
        assertThat(evaluateExpression("${\"\\\\\\\"\\'$#\"}")).isEqualTo("\\\"'$#");

        // Trying to quote # or $ should throw an error
        assertThatThrownBy(() -> evaluateExpression("${'\\$'}"))
                .isInstanceOf(ELException.class)
                .hasMessage("Error parsing '${'\\$'}': lexical error at position 2, encountered invalid escape sequence \\$, expected \\', \\\" or \\\\");
        assertThatThrownBy(() -> evaluateExpression("${'\\$'}"))
                .isInstanceOf(ELException.class)
                .hasMessage("Error parsing '${'\\$'}': lexical error at position 2, encountered invalid escape sequence \\$, expected \\', \\\" or \\\\");

        // Not terminating ' or ""
        assertThatThrownBy(() -> evaluateExpression("${'test''\"}"))
                .isInstanceOf(ELException.class)
                .hasMessage("Error parsing '${'test''\"}': lexical error at position 8, encountered unterminated string, expected '");
        assertThatThrownBy(() -> evaluateExpression("${\"test\"\"'}"))
                .isInstanceOf(ELException.class)
                .hasMessage("Error parsing '${\"test\"\"'}': lexical error at position 8, encountered unterminated string, expected \"");

        assertThat(evaluateExpression("${'\\\\$'}")).isEqualTo("\\$");
        assertThat(evaluateExpression("${'\\\\\\\\$'}")).isEqualTo("\\\\$");

        // Can use ''' inside '"' when quoting with '"' and vice versa without
        // escaping
        assertThat(evaluateExpression("${'\\\\\"'}")).isEqualTo("\\\"");
        assertThat(evaluateExpression("${'\"\\\\'}")).isEqualTo("\"\\");
        assertThat(evaluateExpression("${'\\\\\\''}")).isEqualTo("\\'");
        assertThat(evaluateExpression("${'\\'\\\\'}")).isEqualTo("'\\");
        assertThat(evaluateExpression("${\"\\\\'\"}")).isEqualTo("\\'");
        assertThat(evaluateExpression("${\"'\\\\\"}")).isEqualTo("'\\");
        assertThat(evaluateExpression("${\"\\\\\\\"\"}")).isEqualTo("\\\"");
        assertThat(evaluateExpression("${\"\\\"\\\\\"}")).isEqualTo("\"\\");
    }

    @Test
    void testMultipleEscaping() {
        assertThat(evaluateExpression("${\"\'\'\"}")).isEqualTo("''");
    }

    /**
     * Test mixing ${...} and #{...} in the same expression.
     */
    @Test
    void testMixedTypes() {
        // Mixing types should throw an error
        assertThatThrownBy(() -> evaluateExpression("${1+1}#{1+1}"))
                .isInstanceOf(ELException.class)
                .hasMessage("Error parsing '${1+1}#{1+1}': syntax error at position 6, encountered '#{', expected '${'");
    }

    @Test
    void testEscape01() {
        assertThat(evaluateExpression("$\\${")).isEqualTo("$${");
    }

    @Test
    void testBug49081a() {
        assertThat(evaluateExpression("$${1+1}")).isEqualTo("$2");
    }

    @Test
    void testBug49081b() {
        assertThat(evaluateExpression("##{1+1}")).isEqualTo("#2");
    }

    @Test
    void testBug49081c() {
        assertThat(evaluateExpression("#${1+1}")).isEqualTo("#2");
    }

    @Test
    void testBug49081d() {
        assertThat(evaluateExpression("$#{1+1}")).isEqualTo("$2");
    }

    @Test
    void testBug60431a() {
        assertThat(evaluateExpression("${fn:concat('O','K')}")).isEqualTo("OK");
    }

    @Test
    void testBug60431b() {
        assertThat(evaluateExpression("${fn:concat(fn:toArray('O','K'))}")).isEqualTo("OK");
    }

    @Test
    void testBug60431c() {
        assertThat(evaluateExpression("${fn:concat()}")).isEqualTo("");
    }

    @Test
    void testBug60431d() {
        assertThat(evaluateExpression("${fn:concat2('OK')}")).isEqualTo("OK");
    }

    @Test
    void testBug60431e() {
        assertThat(evaluateExpression("${fn:concat2('RU', fn:toArray('O','K'))}")).isEqualTo("RUOK");
    }

    @Test
    @Disabled("Not supported")
    void testElvis01() {
        assertThat(evaluateExpression("${'true'?:'FAIL'}")).isEqualTo("true");
    }

    @Test
    @Disabled("Not supported")
    void testElvis02() {
        // null coerces to false
        assertThat(evaluateExpression("${null?:'OK'}")).isEqualTo("OK");
    }

    @Test
    @Disabled("Not supported")
    void testElvis03() {
        assertThat(evaluateExpression("${'false'?:'OK'}")).isEqualTo("OK");
    }

    @Test
    @Disabled("Not supported")
    void testElvis04() {
        // Any string other "true" (ignoring case) coerces to false
        evaluateExpression("${'error'?:'OK'}");
    }

    @Test
    @Disabled("Not supported")
    void testElvis05() {
        // Non-string values do not coerce
        assertThatThrownBy(() -> evaluateExpression("${1234?:'OK'}"))
                .isInstanceOf(ELException.class)
                .hasMessage("");
    }

    @Test
    @Disabled("Not supported")
    void testNullCoalescing01() {
        assertThat(evaluateExpression("${'OK'??'FAIL'}")).isEqualTo("OK");
    }

    @Test
    @Disabled("Not supported")
    void testNullCoalescing02() {
        assertThat(evaluateExpression("${null??'OK'}")).isEqualTo("OK");
    }

    // ************************************************************************

    private String evaluateExpression(String expression) {
        ExpressionFactory exprFactory = createExpressionFactory();
        TesterFunctions.FMapper mapper = new TesterFunctions.FMapper();
        FlowableElContext context = new FlowableElContext(new SimpleResolver(), mapper::resolveFunction);
        ValueExpression ve = exprFactory.createValueExpression(context, expression, String.class);
        return ve.getValue(context).toString();
    }
}
