/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.math.BigDecimal;
import java.math.BigInteger;

import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ELResolver;

/**
 * An {@link ELResolver} that supports invoking the Jackson (2 and 3) value node accessor methods
 * (e.g. {@code asText()}, {@code asString()}, {@code textValue()}, {@code asInt()}, {@code isTextual()})
 * on plain {@link String}, {@link Number} and {@link Boolean} values.
 * <p>
 * Plain values are used in places where JSON value nodes used to be exposed
 * (e.g. the element variable of a multi instance or repetition collection that is a JSON array).
 * This resolver keeps expressions that call such methods working.
 * The methods behave like they do on the corresponding Jackson 2 value node,
 * which is the lenient superset of the Jackson 3 behavior.
 * Methods that are not supported by this resolver are left to the other resolvers.
 *
 * @author Filip Hrisafov
 */
public class JsonValueNodeMethodsELResolver extends ELResolver {

    protected static final Object NOT_RESOLVED = new Object();

    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        return null;
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {
        return null;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
        // Nothing to do, this resolver only supports invoking methods
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        return false;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return null;
    }

    @Override
    public Object invoke(ELContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {
        if (method == null || !isResolvable(base)) {
            return null;
        }

        Object result;
        if (params == null || params.length == 0) {
            result = invokeWithoutParameters(base, method.toString());
        } else if (params.length == 1) {
            result = invokeWithDefaultValue(base, method.toString(), params[0]);
        } else {
            result = NOT_RESOLVED;
        }

        if (result == NOT_RESOLVED) {
            return null;
        }

        context.setPropertyResolved(true);
        return result;
    }

    protected Object invokeWithoutParameters(Object base, String methodName) {
        return switch (methodName) {
            case "asText", "asString" -> base.toString();
            case "textValue", "stringValue" -> base instanceof String ? base : null;
            case "asInt" -> asInt(base, 0);
            case "asLong" -> asLong(base, 0L);
            case "asDouble" -> asDouble(base, 0.0);
            case "asBoolean" -> asBoolean(base, false);
            case "intValue" -> base instanceof Number number ? number.intValue() : 0;
            case "longValue" -> base instanceof Number number ? number.longValue() : 0L;
            case "doubleValue" -> base instanceof Number number ? number.doubleValue() : 0.0;
            case "floatValue" -> base instanceof Number number ? number.floatValue() : 0.0f;
            case "decimalValue" -> base instanceof Number number ? toBigDecimal(number) : BigDecimal.ZERO;
            case "bigIntegerValue" -> base instanceof Number number ? toBigDecimal(number).toBigInteger() : BigInteger.ZERO;
            case "numberValue" -> base instanceof Number ? base : null;
            case "booleanValue" -> base instanceof Boolean ? base : Boolean.FALSE;
            case "isTextual", "isString" -> base instanceof String;
            case "isNumber" -> base instanceof Number;
            case "isInt" -> base instanceof Integer;
            case "isLong" -> base instanceof Long;
            case "isDouble" -> base instanceof Double;
            case "isBigDecimal" -> base instanceof BigDecimal;
            case "isBigInteger" -> base instanceof BigInteger;
            case "isBoolean" -> base instanceof Boolean;
            case "isValueNode" -> true;
            case "isNull", "isMissingNode", "isContainerNode", "isContainer", "isObject", "isArray" -> false;
            default -> NOT_RESOLVED;
        };
    }

    protected Object invokeWithDefaultValue(Object base, String methodName, Object defaultValue) {
        return switch (methodName) {
            // Jackson returns the default value only for null or missing nodes
            case "asText", "asString" -> base.toString();
            case "asInt" -> defaultValue instanceof Number number ? asInt(base, number.intValue()) : NOT_RESOLVED;
            case "asLong" -> defaultValue instanceof Number number ? asLong(base, number.longValue()) : NOT_RESOLVED;
            case "asDouble" -> defaultValue instanceof Number number ? asDouble(base, number.doubleValue()) : NOT_RESOLVED;
            case "asBoolean" -> defaultValue instanceof Boolean booleanValue ? asBoolean(base, booleanValue) : NOT_RESOLVED;
            default -> NOT_RESOLVED;
        };
    }

    protected int asInt(Object base, int defaultValue) {
        if (base instanceof Number number) {
            return number.intValue();
        } else if (base instanceof Boolean booleanValue) {
            return booleanValue ? 1 : 0;
        }

        String value = ((String) base).trim();
        if (value.isEmpty()) {
            return defaultValue;
        }

        if (isIntegral(value)) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        try {
            return (int) Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    protected long asLong(Object base, long defaultValue) {
        if (base instanceof Number number) {
            return number.longValue();
        } else if (base instanceof Boolean booleanValue) {
            return booleanValue ? 1L : 0L;
        }

        String value = ((String) base).trim();
        if (value.isEmpty()) {
            return defaultValue;
        }

        if (isIntegral(value)) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        try {
            return (long) Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    protected double asDouble(Object base, double defaultValue) {
        if (base instanceof Number number) {
            return number.doubleValue();
        } else if (base instanceof Boolean booleanValue) {
            return booleanValue ? 1.0 : 0.0;
        }

        String value = ((String) base).trim();
        if (value.isEmpty()) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    protected boolean asBoolean(Object base, boolean defaultValue) {
        if (base instanceof Boolean booleanValue) {
            return booleanValue;
        } else if (base instanceof String string) {
            String value = string.trim();
            if ("true".equals(value)) {
                return true;
            } else if ("false".equals(value)) {
                return false;
            }
            return defaultValue;
        } else if (base instanceof BigInteger bigInteger) {
            return bigInteger.signum() != 0;
        } else if (base instanceof Integer || base instanceof Long || base instanceof Short || base instanceof Byte) {
            return ((Number) base).longValue() != 0;
        }

        // Jackson returns the default value for floating point numbers
        return defaultValue;
    }

    protected boolean isIntegral(String value) {
        int start = value.charAt(0) == '-' || value.charAt(0) == '+' ? 1 : 0;
        if (start == value.length()) {
            return false;
        }
        for (int i = start; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    protected BigDecimal toBigDecimal(Number number) {
        if (number instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        } else if (number instanceof BigInteger bigInteger) {
            return new BigDecimal(bigInteger);
        } else if (number instanceof Double || number instanceof Float) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.valueOf(number.longValue());
    }

    protected boolean isResolvable(Object base) {
        return base instanceof String || base instanceof Number || base instanceof Boolean;
    }
}
