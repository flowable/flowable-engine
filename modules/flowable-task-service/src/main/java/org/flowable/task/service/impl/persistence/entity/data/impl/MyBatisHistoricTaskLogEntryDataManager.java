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
package org.flowable.task.service.impl.persistence.entity.data.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.task.api.history.HistoricTaskLogEntry;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.HistoricTaskLogEntryQueryImpl;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskLogEntryEntity;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskLogEntryEntityImpl;
import org.flowable.task.service.impl.persistence.entity.data.HistoricTaskLogEntryDataManager;

/**
 * @author martin.grofcik
 */
public class MyBatisHistoricTaskLogEntryDataManager extends AbstractDataManager<HistoricTaskLogEntryEntity> implements HistoricTaskLogEntryDataManager {

    protected TaskServiceConfiguration taskServiceConfiguration;
    
    public MyBatisHistoricTaskLogEntryDataManager(TaskServiceConfiguration taskServiceConfiguration) {
        this.taskServiceConfiguration = taskServiceConfiguration;
    }
    
    @Override
    public Class<? extends HistoricTaskLogEntryEntity> getManagedEntityClass() {
        return HistoricTaskLogEntryEntityImpl.class;
    }

    @Override
    public HistoricTaskLogEntryEntity create() {
        return new HistoricTaskLogEntryEntityImpl();
    }

    @Override
    public long findHistoricTaskLogEntriesCountByQueryCriteria(HistoricTaskLogEntryQueryImpl taskLogEntryQuery) {
        return (Long) getDbSqlSession().selectOne("selectHistoricTaskLogEntriesCountByQueryCriteria", taskLogEntryQuery);
    }

    @Override
    public List<HistoricTaskLogEntry> findHistoricTaskLogEntriesByQueryCriteria(HistoricTaskLogEntryQueryImpl taskLogEntryQuery) {
        return getDbSqlSession().selectList("selectHistoricTaskLogEntriesByQueryCriteria", taskLogEntryQuery);
    }

    @Override
    public void deleteHistoricTaskLogEntry(long logEntryNumber) {
        getDbSqlSession().delete("deleteHistoricTaskLogEntryByLogNumber", logEntryNumber, HistoricTaskLogEntryEntityImpl.class);
    }

    @Override
    public void deleteHistoricTaskLogEntriesByProcessDefinitionId(String processDefinitionId) {
        getDbSqlSession().delete("deleteHistoricTaskLogEntriesByProcessDefinitionId", processDefinitionId, HistoricTaskLogEntryEntityImpl.class);
    }

    @Override
    public void deleteHistoricTaskLogEntriesByScopeDefinitionId(String scopeType, String scopeDefinitionId) {
        Map<String, String> params = new HashMap<>(2);
        params.put("scopeDefinitionId", scopeDefinitionId);
        params.put("scopeType", scopeType);
        getDbSqlSession().delete("deleteHistoricTaskLogEntriesByScopeDefinitionId", params, HistoricTaskLogEntryEntityImpl.class);
    }

    @Override
    public void deleteHistoricTaskLogEntriesByTaskId(String taskId) {
        getDbSqlSession().delete("deleteHistoricTaskLogEntriesByTaskId", taskId, HistoricTaskLogEntryEntityImpl.class);
    }

    @Override
    public void deleteHistoricTaskLogEntriesForNonExistingProcessInstances() {
        getDbSqlSession().delete("bulkDeleteHistoricTaskLogEntriesForNonExistingProcessInstances", null, HistoricTaskLogEntryEntityImpl.class);
    }
    
    @Override
    public void deleteHistoricTaskLogEntriesForNonExistingCaseInstances() {
        getDbSqlSession().delete("bulkDeleteHistoricTaskLogEntriesForNonExistingCaseInstances", null, HistoricTaskLogEntryEntityImpl.class);
    }
    
    @Override
    public long findHistoricTaskLogEntriesCountByNativeQueryCriteria(Map<String, Object> nativeHistoricTaskLogEntryQuery) {
        return (Long) getDbSqlSession().selectOne("selectHistoricTaskLogEntriesCountByNativeQueryCriteria", nativeHistoricTaskLogEntryQuery);
    }
    
    @Override
    public List<HistoricTaskLogEntry> findHistoricTaskLogEntriesByNativeQueryCriteria(Map<String, Object> nativeHistoricTaskLogEntryQuery) {
        return getDbSqlSession().selectListWithRawParameter("selectHistoricTaskLogEntriesByNativeQueryCriteria", nativeHistoricTaskLogEntryQuery);
    }
    
    @Override
    protected IdGenerator getIdGenerator() {
        return taskServiceConfiguration.getIdGenerator();
    }
}
