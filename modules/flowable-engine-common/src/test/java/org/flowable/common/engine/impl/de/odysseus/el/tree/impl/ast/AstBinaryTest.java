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
import org.flowable.common.engine.impl.javax.el.ELException;
import org.junit.jupiter.api.Test;

class AstBinaryTest extends TestCase {

    private final Bindings bindings = new Bindings(null, null, null);

    AstBinary parseNode(String expression) {
        return (AstBinary) parse(expression).getRoot().getChild(0);
    }

    @Test
    void testEval() {
        assertThat(eval("${4+2}")).isEqualTo(6L);
        assertThat(eval("${4*2}")).isEqualTo(8L);
        assertThat(eval("${4/2}")).isEqualTo(2d);
        assertThat(eval("${4%2}")).isEqualTo(0L);

        assertThat(eval("${true && false}")).isEqualTo(false);

        assertThat(eval("${true || false}")).isEqualTo(true);

        assertThat(eval("${1 == 1}")).isEqualTo(true);
        assertThat(eval("${1 == 2}")).isEqualTo(false);
        assertThat(eval("${2 == 1}")).isEqualTo(false);

        assertThat(eval("${1 != 1}")).isEqualTo(false);
        assertThat(eval("${1 != 2}")).isEqualTo(true);
        assertThat(eval("${2 == 1}")).isEqualTo(false);

        assertThat(eval("${1 < 1}")).isEqualTo(false);
        assertThat(eval("${1 < 2}")).isEqualTo(true);
        assertThat(eval("${2 < 1}")).isEqualTo(false);

        assertThat(eval("${1 > 1}")).isEqualTo(false);
        assertThat(eval("${1 > 2}")).isEqualTo(false);
        assertThat(eval("${2 > 1}")).isEqualTo(true);

        assertThat(eval("${1 <= 1}")).isEqualTo(true);
        assertThat(eval("${1 <= 2}")).isEqualTo(true);
        assertThat(eval("${2 <= 1}")).isEqualTo(false);

        assertThat(eval("${1 >= 1}")).isEqualTo(true);
        assertThat(eval("${1 >= 2}")).isEqualTo(false);
        assertThat(eval("${2 >= 1}")).isEqualTo(true);
    }

    @Test
    void testAppendStructure() {
        StringBuilder s;
        s = new StringBuilder();
        parseNode("${1+1}").appendStructure(s, bindings);
        parseNode("${1*1}").appendStructure(s, bindings);
        parseNode("${1/1}").appendStructure(s, bindings);
        parseNode("${1%1}").appendStructure(s, bindings);
        assertThat(s.toString()).isEqualTo("1 + 11 * 11 / 11 % 1");

        s = new StringBuilder();
        parseNode("${1<1}").appendStructure(s, bindings);
        parseNode("${1>1}").appendStructure(s, bindings);
        parseNode("${1<=1}").appendStructure(s, bindings);
        parseNode("${1>=1}").appendStructure(s, bindings);
        assertThat(s.toString()).isEqualTo("1 < 11 > 11 <= 11 >= 1");

        s = new StringBuilder();
        parseNode("${1==1}").appendStructure(s, bindings);
        parseNode("${1!=1}").appendStructure(s, bindings);
        assertThat(s.toString()).isEqualTo("1 == 11 != 1");

        s = new StringBuilder();
        parseNode("${1&&1}").appendStructure(s, bindings);
        parseNode("${1||1}").appendStructure(s, bindings);
        assertThat(s.toString()).isEqualTo("1 && 11 || 1");
    }

    @Test
    void testIsLiteralText() {
        assertThat(parseNode("${1+1}").isLiteralText()).isFalse();
    }

    @Test
    void testIsLeftValue() {
        assertThat(parseNode("${1+1}").isLeftValue()).isFalse();
    }

    @Test
    void testGetType() {
        assertThat(parseNode("${1+1}").getType(bindings, null)).isNull();
    }

    @Test
    void testIsReadOnly() {
        assertThat(parseNode("${1+1}").isReadOnly(bindings, null)).isTrue();
    }

    @Test
    void testSetValue() {
        assertThatThrownBy(() -> parseNode("${1+1}").setValue(bindings, null, null))
                .isInstanceOf(ELException.class);
    }

    @Test
    void testGetValue() {
        assertThat(eval("${1+1}")).isEqualTo(2L);
        assertThat(eval("${1+1}", String.class)).isEqualTo("2");
    }

    @Test
    void testGetValueReference() {
        assertThat(parseNode("${1+1}").getValueReference(bindings, null)).isNull();
    }

    @Test
    void testOperators() {
        assertThat(eval("${true and true}", Boolean.class)).isTrue();
        assertThat(eval("${true and false}", Boolean.class)).isFalse();
        assertThat(eval("${false and true}", Boolean.class)).isFalse();
        assertThat(eval("${false and false}", Boolean.class)).isFalse();

        assertThat(eval("${true or true}", Boolean.class)).isTrue();
        assertThat(eval("${true or false}", Boolean.class)).isTrue();
        assertThat(eval("${false or true}", Boolean.class)).isTrue();
        assertThat(eval("${false or false}", Boolean.class)).isFalse();
    }
}
