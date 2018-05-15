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

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInitializingList;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;

/**
 * @author Joram Barrez
 */
public class HistoricCaseInstanceEntityImpl extends AbstractEntity implements HistoricCaseInstanceEntity {
    
    protected String businessKey;
    protected String name;
    protected String parentId;
    protected String caseDefinitionId;
    protected String state;
    protected Date startTime;
    protected Date endTime;
    protected String startUserId;
    protected String callbackId;
    protected String callbackType;
    protected String tenantId = CmmnEngineConfiguration.NO_TENANT_ID;
    protected List<HistoricVariableInstanceEntity> queryVariables;

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("businessKey", businessKey);
        persistentState.put("name", name);
        persistentState.put("parentId", parentId);
        persistentState.put("caseDefinitionId", caseDefinitionId);
        persistentState.put("state", state);
        persistentState.put("startTime", startTime);
        persistentState.put("endTime", endTime);
        persistentState.put("startUserId", startUserId);
        persistentState.put("callbackId", callbackId);
        persistentState.put("callbackType", callbackType);
        persistentState.put("tenantId", tenantId);
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
}
