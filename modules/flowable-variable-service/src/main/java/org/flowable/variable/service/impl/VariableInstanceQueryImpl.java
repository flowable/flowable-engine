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

package org.flowable.variable.service.impl;

import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.query.AbstractQuery;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.api.runtime.VariableInstanceQuery;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.types.CacheableVariable;
import org.flowable.variable.service.impl.types.JPAEntityListVariableType;
import org.flowable.variable.service.impl.types.JPAEntityVariableType;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class VariableInstanceQueryImpl extends AbstractQuery<VariableInstanceQuery, VariableInstance> implements VariableInstanceQuery {

    private static final long serialVersionUID = 1L;
    
    protected VariableServiceConfiguration variableServiceConfiguration;
    
    protected String id;
    protected String taskId;
    protected Set<String> taskIds;
    protected String executionId;
    protected Set<String> executionIds;
    protected String processInstanceId;
    protected String activityInstanceId;
    protected String variableName;
    protected String variableNameLike;
    protected boolean excludeTaskRelated;
    protected boolean excludeVariableInitialization;
    protected String scopeId;
    protected String subScopeId;
    protected String scopeType;
    protected QueryVariableValue queryVariableValue;
    protected boolean excludeLocalVariables;

    public VariableInstanceQueryImpl() {
    }

    public VariableInstanceQueryImpl(CommandContext commandContext, VariableServiceConfiguration variableServiceConfiguration) {
        super(commandContext);
        this.variableServiceConfiguration = variableServiceConfiguration;
    }

    public VariableInstanceQueryImpl(CommandExecutor commandExecutor, VariableServiceConfiguration variableServiceConfiguration) {
        super(commandExecutor);
        this.variableServiceConfiguration = variableServiceConfiguration;
    }

    @Override
    public VariableInstanceQuery id(String id) {
        this.id = id;
        return this;
    }

    @Override
    public VariableInstanceQueryImpl processInstanceId(String processInstanceId) {
        if (processInstanceId == null) {
            throw new FlowableIllegalArgumentException("processInstanceId is null");
        }
        this.processInstanceId = processInstanceId;
        return this;
    }

    @Override
    public VariableInstanceQueryImpl executionId(String executionId) {
        if (executionId == null) {
            throw new FlowableIllegalArgumentException("Execution id is null");
        }
        if (excludeLocalVariables) {
            throw new FlowableIllegalArgumentException("Cannot use executionId together with excludeLocalVariables");
        }
        this.executionId = executionId;
        return this;
    }

    @Override
    public VariableInstanceQueryImpl executionIds(Set<String> executionIds) {
        if (executionIds == null) {
            throw new FlowableIllegalArgumentException("executionIds is null");
        }
        if (executionIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("Set of executionIds is empty");
        }
        if (excludeLocalVariables) {
            throw new FlowableIllegalArgumentException("Cannot use executionIds together with excludeLocalVariables");
        }
        this.executionIds = executionIds;
        return this;
    }

    public VariableInstanceQuery activityInstanceId(String activityInstanceId) {
        this.activityInstanceId = activityInstanceId;
        return this;
    }

    @Override
    public VariableInstanceQuery taskId(String taskId) {
        if (taskId == null) {
            throw new FlowableIllegalArgumentException("taskId is null");
        }
        if (excludeTaskRelated) {
            throw new FlowableIllegalArgumentException("Cannot use taskId together with excludeTaskVariables");
        }
        if (excludeLocalVariables) {
            throw new FlowableIllegalArgumentException("Cannot use taskIds together with excludeLocalVariables");
        }
        this.taskId = taskId;
        return this;
    }

    @Override
    public VariableInstanceQueryImpl taskIds(Set<String> taskIds) {
        if (taskIds == null) {
            throw new FlowableIllegalArgumentException("taskIds is null");
        }
        if (taskIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("Set of taskIds is empty");
        }
        if (excludeTaskRelated) {
            throw new FlowableIllegalArgumentException("Cannot use taskIds together with excludeTaskVariables");
        }
        if (excludeLocalVariables) {
            throw new FlowableIllegalArgumentException("Cannot use taskIds together with excludeLocalVariables");
        }
        this.taskIds = taskIds;
        return this;
    }

    @Override
    public VariableInstanceQuery excludeTaskVariables() {
        if (taskId != null) {
            throw new FlowableIllegalArgumentException("Cannot use taskId together with excludeTaskVariables");
        }
        if (taskIds != null) {
            throw new FlowableIllegalArgumentException("Cannot use taskIds together with excludeTaskVariables");
        }
        excludeTaskRelated = true;
        return this;
    }

    @Override
    public VariableInstanceQuery excludeVariableInitialization() {
        excludeVariableInitialization = true;
        return this;
    }

    @Override
    public VariableInstanceQuery variableName(String variableName) {
        if (variableName == null) {
            throw new FlowableIllegalArgumentException("variableName is null");
        }
        this.variableName = variableName;
        return this;
    }

    @Override
    public VariableInstanceQuery variableValueEquals(String variableName, Object variableValue) {
        if (variableName == null) {
            throw new FlowableIllegalArgumentException("variableName is null");
        }
        if (variableValue == null) {
            throw new FlowableIllegalArgumentException("variableValue is null");
        }
        this.variableName = variableName;
        queryVariableValue = new QueryVariableValue(variableName, variableValue, QueryOperator.EQUALS, true);
        return this;
    }

    @Override
    public VariableInstanceQuery variableValueNotEquals(String variableName, Object variableValue) {
        if (variableName == null) {
            throw new FlowableIllegalArgumentException("variableName is null");
        }
        if (variableValue == null) {
            throw new FlowableIllegalArgumentException("variableValue is null");
        }
        this.variableName = variableName;
        queryVariableValue = new QueryVariableValue(variableName, variableValue, QueryOperator.NOT_EQUALS, true);
        return this;
    }

    @Override
    public VariableInstanceQuery variableValueLike(String variableName, String variableValue) {
        if (variableName == null) {
            throw new FlowableIllegalArgumentException("variableName is null");
        }
        if (variableValue == null) {
            throw new FlowableIllegalArgumentException("variableValue is null");
        }
        this.variableName = variableName;
        queryVariableValue = new QueryVariableValue(variableName, variableValue, QueryOperator.LIKE, true);
        return this;
    }

    @Override
    public VariableInstanceQuery variableValueLikeIgnoreCase(String variableName, String variableValue) {
        if (variableName == null) {
            throw new FlowableIllegalArgumentException("variableName is null");
        }
        if (variableValue == null) {
            throw new FlowableIllegalArgumentException("variableValue is null");
        }
        this.variableName = variableName;
        queryVariableValue = new QueryVariableValue(variableName, variableValue.toLowerCase(), QueryOperator.LIKE_IGNORE_CASE, true);
        return this;
    }

    @Override
    public VariableInstanceQuery variableNameLike(String variableNameLike) {
        if (variableNameLike == null) {
            throw new FlowableIllegalArgumentException("variableNameLike is null");
        }
        this.variableNameLike = variableNameLike;
        return this;
    }
    
    @Override
    public VariableInstanceQuery scopeId(String scopeId) {
        this.scopeId = scopeId;
        return this;
    }
    
    @Override
    public VariableInstanceQuery subScopeId(String subScopeId) {
        if (excludeLocalVariables) {
            throw new FlowableIllegalArgumentException("Cannot use subScopeId together with excludeLocalVariables");
        }
        this.subScopeId = subScopeId;
        return this;
    }
    
    @Override
    public VariableInstanceQuery scopeType(String scopeType) {
        this.scopeType = scopeType;
        return this;
    }

    @Override
    public VariableInstanceQuery excludeLocalVariables() {
        if (taskId != null) {
            throw new FlowableIllegalArgumentException("Cannot use taskId together with excludeLocalVariables");
        }
        if (taskIds != null) {
            throw new FlowableIllegalArgumentException("Cannot use taskIds together with excludeLocalVariables");
        }
        if (executionId != null) {
            throw new FlowableIllegalArgumentException("Cannot use executionId together with excludeLocalVariables");
        }
        if (subScopeId != null) {
            throw new FlowableIllegalArgumentException("Cannot use subScopeId together with excludeLocalVariables");
        }
        excludeLocalVariables = true;
        return this;
    }

    protected void ensureVariablesInitialized() {
        if (this.queryVariableValue != null) {
            queryVariableValue.initialize(variableServiceConfiguration);
        }
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        ensureVariablesInitialized();
        return variableServiceConfiguration.getVariableInstanceEntityManager().findVariableInstanceCountByQueryCriteria(this);
    }

    @Override
    public List<VariableInstance> executeList(CommandContext commandContext) {
        ensureVariablesInitialized();

        List<VariableInstance> variableInstances = variableServiceConfiguration.getVariableInstanceEntityManager().findVariableInstancesByQueryCriteria(this);

        if (!excludeVariableInitialization) {
            for (VariableInstance variableInstance : variableInstances) {
                if (variableInstance instanceof VariableInstanceEntity variableEntity) {
                    if (variableEntity.getType() != null) {
                        variableEntity.getValue();

                        // make sure JPA entities are cached for later retrieval
                        if (JPAEntityVariableType.TYPE_NAME.equals(variableEntity.getType().getTypeName()) || JPAEntityListVariableType.TYPE_NAME.equals(variableEntity.getType().getTypeName())) {
                            ((CacheableVariable) variableEntity.getType()).setForceCacheable(true);
                        }
                    }
                }
            }
        }
        return variableInstances;
    }

    // order by
    // /////////////////////////////////////////////////////////////////

    @Override
    public VariableInstanceQuery orderByProcessInstanceId() {
        orderBy(HistoricVariableInstanceQueryProperty.PROCESS_INSTANCE_ID);
        return this;
    }

    @Override
    public VariableInstanceQuery orderByVariableName() {
        orderBy(HistoricVariableInstanceQueryProperty.VARIABLE_NAME);
        return this;
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getActivityInstanceId() {
        return activityInstanceId;
    }

    public boolean getExcludeTaskRelated() {
        return excludeTaskRelated;
    }

    public String getVariableName() {
        return variableName;
    }

    public String getVariableNameLike() {
        return variableNameLike;
    }
    
    public String getScopeId() {
        return scopeId;
    }
    
    public String getSubScopeId() {
        return subScopeId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public String getId() {
        return id;
    }

    public Set<String> getTaskIds() {
        return taskIds;
    }

    public String getExecutionId() {
        return executionId;
    }

    public Set<String> getExecutionIds() {
        return executionIds;
    }

    public boolean isExcludeTaskRelated() {
        return excludeTaskRelated;
    }

    public boolean isExcludeVariableInitialization() {
        return excludeVariableInitialization;
    }

    public QueryVariableValue getQueryVariableValue() {
        return queryVariableValue;
    }

    public boolean isExcludeLocalVariables() {
        return excludeLocalVariables;
    }

}
