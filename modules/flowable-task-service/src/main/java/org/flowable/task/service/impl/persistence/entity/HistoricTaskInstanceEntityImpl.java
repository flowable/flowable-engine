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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.identitylink.service.IdentityLinkServiceConfiguration;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.util.TaskVariableUtils;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInitializingList;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.flowable.variable.service.impl.util.CommandContextUtil;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class HistoricTaskInstanceEntityImpl extends AbstractTaskServiceEntity implements HistoricTaskInstanceEntity {

    private static final long serialVersionUID = 1L;

    protected String executionId;
    protected String processInstanceId;
    protected String processDefinitionId;
    protected String taskDefinitionId;
    protected String scopeId;
    protected String subScopeId;
    protected String scopeType;
    protected String scopeDefinitionId;
    protected String propagatedStageInstanceId;
    protected Date createTime;
    protected Date endTime;
    protected Long durationInMillis;
    protected String deleteReason;
    protected String name;
    protected String localizedName;
    protected String parentTaskId;
    protected String description;
    protected String localizedDescription;
    protected String owner;
    protected String assignee;
    protected String taskDefinitionKey;
    protected String formKey;
    protected int priority;
    protected Date dueDate;
    protected Date claimTime;
    protected String category;
    protected String tenantId = TaskServiceConfiguration.NO_TENANT_ID;
    protected Date lastUpdateTime;
    protected List<HistoricVariableInstanceEntity> queryVariables;
    protected List<HistoricIdentityLinkEntity> queryIdentityLinks;
    protected List<HistoricIdentityLinkEntity> identityLinks = new ArrayList<>();
    protected boolean isIdentityLinksInitialized;

    public HistoricTaskInstanceEntityImpl() {
    }

    public HistoricTaskInstanceEntityImpl(TaskEntity task) {        
        this.id = task.getId();
        this.taskDefinitionId = task.getTaskDefinitionId();
        this.processDefinitionId = task.getProcessDefinitionId();
        this.processInstanceId = task.getProcessInstanceId();
        this.executionId = task.getExecutionId();
        this.scopeId = task.getScopeId();
        this.subScopeId = task.getSubScopeId();
        this.scopeType = task.getScopeType();
        this.scopeDefinitionId = task.getScopeDefinitionId();
        this.propagatedStageInstanceId = task.getPropagatedStageInstanceId();
        this.name = task.getName();
        this.parentTaskId = task.getParentTaskId();
        this.description = task.getDescription();
        this.owner = task.getOwner();
        this.assignee = task.getAssignee();
        this.createTime = task.getCreateTime();
        this.taskDefinitionKey = task.getTaskDefinitionKey();
        this.formKey = task.getFormKey();

        this.setPriority(task.getPriority());
        this.setDueDate(task.getDueDate());
        this.setCategory(task.getCategory());

        // Inherit tenant id (if applicable)
        if (task.getTenantId() != null) {
            tenantId = task.getTenantId();
        }
    }

    // persistence //////////////////////////////////////////////////////////////

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("name", name);
        persistentState.put("owner", owner);
        persistentState.put("assignee", assignee);
        persistentState.put("endTime", endTime);
        persistentState.put("durationInMillis", durationInMillis);
        persistentState.put("description", description);
        persistentState.put("deleteReason", deleteReason);
        persistentState.put("taskDefinitionKey", taskDefinitionKey);
        persistentState.put("formKey", formKey);
        persistentState.put("priority", priority);
        persistentState.put("category", category);
        persistentState.put("executionId", executionId);
        persistentState.put("processDefinitionId", processDefinitionId);
        persistentState.put("taskDefinitionId", taskDefinitionId);
        persistentState.put("scopeId", scopeId);
        persistentState.put("subScopeId", subScopeId);
        persistentState.put("scopeType", scopeType);
        persistentState.put("scopeDefinitionId", scopeDefinitionId);
        persistentState.put("propagatedStageInstanceId", propagatedStageInstanceId);
        persistentState.put("parentTaskId", parentTaskId);
        persistentState.put("dueDate", dueDate);
        persistentState.put("claimTime", claimTime);
        persistentState.put("lastUpdateTime", lastUpdateTime);
        return persistentState;
    }
    
    @Override
    public void markEnded(String deleteReason, Date endTime) {
        if (this.endTime == null) {
            this.deleteReason = deleteReason;
            if (endTime != null) {
                this.endTime = endTime;
            } else {
                this.endTime = getTaskServiceConfiguration().getClock().getCurrentTime();
            }
            if (endTime != null && createTime != null) {
                this.durationInMillis = endTime.getTime() - createTime.getTime();
            }
        }
    }

    // getters and setters ////////////////////////////////////////////////////////

    @Override
    public String getExecutionId() {
        return executionId;
    }

    @Override
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
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
    public String getTaskDefinitionId() {
        return taskDefinitionId;
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
    public String getPropagatedStageInstanceId() {
        return propagatedStageInstanceId;
    }

    @Override
    public Date getStartTime() {
        return getCreateTime(); // For backwards compatible reason implemented with createTime and startTime
    }

    @Override
    public Date getEndTime() {
        return endTime;
    }

    @Override
    public Long getDurationInMillis() {
        return durationInMillis;
    }

    @Override
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    public void setTaskDefinitionId(String taskDefinitionId) {
        this.taskDefinitionId = taskDefinitionId;
    }

    @Override
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    @Override
    public void setDurationInMillis(Long durationInMillis) {
        this.durationInMillis = durationInMillis;
    }

    @Override
    public String getDeleteReason() {
        return deleteReason;
    }

    @Override
    public void setDeleteReason(String deleteReason) {
        this.deleteReason = deleteReason;
    }

    @Override
    public String getName() {
        if (localizedName != null && localizedName.length() > 0) {
            return localizedName;
        } else {
            return name;
        }
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setLocalizedName(String name) {
        this.localizedName = name;
    }

    @Override
    public String getDescription() {
        if (localizedDescription != null && localizedDescription.length() > 0) {
            return localizedDescription;
        } else {
            return description;
        }
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public void setLocalizedDescription(String description) {
        this.localizedDescription = description;
    }

    @Override
    public String getAssignee() {
        return assignee;
    }

    @Override
    public void setAssignee(String assignee) {
        this.assignee = assignee;
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
    public Date getCreateTime() {
        return createTime;
    }

    @Override
    public String getFormKey() {
        return formKey;
    }

    @Override
    public void setFormKey(String formKey) {
        this.formKey = formKey;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public Date getDueDate() {
        return dueDate;
    }

    @Override
    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public String getParentTaskId() {
        return parentTaskId;
    }

    @Override
    public void setParentTaskId(String parentTaskId) {
        this.parentTaskId = parentTaskId;
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
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public Date getTime() {
        return getCreateTime();
    }

    @Override
    public Long getWorkTimeInMillis() {
        if (endTime == null || claimTime == null) {
            return null;
        }
        return endTime.getTime() - claimTime.getTime();
    }

    @Override
    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    @Override
    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    @Override
    public Map<String, Object> getTaskLocalVariables() {
        Map<String, Object> variables = new HashMap<>();
        if (queryVariables != null) {
            for (HistoricVariableInstanceEntity variableInstance : queryVariables) {
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
            for (HistoricVariableInstanceEntity variableInstance : queryVariables) {
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
            for (HistoricVariableInstanceEntity variableInstance : queryVariables) {
                if (TaskVariableUtils.isCaseRelated(variableInstance) && variableInstance.getScopeId().equals(this.getScopeId())
                        && variableInstance.getTaskId() == null){
                    variables.put(variableInstance.getName(), variableInstance.getValue());
                }
            }
        }
        return variables;
    }

    @Override
    public List<HistoricVariableInstanceEntity> getQueryVariables() {
        if (queryVariables == null && Context.getCommandContext() != null) {
            queryVariables = new HistoricVariableInitializingList();
        }
        return queryVariables;
    }

    @Override
    public void setQueryVariables(List<HistoricVariableInstanceEntity> queryVariables) {
        this.queryVariables = queryVariables;
    }

    @Override
    public List<HistoricIdentityLinkEntity> getIdentityLinks() {
        if (!isIdentityLinksInitialized) {
            if (queryIdentityLinks == null) {
                identityLinks = getIdentityLinkServiceConfiguration().getHistoricIdentityLinkEntityManager().findHistoricIdentityLinksByTaskId(id);
            } else {
                identityLinks = queryIdentityLinks;
            }
            isIdentityLinksInitialized = true;
        }

        return identityLinks;
    }

    public List<HistoricIdentityLinkEntity> getQueryIdentityLinks() {
        if (queryIdentityLinks == null) {
            queryIdentityLinks = new LinkedList<>();
        }
        return queryIdentityLinks;
    }

    public void setQueryIdentityLinks(List<HistoricIdentityLinkEntity> identityLinks) {
        queryIdentityLinks = identityLinks;
    }
    
    protected TaskServiceConfiguration getTaskServiceConfiguration() {
        return (TaskServiceConfiguration) getTaskEngineConfiguration().getServiceConfigurations().get(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG);
    }
    
    protected IdentityLinkServiceConfiguration getIdentityLinkServiceConfiguration() {
        return (IdentityLinkServiceConfiguration) getTaskEngineConfiguration().getServiceConfigurations().get(EngineConfigurationConstants.KEY_IDENTITY_LINK_SERVICE_CONFIG);
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
    public String toString() {
        return "HistoricTaskInstanceEntity[id=" + id + "]";
    }

}
