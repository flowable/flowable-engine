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
package org.flowable.task.service;

import java.util.List;

import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.impl.HistoricTaskInstanceQueryImpl;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * Service which provides access to {@link HistoricTaskInstanceEntity}.
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public interface HistoricTaskService {

    HistoricTaskInstanceEntity getHistoricTask(String id);
    
    List<HistoricTaskInstanceEntity> findHistoricTasksByParentTaskId(String parentTaskId);
    
    List<HistoricTaskInstanceEntity> findHistoricTasksByProcessInstanceId(String processInstanceId);
    
    List<HistoricTaskInstance> findHistoricTaskInstancesByQueryCriteria(HistoricTaskInstanceQueryImpl historicTaskInstanceQuery);
    
    HistoricTaskInstanceEntity createHistoricTask();
    
    HistoricTaskInstanceEntity createHistoricTask(TaskEntity taskEntity);
    
    void updateHistoricTask(HistoricTaskInstanceEntity historicTaskInstanceEntity, boolean fireUpdateEvent);
    
    void insertHistoricTask(HistoricTaskInstanceEntity historicTaskInstanceEntity, boolean fireCreateEvent);
    
    void deleteHistoricTask(HistoricTaskInstanceEntity HistoricTaskInstance);
    
    HistoricTaskInstanceEntity recordTaskCreated(TaskEntity task);
    
    HistoricTaskInstanceEntity recordTaskEnd(TaskEntity task, String deleteReason);
    
    HistoricTaskInstanceEntity recordTaskInfoChange(TaskEntity taskEntity);
    
}
