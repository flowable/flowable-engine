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
import java.util.Collections;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.el.VariableContainerELResolver;

/**
 * @author Joram Barrez
 */
public abstract class AbstractFlowableVariableExpressionFunction extends AbstractFlowableShortHandExpressionFunction {
    
    private static final List<String> FUNCTION_PREFIXES = Arrays.asList("variables", "vars", "var");
    
    private static final String FINAL_FUNCTION_PREFIX = "variables";
    
    public AbstractFlowableVariableExpressionFunction(String functionName) {
        this(Collections.singletonList(functionName), functionName);
    }
    
    public AbstractFlowableVariableExpressionFunction(List<String> functionNameOptions, String functionName) {
        super(VariableContainerELResolver.VARIABLE_CONTAINER_KEY, functionNameOptions, functionName);
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
    
    protected static Object getVariableValue(VariableContainer variableContainer, String variableName) {
        if (variableName == null) {
            throw new FlowableIllegalArgumentException("Variable name passed is null");
        }
        return variableContainer.getVariable((String) variableName);
    }
    
    protected static boolean valuesAreNumbers(Object variableValue, Object actualValue) {
        return actualValue instanceof Number && variableValue instanceof Number;
    }

}
