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

import org.flowable.common.engine.impl.de.odysseus.el.TestCase;
import org.junit.jupiter.api.Test;

class AstEvalTest extends TestCase {

    AstEval parseNode(String expression) {
        return (AstEval) parse(expression).getRoot();
    }

    @Test
    void testIsLeftValue() {
        assertThat(parseNode("${1}").isLeftValue()).isFalse();
        assertThat(parseNode("${foo.bar}").isLeftValue()).isTrue();
    }

    @Test
    void testIsDeferred() {
        assertThat(parseNode("#{1}").isDeferred()).isTrue();
        assertThat(parseNode("${1}").isDeferred()).isFalse();
    }

    @Test
    void testEval() {
        assertThat(eval("${1}")).isEqualTo(1L);
    }

    @Test
    void testAppendStructure() {
        StringBuilder s = new StringBuilder();
        parseNode("${1}").appendStructure(s, null);
        assertThat(s.toString()).isEqualTo("${1}");
    }
}
