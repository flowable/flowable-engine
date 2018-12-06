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
import org.flowable.task.api.TaskLogEntry;
import org.flowable.task.service.impl.TaskLogEntryQueryImpl;
import org.flowable.task.service.impl.persistence.entity.TaskLogEntryEntity;
import org.flowable.task.service.impl.persistence.entity.TaskLogEntryEntityImpl;
import org.flowable.task.service.impl.persistence.entity.data.TaskLogEntryDataManager;

/**
 * @author martin.grofcik
 */
public class MyBatisTaskLogEntryDataManager extends AbstractDataManager<TaskLogEntryEntity> implements TaskLogEntryDataManager {

    @Override
    public Class<? extends TaskLogEntryEntity> getManagedEntityClass() {
        return TaskLogEntryEntityImpl.class;
    }

    @Override
    public TaskLogEntryEntity create() {
        return new TaskLogEntryEntityImpl();
    }

    @Override
    public long findTaskLogEntriesCountByQueryCriteria(TaskLogEntryQueryImpl taskLogEntryQuery) {
        return (Long) getDbSqlSession().selectOne("selectTaskLogEntriesCountByQueryCriteria", taskLogEntryQuery);
    }

    @Override
    public List<TaskLogEntry> findTaskLogEntriesByQueryCriteria(TaskLogEntryQueryImpl taskLogEntryQuery) {
        return getDbSqlSession().selectList("selectTaskLogEntriesByQueryCriteria", taskLogEntryQuery);
    }

    @Override
    public void deleteTaskLogEntry(long logEntryNumber) {
        getDbSqlSession().delete("deleteTaskLogEntryByLogNumber", logEntryNumber, TaskLogEntryEntityImpl.class);
    }

    @Override
    public void deleteTaskLogEntriesByProcessDefinitionId(String processDefinitionId) {
        getDbSqlSession().delete("deleteTaskLogEntriesByProcessDefinitionId", processDefinitionId, TaskLogEntryEntityImpl.class);
    }

    @Override
    public void deleteTaskLogEntriesByScopeDefinitionId(String scopeType, String scopeDefinitionId) {
        Map<String, String> params = new HashMap<>(2);
        params.put("scopeDefinitionId", scopeDefinitionId);
        params.put("scopeType", scopeType);
        getDbSqlSession().delete("deleteTaskLogEntryByScopeDefinitionId", params, TaskLogEntryEntityImpl.class);
    }

    @Override
    public void deleteTaskLogEntriesByTaskId(String taskId) {
        getDbSqlSession().delete("deleteTaskLogEntriesByTaskId", taskId, TaskLogEntryEntityImpl.class);
    }

    @Override
    public long findTaskLogEntriesCountByNativeQueryCriteria(Map<String, Object> nativeTaskLogEntryQuery) {
        return (Long) getDbSqlSession().selectOne("selectTaskLogEntriesCountByNativeQueryCriteria", nativeTaskLogEntryQuery);
    }
    @Override
    public List<TaskLogEntry> findTaskLogEntriesByNativeQueryCriteria(Map<String, Object> nativeTaskLogEntryQuery) {
        return getDbSqlSession().selectListWithRawParameter("selectTaskLogEntriesByNativeQueryCriteria", nativeTaskLogEntryQuery);
    }
}
