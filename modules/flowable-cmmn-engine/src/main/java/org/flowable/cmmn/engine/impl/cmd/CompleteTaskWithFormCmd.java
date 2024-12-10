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

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.task.TaskHelper;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.HasValidateFormFields;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.form.api.FormFieldHandler;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.task.service.delegate.TaskListener;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
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
    protected boolean localScope;

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
        this.localScope = localScope;
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
        this(taskId, formDefinitionId, outcome, variables, transientVariables);
        this.variablesLocal = variablesLocal;
        this.transientVariablesLocal = transientVariablesLocal;
    }
    
    public CompleteTaskWithFormCmd(String taskId, String formDefinitionId, String outcome, String userId,
            Map<String,Object> variables, Map<String, Object> variablesLocal, Map<String, Object> transientVariables, 
            Map<String, Object> transientVariablesLocal) {
        
        this(taskId, formDefinitionId, outcome, variables, variablesLocal, transientVariables, transientVariablesLocal);
        this.userId = userId;
    }

    @Override
    protected Void execute(CommandContext commandContext, TaskEntity task) {
        if (StringUtils.isNotEmpty(task.getProcessInstanceId())) {
            throw new FlowableException(task + " is created by the process engine and should be completed via the process engine API");
        }
        
        FormService formService = CommandContextUtil.getFormService(commandContext);
        if (formService == null) {
            throw new FlowableIllegalArgumentException("Form engine is not initialized");
        }

        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        FormRepositoryService formRepositoryService = CommandContextUtil.getFormRepositoryService(commandContext);
        FormInfo formInfo = formRepositoryService.getFormModelById(formDefinitionId);

        if (formInfo != null) {
            // validate input at first
            FormFieldHandler formFieldHandler = cmmnEngineConfiguration.getFormFieldHandler();
            if (isFormFieldValidationEnabled(task)) {
                formService.validateFormFields(task.getTaskDefinitionKey(), "humanTask", task.getScopeId(), 
                        task.getScopeDefinitionId(), ScopeTypes.CMMN, formInfo, variables);
            }
            // Extract raw variables and complete the task
            Map<String, Object> taskVariables = formService.getVariablesFromFormSubmission(task.getTaskDefinitionKey(), "humanTask", task.getScopeId(), 
                    task.getScopeDefinitionId(), ScopeTypes.CMMN, formInfo, variables, outcome);

            // The taskVariables are the variables that should be used when completing the task
            // the actual variables should instead be used when saving the form instances
            if (task.getProcessInstanceId() != null && variables != null) {
                formService.saveFormInstance(variables, formInfo, task.getId(), task.getProcessInstanceId(),
                                task.getProcessDefinitionId(), task.getTenantId(), outcome);
                
            } else if (task.getScopeId() != null && variables != null) {
                formService.saveFormInstanceWithScopeId(variables, formInfo, task.getId(), task.getScopeId(), task.getScopeType(),
                                task.getScopeDefinitionId(), task.getTenantId(), outcome);
            }

            formFieldHandler.handleFormFieldsOnSubmit(formInfo, task.getId(), null, task.getScopeId(),
                            task.getScopeType(), taskVariables, task.getTenantId());

            completeTask(commandContext, task, taskVariables);
            
            if (cmmnEngineConfiguration.getHumanTaskStateInterceptor() != null) {
                cmmnEngineConfiguration.getHumanTaskStateInterceptor().handleCompleteWithForm(task, formInfo, userId, outcome, taskVariables);
            }

        } else {
            completeTask(commandContext, task, variables);
            
            if (cmmnEngineConfiguration.getHumanTaskStateInterceptor() != null) {
                cmmnEngineConfiguration.getHumanTaskStateInterceptor().handleComplete(task, userId);
            }
        }
        
        return null;
    }

    protected boolean isFormFieldValidationEnabled(TaskEntity task) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();
        if (cmmnEngineConfiguration.isFormFieldValidationEnabled() && CaseDefinitionUtil.getCmmnModel(task.getScopeDefinitionId()).
                findPlanItemDefinition(task.getTaskDefinitionKey()) instanceof HasValidateFormFields hasValidateFormFields) {
            String formFieldValidationExpression = hasValidateFormFields.getValidateFormFields();
            return TaskHelper.isFormFieldValidationEnabled(task, cmmnEngineConfiguration, formFieldValidationExpression);
        }
        return false;
    }

    protected void completeTask(CommandContext commandContext, TaskEntity task, Map<String, Object> taskVariables) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        String planItemInstanceId = task.getSubScopeId();
        PlanItemInstanceEntity planItemInstanceEntity = cmmnEngineConfiguration.getPlanItemInstanceEntityManager().findById(planItemInstanceId);
        if (planItemInstanceEntity == null) {
            throw new FlowableException("Could not find plan item instance for " + task);
        }

        if (taskVariables != null) {
            if (localScope) { // backwards compatibility from before variableLocal was available in constructor
                task.setVariablesLocal(taskVariables);
            } else {
                task.setVariables(taskVariables);
            }
        }

        if (variablesLocal != null) {
            task.setVariablesLocal(variablesLocal);
        }

        if (transientVariables != null) {
            if (localScope) { // backwards compatibility from before variableLocal was available in constructor
                task.setTransientVariablesLocal(transientVariables);
            } else {
                task.setTransientVariables(transientVariables);
            }
        }

        if (transientVariablesLocal != null) {
            task.setTransientVariablesLocal(transientVariablesLocal);
        }
        
        if (userId != null) {
            task.setTempCompletedBy(userId);
        }

        logUserTaskCompleted(task, cmmnEngineConfiguration);

        if (cmmnEngineConfiguration.getIdentityLinkInterceptor() != null) {
            cmmnEngineConfiguration.getIdentityLinkInterceptor().handleCompleteTask(task);
        }
        
        cmmnEngineConfiguration.getListenerNotificationHelper().executeTaskListeners(task, TaskListener.EVENTNAME_COMPLETE);

        CommandContextUtil.getAgenda(commandContext).planTriggerPlanItemInstanceOperation(planItemInstanceEntity);
    }

    @Override
    protected String getSuspendedTaskExceptionPrefix() {
        return "Cannot complete";
    }

}
