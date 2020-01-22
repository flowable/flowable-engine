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
 * @author Joram Barrez
 */
public class VariableGreaterThanOrEqualsExpressionFunction extends AbstractVariableComparatorExpressionFunction {

    public VariableGreaterThanOrEqualsExpressionFunction() {
        super(Arrays.asList("greaterThanOrEquals", "gte"), "greaterThanOrEquals");
    }
    
    public static boolean greaterThanOrEquals(VariableContainer variableContainer, String variableName, Object comparedValue) {
        return compareVariableValue(variableContainer, variableName, comparedValue, OPERATOR.GTE);
    }

}
