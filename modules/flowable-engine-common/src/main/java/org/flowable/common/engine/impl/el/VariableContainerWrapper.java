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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.flowable.common.engine.api.variable.VariableContainer;

/**
 * @author Joram Barrez
 */
public class VariableContainerWrapper implements VariableContainer {
    
    protected Map<String, Object> variables = new HashMap<>();
    protected String instanceId;
    protected String scopeType;
    protected String tenantId;
    
    public VariableContainerWrapper(Map<String, Object> variables) {
        if (variables != null) {
            this.variables.putAll(variables);
        }
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

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public Set<String> getVariableNames() {
        return variables.keySet();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add("instanceId='" + instanceId + "'")
                .add("scopeType='" + scopeType + "'")
                .add("tenantId='" + tenantId + "'")
                .toString();
    }
}
