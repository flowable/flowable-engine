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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.form.api.FormFieldHandler;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.history.HistoricTaskInstance;

/**
 * @author Tijs Rademakers
 */
public class GetTaskFormModelCmd implements Command<FormInfo>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String taskId;
    protected boolean ignoreVariables;

    public GetTaskFormModelCmd(String taskId, boolean ignoreVariables) {
        this.taskId = taskId;
        this.ignoreVariables = ignoreVariables;
    }

    @Override
    public FormInfo execute(CommandContext commandContext) {
        boolean historic = false;

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        FormService formService = CommandContextUtil.getFormService();
        if (formService == null) {
            throw new FlowableIllegalArgumentException("Form engine is not initialized");
        }

        TaskInfo task = processEngineConfiguration.getTaskServiceConfiguration().getTaskService().getTask(taskId);
        Date endTime = null;
        if (task == null) {
            historic = true;
            task = processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().getHistoricTask(taskId);
            if (task != null) {
                endTime = ((HistoricTaskInstance) task).getEndTime();
            }
        }

        if (task == null) {
            throw new FlowableObjectNotFoundException("Task not found with id " + taskId);
        }

        Map<String, Object> variables = new HashMap<>();
        if (!ignoreVariables && task.getProcessInstanceId() != null) {

            if (!historic) {
                processEngineConfiguration.getTaskService()
                        .getVariableInstances(taskId).values()
                        .stream()
                        .forEach(variableInstance -> variables.putIfAbsent(variableInstance.getName(), variableInstance.getValue()));

                processEngineConfiguration.getRuntimeService().getVariableInstances(task.getProcessInstanceId()).values()
                        .stream()
                        .forEach(variableInstance -> variables.putIfAbsent(variableInstance.getName(), variableInstance.getValue()));


            } else {

                processEngineConfiguration.getHistoryService()
                        .createHistoricVariableInstanceQuery().taskId(taskId).list()
                        .stream()
                        .forEach(variableInstance -> variables.putIfAbsent(variableInstance.getVariableName(), variableInstance.getValue()));

                processEngineConfiguration.getHistoryService()
                        .createHistoricVariableInstanceQuery().processInstanceId(task.getProcessInstanceId()).list()
                        .stream()
                        .forEach(variableInstance -> variables.putIfAbsent(variableInstance.getVariableName(), variableInstance.getValue()));

            }
        }

        String parentDeploymentId = null;
        if (StringUtils.isNotEmpty(task.getProcessDefinitionId())) {
            Process process = ProcessDefinitionUtil.getProcess(task.getProcessDefinitionId());
            FlowElement element = process.getFlowElement(task.getTaskDefinitionKey(), true);
            boolean sameDeployment = true;
            if (element instanceof UserTask) {
                sameDeployment = ((UserTask) element).isSameDeployment();
            }
            if (sameDeployment) {
                // If it is not same deployment then there is no need to search for parent deployment
                parentDeploymentId = ProcessDefinitionUtil.getDefinitionDeploymentId(task.getProcessDefinitionId(), processEngineConfiguration);
            }
        }

        FormInfo formInfo = null;
        if (ignoreVariables) {
            FormRepositoryService formRepositoryService = CommandContextUtil.getFormRepositoryService();
            formInfo = formRepositoryService.getFormModelByKeyAndParentDeploymentId(task.getFormKey(), parentDeploymentId,
                    task.getTenantId(), processEngineConfiguration.isFallbackToDefaultTenant());

        } else if (endTime != null) {
            formInfo = formService.getFormInstanceModelByKeyAndParentDeploymentId(task.getFormKey(), parentDeploymentId,
                    taskId, task.getProcessInstanceId(), variables, task.getTenantId(), processEngineConfiguration.isFallbackToDefaultTenant());

        } else {
            formInfo = formService.getFormModelWithVariablesByKeyAndParentDeploymentId(task.getFormKey(), parentDeploymentId,
                    taskId, variables, task.getTenantId(), processEngineConfiguration.isFallbackToDefaultTenant());
        }

        // If form does not exists, we don't want to leak out this info to just anyone
        if (formInfo == null) {
            throw new FlowableObjectNotFoundException("Form model for task " + task.getTaskDefinitionKey() + " cannot be found for form key " + task.getFormKey());
        }

        FormFieldHandler formFieldHandler = processEngineConfiguration.getFormFieldHandler();
        formFieldHandler.enrichFormFields(formInfo);

        return formInfo;
    }

}
