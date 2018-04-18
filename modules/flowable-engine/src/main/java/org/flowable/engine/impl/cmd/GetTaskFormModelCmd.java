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
package org.flowable.engine.impl.cmd;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.form.api.FormFieldHandler;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormService;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;

/**
 * @author Tijs Rademakers
 */
public class GetTaskFormModelCmd implements Command<FormInfo>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String taskId;

    public GetTaskFormModelCmd(String taskId) {
        this.taskId = taskId;
    }

    @Override
    public FormInfo execute(CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        FormService formService = CommandContextUtil.getFormService();
        if (formService == null) {
            throw new FlowableIllegalArgumentException("Form engine is not initialized");
        }

        HistoricTaskInstance task = CommandContextUtil.getHistoricTaskService().getHistoricTask(taskId);
        if (task == null) {
            throw new FlowableObjectNotFoundException("Task not found with id " + taskId);
        }

        Map<String, Object> variables = new HashMap<>();
        if (task.getProcessInstanceId() != null) {
            List<HistoricVariableInstance> variableInstances = processEngineConfiguration.getHistoryService()
                    .createHistoricVariableInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .list();

            for (HistoricVariableInstance historicVariableInstance : variableInstances) {
                variables.put(historicVariableInstance.getVariableName(), historicVariableInstance.getValue());
            }
        }

        String parentDeploymentId = null;
        if (StringUtils.isNotEmpty(task.getProcessDefinitionId())) {
            ProcessDefinition processDefinition = processEngineConfiguration.getRepositoryService()
                    .getProcessDefinition(task.getProcessDefinitionId());
            parentDeploymentId = processDefinition.getDeploymentId();
        }
        
        FormInfo formInfo = null;
        if (task.getEndTime() != null) {
            formInfo = formService.getFormInstanceModelByKeyAndParentDeploymentId(task.getFormKey(), parentDeploymentId,
                            taskId, task.getProcessInstanceId(), variables, task.getTenantId());

        } else {
            formInfo = formService.getFormModelWithVariablesByKeyAndParentDeploymentId(task.getFormKey(), parentDeploymentId,
                            taskId, variables, task.getTenantId());
        }

        // If form does not exists, we don't want to leak out this info to just anyone
        if (formInfo == null) {
            throw new FlowableObjectNotFoundException("Form model for task " + task.getTaskDefinitionKey() + " cannot be found for form key " + task.getFormKey());
        }

        FormFieldHandler formFieldHandler = CommandContextUtil.getProcessEngineConfiguration(commandContext).getFormFieldHandler();
        formFieldHandler.enrichFormFields(formInfo);

        return formInfo;
    }

}
