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

import org.flowable.task.api.TaskLogEntry;
import org.flowable.task.api.TaskLogEntryBuilder;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.TaskLogEntryQueryImpl;
import org.flowable.task.service.impl.persistence.entity.data.TaskLogEntryDataManager;

/**
 * @author martin.grofcik
 */
public class TaskLogEntryEntityManagerImpl extends AbstractEntityManager<TaskLogEntryEntity> implements TaskLogEntryEntityManager {

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
    public List<TaskLogEntry> findTaskLogEntriesByQueryCriteria(TaskLogEntryQueryImpl taskLogEntryQuery) {
        return getDataManager().findTaskLogEntriesByQueryCriteria(taskLogEntryQuery);
    }

    @Override
    public long findTaskLogEntriesCountByQueryCriteria(TaskLogEntryQueryImpl taskLogEntryQuery) {
        return getDataManager().findTaskLogEntriesCountByQueryCriteria(taskLogEntryQuery);
    }

    @Override
    public List<TaskLogEntry> findTaskLogEntriesByNativeQueryCriteria(Map<String, Object> nativeTaskLogEntryQuery) {
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
    public void createTaskLogEntry(TaskLogEntryBuilder taskLogEntryBuilder) {
        TaskLogEntryEntity taskLogEntryEntity = getDataManager().create();
        taskLogEntryEntity.setUserId(taskLogEntryBuilder.getUserId());
        taskLogEntryEntity.setTimeStamp(taskLogEntryBuilder.getTimeStamp());
        taskLogEntryEntity.setTaskId(taskLogEntryBuilder.getTaskId());
        taskLogEntryEntity.setTenantId(taskLogEntryBuilder.getTenantId());
        taskLogEntryEntity.setProcessInstanceId(taskLogEntryBuilder.getProcessInstanceId());
        taskLogEntryEntity.setExecutionId(taskLogEntryBuilder.getExecutionId());
        taskLogEntryEntity.setScopeId(taskLogEntryBuilder.getScopeId());
        taskLogEntryEntity.setSubScopeId(taskLogEntryBuilder.getSubScopeId());
        taskLogEntryEntity.setScopeType(taskLogEntryBuilder.getScopeType());

        taskLogEntryEntity.setType(taskLogEntryBuilder.getType());
        taskLogEntryEntity.setData(taskLogEntryBuilder.getData());
        getDataManager().insert(taskLogEntryEntity);
    }

}
