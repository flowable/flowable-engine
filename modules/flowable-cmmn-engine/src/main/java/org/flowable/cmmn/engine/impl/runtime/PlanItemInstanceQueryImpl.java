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
package org.flowable.cmmn.engine.impl.runtime;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceQuery;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.query.CacheAwareQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.variable.service.impl.AbstractVariableQueryImpl;

/**
 * @author Joram Barrez
 */
public class PlanItemInstanceQueryImpl extends AbstractVariableQueryImpl<PlanItemInstanceQuery, PlanItemInstance> implements PlanItemInstanceQuery,
        CacheAwareQuery<PlanItemInstanceEntity> {
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    
    protected String caseDefinitionId;
    protected String derivedCaseDefinitionId;
    protected String caseInstanceId;
    protected String stageInstanceId;
    protected String planItemInstanceId;
    protected String elementId;
    protected String planItemDefinitionId;
    protected String planItemDefinitionType;
    protected List<String> planItemDefinitionTypes;
    protected String name;
    protected String state;
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
    protected boolean ended;
    protected boolean includeEnded;
    protected String startUserId;
    protected String referenceId;
    protected String referenceType;
    protected boolean completable;
    protected boolean onlyStages;
    protected String entryCriterionId;
    protected String exitCriterionId;
    protected String formKey;
    protected String extraValue;
    protected String involvedUser;
    protected Collection<String> involvedGroups;
    private List<List<String>> safeInvolvedGroups;
    protected String tenantId;
    protected boolean withoutTenantId;
    protected String locale;
    protected boolean withLocalizationFallback;

    public PlanItemInstanceQueryImpl() {
        
    }
    
    public PlanItemInstanceQueryImpl(CommandContext commandContext, CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(commandContext, cmmnEngineConfiguration.getVariableServiceConfiguration());
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    public PlanItemInstanceQueryImpl(CommandExecutor commandExecutor, CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(commandExecutor, cmmnEngineConfiguration.getVariableServiceConfiguration());
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    @Override
    public PlanItemInstanceQuery caseDefinitionId(String caseDefinitionId) {
        if (caseDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Case definition id is null");
        }
        this.caseDefinitionId = caseDefinitionId;
        return this;
    }

    @Override
    public PlanItemInstanceQuery derivedCaseDefinitionId(String derivedCaseDefinitionId) {
        if (derivedCaseDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Derived case definition id is null");
        }
        this.derivedCaseDefinitionId = derivedCaseDefinitionId;
        return this;
    }

    @Override
    public PlanItemInstanceQuery caseInstanceId(String caseInstanceId) {
        if (caseInstanceId == null) {
            throw new FlowableIllegalArgumentException("Case instance id is null");
        }
        this.caseInstanceId = caseInstanceId;
        return this;
    }

    @Override
    public PlanItemInstanceQuery stageInstanceId(String stageInstanceId) {
        if (stageInstanceId == null) {
            throw new FlowableIllegalArgumentException("Stage instance id is null");
        }
        this.stageInstanceId = stageInstanceId;
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceId(String planItemInstanceId) {
        if (planItemInstanceId == null) {
            throw new FlowableIllegalArgumentException("Plan Item instance id is null");
        }
        this.planItemInstanceId = planItemInstanceId;
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceElementId(String elementId) {
        if (elementId == null) {
            throw new FlowableIllegalArgumentException("Element id is null");
        }
        this.elementId = elementId;
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemDefinitionId(String planItemDefinitionId) {
        if (planItemDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Plan item definition id is null");
        }
        this.planItemDefinitionId = planItemDefinitionId;
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemDefinitionType(String planItemDefinitionType) {
        if (planItemDefinitionType == null) {
            throw new FlowableIllegalArgumentException("Plan item definition type is null");
        }
        this.planItemDefinitionType = planItemDefinitionType;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemDefinitionTypes(List<String> planItemDefinitionTypes) {
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
    public PlanItemInstanceQuery planItemInstanceName(String name) {
        if (name == null) {
            throw new FlowableIllegalArgumentException("Name is null");
        }
        this.name = name;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceState(String state) {
        if (state == null) {
            throw new FlowableIllegalArgumentException("State is null");
        }
        this.state = state;
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceStateWaitingForRepetition() {
        return planItemInstanceState(PlanItemInstanceState.WAITING_FOR_REPETITION);
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceStateActive() {
        return planItemInstanceState(PlanItemInstanceState.ACTIVE);
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceStateEnabled() {
        return planItemInstanceState(PlanItemInstanceState.ENABLED);
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceStateDisabled() {
        return planItemInstanceState(PlanItemInstanceState.DISABLED);
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceStateAsyncActive() {
        return planItemInstanceState(PlanItemInstanceState.ASYNC_ACTIVE);
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceStateAvailable() {
        return planItemInstanceState(PlanItemInstanceState.AVAILABLE);
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceStateUnavailable() {
        return planItemInstanceState(PlanItemInstanceState.UNAVAILABLE);
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceStateCompleted() {
        return planItemInstanceState(PlanItemInstanceState.COMPLETED);
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceStateTerminated() {
        return planItemInstanceState(PlanItemInstanceState.TERMINATED);
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceCreatedBefore(Date createdBefore) {
        if (createdBefore == null) {
            throw new FlowableIllegalArgumentException("createdBefore is null");
        }
        this.createdBefore = createdBefore;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceCreatedAfter(Date createdAfter) {
        if (createdAfter == null) {
            throw new FlowableIllegalArgumentException("createdAfter is null");
        }
        this.createdAfter = createdAfter;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastAvailableBefore(Date availableBefore) {
        if (availableBefore == null) {
            throw new FlowableIllegalArgumentException("availableBefore is null");
        }
        this.lastAvailableBefore = availableBefore;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastAvailableAfter(Date availableAfter) {
        if (availableAfter == null) {
            throw new FlowableIllegalArgumentException("availableAfter is null");
        }
        this.lastAvailableAfter = availableAfter;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastUnavailableBefore(Date unavailableBefore) {
        if (unavailableBefore == null) {
            throw new FlowableIllegalArgumentException("unavailableBefore is null");
        }
        this.lastUnavailableBefore = unavailableBefore;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastUnavailableAfter(Date unavailableAfter) {
        if (unavailableAfter == null) {
            throw new FlowableIllegalArgumentException("unavailableAfter is null");
        }
        this.lastUnavailableAfter = unavailableAfter;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastEnabledBefore(Date enabledBefore) {
        if (enabledBefore == null) {
            throw new FlowableIllegalArgumentException("enabledBefore is null");
        }
        this.lastEnabledBefore = enabledBefore;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastEnabledAfter(Date enabledAfter) {
        if (enabledAfter == null) {
            throw new FlowableIllegalArgumentException("enabledAfter is null");
        }
        this.lastEnabledAfter = enabledAfter;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastDisabledBefore(Date disabledBefore) {
        if (disabledBefore == null) {
            throw new FlowableIllegalArgumentException("disabledBefore is null");
        }
        this.lastDisabledBefore = disabledBefore;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastDisabledAfter(Date disabledAfter) {
        if (disabledAfter == null) {
            throw new FlowableIllegalArgumentException("disabledAfter is null");
        }
        this.lastDisabledAfter = disabledAfter;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastStartedBefore(Date startedBefore) {
        if (startedBefore == null) {
            throw new FlowableIllegalArgumentException("activatedBefore is null");
        }
        this.lastStartedBefore = startedBefore;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastStartedAfter(Date startedAfter) {
        if (startedAfter == null) {
            throw new FlowableIllegalArgumentException("startedAfter is null");
        }
        this.lastStartedAfter = startedAfter;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastSuspendedBefore(Date suspendedBefore) {
        if (suspendedBefore == null) {
            throw new FlowableIllegalArgumentException("suspendedBefore is null");
        }
        this.lastSuspendedBefore = suspendedBefore;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastSuspendedAfter(Date suspendedAfter) {
        if (suspendedAfter == null) {
            throw new FlowableIllegalArgumentException("suspendedAfter is null");
        }
        this.lastSuspendedAfter = suspendedAfter;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceCompletedBefore(Date completedBefore) {
        if (completedBefore == null) {
            throw new FlowableIllegalArgumentException("completedBefore is null");
        }
        this.completedBefore = completedBefore;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceCompletedAfter(Date completedAfter) {
        if (completedAfter == null) {
            throw new FlowableIllegalArgumentException("completedAfter is null");
        }
        this.completedAfter = completedAfter;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceOccurredBefore(Date occurredBefore) {
        if (occurredBefore == null) {
            throw new FlowableIllegalArgumentException("occurredBefore is null");
        }
        this.occurredBefore = occurredBefore;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceOccurredAfter(Date occurredAfter) {
        if (occurredAfter == null) {
            throw new FlowableIllegalArgumentException("occurredAfter is null");
        }
        this.occurredAfter = occurredAfter;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceTerminatedBefore(Date terminatedBefore) {
        if (terminatedBefore == null) {
            throw new FlowableIllegalArgumentException("terminatedBefore is null");
        }
        this.terminatedBefore = terminatedBefore;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceTerminatedAfter(Date terminatedAfter) {
        if (terminatedAfter == null) {
            throw new FlowableIllegalArgumentException("terminatedAfter is null");
        }
        this.terminatedAfter = terminatedAfter;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceExitBefore(Date exitBefore) {
        if (exitBefore == null) {
            throw new FlowableIllegalArgumentException("exitBefore is null");
        }
        this.exitBefore = exitBefore;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceExitAfter(Date exitAfter) {
        if (exitAfter == null) {
            throw new FlowableIllegalArgumentException("exitAfter is null");
        }
        this.exitAfter = exitAfter;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceEndedBefore(Date endedBefore) {
        if (endedBefore == null) {
            throw new FlowableIllegalArgumentException("endedBefore is null");
        }
        this.endedBefore = endedBefore;
        return this;
    }
    @Override
    public PlanItemInstanceQuery planItemInstanceEndedAfter(Date endedAfter) {
        if (endedAfter == null) {
            throw new FlowableIllegalArgumentException("endedAfter is null");
        }
        this.endedAfter = endedAfter;
        return this;
    }

    @Override
    public PlanItemInstanceQuery ended() {
        this.ended = true;
        return this;
    }

    @Override
    public PlanItemInstanceQuery includeEnded() {
        this.includeEnded = true;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceStartUserId(String startUserId) {
        if (startUserId == null) {
            throw new FlowableIllegalArgumentException("Start user id is null");
        }
        this.startUserId = startUserId;
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceReferenceId(String referenceId) {
        this.referenceId = referenceId;
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceReferenceType(String referenceType) {
        this.referenceType = referenceType;
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceCompletable() {
        this.completable = true;
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery onlyStages() {
        this.onlyStages = true;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceEntryCriterionId(String entryCriterionId) {
        if (entryCriterionId == null) {
            throw new FlowableIllegalArgumentException("EntryCriterionId is null");
        }
        this.entryCriterionId = entryCriterionId;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceExitCriterionId(String exitCriterionId) {
        if (exitCriterionId == null) {
            throw new FlowableIllegalArgumentException("ExitCriterionId is null");
        }
        this.exitCriterionId = exitCriterionId;
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceFormKey(String formKey) {
        if (formKey == null) {
            throw new FlowableIllegalArgumentException("formKey is null");
        }
        this.formKey = formKey;
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceExtraValue(String extraValue) {
        if (extraValue == null) {
            throw new FlowableIllegalArgumentException("extraValue is null");
        }
        this.extraValue = extraValue;
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery involvedUser(String involvedUser) {
        if (involvedUser == null) {
            throw new FlowableIllegalArgumentException("involvedUser is null");
        }
        this.involvedUser = involvedUser;
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery involvedGroups(Collection<String> involvedGroups) {
        if (involvedGroups == null) {
            throw new FlowableIllegalArgumentException("involvedGroups is null");
        }
        this.involvedGroups = involvedGroups;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("Tenant id is null");
        }
        this.tenantId = tenantId;
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery caseVariableValueEquals(String name, Object value) {
        return variableValueEquals(name, value, false);
    }

    @Override
    public PlanItemInstanceQuery caseVariableValueEquals(Object value) {
        return variableValueEquals(value, false);
    }

    @Override
    public PlanItemInstanceQuery caseVariableValueEqualsIgnoreCase(String name, String value) {
        return variableValueEqualsIgnoreCase(name, value, false);
    }

    @Override
    public PlanItemInstanceQuery caseVariableValueNotEquals(String name, Object value) {
        return variableValueNotEquals(name, value, false);
    }

    @Override
    public PlanItemInstanceQuery caseVariableValueNotEqualsIgnoreCase(String name, String value) {
        return variableValueNotEqualsIgnoreCase(name, value, false);
    }

    @Override
    public PlanItemInstanceQuery caseVariableValueGreaterThan(String name, Object value) {
        return variableValueGreaterThan(name, value, false);
    }

    @Override
    public PlanItemInstanceQuery caseVariableValueGreaterThanOrEqual(String name, Object value) {
        return variableValueGreaterThanOrEqual(name, value, false);
    }

    @Override
    public PlanItemInstanceQuery caseVariableValueLessThan(String name, Object value) {
        return variableValueLessThan(name, value, false);
    }

    @Override
    public PlanItemInstanceQuery caseVariableValueLessThanOrEqual(String name, Object value) {
        return variableValueLessThanOrEqual(name, value, false);
    }

    @Override
    public PlanItemInstanceQuery caseVariableValueLike(String name, String value) {
        return variableValueLike(name, value, false);
    }

    @Override
    public PlanItemInstanceQuery caseVariableValueLikeIgnoreCase(String name, String value) {
        return variableValueLikeIgnoreCase(name, value, false);
    }

    @Override
    public PlanItemInstanceQuery caseVariableExists(String name) {
        return variableExists(name, false);
    }

    @Override
    public PlanItemInstanceQuery caseVariableNotExists(String name) {
        return variableNotExists(name, false);
    }

    @Override
    public PlanItemInstanceQuery locale(String locale) {
        this.locale = locale;
        return this;
    }

    @Override
    public PlanItemInstanceQuery withLocalizationFallback() {
        this.withLocalizationFallback = true;
        return this;
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        ensureVariablesInitialized();
        return cmmnEngineConfiguration.getPlanItemInstanceEntityManager().countByCriteria(this);
    }

    @Override
    public List<PlanItemInstance> executeList(CommandContext commandContext) {
        ensureVariablesInitialized();
        List<PlanItemInstance> planItems = cmmnEngineConfiguration.getPlanItemInstanceEntityManager().findByCriteria(this);
      
        if (cmmnEngineConfiguration.getPlanItemLocalizationManager() != null) {
            for (PlanItemInstance planItemInstance : planItems) {
                cmmnEngineConfiguration.getPlanItemLocalizationManager().localize(planItemInstance, locale, withLocalizationFallback);
            }
        }

        return planItems;
    }
    
    @Override
    public PlanItemInstanceQuery orderByCreateTime() {
        this.orderProperty = PlanItemInstanceQueryProperty.CREATE_TIME;
        return this;
    }

    @Override
    public PlanItemInstanceQuery orderByEndTime() {
        this.orderProperty = PlanItemInstanceQueryProperty.END_TIME;
        return this;
    }

    @Override
    public PlanItemInstanceQuery orderByName() {
        this.orderProperty = PlanItemInstanceQueryProperty.NAME;
        return this;
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
    public String getPlanItemInstanceId() {
        return planItemInstanceId;
    }
    @Override
    public String getId() {
        return planItemInstanceId;
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
    public String getName() {
        return name;
    }
    public String getState() {
        return state;
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
    public boolean isEnded() {
        return ended;
    }
    public boolean isIncludeEnded() {
        return includeEnded;
    }
    public String getStartUserId() {
        return startUserId;
    }
    public String getReferenceId() {
        return referenceId;
    }
    public String getReferenceType() {
        return referenceType;
    }
    public boolean isCompletable() {
        return completable;
    }
    public boolean isOnlyStages() {
        return onlyStages;
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
    public String getTenantId() {
        return tenantId;
    }
    public boolean isWithoutTenantId() {
        return withoutTenantId;
    }

    public List<List<String>> getSafeInvolvedGroups() {
        return safeInvolvedGroups;
    }

    public void setSafeInvolvedGroups(List<List<String>> safeInvolvedGroups) {
        this.safeInvolvedGroups = safeInvolvedGroups;
    }
}
