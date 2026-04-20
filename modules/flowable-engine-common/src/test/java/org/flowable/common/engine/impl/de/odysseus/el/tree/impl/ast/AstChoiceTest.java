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

class AstChoiceTest extends TestCase {

    private final Bindings bindings = new Bindings(null, null, null);

    AstChoice parseNode(String expression) {
        return (AstChoice) parse(expression).getRoot().getChild(0);
    }

    @Test
    void testEval() {
        assertThat(eval("${true?1:2}")).isEqualTo(1L);
        assertThat(eval("${false?1:2}")).isEqualTo(2L);
    }

    @Test
    void testAppendStructure() {
        StringBuilder s = new StringBuilder();
        parseNode("${a?b:c}").appendStructure(s, bindings);
        assertThat(s.toString()).isEqualTo("a ? b : c");
    }

    @Test
    void testIsLiteralText() {
        assertThat(parseNode("${a?b:c}").isLiteralText()).isFalse();
    }

    @Test
    void testIsLeftValue() {
        assertThat(parseNode("${a?b:c}").isLeftValue()).isFalse();
    }

    @Test
    void testGetType() {
        assertThat(parseNode("${a?b:c}").getType(bindings, null)).isNull();
    }

    @Test
    void testIsReadOnly() {
        assertThat(parseNode("${a?b:c}").isReadOnly(bindings, null)).isTrue();
    }

    @Test
    void testSetValue() {
        assertThatThrownBy(() -> parseNode("${a?b:c}").setValue(bindings, null, null))
                .isInstanceOf(ELException.class);
    }

    @Test
    void testGetValue() {
        assertThat(eval("${true?1:2}")).isEqualTo(1L);
        assertThat(eval("${true?1:2}", String.class)).isEqualTo("1");
    }

    @Test
    void testGetValueReference() {
        assertThat(parseNode("${true?1:2}").getValueReference(bindings, null)).isNull();
    }
}
