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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInitializingList;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;

/**
 * @author Joram Barrez
 */
public class HistoricCaseInstanceEntityImpl extends AbstractCmmnEngineEntity implements HistoricCaseInstanceEntity {
    
    protected String businessKey;
    protected String businessStatus;
    protected String name;
    protected String parentId;
    protected String caseDefinitionId;
    protected String state;
    protected Date startTime;
    protected Date endTime;
    protected String startUserId;
    protected Date lastReactivationTime;
    protected String lastReactivationUserId;
    protected String callbackId;
    protected String callbackType;
    protected String referenceId;
    protected String referenceType;
    protected String tenantId = CmmnEngineConfiguration.NO_TENANT_ID;
    protected List<HistoricVariableInstanceEntity> queryVariables;
    protected String endUserId;

    protected String localizedName;
    
    // non persisted
    protected String caseDefinitionKey;
    protected String caseDefinitionName;
    protected Integer caseDefinitionVersion;
    protected String caseDefinitionDeploymentId;

    public HistoricCaseInstanceEntityImpl() {

    }

    public HistoricCaseInstanceEntityImpl(CaseInstance caseInstance) {
        this.id = caseInstance.getId();
        this.businessKey = caseInstance.getBusinessKey();
        this.businessStatus = caseInstance.getBusinessStatus();
        this.name = caseInstance.getName();
        this.parentId = caseInstance.getParentId();
        this.caseDefinitionId = caseInstance.getCaseDefinitionId();
        this.caseDefinitionKey = caseInstance.getCaseDefinitionKey();
        this.caseDefinitionName = caseInstance.getCaseDefinitionName();
        this.caseDefinitionVersion = caseInstance.getCaseDefinitionVersion();
        this.caseDefinitionDeploymentId = caseInstance.getCaseDefinitionDeploymentId();
        this.state = caseInstance.getState();
        this.startTime = caseInstance.getStartTime();
        this.startUserId = caseInstance.getStartUserId();
        this.callbackId = caseInstance.getCallbackId();
        this.callbackType = caseInstance.getCallbackType();
        this.referenceId = caseInstance.getReferenceId();
        this.referenceType = caseInstance.getReferenceType();

        if (caseInstance.getTenantId() != null) {
            this.tenantId = caseInstance.getTenantId();
        }
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
        persistentState.put("endTime", endTime);
        persistentState.put("startUserId", startUserId);
        persistentState.put("callbackId", callbackId);
        persistentState.put("callbackType", callbackType);
        persistentState.put("referenceId", referenceId);
        persistentState.put("referenceType", referenceType);
        persistentState.put("tenantId", tenantId);
        persistentState.put("endUserId", endUserId);
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
    public String getEndUserId() {
        return endUserId;
    }
    @Override
    public void setEndUserId(String endUserId) {
        this.endUserId = endUserId;
    }

    @Override
    public Map<String, Object> getCaseVariables() {
        Map<String, Object> variables = new HashMap<>();
        if (queryVariables != null) {
            for (HistoricVariableInstanceEntity variableInstance : queryVariables) {
                if (variableInstance.getId() != null && variableInstance.getTaskId() == null) {
                    variables.put(variableInstance.getName(), variableInstance.getValue());
                }
            }
        }
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

    public String getLocalizedName() {
        return localizedName;
    }

    @Override
    public void setLocalizedName(String localizedName) {
        this.localizedName = localizedName;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HistoricCaseInstance[id=").append(id)
                .append(", caseDefinitionId=").append(caseDefinitionId);

        if (StringUtils.isNotEmpty(tenantId)) {
            sb.append(", tenantId=").append(tenantId);
        }

        return sb.toString();
    }
}
