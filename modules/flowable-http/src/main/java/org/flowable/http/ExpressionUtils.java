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
package org.flowable.http;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.variable.VariableContainer;

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
            if (value != null) {
                return Integer.parseInt(value.toString());
            }
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

    protected static boolean parseBoolean(Object value) {
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
