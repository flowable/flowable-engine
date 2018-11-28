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

    protected TaskInfo task;
    protected String type;
    protected Date timeStamp;
    protected String userId;
    protected byte[] data;

    public BaseTaskLogEntryBuilderImpl(CommandExecutor commandExecutor, TaskInfo task) {
        this.commandExecutor = commandExecutor;
        this.task = task;
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
    public TaskLogEntryBuilder data(byte[] data) {
        this.data = data;
        return this;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getTaskId() {
        return task.getId();
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
    public byte[] getData() {
        return data;
    }

    @Override
    public String getExecutionId() {
        return task.getExecutionId();
    }

    @Override
    public String getProcessInstanceId() {
        return task.getProcessInstanceId();
    }

    @Override
    public String getScopeId() {
        return task.getScopeId();
    }

    @Override
    public String getSubScopeId() {
        return task.getSubScopeId();
    }

    @Override
    public String getScopeType() {
        return task.getScopeType();
    }

    @Override
    public String getTenantId() {
        return task.getTenantId();
    }
}
