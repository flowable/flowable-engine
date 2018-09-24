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

package org.flowable.rest.service.api.runtime.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.flowable.common.rest.util.DateToStringSerializer;
import org.flowable.rest.service.api.engine.variable.RestVariable;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Frederik Heremans
 */
public class TaskResponse {

    protected String id;
    protected String url;
    protected String owner;
    protected String assignee;
    protected String delegationState;
    protected String name;
    protected String description;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date createTime;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date dueDate;
    protected int priority;
    protected boolean suspended;
    protected String taskDefinitionKey;
    protected String scopeDefinitionId;
    protected String scopeId;
    protected String scopeType;
    protected String tenantId;
    protected String category;
    protected String formKey;

    // References to other resources
    protected String parentTaskId;
    protected String parentTaskUrl;
    protected String executionId;
    protected String executionUrl;
    protected String processInstanceId;
    protected String processInstanceUrl;
    protected String processDefinitionId;
    protected String processDefinitionUrl;

    protected List<RestVariable> variables = new ArrayList<>();

    public TaskResponse(Task task) {
        setId(task.getId());
        setOwner(task.getOwner());
        setAssignee(task.getAssignee());
        setDelegationState(getDelegationStateString(task.getDelegationState()));
        setName(task.getName());
        setDescription(task.getDescription());
        setCreateTime(task.getCreateTime());
        setDueDate(task.getDueDate());
        setPriority(task.getPriority());
        setSuspended(task.isSuspended());
        setTaskDefinitionKey(task.getTaskDefinitionKey());
        setParentTaskId(task.getParentTaskId());
        setExecutionId(task.getExecutionId());
        setCategory(task.getCategory());
        setProcessInstanceId(task.getProcessInstanceId());
        setProcessDefinitionId(task.getProcessDefinitionId());
        setScopeDefinitionId(task.getScopeDefinitionId());
        setScopeId(task.getScopeId());
        setScopeType(task.getScopeType());
        setTenantId(task.getTenantId());
        setFormKey(task.getFormKey());
    }

    protected String getDelegationStateString(DelegationState state) {
        String result = null;
        if (state != null) {
            result = state.toString().toLowerCase();
        }
        return result;
    }

    @ApiModelProperty(example = "8")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(example = "http://localhost:8182/runtime/tasks/8")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @ApiModelProperty(example = "owner")
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @ApiModelProperty(example = "kermit")
    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    @ApiModelProperty(example = "pending", value = "Delegation-state of the task, can be null, \"pending\" or \"resolved\".")
    public String getDelegationState() {
        return delegationState;
    }

    public void setDelegationState(String delegationState) {
        this.delegationState = delegationState;
    }

    @ApiModelProperty(example = "My task")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "Task description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(example = "2018-04-17T10:17:43.902+0000")
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @ApiModelProperty(example = "2018-04-17T10:17:43.902+0000")
    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    @ApiModelProperty(example = "50")
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    @ApiModelProperty(example = "theTask")
    public String getTaskDefinitionKey() {
        return taskDefinitionKey;
    }

    public void setTaskDefinitionKey(String taskDefinitionKey) {
        this.taskDefinitionKey = taskDefinitionKey;
    }

    @ApiModelProperty(example = "null")
    public String getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(String parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    @ApiModelProperty(example = "null")
    public String getParentTaskUrl() {
        return parentTaskUrl;
    }

    public void setParentTaskUrl(String parentTaskUrl) {
        this.parentTaskUrl = parentTaskUrl;
    }

    @ApiModelProperty(example = "5")
    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    @ApiModelProperty(example = "http://localhost:8182/runtime/executions/5")
    public String getExecutionUrl() {
        return executionUrl;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @ApiModelProperty(example = "ExampleCategory")
    public String getCategory() {
        return category;
    }

    public void setExecutionUrl(String executionUrl) {
        this.executionUrl = executionUrl;
    }

    @ApiModelProperty(example = "5")
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @ApiModelProperty(example = "http://localhost:8182/runtime/process-instances/5")
    public String getProcessInstanceUrl() {
        return processInstanceUrl;
    }

    public void setProcessInstanceUrl(String processInstanceUrl) {
        this.processInstanceUrl = processInstanceUrl;
    }

    @ApiModelProperty(example = "oneTaskProcess%3A1%3A4")
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @ApiModelProperty(example = "http://localhost:8182/runtime/process-definitions/oneTaskProcess%3A1%3A4")
    public String getProcessDefinitionUrl() {
        return processDefinitionUrl;
    }

    public void setProcessDefinitionUrl(String processDefinitionUrl) {
        this.processDefinitionUrl = processDefinitionUrl;
    }

    public List<RestVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<RestVariable> variables) {
        this.variables = variables;
    }

    public void addVariable(RestVariable variable) {
        variables.add(variable);
    }
    
    @ApiModelProperty(example = "12")
    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    public void setScopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
    }

    @ApiModelProperty(example = "14")
    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    @ApiModelProperty(example = "cmmn")
    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    @ApiModelProperty(example = "someTenantId")
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getFormKey() {
        return formKey;
    }

    public void setFormKey(String formKey) {
        this.formKey = formKey;
    }
}
