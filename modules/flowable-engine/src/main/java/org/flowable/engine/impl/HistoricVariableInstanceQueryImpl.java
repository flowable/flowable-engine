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

package org.flowable.engine.impl;

import java.util.List;
import java.util.Set;

import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.common.impl.interceptor.CommandExecutor;
import org.flowable.engine.history.HistoricVariableInstance;
import org.flowable.engine.history.HistoricVariableInstanceQuery;
import org.flowable.engine.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.variable.CacheableVariable;
import org.flowable.engine.impl.variable.JPAEntityListVariableType;
import org.flowable.engine.impl.variable.JPAEntityVariableType;
import org.flowable.engine.impl.variable.VariableTypes;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class HistoricVariableInstanceQueryImpl extends AbstractQuery<HistoricVariableInstanceQuery, HistoricVariableInstance> implements HistoricVariableInstanceQuery {

    private static final long serialVersionUID = 1L;
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
    protected QueryVariableValue queryVariableValue;

    public HistoricVariableInstanceQueryImpl() {
    }

    public HistoricVariableInstanceQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public HistoricVariableInstanceQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    public HistoricVariableInstanceQuery id(String id) {
        this.id = id;
        return this;
    }

    public HistoricVariableInstanceQueryImpl processInstanceId(String processInstanceId) {
        if (processInstanceId == null) {
            throw new FlowableIllegalArgumentException("processInstanceId is null");
        }
        this.processInstanceId = processInstanceId;
        return this;
    }

    public HistoricVariableInstanceQueryImpl executionId(String executionId) {
        if (executionId == null) {
            throw new FlowableIllegalArgumentException("Execution id is null");
        }
        this.executionId = executionId;
        return this;
    }

    public HistoricVariableInstanceQueryImpl executionIds(Set<String> executionIds) {
        if (executionIds == null) {
            throw new FlowableIllegalArgumentException("executionIds is null");
        }
        if (executionIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("Set of executionIds is empty");
        }
        this.executionIds = executionIds;
        return this;
    }

    public HistoricVariableInstanceQuery activityInstanceId(String activityInstanceId) {
        this.activityInstanceId = activityInstanceId;
        return this;
    }

    public HistoricVariableInstanceQuery taskId(String taskId) {
        if (taskId == null) {
            throw new FlowableIllegalArgumentException("taskId is null");
        }
        if (excludeTaskRelated) {
            throw new FlowableIllegalArgumentException("Cannot use taskId together with excludeTaskVariables");
        }
        this.taskId = taskId;
        return this;
    }

    public HistoricVariableInstanceQueryImpl taskIds(Set<String> taskIds) {
        if (taskIds == null) {
            throw new FlowableIllegalArgumentException("taskIds is null");
        }
        if (taskIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("Set of taskIds is empty");
        }
        if (excludeTaskRelated) {
            throw new FlowableIllegalArgumentException("Cannot use taskIds together with excludeTaskVariables");
        }
        this.taskIds = taskIds;
        return this;
    }

    @Override
    public HistoricVariableInstanceQuery excludeTaskVariables() {
        if (taskId != null) {
            throw new FlowableIllegalArgumentException("Cannot use taskId together with excludeTaskVariables");
        }
        if (taskIds != null) {
            throw new FlowableIllegalArgumentException("Cannot use taskIds together with excludeTaskVariables");
        }
        excludeTaskRelated = true;
        return this;
    }

    public HistoricVariableInstanceQuery excludeVariableInitialization() {
        excludeVariableInitialization = true;
        return this;
    }

    public HistoricVariableInstanceQuery variableName(String variableName) {
        if (variableName == null) {
            throw new FlowableIllegalArgumentException("variableName is null");
        }
        this.variableName = variableName;
        return this;
    }

    public HistoricVariableInstanceQuery variableValueEquals(String variableName, Object variableValue) {
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

    public HistoricVariableInstanceQuery variableValueNotEquals(String variableName, Object variableValue) {
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

    public HistoricVariableInstanceQuery variableValueLike(String variableName, String variableValue) {
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

    public HistoricVariableInstanceQuery variableValueLikeIgnoreCase(String variableName, String variableValue) {
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

    public HistoricVariableInstanceQuery variableNameLike(String variableNameLike) {
        if (variableNameLike == null) {
            throw new FlowableIllegalArgumentException("variableNameLike is null");
        }
        this.variableNameLike = variableNameLike;
        return this;
    }

    protected void ensureVariablesInitialized() {
        if (this.queryVariableValue != null) {
            VariableTypes variableTypes = CommandContextUtil.getProcessEngineConfiguration().getVariableTypes();
            queryVariableValue.initialize(variableTypes);
        }
    }

    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        ensureVariablesInitialized();
        return CommandContextUtil.getHistoricVariableInstanceEntityManager(commandContext).findHistoricVariableInstanceCountByQueryCriteria(this);
    }

    public List<HistoricVariableInstance> executeList(CommandContext commandContext) {
        checkQueryOk();
        ensureVariablesInitialized();

        List<HistoricVariableInstance> historicVariableInstances = CommandContextUtil.getHistoricVariableInstanceEntityManager(commandContext).findHistoricVariableInstancesByQueryCriteria(this);

        if (!excludeVariableInitialization) {
            for (HistoricVariableInstance historicVariableInstance : historicVariableInstances) {
                if (historicVariableInstance instanceof HistoricVariableInstanceEntity) {
                    HistoricVariableInstanceEntity variableEntity = (HistoricVariableInstanceEntity) historicVariableInstance;
                    if (variableEntity.getVariableType() != null) {
                        variableEntity.getValue();

                        // make sure JPA entities are cached for later retrieval
                        if (JPAEntityVariableType.TYPE_NAME.equals(variableEntity.getVariableType().getTypeName()) || JPAEntityListVariableType.TYPE_NAME.equals(variableEntity.getVariableType().getTypeName())) {
                            ((CacheableVariable) variableEntity.getVariableType()).setForceCacheable(true);
                        }
                    }
                }
            }
        }
        return historicVariableInstances;
    }

    // order by
    // /////////////////////////////////////////////////////////////////

    public HistoricVariableInstanceQuery orderByProcessInstanceId() {
        orderBy(HistoricVariableInstanceQueryProperty.PROCESS_INSTANCE_ID);
        return this;
    }

    public HistoricVariableInstanceQuery orderByVariableName() {
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

    public QueryVariableValue getQueryVariableValue() {
        return queryVariableValue;
    }

}
