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

package org.flowable.task.service.impl.persistence.entity;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.HistoricTaskInstanceQueryImpl;
import org.flowable.task.service.impl.persistence.entity.data.HistoricTaskInstanceDataManager;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class HistoricTaskInstanceEntityManagerImpl extends AbstractEntityManager<HistoricTaskInstanceEntity> implements HistoricTaskInstanceEntityManager {

    protected HistoricTaskInstanceDataManager historicTaskInstanceDataManager;

    public HistoricTaskInstanceEntityManagerImpl(TaskServiceConfiguration taskServiceConfiguration, HistoricTaskInstanceDataManager historicTaskInstanceDataManager) {
        super(taskServiceConfiguration);
        this.historicTaskInstanceDataManager = historicTaskInstanceDataManager;
    }

    @Override
    protected DataManager<HistoricTaskInstanceEntity> getDataManager() {
        return historicTaskInstanceDataManager;
    }

    @Override
    public HistoricTaskInstanceEntity create(TaskEntity task) {
        return historicTaskInstanceDataManager.create(task);
    }
    
    @Override
    public List<HistoricTaskInstanceEntity> findHistoricTasksByParentTaskId(String parentTaskId) {
        return historicTaskInstanceDataManager.findHistoricTasksByParentTaskId(parentTaskId);
    }
    
    @Override
    public List<HistoricTaskInstanceEntity> findHistoricTasksByProcessInstanceId(String processInstanceId) {
        return historicTaskInstanceDataManager.findHistoricTasksByProcessInstanceId(processInstanceId);
    }

    @Override
    public long findHistoricTaskInstanceCountByQueryCriteria(HistoricTaskInstanceQueryImpl historicTaskInstanceQuery) {
        if (taskServiceConfiguration.isHistoryEnabled()) {
            return historicTaskInstanceDataManager.findHistoricTaskInstanceCountByQueryCriteria(historicTaskInstanceQuery);
        }
        return 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricTaskInstance> findHistoricTaskInstancesByQueryCriteria(HistoricTaskInstanceQueryImpl historicTaskInstanceQuery) {
        if (taskServiceConfiguration.isHistoryEnabled()) {
            return historicTaskInstanceDataManager.findHistoricTaskInstancesByQueryCriteria(historicTaskInstanceQuery);
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricTaskInstance> findHistoricTaskInstancesAndRelatedEntitiesByQueryCriteria(HistoricTaskInstanceQueryImpl historicTaskInstanceQuery) {
        if (taskServiceConfiguration.isHistoryEnabled()) {
            return historicTaskInstanceDataManager.findHistoricTaskInstancesAndRelatedEntitiesByQueryCriteria(historicTaskInstanceQuery);
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<HistoricTaskInstance> findHistoricTaskInstancesByNativeQuery(Map<String, Object> parameterMap) {
        return historicTaskInstanceDataManager.findHistoricTaskInstancesByNativeQuery(parameterMap);
    }

    @Override
    public long findHistoricTaskInstanceCountByNativeQuery(Map<String, Object> parameterMap) {
        return historicTaskInstanceDataManager.findHistoricTaskInstanceCountByNativeQuery(parameterMap);
    }

    public HistoricTaskInstanceDataManager getHistoricTaskInstanceDataManager() {
        return historicTaskInstanceDataManager;
    }

    public void setHistoricTaskInstanceDataManager(HistoricTaskInstanceDataManager historicTaskInstanceDataManager) {
        this.historicTaskInstanceDataManager = historicTaskInstanceDataManager;
    }

}
