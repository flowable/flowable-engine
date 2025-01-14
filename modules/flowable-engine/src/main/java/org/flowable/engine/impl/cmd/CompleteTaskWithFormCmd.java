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

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.HasValidateFormFields;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
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
    protected String userId;
    protected Map<String, Object> variables;
    protected Map<String, Object> variablesLocal;
    protected Map<String, Object> transientVariables;
    protected Map<String, Object> transientVariablesLocal;

    public CompleteTaskWithFormCmd(String taskId, String formDefinitionId, String outcome, Map<String, Object> variables) {
        super(taskId);
        this.formDefinitionId = formDefinitionId;
        this.outcome = outcome;
        this.variables = variables;
    }
    
    public CompleteTaskWithFormCmd(String taskId, String formDefinitionId, String outcome, String userId, Map<String, Object> variables) {
        this(taskId, formDefinitionId, outcome, variables);
        this.userId = userId;
    }

    public CompleteTaskWithFormCmd(String taskId, String formDefinitionId, String outcome,
            Map<String, Object> variables, boolean localScope) {
        
        this(taskId, formDefinitionId, outcome, variables);
        if (localScope) {
            this.variablesLocal = variables;
        } else {
            this.variables = variables;
        }
    }
    
    public CompleteTaskWithFormCmd(String taskId, String formDefinitionId, String outcome,
            String userId, Map<String, Object> variables, boolean localScope) {
        
        this(taskId, formDefinitionId, outcome, variables, localScope);
        this.userId = userId;
    }

    public CompleteTaskWithFormCmd(String taskId, String formDefinitionId, String outcome,
                                   Map<String, Object> variables, Map<String, Object> transientVariables) {

        this(taskId, formDefinitionId, outcome, variables);
        this.transientVariables = transientVariables;
    }
    
    public CompleteTaskWithFormCmd(String taskId, String formDefinitionId, String outcome,
            String userId, Map<String, Object> variables, Map<String, Object> transientVariables) {

        this(taskId, formDefinitionId, outcome, variables, transientVariables);
        this.userId = userId;
    }

    public CompleteTaskWithFormCmd(String taskId, String formDefinitionId, String outcome, Map<String,
            Object> variables, Map<String, Object> variablesLocal, Map<String, Object> transientVariables, Map<String, Object> transientVariablesLocal) {
        this(taskId, formDefinitionId, outcome, variables);
        this.variablesLocal = variablesLocal;
        this.transientVariables = transientVariables;
        this.transientVariablesLocal = transientVariablesLocal;
    }
    
    public CompleteTaskWithFormCmd(String taskId, String formDefinitionId, String outcome, String userId, 
            Map<String, Object> variables, Map<String, Object> variablesLocal, 
            Map<String, Object> transientVariables, Map<String, Object> transientVariablesLocal) {
        
        this(taskId, formDefinitionId, outcome, variables, variablesLocal, transientVariables, transientVariablesLocal);
        this.userId = userId;
    }

    @Override
    protected Void execute(CommandContext commandContext, TaskEntity task) {
        if (StringUtils.isNotEmpty(task.getScopeId()) && ScopeTypes.CMMN.equals(task.getScopeType())) {
            throw new FlowableException("The " + task + " is created by the cmmn engine and should be completed via the cmmn engine API");
        }
        
        FormService formService = CommandContextUtil.getFormService();
        if (formService == null) {
            throw new FlowableIllegalArgumentException("Form engine is not initialized");
        }

        FormRepositoryService formRepositoryService = CommandContextUtil.getFormRepositoryService();
        FormInfo formInfo = formRepositoryService.getFormModelById(formDefinitionId);

        Map<String, Object> formVariables;
        boolean local = variablesLocal != null && !variablesLocal.isEmpty();
        if (local) {
            formVariables = variablesLocal;
        } else {
            formVariables = variables;
        }
        Map<String, Object> taskVariables = null;

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        if (formInfo != null) {
            FormFieldHandler formFieldHandler = processEngineConfiguration.getFormFieldHandler();
            if (isFormFieldValidationEnabled(task, processEngineConfiguration, task.getProcessDefinitionId(), task.getTaskDefinitionKey())) {
                formService.validateFormFields(task.getTaskDefinitionKey(), "userTask", task.getProcessInstanceId(), 
                        task.getProcessDefinitionId(), ScopeTypes.BPMN, formInfo, formVariables);
            }

            // Extract raw variables and complete the task
            taskVariables = formService.getVariablesFromFormSubmission(task.getTaskDefinitionKey(), "userTask", task.getProcessInstanceId(), 
                    task.getProcessDefinitionId(), ScopeTypes.BPMN, formInfo, formVariables, outcome);

            // The taskVariables are the variables that should be used when completing the task
            // the actual variables should instead be used when saving the form instances
            if (task.getProcessInstanceId() != null) {
                formService.saveFormInstance(formVariables, formInfo, task.getId(), task.getProcessInstanceId(),
                                task.getProcessDefinitionId(), task.getTenantId(), outcome);
            } else {
                formService.saveFormInstanceWithScopeId(formVariables, formInfo, task.getId(), task.getScopeId(), task.getScopeType(),
                                task.getScopeDefinitionId(), task.getTenantId(), outcome);
            }

            formFieldHandler.handleFormFieldsOnSubmit(formInfo, task.getId(), task.getProcessInstanceId(), null, null, taskVariables, task.getTenantId());

        }

        // Only one set of variables can be used as form submission.
        // When variablesLocal are present then they have precedence and those are used for the completion
        if (local) {
            TaskHelper.completeTask(task, userId, variables, taskVariables, transientVariables, transientVariablesLocal, commandContext);
        } else {
            TaskHelper.completeTask(task, userId, taskVariables, variablesLocal, transientVariables, transientVariablesLocal, commandContext);
        }

        if (processEngineConfiguration.getUserTaskStateInterceptor() != null) {
            processEngineConfiguration.getUserTaskStateInterceptor().handleCompleteWithForm(task, formInfo, userId, outcome, taskVariables);
        }

        return null;
    }

    protected boolean isFormFieldValidationEnabled(TaskEntity task, ProcessEngineConfigurationImpl processEngineConfiguration, String processDefinitionId,
        String taskDefinitionKey) {
        if (processEngineConfiguration.isFormFieldValidationEnabled()) {
            if (ProcessDefinitionUtil.getBpmnModel(processDefinitionId).getFlowElement(taskDefinitionKey) instanceof HasValidateFormFields validateFormFields){
                String formFieldValidationExpression = validateFormFields.getValidateFormFields();
                return TaskHelper.isFormFieldValidationEnabled(task, processEngineConfiguration, formFieldValidationExpression);
            }
        }
        return false;
    }

    @Override
    protected String getSuspendedTaskExceptionPrefix() {
        return "Cannot complete";
    }

}
