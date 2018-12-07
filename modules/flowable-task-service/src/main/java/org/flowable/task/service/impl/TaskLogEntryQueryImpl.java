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
import java.util.List;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.task.api.TaskLogEntry;
import org.flowable.task.api.TaskLogEntryQuery;
import org.flowable.task.service.impl.util.CommandContextUtil;

/**
 * @author martin.grofcik
 */
public class TaskLogEntryQueryImpl extends AbstractQuery<TaskLogEntryQuery, TaskLogEntry> implements TaskLogEntryQuery {

    protected String taskId;
    protected String type;
    protected String userId;
    protected String processInstanceId;
    protected String processDefinitionId;
    protected String scopeId;
    protected String scopeDefinitionId;
    protected String subScopeId;
    protected String scopeType;
    protected Date fromDate;
    protected Date toDate;
    protected String tenantId;
    protected long fromLogNumber = -1;
    protected long toLogNumber = -1;

    public TaskLogEntryQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public TaskLogEntryQuery taskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    @Override
    public TaskLogEntryQuery type(String type) {
        this.type = type;
        return this;
    }

    @Override
    public TaskLogEntryQuery userId(String userId) {
        this.userId = userId;
        return this;
    }
    
    @Override
    public TaskLogEntryQuery processInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    @Override
    public TaskLogEntryQuery processDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    @Override
    public TaskLogEntryQuery scopeId(String scopeId) {
        this.scopeId = scopeId;
        return this;
    }
    
    @Override
    public TaskLogEntryQuery scopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
        return this;
    }

    @Override
    public TaskLogEntryQuery subScopeId(String subScopeId) {
        this.subScopeId = subScopeId;
        return this;
    }
    
    @Override
    public TaskLogEntryQuery scopeType(String scopeType) {
        this.scopeType = scopeType;
        return this;
    }
    
    @Override
    public TaskLogEntryQuery from(Date fromDate) {
        this.fromDate = fromDate;
        return this;
    }
    
    @Override
    public TaskLogEntryQuery to(Date toDate) {
        this.toDate = toDate;
        return this;
    }
    
    @Override
    public TaskLogEntryQuery tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }
    
    @Override
    public TaskLogEntryQuery fromLogNumber(long fromLogNumber) {
        this.fromLogNumber = fromLogNumber;
        return this;
    }
    
    @Override
    public TaskLogEntryQuery toLogNumber(long toLogNumber) {
        this.toLogNumber = toLogNumber;
        return this;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getType() {
        return type;
    }

    public String getUserId() {
        return userId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public String getScopeId() {
        return scopeId;
    }

    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    public String getSubScopeId() {
        return subScopeId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public String getTenantId() {
        return tenantId;
    }

    public long getFromLogNumber() {
        return fromLogNumber;
    }

    public long getToLogNumber() {
        return toLogNumber;
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getTaskLogEntryEntityManager(commandContext).findTaskLogEntriesCountByQueryCriteria(this);
    }

    @Override
    public List<TaskLogEntry> executeList(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getTaskLogEntryEntityManager(commandContext).findTaskLogEntriesByQueryCriteria(this);
    }

    @Override
    public TaskLogEntryQuery orderByLogNumber() {
        orderBy(TaskLogEntryQueryProperty.LOG_NUMBER);
        return this;
    }
    @Override
    public TaskLogEntryQuery orderByTimeStamp() {
        orderBy(TaskLogEntryQueryProperty.TIME_STAMP);
        return this;
    }
}
