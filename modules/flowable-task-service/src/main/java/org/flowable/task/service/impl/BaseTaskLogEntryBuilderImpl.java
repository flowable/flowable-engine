package org.flowable.task.service.impl;

import java.util.Date;

import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.TaskLogEntryBuilder;

/**
 * Base implementation of the {@link TaskLogEntryBuilder} interface
 *
 * @author martin.grofcik
 */
public abstract class BaseTaskLogEntryBuilderImpl implements TaskLogEntryBuilder {
    protected CommandExecutor commandExecutor;

    protected String type;
    protected Date timeStamp;
    protected String userId;
    protected String data;
    protected String processInstanceId;
    protected String processDefinitionId;
    protected String executionId;
    protected String scopeId;
    protected String scopeDefinitionId;
    protected String subScopeId;
    protected String scopeType;
    protected String tenantId;
    protected String taskId;

    public BaseTaskLogEntryBuilderImpl(CommandExecutor commandExecutor, TaskInfo task) {
        this(commandExecutor);
        this.processInstanceId = task.getProcessInstanceId();
        this.processDefinitionId = task.getProcessDefinitionId();
        this.executionId = task.getExecutionId();
        this.tenantId = task.getTenantId();
        this.scopeId = task.getScopeId();
        this.scopeDefinitionId = task.getScopeDefinitionId();
        this.subScopeId = task.getSubScopeId();
        this.scopeType = task.getScopeType();
        this.taskId = task.getId();
    }

    public BaseTaskLogEntryBuilderImpl(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    public TaskLogEntryBuilder taskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    @Override
    public TaskLogEntryBuilder type(String type) {
        this.type = type;
        return this;
    }

    @Override
    public TaskLogEntryBuilder timeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
        return this;
    }

    @Override
    public TaskLogEntryBuilder userId(String userId) {
        this.userId = userId;
        return this;
    }

    @Override
    public TaskLogEntryBuilder data(String data) {
        this.data = data;
        return this;
    }

    @Override
    public TaskLogEntryBuilder processInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    @Override
    public TaskLogEntryBuilder processDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    @Override
    public TaskLogEntryBuilder scopeId(String scopeId) {
        this.scopeId = scopeId;
        return this;
    }

    @Override
    public TaskLogEntryBuilder scopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
        return this;
    }

    @Override
    public TaskLogEntryBuilder subScopeId(String subScopeId) {
        this.subScopeId = subScopeId;
        return this;
    }

    @Override
    public TaskLogEntryBuilder scopeType(String scopeType) {
        this.scopeType = scopeType;
        return this;
    }

    @Override
    public TaskLogEntryBuilder tenantId(String tenantId) {
        return this;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getTaskId() {
        return taskId;
    }
    @Override
    public Date getTimeStamp() {
        return timeStamp;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getData() {
        return data;
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
    public String getScopeId() {
        return scopeId;
    }

    @Override
    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    @Override
    public String getSubScopeId() {
        return subScopeId;
    }

    @Override
    public String getScopeType() {
        return scopeType;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }
}
