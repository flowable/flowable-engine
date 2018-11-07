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
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.HistoricVariableInstanceQueryImpl;
import org.flowable.variable.service.impl.persistence.entity.data.HistoricVariableInstanceDataManager;

/**
 * @author Christian Lipphardt (camunda)
 * @author Joram Barrez
 */
public class HistoricVariableInstanceEntityManagerImpl extends AbstractEntityManager<HistoricVariableInstanceEntity> implements HistoricVariableInstanceEntityManager {

    protected HistoricVariableInstanceDataManager historicVariableInstanceDataManager;

    public HistoricVariableInstanceEntityManagerImpl(VariableServiceConfiguration variableServiceConfiguration, HistoricVariableInstanceDataManager historicVariableInstanceDataManager) {
        super(variableServiceConfiguration);
        this.historicVariableInstanceDataManager = historicVariableInstanceDataManager;
    }

    @Override
    protected DataManager<HistoricVariableInstanceEntity> getDataManager() {
        return historicVariableInstanceDataManager;
    }

    @Override
    public HistoricVariableInstanceEntity createAndInsert(VariableInstanceEntity variableInstance) {
        HistoricVariableInstanceEntity historicVariableInstance = historicVariableInstanceDataManager.create();
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

        copyVariableValue(historicVariableInstance, variableInstance);

        Date time = getClock().getCurrentTime();
        historicVariableInstance.setCreateTime(time);
        historicVariableInstance.setLastUpdatedTime(time);

        insert(historicVariableInstance);

        return historicVariableInstance;
    }

    @Override
    public void copyVariableValue(HistoricVariableInstanceEntity historicVariableInstance, VariableInstanceEntity variableInstance) {
        historicVariableInstance.setTextValue(variableInstance.getTextValue());
        historicVariableInstance.setTextValue2(variableInstance.getTextValue2());
        historicVariableInstance.setDoubleValue(variableInstance.getDoubleValue());
        historicVariableInstance.setLongValue(variableInstance.getLongValue());

        historicVariableInstance.setVariableType(variableInstance.getType());
        if (variableInstance.getByteArrayRef() != null) {
            historicVariableInstance.setBytes(variableInstance.getBytes());
        }

        historicVariableInstance.setLastUpdatedTime(getClock().getCurrentTime());
    }

    @Override
    public void delete(HistoricVariableInstanceEntity entity, boolean fireDeleteEvent) {
        super.delete(entity, fireDeleteEvent);

        if (entity.getByteArrayRef() != null) {
            entity.getByteArrayRef().delete();
        }
    }

    @Override
    public void deleteHistoricVariableInstanceByProcessInstanceId(final String historicProcessInstanceId) {
        if (getVariableServiceConfiguration().isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            List<HistoricVariableInstanceEntity> historicProcessVariables = historicVariableInstanceDataManager.findHistoricVariableInstancesByProcessInstanceId(historicProcessInstanceId);
            for (HistoricVariableInstanceEntity historicProcessVariable : historicProcessVariables) {
                delete(historicProcessVariable);
            }
        }
    }

    @Override
    public long findHistoricVariableInstanceCountByQueryCriteria(HistoricVariableInstanceQueryImpl historicProcessVariableQuery) {
        return historicVariableInstanceDataManager.findHistoricVariableInstanceCountByQueryCriteria(historicProcessVariableQuery);
    }

    @Override
    public List<HistoricVariableInstance> findHistoricVariableInstancesByQueryCriteria(HistoricVariableInstanceQueryImpl historicProcessVariableQuery) {
        return historicVariableInstanceDataManager.findHistoricVariableInstancesByQueryCriteria(historicProcessVariableQuery);
    }

    @Override
    public HistoricVariableInstanceEntity findHistoricVariableInstanceByVariableInstanceId(String variableInstanceId) {
        return historicVariableInstanceDataManager.findHistoricVariableInstanceByVariableInstanceId(variableInstanceId);
    }
    
    @Override
    public List<HistoricVariableInstanceEntity> findHistoricalVariableInstancesByScopeIdAndScopeType(String scopeId, String scopeType) {
        return historicVariableInstanceDataManager.findHistoricalVariableInstancesByScopeIdAndScopeType(scopeId, scopeType);
    }
    
    @Override
    public List<HistoricVariableInstanceEntity> findHistoricalVariableInstancesBySubScopeIdAndScopeType(String subScopeId, String scopeType) {
        return historicVariableInstanceDataManager.findHistoricalVariableInstancesBySubScopeIdAndScopeType(subScopeId, scopeType);
    }

    @Override
    public void deleteHistoricVariableInstancesByTaskId(String taskId) {
        if (getVariableServiceConfiguration().isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            List<HistoricVariableInstanceEntity> historicProcessVariables = historicVariableInstanceDataManager.findHistoricVariableInstancesByTaskId(taskId);
            for (HistoricVariableInstanceEntity historicProcessVariable : historicProcessVariables) {
                delete(historicProcessVariable);
            }
        }
    }

    @Override
    public List<HistoricVariableInstance> findHistoricVariableInstancesByNativeQuery(Map<String, Object> parameterMap) {
        return historicVariableInstanceDataManager.findHistoricVariableInstancesByNativeQuery(parameterMap);
    }

    @Override
    public long findHistoricVariableInstanceCountByNativeQuery(Map<String, Object> parameterMap) {
        return historicVariableInstanceDataManager.findHistoricVariableInstanceCountByNativeQuery(parameterMap);
    }

    public HistoricVariableInstanceDataManager getHistoricVariableInstanceDataManager() {
        return historicVariableInstanceDataManager;
    }

    public void setHistoricVariableInstanceDataManager(HistoricVariableInstanceDataManager historicVariableInstanceDataManager) {
        this.historicVariableInstanceDataManager = historicVariableInstanceDataManager;
    }

}
