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

package org.activiti.engine.impl.persistence.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.db.BulkDeleteable;
import org.activiti.engine.impl.identity.Authentication;

/**
 * @author Tom Baeyens
 * @author Christian Stettler
 * @author Joram Barrez
 */
public class HistoricProcessInstanceEntity extends HistoricScopeInstanceEntity implements HistoricProcessInstance, BulkDeleteable {

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
    protected List<HistoricVariableInstanceEntity> queryVariables;

    public HistoricProcessInstanceEntity() {
    }

    public HistoricProcessInstanceEntity(ExecutionEntity processInstance) {
        id = processInstance.getId();
        processInstanceId = processInstance.getId();
        businessKey = processInstance.getBusinessKey();
        processDefinitionId = processInstance.getProcessDefinitionId();
        processDefinitionKey = processInstance.getProcessDefinitionKey();
        processDefinitionName = processInstance.getProcessDefinitionName();
        processDefinitionVersion = processInstance.getProcessDefinitionVersion();
        deploymentId = processInstance.getDeploymentId();
        startTime = Context.getProcessEngineConfiguration().getClock().getCurrentTime();
        startUserId = Authentication.getAuthenticatedUserId();
        startActivityId = processInstance.getActivityId();
        superProcessInstanceId = processInstance.getSuperExecution() != null ? processInstance.getSuperExecution().getProcessInstanceId() : null;

        // Inherit tenant id (if applicable)
        if (processInstance.getTenantId() != null) {
            tenantId = processInstance.getTenantId();
        }
    }

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("endTime", endTime);
        persistentState.put("businessKey", businessKey);
        persistentState.put("name", name);
        persistentState.put("durationInMillis", durationInMillis);
        persistentState.put("deleteReason", deleteReason);
        persistentState.put("endStateName", endActivityId);
        persistentState.put("superProcessInstanceId", superProcessInstanceId);
        persistentState.put("processDefinitionId", processDefinitionId);
        persistentState.put("processDefinitionKey", processDefinitionKey);
        persistentState.put("processDefinitionName", processDefinitionName);
        persistentState.put("processDefinitionVersion", processDefinitionVersion);
        persistentState.put("deploymentId", deploymentId);
        return persistentState;
    }

    // getters and setters //////////////////////////////////////////////////////

    @Override
    public String getEndActivityId() {
        return endActivityId;
    }

    public void setEndActivityId(String endActivityId) {
        this.endActivityId = endActivityId;
    }

    @Override
    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    @Override
    public String getStartUserId() {
        return startUserId;
    }

    public void setStartUserId(String startUserId) {
        this.startUserId = startUserId;
    }

    @Override
    public String getStartActivityId() {
        return startActivityId;
    }

    public void setStartActivityId(String startUserId) {
        this.startActivityId = startUserId;
    }

    @Override
    public String getSuperProcessInstanceId() {
        return superProcessInstanceId;
    }

    public void setSuperProcessInstanceId(String superProcessInstanceId) {
        this.superProcessInstanceId = superProcessInstanceId;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

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

    public List<HistoricVariableInstanceEntity> getQueryVariables() {
        if (queryVariables == null && Context.getCommandContext() != null) {
            queryVariables = new HistoricVariableInitializingList();
        }
        return queryVariables;
    }

    public void setQueryVariables(List<HistoricVariableInstanceEntity> queryVariables) {
        this.queryVariables = queryVariables;
    }

    // common methods //////////////////////////////////////////////////////////

    @Override
    public String toString() {
        return "HistoricProcessInstanceEntity[superProcessInstanceId=" + superProcessInstanceId + "]";
    }
}
