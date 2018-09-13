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
package org.flowable.cmmn.engine.impl.el.function;

import java.util.Arrays;
import java.util.List;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * @author Joram Barrez
 */
public abstract class AbstractFlowableVariableExpressionFunction extends AbstractFlowableShortHandExpressionFunction {
    
    private static final List<String> FUNCTION_PREFIXES = Arrays.asList("variables", "vars", "var");
    
    private static final String FINAL_FUNCTION_PREFIX = "variables";
    
    public AbstractFlowableVariableExpressionFunction(String functionName) {
        this(Arrays.asList(functionName), functionName);
    }
    
    public AbstractFlowableVariableExpressionFunction(List<String> functionNameOptions, String functionName) {
        super(functionNameOptions, functionName);
    }
    
    @Override
    protected List<String> getFunctionPrefixOptions() {
        return FUNCTION_PREFIXES;
    }
    
    @Override
    protected String getFinalFunctionPrefix() {
        return FINAL_FUNCTION_PREFIX;
    }
    
    @Override
    protected boolean isMultiParameterFunction() {
        return true;
    }
    
    @Override
    public String prefix() {
        return "variables";
    }
    
    // Helper methods
    
    protected static Object getVariableValue(PlanItemInstance planItemInstance, String variableName) {
        if (variableName == null) {
            throw new FlowableIllegalArgumentException("Variable name passed is null");
        }
        return ((VariableScope) planItemInstance).getVariable((String) variableName);
    }
    
    protected static boolean valuesAreNumbers(Object variableValue, Object actualValue) {
        return actualValue instanceof Number && variableValue instanceof Number;
    }

}
