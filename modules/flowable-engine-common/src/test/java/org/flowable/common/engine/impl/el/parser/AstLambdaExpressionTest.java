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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flowable.common.engine.impl.el.BaseElTest;
import org.flowable.common.engine.impl.javax.el.ELException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class AstLambdaExpressionTest extends BaseElTest {

    @Test
    void testSpec01() {
        Object result = eval("(x->x+1)(1)", Integer.class);
        assertThat(result).isEqualTo(2);
    }

    @Test
    void testSpec02() {
        Object result = eval("((x,y)->x+y)(1,2)", Integer.class);
        assertThat(result).isEqualTo(3);
    }

    @Test
    void testSpec03() {
        Object result = eval("(()->64)", Integer.class);
        assertThat(result).isEqualTo(64);
    }

    @Test
    @Disabled("We do not support assignment")
    void testSpec04() {
        Object result = eval("v = (x,y)->x+y; v(3,4)", Integer.class);
        assertThat(result).isEqualTo(7);
    }

    @Test
    @Disabled("We do not support assignment")
    void testSpec05() {
        Object result = eval("fact = n -> n==0? 1: n*fact(n-1); fact(5)", Integer.class);
        assertThat(result).isEqualTo(120);
    }

    @Test
    void testSpec06() {
        Object result = eval("(x->y->x-y)(2)(1)", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void testInvocation01() {
        Object result = eval("(()->2)()", Integer.class);
        assertThat(result).isEqualTo(2);
    }

    @Test
    void testNested01() {
        Object result = eval("(()->y->2-y)()(1)", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void testNested02() {
        Object result = eval("(()->y->()->2-y)()(1)()", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void testNested03() {
        // More method parameters than there are nested lambda expressions
        assertThatThrownBy(() -> eval("(()->y->()->2-y)()(1)()()", Integer.class))
                .isInstanceOf(ELException.class);
    }

    @Test
    void testNested04() {
        Object result = eval("(()->y->()->x->x-y)()(1)()(2)", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void testNested05() {
        Object result = eval("(()->y->()->()->x->x-y)()(1)()()(2)", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void testNested06() {
        Object result = eval("(()->y->()->()->x->x-y)()(1)()(3)(2)", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void testNested07() {
        Object result = eval("()->()->()->42", Integer.class);
        assertThat(result).isEqualTo(42);
    }

    @Test
    @Disabled("We do not support assignment")
    void testLambdaAsFunction01() {
        Object result = eval("v = (x->y->x-y); v(2)(1)", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    @Disabled("We do not support assignment")
    void testLambdaAsFunction02() {
        Object result = eval("v = (()->y->2-y); v()(1)", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    @Disabled("We do not support assignment")
    void testLambdaAsFunction03() {
        Object result = eval("v = (()->y->()->2-y); v()(1)()", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    @Disabled("We do not support assignment")
    void testLambdaAsFunction04() {
        // More method parameters than there are nested lambda expressions
        assertThatThrownBy(() -> eval("v = (()->y->()->2-y); v()(1)()()", Integer.class))
                .isInstanceOf(ELException.class);
    }

    @Test
    @Disabled("We do not support assignment")
    void testLambdaAsFunction05() {
        Object result = eval("v = (()->y->()->x->x-y); v()(1)()(2)", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    @Disabled("We do not support assignment")
    void testLambdaAsFunction06() {
        Object result = eval("v = (()->y->()->()->x->x-y); v()(1)()()(2)", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    @Disabled("We do not support assignment")
    void testLambdaAsFunction07() {
        Object result = eval("v = (()->y->()->()->x->x-y); v()(1)()(3)(2)", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    @Disabled("We do not support assignment")
    void testLambdaAsFunction08() {
        // Using a name space for the function is not allowed
        assertThatThrownBy(() -> eval("foo:v = (x)->x+1; foo:v(0)", Integer.class))
                .isInstanceOf(ELException.class);
    }
}
