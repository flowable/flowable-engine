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
package org.flowable.common.engine.impl.el.function;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.variable.VariableContainer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Checks if the value of a variable (fetched using the variableName through the variable scope) contains all of the provided values.
 * 
 * Depending on the variable type, this means the following:
 * 
 * - {@link String}: following {@link StringUtils#contains(CharSequence, CharSequence)} semantics for all passed values
 * - {@link Collection}: following the {@link Collection#contains(Object)} for all passed values
 * - {@link ArrayNode}: supports checking if the arraynode contains a JsonNode for the types that are supported as variable type
 * 
 * When the variable value is null, false is returned in all cases.
 * When the variable value is not null, and the instance type is not one of the cases above, false will be returned.
 * 
 * @author Joram Barrez
 */
public class VariableContainsExpressionFunction extends AbstractFlowableVariableExpressionFunction {
    
    public VariableContainsExpressionFunction() {
        super("contains");
    }
    
    @SuppressWarnings({ "rawtypes"})
    public static boolean contains(VariableContainer variableContainer, String variableName, Object... values) {
        Object variableValue = getVariableValue(variableContainer, variableName);
        if (variableValue != null) {
            if (variableValue instanceof String variableStringValue) {
                for (Object value : values) {
                    String stringValue = (String) value;
                    if (!StringUtils.contains(variableStringValue, stringValue)) {
                        return false;
                    }
                }
                return true;

            } else if (variableValue instanceof Collection collectionVariableValue) {
                for (Object value : values) {
                   if (!collectionContains(collectionVariableValue, value)) {
                       return false;
                   }
                }
                return true;

            } else if (variableValue instanceof ArrayNode arrayNodeVariableValue) {
                for (Object value : values) {
                   if (!arrayNodeContains(arrayNodeVariableValue, value)) {
                       return false;
                   }
                }
                return true;

            }
        }
        
        return false;
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static boolean collectionContains( Collection collection, Object value) {
        if (value instanceof Number) { // Need to expliticaly check, as Numbers don't work nicely for contains
            Iterator<Object> iterator = collection.iterator();
            while (iterator.hasNext()) {
                Object collectionValue = iterator.next();
                if (collectionValue instanceof Long &&((Number) value).longValue() == ((Long) collectionValue).longValue()) {
                    return true;
                } else if (collectionValue instanceof Integer && ((Number) value).intValue() == ((Integer) collectionValue).intValue()) {
                    return true;
                } else if (collectionValue instanceof Double && ((Number) value).doubleValue() == ((Double) collectionValue).doubleValue()) {
                    return true;
                } else if (collectionValue instanceof Float && ((Number) value).floatValue() == ((Float) collectionValue).floatValue()) {
                    return true;
                } else if (collectionValue instanceof Short && ((Number) value).shortValue() == ((Short) collectionValue).shortValue()) {
                    return true;
                }
            }
            return false;
        } else {
            return collection.contains(value);
        }
    }
    
    public static boolean arrayNodeContains(ArrayNode arrayNode, Object value) {
        Iterator<JsonNode> iterator = arrayNode.iterator();
        while (iterator.hasNext()) {
            JsonNode jsonNode = iterator.next();
            if (value == null && jsonNode.isNull()) {
                return true;
            } else if (value != null) {
                if (value instanceof String && jsonNode.isTextual() && StringUtils.equals(jsonNode.asText(), (String) value)) {
                    return true;
                } else if (value instanceof Number && jsonNode.isLong() && jsonNode.longValue() == ((Number) value).longValue()) {
                    return true;
                } else if (value instanceof Number && jsonNode.isDouble() && jsonNode.doubleValue() == ((Number) value).doubleValue()) {
                    return true;
                } else if (value instanceof Number && jsonNode.isInt() && jsonNode.intValue() == ((Number) value).intValue()) {
                    return true;
                } else if (value instanceof Boolean && jsonNode.isBoolean() && jsonNode.booleanValue() == ((Boolean) value).booleanValue()) {
                    return true;
                }
            }
        }   
        return false;
    }
    
}
