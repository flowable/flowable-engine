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

import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.javax.el.PropertyNotFoundException;

/**
 * Returns the value of a variable (even if it's null), or a default if the variable is not found.
 * This avoids the {@link PropertyNotFoundException} that otherwise gets thrown when referencing a variable in JUEL.
 *
 * @author Balavivek Bala Naga Sethuraja
 */
public class VariableGetOrDefaultIfNotExistExpressionFunction extends AbstractFlowableVariableExpressionFunction {

    public VariableGetOrDefaultIfNotExistExpressionFunction() {
        super("getOrDefaultIfNotExist");
    }

    public static Object getOrDefaultIfNotExist(VariableContainer variableContainer, String variableName, Object value) {
        if (variableContainer.hasVariable(variableName)) {
            return getVariableValue(variableContainer, variableName);
        } else {
            return value;
        }
    }
}
