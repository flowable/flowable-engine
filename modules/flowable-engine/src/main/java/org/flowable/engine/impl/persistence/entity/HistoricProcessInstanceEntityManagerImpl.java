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

package org.flowable.engine.impl.persistence.entity;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.HistoricProcessInstanceQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.data.HistoricProcessInstanceDataManager;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class HistoricProcessInstanceEntityManagerImpl extends AbstractEntityManager<HistoricProcessInstanceEntity> implements HistoricProcessInstanceEntityManager {

    protected HistoricProcessInstanceDataManager historicProcessInstanceDataManager;

    public HistoricProcessInstanceEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, HistoricProcessInstanceDataManager historicProcessInstanceDataManager) {
        super(processEngineConfiguration);
        this.historicProcessInstanceDataManager = historicProcessInstanceDataManager;
    }

    @Override
    protected DataManager<HistoricProcessInstanceEntity> getDataManager() {
        return historicProcessInstanceDataManager;
    }

    @Override
    public HistoricProcessInstanceEntity create(ExecutionEntity processInstanceExecutionEntity) {
        return historicProcessInstanceDataManager.create(processInstanceExecutionEntity);
    }

    @Override
    public long findHistoricProcessInstanceCountByQueryCriteria(HistoricProcessInstanceQueryImpl historicProcessInstanceQuery) {
        if (getHistoryManager().isHistoryEnabled()) {
            return historicProcessInstanceDataManager.findHistoricProcessInstanceCountByQueryCriteria(historicProcessInstanceQuery);
        }
        return 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricProcessInstance> findHistoricProcessInstancesByQueryCriteria(HistoricProcessInstanceQueryImpl historicProcessInstanceQuery) {
        if (getHistoryManager().isHistoryEnabled()) {
            return historicProcessInstanceDataManager.findHistoricProcessInstancesByQueryCriteria(historicProcessInstanceQuery);
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricProcessInstance> findHistoricProcessInstancesAndVariablesByQueryCriteria(HistoricProcessInstanceQueryImpl historicProcessInstanceQuery) {
        if (getHistoryManager().isHistoryEnabled()) {
            return historicProcessInstanceDataManager.findHistoricProcessInstancesAndVariablesByQueryCriteria(historicProcessInstanceQuery);
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<HistoricProcessInstance> findHistoricProcessInstancesByNativeQuery(Map<String, Object> parameterMap) {
        return historicProcessInstanceDataManager.findHistoricProcessInstancesByNativeQuery(parameterMap);
    }
    
    @Override
    public List<HistoricProcessInstance> findHistoricProcessInstancesBySuperProcessInstanceId(String historicProcessInstanceId) {
        return historicProcessInstanceDataManager.findHistoricProcessInstancesBySuperProcessInstanceId(historicProcessInstanceId);
    }
    
    @Override
    public List<String> findHistoricProcessInstanceIdsByProcessDefinitionId(String processDefinitionId) {
        return historicProcessInstanceDataManager.findHistoricProcessInstanceIdsByProcessDefinitionId(processDefinitionId);
    }

    @Override
    public long findHistoricProcessInstanceCountByNativeQuery(Map<String, Object> parameterMap) {
        return historicProcessInstanceDataManager.findHistoricProcessInstanceCountByNativeQuery(parameterMap);
    }

    public HistoricProcessInstanceDataManager getHistoricProcessInstanceDataManager() {
        return historicProcessInstanceDataManager;
    }

    public void setHistoricProcessInstanceDataManager(HistoricProcessInstanceDataManager historicProcessInstanceDataManager) {
        this.historicProcessInstanceDataManager = historicProcessInstanceDataManager;
    }

}
