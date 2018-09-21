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

import org.flowable.common.engine.impl.javax.el.PropertyNotFoundException;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * Returns the value of a variable, or a default if the value is null.
 * This avoids the {@link PropertyNotFoundException} that otherwise gets thrown when referencing a variable in JUEL.
 * 
 * @author Joram Barrez
 */
public class VariableGetOrDefaultExpressionFunction extends AbstractFlowableVariableExpressionFunction {
    
    public VariableGetOrDefaultExpressionFunction(String variableScopeName) {
        super(variableScopeName, "getOrDefault");
    }

    public static Object getOrDefault(VariableScope variableScope, String variableName, Object value) {
        Object variableValue = getVariableValue(variableScope, variableName);
        if (variableValue != null) {
            return variableValue;
        } else {
            return value;
        }
    }

}
