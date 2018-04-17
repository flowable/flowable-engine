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

import org.flowable.common.engine.impl.context.Context;
import org.flowable.engine.ProcessEngineConfiguration;
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
    protected String startUserId;
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
    protected String deploymentId;
    protected String callbackId;
    protected String callbackType;
    protected List<HistoricVariableInstanceEntity> queryVariables;

    public HistoricProcessInstanceEntityImpl() {

    }

    public HistoricProcessInstanceEntityImpl(ExecutionEntity processInstance) {
        id = processInstance.getId();
        processInstanceId = processInstance.getId();
        businessKey = processInstance.getBusinessKey();
        name = processInstance.getName();
        processDefinitionId = processInstance.getProcessDefinitionId();
        processDefinitionKey = processInstance.getProcessDefinitionKey();
        processDefinitionName = processInstance.getProcessDefinitionName();
        processDefinitionVersion = processInstance.getProcessDefinitionVersion();
        deploymentId = processInstance.getDeploymentId();
        startActivityId = processInstance.getStartActivityId();
        startTime = processInstance.getStartTime();
        startUserId = processInstance.getStartUserId();
        superProcessInstanceId = processInstance.getSuperExecution() != null ? processInstance.getSuperExecution().getProcessInstanceId() : null;
        callbackId = processInstance.getCallbackId();
        callbackType = processInstance.getCallbackType();
        
        // Inherit tenant id (if applicable)
        if (processInstance.getTenantId() != null) {
            tenantId = processInstance.getTenantId();
        }
    }

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("startTime", startTime);
        persistentState.put("endTime", endTime);
        persistentState.put("businessKey", businessKey);
        persistentState.put("name", name);
        persistentState.put("durationInMillis", durationInMillis);
        persistentState.put("deleteReason", deleteReason);
        persistentState.put("endActivityId", endActivityId);
        persistentState.put("superProcessInstanceId", superProcessInstanceId);
        persistentState.put("processDefinitionId", processDefinitionId);
        persistentState.put("processDefinitionKey", processDefinitionKey);
        persistentState.put("processDefinitionName", processDefinitionName);
        persistentState.put("processDefinitionVersion", processDefinitionVersion);
        persistentState.put("deploymentId", deploymentId);
        persistentState.put("callbackId", callbackId);
        persistentState.put("callbackType", callbackType);
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
    public String getStartUserId() {
        return startUserId;
    }

    @Override
    public void setStartUserId(String startUserId) {
        this.startUserId = startUserId;
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
        return "HistoricProcessInstanceEntity[superProcessInstanceId=" + superProcessInstanceId + "]";
    }
}
