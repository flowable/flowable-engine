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
package org.flowable.engine.impl.persistence.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.bpmn.model.FlowableListener;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.context.Context;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.persistence.CountingTaskEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.task.DelegationState;
import org.flowable.engine.task.IdentityLink;
import org.flowable.engine.task.IdentityLinkType;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Falko Menge
 * @author Tijs Rademakers
 */
public class TaskEntityImpl extends VariableScopeImpl implements TaskEntity, CountingTaskEntity, Serializable {

    public static final String DELETE_REASON_COMPLETED = "completed";
    public static final String DELETE_REASON_DELETED = "deleted";

    private static final long serialVersionUID = 1L;

    protected String owner;
    protected int assigneeUpdatedCount; // needed for v5 compatibility
    protected String originalAssignee; // needed for v5 compatibility
    protected String assignee;
    protected DelegationState delegationState;

    protected String parentTaskId;

    protected String name;
    protected String localizedName;
    protected String description;
    protected String localizedDescription;
    protected int priority = DEFAULT_PRIORITY;
    protected Date createTime; // The time when the task has been created
    protected Date dueDate;
    protected int suspensionState = SuspensionState.ACTIVE.getStateCode();
    protected String category;

    protected boolean isIdentityLinksInitialized;
    protected List<IdentityLinkEntity> taskIdentityLinkEntities = new ArrayList<>();

    protected String executionId;
    protected ExecutionEntity execution;

    protected String processInstanceId;
    protected ExecutionEntity processInstance;

    protected String processDefinitionId;

    protected String taskDefinitionKey;
    protected String formKey;

    protected boolean isDeleted;
    protected boolean isCanceled;

    private boolean isCountEnabled;
    private int variableCount;
    private int identityLinkCount;

    protected String eventName;
    protected FlowableListener currentListener;

    protected String tenantId = ProcessEngineConfiguration.NO_TENANT_ID;

    protected List<VariableInstanceEntity> queryVariables;
    protected List<IdentityLinkEntity> queryIdentityLinks;

    protected boolean forcedUpdate;

    protected Date claimTime;

    public TaskEntityImpl() {

    }

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("assignee", this.assignee);
        persistentState.put("owner", this.owner);
        persistentState.put("name", this.name);
        persistentState.put("priority", this.priority);
        persistentState.put("category", this.category);
        persistentState.put("formKey", this.formKey);
        if (executionId != null) {
            persistentState.put("executionId", this.executionId);
        }
        if (processDefinitionId != null) {
            persistentState.put("processDefinitionId", this.processDefinitionId);
        }
        if (createTime != null) {
            persistentState.put("createTime", this.createTime);
        }
        if (description != null) {
            persistentState.put("description", this.description);
        }
        if (dueDate != null) {
            persistentState.put("dueDate", this.dueDate);
        }
        if (parentTaskId != null) {
            persistentState.put("parentTaskId", this.parentTaskId);
        }
        if (delegationState != null) {
            persistentState.put("delegationState", this.delegationState);
            persistentState.put("delegationStateString", getDelegationStateString());
        }

        persistentState.put("suspensionState", this.suspensionState);

        if (forcedUpdate) {
            persistentState.put("forcedUpdate", Boolean.TRUE);
        }

        if (claimTime != null) {
            persistentState.put("claimTime", this.claimTime);
        }

        persistentState.put("isCountEnabled", this.isCountEnabled);
        persistentState.put("variableCount", this.variableCount);
        persistentState.put("identityLinkCount", this.identityLinkCount);

        return persistentState;
    }

    @Override
    public void forceUpdate() {
        this.forcedUpdate = true;
    }

    // variables //////////////////////////////////////////////////////////////////

    @Override
    protected VariableScopeImpl getParentVariableScope() {
        if (getExecution() != null) {
            return (ExecutionEntityImpl) execution;
        }
        return null;
    }

    @Override
    protected void initializeVariableInstanceBackPointer(VariableInstanceEntity variableInstance) {
        variableInstance.setTaskId(id);
        variableInstance.setExecutionId(executionId);
        variableInstance.setProcessInstanceId(processInstanceId);
    }

    @Override
    protected List<VariableInstanceEntity> loadVariableInstances() {
        return CommandContextUtil.getVariableInstanceEntityManager().findVariableInstancesByTaskId(id);
    }

    @Override
    protected VariableInstanceEntity createVariableInstance(String variableName, Object value, ExecutionEntity sourceActivityExecution) {
        VariableInstanceEntity result = super.createVariableInstance(variableName, value, sourceActivityExecution);

        // Dispatch event, if needed
        if (CommandContextUtil.getProcessEngineConfiguration() != null && CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
            CommandContextUtil.getProcessEngineConfiguration()
                    .getEventDispatcher()
                    .dispatchEvent(
                            FlowableEventBuilder.createVariableEvent(FlowableEngineEventType.VARIABLE_CREATED, variableName, value, result.getType(), result.getTaskId(), result.getExecutionId(), getProcessInstanceId(),
                                    getProcessDefinitionId()));
        }
        return result;
    }

    @Override
    protected void updateVariableInstance(VariableInstanceEntity variableInstance, Object value, ExecutionEntity sourceActivityExecution) {
        super.updateVariableInstance(variableInstance, value, sourceActivityExecution);

        // Dispatch event, if needed
        if (CommandContextUtil.getProcessEngineConfiguration() != null && CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
            CommandContextUtil.getProcessEngineConfiguration()
                    .getEventDispatcher()
                    .dispatchEvent(
                            FlowableEventBuilder.createVariableEvent(FlowableEngineEventType.VARIABLE_UPDATED, variableInstance.getName(), value, variableInstance.getType(), variableInstance.getTaskId(),
                                    variableInstance.getExecutionId(), getProcessInstanceId(), getProcessDefinitionId()));
        }
    }

    // execution //////////////////////////////////////////////////////////////////

    @Override
    public ExecutionEntity getExecution() {
        if ((execution == null) && (executionId != null)) {
            this.execution = CommandContextUtil.getExecutionEntityManager().findById(executionId);
        }
        return execution;
    }

    // task assignment ////////////////////////////////////////////////////////////

    @Override
    public void addCandidateUser(String userId) {
        CommandContextUtil.getIdentityLinkEntityManager().addCandidateUser(this, userId);
    }

    @Override
    public void addCandidateUsers(Collection<String> candidateUsers) {
        CommandContextUtil.getIdentityLinkEntityManager().addCandidateUsers(this, candidateUsers);
    }

    @Override
    public void addCandidateGroup(String groupId) {
        CommandContextUtil.getIdentityLinkEntityManager().addCandidateGroup(this, groupId);
    }

    @Override
    public void addCandidateGroups(Collection<String> candidateGroups) {
        CommandContextUtil.getIdentityLinkEntityManager().addCandidateGroups(this, candidateGroups);
    }

    @Override
    public void addUserIdentityLink(String userId, String identityLinkType) {
        CommandContextUtil.getIdentityLinkEntityManager().addUserIdentityLink(this, userId, identityLinkType);
    }

    @Override
    public void addGroupIdentityLink(String groupId, String identityLinkType) {
        CommandContextUtil.getIdentityLinkEntityManager().addGroupIdentityLink(this, groupId, identityLinkType);
    }

    @Override
    public Set<IdentityLink> getCandidates() {
        Set<IdentityLink> potentialOwners = new HashSet<>();
        for (IdentityLinkEntity identityLinkEntity : getIdentityLinks()) {
            if (IdentityLinkType.CANDIDATE.equals(identityLinkEntity.getType())) {
                potentialOwners.add(identityLinkEntity);
            }
        }
        return potentialOwners;
    }

    @Override
    public void deleteCandidateGroup(String groupId) {
        deleteGroupIdentityLink(groupId, IdentityLinkType.CANDIDATE);
    }

    @Override
    public void deleteCandidateUser(String userId) {
        deleteUserIdentityLink(userId, IdentityLinkType.CANDIDATE);
    }

    @Override
    public void deleteGroupIdentityLink(String groupId, String identityLinkType) {
        if (groupId != null) {
            CommandContextUtil.getIdentityLinkEntityManager().deleteIdentityLink(this, null, groupId, identityLinkType);
        }
    }

    @Override
    public void deleteUserIdentityLink(String userId, String identityLinkType) {
        if (userId != null) {
            CommandContextUtil.getIdentityLinkEntityManager().deleteIdentityLink(this, userId, null, identityLinkType);
        }
    }

    @Override
    public List<IdentityLinkEntity> getIdentityLinks() {
        if (!isIdentityLinksInitialized) {
            if (queryIdentityLinks == null) {
                taskIdentityLinkEntities = CommandContextUtil.getIdentityLinkEntityManager().findIdentityLinksByTaskId(id);
            } else {
                taskIdentityLinkEntities = queryIdentityLinks;
            }
            isIdentityLinksInitialized = true;
        }

        return taskIdentityLinkEntities;
    }

    @Override
    public void setExecutionVariables(Map<String, Object> parameters) {
        if (getExecution() != null) {
            execution.setVariables(parameters);
        }
    }

    @Override
    public void setName(String taskName) {
        this.name = taskName;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public void setAssignee(String assignee) {
        this.originalAssignee = this.assignee;
        this.assignee = assignee;
        assigneeUpdatedCount++;
    }

    @Override
    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public void setParentTaskId(String parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    @Override
    public String getFormKey() {
        return formKey;
    }

    @Override
    public void setFormKey(String formKey) {
        this.formKey = formKey;
    }

    // Override from VariableScopeImpl

    @Override
    protected boolean isActivityIdUsedForDetails() {
        return false;
    }

    // Overridden to avoid fetching *all* variables (as is the case in the super // call)
    @Override
    protected VariableInstanceEntity getSpecificVariable(String variableName) {
        CommandContext commandContext = Context.getCommandContext();
        if (commandContext == null) {
            throw new FlowableException("lazy loading outside command context");
        }
        VariableInstanceEntity variableInstance = CommandContextUtil.getVariableInstanceEntityManager(commandContext).findVariableInstanceByTaskAndName(id, variableName);

        return variableInstance;
    }

    @Override
    protected List<VariableInstanceEntity> getSpecificVariables(Collection<String> variableNames) {
        CommandContext commandContext = Context.getCommandContext();
        if (commandContext == null) {
            throw new FlowableException("lazy loading outside command context");
        }
        return CommandContextUtil.getVariableInstanceEntityManager(commandContext).findVariableInstancesByTaskAndNames(id, variableNames);
    }

    // regular getters and setters ////////////////////////////////////////////////////////

    @Override
    public String getName() {
        if (localizedName != null && localizedName.length() > 0) {
            return localizedName;
        } else {
            return name;
        }
    }

    public String getLocalizedName() {
        return localizedName;
    }

    @Override
    public void setLocalizedName(String localizedName) {
        this.localizedName = localizedName;
    }

    @Override
    public String getDescription() {
        if (localizedDescription != null && localizedDescription.length() > 0) {
            return localizedDescription;
        } else {
            return description;
        }
    }

    public String getLocalizedDescription() {
        return localizedDescription;
    }

    @Override
    public void setLocalizedDescription(String localizedDescription) {
        this.localizedDescription = localizedDescription;
    }

    @Override
    public Date getDueDate() {
        return dueDate;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public Date getCreateTime() {
        return createTime;
    }

    @Override
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String getExecutionId() {
        return executionId;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    @Override
    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    public String getAssignee() {
        return assignee;
    }

    public String getOriginalAssignee() {
        // Don't ask. A stupid hack for v5 compatibility
        if (assigneeUpdatedCount > 1) {
            return originalAssignee;
        } else {
            return assignee;
        }
    }

    @Override
    public String getTaskDefinitionKey() {
        return taskDefinitionKey;
    }

    @Override
    public void setTaskDefinitionKey(String taskDefinitionKey) {
        this.taskDefinitionKey = taskDefinitionKey;
    }

    @Override
    public String getEventName() {
        return eventName;
    }

    @Override
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    @Override
    public FlowableListener getCurrentFlowableListener() {
        return currentListener;
    }

    @Override
    public void setCurrentFlowableListener(FlowableListener currentListener) {
        this.currentListener = currentListener;
    }

    @Override
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    @Override
    public ExecutionEntity getProcessInstance() {
        if (processInstance == null && processInstanceId != null) {
            processInstance = CommandContextUtil.getExecutionEntityManager().findById(processInstanceId);
        }
        return processInstance;
    }

    public void setProcessInstance(ExecutionEntity processInstance) {
        this.processInstance = processInstance;
    }

    @Override
    public void setExecution(ExecutionEntity execution) {
        this.execution = execution;
    }

    @Override
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public DelegationState getDelegationState() {
        return delegationState;
    }

    @Override
    public void setDelegationState(DelegationState delegationState) {
        this.delegationState = delegationState;
    }

    public String getDelegationStateString() { // Needed for Activiti 5 compatibility, not exposed in interface
        return (delegationState != null ? delegationState.toString() : null);
    }

    public void setDelegationStateString(String delegationStateString) {
        this.delegationState = (delegationStateString != null ? DelegationState.valueOf(DelegationState.class, delegationStateString) : null);
    }

    @Override
    public boolean isDeleted() {
        return isDeleted;
    }

    @Override
    public void setDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    @Override
    public boolean isCanceled() {
        return isCanceled;
    }

    @Override
    public void setCanceled(boolean isCanceled) {
        this.isCanceled = isCanceled;
    }

    @Override
    public String getParentTaskId() {
        return parentTaskId;
    }

    @Override
    public Map<String, VariableInstanceEntity> getVariableInstanceEntities() {
        ensureVariableInstancesInitialized();
        return variableInstances;
    }

    @Override
    public int getSuspensionState() {
        return suspensionState;
    }

    @Override
    public void setSuspensionState(int suspensionState) {
        this.suspensionState = suspensionState;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public boolean isSuspended() {
        return suspensionState == SuspensionState.SUSPENDED.getStateCode();
    }

    @Override
    public Map<String, Object> getTaskLocalVariables() {
        Map<String, Object> variables = new HashMap<>();
        if (queryVariables != null) {
            for (VariableInstanceEntity variableInstance : queryVariables) {
                if (variableInstance.getId() != null && variableInstance.getTaskId() != null) {
                    variables.put(variableInstance.getName(), variableInstance.getValue());
                }
            }
        }
        return variables;
    }

    @Override
    public Map<String, Object> getProcessVariables() {
        Map<String, Object> variables = new HashMap<>();
        if (queryVariables != null) {
            for (VariableInstanceEntity variableInstance : queryVariables) {
                if (variableInstance.getId() != null && variableInstance.getTaskId() == null) {
                    variables.put(variableInstance.getName(), variableInstance.getValue());
                }
            }
        }
        return variables;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public List<VariableInstanceEntity> getQueryVariables() {
        if (queryVariables == null && Context.getCommandContext() != null) {
            queryVariables = new VariableInitializingList();
        }
        return queryVariables;
    }

    public void setQueryVariables(List<VariableInstanceEntity> queryVariables) {
        this.queryVariables = queryVariables;
    }

    public List<IdentityLinkEntity> getQueryIdentityLinks() {
        if (queryIdentityLinks == null) {
            queryIdentityLinks = new LinkedList<>();
        }
        return queryIdentityLinks;
    }

    public void setQueryIdentityLinks(List<IdentityLinkEntity> identityLinks) {
        queryIdentityLinks = identityLinks;
    }

    @Override
    public Date getClaimTime() {
        return claimTime;
    }

    @Override
    public void setClaimTime(Date claimTime) {
        this.claimTime = claimTime;
    }

    public String toString() {
        return "Task[id=" + id + ", name=" + name + "]";
    }

    @Override
    public boolean isCountEnabled() {
        return isCountEnabled;
    }

    @Override
    public void setCountEnabled(boolean isCountEnabled) {
        this.isCountEnabled = isCountEnabled;
    }

    @Override
    public void setVariableCount(int variableCount) {
        this.variableCount = variableCount;
    }

    @Override
    public int getVariableCount() {
        return variableCount;
    }

    @Override
    public void setIdentityLinkCount(int identityLinkCount) {
        this.identityLinkCount = identityLinkCount;
    }

    @Override
    public int getIdentityLinkCount() {
        return identityLinkCount;
    }

}
