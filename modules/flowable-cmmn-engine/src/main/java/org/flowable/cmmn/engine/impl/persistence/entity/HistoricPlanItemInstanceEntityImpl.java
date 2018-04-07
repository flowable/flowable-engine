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

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.engine.common.api.scope.ScopeTypes;
import org.flowable.engine.common.impl.context.Context;
import org.flowable.engine.common.impl.persistence.entity.AbstractEntity;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInitializingList;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Dennis Federico
 */
public class HistoricPlanItemInstanceEntityImpl extends AbstractEntity implements HistoricPlanItemInstanceEntity {

    protected String name;
    protected String state;
    protected String caseDefinitionId;
    protected String caseInstanceId;
    protected String stageInstanceId;
    protected boolean isStage;
    protected String elementId;
    protected String planItemDefinitionId;
    protected String planItemDefinitionType;
    protected Date startTime;
    protected Date endTime;
    protected String startUserId;
    protected String referenceId;
    protected String referenceType;
    protected String tenantId = CmmnEngineConfiguration.NO_TENANT_ID;
    protected List<HistoricVariableInstanceEntity> queryVariables;

    protected boolean countEnabled;
    protected int variableCount;
    protected int sentryPartInstanceCount;

    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("caseDefinitionId", caseDefinitionId);
        persistentState.put("caseInstanceId", caseInstanceId);
        persistentState.put("stageInstanceId", stageInstanceId);
        persistentState.put("isStage", isStage);
        persistentState.put("elementId", elementId);
        persistentState.put("name", name);
        persistentState.put("state", state);
        persistentState.put("startTime", startTime);
        persistentState.put("endTime", endTime);
        persistentState.put("startUserId", startUserId);
        persistentState.put("referenceId", referenceId);
        persistentState.put("referenceType", referenceType);
        persistentState.put("tenantId", tenantId);
        persistentState.put("planItemDefinitionId", planItemDefinitionId);
        persistentState.put("planItemDefinitionType", planItemDefinitionType);
        persistentState.put("countEnabled", countEnabled);
        persistentState.put("variableCount", variableCount);
        persistentState.put("sentryPartInstanceCount", sentryPartInstanceCount);
        return persistentState;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }

    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public void setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }

    public String getStageInstanceId() {
        return stageInstanceId;
    }

    public void setStageInstanceId(String stageInstanceId) {
        this.stageInstanceId = stageInstanceId;
    }

    public boolean isStage() {
        return isStage;
    }

    public void setStage(boolean isStage) {
        this.isStage = isStage;
    }

    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    public String getPlanItemDefinitionId() {
        return planItemDefinitionId;
    }

    public void setPlanItemDefinitionId(String planItemDefinitionId) {
        this.planItemDefinitionId = planItemDefinitionId;
    }

    public String getPlanItemDefinitionType() {
        return planItemDefinitionType;
    }

    public void setPlanItemDefinitionType(String planItemDefinitionType) {
        this.planItemDefinitionType = planItemDefinitionType;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getStartUserId() {
        return startUserId;
    }

    public void setStartUserId(String startUserId) {
        this.startUserId = startUserId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public Map<String, Object> getPlanItemVariables() {

        Map<String, Object> variables = Optional.ofNullable(queryVariables)
                .map(Collection::stream)
                .orElse(Stream.empty())
                .filter(var -> Objects.nonNull(var.getId()))
                .filter(var -> var.getTaskId() == null)
                .filter(var -> Objects.nonNull(var.getScopeType()) && ScopeTypes.CMMN.equals(var.getScopeType()))
                .filter(var -> Objects.nonNull(var.getScopeId()) && caseInstanceId.equals(var.getScopeId()))
                .filter(var -> Objects.nonNull(var.getSubScopeId()) && id.equals(var.getSubScopeId()))
                .collect(Collectors.toMap(HistoricVariableInstanceEntity::getName, HistoricVariableInstance::getValue));
        return variables;
    }

    @Override
    public List<HistoricVariableInstanceEntity> getQueryVariables() {
        if (queryVariables == null && Context.getCommandContext() != null) {
            queryVariables = new HistoricVariableInitializingList();
        }
        return queryVariables;
    }

    public void setQueryVariables(List<HistoricVariableInstanceEntity> queryVariables) {
        this.queryVariables = queryVariables;
    }

    public boolean isCountEnabled() {
        return countEnabled;
    }

    public void setCountEnabled(boolean countEnabled) {
        this.countEnabled = countEnabled;
    }

    public int getVariableCount() {
        return variableCount;
    }

    public void setVariableCount(int variableCount) {
        this.variableCount = variableCount;
    }

    public int getSentryPartInstanceCount() {
        return sentryPartInstanceCount;
    }

    public void setSentryPartInstanceCount(int sentryPartInstanceCount) {
        this.sentryPartInstanceCount = sentryPartInstanceCount;
    }

}
