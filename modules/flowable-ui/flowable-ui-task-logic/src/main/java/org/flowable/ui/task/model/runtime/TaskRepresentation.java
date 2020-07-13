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
package org.flowable.ui.task.model.runtime;

import java.util.Date;
import java.util.List;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.ui.common.model.AbstractRepresentation;
import org.flowable.ui.common.model.UserRepresentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * REST representation of a task.
 *
 * @author Tijs Rademakers
 */
public class TaskRepresentation extends AbstractRepresentation {

    protected String id;
    protected String name;
    protected String description;
    protected String category;
    protected UserRepresentation assignee;
    protected Date created;
    protected Date dueDate;
    protected Date endDate;
    protected Long duration;
    protected Integer priority;
    protected String processInstanceId;
    protected String processInstanceName;

    protected String processDefinitionId;
    protected String processDefinitionName;
    protected String processDefinitionDescription;
    protected String processDefinitionKey;
    protected String processDefinitionCategory;
    protected int processDefinitionVersion;
    protected String processDefinitionDeploymentId;

    protected String scopeId;
    protected String scopeType;
    protected String caseInstanceName;

    protected String scopeDefinitionId;
    protected String caseDefinitionName;
    protected String caseDefinitionDescription;
    protected String caseDefinitionKey;
    protected String caseDefinitionCategory;
    protected int caseDefinitionVersion;
    protected String caseDefinitionDeploymentId;

    protected String parentTaskId;
    protected String parentTaskName;

    protected String formKey;
    protected String processInstanceStartUserId;
    protected boolean initiatorCanCompleteTask;
    protected boolean isMemberOfCandidateGroup;
    protected boolean isMemberOfCandidateUsers;

    @JsonDeserialize(contentAs = UserRepresentation.class)
    @JsonInclude(Include.NON_NULL)
    protected List<UserRepresentation> involvedPeople;

    // Needed for serialization!
    public TaskRepresentation() {
    }

    public TaskRepresentation(Task task) {
        initializeTaskDetails(task);
    }

    public TaskRepresentation(HistoricTaskInstance task) {
        initializeTaskDetails(task);
    }

    public TaskRepresentation(TaskInfo taskInfo, ProcessDefinition processDefinition) {
        initializeTaskDetails(taskInfo);

        if (processDefinition != null) {
            this.processDefinitionName = processDefinition.getName();
            this.processDefinitionDescription = processDefinition.getDescription();
            this.processDefinitionKey = processDefinition.getKey();
            this.processDefinitionCategory = processDefinition.getCategory();
            this.processDefinitionVersion = processDefinition.getVersion();
            this.processDefinitionDeploymentId = processDefinition.getDeploymentId();
        }
    }

    public TaskRepresentation(TaskInfo taskInfo, CaseDefinition caseDefinition) {
        initializeTaskDetails(taskInfo);

        if (caseDefinition != null) {
            this.caseDefinitionName = caseDefinition.getName();
            this.caseDefinitionDescription = caseDefinition.getDescription();
            this.caseDefinitionKey = caseDefinition.getKey();
            this.caseDefinitionCategory = caseDefinition.getCategory();
            this.caseDefinitionVersion = caseDefinition.getVersion();
            this.caseDefinitionDeploymentId = caseDefinition.getDeploymentId();
        }
    }

    public TaskRepresentation(TaskInfo taskInfo, TaskInfo parentTaskInfo) {
        initializeTaskDetails(taskInfo);

        if (parentTaskInfo != null) {
            this.parentTaskId = parentTaskInfo.getId();
            this.parentTaskName = parentTaskInfo.getName();
        }
    }

    public TaskRepresentation(TaskInfo taskInfo, ProcessDefinition processDefinition, String processInstanceName) {
        this(taskInfo, processDefinition);
        this.processInstanceName = processInstanceName;
    }

    public TaskRepresentation(TaskInfo taskInfo, CaseDefinition caseDefinition, String caseInstanceName) {
        this(taskInfo, caseDefinition);
        this.caseInstanceName = caseInstanceName;
    }

    public void initializeTaskDetails(TaskInfo taskInfo) {
        this.id = taskInfo.getId();
        this.name = taskInfo.getName();
        this.description = taskInfo.getDescription();
        this.category = taskInfo.getCategory();
        this.created = taskInfo.getCreateTime();
        this.dueDate = taskInfo.getDueDate();
        this.priority = taskInfo.getPriority();
        this.processInstanceId = taskInfo.getProcessInstanceId();
        this.processDefinitionId = taskInfo.getProcessDefinitionId();
        this.scopeId = taskInfo.getScopeId();
        this.scopeType = taskInfo.getScopeType();
        this.scopeDefinitionId = taskInfo.getScopeDefinitionId();

        if (taskInfo instanceof HistoricTaskInstance) {
            this.endDate = ((HistoricTaskInstance) taskInfo).getEndTime();
            this.formKey = taskInfo.getFormKey();
            this.duration = ((HistoricTaskInstance) taskInfo).getDurationInMillis();
        } else {
            // Rendering of forms for historic tasks not supported currently
            this.formKey = taskInfo.getFormKey();
        }
    }

    public void fillTask(Task task) {
        task.setName(name);
        task.setDescription(description);
        if (assignee != null && assignee.getId() != null) {
            task.setAssignee(String.valueOf(assignee.getId()));
        }
        task.setDueDate(dueDate);
        if (priority != null) {
            task.setPriority(priority);
        }
        task.setCategory(category);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public UserRepresentation getAssignee() {
        return assignee;
    }

    public void setAssignee(UserRepresentation assignee) {
        this.assignee = assignee;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getProcessInstanceName() {
        return processInstanceName;
    }

    public void setProcessInstanceName(String processInstanceName) {
        this.processInstanceName = processInstanceName;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public void setProcessDefinitionName(String processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
    }

    public String getProcessDefinitionDescription() {
        return processDefinitionDescription;
    }

    public void setProcessDefinitionDescription(String processDefinitionDescription) {
        this.processDefinitionDescription = processDefinitionDescription;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public String getProcessDefinitionCategory() {
        return processDefinitionCategory;
    }

    public void setProcessDefinitionCategory(String processDefinitionCategory) {
        this.processDefinitionCategory = processDefinitionCategory;
    }

    public int getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public void setProcessDefinitionVersion(int processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
    }

    public String getProcessDefinitionDeploymentId() {
        return processDefinitionDeploymentId;
    }

    public void setProcessDefinitionDeploymentId(String processDefinitionDeploymentId) {
        this.processDefinitionDeploymentId = processDefinitionDeploymentId;
    }

    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public String getCaseInstanceName() {
        return caseInstanceName;
    }

    public void setCaseInstanceName(String caseInstanceName) {
        this.caseInstanceName = caseInstanceName;
    }

    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    public void setScopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
    }

    public String getCaseDefinitionName() {
        return caseDefinitionName;
    }

    public void setCaseDefinitionName(String caseDefinitionName) {
        this.caseDefinitionName = caseDefinitionName;
    }

    public String getCaseDefinitionDescription() {
        return caseDefinitionDescription;
    }

    public void setCaseDefinitionDescription(String caseDefinitionDescription) {
        this.caseDefinitionDescription = caseDefinitionDescription;
    }

    public String getCaseDefinitionKey() {
        return caseDefinitionKey;
    }

    public void setCaseDefinitionKey(String caseDefinitionKey) {
        this.caseDefinitionKey = caseDefinitionKey;
    }

    public String getCaseDefinitionCategory() {
        return caseDefinitionCategory;
    }

    public void setCaseDefinitionCategory(String caseDefinitionCategory) {
        this.caseDefinitionCategory = caseDefinitionCategory;
    }

    public int getCaseDefinitionVersion() {
        return caseDefinitionVersion;
    }

    public void setCaseDefinitionVersion(int caseDefinitionVersion) {
        this.caseDefinitionVersion = caseDefinitionVersion;
    }

    public String getCaseDefinitionDeploymentId() {
        return caseDefinitionDeploymentId;
    }

    public void setCaseDefinitionDeploymentId(String caseDefinitionDeploymentId) {
        this.caseDefinitionDeploymentId = caseDefinitionDeploymentId;
    }

    public String getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(String parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public String getParentTaskName() {
        return parentTaskName;
    }

    public void setParentTaskName(String parentTaskName) {
        this.parentTaskName = parentTaskName;
    }

    public String getFormKey() {
        return formKey;
    }

    public void setFormKey(String formKey) {
        this.formKey = formKey;
    }

    public String getProcessInstanceStartUserId() {
        return processInstanceStartUserId;
    }

    public void setProcessInstanceStartUserId(String processInstanceStartUserId) {
        this.processInstanceStartUserId = processInstanceStartUserId;
    }

    public boolean isInitiatorCanCompleteTask() {
        return initiatorCanCompleteTask;
    }

    public void setInitiatorCanCompleteTask(boolean initiatorCanCompleteTask) {
        this.initiatorCanCompleteTask = initiatorCanCompleteTask;
    }

    public boolean isMemberOfCandidateGroup() {
        return isMemberOfCandidateGroup;
    }

    public void setMemberOfCandidateGroup(boolean isMemberOfCandidateGroup) {
        this.isMemberOfCandidateGroup = isMemberOfCandidateGroup;
    }

    public boolean isMemberOfCandidateUsers() {
        return isMemberOfCandidateUsers;
    }

    public void setMemberOfCandidateUsers(boolean isMemberOfCandidateUsers) {
        this.isMemberOfCandidateUsers = isMemberOfCandidateUsers;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public List<UserRepresentation> getInvolvedPeople() {
        return involvedPeople;
    }

    public void setInvolvedPeople(List<UserRepresentation> involvedPeople) {
        this.involvedPeople = involvedPeople;
    }
}
