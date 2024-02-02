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

import java.util.Map;

import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.delegate.DelegateTask;

public class TestCacheTaskListener implements TaskListener {
    
    private static final long serialVersionUID = 1L;
    
    public static String TASK_ID;
    public static String HISTORIC_TASK_ID;

    public static Map<String, Object> PROCESS_VARIABLES;
    public static Map<String, Object> HISTORIC_PROCESS_VARIABLES;

    public static Map<String, Object> TASK_PROCESS_VARIABLES;
    public static Map<String, Object> HISTORIC_TASK_PROCESS_VARIABLES;

    public static Map<String, Object> TASK_LOCAL_VARIABLES;
    public static Map<String, Object> HISTORIC_TASK_LOCAL_VARIABLES;

    public static void reset() {
        TASK_ID = null;
        HISTORIC_TASK_ID = null;

        PROCESS_VARIABLES = null;
        HISTORIC_PROCESS_VARIABLES = null;

        TASK_PROCESS_VARIABLES = null;
        HISTORIC_TASK_PROCESS_VARIABLES = null;

        TASK_LOCAL_VARIABLES = null;
        HISTORIC_TASK_LOCAL_VARIABLES = null;
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        TaskService taskService = processEngineConfiguration.getTaskService();
        Task task = taskService.createTaskQuery().taskId(delegateTask.getId()).singleResult();
        if (task != null && task.getId().equals(delegateTask.getId())) {
            TASK_ID = task.getId();
        }
        
        HistoryService historyService = processEngineConfiguration.getHistoryService();
        HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery().taskId(delegateTask.getId()).singleResult();
        if (historicTask != null && historicTask.getId().equals(delegateTask.getId())) {
            HISTORIC_TASK_ID = historicTask.getId();
        }

        delegateTask.setVariable("varFromTheListener", "valueFromTheListener");
        delegateTask.setVariableLocal("localVar", "localValue");

        // Used in CacheTaskTest#testTaskQueryWithIncludeVariables
        ProcessInstance processInstance = processEngineConfiguration.getRuntimeService().createProcessInstanceQuery()
            .processInstanceId(task.getProcessInstanceId())
            .includeProcessVariables()
            .singleResult();
        PROCESS_VARIABLES = processInstance.getProcessVariables();

        HistoricProcessInstance historicProcessInstance = processEngineConfiguration.getHistoryService().createHistoricProcessInstanceQuery()
            .processInstanceId(task.getProcessInstanceId())
            .includeProcessVariables()
            .singleResult();
        HISTORIC_PROCESS_VARIABLES = historicProcessInstance.getProcessVariables();

        // Used in CacheTaskTest#testTaskQueryWithIncludeVariables
        Task taskFromQuery = processEngineConfiguration.getTaskService().createTaskQuery()
            .taskId(delegateTask.getId())
            .includeProcessVariables()
            .includeTaskLocalVariables()
            .singleResult();
        TASK_PROCESS_VARIABLES = taskFromQuery.getProcessVariables();
        TASK_LOCAL_VARIABLES = taskFromQuery.getTaskLocalVariables();

        HistoricTaskInstance historicTaskFromQuery = processEngineConfiguration.getHistoryService().createHistoricTaskInstanceQuery()
            .taskId(delegateTask.getId())
            .includeProcessVariables()
            .includeTaskLocalVariables()
            .singleResult();
        HISTORIC_TASK_PROCESS_VARIABLES = historicTaskFromQuery.getProcessVariables();
        HISTORIC_TASK_LOCAL_VARIABLES = historicTaskFromQuery.getTaskLocalVariables();

    }

}
