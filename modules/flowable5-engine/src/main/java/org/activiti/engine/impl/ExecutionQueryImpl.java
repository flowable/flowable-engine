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
package org.activiti.engine.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.SuspensionState;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ExecutionQuery;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.repository.ProcessDefinition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 * @author Frederik Heremans
 * @author Daniel Meyer
 */
public class ExecutionQueryImpl extends AbstractVariableQueryImpl<ExecutionQuery, Execution>
        implements ExecutionQuery {

    private static final long serialVersionUID = 1L;
    protected String processDefinitionId;
    protected String processDefinitionKey;
    protected String processDefinitionCategory;
    protected String processDefinitionName;
    protected Integer processDefinitionVersion;
    protected String activityId;
    protected String executionId;
    protected String parentId;
    protected String processInstanceId;
    protected List<EventSubscriptionQueryValue> eventSubscriptions;

    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;
    protected String locale;
    protected boolean withLocalizationFallback;

    // Not used by end-users, but needed for dynamic ibatis query
    protected String superProcessInstanceId;
    protected String subProcessInstanceId;
    protected boolean excludeSubprocesses;
    protected SuspensionState suspensionState;
    protected String businessKey;
    protected boolean includeChildExecutionsWithBusinessKeyQuery;
    protected boolean isActive;
    protected String involvedUser;
    protected Set<String> processDefinitionKeys;
    protected Set<String> processDefinitionIds;

    // Not exposed in API, but here for the ProcessInstanceQuery support, since the name lives on the
    // Execution entity/table
    protected String name;
    protected String nameLike;
    protected String nameLikeIgnoreCase;
    protected String deploymentId;
    protected List<String> deploymentIds;
    protected List<ExecutionQueryImpl> orQueryObjects = new ArrayList<>();

    public ExecutionQueryImpl() {
    }

    public ExecutionQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public ExecutionQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    public boolean isProcessInstancesOnly() {
        return false; // see dynamic query
    }

    @Override
    public ExecutionQueryImpl processDefinitionId(String processDefinitionId) {
        if (processDefinitionId == null) {
            throw new ActivitiIllegalArgumentException("Process definition id is null");
        }
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    @Override
    public ExecutionQueryImpl processDefinitionKey(String processDefinitionKey) {
        if (processDefinitionKey == null) {
            throw new ActivitiIllegalArgumentException("Process definition key is null");
        }
        this.processDefinitionKey = processDefinitionKey;
        return this;
    }

    @Override
    public ExecutionQuery processDefinitionCategory(String processDefinitionCategory) {
        if (processDefinitionCategory == null) {
            throw new ActivitiIllegalArgumentException("Process definition category is null");
        }
        this.processDefinitionCategory = processDefinitionCategory;
        return this;
    }

    @Override
    public ExecutionQuery processDefinitionName(String processDefinitionName) {
        if (processDefinitionName == null) {
            throw new ActivitiIllegalArgumentException("Process definition name is null");
        }
        this.processDefinitionName = processDefinitionName;
        return this;
    }

    @Override
    public ExecutionQuery processDefinitionVersion(Integer processDefinitionVersion) {
        if (processDefinitionVersion == null) {
            throw new ActivitiIllegalArgumentException("Process definition version is null");
        }
        this.processDefinitionVersion = processDefinitionVersion;
        return this;
    }

    @Override
    public ExecutionQueryImpl processInstanceId(String processInstanceId) {
        if (processInstanceId == null) {
            throw new ActivitiIllegalArgumentException("Process instance id is null");
        }
        this.processInstanceId = processInstanceId;
        return this;
    }

    @Override
    public ExecutionQuery processInstanceBusinessKey(String businessKey) {
        if (businessKey == null) {
            throw new ActivitiIllegalArgumentException("Business key is null");
        }
        this.businessKey = businessKey;
        return this;
    }

    @Override
    public ExecutionQuery processInstanceBusinessKey(String processInstanceBusinessKey, boolean includeChildExecutions) {
        if (!includeChildExecutions) {
            return processInstanceBusinessKey(processInstanceBusinessKey);
        } else {
            if (processInstanceBusinessKey == null) {
                throw new ActivitiIllegalArgumentException("Business key is null");
            }
            this.businessKey = processInstanceBusinessKey;
            this.includeChildExecutionsWithBusinessKeyQuery = includeChildExecutions;
            return this;
        }
    }

    @Override
    public ExecutionQuery processDefinitionKeys(Set<String> processDefinitionKeys) {
        if (processDefinitionKeys == null) {
            throw new ActivitiIllegalArgumentException("Process definition keys is null");
        }
        this.processDefinitionKeys = processDefinitionKeys;
        return this;
    }

    @Override
    public ExecutionQueryImpl executionId(String executionId) {
        if (executionId == null) {
            throw new ActivitiIllegalArgumentException("Execution id is null");
        }
        this.executionId = executionId;
        return this;
    }

    @Override
    public ExecutionQueryImpl activityId(String activityId) {
        this.activityId = activityId;

        if (activityId != null) {
            isActive = true;
        }
        return this;
    }

    @Override
    public ExecutionQueryImpl parentId(String parentId) {
        if (parentId == null) {
            throw new ActivitiIllegalArgumentException("Parent id is null");
        }
        this.parentId = parentId;
        return this;
    }

    @Override
    public ExecutionQueryImpl executionTenantId(String tenantId) {
        if (tenantId == null) {
            throw new ActivitiIllegalArgumentException("execution tenant id is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public ExecutionQueryImpl executionTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new ActivitiIllegalArgumentException("execution tenant id is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    @Override
    public ExecutionQueryImpl executionWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    @Override
    public ExecutionQuery signalEventSubscription(String signalName) {
        return eventSubscription("signal", signalName);
    }

    @Override
    public ExecutionQuery signalEventSubscriptionName(String signalName) {
        return eventSubscription("signal", signalName);
    }

    @Override
    public ExecutionQuery messageEventSubscriptionName(String messageName) {
        return eventSubscription("message", messageName);
    }

    public ExecutionQuery eventSubscription(String eventType, String eventName) {
        if (eventName == null) {
            throw new ActivitiIllegalArgumentException("event name is null");
        }
        if (eventType == null) {
            throw new ActivitiIllegalArgumentException("event type is null");
        }
        if (eventSubscriptions == null) {
            eventSubscriptions = new ArrayList<>();
        }
        eventSubscriptions.add(new EventSubscriptionQueryValue(eventName, eventType));
        return this;
    }

    @Override
    public ExecutionQuery processVariableValueEquals(String variableName, Object variableValue) {
        return variableValueEquals(variableName, variableValue, false);
    }

    @Override
    public ExecutionQuery processVariableValueEquals(Object variableValue) {
        return variableValueEquals(variableValue, false);
    }

    @Override
    public ExecutionQuery processVariableValueNotEquals(String variableName, Object variableValue) {
        return variableValueNotEquals(variableName, variableValue, false);
    }

    @Override
    public ExecutionQuery processVariableValueEqualsIgnoreCase(String name, String value) {
        return variableValueEqualsIgnoreCase(name, value, false);
    }

    @Override
    public ExecutionQuery processVariableValueNotEqualsIgnoreCase(String name, String value) {
        return variableValueNotEqualsIgnoreCase(name, value, false);
    }

    @Override
    public ExecutionQuery processVariableValueLike(String name, String value) {
        return variableValueLike(name, value, false);
    }

    @Override
    public ExecutionQuery processVariableValueLikeIgnoreCase(String name, String value) {
        return variableValueLikeIgnoreCase(name, value, false);
    }

    @Override
    public ExecutionQuery locale(String locale) {
        this.locale = locale;
        return this;
    }

    @Override
    public ExecutionQuery withLocalizationFallback() {
        withLocalizationFallback = true;
        return this;
    }

    // ordering ////////////////////////////////////////////////////

    @Override
    public ExecutionQueryImpl orderByProcessInstanceId() {
        this.orderProperty = ExecutionQueryProperty.PROCESS_INSTANCE_ID;
        return this;
    }

    @Override
    public ExecutionQueryImpl orderByProcessDefinitionId() {
        this.orderProperty = ExecutionQueryProperty.PROCESS_DEFINITION_ID;
        return this;
    }

    @Override
    public ExecutionQueryImpl orderByProcessDefinitionKey() {
        this.orderProperty = ExecutionQueryProperty.PROCESS_DEFINITION_KEY;
        return this;
    }

    @Override
    public ExecutionQueryImpl orderByTenantId() {
        this.orderProperty = ExecutionQueryProperty.TENANT_ID;
        return this;
    }

    // results ////////////////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        ensureVariablesInitialized();
        return commandContext
                .getExecutionEntityManager()
                .findExecutionCountByQueryCriteria(this);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public List<Execution> executeList(CommandContext commandContext, Page page) {
        checkQueryOk();
        ensureVariablesInitialized();
        List<?> executions = commandContext.getExecutionEntityManager().findExecutionsByQueryCriteria(this, page);

        for (ExecutionEntity execution : (List<ExecutionEntity>) executions) {
            String activityId = null;
            if (execution.getId().equals(execution.getProcessInstanceId())) {
                if (execution.getProcessDefinitionId() != null) {
                    ProcessDefinition processDefinition = commandContext.getProcessEngineConfiguration()
                            .getDeploymentManager()
                            .findDeployedProcessDefinitionById(execution.getProcessDefinitionId());
                    activityId = processDefinition.getKey();
                }

            } else {
                activityId = execution.getActivityId();
            }

            if (activityId != null) {
                localize(execution, activityId);
            }
        }

        return (List<Execution>) executions;
    }

    protected void localize(Execution execution, String activityId) {
        ExecutionEntity executionEntity = (ExecutionEntity) execution;
        executionEntity.setLocalizedName(null);
        executionEntity.setLocalizedDescription(null);

        String processDefinitionId = executionEntity.getProcessDefinitionId();
        if (locale != null && processDefinitionId != null) {
            ObjectNode languageNode = Context.getLocalizationElementProperties(locale, activityId, processDefinitionId, withLocalizationFallback);
            if (languageNode != null) {
                JsonNode languageNameNode = languageNode.get(DynamicBpmnConstants.LOCALIZATION_NAME);
                if (languageNameNode != null && !languageNameNode.isNull()) {
                    executionEntity.setLocalizedName(languageNameNode.asText());
                }

                JsonNode languageDescriptionNode = languageNode.get(DynamicBpmnConstants.LOCALIZATION_DESCRIPTION);
                if (languageDescriptionNode != null && !languageDescriptionNode.isNull()) {
                    executionEntity.setLocalizedDescription(languageDescriptionNode.asText());
                }
            }
        }
    }

    // getters ////////////////////////////////////////////////////

    public boolean getOnlyProcessInstances() {
        return false;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public String getProcessDefinitionCategory() {
        return processDefinitionCategory;
    }

    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public String getActivityId() {
        return activityId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getProcessInstanceIds() {
        return null;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getSuperProcessInstanceId() {
        return superProcessInstanceId;
    }

    public String getSubProcessInstanceId() {
        return subProcessInstanceId;
    }

    public boolean isExcludeSubprocesses() {
        return excludeSubprocesses;
    }

    public SuspensionState getSuspensionState() {
        return suspensionState;
    }

    public void setSuspensionState(SuspensionState suspensionState) {
        this.suspensionState = suspensionState;
    }

    public List<EventSubscriptionQueryValue> getEventSubscriptions() {
        return eventSubscriptions;
    }

    public boolean isIncludeChildExecutionsWithBusinessKeyQuery() {
        return includeChildExecutionsWithBusinessKeyQuery;
    }

    public void setEventSubscriptions(List<EventSubscriptionQueryValue> eventSubscriptions) {
        this.eventSubscriptions = eventSubscriptions;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getInvolvedUser() {
        return involvedUser;
    }

    public void setInvolvedUser(String involvedUser) {
        this.involvedUser = involvedUser;
    }

    public Set<String> getProcessDefinitionIds() {
        return processDefinitionIds;
    }

    public Set<String> getProcessDefinitionKeys() {
        return processDefinitionKeys;
    }

    public String getParentId() {
        return parentId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getTenantIdLike() {
        return tenantIdLike;
    }

    public boolean isWithoutTenantId() {
        return withoutTenantId;
    }

    public String getName() {
        return name;
    }

    public String getNameLike() {
        return nameLike;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNameLike(String nameLike) {
        this.nameLike = nameLike;
    }

    public String getNameLikeIgnoreCase() {
        return nameLikeIgnoreCase;
    }

    public void setNameLikeIgnoreCase(String nameLikeIgnoreCase) {
        this.nameLikeIgnoreCase = nameLikeIgnoreCase;
    }

}
