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

import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.flowable.common.engine.impl.el.BaseElTest;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ExpressionFactory;
import org.flowable.common.engine.impl.javax.el.ValueExpression;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("We do not support semicolon operator https://jakarta.ee/specifications/expression-language/6.0/jakarta-expression-language-spec-6.0#semicolon-operator-a-b")
class AstSemicolonTest extends BaseElTest {

    @Test
    void testGetValue01() {
        assertThat(eval("1;2", String.class)).isEqualTo("2");
    }


    @Test
    void testGetValue02() {
        assertThat(eval("1;2", Integer.class)).isEqualTo(2);
    }


    @Test
    void testGetValue03() {
        assertThat(eval("1;2 + 3", Integer.class)).isEqualTo(5);
    }


    @Test
    void testGetType() {
        ELContext context = new SimpleContext();
        ExpressionFactory factory = createExpressionFactory();

        ValueExpression ve = factory.createValueExpression(
                context, "${1+1;2+2}", Integer.class);

        assertThat(ve.getType(context)).isEqualTo(Number.class);
        assertThat(ve.<Integer>getValue(context)).isEqualTo(4);
    }
}
