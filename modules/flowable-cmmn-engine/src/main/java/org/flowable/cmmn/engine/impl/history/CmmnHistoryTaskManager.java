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
package org.flowable.cmmn.engine.impl.history;

import java.util.Date;

import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
import org.flowable.task.service.history.InternalHistoryTaskManager;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Joram Barrez
 */
public class CmmnHistoryTaskManager implements InternalHistoryTaskManager {

    protected CmmnHistoryManager cmmnHistoryManager;

    public CmmnHistoryTaskManager(CmmnHistoryManager cmmnHistoryManager) {
        this.cmmnHistoryManager = cmmnHistoryManager;
    }

    @Override
    public void recordTaskInfoChange(TaskEntity taskEntity, Date changeTime) {
        CommandContextUtil.getCmmnHistoryManager().recordTaskInfoChange(taskEntity, changeTime);
    }

    @Override
    public void recordTaskCreated(TaskEntity taskEntity) {
        cmmnHistoryManager.recordTaskCreated(taskEntity);
    }

    @Override
    public void recordHistoryUserTaskLog(HistoricTaskLogEntryBuilder taskLogEntryBuilder) {
        CommandContextUtil.getCmmnHistoryManager().recordHistoricUserTaskLogEntry(taskLogEntryBuilder);
    }

    @Override
    public void deleteHistoryUserTaskLog(long logNumber) {
        CommandContextUtil.getCmmnHistoryManager().deleteHistoricUserTaskLogEntry(logNumber);
    }
}
