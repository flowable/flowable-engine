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
package org.flowable.cmmn.test.runtime;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;

public class CacheTaskListener implements TaskListener {
    
    private static final long serialVersionUID = 1L;
    
    public static String taskId;
    public static String historicTaskId;

    public static void reset() {
        taskId = null;
        historicTaskId = null;
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();
        CmmnTaskService taskService = cmmnEngineConfiguration.getCmmnTaskService();
        Task task = taskService.createTaskQuery().taskId(delegateTask.getId()).singleResult();
        if (task != null && task.getId().equals(delegateTask.getId())) {
            taskId = task.getId();
        }
        
        CmmnHistoryService historyService = cmmnEngineConfiguration.getCmmnHistoryService();
        HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery().taskId(delegateTask.getId()).singleResult();
        if (historicTask != null && historicTask.getId().equals(delegateTask.getId())) {
            historicTaskId = historicTask.getId();
        }
    }

}
