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

import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.service.delegate.DelegateTask;

public class TestQueryWithIncludeVariabesTaskListener implements TaskListener {

    public static Map<String, Object> PROCESS_INSTANCE_VARIABLES;

    public static Map<String, Object> TASK_VARIABLES;

    public static Map<String, Object> TASK_LOCAL_VARIABLES;

    @Override
    public void notify(DelegateTask delegateTask) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();

        ProcessInstance processInstance = processEngineConfiguration.getRuntimeService().createProcessInstanceQuery()
            .processInstanceId(delegateTask.getProcessInstanceId())
            .includeProcessVariables()
            .singleResult();
        PROCESS_INSTANCE_VARIABLES = processInstance.getProcessVariables();

        Task task = processEngineConfiguration.getTaskService().createTaskQuery()
            .taskId(delegateTask.getId())
            .includeProcessVariables()
            .singleResult();
        TASK_VARIABLES = task.getProcessVariables();

        TASK_LOCAL_VARIABLES = task.getTaskLocalVariables();

    }

}
