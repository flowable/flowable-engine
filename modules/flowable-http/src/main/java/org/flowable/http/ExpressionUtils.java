package org.flowable.http;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.delegate.Expression;
import org.flowable.engine.common.api.variable.VariableContainer;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author martin.grofcik
 */
public abstract class ExpressionUtils {
    private ExpressionUtils() {
        throw new RuntimeException("Instantiation not supported");
    }

    public static int getIntFromField(Expression expression, VariableContainer execution) {
        if (expression != null) {
            Object value = expression.getValue(execution);
            return parseInt(value);
        }
        return 0;
    }

    public static int parseInt(Object value) {
        if (value != null) {
            return Integer.parseInt(value.toString());
        }
        return 0;
    }

    public static boolean getBooleanFromField(final Expression expression, final VariableContainer execution) {
        if (expression != null) {
            Object value = expression.getValue(execution);
            return parseBoolean(value);
        }
        return false;
    }

    public static boolean parseBoolean(Object value) {
        if (value != null) {
            if (value instanceof String) {
                String stringValue = (String) value;
                if (stringValue.equalsIgnoreCase("true") || stringValue.equalsIgnoreCase("false")) {
                    return Boolean.parseBoolean(value.toString());
                }
                throw new FlowableException("String value \"" + value + "\" is not alloved in boolean expression");
            }
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            throw new FlowableException("Value \"" + value + "\" can not be converted into boolean");
        }
        return false;
    }

    public static String getStringFromField(final Expression expression, final VariableContainer execution) {
        if (expression != null) {
            Object value = expression.getValue(execution);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    public static Set<String> getStringSetFromField(final String field) {
        String[] codes = field.split(",");
        Set<String> codeSet = new HashSet<>(Arrays.asList(codes));
        Collections.addAll(codeSet, codes);
        return codeSet;
    }

}
