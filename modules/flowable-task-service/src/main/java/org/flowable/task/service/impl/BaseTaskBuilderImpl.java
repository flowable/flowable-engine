package org.flowable.task.service.impl;

import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskBuilder;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

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
    protected int priority = Task.DEFAULT_PRIORITY;
    protected String ownerId;
    protected String assigneeId;
    protected Date dueDate;
    protected String category;
    protected String parentTaskId;
    protected String tenantId;
    protected String formKey;
    protected String taskDefinitionId;
    protected String taskDefinitionKey;
    protected String scopeId;
    protected String scopeType;
    protected Set<? extends IdentityLinkInfo> identityLinks = Collections.EMPTY_SET;

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

    @Override
    public TaskBuilder identityLinks(Set<? extends IdentityLinkInfo> identityLinks) {
        this.identityLinks = identityLinks;
        return this;
    }

    @Override
    public TaskBuilder scopeId(String scopeId) {
        this.scopeId = scopeId;
        return this;
    }

    @Override
    public TaskBuilder scopeType(String scopeType) {
        this.scopeType = scopeType;
        return this;
    }

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
    public String getTaskDefinitionId() {
        return taskDefinitionId;
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
    public Set<? extends IdentityLinkInfo> getIdentityLinks() {
        return identityLinks;
    }

    @Override
    public String getScopeId() {
        return this.scopeId;
    }

    @Override
    public String getScopeType() {
        return this.scopeType;
    }

}
