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

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CmmnLoggingSessionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.variablelistener.VariableListenerSession;
import org.flowable.common.engine.impl.variablelistener.VariableListenerSessionData;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.VariableInitializingList;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableScopeImpl;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public class CaseInstanceEntityImpl extends AbstractCmmnEngineVariableScopeEntity implements CaseInstanceEntity {

    protected String businessKey;
    protected String businessStatus;
    protected String name;
    protected String parentId;
    protected String caseDefinitionId;
    protected String state;
    protected Date startTime;
    protected String startUserId;
    protected Date lastReactivationTime;
    protected String lastReactivationUserId;
    protected String callbackId;
    protected String callbackType;
    protected String referenceId;
    protected String referenceType;
    protected boolean completable;
    protected String tenantId = CmmnEngineConfiguration.NO_TENANT_ID;

    protected Date lockTime;
    protected String lockOwner;

    // non persisted
    protected List<PlanItemInstanceEntity> childPlanItemInstances;
    protected List<SentryPartInstanceEntity> satisfiedSentryPartInstances;
    protected String localizedName;

    protected List<VariableInstanceEntity> queryVariables;

    protected String caseDefinitionKey;
    protected String caseDefinitionName;
    protected Integer caseDefinitionVersion;
    protected String caseDefinitionDeploymentId;

    public CaseInstanceEntityImpl() {
    }

    public CaseInstanceEntityImpl(HistoricCaseInstance historicCaseInstance, Map<String, VariableInstanceEntity> variables) {
        this.id = historicCaseInstance.getId();
        this.businessKey = historicCaseInstance.getBusinessKey();
        this.businessStatus = historicCaseInstance.getBusinessStatus();
        this.name = historicCaseInstance.getName();
        this.parentId = historicCaseInstance.getParentId();
        this.caseDefinitionId = historicCaseInstance.getCaseDefinitionId();
        this.caseDefinitionKey = historicCaseInstance.getCaseDefinitionKey();
        this.caseDefinitionName = historicCaseInstance.getCaseDefinitionName();
        this.caseDefinitionVersion = historicCaseInstance.getCaseDefinitionVersion();
        this.caseDefinitionDeploymentId = historicCaseInstance.getCaseDefinitionDeploymentId();
        this.state = historicCaseInstance.getState();
        this.startTime = historicCaseInstance.getStartTime();
        this.startUserId = historicCaseInstance.getStartUserId();
        this.callbackId = historicCaseInstance.getCallbackId();
        this.callbackType = historicCaseInstance.getCallbackType();
        this.referenceId = historicCaseInstance.getReferenceId();
        this.referenceType = historicCaseInstance.getReferenceType();

        if (historicCaseInstance.getTenantId() != null) {
            this.tenantId = historicCaseInstance.getTenantId();
        }
        this.variableInstances = variables;
    }

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("businessKey", businessKey);
        persistentState.put("businessStatus", businessStatus);
        persistentState.put("name", name);
        persistentState.put("parentId", parentId);
        persistentState.put("caseDefinitionId", caseDefinitionId);
        persistentState.put("state", state);
        persistentState.put("startTime", startTime);
        persistentState.put("startUserId", startUserId);
        persistentState.put("callbackId", callbackId);
        persistentState.put("callbackType", callbackType);
        persistentState.put("referenceId", referenceId);
        persistentState.put("referenceType", referenceType);
        persistentState.put("completeable", completable);
        persistentState.put("tenantId", tenantId);
        persistentState.put("lockTime", lockTime);
        persistentState.put("lockOwner", lockOwner);
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
    public String getBusinessStatus() {
        return businessStatus;
    }
    @Override
    public void setBusinessStatus(String businessStatus) {
        this.businessStatus = businessStatus;
    }
    @Override
    public String getName() {
        if(StringUtils.isNotBlank(localizedName)) {
            return localizedName;
        }
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
    public Date getLastReactivationTime() {
        return lastReactivationTime;
    }
    @Override
    public void setLastReactivationTime(Date lastReactivationTime) {
        this.lastReactivationTime = lastReactivationTime;
    }
    @Override
    public String getLastReactivationUserId() {
        return lastReactivationUserId;
    }
    @Override
    public void setLastReactivationUserId(String lastReactivationUserId) {
        this.lastReactivationUserId = lastReactivationUserId;
    }
    @Override
    public boolean isCompletable() {
        return completable;
    }
    @Override
    public void setCompletable(boolean completable) {
        this.completable = completable;
    }
    /**
     * Only here due to MyBatis and the old typo can be removed, if we would do a DB update
     */
    public boolean isCompleteable() {
        return completable;
    }
    /**
     * Only here due to MyBatis and the old typo can be removed, if we would do a DB update
     */
    public void setCompleteable(boolean completable) {
        this.completable = completable;
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
    public String getTenantId() {
        return tenantId;
    }
    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public Date getLockTime() {
        return lockTime;
    }

    @Override
    public void setLockTime(Date lockTime) {
        this.lockTime = lockTime;
    }

    @Override
    public String getLockOwner() {
        return lockOwner;
    }

    @Override
    public void setLockOwner(String lockOwner) {
        this.lockOwner = lockOwner;
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
        return getVariableServiceConfiguration().getVariableService()
                .findVariableInstanceByScopeIdAndScopeType(id, ScopeTypes.CMMN);
    }

    @Override
    protected VariableScopeImpl getParentVariableScope() {
        // A case instance is the root of variables.
        // In case of parent-child case instances, the variables needs to be defined explicitly in input/output vars
        return null;
    }

    @Override
    protected void initializeVariableInstanceBackPointer(VariableInstanceEntity variableInstance) {
        variableInstance.setScopeId(id);
        variableInstance.setScopeType(ScopeTypes.CMMN);
    }

    @Override
    protected void addLoggingSessionInfo(ObjectNode loggingNode) {
        CmmnLoggingSessionUtil.fillLoggingData(loggingNode, this);
    }

    @Override
    protected VariableInstanceEntity getSpecificVariable(String variableName) {
        return getVariableServiceConfiguration().getVariableService()
                .createInternalVariableInstanceQuery()
                .scopeId(id)
                .withoutSubScopeId()
                .scopeType(ScopeTypes.CMMN)
                .name(variableName)
                .singleResult();
    }

    @Override
    protected List<VariableInstanceEntity> getSpecificVariables(Collection<String> variableNames) {
        return getVariableServiceConfiguration().getVariableService()
                .createInternalVariableInstanceQuery()
                .scopeId(id)
                .withoutSubScopeId()
                .scopeType(ScopeTypes.CMMN)
                .names(variableNames)
                .list();
    }
    
    @Override
    protected VariableInstanceEntity createVariableInstance(String variableName, Object value) {
        VariableInstanceEntity variableInstance = super.createVariableInstance(variableName, value);
        
        VariableListenerSession variableListenerSession = Context.getCommandContext().getSession(VariableListenerSession.class);
        variableListenerSession.addVariableData(variableInstance.getName(), VariableListenerSessionData.VARIABLE_CREATE, 
                variableInstance.getScopeId(), ScopeTypes.CMMN, caseDefinitionId);

        return variableInstance;
    }
    
    @Override
    protected void updateVariableInstance(VariableInstanceEntity variableInstance, Object value) {
        super.updateVariableInstance(variableInstance, value);

        VariableListenerSession variableListenerSession = Context.getCommandContext().getSession(VariableListenerSession.class);
        variableListenerSession.addVariableData(variableInstance.getName(), VariableListenerSessionData.VARIABLE_UPDATE, 
                variableInstance.getScopeId(), ScopeTypes.CMMN, caseDefinitionId);
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
    public Map<String, Object> getCaseVariables() {
        Map<String, Object> caseVariables = new HashMap<>();

        if (this.queryVariables != null) {
            for (VariableInstanceEntity queryVariable : queryVariables) {
                if (queryVariable.getId() != null && queryVariable.getTaskId() == null) {
                    caseVariables.put(queryVariable.getName(), queryVariable.getValue());
                }
            }
        }

        // The variables from the cache have precedence
        if (variableInstances != null) {
            for (String variableName : variableInstances.keySet()) {
                caseVariables.put(variableName, variableInstances.get(variableName).getValue());
            }
        }

        return caseVariables;
    }

    public String getLocalizedName() {
        return localizedName;
    }

    @Override
    public void setLocalizedName(String localizedName) {
        this.localizedName = localizedName;
    }

    @Override
    public List<VariableInstanceEntity> getQueryVariables() {
        if (queryVariables == null && Context.getCommandContext() != null) {
            queryVariables = new VariableInitializingList();
        }
        return queryVariables;
    }

    public void setQueryVariables(List<VariableInstanceEntity> queryVariables) {
        this.queryVariables = queryVariables;
    }

    @Override
    public String getCaseDefinitionKey() {
        return caseDefinitionKey;
    }

    @Override
    public void setCaseDefinitionKey(String caseDefinitionKey) {
        this.caseDefinitionKey = caseDefinitionKey;
    }

    @Override
    public String getCaseDefinitionName() {
        return caseDefinitionName;
    }

    @Override
    public void setCaseDefinitionName(String caseDefinitionName) {
        this.caseDefinitionName = caseDefinitionName;
    }

    @Override
    public Integer getCaseDefinitionVersion() {
        return caseDefinitionVersion;
    }

    @Override
    public void setCaseDefinitionVersion(Integer caseDefinitionVersion) {
        this.caseDefinitionVersion = caseDefinitionVersion;
    }

    @Override
    public String getCaseDefinitionDeploymentId() {
        return caseDefinitionDeploymentId;
    }

    @Override
    public void setCaseDefinitionDeploymentId(String caseDefinitionDeploymentId) {
        this.caseDefinitionDeploymentId = caseDefinitionDeploymentId;
    }
}
