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
package org.flowable.common.engine.impl.de.odysseus.el.tree.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flowable.common.engine.impl.de.odysseus.el.TestCase;
import org.flowable.common.engine.impl.de.odysseus.el.tree.ExpressionNode;
import org.flowable.common.engine.impl.de.odysseus.el.tree.Tree;
import org.flowable.common.engine.impl.de.odysseus.el.tree.TreeBuilderException;
import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.ast.AstBinary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ParserTest extends TestCase {

    static Tree verifyEvalExpression(String canonical) {
        Tree tree = parse(canonical);
        assertThat(tree.getRoot().isLiteralText()).isFalse();
        assertThat(tree.getRoot().getStructuralId(null)).isEqualTo(canonical);
        return tree;
    }

    static Tree verifyEvalExpression(String canonical, String expression) {
        Tree tree = parse(expression);
        assertThat(tree.getRoot().isLiteralText()).isFalse();
        assertThat(tree.getRoot().getStructuralId(null)).isEqualTo(canonical);
        return verifyEvalExpression(canonical);
    }

    static Tree verifyEvalExpression(String canonical, String expression1, String expression2) {
        Tree tree = parse(expression2);
        assertThat(tree.getRoot().isLiteralText()).isFalse();
        assertThat(tree.getRoot().getStructuralId(null)).isEqualTo(canonical);
        return verifyEvalExpression(canonical, expression1);
    }

    static Tree verifyCompositeExpression(String canonical) {
        Tree tree = parse(canonical);
        assertThat(tree.getRoot().isLiteralText()).isFalse();
        assertThat(tree.getRoot().getStructuralId(null)).isEqualTo(canonical);
        return tree;
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "$",
            "#",
            "1",
            "{1}",
            "\\${1}",
            "\\#{1}",
            "\\${1}\\#{1}",
            "\\",
            "\\\\",
            "foo",
            "\\f\\o\\o\\",
            "\"foo\"",
            "'foo'"
    })
    void testLiteral(String expression) {
        ExpressionNode node = parse(expression).getRoot();
        assertThat(node.isLiteralText()).isTrue();
        assertThat(node.getStructuralId(null)).isEqualTo(expression);
    }

    Tree verifyBinary(AstBinary.Operator op, String canonical) {
        Tree tree = verifyEvalExpression(canonical);
        assertThat((tree.getRoot()).getChild(0) instanceof AstBinary).isTrue();
        assertThat(((AstBinary) tree.getRoot().getChild(0)).getOperator()).isEqualTo(op);
        return tree;
    }

    @Test
    void testBinray() {
        verifyEvalExpression("${a * a}");
        verifyEvalExpression("${a / a}", "${a div a}");
        verifyEvalExpression("${a % a}", "${a mod a}");
        verifyEvalExpression("${a + a}");
        verifyEvalExpression("${a - a}");
        verifyEvalExpression("${a < a}", "${a lt a}");
        verifyEvalExpression("${a > a}", "${a gt a}");
        verifyEvalExpression("${a <= a}", "${a le a}");
        verifyEvalExpression("${a >= a}", "${a ge a}");
        verifyEvalExpression("${a == a}", "${a eq a}");
        verifyEvalExpression("${a != a}", "${a ne a}");
        verifyEvalExpression("${a && a}", "${a and a}");
        verifyEvalExpression("${a || a}", "${a or a}");

        verifyBinary(AstBinary.DIV, "${a * a / a}");
        verifyBinary(AstBinary.MUL, "${a / a * a}");
        verifyBinary(AstBinary.MOD, "${a / a % a}");
        verifyBinary(AstBinary.DIV, "${a % a / a}");
        verifyBinary(AstBinary.ADD, "${a % a + a}");
        verifyBinary(AstBinary.ADD, "${a + a % a}");
        verifyBinary(AstBinary.SUB, "${a + a - a}");
        verifyBinary(AstBinary.ADD, "${a - a + a}");
        verifyBinary(AstBinary.LT, "${a - a < a}");
        verifyBinary(AstBinary.LT, "${a < a - a}");
        verifyBinary(AstBinary.GT, "${a < a > a}");
        verifyBinary(AstBinary.LT, "${a > a < a}");
        verifyBinary(AstBinary.LE, "${a > a <= a}");
        verifyBinary(AstBinary.GT, "${a <= a > a}");
        verifyBinary(AstBinary.GE, "${a <= a >= a}");
        verifyBinary(AstBinary.LE, "${a >= a <= a}");
        verifyBinary(AstBinary.EQ, "${a == a >= a}");
        verifyBinary(AstBinary.EQ, "${a >= a == a}");
        verifyBinary(AstBinary.NE, "${a == a != a}");
        verifyBinary(AstBinary.EQ, "${a != a == a}");
        verifyBinary(AstBinary.AND, "${a && a != a}");
        verifyBinary(AstBinary.AND, "${a != a && a}");
        verifyBinary(AstBinary.OR, "${a && a || a}");
        verifyBinary(AstBinary.OR, "${a || a && a}");
        verifyBinary(AstBinary.OR, "${! a || a}");
    }

    @Test
    void testUnary() {
        verifyEvalExpression("${- a}");
        verifyEvalExpression("${- - a}");
        verifyEvalExpression("${empty a}");
        verifyEvalExpression("${empty empty a}");
        verifyEvalExpression("${! a}", "${not a}");
        verifyEvalExpression("${! ! a}", "${not not a}", "${not ! a}");
    }

    @Test
    void testDeferredExpression() {
        verifyEvalExpression("#{a}", "#{ a }");
    }

    @Test
    void testComposite() {
        verifyCompositeExpression("a${a}a");
        verifyCompositeExpression("a ${a} a");
        verifyCompositeExpression("${a}${a}");
        assertThatThrownBy(() -> parse("#{a}${a}"))
                .isInstanceOf(TreeBuilderException.class)
                .hasMessage("Error parsing '#{a}${a}': syntax error at position 4, encountered '${', expected '#{'");
    }

    @Test
    void testInteger() {
        verifyEvalExpression("${0}");
    }

    @Test
    void testBoolean() {
        verifyEvalExpression("${true}");
        verifyEvalExpression("${false}");
    }

    @Test
    void testNull() {
        verifyEvalExpression("${null}");
    }

    @Test
    void testString() {
        verifyEvalExpression("${''}", "${\"\"}");
        verifyEvalExpression("${'\\''}", "${\"'\"}");
        verifyEvalExpression("${'\"'}", "${\"\\\"\"}");
        verifyEvalExpression("${'a'}", "${\"a\"}");
    }

    @Test
    void testFloat() {
        verifyEvalExpression("${0.0}", "${0.0e0}");
        verifyEvalExpression("${0.0}", "${0e0}", "${0E0}");
        verifyEvalExpression("${0.0}", "${0.}", "${0.e0}");
        verifyEvalExpression("${0.0}", "${.0}", "${.0e0}");
        verifyEvalExpression("${0.0}", "${0e+0}", "${0e-0}");
    }

    @Test
    void testChoice() {
        verifyEvalExpression("${a ? a : a}", "${a?a:a}");
        verifyEvalExpression("${a ? b ? b : b : a}", "${a?b?b:b:a}");
        verifyEvalExpression("${a ? a : b ? b : b}", "${a?a:b?b:b}");
        verifyEvalExpression("${c ? b : (f())}", "${c?b:(f())}");
        verifyEvalExpression("${a ? f() : a}", "${a?f():a}");
        verifyEvalExpression("${a ? a : a:f()}", "${a?a:a:f()}");
        verifyEvalExpression("${a ? a:f() : a}", "${a?a:f():a}");
        assertThatThrownBy(() -> parse("${a?a:f()}"))
                .isInstanceOf(TreeBuilderException.class)
                .hasMessage("Error parsing '${a?a:f()}': syntax error at position 9, encountered '}', expected ':'");
    }

    @Test
    void testNested() {
        verifyEvalExpression("${(a)}", "${ ( a ) }");
        verifyEvalExpression("${((a))}");
    }

    @Test
    void testIdentifier() {
        verifyEvalExpression("${a}", "${ a}", "${a }");
        assertThat(parse("${a}").getRoot().isLeftValue()).isTrue();
    }

    @Test
    void testFunction() {
        verifyEvalExpression("${a()}");
        verifyEvalExpression("${a(a)}");
        verifyEvalExpression("${a(a, a)}");
        verifyEvalExpression("${a:a()}");
        verifyEvalExpression("${a:a(a)}");
        verifyEvalExpression("${a:a(a, a)}");
    }

    @Test
    void testProperty() {
        verifyEvalExpression("${a.a}", "${ a . a }");
        verifyEvalExpression("${a.a.a}");
        verifyEvalExpression("${a[a]}", "${ a [ a ] }");
        verifyEvalExpression("${a[a][a]}");
        verifyEvalExpression("${a[a[a]]}");

        assertThat(parse("${a.a}").getRoot().isLeftValue()).isTrue();
        assertThat(parse("${1 . a}").getRoot().isLeftValue()).isFalse();
        assertThat(parse("${(1).a}").getRoot().isLeftValue()).isTrue();

        assertThat(parse("${a[a]}").getRoot().isLeftValue()).isTrue();
        assertThat(parse("${1[a]}").getRoot().isLeftValue()).isFalse();
        assertThat(parse("${(1)[a]}").getRoot().isLeftValue()).isTrue();
    }

    @Test
    void testIsDeferred() {
        assertThat(parse("foo").isDeferred()).isFalse();
        assertThat(parse("${foo}").isDeferred()).isFalse();
        assertThat(parse("${foo}bar${foo}").isDeferred()).isFalse();
        assertThat(parse("#{foo}").isDeferred()).isTrue();
        assertThat(parse("#{foo}bar#{foo}").isDeferred()).isTrue();
    }
}
