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
 * Returns whether or not a variable with the given name exists when fetched through the provided variable scope.
 * 
 * @author Joram Barrez
 */
public class VariableExistsExpressionFunction extends AbstractFlowableVariableExpressionFunction {
    
    public VariableExistsExpressionFunction(String variableScopeName) {
        super(variableScopeName, Arrays.asList("exists", "exist"), "exists");
    }
    
    @Override
    protected boolean isMultiParameterFunction() {
        return false;
    }
    
    public static boolean exists(VariableScope variableScope, String variableName) {
        return getVariableValue(variableScope, variableName) != null;
    }

}
