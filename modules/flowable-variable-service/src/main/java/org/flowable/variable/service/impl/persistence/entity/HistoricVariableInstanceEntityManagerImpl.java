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

package org.flowable.variable.service.impl.persistence.entity;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.persistence.entity.AbstractServiceEngineEntityManager;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.HistoricVariableInstanceQueryImpl;
import org.flowable.variable.service.impl.persistence.entity.data.HistoricVariableInstanceDataManager;

/**
 * @author Christian Lipphardt (camunda)
 * @author Joram Barrez
 */
public class HistoricVariableInstanceEntityManagerImpl
    extends AbstractServiceEngineEntityManager<VariableServiceConfiguration, HistoricVariableInstanceEntity, HistoricVariableInstanceDataManager>
    implements HistoricVariableInstanceEntityManager {

    public HistoricVariableInstanceEntityManagerImpl(VariableServiceConfiguration variableServiceConfiguration, HistoricVariableInstanceDataManager historicVariableInstanceDataManager) {
        super(variableServiceConfiguration, variableServiceConfiguration.getEngineName(), historicVariableInstanceDataManager);
    }

    @Override
    public HistoricVariableInstanceEntity create(VariableInstanceEntity variableInstance, Date createTime) {
        HistoricVariableInstanceEntity historicVariableInstance = dataManager.create();
        historicVariableInstance.setId(variableInstance.getId());
        historicVariableInstance.setProcessInstanceId(variableInstance.getProcessInstanceId());
        historicVariableInstance.setExecutionId(variableInstance.getExecutionId());
        historicVariableInstance.setTaskId(variableInstance.getTaskId());
        historicVariableInstance.setRevision(variableInstance.getRevision());
        historicVariableInstance.setName(variableInstance.getName());
        historicVariableInstance.setVariableType(variableInstance.getType());
        historicVariableInstance.setScopeId(variableInstance.getScopeId());
        historicVariableInstance.setSubScopeId(variableInstance.getSubScopeId());
        historicVariableInstance.setScopeType(variableInstance.getScopeType());

        copyVariableValue(historicVariableInstance, variableInstance, createTime);

        historicVariableInstance.setCreateTime(createTime);
        historicVariableInstance.setLastUpdatedTime(createTime);

        return historicVariableInstance;
    }

    @Override
    public HistoricVariableInstanceEntity createAndInsert(VariableInstanceEntity variableInstance, Date createTime) {
        HistoricVariableInstanceEntity historicVariableInstance = create(variableInstance, createTime);

        insert(historicVariableInstance);

        return historicVariableInstance;
    }

    @Override
    public void copyVariableValue(HistoricVariableInstanceEntity historicVariableInstance, VariableInstanceEntity variableInstance, Date updateTime) {
        historicVariableInstance.setTextValue(variableInstance.getTextValue());
        historicVariableInstance.setTextValue2(variableInstance.getTextValue2());
        historicVariableInstance.setDoubleValue(variableInstance.getDoubleValue());
        historicVariableInstance.setLongValue(variableInstance.getLongValue());

        historicVariableInstance.setVariableType(variableInstance.getType());
        if (variableInstance.getByteArrayRef() != null) {
            historicVariableInstance.setBytes(variableInstance.getBytes());
        }

        historicVariableInstance.setLastUpdatedTime(updateTime);
    }

    @Override
    public void delete(HistoricVariableInstanceEntity entity, boolean fireDeleteEvent) {
        super.delete(entity, fireDeleteEvent);

        if (entity.getByteArrayRef() != null) {
            entity.getByteArrayRef().delete(serviceConfiguration.getEngineName());
        }
    }

    @Override
    public void deleteHistoricVariableInstanceByProcessInstanceId(final String historicProcessInstanceId) {
        if (serviceConfiguration.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            List<HistoricVariableInstanceEntity> historicProcessVariables = dataManager.findHistoricVariableInstancesByProcessInstanceId(historicProcessInstanceId);
            for (HistoricVariableInstanceEntity historicProcessVariable : historicProcessVariables) {
                delete(historicProcessVariable);
            }
        }
    }

    @Override
    public long findHistoricVariableInstanceCountByQueryCriteria(HistoricVariableInstanceQueryImpl historicProcessVariableQuery) {
        return dataManager.findHistoricVariableInstanceCountByQueryCriteria(historicProcessVariableQuery);
    }

    @Override
    public List<HistoricVariableInstance> findHistoricVariableInstancesByQueryCriteria(HistoricVariableInstanceQueryImpl historicProcessVariableQuery) {
        return dataManager.findHistoricVariableInstancesByQueryCriteria(historicProcessVariableQuery);
    }

    @Override
    public HistoricVariableInstanceEntity findHistoricVariableInstanceByVariableInstanceId(String variableInstanceId) {
        return dataManager.findHistoricVariableInstanceByVariableInstanceId(variableInstanceId);
    }
    
    @Override
    public List<HistoricVariableInstanceEntity> findHistoricalVariableInstancesByProcessInstanceId(String processInstanceId) {
        return dataManager.findHistoricVariableInstancesByProcessInstanceId(processInstanceId);
    }

    @Override
    public List<HistoricVariableInstanceEntity> findHistoricalVariableInstancesByTaskId(String taskId) {
        return dataManager.findHistoricVariableInstancesByTaskId(taskId);
    }

    @Override
    public List<HistoricVariableInstanceEntity> findHistoricalVariableInstancesByScopeIdAndScopeType(String scopeId, String scopeType) {
        return dataManager.findHistoricalVariableInstancesByScopeIdAndScopeType(scopeId, scopeType);
    }
    
    @Override
    public List<HistoricVariableInstanceEntity> findHistoricalVariableInstancesBySubScopeIdAndScopeType(String subScopeId, String scopeType) {
        return dataManager.findHistoricalVariableInstancesBySubScopeIdAndScopeType(subScopeId, scopeType);
    }

    @Override
    public void deleteHistoricVariableInstancesByTaskId(String taskId) {
        if (serviceConfiguration.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            List<HistoricVariableInstanceEntity> historicProcessVariables = dataManager.findHistoricVariableInstancesByTaskId(taskId);
            for (HistoricVariableInstanceEntity historicProcessVariable : historicProcessVariables) {
                delete(historicProcessVariable);
            }
        }
    }
    
    @Override
    public void deleteHistoricVariableInstancesForNonExistingProcessInstances() {
        if (serviceConfiguration.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            dataManager.deleteHistoricVariableInstancesForNonExistingProcessInstances();
        }
    }
    
    @Override
    public void deleteHistoricVariableInstancesForNonExistingCaseInstances() {
        if (serviceConfiguration.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            dataManager.deleteHistoricVariableInstancesForNonExistingCaseInstances();
        }
    }

    @Override
    public List<HistoricVariableInstance> findHistoricVariableInstancesByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findHistoricVariableInstancesByNativeQuery(parameterMap);
    }

    @Override
    public long findHistoricVariableInstanceCountByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findHistoricVariableInstanceCountByNativeQuery(parameterMap);
    }

}
