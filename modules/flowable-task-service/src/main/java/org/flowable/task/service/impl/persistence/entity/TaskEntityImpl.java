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

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.context.Context;
import org.flowable.engine.common.impl.db.SuspensionState;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.service.IdentityLinkType;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityManager;
import org.flowable.task.api.DelegationState;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.flowable.task.service.impl.util.CommandContextUtil;
import org.flowable.task.service.impl.util.CountingTaskUtil;
import org.flowable.variable.api.type.VariableScopeType;
import org.flowable.variable.service.impl.persistence.entity.VariableInitializingList;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableScopeImpl;

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
    protected String processInstanceId;
    protected String processDefinitionId;
    
    protected String scopeId;
    protected String subScopeId;
    protected String scopeType;
    protected String scopeDefinitionId;

    protected String taskDefinitionKey;
    protected String formKey;

    protected boolean isDeleted;
    protected boolean isCanceled;

    private boolean isCountEnabled;
    private int variableCount;
    private int identityLinkCount;
    

    protected Date claimTime;

    protected String tenantId = TaskServiceConfiguration.NO_TENANT_ID;

    // Non-persisted
    protected String eventName;
    protected String eventHandlerId;
    protected List<VariableInstanceEntity> queryVariables;
    protected List<IdentityLinkEntity> queryIdentityLinks;
    protected boolean forcedUpdate;


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
        if (processInstanceId != null) {
            persistentState.put("processInstanceId", this.processInstanceId);
        }
        if (processDefinitionId != null) {
            persistentState.put("processDefinitionId", this.processDefinitionId);
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
        return CommandContextUtil.getTaskServiceConfiguration().getInternalTaskVariableScopeResolver().resolveParentVariableScope(this);
    }

    @Override
    protected void initializeVariableInstanceBackPointer(VariableInstanceEntity variableInstance) {
        variableInstance.setTaskId(id);
        if (VariableScopeType.CMMN.equals(this.scopeType)) {
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
    protected List<VariableInstanceEntity> loadVariableInstances() {
        return CommandContextUtil.getVariableInstanceEntityManager().findVariableInstancesByTaskId(id);
    }
    
    @Override
    protected VariableInstanceEntity createVariableInstance(String variableName, Object value) {
        VariableInstanceEntity variableInstance = super.createVariableInstance(variableName, value);
        
        CountingTaskUtil.handleInsertVariableInstanceEntityCount(variableInstance);

        return variableInstance;
        
    }
    
    @Override
    protected void deleteVariableInstanceForExplicitUserCall(VariableInstanceEntity variableInstance) {
        super.deleteVariableInstanceForExplicitUserCall(variableInstance);
        
        CountingTaskUtil.handleDeleteVariableInstanceEntityCount(variableInstance, true);
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
                taskIdentityLinkEntities = CommandContextUtil.getIdentityLinkEntityManager().findIdentityLinksByTaskId(id);
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
    public void addUserIdentityLink(String userId, String identityLinkType) {
        IdentityLinkEntityManager identityLinkEntityManager = CommandContextUtil.getIdentityLinkEntityManager();
        identityLinkEntityManager.addTaskIdentityLink(this.id, userId, null, identityLinkType);
    }

    @Override
    public void addGroupIdentityLink(String groupId, String identityLinkType) {
        IdentityLinkEntityManager identityLinkEntityManager = CommandContextUtil.getIdentityLinkEntityManager();
        identityLinkEntityManager.addTaskIdentityLink(this.id, null, groupId, identityLinkType);
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
        IdentityLinkEntityManager identityLinkEntityManager = CommandContextUtil.getIdentityLinkEntityManager();
        identityLinkEntityManager.deleteTaskIdentityLink(this.id, getIdentityLinks(), userId, null, identityLinkType);
    }
    
    @Override
    public void deleteGroupIdentityLink(String groupId, String identityLinkType) {
        IdentityLinkEntityManager identityLinkEntityManager = CommandContextUtil.getIdentityLinkEntityManager();
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
        VariableInstanceEntity variableInstance = CommandContextUtil.getVariableInstanceEntityManager().findVariableInstanceByTaskAndName(id, variableName);

        return variableInstance;
    }

    @Override
    protected List<VariableInstanceEntity> getSpecificVariables(Collection<String> variableNames) {
        CommandContext commandContext = Context.getCommandContext();
        if (commandContext == null) {
            throw new FlowableException("lazy loading outside command context");
        }
        return CommandContextUtil.getVariableInstanceEntityManager().findVariableInstancesByTaskAndNames(id, variableNames);
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
        IdentityLinkEntityManager identityLinkEntityManager = CommandContextUtil.getIdentityLinkEntityManager();
        identityLinkEntityManager.addCandidateUser(this.id, userId);
    }
    
    @Override
    public void addCandidateUsers(Collection<String> candidateUsers) {
        IdentityLinkEntityManager identityLinkEntityManager = CommandContextUtil.getIdentityLinkEntityManager();
        identityLinkEntityManager.addCandidateUsers(this.id, candidateUsers);        
    }
    
    @Override
    public void addCandidateGroup(String groupId) {
        IdentityLinkEntityManager identityLinkEntityManager = CommandContextUtil.getIdentityLinkEntityManager();
        identityLinkEntityManager.addCandidateGroup(this.id, groupId);
    }
    
    @Override
    public void addCandidateGroups(Collection<String> candidateGroups) {
        IdentityLinkEntityManager identityLinkEntityManager = CommandContextUtil.getIdentityLinkEntityManager();
        identityLinkEntityManager.addCandidateGroups(this.id, candidateGroups);
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

    @Override
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
