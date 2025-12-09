/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.common.engine.impl.el;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flowable.common.engine.impl.javax.el.ExpressionFactory;
import org.junit.jupiter.api.Test;

class ExpressionFactoryTest extends BaseElTest {

    @Test
    void testCoerceToTypeString() {
        ExpressionFactory factory = createExpressionFactory();
        TestObject testObjectA = new TestObject();
        String result = (String) factory.coerceToType(testObjectA, String.class);
        assertThat(result).isEqualTo(TestObject.OK);
    }

    @Test
    void testCoerceToTypeStringThrowsException() {
        ExpressionFactory factory = ExpressionFactory.newInstance();
        TestObjectException testObjectA = new TestObjectException();
        assertThatThrownBy(() -> factory.coerceToType(testObjectA, String.class))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test exception");
    }

    private static class TestObject {

        private static final String OK = "OK";

        @Override
        public String toString() {
            return OK;
        }
    }

    private static class TestObjectException {

        @Override
        public String toString() {
            throw new RuntimeException("Test exception");
        }
    }
}
