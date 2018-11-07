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
package org.flowable.cmmn.engine.impl.history;

import java.util.List;
import java.util.Set;

import org.flowable.cmmn.api.history.HistoricVariableInstanceQuery;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.service.impl.HistoricVariableInstanceQueryImpl;

/**
 * Wrapper class around the {@link HistoricVariableInstanceQueryImpl} from the variable service,
 * specialized for usage in CMMN.
 * 
 * @author Joram Barrez
 */
public class CmmnHistoricVariableInstanceQueryImpl implements HistoricVariableInstanceQuery {
    
    protected HistoricVariableInstanceQueryImpl wrappedHistoricVariableInstanceQuery;
    
    public CmmnHistoricVariableInstanceQueryImpl(CommandExecutor commandExecutor) {
        this.wrappedHistoricVariableInstanceQuery = new HistoricVariableInstanceQueryImpl(commandExecutor);
    }

    @Override
    public HistoricVariableInstanceQuery id(String id) {
        wrappedHistoricVariableInstanceQuery.id(id);
        return this;
    }

    @Override
    public HistoricVariableInstanceQuery caseInstanceId(String caseInstanceId) {
        wrappedHistoricVariableInstanceQuery.scopeId(caseInstanceId);
        wrappedHistoricVariableInstanceQuery.scopeType(ScopeTypes.CMMN);
        return this;
    }
    
    @Override
    public HistoricVariableInstanceQuery planItemInstanceId(String planItemInstanceId) {
        wrappedHistoricVariableInstanceQuery.subScopeId(planItemInstanceId);
        wrappedHistoricVariableInstanceQuery.scopeType(ScopeTypes.CMMN);
        return this;
    }

    @Override
    public HistoricVariableInstanceQuery taskId(String taskId) {
        wrappedHistoricVariableInstanceQuery.taskId(taskId);
        return this;
    }

    @Override
    public HistoricVariableInstanceQuery taskIds(Set<String> taskIds) {
        wrappedHistoricVariableInstanceQuery.taskIds(taskIds);
        return this;
    }

    @Override
    public HistoricVariableInstanceQuery variableName(String variableName) {
        wrappedHistoricVariableInstanceQuery.variableName(variableName);
        return this;
    }

    @Override
    public HistoricVariableInstanceQuery variableNameLike(String variableNameLike) {
        wrappedHistoricVariableInstanceQuery.variableNameLike(variableNameLike);
        return this;
    }

    @Override
    public HistoricVariableInstanceQuery excludeTaskVariables() {
        wrappedHistoricVariableInstanceQuery.excludeTaskVariables();
        return this;
    }

    @Override
    public HistoricVariableInstanceQuery excludeVariableInitialization() {
        wrappedHistoricVariableInstanceQuery.excludeVariableInitialization();
        return this;
    }

    @Override
    public HistoricVariableInstanceQuery variableValueEquals(String variableName, Object variableValue) {
        wrappedHistoricVariableInstanceQuery.variableValueEquals(variableName, variableValue);
        return this;
    }

    @Override
    public HistoricVariableInstanceQuery variableValueNotEquals(String variableName, Object variableValue) {
        wrappedHistoricVariableInstanceQuery.variableValueNotEquals(variableName, variableValue);
        return this;
    }

    @Override
    public HistoricVariableInstanceQuery variableValueLike(String variableName, String variableValue) {
        wrappedHistoricVariableInstanceQuery.variableValueLike(variableName, variableValue);
        return this;
    }

    @Override
    public HistoricVariableInstanceQuery variableValueLikeIgnoreCase(String variableName, String variableValue) {
        wrappedHistoricVariableInstanceQuery.variableValueLikeIgnoreCase(variableName, variableValue);
        return this;
    }

    @Override
    public HistoricVariableInstanceQuery orderByVariableName() {
        wrappedHistoricVariableInstanceQuery.orderByVariableName();
        return this;
    }

    @Override
    public HistoricVariableInstanceQuery asc() {
        wrappedHistoricVariableInstanceQuery.asc();
        return this;
    }

    @Override
    public HistoricVariableInstanceQuery desc() {
        wrappedHistoricVariableInstanceQuery.desc();
        return this;
    }

    @Override
    public HistoricVariableInstanceQuery orderBy(QueryProperty property) {
        wrappedHistoricVariableInstanceQuery.orderBy(property);
        return this;
    }

    @Override
    public HistoricVariableInstanceQuery orderBy(QueryProperty property, NullHandlingOnOrder nullHandlingOnOrder) {
        wrappedHistoricVariableInstanceQuery.orderBy(property, nullHandlingOnOrder);
        return this;
    }

    @Override
    public long count() {
        return wrappedHistoricVariableInstanceQuery.count();
    }

    @Override
    public HistoricVariableInstance singleResult() {
        return wrappedHistoricVariableInstanceQuery.singleResult();
    }

    @Override
    public List<HistoricVariableInstance> list() {
        return wrappedHistoricVariableInstanceQuery.list();
    }

    @Override
    public List<HistoricVariableInstance> listPage(int firstResult, int maxResults) {
        return wrappedHistoricVariableInstanceQuery.listPage(firstResult, maxResults);
    }

}
