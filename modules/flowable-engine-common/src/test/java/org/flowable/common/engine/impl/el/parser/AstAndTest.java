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

import org.flowable.common.engine.impl.el.BaseElTest;
import org.junit.jupiter.api.Test;

class AstAndTest extends BaseElTest {

    @Test
    void test01() {
        assertThat(eval("true && true")).isEqualTo(Boolean.TRUE);
    }

    @Test
    void test02() {
        assertThat(eval("true && null")).isEqualTo(Boolean.FALSE);
    }

    @Test
    void test03() {
        assertThat(eval("null && true")).isEqualTo(Boolean.FALSE);
    }

    @Test
    void test04() {
        assertThat(eval("null && null")).isEqualTo(Boolean.FALSE);
    }

    @Test
    void test05() {
        assertThat(eval("true && true && true && true && true")).isEqualTo(Boolean.TRUE);
    }

    @Test
    void test06() {
        assertThat(eval("true && true && true && true && false")).isEqualTo(Boolean.FALSE);
    }

    @Test
    void test07() {
        assertThat(eval("false && true && true && true && true")).isEqualTo(Boolean.FALSE);
    }

    @Test
    void test08() {
        assertThat(eval("true && false && true && true && true")).isEqualTo(Boolean.FALSE);
    }

    @Test
    void test09() {
        assertThat(eval("true && true && false && true && true")).isEqualTo(Boolean.FALSE);
    }

    @Test
    void test10() {
        assertThat(eval("true && true && true && false &&  true")).isEqualTo(Boolean.FALSE);
    }

}
