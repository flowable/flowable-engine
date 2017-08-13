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
package org.flowable.task.service.impl;

import java.util.List;

import org.flowable.task.service.HistoricTaskService;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.history.HistoricTaskInstance;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class HistoricTaskServiceImpl extends ServiceImpl implements HistoricTaskService {

    public HistoricTaskServiceImpl() {

    }

    public HistoricTaskServiceImpl(TaskServiceConfiguration taskServiceConfiguration) {
        super(taskServiceConfiguration);
    }

    @Override
    public HistoricTaskInstanceEntity getHistoricTask(String id) {
        return getHistoricTaskInstanceEntityManager().findById(id);
    }

    @Override
    public List<HistoricTaskInstanceEntity> findHistoricTasksByParentTaskId(String parentTaskId) {
        return getHistoricTaskInstanceEntityManager().findHistoricTasksByParentTaskId(parentTaskId);
    }
    
    @Override
    public List<HistoricTaskInstanceEntity> findHistoricTasksByProcessInstanceId(String processInstanceId) {
        return getHistoricTaskInstanceEntityManager().findHistoricTasksByProcessInstanceId(processInstanceId);
    }

    @Override
    public List<HistoricTaskInstance> findHistoricTaskInstancesByQueryCriteria(HistoricTaskInstanceQueryImpl historicTaskInstanceQuery) {
        return getHistoricTaskInstanceEntityManager().findHistoricTaskInstancesByQueryCriteria(historicTaskInstanceQuery);
    }

    @Override
    public HistoricTaskInstanceEntity createHistoricTask() {
        return getHistoricTaskInstanceEntityManager().create();
    }

    @Override
    public HistoricTaskInstanceEntity createHistoricTask(TaskEntity taskEntity) {
        return getHistoricTaskInstanceEntityManager().create(taskEntity);
    }

    @Override
    public void insertHistoricTask(HistoricTaskInstanceEntity historicTaskInstanceEntity, boolean fireCreateEvent) {
        getHistoricTaskInstanceEntityManager().insert(historicTaskInstanceEntity, fireCreateEvent);
    }

    @Override
    public void deleteHistoricTask(HistoricTaskInstanceEntity HistoricTaskInstance) {
        getHistoricTaskInstanceEntityManager().delete(HistoricTaskInstance);
    }

}
