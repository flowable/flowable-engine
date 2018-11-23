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

import org.flowable.task.api.TaskLogEntry;
import org.flowable.task.service.TaskServiceConfiguration;
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
    public List<TaskLogEntry> findTaskLogEntriesByTaskInstanceId(String taskInstanceId) {
        return getDataManager().findTaskLogEntriesByTaskInstanceId(taskInstanceId);
    }
    @Override
    public void deleteTaskLogEntry(long logNr) {
        getDataManager().deleteTaskLogEntry(logNr);
    }
}
