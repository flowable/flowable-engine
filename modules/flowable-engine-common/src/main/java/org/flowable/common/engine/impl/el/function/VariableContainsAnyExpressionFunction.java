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

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.variable.VariableContainer;

import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Checks if the value of a variable (fetched using the variableName through the variable scope) contains any of the provided values.
 * 
 * Depending on the variable type, this means the following:
 * 
 * - {@link String}: following {@link StringUtils#contains(CharSequence, CharSequence)} semantics for one of the passed values
 * - {@link Collection}: following the {@link Collection#contains(Object)} for one of the passed values
 * - {@link ArrayNode}: supports checking if the arraynode contains a JsonNode for the types that are supported as variable type
 * 
 * When the variable value is null, false is returned in all cases.
 * When the variable value is not null, and the instance type is not one of the cases above, false will be returned.
 * 
 * @author Joram Barrez
 */
public class VariableContainsAnyExpressionFunction extends AbstractFlowableVariableExpressionFunction {
    
    public VariableContainsAnyExpressionFunction() {
        super("containsAny");
    }
    
    @SuppressWarnings({ "rawtypes"})
    public static boolean containsAny(VariableContainer variableContainer, String variableName, Object... values) {
        Object variableValue = getVariableValue(variableContainer, variableName);
        if (variableValue != null) {
            if (variableValue instanceof String variableStringValue) {
                for (Object value : values) {
                    String stringValue = (String) value;
                    if (StringUtils.contains(variableStringValue, stringValue)) {
                        return true;
                    }
                }
                return false;

            } else if (variableValue instanceof Collection collectionVariableValue) {
                for (Object value : values) {
                   if (VariableContainsExpressionFunction.collectionContains(collectionVariableValue, value)) {
                       return true;
                   }
                }
                return false;

            } else if (variableValue instanceof ArrayNode arrayNodeVariableValue) {
                for (Object value : values) {
                   if (VariableContainsExpressionFunction.arrayNodeContains(arrayNodeVariableValue, value)) {
                       return true;
                   }
                }
                return false;

            }
        }
        
        return false;
    }
    
}
