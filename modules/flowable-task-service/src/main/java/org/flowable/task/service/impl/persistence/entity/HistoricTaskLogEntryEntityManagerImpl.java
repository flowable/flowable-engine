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
import org.flowable.task.service.impl.persistence.entity.data.HistoricTaskLogEntryDataManager;

/**
 * @author martin.grofcik
 */
public class HistoricTaskLogEntryEntityManagerImpl
    extends AbstractTaskServiceEntityManager<HistoricTaskLogEntryEntity, HistoricTaskLogEntryDataManager>
    implements HistoricTaskLogEntryEntityManager {

    public HistoricTaskLogEntryEntityManagerImpl(TaskServiceConfiguration taskServiceConfiguration, HistoricTaskLogEntryDataManager taskLogDataManager) {
        super(taskServiceConfiguration, taskLogDataManager);
    }

    @Override
    public List<HistoricTaskLogEntry> findHistoricTaskLogEntriesByQueryCriteria(HistoricTaskLogEntryQueryImpl taskLogEntryQuery) {
        return getDataManager().findHistoricTaskLogEntriesByQueryCriteria(taskLogEntryQuery);
    }

    @Override
    public long findHistoricTaskLogEntriesCountByQueryCriteria(HistoricTaskLogEntryQueryImpl taskLogEntryQuery) {
        return getDataManager().findHistoricTaskLogEntriesCountByQueryCriteria(taskLogEntryQuery);
    }

    @Override
    public List<HistoricTaskLogEntry> findHistoricTaskLogEntriesByNativeQueryCriteria(Map<String, Object> nativeHistoricTaskLogEntryQuery) {
        return getDataManager().findHistoricTaskLogEntriesByNativeQueryCriteria(nativeHistoricTaskLogEntryQuery);
    }
    @Override
    public long findHistoricTaskLogEntriesCountByNativeQueryCriteria(Map<String, Object> nativeHistoricTaskLogEntryQuery) {
        return getDataManager().findHistoricTaskLogEntriesCountByNativeQueryCriteria(nativeHistoricTaskLogEntryQuery);
    }

    @Override
    public void deleteHistoricTaskLogEntry(long logNr) {
        getDataManager().deleteHistoricTaskLogEntry(logNr);
    }

    @Override
    public void deleteHistoricTaskLogEntriesForProcessDefinition(String processDefinitionId) {
        getDataManager().deleteHistoricTaskLogEntriesByProcessDefinitionId(processDefinitionId);
    }

    @Override
    public void deleteHistoricTaskLogEntriesForScopeDefinition(String scopeType, String scopeDefinitionId) {
        getDataManager().deleteHistoricTaskLogEntriesByScopeDefinitionId(scopeType, scopeDefinitionId);
    }

    @Override
    public void deleteHistoricTaskLogEntriesForTaskId(String taskId) {
        getDataManager().deleteHistoricTaskLogEntriesByTaskId(taskId);
    }
    
    @Override
    public void deleteHistoricTaskLogEntriesForNonExistingProcessInstances() {
        getDataManager().deleteHistoricTaskLogEntriesForNonExistingProcessInstances();
    }
    
    @Override
    public void deleteHistoricTaskLogEntriesForNonExistingCaseInstances() {
        getDataManager().deleteHistoricTaskLogEntriesForNonExistingCaseInstances();
    }

    @Override
    public void createHistoricTaskLogEntry(HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder) {
        HistoricTaskLogEntryEntity historicTaskLogEntryEntity = getDataManager().create();
        historicTaskLogEntryEntity.setUserId(historicTaskLogEntryBuilder.getUserId());
        historicTaskLogEntryEntity.setTimeStamp(historicTaskLogEntryBuilder.getTimeStamp());
        historicTaskLogEntryEntity.setTaskId(historicTaskLogEntryBuilder.getTaskId());
        historicTaskLogEntryEntity.setTenantId(historicTaskLogEntryBuilder.getTenantId());
        historicTaskLogEntryEntity.setProcessInstanceId(historicTaskLogEntryBuilder.getProcessInstanceId());
        historicTaskLogEntryEntity.setProcessDefinitionId(historicTaskLogEntryBuilder.getProcessDefinitionId());
        historicTaskLogEntryEntity.setExecutionId(historicTaskLogEntryBuilder.getExecutionId());
        historicTaskLogEntryEntity.setScopeId(historicTaskLogEntryBuilder.getScopeId());
        historicTaskLogEntryEntity.setScopeDefinitionId(historicTaskLogEntryBuilder.getScopeDefinitionId());
        historicTaskLogEntryEntity.setSubScopeId(historicTaskLogEntryBuilder.getSubScopeId());
        historicTaskLogEntryEntity.setScopeType(historicTaskLogEntryBuilder.getScopeType());

        historicTaskLogEntryEntity.setType(historicTaskLogEntryBuilder.getType());
        historicTaskLogEntryEntity.setData(historicTaskLogEntryBuilder.getData());
        getDataManager().insert(historicTaskLogEntryEntity);
    }

}
