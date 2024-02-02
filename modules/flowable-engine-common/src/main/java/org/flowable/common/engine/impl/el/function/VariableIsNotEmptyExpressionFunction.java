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

import org.flowable.common.engine.api.variable.VariableContainer;

/**
 * Opposite operation of the ${@link VariableIsEmptyExpressionFunction} .
 *  
 * @author Joram Barrez
 */
public class VariableIsNotEmptyExpressionFunction extends AbstractFlowableVariableExpressionFunction {
    
    public VariableIsNotEmptyExpressionFunction() {
        super(Arrays.asList("isNotEmpty", "notEmpty"), "isNotEmpty");
    }
    
    public static boolean isNotEmpty(VariableContainer variableContainer, String variableName) {
        return !VariableIsEmptyExpressionFunction.isEmpty(variableContainer, variableName);
    }

}
