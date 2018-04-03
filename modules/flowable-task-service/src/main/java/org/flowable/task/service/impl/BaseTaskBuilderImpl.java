package org.flowable.task.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.flowable.engine.common.impl.interceptor.CommandExecutor;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskBuilder;
import org.flowable.task.api.TaskInfo;

/**
 * Base implementation of the {@link TaskBuilder} interface
 * 
 * @author martin.grofcik
 */
public abstract class BaseTaskBuilderImpl implements TaskBuilder {
    protected CommandExecutor commandExecutor;
    protected String id;
    protected String name;
    protected String description;
    protected int priority;
    protected String ownerId;
    protected String assigneeId;
    protected Date dueDate;
    protected String category;
    protected String parentTaskId;
    protected String tenantId;
    protected String formKey;
    protected String taskDefinitionId;
    protected String taskDefinitionKey;

    public BaseTaskBuilderImpl(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    public abstract Task create();

    @Override
    public TaskBuilder id(String id) {
        this.id = id;
        return this;
    }

    @Override
    public TaskBuilder name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public TaskBuilder description(String description) {
        this.description = description;
        return this;
    }

    @Override
    public TaskBuilder priority(int priority) {
        this.priority = priority;
        return this;
    }

    @Override
    public TaskBuilder owner(String ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    @Override
    public TaskBuilder assignee(String assigneId) {
        this.assigneeId = assigneId;
        return this;
    }

    @Override
    public TaskBuilder dueDate(Date dueDate) {
        this.dueDate = dueDate;
        return this;
    }

    @Override
    public TaskBuilder category(String category) {
        this.category = category;
        return this;
    }

    @Override
    public TaskBuilder parentTaskId(String parentTaskId) {
        this.parentTaskId = parentTaskId;
        return this;
    }

    @Override
    public TaskBuilder tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public TaskBuilder formKey(String formKey) {
        this.formKey = formKey;
        return this;
    }

    @Override
    public TaskBuilder taskDefinitionId(String taskDefinitionId) {
        this.taskDefinitionId = taskDefinitionId;
        return this;
    }

    @Override
    public TaskBuilder taskDefinitionKey(String taskDefinitionKey) {
        this.taskDefinitionKey = taskDefinitionKey;
        return this;
    }

    public class TemplateTaskInfo implements TaskInfo {
        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public String getOwner() {
            return ownerId;
        }

        @Override
        public String getAssignee() {
            return assigneeId;
        }

        @Override
        public String getProcessInstanceId() {
            return null;
        }

        @Override
        public String getExecutionId() {
            return null;
        }

        @Override
        public String getTaskDefinitionId() {
            return taskDefinitionId;
        }

        @Override
        public String getProcessDefinitionId() {
            return null;
        }

        @Override
        public String getScopeId() {
            return null;
        }

        @Override
        public String getSubScopeId() {
            return null;
        }

        @Override
        public String getScopeType() {
            return null;
        }

        @Override
        public String getScopeDefinitionId() {
            return null;
        }

        @Override
        public Date getCreateTime() {
            return null;
        }

        @Override
        public String getTaskDefinitionKey() {
            return taskDefinitionKey;
        }

        @Override
        public Date getDueDate() {
            return dueDate;
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public String getParentTaskId() {
            return parentTaskId;
        }

        @Override
        public String getTenantId() {
            return tenantId;
        }

        @Override
        public String getFormKey() {
            return formKey;
        }

        @Override
        public Map<String, Object> getTaskLocalVariables() {
            return null;
        }

        @Override
        public Map<String, Object> getProcessVariables() {
            return null;
        }

        @Override
        public List<? extends IdentityLinkInfo> getIdentityLinks() {
            return Collections.EMPTY_LIST;
        }

        @Override
        public Date getClaimTime() {
            return null;
        }
    }
}
