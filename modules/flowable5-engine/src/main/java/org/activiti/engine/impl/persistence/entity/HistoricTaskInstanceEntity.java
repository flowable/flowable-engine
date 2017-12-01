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

package org.activiti.engine.impl.persistence.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.db.BulkDeleteable;
import org.activiti.engine.impl.db.PersistentObject;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class HistoricTaskInstanceEntity extends HistoricScopeInstanceEntity implements HistoricTaskInstance, BulkDeleteable, PersistentObject {

    private static final long serialVersionUID = 1L;

    protected String executionId;
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
    protected String tenantId = ProcessEngineConfiguration.NO_TENANT_ID;
    protected List<HistoricVariableInstanceEntity> queryVariables;

    public HistoricTaskInstanceEntity() {
    }

    public HistoricTaskInstanceEntity(TaskEntity task, ExecutionEntity execution) {
        this.id = task.getId();
        if (execution != null) {
            this.processDefinitionId = execution.getProcessDefinitionId();
            this.processInstanceId = execution.getProcessInstanceId();
            this.executionId = execution.getId();
        }
        this.name = task.getName();
        this.parentTaskId = task.getParentTaskId();
        this.description = task.getDescription();
        this.owner = task.getOwner();
        this.assignee = task.getAssignee();
        this.startTime = Context.getProcessEngineConfiguration().getClock().getCurrentTime();
        this.taskDefinitionKey = task.getTaskDefinitionKey();

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
        persistentState.put("processDefinitionId", processDefinitionId);
        if (parentTaskId != null) {
            persistentState.put("parentTaskId", parentTaskId);
        }
        if (dueDate != null) {
            persistentState.put("dueDate", dueDate);
        }
        if (claimTime != null) {
            persistentState.put("claimTime", claimTime);
        }
        return persistentState;
    }

    // getters and setters //////////////////////////////////////////////////////
    @Override
    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    @Override
    public String getName() {
        if (localizedName != null && localizedName.length() > 0) {
            return localizedName;
        } else {
            return name;
        }
    }

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

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    @Override
    public String getTaskDefinitionKey() {
        return taskDefinitionKey;
    }

    public void setTaskDefinitionKey(String taskDefinitionKey) {
        this.taskDefinitionKey = taskDefinitionKey;
    }

    @Override
    public Date getCreateTime() {
        return getStartTime(); // For backwards compatible reason implemented with createTime and startTime
    }

    @Override
    public String getFormKey() {
        return formKey;
    }

    public void setFormKey(String formKey) {
        this.formKey = formKey;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    @Override
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public String getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(String parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    @Override
    public Date getClaimTime() {
        return claimTime;
    }

    public void setClaimTime(Date claimTime) {
        this.claimTime = claimTime;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public Date getTime() {
        return getStartTime();
    }

    @Override
    public Long getWorkTimeInMillis() {
        if (endTime == null || claimTime == null) {
            return null;
        }
        return endTime.getTime() - claimTime.getTime();
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
                if (variableInstance.getId() != null && variableInstance.getTaskId() == null) {
                    variables.put(variableInstance.getName(), variableInstance.getValue());
                }
            }
        }
        return variables;
    }

    public List<HistoricVariableInstanceEntity> getQueryVariables() {
        if (queryVariables == null && Context.getCommandContext() != null) {
            queryVariables = new HistoricVariableInitializingList();
        }
        return queryVariables;
    }

    public void setQueryVariables(List<HistoricVariableInstanceEntity> queryVariables) {
        this.queryVariables = queryVariables;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HistoricTaskInstanceEntity[");
        sb.append("id=").append(id);
        sb.append(", name=").append(name);
        sb.append("]");
        return sb.toString();
    }

}
