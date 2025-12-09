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

class AstTextTest extends TestCase {

    private Bindings bindings = new Bindings(null, null, null);

    AstText parseNode(String expression) {
        return (AstText) parse(expression).getRoot();
    }

    @Test
    void testEval() {
        assertThat(eval("foo")).isEqualTo("foo");
    }

    @Test
    void testAppendStructure() {
        StringBuilder s = new StringBuilder();
        parseNode("foo").appendStructure(s, bindings);
        assertThat(s.toString()).isEqualTo("foo");
    }

    @Test
    void testIsLiteralText() {
        assertThat(parseNode("foo").isLiteralText()).isTrue();
    }

    @Test
    void testIsLeftValue() {
        assertThat(parseNode("foo").isLeftValue()).isFalse();
    }

    @Test
    void testGetType() {
        assertThat(parseNode("foo").getType(bindings, null)).isNull();
    }

    @Test
    void testIsReadOnly() {
        assertThat(parseNode("foo").isReadOnly(bindings, null)).isTrue();
    }

    @Test
    void testSetValue() {
        assertThatThrownBy(() -> parseNode("foo").setValue(bindings, null, null))
                .isInstanceOf(ELException.class);
    }

    @Test
    void testGetValue() {
        assertThat(eval("1")).isEqualTo("1");
        assertThat(eval("1", Long.class)).isEqualTo(1L);
    }

    @Test
    void testGetValueReference() {
        assertThat(parseNode("1").getValueReference(null, null)).isNull();
    }

    @Test
    void testInvoke() {
        assertThat(parseNode("1").invoke(bindings, null, null, null, null)).isEqualTo("1");
        assertThat(parseNode("1").invoke(bindings, null, Long.class, null, null)).isEqualTo(1L);
    }

    @Test
    void testGetMethodInfo() {
        assertThat(parseNode("foo").getMethodInfo(bindings, null, null, null)).isNull();
    }
}
