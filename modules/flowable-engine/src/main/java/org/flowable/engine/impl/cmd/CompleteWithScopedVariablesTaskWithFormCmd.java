package org.flowable.engine.impl.cmd;

import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.util.ScopedVariableContainerHelper;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.impl.util.TaskHelper;
import org.flowable.form.api.FormFieldHandler;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

import java.util.Map;

/**
 * @author Ievgenii Bespal
 */
public class CompleteWithScopedVariablesTaskWithFormCmd extends NeedsActiveTaskCmd<Void> {
    private static final long serialVersionUID = 1L;
    protected String formDefinitionId;
    protected String outcome;
    private final ScopedVariableContainerHelper scopedVariableContainerHelper;
    protected Map<String, Object> variables;
    protected boolean localScope;

    public CompleteWithScopedVariablesTaskWithFormCmd(String taskId, String formDefinitionId, String outcome,
                                                      ScopedVariableContainerHelper scopedVariableContainerHelper) {
        super(taskId);
        this.formDefinitionId = formDefinitionId;
        this.outcome = outcome;
        this.scopedVariableContainerHelper = scopedVariableContainerHelper;
    }

    @Override
    protected Void execute(CommandContext commandContext, TaskEntity task) {
        // Backwards compatibility
        this.localScope = scopedVariableContainerHelper.hasVariablesLocal();

        if (scopedVariableContainerHelper.hasAnyVariables()) {
            this.variables = scopedVariableContainerHelper.getAllVariables();
        }

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
                formService.validateFormFields(formInfo, variables);
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

            TaskHelper.completeTaskWithScopedVariables(task, this.scopedVariableContainerHelper, commandContext);
        } else {
            TaskHelper.completeTaskWithScopedVariables(task, this.scopedVariableContainerHelper, commandContext);
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
