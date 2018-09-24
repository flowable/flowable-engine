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
package org.flowable.common.engine.impl.el;

import java.util.Map;

import org.flowable.common.engine.api.variable.VariableContainer;

/**
 * @author Joram Barrez
 */
public class VariableContainerWrapper implements VariableContainer {
    
    protected Map<String, Object> variables;
    
    public VariableContainerWrapper(Map<String, Object> variables) {
        this.variables = variables;
    }

    @Override
    public boolean hasVariable(String variableName) {
        return variables.containsKey(variableName);
    }

    @Override
    public Object getVariable(String variableName) {
        return variables.get(variableName);
    }

    @Override
    public void setVariable(String variableName, Object variableValue) {
        variables.put(variableName, variableValue);
    }
    
    @Override
    public void setTransientVariable(String variableName, Object variableValue) {
        throw new UnsupportedOperationException();
    }

}
