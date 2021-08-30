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
package org.flowable.cmmn.engine.impl.persistence.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.delegate.ReadOnlyDelegatePlanItemInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.listener.PlanItemInstanceLifecycleListener;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.delegate.ReadOnlyDelegatePlanItemInstanceImpl;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.ExpressionUtil;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.FlowableListener;
import org.flowable.cmmn.model.PlanFragment;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.RepetitionRule;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableScopeImpl;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public class PlanItemInstanceEntityImpl extends AbstractCmmnEngineVariableScopeEntity implements PlanItemInstanceEntity, CountingPlanItemInstanceEntity {
    
    protected String caseDefinitionId;
    protected String derivedCaseDefinitionId;
    protected String caseInstanceId;
    protected String stageInstanceId;
    protected boolean isStage;
    protected String elementId;
    protected String planItemDefinitionId;
    protected String planItemDefinitionType;
    protected String name;
    protected String state;
    protected Date createTime;
    protected Date lastAvailableTime;
    protected Date lastUnavailableTime;
    protected Date lastEnabledTime;
    protected Date lastDisabledTime;
    protected Date lastStartedTime;
    protected Date lastSuspendedTime;
    protected Date completedTime;
    protected Date occurredTime;
    protected Date terminatedTime;
    protected Date exitTime;
    protected Date endedTime;
    protected String startUserId;
    protected String referenceId;
    protected String referenceType;
    protected boolean completable;
    protected String entryCriterionId;
    protected String exitCriterionId;
    protected String extraValue;
    protected String tenantId = CmmnEngineConfiguration.NO_TENANT_ID;
    
    // Counts
    protected boolean countEnabled;
    protected int variableCount;
    protected int sentryPartInstanceCount;
    
    // Non-persisted
    protected PlanItem planItem;
    protected List<PlanItemInstanceEntity> childPlanItemInstances;
    protected PlanItemInstanceEntity stagePlanItemInstance;
    protected List<SentryPartInstanceEntity> satisfiedSentryPartInstances;
    protected String localizedName;

    protected PlanItemInstanceLifecycleListener currentLifecycleListener; // Only set when executing an plan item lifecycle listener
    protected FlowableListener currentFlowableListener; // Only set when executing an plan item lifecycle listener

    public PlanItemInstanceEntityImpl() {
    }

    public PlanItemInstanceEntityImpl(HistoricPlanItemInstance historicPlanItemInstance) {
        setId(historicPlanItemInstance.getId());
        setName(historicPlanItemInstance.getName());
        setState(historicPlanItemInstance.getState());
        setCaseDefinitionId(historicPlanItemInstance.getCaseDefinitionId());
        setDerivedCaseDefinitionId(historicPlanItemInstance.getDerivedCaseDefinitionId());
        setCaseInstanceId(historicPlanItemInstance.getCaseInstanceId());
        setStageInstanceId(historicPlanItemInstance.getStageInstanceId());
        setStage(historicPlanItemInstance.isStage());
        setElementId(historicPlanItemInstance.getElementId());
        setPlanItemDefinitionId(historicPlanItemInstance.getPlanItemDefinitionId());
        setPlanItemDefinitionType(historicPlanItemInstance.getPlanItemDefinitionType());
        setCreateTime(historicPlanItemInstance.getCreateTime());
        setLastAvailableTime(historicPlanItemInstance.getLastAvailableTime());
        setLastUnavailableTime(historicPlanItemInstance.getLastUnavailableTime());
        setLastEnabledTime(historicPlanItemInstance.getLastEnabledTime());
        setLastDisabledTime(historicPlanItemInstance.getLastDisabledTime());
        setLastStartedTime(historicPlanItemInstance.getLastStartedTime());
        setLastSuspendedTime(historicPlanItemInstance.getLastSuspendedTime());
        setCompletedTime(historicPlanItemInstance.getCompletedTime());
        setOccurredTime(historicPlanItemInstance.getOccurredTime());
        setTerminatedTime(historicPlanItemInstance.getTerminatedTime());
        setExitTime(historicPlanItemInstance.getExitTime());
        setEndedTime(historicPlanItemInstance.getEndedTime());
        setStartUserId(historicPlanItemInstance.getStartUserId());
        setReferenceId(historicPlanItemInstance.getReferenceId());
        setReferenceType(historicPlanItemInstance.getReferenceType());
        setEntryCriterionId(historicPlanItemInstance.getEntryCriterionId());
        setExitCriterionId(historicPlanItemInstance.getExitCriterionId());
        setFormKey(historicPlanItemInstance.getFormKey());
        setExtraValue(historicPlanItemInstance.getExtraValue());
        setTenantId(historicPlanItemInstance.getTenantId());
    }

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("caseDefinitionId", caseDefinitionId);
        persistentState.put("derivedCaseDefinitionId", derivedCaseDefinitionId);
        persistentState.put("caseInstanceId", caseInstanceId);
        persistentState.put("stageInstanceId", stageInstanceId);
        persistentState.put("isStage", isStage);
        persistentState.put("elementId", elementId);
        persistentState.put("planItemDefinitionId", planItemDefinitionId);
        persistentState.put("planItemDefinitionType", planItemDefinitionType);
        persistentState.put("name", name);
        persistentState.put("state", state);
        persistentState.put("createTime", createTime);
        persistentState.put("lastAvailableTime", lastAvailableTime);
        persistentState.put("lastUnavailableTime", lastUnavailableTime);
        persistentState.put("lastEnabledTime", lastEnabledTime);
        persistentState.put("lastDisabledTime", lastDisabledTime);
        persistentState.put("lastStartedTime", lastStartedTime);
        persistentState.put("lastSuspendedTime", lastSuspendedTime);
        persistentState.put("completedTime", completedTime);
        persistentState.put("occurredTime", occurredTime);
        persistentState.put("terminatedTime", terminatedTime);
        persistentState.put("exitTime", exitTime);
        persistentState.put("endedTime", endedTime);
        persistentState.put("startUserId", startUserId);
        persistentState.put("referenceId", referenceId);
        persistentState.put("referenceType", referenceType);
        persistentState.put("completeable", completable);
        persistentState.put("entryCriterionId", entryCriterionId);
        persistentState.put("exitCriterionId", exitCriterionId);
        persistentState.put("extraValue", extraValue);
        persistentState.put("countEnabled", countEnabled);
        persistentState.put("variableCount", variableCount);
        persistentState.put("sentryPartInstanceCount", sentryPartInstanceCount);
        persistentState.put("tenantId", tenantId);
        return persistentState;
    }
    
    @Override
    public ReadOnlyDelegatePlanItemInstance snapshotReadOnly() {
        return new ReadOnlyDelegatePlanItemInstanceImpl(this);
    }

    @Override
    public PlanItem getPlanItem() {
        if (planItem == null) {
            Case caze;

            // check, if the plan item is a derived one
            if (derivedCaseDefinitionId != null) {
                caze = CaseDefinitionUtil.getCase(derivedCaseDefinitionId);
            } else {
                caze = CaseDefinitionUtil.getCase(caseDefinitionId);
            }
            planItem = (PlanItem) caze.getAllCaseElements().get(elementId);
        }
        return planItem;
    }
    
    @Override
    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }
    @Override
    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }
    @Override
    public String getDerivedCaseDefinitionId() {
        return derivedCaseDefinitionId;
    }
    @Override
    public void setDerivedCaseDefinitionId(String derivedCaseDefinitionId) {
        this.derivedCaseDefinitionId = derivedCaseDefinitionId;
    }
    @Override
    public String getCaseInstanceId() {
        return caseInstanceId;
    }
    @Override
    public void setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }
    @Override
    public String getStageInstanceId() {
        return stageInstanceId;
    }
    @Override
    public void setStageInstanceId(String stageInstanceId) {
        this.stageInstanceId = stageInstanceId;
    }
    @Override
    public boolean isStage() {
        return isStage;
    }
    @Override
    public void setStage(boolean isStage) {
        this.isStage = isStage;
    }
    @Override
    public String getElementId() {
        return elementId;
    }
    @Override
    public void setElementId(String elementId) {
        this.elementId = elementId;
    }
    @Override
    public String getPlanItemDefinitionId() {
        return planItemDefinitionId;
    }
    @Override
    public void setPlanItemDefinitionId(String planItemDefinitionId) {
        this.planItemDefinitionId = planItemDefinitionId;
    }
    @Override
    public String getPlanItemDefinitionType() {
        return planItemDefinitionType;
    }
    @Override
    public void setPlanItemDefinitionType(String planItemDefinitionType) {
        this.planItemDefinitionType = planItemDefinitionType;
    }
    @Override
    public String getName() {
        if (StringUtils.isNotBlank(localizedName)) {
            return localizedName;
        }
        return name;
    }
    @Override
    public void setName(String name) {
        this.name = name;
    }
    @Override
    public String getState() {
        return state;
    }
    @Override
    public void setState(String state) {
        this.state = state;
    }
    @Override
    public Date getCreateTime() {
        return createTime;
    }
    @Override
    public Date getStartTime() {
        return getCreateTime();
    }
    @Override
    public void setStartTime(Date startTime) {
        setCreateTime(startTime);
    }
    @Override
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
    @Override
    public Date getLastAvailableTime() {
        return lastAvailableTime;
    }
    @Override
    public void setLastAvailableTime(Date lastAvailableTime) {
        this.lastAvailableTime = lastAvailableTime;
    }
    @Override
    public Date getLastUnavailableTime() {
        return lastUnavailableTime;
    }
    @Override
    public void setLastUnavailableTime(Date lastUnavailableTime) {
        this.lastUnavailableTime = lastUnavailableTime;
    }
    @Override
    public Date getLastEnabledTime() {
        return lastEnabledTime;
    }
    @Override
    public void setLastEnabledTime(Date lastEnabledTime) {
        this.lastEnabledTime = lastEnabledTime;
    }
    @Override
    public Date getLastDisabledTime() {
        return lastDisabledTime;
    }
    @Override
    public void setLastDisabledTime(Date lastDisabledTime) {
        this.lastDisabledTime = lastDisabledTime;
    }
    @Override
    public Date getLastStartedTime() {
        return lastStartedTime;
    }
    @Override
    public void setLastStartedTime(Date lastStartedTime) {
        this.lastStartedTime = lastStartedTime;
    }
    @Override
    public Date getLastSuspendedTime() {
        return lastSuspendedTime;
    }
    @Override
    public void setLastSuspendedTime(Date lastSuspendedTime) {
        this.lastSuspendedTime = lastSuspendedTime;
    }
    @Override
    public Date getCompletedTime() {
        return completedTime;
    }
    @Override
    public void setCompletedTime(Date completedTime) {
        this.completedTime = completedTime;
    }
    @Override
    public Date getOccurredTime() {
        return occurredTime;
    }
    @Override
    public void setOccurredTime(Date occurredTime) {
        this.occurredTime = occurredTime;
    }
    @Override
    public Date getTerminatedTime() {
        return terminatedTime;
    }
    @Override
    public void setTerminatedTime(Date terminatedTime) {
        this.terminatedTime = terminatedTime;
    }
    @Override
    public Date getExitTime() {
        return exitTime;
    }
    @Override
    public void setExitTime(Date exitTime) {
        this.exitTime = exitTime;
    }
    @Override
    public Date getEndedTime() {
        return endedTime;
    }
    @Override
    public void setEndedTime(Date endedTime) {
        this.endedTime = endedTime;
    }
    public void setPlanItem(PlanItem planItem) {
        this.planItem = planItem;
    }
    public PlanItemInstanceEntity getStagePlanItemInstance() {
        return stagePlanItemInstance;
    }
    public void setStagePlanItemInstance(PlanItemInstanceEntity stagePlanItemInstance) {
        this.stagePlanItemInstance = stagePlanItemInstance;
    }
    @Override
    public String getStartUserId() {
        return startUserId;
    }
    @Override
    public void setStartUserId(String startUserId) {
        this.startUserId = startUserId;
    }
    @Override
    public String getReferenceId() {
        return referenceId;
    }
    @Override
    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
    @Override
    public String getReferenceType() {
        return referenceType;
    }
    @Override
    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }
    @Override
    public boolean isCompletable() {
        return completable;
    }
    @Override
    public void setCompletable(boolean completable) {
        this.completable = completable;
    }
    @Override
    public String getEntryCriterionId() {
        return entryCriterionId;
    }
    @Override
    public void setEntryCriterionId(String entryCriterionId) {
        this.entryCriterionId = entryCriterionId;
    }
    @Override
    public String getExitCriterionId() {
        return exitCriterionId;
    }
    @Override
    public void setExitCriterionId(String exitCriterionId) {
        this.exitCriterionId = exitCriterionId;
    }
    @Override
    public String getFormKey() {
        return extraValue;
    }
    @Override
    public void setFormKey(String formKey) {
        this.extraValue = formKey;
    }
    @Override
    public String getExtraValue() {
        return extraValue;
    }
    @Override
    public void setExtraValue(String extraValue) {
        this.extraValue = extraValue;
    }
    @Override
    public String getTenantId() {
        return tenantId;
    }
    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    @Override
    public void setChildPlanItemInstances(List<PlanItemInstanceEntity> childPlanItemInstances) {
        this.childPlanItemInstances = childPlanItemInstances;
    }

    @Override
    public List<PlanItem> getPlanItems() {
        PlanItem planItem = getPlanItem();
        if (planItem != null && planItem.getPlanItemDefinition() instanceof PlanFragment) {
            return ((PlanFragment) planItem.getPlanItemDefinition()).getPlanItems();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<PlanItemInstanceEntity> getChildPlanItemInstances() {
        if (childPlanItemInstances == null && id != null) {
            childPlanItemInstances = CommandContextUtil.getPlanItemInstanceEntityManager().findByStagePlanItemInstanceId(id);
        }
        return childPlanItemInstances;
    }
    
    @Override
    public PlanItemInstanceEntity getStagePlanItemInstanceEntity() {
        if (stagePlanItemInstance == null && stageInstanceId != null) {
            stagePlanItemInstance = CommandContextUtil.getPlanItemInstanceEntityManager().findById(stageInstanceId);
        }
        return stagePlanItemInstance;
    }
    
    @Override
    public List<SentryPartInstanceEntity> getSatisfiedSentryPartInstances() {
        if (satisfiedSentryPartInstances == null) {
            if (sentryPartInstanceCount == 0) {
                satisfiedSentryPartInstances = new ArrayList<>(1);
            } else {
                satisfiedSentryPartInstances = CommandContextUtil.getSentryPartInstanceEntityManager().findSentryPartInstancesByPlanItemInstanceId(id);
            }
        }
        return satisfiedSentryPartInstances;
    }
    
    @Override
    public void setSatisfiedSentryPartInstances(List<SentryPartInstanceEntity> satisfiedSentryPartInstances) {
        this.satisfiedSentryPartInstances = satisfiedSentryPartInstances;
    }

    // VariableScopeImpl methods

    @Override
    protected Collection<VariableInstanceEntity> loadVariableInstances() {
        return getVariableServiceConfiguration().getVariableService().findVariableInstanceBySubScopeIdAndScopeType(id, ScopeTypes.CMMN);
    }

    @Override
    public VariableScopeImpl getParentVariableScope() {
        PlanItemInstanceEntity stagePlanItem = getStagePlanItemInstanceEntity();
        if (stagePlanItem != null) {
            return (VariableScopeImpl) stagePlanItem;
        }
        if (caseInstanceId != null) {
            return (VariableScopeImpl) CommandContextUtil.getCaseInstanceEntityManager().findById(caseInstanceId);
        }
        return null;
    }

    @Override
    protected void initializeVariableInstanceBackPointer(VariableInstanceEntity variableInstance) {
        variableInstance.setScopeId(caseInstanceId);
        variableInstance.setSubScopeId(id);
        variableInstance.setScopeType(ScopeTypes.CMMN);
    }

    @Override
    protected boolean storeVariableLocal(String variableName) {
        if (super.storeVariableLocal(variableName)) {
            return true;
        }

        RepetitionRule repetitionRule = ExpressionUtil.getRepetitionRule(this);
        if (repetitionRule != null && repetitionRule.getAggregations() != null) {
            // If this is a plan item with a repetition rule and has aggregations then we need to store the variables locally
            // Checking for the aggregations is for backwards compatibility
            return true;
        }

        return false;
    }

    @Override
    protected void addLoggingSessionInfo(ObjectNode loggingNode) {
        // TODO
    }

    @Override
    protected void createVariableLocal(String variableName, Object value) {
        super.createVariableLocal(variableName, value);
        setVariableCount(variableCount + 1); 
    }

    @Override
    protected VariableInstanceEntity getSpecificVariable(String variableName) {
        return getVariableServiceConfiguration().getVariableService()
                .createInternalVariableInstanceQuery()
                .subScopeId(id)
                .scopeType(ScopeTypes.CMMN)
                .name(variableName)
                .singleResult();
    }

    @Override
    protected List<VariableInstanceEntity> getSpecificVariables(Collection<String> variableNames) {
        return getVariableServiceConfiguration().getVariableService()
                .createInternalVariableInstanceQuery()
                .subScopeId(id)
                .scopeType(ScopeTypes.CMMN)
                .names(variableNames)
                .list();
    }

    @Override
    protected boolean isPropagateToHistoricVariable() {
        return true;
    }

    @Override
    protected VariableServiceConfiguration getVariableServiceConfiguration() {
        return CommandContextUtil.getCmmnEngineConfiguration().getVariableServiceConfiguration();
    }

    @Override
    public boolean isCountEnabled() {
        return countEnabled;
    }

    @Override
    public void setCountEnabled(boolean countEnabled) {
        this.countEnabled = countEnabled;
    }

    @Override
    public int getVariableCount() {
        return variableCount;
    }

    @Override
    public void setVariableCount(int variableCount) {
        this.variableCount = variableCount;
    }

    @Override
    public int getSentryPartInstanceCount() {
        return sentryPartInstanceCount;
    }

    @Override
    public void setSentryPartInstanceCount(int sentryPartInstanceCount) {
        this.sentryPartInstanceCount = sentryPartInstanceCount;
    }

    @Override
    public FlowableListener getCurrentFlowableListener() {
        return currentFlowableListener;
    }

    @Override
    public PlanItemInstanceLifecycleListener getCurrentLifecycleListener() {
        return currentLifecycleListener;
    }

    @Override
    public void setCurrentLifecycleListener(PlanItemInstanceLifecycleListener lifecycleListener, FlowableListener flowableListener) {
        this.currentLifecycleListener = lifecycleListener;
        this.currentFlowableListener = flowableListener;
    }

    public String getLocalizedName() {
        return localizedName;
    }

    @Override
    public void setLocalizedName(String localizedName) {
        this.localizedName = localizedName;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PlanItemInstance with id: ")
            .append(id);

        if (getName() != null) {
            stringBuilder.append(", name: ").append(getName());
        }
        stringBuilder.append(", definitionId: ")
            .append(planItemDefinitionId)
            .append(", state: ")
            .append(state);
        return stringBuilder.toString();
    }
}
