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

import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.task.api.history.HistoricTaskLogEntry;
import org.flowable.task.service.impl.HistoricTaskLogEntryQueryImpl;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskLogEntryEntity;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskLogEntryEntityImpl;
import org.flowable.task.service.impl.persistence.entity.data.TaskLogEntryDataManager;

/**
 * @author martin.grofcik
 */
public class MyBatisTaskLogEntryDataManager extends AbstractDataManager<HistoricTaskLogEntryEntity> implements TaskLogEntryDataManager {

    @Override
    public Class<? extends HistoricTaskLogEntryEntity> getManagedEntityClass() {
        return HistoricTaskLogEntryEntityImpl.class;
    }

    @Override
    public HistoricTaskLogEntryEntity create() {
        return new HistoricTaskLogEntryEntityImpl();
    }

    @Override
    public long findTaskLogEntriesCountByQueryCriteria(HistoricTaskLogEntryQueryImpl taskLogEntryQuery) {
        return (Long) getDbSqlSession().selectOne("selectTaskLogEntriesCountByQueryCriteria", taskLogEntryQuery);
    }

    @Override
    public List<HistoricTaskLogEntry> findTaskLogEntriesByQueryCriteria(HistoricTaskLogEntryQueryImpl taskLogEntryQuery) {
        return getDbSqlSession().selectList("selectTaskLogEntriesByQueryCriteria", taskLogEntryQuery);
    }

    @Override
    public void deleteTaskLogEntry(long logEntryNumber) {
        getDbSqlSession().delete("deleteTaskLogEntryByLogNumber", logEntryNumber, HistoricTaskLogEntryEntityImpl.class);
    }

    @Override
    public void deleteTaskLogEntriesByProcessDefinitionId(String processDefinitionId) {
        getDbSqlSession().delete("deleteTaskLogEntriesByProcessDefinitionId", processDefinitionId, HistoricTaskLogEntryEntityImpl.class);
    }

    @Override
    public void deleteTaskLogEntriesByScopeDefinitionId(String scopeType, String scopeDefinitionId) {
        Map<String, String> params = new HashMap<>(2);
        params.put("scopeDefinitionId", scopeDefinitionId);
        params.put("scopeType", scopeType);
        getDbSqlSession().delete("deleteTaskLogEntriesByScopeDefinitionId", params, HistoricTaskLogEntryEntityImpl.class);
    }

    @Override
    public void deleteTaskLogEntriesByTaskId(String taskId) {
        getDbSqlSession().delete("deleteTaskLogEntriesByTaskId", taskId, HistoricTaskLogEntryEntityImpl.class);
    }

    @Override
    public long findTaskLogEntriesCountByNativeQueryCriteria(Map<String, Object> nativeTaskLogEntryQuery) {
        return (Long) getDbSqlSession().selectOne("selectTaskLogEntriesCountByNativeQueryCriteria", nativeTaskLogEntryQuery);
    }
    @Override
    public List<HistoricTaskLogEntry> findTaskLogEntriesByNativeQueryCriteria(Map<String, Object> nativeTaskLogEntryQuery) {
        return getDbSqlSession().selectListWithRawParameter("selectTaskLogEntriesByNativeQueryCriteria", nativeTaskLogEntryQuery);
    }
}
