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
package org.flowable.cmmn.engine.impl.history;

import java.util.Date;
import java.util.List;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.history. HistoricPlanItemInstanceQuery;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.AbstractQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;

/**
 * @author Dennis Federico
 */
public class HistoricPlanItemInstanceQueryImpl extends AbstractQuery<HistoricPlanItemInstanceQuery, HistoricPlanItemInstance> implements HistoricPlanItemInstanceQuery {

    private static final long serialVersionUID = 1L;

    protected String planItemInstanceId;
    protected String planItemInstanceName;
    protected String state;
    protected String caseDefinitionId;
    protected String caseInstanceId;
    protected String stageInstanceId;
    protected String elementId;
    protected String planItemDefinitionId;
    protected String planItemDefinitionType;
    protected Date createdBefore;
    protected Date createdAfter;
    protected Date lastAvailableBefore;
    protected Date lastAvailableAfter;
    protected Date lastEnabledBefore;
    protected Date lastEnabledAfter;
    protected Date lastDisabledBefore;
    protected Date lastDisabledAfter;
    protected Date lastStartedBefore;
    protected Date lastStartedAfter;
    protected Date lastSuspendedBefore;
    protected Date lastSuspendedAfter;
    protected Date completedBefore;
    protected Date completedAfter;
    protected Date terminatedBefore;
    protected Date terminatedAfter;
    protected Date occurredBefore;
    protected Date occurredAfter;
    protected Date exitBefore;
    protected Date exitAfter;
    protected Date endedBefore;
    protected Date endedAfter;
    protected String startUserId;
    protected String referenceId;
    protected String referenceType;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;

    public HistoricPlanItemInstanceQueryImpl() {

    }

    public HistoricPlanItemInstanceQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceId(String planItemInstanceId) {
        this.planItemInstanceId = planItemInstanceId;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceName(String planItemInstanceName) {
        this.planItemInstanceName = planItemInstanceName;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceState(String state) {
        this.state = state;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceStageInstanceId(String stageInstanceId) {
        this.stageInstanceId = stageInstanceId;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceElementId(String elementId) {
        this.elementId = elementId;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceDefinitionId(String planItemDefinitionId) {
        this.planItemDefinitionId = planItemDefinitionId;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceDefinitionType(String planItemDefinitionType) {
        this.planItemDefinitionType = planItemDefinitionType;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceStartUserId(String startUserId) {
        this.startUserId = startUserId;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceReferenceId(String referenceId) {
        this.referenceId = referenceId;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceReferenceType(String referenceType) {
        this.referenceType = referenceType;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceTenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }
    
    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("tenant id is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery createdBefore(Date createdBefore) {
        this.createdBefore = createdBefore;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery createdAfter(Date createdAfter) {
        this.createdAfter = createdAfter;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery lastAvailableBefore(Date lastAvailableBefore) {
        this.lastAvailableBefore = lastAvailableBefore;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery lastAvailableAfter(Date lastAvailableAfter) {
        this.lastAvailableAfter = lastAvailableAfter;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery lastEnabledBefore(Date lastEnabledBefore) {
        this.lastEnabledBefore = lastEnabledBefore;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery lastEnabledAfter(Date lastEnabledAfter) {
        this.lastEnabledAfter = lastEnabledAfter;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery lastDisabledBefore(Date lastDisabledBefore) {
        this.lastDisabledBefore = lastDisabledBefore;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery lastDisabledAfter(Date lastDisabledAfter) {
        this.lastDisabledAfter = lastDisabledAfter;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery lastStartedBefore(Date lastStartedBefore) {
        this.lastStartedBefore = lastStartedBefore;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery lastStartedAfter(Date lastStartedAfter) {
        this.lastStartedAfter = lastStartedAfter;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery lastSuspendedBefore(Date lastSuspendedBefore) {
        this.lastSuspendedBefore = lastSuspendedBefore;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery lastSuspendedAfter(Date lastSuspendedAfter) {
        this.lastSuspendedAfter = lastSuspendedAfter;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery completedBefore(Date completedBefore) {
        this.completedBefore = completedBefore;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery completedAfter(Date completedAfter) {
        this.completedAfter = completedAfter;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery occurredBefore(Date occurredBefore) {
        this.occurredBefore = occurredBefore;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery occurredAfter(Date occurredAfter) {
        this.occurredAfter = occurredAfter;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery terminatedBefore(Date terminatedBefore) {
        this.terminatedBefore = terminatedBefore;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery terminatedAfter(Date terminatedAfter) {
        this.terminatedAfter = terminatedAfter;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery exitBefore(Date exitBefore) {
        this.exitBefore = exitBefore;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery exitAfter(Date exitAfter) {
        this.exitAfter = exitAfter;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery endedBefore(Date endedBefore) {
        this.endedBefore = endedBefore;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery endedAfter(Date endedAfter) {
        this.endedAfter = endedAfter;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery orderByCreatedTime() {
        return orderBy(HistoricPlanItemInstanceQueryProperty.CREATED_TIME);

    }

    @Override
    public HistoricPlanItemInstanceQuery orderByEndedTime() {
        return orderBy(HistoricPlanItemInstanceQueryProperty.ENDED_TIME);

    }

    @Override
    public HistoricPlanItemInstanceQuery orderByName() {
        return orderBy(HistoricPlanItemInstanceQueryProperty.NAME);
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        return CommandContextUtil.getHistoricPlanItemInstanceEntityManager(commandContext).countByCriteria(this);
    }

    @Override
    public List<HistoricPlanItemInstance> executeList(CommandContext commandContext) {
        return CommandContextUtil.getHistoricPlanItemInstanceEntityManager(commandContext).findByCriteria(this);
    }

}
