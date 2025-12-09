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
import org.flowable.common.engine.impl.de.odysseus.el.tree.TreeBuilderException;
import org.junit.jupiter.api.Test;

class AstNestedTest extends TestCase {

    AstNested parseNode(String expression) {
        return (AstNested) parse(expression).getRoot().getChild(0);
    }

    @Test
    void testIsLeftValue() {
        assertThat(parseNode("${(a)}").isLeftValue()).isFalse();
    }

    @Test
    void testEval() {
        assertThat(eval("${(1)}")).isEqualTo(1L);
    }

    @Test
    void testAppendStructure() {
        StringBuilder s = new StringBuilder();
        parseNode("${(1)}").appendStructure(s, null);
        assertThat(s.toString()).isEqualTo("(1)");
    }

    @Test
    void testGetValueReference() {
        assertThat(parseNode("${(1)}").getValueReference(null, null)).isNull();
    }

    @Test
    void testInvalid() {
        assertThatThrownBy(() -> parseNode("${(a, ab)}"))
                .isInstanceOf(TreeBuilderException.class)
                .hasMessage("Error parsing '${(a, ab)}': syntax error at position 4, encountered ',', expected ')'");

        assertThatThrownBy(() -> parseNode("${()}"))
                .isInstanceOf(TreeBuilderException.class)
                .hasMessage(
                        "Error parsing '${()}': syntax error at position 3, encountered ')', expected <IDENTIFIER>|<STRING>|<FLOAT>|<INTEGER>|'true'|'false'|'null'|'-'|'!'|'not'|'empty'|'('");
    }
}
