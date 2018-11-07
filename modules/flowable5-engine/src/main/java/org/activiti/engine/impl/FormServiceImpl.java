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

package org.activiti.engine.impl;

import java.util.Map;

import org.activiti.engine.FormService;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.cmd.GetFormKeyCmd;
import org.activiti.engine.impl.cmd.GetRenderedStartFormCmd;
import org.activiti.engine.impl.cmd.GetRenderedTaskFormCmd;
import org.activiti.engine.impl.cmd.GetStartFormCmd;
import org.activiti.engine.impl.cmd.GetTaskFormCmd;
import org.activiti.engine.impl.cmd.SubmitStartFormCmd;
import org.activiti.engine.impl.cmd.SubmitTaskFormCmd;
import org.activiti.engine.runtime.ProcessInstance;
import org.flowable.engine.form.StartFormData;

/**
 * @author Tom Baeyens
 * @author Falko Menge (camunda)
 */
public class FormServiceImpl extends ServiceImpl implements FormService {

    @Override
    public Object getRenderedStartForm(String processDefinitionId) {
        return commandExecutor.execute(new GetRenderedStartFormCmd(processDefinitionId, null));
    }

    @Override
    public Object getRenderedStartForm(String processDefinitionId, String engineName) {
        return commandExecutor.execute(new GetRenderedStartFormCmd(processDefinitionId, engineName));
    }

    @Override
    public Object getRenderedTaskForm(String taskId) {
        return commandExecutor.execute(new GetRenderedTaskFormCmd(taskId, null));
    }

    @Override
    public Object getRenderedTaskForm(String taskId, String engineName) {
        return commandExecutor.execute(new GetRenderedTaskFormCmd(taskId, engineName));
    }

    @Override
    public StartFormData getStartFormData(String processDefinitionId) {
        return commandExecutor.execute(new GetStartFormCmd(processDefinitionId));
    }

    @Override
    public TaskFormData getTaskFormData(String taskId) {
        return commandExecutor.execute(new GetTaskFormCmd(taskId));
    }

    @Override
    public ProcessInstance submitStartFormData(String processDefinitionId, Map<String, String> properties) {
        return commandExecutor.execute(new SubmitStartFormCmd(processDefinitionId, null, properties));
    }

    @Override
    public ProcessInstance submitStartFormData(String processDefinitionId, String businessKey, Map<String, String> properties) {
        return commandExecutor.execute(new SubmitStartFormCmd(processDefinitionId, businessKey, properties));
    }

    @Override
    public void submitTaskFormData(String taskId, Map<String, String> properties) {
        commandExecutor.execute(new SubmitTaskFormCmd(taskId, properties, true));
    }

    @Override
    public String getStartFormKey(String processDefinitionId) {
        return commandExecutor.execute(new GetFormKeyCmd(processDefinitionId));
    }

    @Override
    public String getTaskFormKey(String processDefinitionId, String taskDefinitionKey) {
        return commandExecutor.execute(new GetFormKeyCmd(processDefinitionId, taskDefinitionKey));
    }

    @Override
    public void saveFormData(String taskId, Map<String, String> properties) {
        commandExecutor.execute(new SubmitTaskFormCmd(taskId, properties, false));
    }
}
