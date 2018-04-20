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
import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
    protected Date activationTime;
    protected Date endTime;
    protected String startUserId;
    protected String referenceId;
    protected String referenceType;
    protected String tenantId = CmmnEngineConfiguration.NO_TENANT_ID;

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
        persistentState.put("activationTime", endTime);
        persistentState.put("endTime", endTime);
        persistentState.put("startUserId", startUserId);
        persistentState.put("referenceId", referenceId);
        persistentState.put("referenceType", referenceType);
        persistentState.put("tenantId", tenantId);
        persistentState.put("planItemDefinitionId", planItemDefinitionId);
        persistentState.put("planItemDefinitionType", planItemDefinitionType);
        return persistentState;
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
    public Date getStartTime() {
        return startTime;
    }

    @Override
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @Override
    public Date getActivationTime() {
        return activationTime;
    }

    @Override
    public void setActivationTime(Date activationTime) {
        this.activationTime = activationTime;
    }

    @Override
    public Date getEndTime() {
        return endTime;
    }

    @Override
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
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
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

}
