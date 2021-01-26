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

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.HistoricActivityInstanceQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.HistoryManager;
import org.flowable.engine.impl.persistence.entity.data.HistoricActivityInstanceDataManager;
import org.flowable.engine.runtime.ActivityInstance;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class HistoricActivityInstanceEntityManagerImpl
    extends AbstractProcessEngineEntityManager<HistoricActivityInstanceEntity, HistoricActivityInstanceDataManager>
    implements HistoricActivityInstanceEntityManager {

    public HistoricActivityInstanceEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, HistoricActivityInstanceDataManager historicActivityInstanceDataManager) {
        super(processEngineConfiguration, historicActivityInstanceDataManager);
    }

    @Override
    public HistoricActivityInstanceEntity create(ActivityInstance activityInstance) {
        return dataManager.create(activityInstance);
    }

    @Override
    public List<HistoricActivityInstanceEntity> findUnfinishedHistoricActivityInstancesByExecutionAndActivityId(String executionId, String activityId) {
        return dataManager.findUnfinishedHistoricActivityInstancesByExecutionAndActivityId(executionId, activityId);
    }
    
    @Override
    public List<HistoricActivityInstanceEntity> findHistoricActivityInstancesByExecutionAndActivityId(String executionId, String activityId) {
        return dataManager.findHistoricActivityInstancesByExecutionIdAndActivityId(executionId, activityId);
    }

    @Override
    public List<HistoricActivityInstanceEntity> findUnfinishedHistoricActivityInstancesByProcessInstanceId(String processInstanceId) {
        return dataManager.findUnfinishedHistoricActivityInstancesByProcessInstanceId(processInstanceId);
    }

    @Override
    public void deleteHistoricActivityInstancesByProcessInstanceId(String historicProcessInstanceId) {
        if (getHistoryManager().isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            dataManager.deleteHistoricActivityInstancesByProcessInstanceId(historicProcessInstanceId);
        }
    }

    @Override
    public long findHistoricActivityInstanceCountByQueryCriteria(HistoricActivityInstanceQueryImpl historicActivityInstanceQuery) {
        return dataManager.findHistoricActivityInstanceCountByQueryCriteria(historicActivityInstanceQuery);
    }

    @Override
    public List<HistoricActivityInstance> findHistoricActivityInstancesByQueryCriteria(HistoricActivityInstanceQueryImpl historicActivityInstanceQuery) {
        return dataManager.findHistoricActivityInstancesByQueryCriteria(historicActivityInstanceQuery);
    }

    @Override
    public List<HistoricActivityInstance> findHistoricActivityInstancesByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findHistoricActivityInstancesByNativeQuery(parameterMap);
    }

    @Override
    public long findHistoricActivityInstanceCountByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findHistoricActivityInstanceCountByNativeQuery(parameterMap);
    }
    
    @Override
    public void deleteHistoricActivityInstances(HistoricActivityInstanceQueryImpl historicActivityInstanceQuery) {
        dataManager.deleteHistoricActivityInstances(historicActivityInstanceQuery);
    }
    
    @Override
    public void deleteHistoricActivityInstancesForNonExistingProcessInstances() {
        dataManager.deleteHistoricActivityInstancesForNonExistingProcessInstances();
    }

    protected HistoryManager getHistoryManager() {
        return engineConfiguration.getHistoryManager();
    }

}
