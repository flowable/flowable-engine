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

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstanceQuery;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.runtime.PlanItemInstanceQueryProperty;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.engine.common.impl.AbstractQuery;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.common.impl.interceptor.CommandExecutor;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;

import java.util.Date;
import java.util.List;

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
    protected Date startedAfter;
    protected Date startedBefore;
    protected Date endedAfter;
    protected Date endedBefore;
    protected String startUserId;
    protected String referenceId;
    protected String referenceType;
    protected String tenantId = CmmnEngineConfiguration.NO_TENANT_ID;
    protected boolean includePlanItemVariables;

    protected List<HistoricVariableInstanceEntity> queryVariable;


    public HistoricPlanItemInstanceQueryImpl() {

    }

    public HistoricPlanItemInstanceQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public HistoricPlanItemInstanceQuery withId(String planItemInstanceId) {
        this.planItemInstanceId = planItemInstanceId;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery withName(String planItemInstanceName) {
        this.planItemInstanceName = planItemInstanceName;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery withState(String state) {
        this.state = state;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery withCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery withCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery withStageInstanceId(String stageInstanceId) {
        this.stageInstanceId = stageInstanceId;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery withElementId(String elementId) {
        this.elementId = elementId;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery withDefinitionId(String planItemDefinitionId) {
        this.planItemDefinitionId = planItemDefinitionId;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery withDefinitionType(String planItemDefinitionType) {
        this.planItemDefinitionType = planItemDefinitionType;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery startedBefore(Date startedBefore) {
        this.startedBefore = startedBefore;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery startedAfter(Date startedAfter) {
        this.startedAfter = startedAfter;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery withStartUserId(String startUserId) {
        this.startUserId = startUserId;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery withReferenceId(String referenceId) {
        this.referenceId = referenceId;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery withReferenceType(String referenceType) {
        this.referenceType = referenceType;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery withTenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery withoutTenantId() {
        this.tenantId = CmmnEngineConfiguration.NO_TENANT_ID;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery endedBefore(Date beforeTime) {
        this.endedBefore = beforeTime;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery endedAfter(Date afterTime) {
        this.endedAfter = afterTime;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery includeInstanceVariables() {
        this.includePlanItemVariables = true;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery orderByStartTime() {
        return orderBy(PlanItemInstanceQueryProperty.START_TIME);

    }

    @Override
    public HistoricPlanItemInstanceQuery orderByName() {
        return orderBy(PlanItemInstanceQueryProperty.NAME);
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        return CommandContextUtil.getHistoricPlanItemInstanceEntityManager(commandContext).countByCriteria(this);
    }

    @Override
    public List<HistoricPlanItemInstance> executeList(CommandContext commandContext) {
        if (includePlanItemVariables) {
            return CommandContextUtil.getHistoricPlanItemInstanceEntityManager(commandContext).findByCriteriaWithVariables(this);
        } else {
            return CommandContextUtil.getHistoricPlanItemInstanceEntityManager(commandContext).findByCriteria(this);
        }
    }
}
