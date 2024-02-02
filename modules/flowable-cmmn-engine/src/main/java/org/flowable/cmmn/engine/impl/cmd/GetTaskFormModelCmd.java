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
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.PlanItemDefinition;
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
        FormService formService = CommandContextUtil.getFormService();
        if (formService == null) {
            throw new FlowableIllegalArgumentException("Form engine is not initialized");
        }

        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        TaskInfo task = cmmnEngineConfiguration.getTaskServiceConfiguration().getTaskService().getTask(taskId);
        Date endTime = null;
        if (task == null) {
            task = cmmnEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().getHistoricTask(taskId);
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
            PlanItemDefinition itemDefinition = CaseDefinitionUtil.getCmmnModel(task.getScopeDefinitionId()).findPlanItemDefinition(task.getTaskDefinitionKey());
            boolean sameDeployment = true;
            if (itemDefinition instanceof HumanTask) {
                sameDeployment = ((HumanTask) itemDefinition).isSameDeployment();
            }

            if (sameDeployment) {
                // If it is not same deployment then there is no need to search for parent deployment
                parentDeploymentId = CaseDefinitionUtil.getDefinitionDeploymentId(task.getScopeDefinitionId(), cmmnEngineConfiguration);
            }
        }

        FormInfo formInfo = null;
        if (ignoreVariables) {
            FormRepositoryService formRepositoryService = CommandContextUtil.getFormRepositoryService();
            formInfo = formRepositoryService.getFormModelByKeyAndParentDeploymentId(task.getFormKey(), parentDeploymentId, 
                            task.getTenantId(), cmmnEngineConfiguration.isFallbackToDefaultTenant());
            
        } else if (endTime != null) {
            formInfo = formService.getFormInstanceModelByKeyAndParentDeploymentIdAndScopeId(task.getFormKey(), parentDeploymentId, task.getScopeId(), 
                            task.getScopeType(), variables, task.getTenantId(), cmmnEngineConfiguration.isFallbackToDefaultTenant());

        } else {
            formInfo = formService.getFormModelWithVariablesByKeyAndParentDeploymentId(task.getFormKey(), parentDeploymentId,
                            taskId, variables, task.getTenantId(), cmmnEngineConfiguration.isFallbackToDefaultTenant());
        }

        // If form does not exists, we don't want to leak out this info to just anyone
        if (formInfo == null) {
            throw new FlowableObjectNotFoundException("Form model for task " + task.getTaskDefinitionKey() + " cannot be found for form key " + task.getFormKey());
        }

        FormFieldHandler formFieldHandler = cmmnEngineConfiguration.getFormFieldHandler();
        formFieldHandler.enrichFormFields(formInfo);

        return formInfo;
    }

}
