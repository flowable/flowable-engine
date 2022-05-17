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
package org.flowable.cmmn.engine.impl.runtime;

import java.util.List;
import java.util.Set;

import org.flowable.cmmn.api.runtime.VariableInstanceQuery;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.VariableInstanceQueryImpl;

/**
 * Wrapper class around the {@link VariableInstanceQueryImpl} from the variable service, specialized for usage in CMMN.
 * 
 * @author Joram Barrez
 */
public class CmmnVariableInstanceQueryImpl implements VariableInstanceQuery {
    
    protected VariableInstanceQueryImpl wrappedVariableInstanceQuery;
    
    public CmmnVariableInstanceQueryImpl(CommandExecutor commandExecutor, CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.wrappedVariableInstanceQuery = new VariableInstanceQueryImpl(commandExecutor, 
                cmmnEngineConfiguration.getVariableServiceConfiguration());
    }

    @Override
    public VariableInstanceQuery id(String id) {
        wrappedVariableInstanceQuery.id(id);
        return this;
    }

    @Override
    public VariableInstanceQuery caseInstanceId(String caseInstanceId) {
        wrappedVariableInstanceQuery.scopeId(caseInstanceId);
        wrappedVariableInstanceQuery.scopeType(ScopeTypes.CMMN);
        return this;
    }
    
    @Override
    public VariableInstanceQuery planItemInstanceId(String planItemInstanceId) {
        wrappedVariableInstanceQuery.subScopeId(planItemInstanceId);
        wrappedVariableInstanceQuery.scopeType(ScopeTypes.CMMN);
        return this;
    }

    @Override
    public VariableInstanceQuery taskId(String taskId) {
        wrappedVariableInstanceQuery.taskId(taskId);
        return this;
    }

    @Override
    public VariableInstanceQuery taskIds(Set<String> taskIds) {
        wrappedVariableInstanceQuery.taskIds(taskIds);
        return this;
    }

    @Override
    public VariableInstanceQuery variableName(String variableName) {
        wrappedVariableInstanceQuery.variableName(variableName);
        return this;
    }

    @Override
    public VariableInstanceQuery variableNameLike(String variableNameLike) {
        wrappedVariableInstanceQuery.variableNameLike(variableNameLike);
        return this;
    }

    @Override
    public VariableInstanceQuery excludeTaskVariables() {
        wrappedVariableInstanceQuery.excludeTaskVariables();
        return this;
    }

    @Override
    public VariableInstanceQuery excludeLocalVariables() {
        wrappedVariableInstanceQuery.excludeLocalVariables();
        return this;
    }

    @Override
    public VariableInstanceQuery excludeVariableInitialization() {
        wrappedVariableInstanceQuery.excludeVariableInitialization();
        return this;
    }

    @Override
    public VariableInstanceQuery variableValueEquals(String variableName, Object variableValue) {
        wrappedVariableInstanceQuery.variableValueEquals(variableName, variableValue);
        return this;
    }

    @Override
    public VariableInstanceQuery variableValueNotEquals(String variableName, Object variableValue) {
        wrappedVariableInstanceQuery.variableValueNotEquals(variableName, variableValue);
        return this;
    }

    @Override
    public VariableInstanceQuery variableValueLike(String variableName, String variableValue) {
        wrappedVariableInstanceQuery.variableValueLike(variableName, variableValue);
        return this;
    }

    @Override
    public VariableInstanceQuery variableValueLikeIgnoreCase(String variableName, String variableValue) {
        wrappedVariableInstanceQuery.variableValueLikeIgnoreCase(variableName, variableValue);
        return this;
    }

    @Override
    public VariableInstanceQuery orderByVariableName() {
        wrappedVariableInstanceQuery.orderByVariableName();
        return this;
    }

    @Override
    public VariableInstanceQuery asc() {
        wrappedVariableInstanceQuery.asc();
        return this;
    }

    @Override
    public VariableInstanceQuery desc() {
        wrappedVariableInstanceQuery.desc();
        return this;
    }

    @Override
    public VariableInstanceQuery orderBy(QueryProperty property) {
        wrappedVariableInstanceQuery.orderBy(property);
        return this;
    }

    @Override
    public VariableInstanceQuery orderBy(QueryProperty property, NullHandlingOnOrder nullHandlingOnOrder) {
        wrappedVariableInstanceQuery.orderBy(property, nullHandlingOnOrder);
        return this;
    }

    @Override
    public long count() {
        return wrappedVariableInstanceQuery.count();
    }

    @Override
    public VariableInstance singleResult() {
        return wrappedVariableInstanceQuery.singleResult();
    }

    @Override
    public List<VariableInstance> list() {
        return wrappedVariableInstanceQuery.list();
    }

    @Override
    public List<VariableInstance> listPage(int firstResult, int maxResults) {
        return wrappedVariableInstanceQuery.listPage(firstResult, maxResults);
    }

}
