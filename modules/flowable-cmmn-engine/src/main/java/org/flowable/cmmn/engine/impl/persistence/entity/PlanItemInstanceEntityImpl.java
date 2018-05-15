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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableScopeImpl;

/**
 * @author Joram Barrez
 */
public class PlanItemInstanceEntityImpl extends VariableScopeImpl implements PlanItemInstanceEntity, CountingPlanItemInstanceEntity {
    
    protected String caseDefinitionId;
    protected String caseInstanceId;
    protected String stageInstanceId;
    protected boolean isStage;
    protected String elementId;
    protected String planItemDefinitionId;
    protected String planItemDefinitionType;
    protected String name;
    protected String state;
    protected Date startTime;
    protected String startUserId;
    protected String referenceId;
    protected String referenceType;
    protected boolean completeable;
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
    
    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("caseDefinitionId", caseDefinitionId);
        persistentState.put("caseInstanceId", caseInstanceId);
        persistentState.put("stageInstanceId", stageInstanceId);
        persistentState.put("isStage", isStage);
        persistentState.put("elementId", elementId);
        persistentState.put("planItemDefinitionId", planItemDefinitionId);
        persistentState.put("planItemDefinitionType", planItemDefinitionType);
        persistentState.put("name", name);
        persistentState.put("state", state);
        persistentState.put("startTime", startTime);
        persistentState.put("startUserId", startUserId);
        persistentState.put("referenceId", referenceId);
        persistentState.put("referenceType", referenceType);
        persistentState.put("completeable", completeable);
        persistentState.put("countEnabled", countEnabled);
        persistentState.put("variableCount", variableCount);
        persistentState.put("sentryPartInstanceCount", sentryPartInstanceCount);
        persistentState.put("tenantId", tenantId);
        return persistentState;
    }
    
    @Override
    public PlanItem getPlanItem() {
        if (planItem == null) {
            Case caze = CaseDefinitionUtil.getCase(caseDefinitionId);
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
    public Date getStartTime() {
        return startTime;
    }
    @Override
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
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
    public boolean isCompleteable() {
        return completeable;
    }
    @Override
    public void setCompleteable(boolean completeable) {
        this.completeable = completeable;
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
    public List<PlanItemInstanceEntity> getChildPlanItemInstances() {
        return childPlanItemInstances;
    }
    
    @Override
    public PlanItemInstanceEntity getStagePlanItemInstanceEntity() {
        if (stagePlanItemInstance == null) {
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
        return CommandContextUtil.getVariableService().findVariableInstanceBySubScopeIdAndScopeType(id, ScopeTypes.CMMN);
    }

    @Override
    protected VariableScopeImpl getParentVariableScope() {
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
    protected void createVariableLocal(String variableName, Object value) {
        super.createVariableLocal(variableName, value);
        setVariableCount(variableCount + 1); 
    }

    @Override
    protected VariableInstanceEntity getSpecificVariable(String variableName) {
        return CommandContextUtil.getVariableService().findVariableInstanceBySubScopeIdAndScopeTypeAndName(id, ScopeTypes.CMMN, variableName);
    }

    @Override
    protected List<VariableInstanceEntity> getSpecificVariables(Collection<String> variableNames) {
        return CommandContextUtil.getVariableService().findVariableInstancesBySubScopeIdAndScopeTypeAndNames(id, ScopeTypes.CMMN, variableNames);
    }

    @Override
    protected boolean isPropagateToHistoricVariable() {
        return true;
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
    
}
