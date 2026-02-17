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

import java.util.Set;

import org.flowable.common.engine.api.variable.VariableContainer;

/**
 * A {@link VariableContainer} that carries definition context (definitionId, definitionKey, deploymentId, scopeType, tenantId)
 * for expression evaluation when no execution is active (e.g. during deployment).
 * This allows EL resolvers to look up the parent deployment and resolve definition-scoped expressions.
 *
 * @author Christopher Welsch
 */
public class DefinitionVariableContainer implements VariableContainer {

    protected String definitionId;
    protected String definitionKey;
    protected String deploymentId;
    protected String scopeType;
    protected String tenantId;

    public DefinitionVariableContainer(String definitionId, String definitionKey, String deploymentId, String scopeType, String tenantId) {
        this.definitionId = definitionId;
        this.definitionKey = definitionKey;
        this.deploymentId = deploymentId;
        this.scopeType = scopeType;
        this.tenantId = tenantId;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public String getDefinitionKey() {
        return definitionKey;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public String getScopeType() {
        return scopeType;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public boolean hasVariable(String variableName) {
        return false;
    }

    @Override
    public Object getVariable(String variableName) {
        return null;
    }

    @Override
    public void setVariable(String variableName, Object variableValue) {

    }

    @Override
    public void setTransientVariable(String variableName, Object variableValue) {

    }

    @Override
    public Set<String> getVariableNames() {
        return Set.of();
    }
}
