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
package org.flowable.common.engine.impl.de.odysseus.el.misc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import org.flowable.common.engine.impl.javax.el.ELException;
import org.junit.jupiter.api.Test;

/**
 * JUnit test case for {@link TypeConverterImpl}.
 *
 * @author Christoph Beck
 */
class TypeConverterImplTest {

    /**
     * Test property editor for date objects.
     * Accepts integer strings as text input and uses them as time value in milliseconds.
     */
    public static class DateEditor implements PropertyEditor {

        private Date value;

        public void addPropertyChangeListener(PropertyChangeListener listener) {
        }

        public String getAsText() {
            return value == null ? null : "" + value.getTime();
        }

        public Component getCustomEditor() {
            return null;
        }

        public String getJavaInitializationString() {
            return null;
        }

        public String[] getTags() {
            return null;
        }

        public Object getValue() {
            return value;
        }

        public boolean isPaintable() {
            return false;
        }

        public void paintValue(Graphics gfx, Rectangle box) {
        }

        public void removePropertyChangeListener(PropertyChangeListener listener) {
        }

        public void setAsText(String text) throws IllegalArgumentException {
            value = new Date(Long.parseLong(text));
        }

        public void setValue(Object value) {
            this.value = (Date) value;
        }

        public boolean supportsCustomEditor() {
            return false;
        }
    }

    static {
        PropertyEditorManager.registerEditor(Date.class, DateEditor.class);
    }

    /**
     * Test enum type
     */
    enum Foo {
        BAR,
        BAZ {
            @Override
            public String toString() {
                return "XXX";
            }
        }
    }

    TypeConverterImpl converter = new TypeConverterImpl();

    @Test
    void testToBoolean() {
        assertThat(converter.coerceToBoolean(null)).isFalse();
        assertThat(converter.coerceToBoolean("")).isFalse();
        assertThat(converter.coerceToBoolean(Boolean.TRUE)).isTrue();
        assertThat(converter.coerceToBoolean(Boolean.FALSE)).isFalse();
        assertThat(converter.coerceToBoolean("true")).isTrue();
        assertThat(converter.coerceToBoolean("false")).isFalse();
        assertThat(converter.coerceToBoolean("yes")).isFalse(); // Boolean.valueOf(String) never throws an exception...
    }

    @Test
    void testToCharacter() {
        assertThat(converter.coerceToCharacter(null)).isEqualTo(Character.valueOf((char) 0));
        assertThat(converter.coerceToCharacter("")).isEqualTo(Character.valueOf((char) 0));
        Character c = (char) 99;
        assertThat(converter.coerceToCharacter(c)).isSameAs(c);
        assertThatThrownBy(() -> converter.coerceToCharacter(Boolean.TRUE))
                .isInstanceOf(ELException.class);
        assertThatThrownBy(() -> converter.coerceToCharacter(Boolean.FALSE))
                .isInstanceOf(ELException.class);
        assertThat(converter.coerceToCharacter((byte) 99)).isEqualTo(c);
        assertThat(converter.coerceToCharacter((short) 99)).isEqualTo(c);
        assertThat(converter.coerceToCharacter(99)).isEqualTo(c);
        assertThat(converter.coerceToCharacter(99L)).isEqualTo(c);
        assertThat(converter.coerceToCharacter((float) 99.5)).isEqualTo(c);
        assertThat(converter.coerceToCharacter(99.5)).isEqualTo(c);
        assertThat(converter.coerceToCharacter(new BigDecimal("99.5"))).isEqualTo(c);
        assertThat(converter.coerceToCharacter(new BigInteger("99"))).isEqualTo(c);
        assertThat(converter.coerceToCharacter("c#")).isEqualTo(c);
        assertThatThrownBy(() -> converter.coerceToCharacter(this))
                .isInstanceOf(ELException.class);
    }

    @Test
    void testToLong() {
        Number zero = 0L;
        Number ninetynine = 99L;
        assertThat(converter.coerceToLong(null)).isEqualTo(zero);
        assertThat(converter.coerceToLong("")).isEqualTo(zero);
        assertThat(converter.coerceToLong('c')).isEqualTo(ninetynine);
        assertThat(converter.coerceToLong((byte) 99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToLong((short) 99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToLong(99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToLong(99L)).isEqualTo(ninetynine);
        assertThat(converter.coerceToLong(99F)).isEqualTo(ninetynine);
        assertThat(converter.coerceToLong(99.0)).isEqualTo(ninetynine);
        assertThat(converter.coerceToLong(new BigDecimal(99))).isEqualTo(ninetynine);
        assertThat(converter.coerceToLong(new BigInteger("99"))).isEqualTo(ninetynine);
        assertThat(converter.coerceToLong(ninetynine.toString())).isEqualTo(ninetynine);
        assertThatThrownBy(() -> converter.coerceToLong("foo"))
                .isInstanceOf(ELException.class);
    }

    @Test
    void testToInteger() {
        Number zero = 0;
        Number ninetynine = 99;
        assertThat(converter.coerceToInteger(null)).isEqualTo(zero);
        assertThat(converter.coerceToInteger("")).isEqualTo(zero);
        assertThat(converter.coerceToInteger('c')).isEqualTo(ninetynine);
        assertThat(converter.coerceToInteger((byte) 99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToInteger((short) 99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToInteger(99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToInteger(99L)).isEqualTo(ninetynine);
        assertThat(converter.coerceToInteger(99F)).isEqualTo(ninetynine);
        assertThat(converter.coerceToInteger(99.0)).isEqualTo(ninetynine);
        assertThat(converter.coerceToInteger(new BigDecimal(99))).isEqualTo(ninetynine);
        assertThat(converter.coerceToInteger(new BigInteger("99"))).isEqualTo(ninetynine);
        assertThat(converter.coerceToInteger(ninetynine.toString())).isEqualTo(ninetynine);
        assertThatThrownBy(() -> converter.coerceToInteger("foo"))
                .isInstanceOf(ELException.class);
    }

    @Test
    void testToShort() {
        Number zero = (short) 0;
        Number ninetynine = (short) 99;
        assertThat(converter.coerceToShort(null)).isEqualTo(zero);
        assertThat(converter.coerceToShort("")).isEqualTo(zero);
        assertThat(converter.coerceToShort('c')).isEqualTo(ninetynine);
        assertThat(converter.coerceToShort((byte) 99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToShort((short) 99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToShort(99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToShort(99L)).isEqualTo(ninetynine);
        assertThat(converter.coerceToShort(99F)).isEqualTo(ninetynine);
        assertThat(converter.coerceToShort(99.0)).isEqualTo(ninetynine);
        assertThat(converter.coerceToShort(new BigDecimal(99))).isEqualTo(ninetynine);
        assertThat(converter.coerceToShort(new BigInteger("99"))).isEqualTo(ninetynine);
        assertThat(converter.coerceToShort(ninetynine.toString())).isEqualTo(ninetynine);
        assertThatThrownBy(() -> converter.coerceToShort("foo"))
                .isInstanceOf(ELException.class);
    }

    @Test
    void testToByte() {
        Number zero = (byte) 0;
        Number ninetynine = (byte) 99;
        assertThat(converter.coerceToByte(null)).isEqualTo(zero);
        assertThat(converter.coerceToByte("")).isEqualTo(zero);
        assertThat(converter.coerceToByte('c')).isEqualTo(ninetynine);
        assertThat(converter.coerceToByte((byte) 99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToByte((short) 99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToByte(99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToByte(99L)).isEqualTo(ninetynine);
        assertThat(converter.coerceToByte(99F)).isEqualTo(ninetynine);
        assertThat(converter.coerceToByte(99.0)).isEqualTo(ninetynine);
        assertThat(converter.coerceToByte(new BigDecimal(99))).isEqualTo(ninetynine);
        assertThat(converter.coerceToByte(new BigInteger("99"))).isEqualTo(ninetynine);
        assertThat(converter.coerceToByte(ninetynine.toString())).isEqualTo(ninetynine);
        assertThatThrownBy(() -> converter.coerceToByte("foo"))
                .isInstanceOf(ELException.class);
    }

    @Test
    void testToDouble() {
        Number zero = (double) 0;
        Number ninetynine = 99.0;
        assertThat(converter.coerceToDouble(null)).isEqualTo(zero);
        assertThat(converter.coerceToDouble("")).isEqualTo(zero);
        assertThat(converter.coerceToDouble('c')).isEqualTo(ninetynine);
        assertThat(converter.coerceToDouble((byte) 99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToDouble((short) 99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToDouble(99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToDouble(99L)).isEqualTo(ninetynine);
        assertThat(converter.coerceToDouble(99F)).isEqualTo(ninetynine);
        assertThat(converter.coerceToDouble(99.0)).isEqualTo(ninetynine);
        assertThat(converter.coerceToDouble(new BigDecimal(99))).isEqualTo(ninetynine);
        assertThat(converter.coerceToDouble(new BigInteger("99"))).isEqualTo(ninetynine);
        assertThat(converter.coerceToDouble(ninetynine.toString())).isEqualTo(ninetynine);
        assertThatThrownBy(() -> converter.coerceToDouble("foo"))
                .isInstanceOf(ELException.class);
    }

    @Test
    void testToFloat() {
        Number zero = (float) 0;
        Number ninetynine = 99F;
        assertThat(converter.coerceToFloat(null)).isEqualTo(zero);
        assertThat(converter.coerceToFloat("")).isEqualTo(zero);
        assertThat(converter.coerceToFloat('c')).isEqualTo(ninetynine);
        assertThat(converter.coerceToFloat((byte) 99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToFloat((short) 99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToFloat(99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToFloat(99L)).isEqualTo(ninetynine);
        assertThat(converter.coerceToFloat(99F)).isEqualTo(ninetynine);
        assertThat(converter.coerceToFloat(99.0)).isEqualTo(ninetynine);
        assertThat(converter.coerceToFloat(new BigDecimal(99))).isEqualTo(ninetynine);
        assertThat(converter.coerceToFloat(new BigInteger("99"))).isEqualTo(ninetynine);
        assertThat(converter.coerceToFloat(ninetynine.toString())).isEqualTo(ninetynine);
        assertThatThrownBy(() -> converter.coerceToFloat("foo"))
                .isInstanceOf(ELException.class);
    }

    @Test
    void testToBigDecimal() {
        Number zero = BigDecimal.valueOf(0);
        Number ninetynine = BigDecimal.valueOf(99);
        assertThat(converter.coerceToBigDecimal(null)).isEqualTo(zero);
        assertThat(converter.coerceToBigDecimal("")).isEqualTo(zero);
        assertThat(converter.coerceToBigDecimal('c')).isEqualTo(ninetynine);
        assertThat(converter.coerceToBigDecimal((byte) 99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToBigDecimal((short) 99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToBigDecimal(99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToBigDecimal(99L)).isEqualTo(ninetynine);
        assertThat(converter.coerceToBigDecimal(99F)).isEqualTo(ninetynine);
        assertThat(converter.coerceToBigDecimal(99.0)).isEqualTo(ninetynine);
        assertThat(converter.coerceToBigDecimal(new BigDecimal(99))).isEqualTo(ninetynine);
        assertThat(converter.coerceToBigDecimal(new BigInteger("99"))).isEqualTo(ninetynine);
        assertThat(converter.coerceToBigDecimal(ninetynine.toString())).isEqualTo(ninetynine);
        assertThatThrownBy(() -> converter.coerceToBigDecimal("foo"))
                .isInstanceOf(ELException.class);
    }

    @Test
    void testToBigInteger() {
        Number zero = BigInteger.valueOf(0);
        Number ninetynine = BigInteger.valueOf(99);
        assertThat(converter.coerceToBigInteger(null)).isEqualTo(zero);
        assertThat(converter.coerceToBigInteger("")).isEqualTo(zero);
        assertThat(converter.coerceToBigInteger('c')).isEqualTo(ninetynine);
        assertThat(converter.coerceToBigInteger((byte) 99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToBigInteger((short) 99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToBigInteger(99)).isEqualTo(ninetynine);
        assertThat(converter.coerceToBigInteger(99L)).isEqualTo(ninetynine);
        assertThat(converter.coerceToBigInteger(99F)).isEqualTo(ninetynine);
        assertThat(converter.coerceToBigInteger(99.0)).isEqualTo(ninetynine);
        assertThat(converter.coerceToBigInteger(new BigDecimal(99))).isEqualTo(ninetynine);
        assertThat(converter.coerceToBigInteger(new BigInteger("99"))).isEqualTo(ninetynine);
        assertThat(converter.coerceToBigInteger(ninetynine.toString())).isEqualTo(ninetynine);
        assertThatThrownBy(() -> converter.coerceToBigInteger("foo"))
                .isInstanceOf(ELException.class);
    }

    @Test
    void testToString() {
        assertThat(converter.coerceToString("foo")).isSameAs("foo");
        assertThat(converter.coerceToString(null)).isEqualTo("");
        assertThat(converter.coerceToString(Foo.BAR)).isEqualTo(Foo.BAR.name());
        Object value = new BigDecimal("99.345");
        assertThat(converter.coerceToString(value)).isEqualTo(value.toString());
    }

    @Test
    void testToEnum() {
        assertThat(converter.coerceToEnum(null, Foo.class)).isNull();
        assertThat(converter.coerceToEnum(Foo.BAR, Foo.class)).isSameAs(Foo.BAR);
        assertThat(converter.coerceToEnum("", Foo.class)).isNull();
        assertThat(converter.coerceToEnum("BAR", Foo.class)).isSameAs(Foo.BAR);
        assertThat(converter.coerceToEnum("BAZ", Foo.class)).isSameAs(Foo.BAZ);
    }

    @Test
    void testToType() {
        assertThat(converter.coerceToType("foo", String.class)).isEqualTo("foo");
        assertThat(converter.coerceToType("0", Long.class)).isEqualTo(0L);
        assertThat(converter.coerceToType("c", Character.class)).isEqualTo('c');
        assertThat(converter.coerceToType("true", Boolean.class)).isEqualTo(Boolean.TRUE);
        assertThat(converter.coerceToType("BAR", Foo.class)).isEqualTo(Foo.BAR);
        // other types
        assertThat(converter.coerceToType(null, Object.class)).isNull();
        Object value = new Date(0);
        assertThat(converter.coerceToType(value, Object.class)).isSameAs(value);
        assertThat(converter.coerceToType("0", Date.class)).isEqualTo(new Date(0));
        assertThat(converter.coerceToType("", Date.class)).isNull();
        assertThatThrownBy(() -> converter.coerceToType("foo", Date.class))
                .isInstanceOf(ELException.class);
        assertThat(converter.coerceToType("", getClass())).isNull();
        assertThatThrownBy(() -> converter.coerceToType("bar", getClass()))
                .isInstanceOf(ELException.class);
        assertThat(converter.coerceToType("false", boolean.class)).isEqualTo(false);
        assertThat(converter.coerceToType("0", byte.class)).isEqualTo((byte) 0);
        assertThat(converter.coerceToType("0", short.class)).isEqualTo((short) 0);
        assertThat(converter.coerceToType("0", int.class)).isEqualTo(0);
        assertThat(converter.coerceToType("0", long.class)).isEqualTo((long) 0);
        assertThat(converter.coerceToType("0", float.class)).isEqualTo((float) 0);
        assertThat(converter.coerceToType("0", double.class)).isEqualTo((double) 0);
        assertThat(converter.coerceToType("0", char.class)).isEqualTo('0');
    }
}
