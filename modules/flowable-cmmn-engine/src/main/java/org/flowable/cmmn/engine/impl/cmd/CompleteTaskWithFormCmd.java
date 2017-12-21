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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.content.api.ContentItem;
import org.flowable.content.api.ContentService;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.form.model.FormField;
import org.flowable.form.model.FormFieldTypes;
import org.flowable.form.model.FormModel;
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
        FormModel formModel = formRepositoryService.getFormModelById(formDefinitionId);

        if (formModel != null) {
            // Extract raw variables and complete the task
            Map<String, Object> formVariables = formService.getVariablesFromFormSubmission(formModel, variables, outcome);

            if (task.getProcessInstanceId() != null) {
                formService.saveFormInstance(formVariables, formModel, task.getId(), task.getProcessInstanceId(), task.getProcessDefinitionId());
            } else if (task.getScopeId() != null) {
                formService.saveFormInstanceWithScopeId(formVariables, formModel, task.getId(), task.getScopeId(), task.getScopeType(), task.getScopeDefinitionId());
            }

            processUploadFieldsIfNeeded(formModel, task, commandContext);

            completeTask(commandContext, task, formVariables);

        } else {
            completeTask(commandContext, task, variables);
        }
        
        return null;
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
        
        CommandContextUtil.getAgenda(commandContext).planTriggerPlanItemInstanceOperation(planItemInstanceEntity);
    }

    /**
     * When content is uploaded for a field, it is uploaded as a 'temporary related content'. Now that the task is completed, we need to associate the field/taskId/processInstanceId with the related
     * content so we can retrieve it later.
     */
    protected void processUploadFieldsIfNeeded(FormModel formModel, TaskEntity task, CommandContext commandContext) {
        ContentService contentService = CommandContextUtil.getContentService();
        if (contentService == null) {
            return;
        }

        if (formModel != null && formModel.getFields() != null) {
            for (FormField formField : formModel.getFields()) {
                if (FormFieldTypes.UPLOAD.equals(formField.getType())) {

                    String variableName = formField.getId();
                    if (variables.containsKey(variableName)) {
                        String variableValue = (String) variables.get(variableName);
                        if (StringUtils.isNotEmpty(variableValue)) {
                            String[] contentItemIds = StringUtils.split(variableValue, ",");
                            Set<String> contentItemIdSet = new HashSet<>();
                            Collections.addAll(contentItemIdSet, contentItemIds);

                            List<ContentItem> contentItems = contentService.createContentItemQuery().ids(contentItemIdSet).list();

                            for (ContentItem contentItem : contentItems) {
                                contentItem.setTaskId(task.getId());
                                contentItem.setProcessInstanceId(task.getProcessInstanceId());
                                contentItem.setField(formField.getId());
                                contentService.saveContentItem(contentItem);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected String getSuspendedTaskException() {
        return "Cannot complete a suspended task";
    }

}
