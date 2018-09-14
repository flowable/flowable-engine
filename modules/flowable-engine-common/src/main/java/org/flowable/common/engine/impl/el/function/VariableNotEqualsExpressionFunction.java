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

import org.flowable.variable.api.delegate.VariableScope;

/**
 * Compares the value of a variable (fetched using the variableName through the variable scope).
 * with a value on inequality. If the variable value is null, false is returned (unless compared to null).
 *  
 * @author Joram Barrez
 */
public class VariableNotEqualsExpressionFunction extends AbstractFlowableVariableExpressionFunction {
    
    public VariableNotEqualsExpressionFunction(String variableScopeName) {
        super(variableScopeName, Arrays.asList("notEquals", "ne"), "notEquals");
    }

    public static boolean notEquals(VariableScope variableScope, String variableName, Object variableValue) {
        
        // Special handling for null: when the variable is null, false is returned.
        // This is similar to equals, where a null variable value will always return false 
        // (it's effectively ignored) - unless it's compared to null itself)
        
        Object actualValue = getVariableValue(variableScope, variableName);
        if (actualValue != null) {
            return !VariableEqualsExpressionFunction.equals(variableScope, variableName, variableValue);
        }
        
        return false;
    }
}
