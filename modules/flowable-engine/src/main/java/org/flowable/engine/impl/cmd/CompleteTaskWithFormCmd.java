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

import java.util.Map;

import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.impl.util.TaskHelper;
import org.flowable.form.api.FormFieldHandler;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Tijs Rademakers
 */
public class CompleteTaskWithFormCmd extends NeedsActiveTaskCmd<Void> {

    private static final long serialVersionUID = 1L;
    protected String formDefinitionId;
    protected String outcome;
    protected Map<String, Object> variables;
    protected Map<String, Object> transientVariables;
    protected boolean localScope;

    public CompleteTaskWithFormCmd(String taskId, String formDefinitionId, String outcome, Map<String, Object> variables) {
        super(taskId);
        this.formDefinitionId = formDefinitionId;
        this.outcome = outcome;
        this.variables = variables;
    }

    public CompleteTaskWithFormCmd(String taskId, String formDefinitionId, String outcome,
            Map<String, Object> variables, boolean localScope) {

        this(taskId, formDefinitionId, outcome, variables);
        this.localScope = localScope;
    }

    public CompleteTaskWithFormCmd(String taskId, String formDefinitionId, String outcome,
            Map<String, Object> variables, Map<String, Object> transientVariables) {

        this(taskId, formDefinitionId, outcome, variables);
        this.transientVariables = transientVariables;
    }

    @Override
    protected Void execute(CommandContext commandContext, TaskEntity task) {
        FormService formService = CommandContextUtil.getFormService();
        if (formService == null) {
            throw new FlowableIllegalArgumentException("Form engine is not initialized");
        }

        FormRepositoryService formRepositoryService = CommandContextUtil.getFormRepositoryService();
        FormInfo formInfo = formRepositoryService.getFormModelById(formDefinitionId);

        if (formInfo != null) {
            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
            FormFieldHandler formFieldHandler = processEngineConfiguration.getFormFieldHandler();
            if (isFormFieldValidationEnabled(task, processEngineConfiguration, task.getProcessDefinitionId(), task.getTaskDefinitionKey())) {
                formFieldHandler.validateFormFieldsOnSubmit(formInfo, task, variables);
            }

            // Extract raw variables and complete the task
            Map<String, Object> taskVariables = formService.getVariablesFromFormSubmission(formInfo, variables, outcome);

            // The taskVariables are the variables that should be used when completing the task
            // the actual variables should instead be used when saving the form instances
            if (task.getProcessInstanceId() != null) {
                formService.saveFormInstance(variables, formInfo, task.getId(), task.getProcessInstanceId(),
                                task.getProcessDefinitionId(), task.getTenantId(), outcome);
            } else {
                formService.saveFormInstanceWithScopeId(variables, formInfo, task.getId(), task.getScopeId(), task.getScopeType(),
                                task.getScopeDefinitionId(), task.getTenantId(), outcome);
            }

            formFieldHandler.handleFormFieldsOnSubmit(formInfo, task.getId(), task.getProcessInstanceId(), null, null, taskVariables, task.getTenantId());

            TaskHelper.completeTask(task, taskVariables, transientVariables, localScope, commandContext);

        } else {
            TaskHelper.completeTask(task, variables, transientVariables, localScope, commandContext);
        }

        return null;
    }

    protected boolean isFormFieldValidationEnabled(TaskEntity task, ProcessEngineConfigurationImpl processEngineConfiguration, String processDefinitionId,
        String taskDefinitionKey) {
        if (processEngineConfiguration.isFormFieldValidationEnabled()) {
            UserTask userTask = (UserTask) ProcessDefinitionUtil.getBpmnModel(processDefinitionId).getFlowElement(taskDefinitionKey);
            String formFieldValidationExpression = userTask.getValidateFormFields();
            return TaskHelper.isFormFieldValidationEnabled(task, processEngineConfiguration, formFieldValidationExpression);
        }
        return false;
    }

    @Override
    protected String getSuspendedTaskException() {
        return "Cannot complete a suspended task";
    }

}
