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

class AstNullTest extends TestCase {

    private Bindings bindings = new Bindings(null, null, null);

    AstNull parseNode(String expression) {
        return (AstNull) parse(expression).getRoot().getChild(0);
    }

    @Test
    void testEval() {
        assertThat(eval("${null}")).isNull();
    }

    @Test
    void testAppendStructure() {
        StringBuilder s = new StringBuilder();
        parseNode("${null}").appendStructure(s, bindings);
        assertThat(s.toString()).isEqualTo("null");
    }

    @Test
    void testIsLiteralText() {
        assertThat(parseNode("${null}").isLiteralText()).isFalse();
    }

    @Test
    void testIsLeftValue() {
        assertThat(parseNode("${null}").isLeftValue()).isFalse();
    }

    @Test
    void testGetType() {
        assertThat(parseNode("${null}").getType(bindings, null)).isNull();
    }

    @Test
    void testIsReadOnly() {
        assertThat(parseNode("${null}").isReadOnly(bindings, null)).isTrue();
    }

    @Test
    void testSetValue() {
        assertThatThrownBy(() -> parseNode("${null}").setValue(bindings, null, null))
                .isInstanceOf(ELException.class);
    }

    @Test
    void testGetValue() {
        assertThat(eval("${null}")).isNull();
        assertThat(eval("${null}", String.class)).isEqualTo("");
    }

    @Test
    void testGetValueReference() {
        assertThat(parseNode("${null}").getValueReference(null, null)).isNull();
    }
}
