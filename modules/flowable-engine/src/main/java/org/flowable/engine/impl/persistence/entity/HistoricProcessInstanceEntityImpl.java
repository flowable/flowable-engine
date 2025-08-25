/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.engine.impl.persistence.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.runtime.callback.ProcessInstanceState;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInitializingList;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;

/**
 * @author Tom Baeyens
 * @author Christian Stettler
 * @author Joram Barrez
 */
public class HistoricProcessInstanceEntityImpl extends HistoricScopeInstanceEntityImpl implements HistoricProcessInstanceEntity {

    private static final long serialVersionUID = 1L;

    protected String endActivityId;
    protected String businessKey;
    protected String businessStatus;
    protected String startUserId;
    protected String state;
    protected String endUserId;
    protected String startActivityId;
    protected String superProcessInstanceId;
    protected String tenantId = ProcessEngineConfiguration.NO_TENANT_ID;
    protected String name;
    protected String localizedName;
    protected String description;
    protected String localizedDescription;
    protected String processDefinitionKey;
    protected String processDefinitionName;
    protected Integer processDefinitionVersion;
    protected String processDefinitionCategory;
    protected String deploymentId;
    protected String callbackId;
    protected String callbackType;
    protected String referenceId;
    protected String referenceType;
    protected String propagatedStageInstanceId;
    protected List<HistoricVariableInstanceEntity> queryVariables;

    public HistoricProcessInstanceEntityImpl() {

    }

    public HistoricProcessInstanceEntityImpl(ExecutionEntity processInstance) {
        this.id = processInstance.getId();
        this.processInstanceId = processInstance.getId();
        this.businessKey = processInstance.getBusinessKey();
        this.businessStatus = processInstance.getBusinessStatus();
        this.name = processInstance.getName();
        this.processDefinitionId = processInstance.getProcessDefinitionId();
        this.processDefinitionKey = processInstance.getProcessDefinitionKey();
        this.processDefinitionName = processInstance.getProcessDefinitionName();
        this.processDefinitionVersion = processInstance.getProcessDefinitionVersion();
        this.processDefinitionCategory = processInstance.getProcessDefinitionCategory();
        this.deploymentId = processInstance.getDeploymentId();
        this.startActivityId = processInstance.getStartActivityId();
        this.startTime = processInstance.getStartTime();
        this.startUserId = processInstance.getStartUserId();
        this.superProcessInstanceId = processInstance.getSuperExecution() != null ? processInstance.getSuperExecution().getProcessInstanceId() : null;
        this.callbackId = processInstance.getCallbackId();
        this.callbackType = processInstance.getCallbackType();
        this.referenceId = processInstance.getReferenceId();
        this.referenceType = processInstance.getReferenceType();
        this.propagatedStageInstanceId = processInstance.getPropagatedStageInstanceId();
        this.state = ProcessInstanceState.RUNNING;

        // Inherit tenant id (if applicable)
        if (processInstance.getTenantId() != null) {
            this.tenantId = processInstance.getTenantId();
        }
    }

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("startTime", startTime);
        persistentState.put("endTime", endTime);
        persistentState.put("businessKey", businessKey);
        persistentState.put("businessStatus", businessStatus);
        persistentState.put("name", name);
        persistentState.put("durationInMillis", durationInMillis);
        persistentState.put("deleteReason", deleteReason);
        persistentState.put("endActivityId", endActivityId);
        persistentState.put("superProcessInstanceId", superProcessInstanceId);
        persistentState.put("processDefinitionId", processDefinitionId);
        persistentState.put("processDefinitionKey", processDefinitionKey);
        persistentState.put("processDefinitionName", processDefinitionName);
        persistentState.put("processDefinitionVersion", processDefinitionVersion);
        persistentState.put("processDefinitionCategory", processDefinitionCategory);
        persistentState.put("deploymentId", deploymentId);
        persistentState.put("callbackId", callbackId);
        persistentState.put("callbackType", callbackType);
        persistentState.put("referenceId", referenceId);
        persistentState.put("referenceType", referenceType);
        persistentState.put("propagatedStageInstanceId", propagatedStageInstanceId);
        persistentState.put("state", state);
        persistentState.put("endUserId", endUserId);
        return persistentState;
    }

    // getters and setters ////////////////////////////////////////////////////////

    @Override
    public String getEndActivityId() {
        return endActivityId;
    }

    @Override
    public void setEndActivityId(String endActivityId) {
        this.endActivityId = endActivityId;
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
    public String getStartUserId() {
        return startUserId;
    }

    @Override
    public void setStartUserId(String startUserId) {
        this.startUserId = startUserId;
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
    public String getEndUserId() {
        return endUserId;
    }

    @Override
    public void setEndUserId(String endUserId) {
        this.endUserId = endUserId;
    }

    @Override
    public String getStartActivityId() {
        return startActivityId;
    }

    @Override
    public void setStartActivityId(String startUserId) {
        this.startActivityId = startUserId;
    }

    @Override
    public String getSuperProcessInstanceId() {
        return superProcessInstanceId;
    }

    @Override
    public void setSuperProcessInstanceId(String superProcessInstanceId) {
        this.superProcessInstanceId = superProcessInstanceId;
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
    public String getName() {
        if (localizedName != null && localizedName.length() > 0) {
            return localizedName;
        } else {
            return name;
        }
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public String getLocalizedName() {
        return localizedName;
    }

    @Override
    public void setLocalizedName(String localizedName) {
        this.localizedName = localizedName;
    }

    @Override
    public String getDescription() {
        if (localizedDescription != null && localizedDescription.length() > 0) {
            return localizedDescription;
        } else {
            return description;
        }
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocalizedDescription() {
        return localizedDescription;
    }

    @Override
    public void setLocalizedDescription(String localizedDescription) {
        this.localizedDescription = localizedDescription;
    }

    @Override
    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    @Override
    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    @Override
    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    @Override
    public void setProcessDefinitionName(String processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
    }

    @Override
    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    @Override
    public void setProcessDefinitionVersion(Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
    }

    @Override
    public String getProcessDefinitionCategory() {
        return processDefinitionCategory;
    }

    @Override
    public void setProcessDefinitionCategory(String processDefinitionCategory) {
        this.processDefinitionCategory = processDefinitionCategory;
    }

    @Override
    public String getDeploymentId() {
        return deploymentId;
    }

    @Override
    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
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
    public String getPropagatedStageInstanceId() {
        return propagatedStageInstanceId;
    }

    @Override
    public void setPropagatedStageInstanceId(String propagatedStageInstanceId) {
        this.propagatedStageInstanceId = propagatedStageInstanceId;
    }

    @Override
    public Map<String, Object> getProcessVariables() {
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

    @Override
    public void setQueryVariables(List<HistoricVariableInstanceEntity> queryVariables) {
        this.queryVariables = queryVariables;
    }

    // common methods //////////////////////////////////////////////////////////

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HistoricProcessInstanceEntity[id=").append(getId())
                .append(", definition=").append(getProcessDefinitionId());
        if (superProcessInstanceId != null) {
            sb.append(", superProcessInstanceId=").append(superProcessInstanceId);
        }

        if (referenceId != null) {
            sb.append(", referenceId=").append(referenceId)
                    .append(", referenceType=").append(referenceType);
        }

        if (StringUtils.isNotEmpty(tenantId)) {
            sb.append(", tenantId=").append(tenantId);
        }
        sb.append("]");
        return sb.toString();
    }
}
