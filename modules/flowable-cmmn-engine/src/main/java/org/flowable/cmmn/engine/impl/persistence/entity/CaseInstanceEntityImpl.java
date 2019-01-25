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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableScopeImpl;

/**
 * @author Joram Barrez
 */
public class CaseInstanceEntityImpl extends AbstractCmmnEngineVariableScopeEntity implements CaseInstanceEntity {

    protected String businessKey;
    protected String name;
    protected String parentId;
    protected String caseDefinitionId;
    protected String state;
    protected Date startTime;
    protected String startUserId;
    protected String callbackId;
    protected String callbackType;
    protected boolean completeable;
    protected String tenantId = CmmnEngineConfiguration.NO_TENANT_ID;

    protected Date lockTime;

    // non persisted
    protected List<PlanItemInstanceEntity> childPlanItemInstances;
    protected List<SentryPartInstanceEntity> satisfiedSentryPartInstances;

    protected List<VariableInstanceEntity> queryVariables;

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("businessKey", businessKey);
        persistentState.put("name", name);
        persistentState.put("parentId", parentId);
        persistentState.put("caseDefinitionId", caseDefinitionId);
        persistentState.put("state", state);
        persistentState.put("startTime", startTime);
        persistentState.put("startUserId", startUserId);
        persistentState.put("callbackId", callbackId);
        persistentState.put("callbackType", callbackType);
        persistentState.put("completeable", completeable);
        persistentState.put("tenantId", tenantId);
        persistentState.put("lockTime", lockTime);
        return persistentState;
    }

    @Override
    public String getBusinessKey() {
        return businessKey;
    }
    @Override
    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
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
    public String getParentId() {
        return parentId;
    }
    @Override
    public void setParentId(String parentId) {
        this.parentId = parentId;
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
    public boolean isCompleteable() {
        return completeable;
    }
    @Override
    public void setCompleteable(boolean completeable) {
        this.completeable = completeable;
    }
    @Override
    public String getCallbackId() {
        return callbackId;
    }
    @Override
    public void setCallbackId(String callbackId) {
        this.callbackId = callbackId;
    }
    @Override
    public String getCallbackType() {
        return callbackType;
    }
    @Override
    public void setCallbackType(String callbackType) {
        this.callbackType = callbackType;
    }
    @Override
    public String getTenantId() {
        return tenantId;
    }
    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    public Date getLockTime() {
        return lockTime;
    }
    public void setLockTime(Date lockTime) {
        this.lockTime = lockTime;
    }

    @Override
    public List<PlanItem> getPlanItems() {
        if (caseDefinitionId != null) {
            return CaseDefinitionUtil.getCase(caseDefinitionId).getPlanModel().getPlanItems();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<PlanItemInstanceEntity> getChildPlanItemInstances() {
        return childPlanItemInstances;
    }

    @Override
    public void setChildPlanItemInstances(List<PlanItemInstanceEntity> childPlanItemInstances) {
        this.childPlanItemInstances = childPlanItemInstances;
    }

    @Override
    public List<SentryPartInstanceEntity> getSatisfiedSentryPartInstances() {
        if (satisfiedSentryPartInstances == null) {
            satisfiedSentryPartInstances = CommandContextUtil.getSentryPartInstanceEntityManager()
                    .findSentryPartInstancesByCaseInstanceIdAndNullPlanItemInstanceId(id);
        }
        return satisfiedSentryPartInstances;
    }

    @Override
    public void setSatisfiedSentryPartInstances(List<SentryPartInstanceEntity> sentryPartInstanceEntities) {
        this.satisfiedSentryPartInstances = sentryPartInstanceEntities;
    }


    // VariableScopeImpl methods

    @Override
    protected Collection<VariableInstanceEntity> loadVariableInstances() {
        return CommandContextUtil.getVariableService().findVariableInstanceByScopeIdAndScopeType(id, ScopeTypes.CMMN);
    }

    @Override
    protected VariableScopeImpl getParentVariableScope() {
        // A case instance is the root of variables.
        // In case of parent-child case instances, the variables needs to be defined explictely in input/outpur vars 
        return null;
    }

    @Override
    protected void initializeVariableInstanceBackPointer(VariableInstanceEntity variableInstance) {
        variableInstance.setScopeId(id);
        variableInstance.setScopeType(ScopeTypes.CMMN);
    }

    @Override
    protected VariableInstanceEntity getSpecificVariable(String variableName) {
        return CommandContextUtil.getVariableService().findVariableInstanceByScopeIdAndScopeTypeAndName(id, ScopeTypes.CMMN, variableName);
    }

    @Override
    protected List<VariableInstanceEntity> getSpecificVariables(Collection<String> variableNames) {
        return CommandContextUtil.getVariableService().findVariableInstancesByScopeIdAndScopeTypeAndNames(id, ScopeTypes.CMMN, variableNames);
    }

    @Override
    protected boolean isPropagateToHistoricVariable() {
        return true;
    }

    @Override
    public Map<String, Object> getCaseVariables() {
        Map<String, Object> caseVariables = new HashMap<>();
        if (this.queryVariables != null) {
            for (VariableInstanceEntity queryVariable : queryVariables) {
                if (queryVariable.getId() != null && queryVariable.getTaskId() == null) {
                    caseVariables.put(queryVariable.getName(), queryVariable.getValue());
                }
            }
        }
        return caseVariables;
    }

    public List<VariableInstanceEntity> getQueryVariables() {
        return queryVariables;
    }

    public void setQueryVariables(List<VariableInstanceEntity> queryVariables) {
        this.queryVariables = queryVariables;
    }
}
