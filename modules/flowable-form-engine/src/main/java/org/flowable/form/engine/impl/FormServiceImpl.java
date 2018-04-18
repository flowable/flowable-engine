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

import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormInstance;
import org.flowable.form.api.FormInstanceInfo;
import org.flowable.form.api.FormInstanceQuery;
import org.flowable.form.api.FormService;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.cmd.CreateFormInstanceCmd;
import org.flowable.form.engine.impl.cmd.GetFormInstanceModelCmd;
import org.flowable.form.engine.impl.cmd.GetFormModelWithVariablesCmd;
import org.flowable.form.engine.impl.cmd.GetVariablesFromFormSubmissionCmd;
import org.flowable.form.engine.impl.cmd.SaveFormInstanceCmd;

/**
 * @author Tijs Rademakers
 */
public class FormServiceImpl extends CommonEngineServiceImpl<FormEngineConfiguration> implements FormService {

    public FormServiceImpl(FormEngineConfiguration engineConfiguration) {
        super(engineConfiguration);
    }

    public Map<String, Object> getVariablesFromFormSubmission(FormInfo formInfo, Map<String, Object> values) {
        return commandExecutor.execute(new GetVariablesFromFormSubmissionCmd(formInfo, values));
    }

    @Override
    public Map<String, Object> getVariablesFromFormSubmission(FormInfo formInfo, Map<String, Object> values, String outcome) {
        return commandExecutor.execute(new GetVariablesFromFormSubmissionCmd(formInfo, values, outcome));
    }

    @Override
    public FormInstance createFormInstance(Map<String, Object> variables, FormInfo formInfo, String taskId, String processInstanceId, String processDefinitionId) {
        return commandExecutor.execute(new CreateFormInstanceCmd(formInfo, variables, taskId, processInstanceId, processDefinitionId));
    }

    @Override
    public FormInstance saveFormInstance(Map<String, Object> variables, FormInfo formInfo, String taskId, String processInstanceId, String processDefinitionId) {
        return commandExecutor.execute(new SaveFormInstanceCmd(formInfo, variables, taskId, processInstanceId, processDefinitionId));
    }

    @Override
    public FormInstance saveFormInstanceByFormDefinitionId(Map<String, Object> variables, String formDefinitionId, String taskId, String processInstanceId, String processDefinitionId) {
        return commandExecutor.execute(new SaveFormInstanceCmd(formDefinitionId, variables, taskId, processInstanceId, processDefinitionId));
    }
    
    @Override
    public FormInstance createFormInstanceWithScopeId(Map<String, Object> variables, FormInfo formInfo, String taskId, String scopeId, String scopeType, String scopeDefinitionId) {
        return commandExecutor.execute(new CreateFormInstanceCmd(formInfo, variables, taskId, scopeId, scopeType, scopeDefinitionId));
    }

    @Override
    public FormInstance saveFormInstanceWithScopeId(Map<String, Object> variables, FormInfo formInfo, String taskId, String scopeId, String scopeType, String scopeDefinitionId) {
        return commandExecutor.execute(new SaveFormInstanceCmd(formInfo, variables, taskId, scopeId, scopeType, scopeDefinitionId));
    }

    @Override
    public FormInstance saveFormInstanceWithScopeId(Map<String, Object> variables, String formModelId, String taskId, String scopeId, String scopeType, String scopeDefinitionId) {
        return commandExecutor.execute(new SaveFormInstanceCmd(formModelId, variables, taskId, scopeId, scopeType, scopeDefinitionId));
    }

    @Override
    public FormInfo getFormModelWithVariablesById(String formDefinitionId, String taskId, Map<String, Object> variables) {
        return commandExecutor.execute(new GetFormModelWithVariablesCmd(null, formDefinitionId, taskId, variables));
    }

    @Override
    public FormInfo getFormModelWithVariablesById(String formId, String taskId,Map<String, Object> variables, String tenantId) {
        return commandExecutor.execute(new GetFormModelWithVariablesCmd(null, formId, null, taskId, tenantId, variables));
    }

    @Override
    public FormInfo getFormModelWithVariablesByKey(String formDefinitionKey, String taskId, Map<String, Object> variables) {
        return commandExecutor.execute(new GetFormModelWithVariablesCmd(formDefinitionKey, null, taskId, variables));
    }

    @Override
    public FormInfo getFormModelWithVariablesByKey(String formDefinitionKey, String taskId, Map<String, Object> variables, String tenantId) {
        return commandExecutor.execute(new GetFormModelWithVariablesCmd(formDefinitionKey, null, null, taskId, tenantId, variables));
    }

    @Override
    public FormInfo getFormModelWithVariablesByKeyAndParentDeploymentId(String formDefinitionKey, String parentDeploymentId,
                                                                         String taskId, Map<String, Object> variables) {

        return commandExecutor.execute(new GetFormModelWithVariablesCmd(formDefinitionKey, parentDeploymentId, null, taskId, variables));
    }

    @Override
    public FormInfo getFormModelWithVariablesByKeyAndParentDeploymentId(String formDefinitionKey, String parentDeploymentId, String taskId,
                                                                         Map<String, Object> variables, String tenantId) {

        return commandExecutor.execute(new GetFormModelWithVariablesCmd(formDefinitionKey, parentDeploymentId, null, taskId, tenantId, variables));
    }

    @Override
    public FormInstanceInfo getFormInstanceModelById(String formInstanceId, Map<String, Object> variables) {

        return commandExecutor.execute(new GetFormInstanceModelCmd(formInstanceId, variables));
    }

    @Override
    public FormInstanceInfo getFormInstanceModelById(String formDefinitionId, String taskId, String processInstanceId, Map<String, Object> variables) {
        return commandExecutor.execute(new GetFormInstanceModelCmd(null, formDefinitionId, taskId, processInstanceId, variables));
    }

    @Override
    public FormInstanceInfo getFormInstanceModelById(String formDefinitionId, String taskId, String processInstanceId,
            Map<String, Object> variables, String tenantId) {
        return commandExecutor.execute(new GetFormInstanceModelCmd(null, formDefinitionId, taskId, processInstanceId, tenantId, variables));
    }

    @Override
    public FormInstanceInfo getFormInstanceModelByKey(String formDefinitionKey, String taskId, String processInstanceId, Map<String, Object> variables) {
        return commandExecutor.execute(new GetFormInstanceModelCmd(formDefinitionKey, null, taskId, processInstanceId, variables));
    }

    @Override
    public FormInstanceInfo getFormInstanceModelByKey(String formDefinitionKey, String taskId, String processInstanceId,
            Map<String, Object> variables, String tenantId) {

        return commandExecutor.execute(new GetFormInstanceModelCmd(formDefinitionKey, null, null, taskId, processInstanceId, tenantId, variables));
    }

    @Override
    public FormInstanceInfo getFormInstanceModelByKeyAndParentDeploymentId(String formDefinitionKey, String parentDeploymentId,
            String taskId, String processInstanceId, Map<String, Object> variables) {

        return commandExecutor.execute(new GetFormInstanceModelCmd(formDefinitionKey, parentDeploymentId, null, taskId, processInstanceId, variables));
    }

    @Override
    public FormInstanceInfo getFormInstanceModelByKeyAndParentDeploymentId(String formDefinitionKey, String parentDeploymentId,
            String taskId, String processInstanceId, Map<String, Object> variables, String tenantId) {

        return commandExecutor.execute(new GetFormInstanceModelCmd(formDefinitionKey, parentDeploymentId, null,
                taskId, processInstanceId, tenantId, variables));
    }

    @Override
    public FormInstanceQuery createFormInstanceQuery() {
        return new FormInstanceQueryImpl(commandExecutor);
    }
}
