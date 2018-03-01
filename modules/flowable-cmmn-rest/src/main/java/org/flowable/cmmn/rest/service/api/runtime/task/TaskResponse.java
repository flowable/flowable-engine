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

package org.flowable.cmmn.rest.service.api.runtime.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable;
import org.flowable.rest.util.DateToStringSerializer;
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
    protected String tenantId;
    protected String category;
    protected String formKey;

    // References to other resources
    protected String parentTaskId;
    protected String parentTaskUrl;
    protected String caseInstanceId;
    protected String caseInstanceUrl;
    protected String caseDefinitionId;
    protected String caseDefinitionUrl;

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
        setCategory(task.getCategory());
        if ("cmmn".equals(task.getScopeType())) {
            setCaseInstanceId(task.getScopeId());
            setCaseDefinitionId(task.getScopeDefinitionId());
        }
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

    @ApiModelProperty(example = "http://localhost:8182/cmmn-runtime/tasks/8")
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

    @ApiModelProperty(example = "2013-04-17T10:17:43.902+0000")
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @ApiModelProperty(example = "2013-04-17T10:17:43.902+0000")
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

    public void setCategory(String category) {
        this.category = category;
    }

    @ApiModelProperty(example = "ExampleCategory")
    public String getCategory() {
        return category;
    }

    @ApiModelProperty(example = "5")
    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public void setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }

    @ApiModelProperty(example = "http://localhost:8182/cmmn-runtime/case-instances/5")
    public String getCaseInstanceUrl() {
        return caseInstanceUrl;
    }

    public void setCaseInstanceUrl(String caseInstanceUrl) {
        this.caseInstanceUrl = caseInstanceUrl;
    }

    @ApiModelProperty(example = "oneTaskCase%3A1%3A4")
    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }

    @ApiModelProperty(example = "http://localhost:8182/cmmn-runtime/case-definitions/oneTaskCase%3A1%3A4")
    public String getCaseDefinitionUrl() {
        return caseDefinitionUrl;
    }

    public void setCaseDefinitionUrl(String caseDefinitionUrl) {
        this.caseDefinitionUrl = caseDefinitionUrl;
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

    @ApiModelProperty(example = "null")
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
