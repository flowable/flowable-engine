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
package org.flowable.common.engine.impl.de.odysseus.el;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flowable.common.engine.impl.de.odysseus.el.misc.TypeConverter;
import org.flowable.common.engine.impl.javax.el.ELException;
import org.junit.jupiter.api.Test;

class ObjectValueExpressionTest extends TestCase {

    private TypeConverter converter = TypeConverter.DEFAULT;

    @Test
    void testHashCode() {
        assertThat(new ObjectValueExpression(converter, "foo", Object.class).hashCode()).isEqualTo("foo".hashCode());
    }

    @Test
    void testEqualsObject() {
        assertThat(new ObjectValueExpression(converter, "foo", Object.class).equals(new ObjectValueExpression(converter, "foo", Object.class))).isTrue();
        assertThat(new ObjectValueExpression(converter, new String("foo"), Object.class).equals(
                new ObjectValueExpression(converter, "foo", Object.class))).isTrue();
        assertThat(new ObjectValueExpression(converter, "foo", Object.class).equals(new ObjectValueExpression(converter, "bar", Object.class))).isFalse();
    }

    @Test
    void testGetValue() {
        assertThat(new ObjectValueExpression(converter, "foo", Object.class).getValue(null)).isEqualTo("foo");
    }

    @Test
    void testGetExpressionString() {
        assertThat(new ObjectValueExpression(converter, "foo", Object.class).getExpressionString()).isNull();
    }

    @Test
    void testIsLiteralText() {
        assertThat(new ObjectValueExpression(converter, "foo", Object.class).isLiteralText()).isFalse();
    }

    @Test
    void testGetType() {
        assertThat(new ObjectValueExpression(converter, "foo", Object.class).getType(null)).isNull();
    }

    @Test
    void testIsReadOnly() {
        assertThat(new ObjectValueExpression(converter, "foo", Object.class).isReadOnly(null)).isTrue();
    }

    @Test
    void testSetValue() {
        assertThatThrownBy(() -> new ObjectValueExpression(converter, "foo", Object.class).setValue(null, "bar"))
                .isInstanceOf(ELException.class);
    }

    @Test
    void testSerialize() throws Exception {
        ObjectValueExpression expression = new ObjectValueExpression(converter, "foo", Object.class);
        assertThat(deserialize(serialize(expression))).isEqualTo(expression);
    }
}
