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
package org.flowable.engine.test.bpmn.cache;

import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.delegate.DelegateTask;

public class CacheTaskListener implements TaskListener {
    
    private static final long serialVersionUID = 1L;
    
    public static String taskId;
    public static String historicTaskId;

    @Override
    public void notify(DelegateTask delegateTask) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        TaskService taskService = processEngineConfiguration.getTaskService();
        Task task = taskService.createTaskQuery().taskId(delegateTask.getId()).singleResult();
        if (task != null && task.getId().equals(delegateTask.getId())) {
            taskId = task.getId();
        }
        
        HistoryService historyService = processEngineConfiguration.getHistoryService();
        HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery().taskId(delegateTask.getId()).singleResult();
        if (historicTask != null && historicTask.getId().equals(delegateTask.getId())) {
            historicTaskId = historicTask.getId();
        }
    }

}
