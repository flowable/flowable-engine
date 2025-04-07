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
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.query.AbstractQuery;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.ActivityInstanceQuery;

/**
 * @author martin.grofcik
 */
public class ActivityInstanceQueryImpl extends AbstractQuery<ActivityInstanceQuery, ActivityInstance> implements ActivityInstanceQuery {

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
    protected String assignee;
    protected String completedBy;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;
    protected boolean finished;
    protected boolean unfinished;
    protected String deleteReason;
    protected String deleteReasonLike;

    public ActivityInstanceQueryImpl() {
    }

    public ActivityInstanceQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public ActivityInstanceQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        return CommandContextUtil.getActivityInstanceEntityManager(commandContext).findActivityInstanceCountByQueryCriteria(this);
    }

    @Override
    public List<ActivityInstance> executeList(CommandContext commandContext) {
        return CommandContextUtil.getActivityInstanceEntityManager(commandContext).findActivityInstancesByQueryCriteria(this);
    }

    @Override
    public ActivityInstanceQueryImpl processInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    @Override
    public ActivityInstanceQuery processInstanceIds(Set<String> processInstanceIds) {
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
    public ActivityInstanceQueryImpl executionId(String executionId) {
        this.executionId = executionId;
        return this;
    }

    @Override
    public ActivityInstanceQueryImpl processDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    @Override
    public ActivityInstanceQueryImpl activityId(String activityId) {
        this.activityId = activityId;
        return this;
    }

    @Override
    public ActivityInstanceQueryImpl activityName(String activityName) {
        this.activityName = activityName;
        return this;
    }

    @Override
    public ActivityInstanceQueryImpl activityType(String activityType) {
        this.activityType = activityType;
        return this;
    }

    @Override
    public ActivityInstanceQueryImpl taskAssignee(String assignee) {
        this.assignee = assignee;
        return this;
    }

    @Override
    public ActivityInstanceQuery taskCompletedBy(String userId) {
        this.completedBy = userId;
        return this;
    }

    @Override
    public ActivityInstanceQueryImpl finished() {
        this.finished = true;
        this.unfinished = false;
        return this;
    }

    @Override
    public ActivityInstanceQueryImpl unfinished() {
        this.unfinished = true;
        this.finished = false;
        return this;
    }

    @Override
    public ActivityInstanceQuery deleteReason(String deleteReason) {
        this.deleteReason = deleteReason;
        return this;
    }

    @Override
    public ActivityInstanceQuery deleteReasonLike(String deleteReasonLike) {
        this.deleteReasonLike = deleteReasonLike;
        return this;
    }

    @Override
    public ActivityInstanceQueryImpl activityTenantId(String tenantId) {
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
    public ActivityInstanceQueryImpl activityTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("activity tenant id is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    public String getTenantIdLike() {
        return tenantIdLike;
    }

    @Override
    public ActivityInstanceQueryImpl activityWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    public boolean isWithoutTenantId() {
        return withoutTenantId;
    }

    // ordering
    // /////////////////////////////////////////////////////////////////

    @Override
    public ActivityInstanceQueryImpl orderByActivityInstanceDuration() {
        orderBy(ActivityInstanceQueryProperty.DURATION);
        return this;
    }

    @Override
    public ActivityInstanceQueryImpl orderByActivityInstanceEndTime() {
        orderBy(ActivityInstanceQueryProperty.END);
        return this;
    }

    @Override
    public ActivityInstanceQueryImpl orderByExecutionId() {
        orderBy(ActivityInstanceQueryProperty.EXECUTION_ID);
        return this;
    }

    @Override
    public ActivityInstanceQueryImpl orderByActivityInstanceId() {
        orderBy(ActivityInstanceQueryProperty.ACTIVITY_INSTANCE_ID);
        return this;
    }

    @Override
    public ActivityInstanceQueryImpl orderByProcessDefinitionId() {
        orderBy(ActivityInstanceQueryProperty.PROCESS_DEFINITION_ID);
        return this;
    }

    @Override
    public ActivityInstanceQueryImpl orderByProcessInstanceId() {
        orderBy(ActivityInstanceQueryProperty.PROCESS_INSTANCE_ID);
        return this;
    }

    @Override
    public ActivityInstanceQueryImpl orderByActivityInstanceStartTime() {
        orderBy(ActivityInstanceQueryProperty.START);
        return this;
    }

    @Override
    public ActivityInstanceQuery orderByActivityId() {
        orderBy(ActivityInstanceQueryProperty.ACTIVITY_ID);
        return this;
    }

    @Override
    public ActivityInstanceQueryImpl orderByActivityName() {
        orderBy(ActivityInstanceQueryProperty.ACTIVITY_NAME);
        return this;
    }

    @Override
    public ActivityInstanceQueryImpl orderByActivityType() {
        orderBy(ActivityInstanceQueryProperty.ACTIVITY_TYPE);
        return this;
    }

    @Override
    public ActivityInstanceQueryImpl orderByTenantId() {
        orderBy(ActivityInstanceQueryProperty.TENANT_ID);
        return this;
    }

    @Override
    public ActivityInstanceQueryImpl activityInstanceId(String activityInstanceId) {
        this.activityInstanceId = activityInstanceId;
        return this;
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

    public List<List<String>> getSafeProcessInstanceIds() {
        return safeProcessInstanceIds;
    }

    public void setSafeProcessInstanceIds(List<List<String>> safeProcessInstanceIds) {
        this.safeProcessInstanceIds = safeProcessInstanceIds;
    }

    public Collection<String> getProcessInstanceIds() {
        return processInstanceIds;
    }
}
