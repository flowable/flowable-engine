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
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.variable.api.delegate.VariableScope;

import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Checks if the value of a variable (fetched using the variableName through the variable scope) is empty.
 * 
 * Depending on the variable type, this means the following:
 * 
 * - {@link String}: following {@link StringUtils#isEmpty(CharSequence)} semantics 
 * - {@link Collection}: if the collection has no elements
 * - {@link ArrayNode}: if the json array has no elements.
 * 
 * When the variable value is null, true is returned in all cases.
 * When the variale value is not null, and the instance type is not one of the cases above, false will be returned.
 * 
 * @author Joram Barrez
 */
public class VariableIsEmptyExpressionFunction extends AbstractFlowableVariableExpressionFunction {
    
    public VariableIsEmptyExpressionFunction(String variableScopeName) {
        super(variableScopeName, Arrays.asList("isEmpty", "empty"), "isEmpty");
    }
    
    @Override
    protected boolean isMultiParameterFunction() {
        return false;
    }
    
    @SuppressWarnings("rawtypes")
    public static boolean isEmpty(VariableScope variableScope, String variableName) {
        Object variableValue = getVariableValue(variableScope, variableName);
        if (variableValue == null)  {
            return true;
            
        } else if (variableValue instanceof String) {
            return StringUtils.isEmpty((String) variableValue);
            
        } else if  (variableValue instanceof Collection) {
            return CollectionUtil.isEmpty((Collection) variableValue);
            
        } else if (variableValue instanceof ArrayNode) {
            return ((ArrayNode) variableValue).size() == 0;
            
        } else {
            return false;
            
        }
    }

}
