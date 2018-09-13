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

import org.flowable.cmmn.api.runtime.PlanItemInstance;

/**
 * Returns whether or not a variable with the given name exists when fetched through the provided {@link PlanItemInstance}.
 * 
 * @author Joram Barrez
 */
public class VariableExistsExpressionFunction extends AbstractFlowableVariableExpressionFunction {
    
    public VariableExistsExpressionFunction() {
        super(Arrays.asList("exists", "exist"), "exists");
    }
    
    @Override
    protected boolean isMultiParameterFunction() {
        return false;
    }
    
    public static boolean exists(PlanItemInstance planItemInstance, String variableName) {
        return getVariableValue(planItemInstance, variableName) != null;
    }

}
