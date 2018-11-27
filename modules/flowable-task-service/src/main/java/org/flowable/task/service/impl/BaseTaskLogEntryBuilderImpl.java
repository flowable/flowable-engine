package org.flowable.task.service.impl;

import java.util.Date;

import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.task.api.TaskLogEntryBuilder;

/**
 * Base implementation of the {@link TaskLogEntryBuilder} interface
 *
 * @author martin.grofcik
 */
public abstract class BaseTaskLogEntryBuilderImpl implements TaskLogEntryBuilder {
    protected CommandExecutor commandExecutor;

    protected String type;
    protected String taskId;
    protected Date timeStamp;
    protected String userId;
    protected byte[] data;

    public BaseTaskLogEntryBuilderImpl(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    public TaskLogEntryBuilder type(String type) {
        this.type = type;
        return this;
    }
    @Override
    public TaskLogEntryBuilder taskId(String taskId) {
        this.taskId = taskId;
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
        return this.type;
    }

    @Override
    public String getTaskId() {
        return this.taskId;
    }
    @Override
    public Date getTimeStamp() {
        return this.timeStamp;
    }

    @Override
    public String getUserId() {
        return this.userId;
    }

    @Override
    public byte[] getData() {
        return this.data;
    }
}
