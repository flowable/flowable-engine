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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.HistoricProcessInstanceQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.HistoryManager;
import org.flowable.engine.impl.persistence.entity.data.HistoricProcessInstanceDataManager;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class HistoricProcessInstanceEntityManagerImpl
        extends AbstractProcessEngineEntityManager<HistoricProcessInstanceEntity, HistoricProcessInstanceDataManager>
        implements HistoricProcessInstanceEntityManager {

    public HistoricProcessInstanceEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, HistoricProcessInstanceDataManager historicProcessInstanceDataManager) {
        super(processEngineConfiguration, historicProcessInstanceDataManager);
    }

    @Override
    public HistoricProcessInstanceEntity create(ExecutionEntity processInstanceExecutionEntity) {
        return dataManager.create(processInstanceExecutionEntity);
    }

    @Override
    public long findHistoricProcessInstanceCountByQueryCriteria(HistoricProcessInstanceQueryImpl historicProcessInstanceQuery) {
        return dataManager.findHistoricProcessInstanceCountByQueryCriteria(historicProcessInstanceQuery);
    }

    @Override
    public List<HistoricProcessInstance> findHistoricProcessInstancesByQueryCriteria(HistoricProcessInstanceQueryImpl historicProcessInstanceQuery) {
        return dataManager.findHistoricProcessInstancesByQueryCriteria(historicProcessInstanceQuery);
    }

    @Override
    public List<HistoricProcessInstance> findHistoricProcessInstancesAndVariablesByQueryCriteria(HistoricProcessInstanceQueryImpl historicProcessInstanceQuery) {
        return dataManager.findHistoricProcessInstancesAndVariablesByQueryCriteria(historicProcessInstanceQuery);
    }
    
    @Override
    public List<HistoricProcessInstance> findHistoricProcessInstanceIdsByQueryCriteria(HistoricProcessInstanceQueryImpl historicProcessInstanceQuery) {
        return dataManager.findHistoricProcessInstanceIdsByQueryCriteria(historicProcessInstanceQuery);
    }

    @Override
    public List<HistoricProcessInstance> findHistoricProcessInstancesByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findHistoricProcessInstancesByNativeQuery(parameterMap);
    }
    
    @Override
    public List<HistoricProcessInstance> findHistoricProcessInstancesBySuperProcessInstanceId(String historicProcessInstanceId) {
        return dataManager.findHistoricProcessInstancesBySuperProcessInstanceId(historicProcessInstanceId);
    }
    
    @Override
    public List<String> findHistoricProcessInstanceIdsBySuperProcessInstanceIds(Collection<String> superProcessInstanceIds) {
        return dataManager.findHistoricProcessInstanceIdsBySuperProcessInstanceIds(superProcessInstanceIds);
    }

    @Override
    public List<String> findHistoricProcessInstanceIdsByProcessDefinitionId(String processDefinitionId) {
        return dataManager.findHistoricProcessInstanceIdsByProcessDefinitionId(processDefinitionId);
    }

    @Override
    public long findHistoricProcessInstanceCountByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findHistoricProcessInstanceCountByNativeQuery(parameterMap);
    }
    
    @Override
    public void deleteHistoricProcessInstances(HistoricProcessInstanceQueryImpl historicProcessInstanceQuery) {
        dataManager.deleteHistoricProcessInstances(historicProcessInstanceQuery);
    }
    
    @Override
    public void bulkDeleteHistoricProcessInstances(Collection<String> processInstanceIds) {
        dataManager.bulkDeleteHistoricProcessInstances(processInstanceIds);
    }

    protected HistoryManager getHistoryManager() {
        return engineConfiguration.getHistoryManager();
    }

}
