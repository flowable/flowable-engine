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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstanceQuery;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.query.CacheAwareQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.query.AbstractQuery;

/**
 * @author Dennis Federico
 */
public class HistoricPlanItemInstanceQueryImpl extends AbstractQuery<HistoricPlanItemInstanceQuery, HistoricPlanItemInstance>
        implements HistoricPlanItemInstanceQuery, CacheAwareQuery<HistoricPlanItemInstanceEntity> {

    private static final long serialVersionUID = 1L;

    protected String planItemInstanceId;
    protected String planItemInstanceName;
    protected String state;
    protected String caseDefinitionId;
    protected String derivedCaseDefinitionId;
    protected String caseInstanceId;
    protected Set<String> caseInstanceIds;
    private List<List<String>> safeCaseInstanceIds;
    protected String stageInstanceId;
    protected String elementId;
    protected String planItemDefinitionId;
    protected String planItemDefinitionType;
    protected List<String> planItemDefinitionTypes;
    protected Date createdBefore;
    protected Date createdAfter;
    protected Date lastAvailableBefore;
    protected Date lastAvailableAfter;
    protected Date lastUnavailableBefore;
    protected Date lastUnavailableAfter;
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
    protected String assignee;
    protected String completedBy;
    protected String referenceId;
    protected String referenceType;
    protected boolean ended;
    protected boolean notEnded;
    protected String entryCriterionId;
    protected String exitCriterionId;
    protected String formKey;
    protected String extraValue;
    protected String involvedUser;
    protected Collection<String> involvedGroups;
    private List<List<String>> safeInvolvedGroups;
    protected boolean onlyStages;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;
    protected String locale;
    protected boolean withLocalizationFallback;
    protected boolean includeLocalVariables;

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
    public HistoricPlanItemInstanceQuery planItemInstanceDerivedCaseDefinitionId(String derivedCaseDefinitionId) {
        this.derivedCaseDefinitionId = derivedCaseDefinitionId;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceCaseInstanceIds(Set<String> caseInstanceIds) {
        if (caseInstanceIds == null) {
            throw new FlowableIllegalArgumentException("Set of case instance ids is null");
        }
        if (caseInstanceIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("Set of case instance ids is empty");
        }
        this.caseInstanceIds = caseInstanceIds;
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
        if (planItemDefinitionType == null) {
            throw new FlowableIllegalArgumentException("Plan item definition type is null");
        }
        this.planItemDefinitionType = planItemDefinitionType;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceDefinitionTypes(List<String> planItemDefinitionTypes) {
        if (planItemDefinitionTypes == null) {
            throw new FlowableIllegalArgumentException("Plan item definition types is null");
        }
        if (planItemDefinitionTypes.isEmpty()) {
            throw new FlowableIllegalArgumentException("Plan item definition types is empty");
        }
        this.planItemDefinitionTypes = planItemDefinitionTypes;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceStartUserId(String startUserId) {
        this.startUserId = startUserId;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceAssignee(String assignee) {
        this.assignee = assignee;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceCompletedBy(String completedBy) {
        this.completedBy = completedBy;
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
    public HistoricPlanItemInstanceQuery planItemInstanceEntryCriterionId(String entryCriterionId) {
        if (entryCriterionId == null) {
            throw new FlowableIllegalArgumentException("EntryCriterionId is null");
        }
        this.entryCriterionId = entryCriterionId;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceExitCriterionId(String exitCriterionId) {
        if (exitCriterionId == null) {
            throw new FlowableIllegalArgumentException("ExitCriterionId is null");
        }
        this.exitCriterionId = exitCriterionId;
        return this;
    }
    
    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceFormKey(String formKey) {
        if (formKey == null) {
            throw new FlowableIllegalArgumentException("formKey is null");
        }
        this.formKey = formKey;
        return this;
    }
    
    @Override
    public HistoricPlanItemInstanceQuery planItemInstanceExtraValue(String extraValue) {
        if (extraValue == null) {
            throw new FlowableIllegalArgumentException("extraValue is null");
        }
        this.extraValue = extraValue;
        return this;
    }
    
    @Override
    public HistoricPlanItemInstanceQuery involvedUser(String involvedUser) {
        if (involvedUser == null) {
            throw new FlowableIllegalArgumentException("involvedUser is null");
        }
        this.involvedUser = involvedUser;
        return this;
    }
    
    @Override
    public HistoricPlanItemInstanceQuery involvedGroups(Collection<String> involvedGroups) {
        if (involvedGroups == null) {
            throw new FlowableIllegalArgumentException("involvedGroups is null");
        }
        this.involvedGroups = involvedGroups;
        return this;
    }
    
    @Override
    public HistoricPlanItemInstanceQuery onlyStages() {
        this.onlyStages = true;
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
    public HistoricPlanItemInstanceQuery lastUnavailableAfter(Date unavailableAfter) {
        this.lastUnavailableAfter = unavailableAfter;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery lastUnavailableBefore(Date unavailableBefore) {
        this.lastUnavailableBefore = unavailableBefore;
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
    public HistoricPlanItemInstanceQuery lastStartedBefore(Date startedBefore) {
        this.lastStartedBefore = startedBefore;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery lastStartedAfter(Date startedAfter) {
        this.lastStartedAfter = startedAfter;
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
    public HistoricPlanItemInstanceQuery ended() {
        this.ended = true;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery notEnded() {
        this.notEnded = true;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery locale(String locale) {
        this.locale = locale;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery withLocalizationFallback() {
        this.withLocalizationFallback = true;
        return this;
    }

    @Override
    public HistoricPlanItemInstanceQuery includeLocalVariables() {
        this.includeLocalVariables = true;
        return this;
    }


    @Override
    public HistoricPlanItemInstanceQuery orderByCreateTime() {
        return orderBy(HistoricPlanItemInstanceQueryProperty.CREATE_TIME);
    }

    @Override
    public HistoricPlanItemInstanceQuery orderByEndedTime() {
        return orderBy(HistoricPlanItemInstanceQueryProperty.ENDED_TIME);
    }

    @Override
    public HistoricPlanItemInstanceQuery orderByLastAvailableTime() {
        return orderBy(HistoricPlanItemInstanceQueryProperty.LAST_AVAILABLE_TIME);
    }

    @Override
    public HistoricPlanItemInstanceQuery orderByLastEnabledTime() {
        return orderBy(HistoricPlanItemInstanceQueryProperty.LAST_ENABLED_TIME);
    }

    @Override
    public HistoricPlanItemInstanceQuery orderByLastDisabledTime() {
        return orderBy(HistoricPlanItemInstanceQueryProperty.LAST_DISABLED_TIME);
    }

    @Override
    public HistoricPlanItemInstanceQuery orderByLastStartedTime() {
        return orderBy(HistoricPlanItemInstanceQueryProperty.LAST_STARTED_TIME);
    }

    @Override
    public HistoricPlanItemInstanceQuery orderByLastSuspendedTime() {
        return orderBy(HistoricPlanItemInstanceQueryProperty.LAST_SUSPENDED_TIME);
    }

    @Override
    public HistoricPlanItemInstanceQuery orderByLastUpdatedTime() {
        return orderBy(HistoricPlanItemInstanceQueryProperty.LAST_UPDATED_TIME);
    }

    @Override
    public HistoricPlanItemInstanceQuery orderByCompletedTime() {
        return orderBy(HistoricPlanItemInstanceQueryProperty.COMPLETED_TIME);
    }

    @Override
    public HistoricPlanItemInstanceQuery orderByOccurredTime() {
        return orderBy(HistoricPlanItemInstanceQueryProperty.OCCURRED_TIME);
    }

    @Override
    public HistoricPlanItemInstanceQuery orderByTerminatedTime() {
        return orderBy(HistoricPlanItemInstanceQueryProperty.TERMINATED_TIME);
    }

    @Override
    public HistoricPlanItemInstanceQuery orderByExitTime() {
        return orderBy(HistoricPlanItemInstanceQueryProperty.EXIT_TIME);
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
        List<HistoricPlanItemInstance> historicPlanItems;
        if (includeLocalVariables){
            historicPlanItems = CommandContextUtil.getHistoricPlanItemInstanceEntityManager(commandContext).findWithVariablesByCriteria(this);
        } else {
             historicPlanItems =CommandContextUtil.getHistoricPlanItemInstanceEntityManager(commandContext).findByCriteria(this);
        }

        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        if (cmmnEngineConfiguration.getPlanItemLocalizationManager() != null) {
            for (HistoricPlanItemInstance historicPlanItemInstance : historicPlanItems) {
                cmmnEngineConfiguration.getPlanItemLocalizationManager().localize(historicPlanItemInstance, locale, withLocalizationFallback);
            }
        }
        return historicPlanItems;
    }

    public String getPlanItemInstanceId() {
        return planItemInstanceId;
    }
    @Override
    public String getId() {
        return planItemInstanceId;
    }
    public String getPlanItemInstanceName() {
        return planItemInstanceName;
    }
    public String getState() {
        return state;
    }
    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }
    public String getDerivedCaseDefinitionId() {
        return derivedCaseDefinitionId;
    }
    public String getCaseInstanceId() {
        return caseInstanceId;
    }
    public String getStageInstanceId() {
        return stageInstanceId;
    }
    public String getElementId() {
        return elementId;
    }
    public String getPlanItemDefinitionId() {
        return planItemDefinitionId;
    }
    public String getPlanItemDefinitionType() {
        return planItemDefinitionType;
    }
    public List<String> getPlanItemDefinitionTypes() {
        return planItemDefinitionTypes;
    }
    public Date getCreatedBefore() {
        return createdBefore;
    }
    public Date getCreatedAfter() {
        return createdAfter;
    }
    public Date getLastAvailableBefore() {
        return lastAvailableBefore;
    }
    public Date getLastAvailableAfter() {
        return lastAvailableAfter;
    }
    public Date getLastUnavailableBefore() {
        return lastUnavailableBefore;
    }
    public Date getLastUnavailableAfter() {
        return lastUnavailableAfter;
    }
    public Date getLastEnabledBefore() {
        return lastEnabledBefore;
    }
    public Date getLastEnabledAfter() {
        return lastEnabledAfter;
    }
    public Date getLastDisabledBefore() {
        return lastDisabledBefore;
    }
    public Date getLastDisabledAfter() {
        return lastDisabledAfter;
    }
    public Date getLastStartedBefore() {
        return lastStartedBefore;
    }
    public Date getLastStartedAfter() {
        return lastStartedAfter;
    }
    public Date getLastSuspendedBefore() {
        return lastSuspendedBefore;
    }
    public Date getLastSuspendedAfter() {
        return lastSuspendedAfter;
    }
    public Date getCompletedBefore() {
        return completedBefore;
    }
    public Date getCompletedAfter() {
        return completedAfter;
    }
    public Date getTerminatedBefore() {
        return terminatedBefore;
    }
    public Date getTerminatedAfter() {
        return terminatedAfter;
    }
    public Date getOccurredBefore() {
        return occurredBefore;
    }
    public Date getOccurredAfter() {
        return occurredAfter;
    }
    public Date getExitBefore() {
        return exitBefore;
    }
    public Date getExitAfter() {
        return exitAfter;
    }
    public Date getEndedBefore() {
        return endedBefore;
    }
    public Date getEndedAfter() {
        return endedAfter;
    }
    public String getStartUserId() {
        return startUserId;
    }
    public String getAssignee() {
        return assignee;
    }
    public String getCompletedBy() {
        return completedBy;
    }
    public String getReferenceId() {
        return referenceId;
    }
    public String getReferenceType() {
        return referenceType;
    }
    public boolean isEnded() {
        return ended;
    }
    public boolean isNotEnded() {
        return notEnded;
    }
    public String getEntryCriterionId() {
        return entryCriterionId;
    }
    public String getExitCriterionId() {
        return exitCriterionId;
    }
    public String getFormKey() {
        return formKey;
    }
    public String getExtraValue() {
        return extraValue;
    }
    public String getInvolvedUser() {
        return involvedUser;
    }
    public Collection<String> getInvolvedGroups() {
        return involvedGroups;
    }
    public boolean isOnlyStages() {
        return onlyStages;
    }
    public String getTenantId() {
        return tenantId;
    }
    public String getTenantIdLike() {
        return tenantIdLike;
    }
    public boolean isWithoutTenantId() {
        return withoutTenantId;
    }
    public boolean isIncludeLocalVariables() {
        return includeLocalVariables;
    }

    public List<List<String>> getSafeInvolvedGroups() {
        return safeInvolvedGroups;
    }

    public void setSafeInvolvedGroups(List<List<String>> safeInvolvedGroups) {
        this.safeInvolvedGroups = safeInvolvedGroups;
    }

    public List<List<String>> getSafeCaseInstanceIds() {
        return safeCaseInstanceIds;
    }

    public void setSafeCaseInstanceIds(List<List<String>> safeProcessInstanceIds) {
        this.safeCaseInstanceIds = safeProcessInstanceIds;
    }

    public Collection<String> getCaseInstanceIds() {
        return caseInstanceIds;
    }
}
