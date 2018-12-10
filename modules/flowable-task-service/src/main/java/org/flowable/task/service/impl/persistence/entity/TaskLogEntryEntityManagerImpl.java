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

import java.util.List;
import java.util.Map;

import org.flowable.task.api.history.HistoricTaskLogEntry;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.HistoricTaskLogEntryQueryImpl;
import org.flowable.task.service.impl.persistence.entity.data.TaskLogEntryDataManager;

/**
 * @author martin.grofcik
 */
public class TaskLogEntryEntityManagerImpl extends AbstractEntityManager<HistoricTaskLogEntryEntity> implements TaskLogEntryEntityManager {

    private final TaskLogEntryDataManager taskLogDataManager;

    public TaskLogEntryEntityManagerImpl(TaskServiceConfiguration taskServiceConfiguration, TaskLogEntryDataManager taskLogDataManager) {
        super(taskServiceConfiguration);
        this.taskLogDataManager = taskLogDataManager;
    }

    @Override
    protected TaskLogEntryDataManager getDataManager() {
        return taskLogDataManager;
    }

    @Override
    public List<HistoricTaskLogEntry> findTaskLogEntriesByQueryCriteria(HistoricTaskLogEntryQueryImpl taskLogEntryQuery) {
        return getDataManager().findTaskLogEntriesByQueryCriteria(taskLogEntryQuery);
    }

    @Override
    public long findTaskLogEntriesCountByQueryCriteria(HistoricTaskLogEntryQueryImpl taskLogEntryQuery) {
        return getDataManager().findTaskLogEntriesCountByQueryCriteria(taskLogEntryQuery);
    }

    @Override
    public List<HistoricTaskLogEntry> findTaskLogEntriesByNativeQueryCriteria(Map<String, Object> nativeTaskLogEntryQuery) {
        return getDataManager().findTaskLogEntriesByNativeQueryCriteria(nativeTaskLogEntryQuery);
    }
    @Override
    public long findTaskLogEntriesCountByNativeQueryCriteria(Map<String, Object> nativeTaskLogEntryQuery) {
        return getDataManager().findTaskLogEntriesCountByNativeQueryCriteria(nativeTaskLogEntryQuery);
    }

    @Override
    public void deleteTaskLogEntry(long logNr) {
        getDataManager().deleteTaskLogEntry(logNr);
    }

    @Override
    public void deleteTaskLogEntriesForProcessDefinition(String processDefinitionId) {
        getDataManager().deleteTaskLogEntriesByProcessDefinitionId(processDefinitionId);
    }

    @Override
    public void deleteTaskLogEntriesForScopeDefinition(String scopeType, String scopeDefinitionId) {
        getDataManager().deleteTaskLogEntriesByScopeDefinitionId(scopeType, scopeDefinitionId);
    }

    @Override
    public void deleteTaskLogEntriesForTaskId(String taskId) {
        getDataManager().deleteTaskLogEntriesByTaskId(taskId);
    }

    @Override
    public void createTaskLogEntry(HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder) {
        HistoricTaskLogEntryEntity taskLogEntryEntity = getDataManager().create();
        taskLogEntryEntity.setUserId(historicTaskLogEntryBuilder.getUserId());
        taskLogEntryEntity.setTimeStamp(historicTaskLogEntryBuilder.getTimeStamp());
        taskLogEntryEntity.setTaskId(historicTaskLogEntryBuilder.getTaskId());
        taskLogEntryEntity.setTenantId(historicTaskLogEntryBuilder.getTenantId());
        taskLogEntryEntity.setProcessInstanceId(historicTaskLogEntryBuilder.getProcessInstanceId());
        taskLogEntryEntity.setProcessDefinitionId(historicTaskLogEntryBuilder.getProcessDefinitionId());
        taskLogEntryEntity.setExecutionId(historicTaskLogEntryBuilder.getExecutionId());
        taskLogEntryEntity.setScopeId(historicTaskLogEntryBuilder.getScopeId());
        taskLogEntryEntity.setScopeDefinitionId(historicTaskLogEntryBuilder.getScopeDefinitionId());
        taskLogEntryEntity.setSubScopeId(historicTaskLogEntryBuilder.getSubScopeId());
        taskLogEntryEntity.setScopeType(historicTaskLogEntryBuilder.getScopeType());

        taskLogEntryEntity.setType(historicTaskLogEntryBuilder.getType());
        taskLogEntryEntity.setData(historicTaskLogEntryBuilder.getData());
        getDataManager().insert(taskLogEntryEntity);
    }

}
