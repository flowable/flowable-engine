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
package org.flowable.cmmn.engine.impl.el;

import java.util.Objects;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.delegate.FlowableExpressionEnhancer;
import org.flowable.common.engine.api.delegate.FlowableFunctionDelegate;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * A class containing the static functions that can be used in an {@link Expression}.
 * 
 * To register a custom function:
 * 
 * - Create the appropriate {@link FlowableFunctionDelegate} and register it with the {@link ExpressionManager}. 
 *   The function delegate will link to this class thtrough reflection.
 * - Create the appropriate {@link FlowableExpressionEnhancer} that enhances function shorthand to their real function call (if needed).  
 * 
 * @author Joram Barrez
 */
public class VariableExpressionFunctionsUtil {
    
    public static boolean equals(PlanItemInstance planItemInstance, String variableName, Object variableValue) {
        
        if (variableName == null) {
            throw new FlowableIllegalArgumentException("Variable name passed is null");
        }
        
        Object actualValue = ((VariableScope) planItemInstance).getVariable((String) variableName);
        
        if (variableValue != null && actualValue != null) {
            
            // Numbers are not necessarily of the expected type due to coming from JUEL, 
            // (eg. variable can be an Integer but JUEL passes a Long). 
            // As such, a specific check for the supported Number variable types of Flowable is done.
            
            if (valuesAreNumbers(variableValue, actualValue)) {
                if (actualValue instanceof Long) {
                    return ((Number) actualValue).longValue() == ((Number) variableValue).longValue();

                } else if(actualValue instanceof Integer) {
                    return ((Number) actualValue).intValue() == ((Number) variableValue).intValue();
                    
                } else if(actualValue instanceof Double) {
                    return ((Number) actualValue).doubleValue() == ((Number) variableValue).doubleValue();
                    
                } else if(actualValue instanceof Float) {
                    return ((Number) actualValue).floatValue() == ((Number) variableValue).floatValue();
                    
                } else if(actualValue instanceof Short) {
                    return ((Number) actualValue).shortValue() == ((Number) variableValue).shortValue();
                    
                } // Other subtypes possible (e.g. BigDecimal, AtomicInteger, etc.), will fall back to default comparison
                
            }
            
        }
        
        return defaultEquals(variableValue, actualValue);
    }

    protected static boolean valuesAreNumbers(Object variableValue, Object actualValue) {
        return actualValue instanceof Number && variableValue instanceof Number;
    }
    
    protected static boolean defaultEquals(Object variableValue, Object actualValue) {
        return Objects.equals(actualValue, variableValue);
    }
    
    public static boolean notEquals(PlanItemInstance planItemInstance, String variableName, Object variableValue) {
        return !equals(planItemInstance, variableName, variableValue);
    }

}
