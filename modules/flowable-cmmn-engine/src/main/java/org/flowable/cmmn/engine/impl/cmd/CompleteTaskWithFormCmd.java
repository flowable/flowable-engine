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
package org.flowable.cmmn.engine.impl.cmd;

import static org.flowable.cmmn.engine.impl.task.TaskHelper.logUserTaskCompleted;

import java.util.Map;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.task.TaskHelper;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.form.api.FormFieldHandler;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.task.service.delegate.TaskListener;
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
        FormService formService = CommandContextUtil.getFormService(commandContext);
        if (formService == null) {
            throw new FlowableIllegalArgumentException("Form engine is not initialized");
        }

        FormRepositoryService formRepositoryService = CommandContextUtil.getFormRepositoryService(commandContext);
        FormInfo formInfo = formRepositoryService.getFormModelById(formDefinitionId);

        if (formInfo != null) {
            // Extract raw variables and complete the task
            Map<String, Object> formVariables = formService.getVariablesFromFormSubmission(formInfo, variables, outcome);

            if (task.getProcessInstanceId() != null) {
                formService.saveFormInstance(formVariables, formInfo, task.getId(), task.getProcessInstanceId(), 
                                task.getProcessDefinitionId(), task.getTenantId());
                
            } else if (task.getScopeId() != null) {
                formService.saveFormInstanceWithScopeId(formVariables, formInfo, task.getId(), task.getScopeId(), task.getScopeType(), 
                                task.getScopeDefinitionId(), task.getTenantId());
            }

            CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
            FormFieldHandler formFieldHandler = cmmnEngineConfiguration.getFormFieldHandler();
            if (isFormFieldValidationEnabled(cmmnEngineConfiguration, task)) {
                formFieldHandler.validateFormFieldsOnSubmit(formInfo, task.getId(), variables);
            }
            formFieldHandler.handleFormFieldsOnSubmit(formInfo, task.getId(), null, task.getScopeId(),
                            task.getScopeType(), variables, task.getTenantId());

            completeTask(commandContext, task, formVariables);

        } else {
            completeTask(commandContext, task, variables);
        }
        
        return null;
    }

    protected boolean isFormFieldValidationEnabled(CmmnEngineConfiguration cmmnEngineConfiguration, TaskEntity task) {
        if (cmmnEngineConfiguration.isFormFieldValidationEnabled()) {
            HumanTask humanTask = (HumanTask) cmmnEngineConfiguration.getCmmnRepositoryService().
                getCmmnModel(task.getScopeDefinitionId()).
                findPlanItemDefinition(task.getTaskDefinitionKey());
            String formFieldValidationExpression = humanTask.getValidateFormFields();

            return TaskHelper.isFormFieldValidationEnabled(task, cmmnEngineConfiguration, formFieldValidationExpression);
        }
        return false;
    }

    protected void completeTask(CommandContext commandContext, TaskEntity task, Map<String, Object> taskVariables) {
        String planItemInstanceId = task.getSubScopeId();
        PlanItemInstanceEntity planItemInstanceEntity = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).findById(planItemInstanceId);
        if (planItemInstanceEntity == null) {
            throw new FlowableException("Could not find plan item instance for task " + taskId);
        }
        
        if (taskVariables != null) {
            if (localScope) {
                task.setVariablesLocal(taskVariables);
            } else {
                task.setVariables(taskVariables);
            }
        }
        
        if (transientVariables != null) {
            if (localScope) {
                task.setTransientVariablesLocal(transientVariables);
            } else {
                task.setTransientVariables(transientVariables);
            }
        }

        logUserTaskCompleted(task);

        CommandContextUtil.getInternalTaskAssignmentManager(commandContext).addUserIdentityLinkToParent(task, Authentication.getAuthenticatedUserId());
        CommandContextUtil.getCmmnEngineConfiguration(commandContext).getListenerNotificationHelper().executeTaskListeners(task, TaskListener.EVENTNAME_COMPLETE);

        CommandContextUtil.getAgenda(commandContext).planTriggerPlanItemInstanceOperation(planItemInstanceEntity);
    }

    @Override
    protected String getSuspendedTaskException() {
        return "Cannot complete a suspended task";
    }

}
