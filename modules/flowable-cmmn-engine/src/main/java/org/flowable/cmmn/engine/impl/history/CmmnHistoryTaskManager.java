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

import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
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
    public void recordTaskInfoChange(TaskEntity taskEntity) {
        CommandContextUtil.getHistoricTaskService().recordTaskInfoChange(taskEntity);
    }

}
