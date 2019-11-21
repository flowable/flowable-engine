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
package org.flowable.task.service.impl;

import java.util.Date;

import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;

/**
 * Base implementation of the {@link HistoricTaskLogEntryBuilder} interface
 *
 * @author martin.grofcik
 */
public class BaseHistoricTaskLogEntryBuilderImpl implements HistoricTaskLogEntryBuilder {

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

    public BaseHistoricTaskLogEntryBuilderImpl(TaskInfo task) {
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

    public BaseHistoricTaskLogEntryBuilderImpl() {
    }

    @Override
    public HistoricTaskLogEntryBuilder taskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    @Override
    public HistoricTaskLogEntryBuilder type(String type) {
        this.type = type;
        return this;
    }

    @Override
    public HistoricTaskLogEntryBuilder timeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
        return this;
    }

    @Override
    public HistoricTaskLogEntryBuilder userId(String userId) {
        this.userId = userId;
        return this;
    }

    @Override
    public HistoricTaskLogEntryBuilder data(String data) {
        this.data = data;
        return this;
    }

    @Override
    public HistoricTaskLogEntryBuilder processInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    @Override
    public HistoricTaskLogEntryBuilder processDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    @Override
    public HistoricTaskLogEntryBuilder executionId(String executionId) {
        this.executionId = executionId;
        return this;
    }

    @Override
    public HistoricTaskLogEntryBuilder scopeId(String scopeId) {
        this.scopeId = scopeId;
        return this;
    }

    @Override
    public HistoricTaskLogEntryBuilder scopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
        return this;
    }

    @Override
    public HistoricTaskLogEntryBuilder subScopeId(String subScopeId) {
        this.subScopeId = subScopeId;
        return this;
    }

    @Override
    public HistoricTaskLogEntryBuilder scopeType(String scopeType) {
        this.scopeType = scopeType;
        return this;
    }

    @Override
    public HistoricTaskLogEntryBuilder tenantId(String tenantId) {
        this.tenantId = tenantId;
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

    @Override
    public void create() {
        // add is not supported by default
        throw new RuntimeException("Operation is not supported");
    }
}
