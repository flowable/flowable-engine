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

import java.util.Arrays;
import java.util.Objects;

import org.flowable.common.engine.api.variable.VariableContainer;

/**
 * Compares the value of a variable (fetched using the variableName through the variable scope)
 * with a value on equality. If the variable value is null, false is returned (unless compared to null).
 *  
 * @author Joram Barrez
 */
public class VariableEqualsExpressionFunction extends AbstractFlowableVariableExpressionFunction {
    
    public VariableEqualsExpressionFunction() {
        super(Arrays.asList("equals", "eq"), "equals");
    }
    
    public static boolean equals(VariableContainer variableContainer, String variableName, Object comparedValue) {
        
        Object variableValue = getVariableValue(variableContainer, variableName);
        if (comparedValue != null && variableValue != null) {
            
            // Numbers are not necessarily of the expected type due to coming from JUEL, 
            // (eg. variable can be an Integer but JUEL passes a Long). 
            // As such, a specific check for the supported Number variable types of Flowable is done.
            
            if (valuesAreNumbers(comparedValue, variableValue)) {
                if (variableValue instanceof Long) {
                    return ((Number) variableValue).longValue() == ((Number) comparedValue).longValue();

                } else if (variableValue instanceof Integer) {
                    return ((Number) variableValue).intValue() == ((Number) comparedValue).intValue();
                    
                } else if (variableValue instanceof Double) {
                    return ((Number) variableValue).doubleValue() == ((Number) comparedValue).doubleValue();
                    
                } else if (variableValue instanceof Float) {
                    return ((Number) variableValue).floatValue() == ((Number) comparedValue).floatValue();
                    
                } else if (variableValue instanceof Short) {
                    return ((Number) variableValue).shortValue() == ((Number) comparedValue).shortValue();
                    
                } // Other subtypes possible (e.g. BigDecimal, AtomicInteger, etc.), will fall back to default comparison
                
            }
            
        }
        
        return defaultEquals(comparedValue, variableValue);
    }

    protected static boolean defaultEquals(Object variableValue, Object actualValue) {
        return Objects.equals(actualValue, variableValue);
    }
    

}
