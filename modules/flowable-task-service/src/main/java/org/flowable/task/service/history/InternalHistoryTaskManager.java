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
package org.flowable.task.service.history;

import java.util.Date;

import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

public interface InternalHistoryTaskManager {

    /**
     * Record task name change, if audit history is enabled.
     */
    void recordTaskInfoChange(TaskEntity taskEntity, Date changeTime);

    /**
     * Record task created.
     */
    void recordTaskCreated(TaskEntity taskEntity);

    /**
     * Record historyUserTaskLogEntry
     */
    void recordHistoryUserTaskLog(HistoricTaskLogEntryBuilder taskLogEntryBuilder);

    /**
     * Delete historyUserTaskLogEntry
     */
    void deleteHistoryUserTaskLog(long logNumber);
}
