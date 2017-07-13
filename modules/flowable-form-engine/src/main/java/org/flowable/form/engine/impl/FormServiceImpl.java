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
package org.flowable.form.engine.impl;

import java.util.Map;

import org.flowable.form.api.FormInstance;
import org.flowable.form.api.FormInstanceQuery;
import org.flowable.form.api.FormService;
import org.flowable.form.engine.impl.cmd.CreateFormInstanceCmd;
import org.flowable.form.engine.impl.cmd.GetFormInstanceModelCmd;
import org.flowable.form.engine.impl.cmd.GetFormModelWithVariablesCmd;
import org.flowable.form.engine.impl.cmd.GetVariablesFromFormSubmissionCmd;
import org.flowable.form.engine.impl.cmd.SaveFormInstanceCmd;
import org.flowable.form.model.FormInstanceModel;
import org.flowable.form.model.FormModel;

/**
 * @author Tijs Rademakers
 */
public class FormServiceImpl extends ServiceImpl implements FormService {

    public Map<String, Object> getVariablesFromFormSubmission(FormModel formModel, Map<String, Object> values) {
        return commandExecutor.execute(new GetVariablesFromFormSubmissionCmd(formModel, values));
    }

    public Map<String, Object> getVariablesFromFormSubmission(FormModel formModel, Map<String, Object> values, String outcome) {
        return commandExecutor.execute(new GetVariablesFromFormSubmissionCmd(formModel, values, outcome));
    }

    public FormInstance createFormInstance(Map<String, Object> variables, FormModel formModel, String taskId, String processInstanceId) {
        return commandExecutor.execute(new CreateFormInstanceCmd(formModel, variables, taskId, processInstanceId));
    }

    public FormInstance saveFormInstance(Map<String, Object> variables, FormModel formModel, String taskId, String processInstanceId) {
        return commandExecutor.execute(new SaveFormInstanceCmd(formModel, variables, taskId, processInstanceId));
    }

    public FormInstance saveFormInstanceByFormModelId(Map<String, Object> variables, String formModelId, String taskId, String processInstanceId) {
        return commandExecutor.execute(new SaveFormInstanceCmd(formModelId, variables, taskId, processInstanceId));
    }

    public FormModel getFormModelWithVariablesById(String formDefinitionId, String processInstanceId, String taskId, Map<String, Object> variables) {
        return commandExecutor.execute(new GetFormModelWithVariablesCmd(null, formDefinitionId, processInstanceId, taskId, variables));
    }

    public FormModel getFormModelWithVariablesById(String formId, String processInstanceId, String taskId,
                                                   Map<String, Object> variables, String tenantId) {
        return commandExecutor.execute(new GetFormModelWithVariablesCmd(null, formId, null, processInstanceId, taskId, tenantId, variables));
    }

    public FormModel getFormModelWithVariablesByKey(String formDefinitionKey, String processInstanceId, String taskId, Map<String, Object> variables) {
        return commandExecutor.execute(new GetFormModelWithVariablesCmd(formDefinitionKey, null, processInstanceId, taskId, variables));
    }

    public FormModel getFormModelWithVariablesByKey(String formDefinitionKey, String processInstanceId, String taskId,
                                                    Map<String, Object> variables, String tenantId) {

        return commandExecutor.execute(new GetFormModelWithVariablesCmd(formDefinitionKey, null, null, processInstanceId, taskId, tenantId, variables));
    }

    public FormModel getFormModelWithVariablesByKeyAndParentDeploymentId(String formDefinitionKey, String parentDeploymentId,
                                                                         String processInstanceId, String taskId, Map<String, Object> variables) {

        return commandExecutor.execute(new GetFormModelWithVariablesCmd(formDefinitionKey, parentDeploymentId, null, processInstanceId, taskId, variables));
    }

    public FormModel getFormModelWithVariablesByKeyAndParentDeploymentId(String formDefinitionKey, String parentDeploymentId, String processInstanceId, String taskId,
                                                                         Map<String, Object> variables, String tenantId) {

        return commandExecutor.execute(new GetFormModelWithVariablesCmd(formDefinitionKey, parentDeploymentId, null, processInstanceId, taskId, tenantId, variables));
    }

    public FormInstanceModel getFormInstanceModelById(String formInstanceId, Map<String, Object> variables) {

        return commandExecutor.execute(new GetFormInstanceModelCmd(formInstanceId, variables));
    }

    public FormInstanceModel getFormInstanceModelById(String formDefinitionId, String taskId, String processInstanceId, Map<String, Object> variables) {
        return commandExecutor.execute(new GetFormInstanceModelCmd(null, formDefinitionId, taskId, processInstanceId, variables));
    }

    public FormInstanceModel getFormInstanceModelById(String formDefinitionId, String taskId, String processInstanceId,
            Map<String, Object> variables, String tenantId) {
        return commandExecutor.execute(new GetFormInstanceModelCmd(null, formDefinitionId, taskId, processInstanceId, tenantId, variables));
    }

    public FormInstanceModel getFormInstanceModelByKey(String formDefinitionKey, String taskId, String processInstanceId, Map<String, Object> variables) {
        return commandExecutor.execute(new GetFormInstanceModelCmd(formDefinitionKey, null, taskId, processInstanceId, variables));
    }

    public FormInstanceModel getFormInstanceModelByKey(String formDefinitionKey, String taskId, String processInstanceId,
            Map<String, Object> variables, String tenantId) {

        return commandExecutor.execute(new GetFormInstanceModelCmd(formDefinitionKey, null, null, taskId, processInstanceId, tenantId, variables));
    }

    public FormInstanceModel getFormInstanceModelByKeyAndParentDeploymentId(String formDefinitionKey, String parentDeploymentId,
            String taskId, String processInstanceId, Map<String, Object> variables) {

        return commandExecutor.execute(new GetFormInstanceModelCmd(formDefinitionKey, parentDeploymentId, null, taskId, processInstanceId, variables));
    }

    public FormInstanceModel getFormInstanceModelByKeyAndParentDeploymentId(String formDefinitionKey, String parentDeploymentId,
            String taskId, String processInstanceId, Map<String, Object> variables, String tenantId) {

        return commandExecutor.execute(new GetFormInstanceModelCmd(formDefinitionKey, parentDeploymentId, null,
                taskId, processInstanceId, tenantId, variables));
    }

    public FormInstanceQuery createFormInstanceQuery() {
        return new FormInstanceQueryImpl(commandExecutor);
    }
}
