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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.db.SuspensionState;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.impl.context.BpmnOverrideContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ExecutionQuery;
import org.flowable.variable.service.impl.AbstractVariableQueryImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 * @author Frederik Heremans
 * @author Daniel Meyer
 */
public class ExecutionQueryImpl extends AbstractVariableQueryImpl<ExecutionQuery, Execution> implements ExecutionQuery {

    private static final long serialVersionUID = 1L;
    protected String processDefinitionId;
    protected String processDefinitionKey;
    protected String processDefinitionCategory;
    protected String processDefinitionName;
    protected Integer processDefinitionVersion;
    protected String processDefinitionEngineVersion;
    protected String activityId;
    protected String executionId;
    protected String parentId;
    protected boolean onlyChildExecutions;
    protected boolean onlySubProcessExecutions;
    protected boolean onlyProcessInstanceExecutions;
    protected String processInstanceId;
    protected String rootProcessInstanceId;
    protected List<EventSubscriptionQueryValue> eventSubscriptions;

    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;
    protected String locale;
    protected boolean withLocalizationFallback;

    protected Date startedBefore;
    protected Date startedAfter;
    protected String startedBy;

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

    // Not exposed in API, but here for the ProcessInstanceQuery support, since
    // the name lives on the
    // Execution entity/table
    protected String name;
    protected String nameLike;
    protected String nameLikeIgnoreCase;
    protected String deploymentId;
    protected List<String> deploymentIds;
    
    protected List<ExecutionQueryImpl> orQueryObjects = new ArrayList<>();
    protected ExecutionQueryImpl currentOrQueryObject;
    protected boolean inOrStatement;

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
            throw new FlowableIllegalArgumentException("Process definition id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionId = processDefinitionId;
        } else {
            this.processDefinitionId = processDefinitionId;
        }
        return this;
    }

    @Override
    public ExecutionQueryImpl processDefinitionKey(String processDefinitionKey) {
        if (processDefinitionKey == null) {
            throw new FlowableIllegalArgumentException("Process definition key is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionKey = processDefinitionKey;
        } else {
            this.processDefinitionKey = processDefinitionKey;
        }
        return this;
    }

    @Override
    public ExecutionQuery processDefinitionCategory(String processDefinitionCategory) {
        if (processDefinitionCategory == null) {
            throw new FlowableIllegalArgumentException("Process definition category is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionCategory = processDefinitionCategory;
        } else {
            this.processDefinitionCategory = processDefinitionCategory;
        }
        return this;
    }

    @Override
    public ExecutionQuery processDefinitionName(String processDefinitionName) {
        if (processDefinitionName == null) {
            throw new FlowableIllegalArgumentException("Process definition name is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionName = processDefinitionName;
        } else {
            this.processDefinitionName = processDefinitionName;
        }
        return this;
    }

    @Override
    public ExecutionQuery processDefinitionVersion(Integer processDefinitionVersion) {
        if (processDefinitionVersion == null) {
            throw new FlowableIllegalArgumentException("Process definition version is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionVersion = processDefinitionVersion;
        } else {
            this.processDefinitionVersion = processDefinitionVersion;
        }
        return this;
    }

    @Override
    public ExecutionQuery processDefinitionEngineVersion(String processDefinitionEngineVersion) {
        if (processDefinitionEngineVersion == null) {
            throw new FlowableIllegalArgumentException("Process definition engine version is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionEngineVersion = processDefinitionEngineVersion;
        } else {
            this.processDefinitionEngineVersion = processDefinitionEngineVersion;
        }
        return this;
    }

    @Override
    public ExecutionQueryImpl processInstanceId(String processInstanceId) {
        if (processInstanceId == null) {
            throw new FlowableIllegalArgumentException("Process instance id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.processInstanceId = processInstanceId;
        } else {
            this.processInstanceId = processInstanceId;
        }
        return this;
    }

    @Override
    public ExecutionQueryImpl rootProcessInstanceId(String rootProcessInstanceId) {
        if (rootProcessInstanceId == null) {
            throw new FlowableIllegalArgumentException("Root process instance id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.rootProcessInstanceId = rootProcessInstanceId;
        } else {
            this.rootProcessInstanceId = rootProcessInstanceId;
        }
        return this;
    }

    @Override
    public ExecutionQuery processInstanceBusinessKey(String businessKey) {
        if (businessKey == null) {
            throw new FlowableIllegalArgumentException("Business key is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.businessKey = businessKey;
        } else {
            this.businessKey = businessKey;
        }
        return this;
    }

    @Override
    public ExecutionQuery processInstanceBusinessKey(String processInstanceBusinessKey, boolean includeChildExecutions) {
        if (!includeChildExecutions) {
            return processInstanceBusinessKey(processInstanceBusinessKey);
        } else {
            if (processInstanceBusinessKey == null) {
                throw new FlowableIllegalArgumentException("Business key is null");
            }
            
            if (inOrStatement) {
                this.currentOrQueryObject.businessKey = processInstanceBusinessKey;
                this.currentOrQueryObject.includeChildExecutionsWithBusinessKeyQuery = includeChildExecutions;
            } else {
                this.businessKey = processInstanceBusinessKey;
                this.includeChildExecutionsWithBusinessKeyQuery = includeChildExecutions;
            }
            
            return this;
        }
    }

    @Override
    public ExecutionQuery processDefinitionKeys(Set<String> processDefinitionKeys) {
        if (processDefinitionKeys == null) {
            throw new FlowableIllegalArgumentException("Process definition keys is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionKeys = processDefinitionKeys;
        } else {
            this.processDefinitionKeys = processDefinitionKeys;
        }
        return this;
    }

    @Override
    public ExecutionQueryImpl executionId(String executionId) {
        if (executionId == null) {
            throw new FlowableIllegalArgumentException("Execution id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.executionId = executionId;
        } else {
            this.executionId = executionId;
        }
        return this;
    }

    @Override
    public ExecutionQueryImpl activityId(String activityId) {
        if (inOrStatement) {
            this.currentOrQueryObject.activityId = activityId;
            if (activityId != null) {
                this.currentOrQueryObject.isActive = true;
            }
            
        } else {
            this.activityId = activityId;

            if (activityId != null) {
                this.isActive = true;
            }
        }
        
        return this;
    }

    @Override
    public ExecutionQueryImpl parentId(String parentId) {
        if (parentId == null) {
            throw new FlowableIllegalArgumentException("Parent id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.parentId = parentId;
        } else {
            this.parentId = parentId;
        }
        return this;
    }

    @Override
    public ExecutionQuery onlyChildExecutions() {
        if (inOrStatement) {
            this.currentOrQueryObject.onlyChildExecutions = true;
        } else {
            this.onlyChildExecutions = true;
        }
        return this;
    }

    @Override
    public ExecutionQuery onlySubProcessExecutions() {
        if (inOrStatement) {
            this.currentOrQueryObject.onlySubProcessExecutions = true;
        } else {
            this.onlySubProcessExecutions = true;
        }
        return this;
    }

    @Override
    public ExecutionQuery onlyProcessInstanceExecutions() {
        if (inOrStatement) {
            this.currentOrQueryObject.onlyProcessInstanceExecutions = true;
        } else {
            this.onlyProcessInstanceExecutions = true;
        }
        return this;
    }

    @Override
    public ExecutionQueryImpl executionTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("execution tenant id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.tenantId = tenantId;
        } else {
            this.tenantId = tenantId;
        }
        return this;
    }

    @Override
    public ExecutionQueryImpl executionTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("execution tenant id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.tenantIdLike = tenantIdLike;
        } else {
            this.tenantIdLike = tenantIdLike;
        }
        return this;
    }

    @Override
    public ExecutionQueryImpl executionWithoutTenantId() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutTenantId = true;
        } else {
            this.withoutTenantId = true;
        }
        
        return this;
    }

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
            throw new FlowableIllegalArgumentException("event name is null");
        }
        if (eventType == null) {
            throw new FlowableIllegalArgumentException("event type is null");
        }
        
        if (inOrStatement) {
            if (this.currentOrQueryObject.eventSubscriptions == null) {
                this.currentOrQueryObject.eventSubscriptions = new ArrayList<>();
            }
            this.currentOrQueryObject.eventSubscriptions.add(new EventSubscriptionQueryValue(eventName, eventType));
           
        } else {
            if (eventSubscriptions == null) {
                eventSubscriptions = new ArrayList<>();
            }
            eventSubscriptions.add(new EventSubscriptionQueryValue(eventName, eventType));
        }
        
        return this;
    }

    @Override
    public ExecutionQuery processVariableValueEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEquals(variableName, variableValue, false);
            return this;
        } else {
            return variableValueEquals(variableName, variableValue, false);
        }
    }

    @Override
    public ExecutionQuery processVariableValueEquals(Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEquals(variableValue, false);
            return this;
        } else {
            return variableValueEquals(variableValue, false);
        }
    }

    @Override
    public ExecutionQuery processVariableValueNotEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEquals(variableName, variableValue, false);
            return this;
        } else {
            return variableValueNotEquals(variableName, variableValue, false);
        }
    }

    @Override
    public ExecutionQuery processVariableValueEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEqualsIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueEqualsIgnoreCase(name, value, false);
        }
    }

    @Override
    public ExecutionQuery processVariableValueNotEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEqualsIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueNotEqualsIgnoreCase(name, value, false);
        } 
    }

    @Override
    public ExecutionQuery processVariableValueLike(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLike(name, value, false);
            return this;
        } else {
            return variableValueLike(name, value, false);
        }
    }

    @Override
    public ExecutionQuery processVariableValueLikeIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLikeIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueLikeIgnoreCase(name, value, false);
        }
    }
    
    @Override
    public ExecutionQuery processVariableValueGreaterThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueGreaterThan(name, value, false);
        } else {
            this.variableValueGreaterThan(name, value, false);
        }
        return this;
    }

    @Override
    public ExecutionQuery processVariableValueGreaterThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueGreaterThanOrEqual(name, value, false);
        } else {
            this.variableValueGreaterThanOrEqual(name, value, false);
        }
        return this;
    }

    @Override
    public ExecutionQuery processVariableValueLessThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLessThan(name, value, false);
        } else {
            this.variableValueLessThan(name, value, false);
        }
        return this;
    }

    @Override
    public ExecutionQuery processVariableValueLessThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLessThanOrEqual(name, value, false);
        } else {
            this.variableValueLessThanOrEqual(name, value, false);
        }
        return this;
    }

    @Override
    public ExecutionQuery processVariableExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.variableExists(name, false);
            return this;
        } else {
            return variableExists(name, false);
        }
    }

    @Override
    public ExecutionQuery processVariableNotExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.variableNotExists(name, false);
            return this;
        } else {
            return variableNotExists(name, false);
        }
    }
    
    @Override
    public ExecutionQuery variableValueEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEquals(variableName, variableValue, true);
            return this;
        } else {
            return variableValueEquals(variableName, variableValue, true);
        }
    }

    @Override
    public ExecutionQuery variableValueEquals(Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEquals(variableValue, true);
            return this;
        } else {
            return variableValueEquals(variableValue, true);
        }
    }

    @Override
    public ExecutionQuery variableValueNotEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEquals(variableName, variableValue, true);
            return this;
        } else {
            return variableValueNotEquals(variableName, variableValue, true);
        }
    }

    @Override
    public ExecutionQuery variableValueEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEqualsIgnoreCase(name, value, true);
            return this;
        } else {
            return variableValueEqualsIgnoreCase(name, value, true);
        }
    }

    @Override
    public ExecutionQuery variableValueNotEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEqualsIgnoreCase(name, value, true);
            return this;
        } else {
            return variableValueNotEqualsIgnoreCase(name, value, true);
        } 
    }

    @Override
    public ExecutionQuery variableValueLike(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLike(name, value, true);
            return this;
        } else {
            return variableValueLike(name, value, true);
        }
    }

    @Override
    public ExecutionQuery variableValueLikeIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLikeIgnoreCase(name, value, true);
            return this;
        } else {
            return variableValueLikeIgnoreCase(name, value, true);
        }
    }
    
    @Override
    public ExecutionQuery variableValueGreaterThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueGreaterThan(name, value, true);
            return this;
        } else {
            return variableValueGreaterThan(name, value, true);
        }
    }

    @Override
    public ExecutionQuery variableValueGreaterThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueGreaterThanOrEqual(name, value, true);
            return this;
        } else {
            return variableValueGreaterThanOrEqual(name, value, true);
        }
    }

    @Override
    public ExecutionQuery variableValueLessThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLessThan(name, value, true);
            return this;
        } else {
            return variableValueLessThan(name, value, true);
        }
    }

    @Override
    public ExecutionQuery variableValueLessThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLessThanOrEqual(name, value, true);
            return this;
        } else {
            return variableValueLessThanOrEqual(name, value, true);
        }
    }

    @Override
    public ExecutionQuery variableExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.variableExists(name, true);
            return this;
        } else {
            return variableExists(name, true);
        }
    }

    @Override
    public ExecutionQuery variableNotExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.variableNotExists(name, true);
            return this;
        } else {
            return variableNotExists(name, true);
        }
    }

    @Override
    public ExecutionQuery locale(String locale) {
        if (inOrStatement) {
            currentOrQueryObject.locale = locale;
        } else {
            this.locale = locale;
        }
        
        return this;
    }

    @Override
    public ExecutionQuery withLocalizationFallback() {
        if (inOrStatement) {
            currentOrQueryObject.withLocalizationFallback = true;
        } else {
            this.withLocalizationFallback = true;
        }
        
        return this;
    }

    @Override
    public ExecutionQuery startedBefore(Date beforeTime) {
        if (beforeTime == null) {
            throw new FlowableIllegalArgumentException("before time is null");
        }
        
        if (inOrStatement) {
            currentOrQueryObject.startedBefore = beforeTime;
        } else {
            this.startedBefore = beforeTime;
        }
        
        return this;
    }

    @Override
    public ExecutionQuery startedAfter(Date afterTime) {
        if (afterTime == null) {
            throw new FlowableIllegalArgumentException("after time is null");
        }
        
        if (inOrStatement) {
            currentOrQueryObject.startedAfter = afterTime;
        } else {
            this.startedAfter = afterTime;
        }

        return this;
    }

    @Override
    public ExecutionQuery startedBy(String userId) {
        if (userId == null) {
            throw new FlowableIllegalArgumentException("user id is null");
        }
        
        if (inOrStatement) {
            currentOrQueryObject.startedBy = userId;
        } else {
            this.startedBy = userId;
        }

        return this;
    }
    
    @Override
    public ExecutionQuery or() {
        if (inOrStatement) {
            throw new FlowableException("the query is already in an or statement");
        }

        inOrStatement = true;
        currentOrQueryObject = new ExecutionQueryImpl();
        orQueryObjects.add(currentOrQueryObject);
        return this;
    }

    @Override
    public ExecutionQuery endOr() {
        if (!inOrStatement) {
            throw new FlowableException("endOr() can only be called after calling or()");
        }

        inOrStatement = false;
        currentOrQueryObject = null;
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
        return CommandContextUtil.getExecutionEntityManager(commandContext).findExecutionCountByQueryCriteria(this);
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public List<Execution> executeList(CommandContext commandContext) {
        checkQueryOk();
        ensureVariablesInitialized();
        List<?> executions = CommandContextUtil.getExecutionEntityManager(commandContext).findExecutionsByQueryCriteria(this);

        if (CommandContextUtil.getProcessEngineConfiguration().getPerformanceSettings().isEnableLocalization()) {
            for (ExecutionEntity execution : (List<ExecutionEntity>) executions) {
                String activityId = null;
                if (execution.getId().equals(execution.getProcessInstanceId())) {
                    if (execution.getProcessDefinitionId() != null) {
                        ProcessDefinition processDefinition = CommandContextUtil.getProcessEngineConfiguration(commandContext)
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
        }

        return (List<Execution>) executions;
    }

    protected void localize(Execution execution, String activityId) {
        ExecutionEntity executionEntity = (ExecutionEntity) execution;
        executionEntity.setLocalizedName(null);
        executionEntity.setLocalizedDescription(null);

        String processDefinitionId = executionEntity.getProcessDefinitionId();
        if (locale != null && processDefinitionId != null) {
            ObjectNode languageNode = BpmnOverrideContext.getLocalizationElementProperties(locale, activityId, processDefinitionId, withLocalizationFallback);
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
    
    @Override
    protected void ensureVariablesInitialized() {
        super.ensureVariablesInitialized();

        for (ExecutionQueryImpl orQueryObject : orQueryObjects) {
            orQueryObject.ensureVariablesInitialized();
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

    public String getProcessDefinitionEngineVersion() {
        return processDefinitionEngineVersion;
    }

    public String getActivityId() {
        return activityId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getRootProcessInstanceId() {
        return rootProcessInstanceId;
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

    public boolean isOnlyChildExecutions() {
        return onlyChildExecutions;
    }

    public boolean isOnlySubProcessExecutions() {
        return onlySubProcessExecutions;
    }

    public boolean isOnlyProcessInstanceExecutions() {
        return onlyProcessInstanceExecutions;
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

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public List<String> getDeploymentIds() {
        return deploymentIds;
    }

    public void setDeploymentIds(List<String> deploymentIds) {
        this.deploymentIds = deploymentIds;
    }

    public Date getStartedBefore() {
        return startedBefore;
    }

    public void setStartedBefore(Date startedBefore) {
        this.startedBefore = startedBefore;
    }

    public Date getStartedAfter() {
        return startedAfter;
    }

    public void setStartedAfter(Date startedAfter) {
        this.startedAfter = startedAfter;
    }

    public String getStartedBy() {
        return startedBy;
    }

    public void setStartedBy(String startedBy) {
        this.startedBy = startedBy;
    }
}
