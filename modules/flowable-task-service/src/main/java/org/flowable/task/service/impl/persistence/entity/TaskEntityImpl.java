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
package org.flowable.task.service.impl.persistence.entity;

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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.db.SuspensionState;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.service.IdentityLinkServiceConfiguration;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityManager;
import org.flowable.task.api.DelegationState;
import org.flowable.task.service.InternalTaskAssignmentManager;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.flowable.task.service.impl.util.CountingTaskUtil;
import org.flowable.task.service.impl.util.TaskVariableUtils;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.VariableInitializingList;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableScopeImpl;
import org.flowable.variable.service.impl.util.CommandContextUtil;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Falko Menge
 * @author Tijs Rademakers
 */
public class TaskEntityImpl extends AbstractTaskServiceVariableScopeEntity implements TaskEntity, CountingTaskEntity, Serializable {

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
    protected String processInstanceId;
    protected String processDefinitionId;
    protected String taskDefinitionId;
    
    protected String scopeId;
    protected String subScopeId;
    protected String scopeType;
    protected String scopeDefinitionId;
    protected String propagatedStageInstanceId;

    protected String taskDefinitionKey;
    protected String formKey;

    protected boolean isCanceled;

    private boolean isCountEnabled;
    protected int variableCount;
    protected int identityLinkCount;
    protected int subTaskCount;

    protected Date claimTime;

    protected String tenantId = TaskServiceConfiguration.NO_TENANT_ID;

    // Non-persisted
    protected String eventName;
    protected String eventHandlerId;
    protected List<VariableInstanceEntity> queryVariables;
    protected List<IdentityLinkEntity> queryIdentityLinks;
    protected boolean forcedUpdate;

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
        if (processInstanceId != null) {
            persistentState.put("processInstanceId", this.processInstanceId);
        }
        if (processDefinitionId != null) {
            persistentState.put("processDefinitionId", this.processDefinitionId);
        }
        if (taskDefinitionId != null) {
            persistentState.put("taskDefinitionId", this.taskDefinitionId);
        }
        if (taskDefinitionKey != null) {
            persistentState.put("taskDefinitionKey", this.taskDefinitionKey);
        }
        if (scopeId != null) {
            persistentState.put("scopeId", this.scopeId);
        }
        if (subScopeId != null) {
            persistentState.put("subScopeId", this.subScopeId);
        }
        if (scopeType != null) {
            persistentState.put("scopeType", this.scopeType);
        }
        if (scopeDefinitionId != null) {
            persistentState.put("scopeDefinitionId", this.scopeDefinitionId);
        }
        if (propagatedStageInstanceId != null) {
            persistentState.put("propagatedStageInstanceId", propagatedStageInstanceId);
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
        persistentState.put("subTaskCount", this.subTaskCount);

        return persistentState;
    }

    @Override
    public void forceUpdate() {
        this.forcedUpdate = true;
    }

    // variables //////////////////////////////////////////////////////////////////

    @Override
    protected VariableScopeImpl getParentVariableScope() {
        return getTaskServiceConfiguration().getInternalTaskVariableScopeResolver().resolveParentVariableScope(this);
    }

    @Override
    protected void initializeVariableInstanceBackPointer(VariableInstanceEntity variableInstance) {
        variableInstance.setTaskId(id);
        if (ScopeTypes.CMMN.equals(this.scopeType)) {
            variableInstance.setScopeId(this.scopeId);
            variableInstance.setScopeType(this.scopeType);
            variableInstance.setSubScopeId(this.subScopeId);
        } else {
            variableInstance.setExecutionId(this.executionId);
            variableInstance.setProcessInstanceId(this.processInstanceId);
            variableInstance.setProcessDefinitionId(this.processDefinitionId);
        }
    }

    @Override
    protected void addLoggingSessionInfo(ObjectNode loggingNode) {
        // TODO
    }

    @Override
    protected List<VariableInstanceEntity> loadVariableInstances() {
        return getVariableServiceConfiguration().getVariableService().createInternalVariableInstanceQuery().taskId(id).list();
    }
    
    @Override
    protected VariableInstanceEntity createVariableInstance(String variableName, Object value) {
        VariableInstanceEntity variableInstance = super.createVariableInstance(variableName, value);
        
        CountingTaskUtil.handleInsertVariableInstanceEntityCount(variableInstance, getTaskServiceConfiguration());

        return variableInstance;
        
    }
    
    @Override
    protected void deleteVariableInstanceForExplicitUserCall(VariableInstanceEntity variableInstance) {
        super.deleteVariableInstanceForExplicitUserCall(variableInstance);
        
        CountingTaskUtil.handleDeleteVariableInstanceEntityCount(variableInstance, true, getTaskServiceConfiguration());
    }

    // task assignment ////////////////////////////////////////////////////////////

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
    public List<IdentityLinkEntity> getIdentityLinks() {
        if (!isIdentityLinksInitialized) {
            if (queryIdentityLinks == null) {
                taskIdentityLinkEntities = getIdentityLinkServiceConfiguration().getIdentityLinkEntityManager().findIdentityLinksByTaskId(id);
            } else {
                taskIdentityLinkEntities = queryIdentityLinks;
            }
            isIdentityLinksInitialized = true;
        }

        return taskIdentityLinkEntities;
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
    public void setAssigneeValue(String assignee) {
        InternalTaskAssignmentManager taskAssignmentManager = getTaskAssignmentManager();
        if (taskAssignmentManager != null) {
            taskAssignmentManager.changeAssignee(this, assignee);
        } else {
            this.originalAssignee = this.assignee;
            this.assignee = assignee;
            assigneeUpdatedCount++;
        }
    }

    @Override
    public void setOwner(String owner) {
        this.owner = owner;
    }
    
    @Override
    public void setOwnerValue(String owner) {
        InternalTaskAssignmentManager taskAssignmentManager = getTaskAssignmentManager();
        if (taskAssignmentManager != null) {
            taskAssignmentManager.changeOwner(this, owner);
        } else {
            this.owner = owner;
        }
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
    public void addUserIdentityLink(String userId, String identityLinkType) {
        IdentityLinkEntityManager identityLinkEntityManager = getIdentityLinkServiceConfiguration().getIdentityLinkEntityManager();
        IdentityLinkEntity identityLink = identityLinkEntityManager.addTaskIdentityLink(this.id, userId, null, identityLinkType);
        InternalTaskAssignmentManager taskAssignmentManager = getTaskAssignmentManager();
        if (taskAssignmentManager != null) {
            taskAssignmentManager.addUserIdentityLink(this, identityLink);
        }
    }

    @Override
    public void addGroupIdentityLink(String groupId, String identityLinkType) {
        IdentityLinkEntityManager identityLinkEntityManager = getIdentityLinkServiceConfiguration().getIdentityLinkEntityManager();
        IdentityLinkEntity identityLink = identityLinkEntityManager.addTaskIdentityLink(this.id, null, groupId, identityLinkType);
        InternalTaskAssignmentManager taskAssignmentManager = getTaskAssignmentManager();
        if (taskAssignmentManager != null) {
            taskAssignmentManager.addGroupIdentityLink(this, identityLink);
        }
    }

    @Override
    public void deleteCandidateUser(String userId) {
        deleteUserIdentityLink(userId, IdentityLinkType.CANDIDATE);
    }

    @Override
    public void deleteCandidateGroup(String groupId) {
        deleteGroupIdentityLink(groupId, IdentityLinkType.CANDIDATE);
    }
    
    @Override
    public void deleteUserIdentityLink(String userId, String identityLinkType) {
        IdentityLinkEntityManager identityLinkEntityManager = getIdentityLinkServiceConfiguration().getIdentityLinkEntityManager();
        identityLinkEntityManager.deleteTaskIdentityLink(this.id, getIdentityLinks(), userId, null, identityLinkType);
    }
    
    @Override
    public void deleteGroupIdentityLink(String groupId, String identityLinkType) {
        IdentityLinkEntityManager identityLinkEntityManager = getIdentityLinkServiceConfiguration().getIdentityLinkEntityManager();
        identityLinkEntityManager.deleteTaskIdentityLink(this.id, getIdentityLinks(), null, groupId,identityLinkType);
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
    protected boolean isPropagateToHistoricVariable() {
        return true;
    }

    // Overridden to avoid fetching *all* variables (as is the case in the super // call)
    @Override
    protected VariableInstanceEntity getSpecificVariable(String variableName) {
        CommandContext commandContext = Context.getCommandContext();
        if (commandContext == null) {
            throw new FlowableException("lazy loading outside command context");
        }

        return getVariableServiceConfiguration().getVariableService()
                .createInternalVariableInstanceQuery()
                .taskId(id)
                .name(variableName)
                .singleResult();
    }

    @Override
    protected List<VariableInstanceEntity> getSpecificVariables(Collection<String> variableNames) {
        CommandContext commandContext = Context.getCommandContext();
        if (commandContext == null) {
            throw new FlowableException("lazy loading outside command context");
        }
        return getVariableServiceConfiguration().getVariableService()
                .createInternalVariableInstanceQuery()
                .taskId(id)
                .names(variableNames)
                .list();
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
    public String getTaskDefinitionId() {
        return taskDefinitionId;
    }

    @Override
    public void setTaskDefinitionId(String taskDefinitionId) {
        this.taskDefinitionId = taskDefinitionId;
    }

    @Override
    public String getScopeId() {
        return scopeId;
    }

    @Override
    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    @Override
    public String getSubScopeId() {
        return subScopeId;
    }

    @Override
    public void setSubScopeId(String subScopeId) {
        this.subScopeId = subScopeId;
    }

    @Override
    public String getScopeType() {
        return scopeType;
    }

    @Override
    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    @Override
    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    @Override
    public void setScopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
    }

    @Override
    public void setPropagatedStageInstanceId(String propagatedStageInstanceId) {
        this.propagatedStageInstanceId = propagatedStageInstanceId;
    }

    @Override
    public String getPropagatedStageInstanceId() {
        return propagatedStageInstanceId;
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
    public String getEventHandlerId() {
        return eventHandlerId;
    }
    
    @Override
    public void setEventHandlerId(String eventHandlerId) {
        this.eventHandlerId = eventHandlerId;
    }

    @Override
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
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
    public void addCandidateUser(String userId) {
        IdentityLinkEntityManager identityLinkEntityManager = getIdentityLinkServiceConfiguration().getIdentityLinkEntityManager();
        IdentityLinkEntity identityLink = identityLinkEntityManager.addCandidateUser(this.id, userId);
        InternalTaskAssignmentManager taskAssignmentManager = getTaskAssignmentManager();
        if (taskAssignmentManager != null) {
            taskAssignmentManager.addCandidateUser(this, identityLink);
        }
    }
    
    @Override
    public void addCandidateUsers(Collection<String> candidateUsers) {
        IdentityLinkEntityManager identityLinkEntityManager = getIdentityLinkServiceConfiguration().getIdentityLinkEntityManager();
        List<IdentityLinkEntity> identityLinks = identityLinkEntityManager.addCandidateUsers(this.id, candidateUsers);
        InternalTaskAssignmentManager taskAssignmentManager = getTaskAssignmentManager();
        if (taskAssignmentManager != null) {
            taskAssignmentManager.addCandidateUsers(this, convertToIdentityLinks(identityLinks));
        }
    }
    
    @Override
    public void addCandidateGroup(String groupId) {
        IdentityLinkEntityManager identityLinkEntityManager = getIdentityLinkServiceConfiguration().getIdentityLinkEntityManager();
        IdentityLinkEntity identityLink = identityLinkEntityManager.addCandidateGroup(this.id, groupId);
        InternalTaskAssignmentManager taskAssignmentManager = getTaskAssignmentManager();
        if (taskAssignmentManager != null) {
            taskAssignmentManager.addCandidateGroup(this, identityLink);
        }
    }
    
    @Override
    public void addCandidateGroups(Collection<String> candidateGroups) {
        IdentityLinkEntityManager identityLinkEntityManager = getIdentityLinkServiceConfiguration().getIdentityLinkEntityManager();
        List<IdentityLinkEntity> identityLinks = identityLinkEntityManager.addCandidateGroups(this.id, candidateGroups);
        InternalTaskAssignmentManager taskAssignmentManager = getTaskAssignmentManager();
        if (taskAssignmentManager != null) {
            taskAssignmentManager.addCandidateGroups(this, convertToIdentityLinks(identityLinks));
        }
    }
    
    protected List<IdentityLink> convertToIdentityLinks(List<IdentityLinkEntity> identityLinks) {
        List<IdentityLink> identityLinkObjects = new ArrayList<>(identityLinks);
        return identityLinkObjects;
    }
    
    protected InternalTaskAssignmentManager getTaskAssignmentManager() {
        TaskServiceConfiguration taskServiceConfiguration = getTaskServiceConfiguration();
        if (taskServiceConfiguration != null) {
            return taskServiceConfiguration.getInternalTaskAssignmentManager();
        }
        
        return null;
    }

    protected TaskServiceConfiguration getTaskServiceConfiguration() {
        return (TaskServiceConfiguration) getTaskEngineConfiguration().getServiceConfigurations().get(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG);
    }

    protected IdentityLinkServiceConfiguration getIdentityLinkServiceConfiguration() {
        return (IdentityLinkServiceConfiguration) getTaskEngineConfiguration().getServiceConfigurations().get(EngineConfigurationConstants.KEY_IDENTITY_LINK_SERVICE_CONFIG);
    }

    @Override
    protected VariableServiceConfiguration getVariableServiceConfiguration() {
        return (VariableServiceConfiguration) getTaskEngineConfiguration().getServiceConfigurations().get(EngineConfigurationConstants.KEY_VARIABLE_SERVICE_CONFIG);
    }

    protected AbstractEngineConfiguration getTaskEngineConfiguration() {
        Map<String, AbstractEngineConfiguration> engineConfigurations = CommandContextUtil.getCommandContext().getEngineConfigurations();
        AbstractEngineConfiguration engineConfiguration = null;
        if (ScopeTypes.CMMN.equals(scopeType)) {
            engineConfiguration = engineConfigurations.get(EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG);
        } else {
            engineConfiguration = engineConfigurations.get(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG);
            if (engineConfiguration == null) {
                engineConfiguration = engineConfigurations.get(EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG);
            }
        }

        return engineConfiguration;
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
                if (this.getProcessInstanceId() != null && this.getProcessInstanceId()
                        .equals(variableInstance.getProcessInstanceId()) && variableInstance.getTaskId() == null) {
                    variables.put(variableInstance.getName(), variableInstance.getValue());
                }
            }
        }
        return variables;
    }

    @Override
    public Map<String, Object> getCaseVariables() {
        Map<String, Object> variables = new HashMap<>();
        if (queryVariables != null) {
            for (VariableInstanceEntity variableInstance : queryVariables) {
                if (TaskVariableUtils.isCaseRelated(variableInstance) && variableInstance.getScopeId().equals(this.getScopeId())
                        && variableInstance.getTaskId() == null) {
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

    @Override
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

    @Override
    public String toString() {
        return "Task[id=" + id + ", name=" + name + "]";
    }

    @Override
    public boolean isCountEnabled() {
        return isCountEnabled;
    }

    public boolean getIsCountEnabled() {
        return isCountEnabled;
    }

    @Override
    public void setCountEnabled(boolean isCountEnabled) {
        this.isCountEnabled = isCountEnabled;
    }

    public void setIsCountEnabled(boolean isCountEnabled) {
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

    @Override
    public int getSubTaskCount() {
        return subTaskCount;
    }

    @Override
    public void setSubTaskCount(int subTaskCount) {
        this.subTaskCount = subTaskCount;
    }
    
}
