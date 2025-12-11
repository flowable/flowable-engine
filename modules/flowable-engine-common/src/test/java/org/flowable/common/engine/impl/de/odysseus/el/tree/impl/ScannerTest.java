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
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.AND;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.ARROW;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.COLON;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.COMMA;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.DIV;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.DOT;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.EMPTY;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.END_EVAL;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.EOF;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.EQ;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.FALSE;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.FLOAT;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.GE;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.GT;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.IDENTIFIER;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.INSTANCEOF;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.INTEGER;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.LBRACK;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.LE;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.LPAREN;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.LT;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.MINUS;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.MOD;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.MUL;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.NE;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.NOT;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.NULL;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.OR;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.PLUS;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.QUESTION;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.RBRACK;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.RPAREN;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.START_EVAL_DEFERRED;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.START_EVAL_DYNAMIC;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.STRING;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.TEXT;
import static org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Scanner.Symbol.TRUE;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.flowable.common.engine.impl.de.odysseus.el.TestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ScannerTest extends TestCase {

    Scanner.Symbol[] symbols(String expression) throws Scanner.ScanException {
        List<Scanner.Symbol> list = new ArrayList<>();
        Scanner scanner = new Scanner(expression);
        Scanner.Token token = scanner.next();
        while (token.getSymbol() != EOF) {
            list.add(token.getSymbol());
            token = scanner.next();
        }
        return list.toArray(new Scanner.Symbol[0]);
    }

    @ParameterizedTest
    @ValueSource(strings = { "${0}", "${1}", "${01234567890}" })
    void testInteger(String expression) throws Scanner.ScanException {
        assertThat(symbols(expression)).containsExactly(START_EVAL_DYNAMIC, INTEGER, END_EVAL);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "${0.}", "${023456789.}", "${.0}", "${.023456789}", "${0.0}",
            "${0e0}", "${0E0}", "${0e+0}", "${0E+0}", "${0e+0}", "${0E+0}",
            "${.0e0}", "${.0E0}", "${.0e+0}", "${.0E+0}", "${.0e-0}", "${.0E-0}",
            "${0.e0}", "${0.E0}", "${0.e+0}", "${0.E+0}", "${0.e-0}", "${0.E-0}",
            "${0.0e0}", "${0.0E0}", "${0.0e+0}", "${0.0E+0}", "${0.0e-0}", "${0.0E-0}"
    })
    void testFloat(String expression) throws Scanner.ScanException {
        assertThat(symbols(expression)).containsExactly(START_EVAL_DYNAMIC, FLOAT, END_EVAL);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "${'foo'}", "${'f\"o'}", "${'f\\'o'}", "${'f\\\"o'}",
            "${\"foo\"}", "${\"f\\\"o\"}", "${\"f'o\"}", "${\"f\\'o\"}"
    })
    void testString(String expression) throws Scanner.ScanException {
        assertThat(symbols(expression)).containsExactly(START_EVAL_DYNAMIC, STRING, END_EVAL);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "${'f\\oo'}", "${'foo", "${\"f\\oo\"}", "${\"foo"
    })
    void invalidString(String expression) {
        assertThatThrownBy(() -> symbols(expression)).isInstanceOf(Scanner.ScanException.class);
    }

    @ParameterizedTest
    @MethodSource("keywordArguments")
    void testKeywords(String expression, Scanner.Symbol symbol) throws Scanner.ScanException {
        assertThat(symbols(expression)).containsExactly(START_EVAL_DYNAMIC, symbol, END_EVAL);
    }

    static Stream<Arguments> keywordArguments() {
        return Stream.of(
                Arguments.of("${null}", NULL),
                Arguments.of("${true}", TRUE),
                Arguments.of("${false}", FALSE),
                Arguments.of("${empty}", EMPTY),
                Arguments.of("${div}", DIV),
                Arguments.of("${mod}", MOD),
                Arguments.of("${not}", NOT),
                Arguments.of("${and}", AND),
                Arguments.of("${or}", OR),
                Arguments.of("${le}", LE),
                Arguments.of("${lt}", LT),
                Arguments.of("${eq}", EQ),
                Arguments.of("${ne}", NE),
                Arguments.of("${ge}", GE),
                Arguments.of("${gt}", GT),
                Arguments.of("${instanceof}", INSTANCEOF)
        );
    }

    @ParameterizedTest
    @MethodSource("operatorArguments")
    void testOperators(String expression, Scanner.Symbol symbol) throws Scanner.ScanException {
        assertThat(symbols(expression)).containsExactly(START_EVAL_DYNAMIC, symbol, END_EVAL);
    }

    static Stream<Arguments> operatorArguments() {
        return Stream.of(
                Arguments.of("${*}", MUL),
                Arguments.of("${/}", DIV),
                Arguments.of("${%}", MOD),
                Arguments.of("${+}", PLUS),
                Arguments.of("${-}", MINUS),
                Arguments.of("${?}", QUESTION),
                Arguments.of("${:}", COLON),
                Arguments.of("${[}", LBRACK),
                Arguments.of("${]}", RBRACK),
                Arguments.of("${(}", LPAREN),
                Arguments.of("${)}", RPAREN),
                Arguments.of("${,}", COMMA),
                Arguments.of("${.}", DOT),
                Arguments.of("${&&}", AND),
                Arguments.of("${||}", OR),
                Arguments.of("${!}", NOT),
                Arguments.of("${<=}", LE),
                Arguments.of("${<}", LT),
                Arguments.of("${==}", EQ),
                Arguments.of("${!=}", NE),
                Arguments.of("${>=}", GE),
                Arguments.of("${>}", GT),
                Arguments.of("${->}", ARROW)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = { "${&)", "${|)", "${=)" })
    void invalidOperator(String expression) {
        assertThatThrownBy(() -> symbols(expression)).isInstanceOf(Scanner.ScanException.class);
    }

    @Test
    void testWhitespace() throws Scanner.ScanException {
        assertThat(symbols("${\t\n\r }")).containsExactly(START_EVAL_DYNAMIC, END_EVAL);
    }

    @ParameterizedTest
    @ValueSource(strings = { "${foo}", "${foo_1}", "${xnull}", "${nullx}" })
    void testIdentifier(String expression) throws Scanner.ScanException {
        assertThat(symbols(expression)).containsExactly(START_EVAL_DYNAMIC, IDENTIFIER, END_EVAL);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "foo",
            "foo\\",
            "foo\\$",
            "foo\\#",
            "foo\\${",
            "foo\\#{",
            "\\${foo}",
            "\\${foo}"
    })
    void testText(String expression) throws Scanner.ScanException {
        assertThat(symbols(expression)).containsExactly(TEXT);
    }

    @Test
    void testMixed() throws Scanner.ScanException {
        assertThat(symbols("foo${")).containsExactly(TEXT, START_EVAL_DYNAMIC);
        assertThat(symbols("${bar")).containsExactly(START_EVAL_DYNAMIC, IDENTIFIER);
        assertThat(symbols("${}bar")).containsExactly(START_EVAL_DYNAMIC, END_EVAL, TEXT);
        assertThat(symbols("foo${}bar")).containsExactly(TEXT, START_EVAL_DYNAMIC, END_EVAL, TEXT);
    }

    @Test
    void testDeferred() throws Scanner.ScanException {
        assertThat(symbols("#{")).containsExactly(START_EVAL_DEFERRED);
    }
}