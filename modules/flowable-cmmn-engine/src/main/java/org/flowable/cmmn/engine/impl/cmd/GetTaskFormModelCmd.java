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

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.form.api.FormFieldHandler;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;

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
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        FormService formService = CommandContextUtil.getFormService();
        if (formService == null) {
            throw new FlowableIllegalArgumentException("Form engine is not initialized");
        }

        TaskInfo task = CommandContextUtil.getTaskService().getTask(taskId);
        Date endTime = null;
        if (task == null) {
            task = CommandContextUtil.getHistoricTaskService().getHistoricTask(taskId);
            if (task != null) {
                endTime = ((HistoricTaskInstance) task).getEndTime();
            }
        }
        
        if (task == null) {
            throw new FlowableObjectNotFoundException("Task not found with id " + taskId);
        }

        Map<String, Object> variables = new HashMap<>();
        if (!ignoreVariables && task.getScopeId() != null) {
            List<HistoricVariableInstance> variableInstances = cmmnEngineConfiguration.getCmmnHistoryService()
                    .createHistoricVariableInstanceQuery()
                    .caseInstanceId(task.getScopeId())
                    .list();

            for (HistoricVariableInstance historicVariableInstance : variableInstances) {
                variables.put(historicVariableInstance.getVariableName(), historicVariableInstance.getValue());
            }
        }

        String parentDeploymentId = null;
        if (StringUtils.isNotEmpty(task.getScopeDefinitionId())) {
            CmmnRepositoryService cmmnRepositoryService = cmmnEngineConfiguration.getCmmnRepositoryService();
            CaseDefinition caseDefinition = cmmnRepositoryService.getCaseDefinition(task.getScopeDefinitionId());
            CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeploymentQuery().deploymentId(caseDefinition.getDeploymentId()).singleResult();
            if (cmmnDeployment.getParentDeploymentId() != null) {
                parentDeploymentId = cmmnDeployment.getParentDeploymentId();
            } else {
                parentDeploymentId = cmmnDeployment.getId();
            }
        }

        FormInfo formInfo = null;
        if (ignoreVariables) {
            FormRepositoryService formRepositoryService = CommandContextUtil.getFormRepositoryService();
            formInfo = formRepositoryService.getFormModelByKeyAndParentDeploymentId(task.getFormKey(), parentDeploymentId);
            
            // fallback to search for a form model without parent deployment id
            if (formInfo == null && parentDeploymentId != null) {
                formInfo = formRepositoryService.getFormModelByKey(task.getFormKey());
            }
            
        } else if (endTime != null) {
            formInfo = formService.getFormInstanceModelByKeyAndParentDeploymentIdAndScopeId(task.getFormKey(), parentDeploymentId, task.getScopeId(), 
                            task.getScopeType(), variables, task.getTenantId());
            
            // fallback to search for a form model without parent deployment id
            if (formInfo == null && parentDeploymentId != null) {
                formInfo = formService.getFormInstanceModelByKeyAndScopeId(task.getFormKey(), task.getScopeId(), 
                                task.getScopeType(), variables, task.getTenantId());
            }

        } else {
            formInfo = formService.getFormModelWithVariablesByKeyAndParentDeploymentId(task.getFormKey(), parentDeploymentId,
                            taskId, variables, task.getTenantId());
            
            // fallback to search for a form model without parent deployment id
            if (formInfo == null && parentDeploymentId != null) {
                formInfo = formService.getFormModelWithVariablesByKey(task.getFormKey(), taskId, variables, task.getTenantId());
            }
        }

        // If form does not exists, we don't want to leak out this info to just anyone
        if (formInfo == null) {
            throw new FlowableObjectNotFoundException("Form model for task " + task.getTaskDefinitionKey() + " cannot be found for form key " + task.getFormKey());
        }

        FormFieldHandler formFieldHandler = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getFormFieldHandler();
        formFieldHandler.enrichFormFields(formInfo);

        return formInfo;
    }

}
