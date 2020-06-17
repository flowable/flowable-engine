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

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.variable.VariableContainer;

/**
 * Returns the Base64 encoded value of a variable.
 * 
 * @author Joram Barrez
 */
public class VariableBase64ExpressionFunction extends AbstractFlowableVariableExpressionFunction {
    
    public VariableBase64ExpressionFunction() {
        super("base64");
    }
    
    public static Object base64(VariableContainer variableContainer, String variableName) {
        Object value = getVariableValue(variableContainer, variableName);

        if (value == null) {
            return null;
        } else if (value instanceof Byte[] || value instanceof byte[]) {
            return java.util.Base64.getEncoder().encodeToString( (byte[]) value);
        } else if (value instanceof String) {
            return java.util.Base64.getEncoder().encodeToString(((String) value).getBytes());
        } else {
            throw new FlowableIllegalArgumentException("Variable type must be byte[] or string");
        }
    }

}
