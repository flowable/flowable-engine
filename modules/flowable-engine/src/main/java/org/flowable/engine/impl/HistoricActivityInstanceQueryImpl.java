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

package org.flowable.engine.impl;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.query.AbstractQuery;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.impl.cmd.DeleteHistoricActivityInstancesCmd;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Tom Baeyens
 * @author Zheng Ji
 */
public class HistoricActivityInstanceQueryImpl extends AbstractQuery<HistoricActivityInstanceQuery, HistoricActivityInstance> implements HistoricActivityInstanceQuery {

    private static final long serialVersionUID = 1L;
    protected String activityInstanceId;
    protected String processInstanceId;
    protected Set<String> processInstanceIds;
    private List<List<String>> safeProcessInstanceIds;
    protected String executionId;
    protected String processDefinitionId;
    protected String activityId;
    protected String activityName;
    protected String activityType;
    protected Set<String> activityTypes;
    protected String assignee;
    protected String completedBy;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;
    protected boolean finished;
    protected boolean unfinished;
    protected String deleteReason;
    protected String deleteReasonLike;
    protected Date startedBefore;
    protected Date startedAfter;
    protected Date finishedBefore;
    protected Date finishedAfter;
    protected List<String> tenantIds;
    protected Set<String> calledProcessInstanceIds;
    private List<List<String>> safeCalledProcessInstanceIds;

    public HistoricActivityInstanceQueryImpl() {
    }

    public HistoricActivityInstanceQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public HistoricActivityInstanceQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        return CommandContextUtil.getHistoricActivityInstanceEntityManager(commandContext).findHistoricActivityInstanceCountByQueryCriteria(this);
    }

    @Override
    public List<HistoricActivityInstance> executeList(CommandContext commandContext) {
        return CommandContextUtil.getHistoricActivityInstanceEntityManager(commandContext).findHistoricActivityInstancesByQueryCriteria(this);
    }

    @Override
    public HistoricActivityInstanceQueryImpl processInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    @Override
    public HistoricActivityInstanceQueryImpl processInstanceIds(Set<String> processInstanceIds) {
        if (processInstanceIds == null) {
            throw new FlowableIllegalArgumentException("Set of process instance ids is null");
        }
        if (processInstanceIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("Set of process instance ids is empty");
        }
        this.processInstanceIds = processInstanceIds;
        return this;
    }

    @Override
    public HistoricActivityInstanceQueryImpl executionId(String executionId) {
        this.executionId = executionId;
        return this;
    }

    @Override
    public HistoricActivityInstanceQueryImpl processDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    @Override
    public HistoricActivityInstanceQueryImpl activityId(String activityId) {
        this.activityId = activityId;
        return this;
    }

    @Override
    public HistoricActivityInstanceQueryImpl activityName(String activityName) {
        this.activityName = activityName;
        return this;
    }

    @Override
    public HistoricActivityInstanceQueryImpl activityType(String activityType) {
        this.activityType = activityType;
        return this;
    }
    @Override
    public HistoricActivityInstanceQueryImpl startedAfter(Date date) {
        this.startedAfter = date;
        return this;
    }
    @Override
    public HistoricActivityInstanceQueryImpl startedBefore(Date date) {
        this.startedBefore = date;
        return this;
    }
    @Override
    public HistoricActivityInstanceQueryImpl finishedAfter(Date date) {
        this.finishedAfter = date;
        return this;
    }
    @Override
    public HistoricActivityInstanceQueryImpl finishedBefore(Date date) {
        this.finishedBefore = date;
        return this;
    }

    @Override
    public HistoricActivityInstanceQuery activityTypes(Set<String> activityTypes) {
        this.activityTypes=activityTypes;
        return this;
    }

    @Override
    public HistoricActivityInstanceQuery calledProcessInstanceIds(Set<String> calledProcessInstanceIds) {
        if (calledProcessInstanceIds == null) {
            throw new FlowableIllegalArgumentException("Set of called process instance ids is null");
        }
        if (calledProcessInstanceIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("Set of called process instance ids is empty");
        }
        this.calledProcessInstanceIds = calledProcessInstanceIds;
        return this;
    }

    @Override
    public HistoricActivityInstanceQueryImpl taskAssignee(String assignee) {
        this.assignee = assignee;
        return this;
    }

    @Override
    public HistoricActivityInstanceQuery taskCompletedBy(String userId) {
        this.completedBy = userId;
        return this;
    }

    @Override
    public HistoricActivityInstanceQueryImpl finished() {
        this.finished = true;
        this.unfinished = false;
        return this;
    }

    @Override
    public HistoricActivityInstanceQueryImpl unfinished() {
        this.unfinished = true;
        this.finished = false;
        return this;
    }

    @Override
    public HistoricActivityInstanceQuery deleteReason(String deleteReason) {
        this.deleteReason = deleteReason;
        return this;
    }

    @Override
    public HistoricActivityInstanceQuery deleteReasonLike(String deleteReasonLike) {
        this.deleteReasonLike = deleteReasonLike;
        return this;
    }

    @Override
    public HistoricActivityInstanceQueryImpl activityTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("activity tenant id is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    public String getTenantId() {
        return tenantId;
    }

    @Override
    public HistoricActivityInstanceQueryImpl activityTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("activity tenant id is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    @Override
    public HistoricActivityInstanceQuery tenantIdIn(List<String> tenantIds) {
        this.tenantIds = tenantIds;
        return this;
    }

    public String getTenantIdLike() {
        return tenantIdLike;
    }

    @Override
    public HistoricActivityInstanceQueryImpl activityWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    public boolean isWithoutTenantId() {
        return withoutTenantId;
    }

    // ordering
    // /////////////////////////////////////////////////////////////////

    @Override
    public HistoricActivityInstanceQueryImpl orderByHistoricActivityInstanceDuration() {
        orderBy(HistoricActivityInstanceQueryProperty.DURATION);
        return this;
    }

    @Override
    public HistoricActivityInstanceQueryImpl orderByHistoricActivityInstanceEndTime() {
        orderBy(HistoricActivityInstanceQueryProperty.END);
        return this;
    }

    @Override
    public HistoricActivityInstanceQueryImpl orderByExecutionId() {
        orderBy(HistoricActivityInstanceQueryProperty.EXECUTION_ID);
        return this;
    }

    @Override
    public HistoricActivityInstanceQueryImpl orderByHistoricActivityInstanceId() {
        orderBy(HistoricActivityInstanceQueryProperty.HISTORIC_ACTIVITY_INSTANCE_ID);
        return this;
    }

    @Override
    public HistoricActivityInstanceQueryImpl orderByProcessDefinitionId() {
        orderBy(HistoricActivityInstanceQueryProperty.PROCESS_DEFINITION_ID);
        return this;
    }

    @Override
    public HistoricActivityInstanceQueryImpl orderByProcessInstanceId() {
        orderBy(HistoricActivityInstanceQueryProperty.PROCESS_INSTANCE_ID);
        return this;
    }

    @Override
    public HistoricActivityInstanceQueryImpl orderByHistoricActivityInstanceStartTime() {
        orderBy(HistoricActivityInstanceQueryProperty.START);
        return this;
    }

    @Override
    public HistoricActivityInstanceQuery orderByActivityId() {
        orderBy(HistoricActivityInstanceQueryProperty.ACTIVITY_ID);
        return this;
    }

    @Override
    public HistoricActivityInstanceQueryImpl orderByActivityName() {
        orderBy(HistoricActivityInstanceQueryProperty.ACTIVITY_NAME);
        return this;
    }

    @Override
    public HistoricActivityInstanceQueryImpl orderByActivityType() {
        orderBy(HistoricActivityInstanceQueryProperty.ACTIVITY_TYPE);
        return this;
    }

    @Override
    public HistoricActivityInstanceQueryImpl orderByTenantId() {
        orderBy(HistoricActivityInstanceQueryProperty.TENANT_ID);
        return this;
    }

    @Override
    public HistoricActivityInstanceQueryImpl activityInstanceId(String activityInstanceId) {
        this.activityInstanceId = activityInstanceId;
        return this;
    }

    @Override
    public void delete() {
        if (commandExecutor != null) {
            commandExecutor.execute(new DeleteHistoricActivityInstancesCmd(this));
        } else {
            new DeleteHistoricActivityInstancesCmd(this).execute(Context.getCommandContext());
        }
    }

    @Override
    @Deprecated
    public void deleteWithRelatedData() {
        delete();
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public String getActivityId() {
        return activityId;
    }

    public String getActivityName() {
        return activityName;
    }

    public String getActivityType() {
        return activityType;
    }

    public Set<String> getActivityTypes() {
        return activityTypes;
    }

    public String getAssignee() {
        return assignee;
    }

    public String getCompletedBy() {
        return completedBy;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isUnfinished() {
        return unfinished;
    }

    public String getActivityInstanceId() {
        return activityInstanceId;
    }

    public String getDeleteReason() {
        return deleteReason;
    }

    public String getDeleteReasonLike() {
        return deleteReasonLike;
    }
    
    public Date getStartedAfter() {
        return startedAfter;
    }
    
    public Date getStartedBefore() {
        return startedBefore;
    }
    
    public Date getFinishedAfter() {
        return finishedAfter;
    }
    
    public Date getFinishedBefore() {
        return finishedBefore;
    }
    
    public List<String> getTenantIds() {
        return tenantIds;
    }

    public List<List<String>> getSafeProcessInstanceIds() {
        return safeProcessInstanceIds;
    }

    public void setSafeProcessInstanceIds(List<List<String>> safeProcessInstanceIds) {
        this.safeProcessInstanceIds = safeProcessInstanceIds;
    }

    public Collection<String> getProcessInstanceIds() {
        return processInstanceIds;
    }

    public Set<String> getCalledProcessInstanceIds() {
        return calledProcessInstanceIds;
    }

    public void setCalledProcessInstanceIds(Set<String> calledProcessInstanceIds) {
        this.calledProcessInstanceIds = calledProcessInstanceIds;
    }

    public List<List<String>> getSafeCalledProcessInstanceIds() {
        return safeCalledProcessInstanceIds;
    }

    public void setSafeCalledProcessInstanceIds(List<List<String>> safeCalledProcessInstanceIds) {
        this.safeCalledProcessInstanceIds = safeCalledProcessInstanceIds;
    }
}
