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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.flowable.content.api.ContentItem;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.engine.history.HistoricTaskInstance;
import org.flowable.engine.history.HistoricVariableInstance;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.interceptor.Command;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.form.model.FormField;
import org.flowable.form.model.FormFieldTypes;
import org.flowable.form.model.FormModel;

/**
 * @author Tijs Rademakers
 */
public class GetTaskFormModelCmd implements Command<FormModel>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String taskId;

    public GetTaskFormModelCmd(String taskId) {
        this.taskId = taskId;
    }

    public FormModel execute(CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = commandContext.getProcessEngineConfiguration();
        if (!processEngineConfiguration.isFormEngineInitialized()) {
            throw new FlowableIllegalArgumentException("Form engine is not initialized");
        }

        HistoricTaskInstance task = processEngineConfiguration.getHistoricTaskInstanceEntityManager().findById(taskId);
        if (task == null) {
            throw new FlowableObjectNotFoundException("Task not found with id " + taskId);
        }

        Map<String, Object> variables = new HashMap<String, Object>();
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

        FormModel formModel = null;
        if (task.getEndTime() != null) {
            formModel = processEngineConfiguration.getFormEngineFormService()
                    .getFormInstanceModelByKeyAndParentDeploymentId(task.getFormKey(), parentDeploymentId,
                            taskId, task.getProcessInstanceId(), variables, task.getTenantId());

        } else {
            formModel = processEngineConfiguration.getFormEngineFormService()
                    .getFormModelWithVariablesByKeyAndParentDeploymentId(task.getFormKey(), parentDeploymentId,
                            task.getProcessInstanceId(), variables, task.getTenantId());
        }

        // If form does not exists, we don't want to leak out this info to just anyone
        if (formModel == null) {
            throw new FlowableObjectNotFoundException("Form model for task " + task.getTaskDefinitionKey() + " cannot be found for form key " + task.getFormKey());
        }

        fetchRelatedContentInfoIfNeeded(formModel, processEngineConfiguration);

        return formModel;
    }

    protected void fetchRelatedContentInfoIfNeeded(FormModel formModel, ProcessEngineConfigurationImpl processEngineConfiguration) {
        if (!processEngineConfiguration.isContentEngineInitialized()) {
            return;
        }

        if (formModel.getFields() != null) {
            for (FormField formField : formModel.getFields()) {
                if (FormFieldTypes.UPLOAD.equals(formField.getType())) {

                    List<String> contentItemIds = null;
                    if (formField.getValue() instanceof List) {
                        contentItemIds = (List<String>) formField.getValue();

                    } else if (formField.getValue() instanceof String) {
                        String[] splittedString = ((String) formField.getValue()).split(",");
                        contentItemIds = new ArrayList<String>();
                        Collections.addAll(contentItemIds, splittedString);
                    }

                    if (contentItemIds != null) {
                        Set<String> contentItemIdSet = new HashSet<>(contentItemIds);

                        List<ContentItem> contentItems = processEngineConfiguration.getContentService()
                                .createContentItemQuery()
                                .ids(contentItemIdSet)
                                .list();

                        formField.setValue(contentItems);
                    }
                }
            }
        }
    }

}
