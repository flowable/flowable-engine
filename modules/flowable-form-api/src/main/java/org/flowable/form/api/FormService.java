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
package org.flowable.form.api;

import java.util.Map;

/**
 * @author Tijs Rademakers
 */
public interface FormService {

    /**
     * Apply validation restrictions on the submitted variables
     *
     * @param formInfo
     *     form description
     * @param values
     *     submitted variables
     * @throws org.flowable.common.engine.api.FlowableException in the case when validation failed
     */
    void validateFormFields(FormInfo formInfo, Map<String, Object> values);

    /**
     * @param formInfo
     *            form definition to use for type-conversion and validation
     * @param values
     *            values submitted by the user
     * @param outcome
     *            outcome selected by the user. If null, no outcome is used and any outcome definitions are ignored.
     *
     * @return raw variables that can be used in the process engine, based on the filled in values and selected outcome.
     */
    Map<String, Object> getVariablesFromFormSubmission(FormInfo formInfo, Map<String, Object> values, String outcome);

    /**
     * Store the submitted form values.
     * 
     * @param formInfo
     *            form instance of the submitted form
     * @param taskId
     *            task instance id of the completed task
     * @param processInstanceId
     *            process instance id of the completed task
     * @param variables
     *            json node with the values of the
     */
    FormInstance createFormInstance(Map<String, Object> variables, FormInfo formInfo, String taskId, String processInstanceId, String processDefinitionId,
        String tenantId, String outcome);

    FormInstance saveFormInstance(Map<String, Object> variables, FormInfo formInfo, String taskId, String processInstanceId, String processDefinitionId,
        String tenantId, String outcome);

    FormInstance saveFormInstanceByFormDefinitionId(Map<String, Object> variables, String formDefinitionId, String taskId, String processInstanceId,
        String processDefinitionId, String tenantId, String outcome);
    
    FormInstance createFormInstanceWithScopeId(Map<String, Object> variables, FormInfo formInfo, String taskId, String scopeId, String scopeType,
        String scopeDefinitionId, String tenantId, String outcome);

    FormInstance saveFormInstanceWithScopeId(Map<String, Object> variables, FormInfo formInfo, String taskId, String scopeId, String scopeType,
        String scopeDefinitionId, String tenantId, String outcome);

    FormInstance saveFormInstanceWithScopeId(Map<String, Object> variables, String formDefinitionId, String taskId, String scopeId, String scopeType,
        String scopeDefinitionId, String tenantId, String outcome);

    FormInfo getFormModelWithVariablesById(String formDefinitionId, String taskId, Map<String, Object> variables);

    FormInfo getFormModelWithVariablesById(String formDefinitionId, String taskId, Map<String, Object> variables, String tenantId, boolean fallbackToDefaultTenant);

    FormInfo getFormModelWithVariablesByKey(String formDefinitionKey, String taskId, Map<String, Object> variables);

    FormInfo getFormModelWithVariablesByKey(String formDefinitionKey, String taskId, Map<String, Object> variables, String tenantId, boolean fallbackToDefaultTenant);

    FormInfo getFormModelWithVariablesByKeyAndParentDeploymentId(String formDefinitionKey, String parentDeploymentId, String taskId, Map<String, Object> variables);

    FormInfo getFormModelWithVariablesByKeyAndParentDeploymentId(String formDefinitionKey, String parentDeploymentId,
                    String taskId, Map<String, Object> variables, String tenantId, boolean fallbackToDefaultTenant);

    FormInstanceInfo getFormInstanceModelById(String formInstanceId, Map<String, Object> variables);

    FormInstanceInfo getFormInstanceModelById(String formDefinitionId, String taskId, String processInstanceId, Map<String, Object> variables);

    FormInstanceInfo getFormInstanceModelById(String formDefinitionId, String taskId, String processInstanceId,
                    Map<String, Object> variables, String tenantId, boolean fallbackToDefaultTenant);

    FormInstanceInfo getFormInstanceModelByKey(String formDefinitionKey, String taskId, String processInstanceId, Map<String, Object> variables);

    FormInstanceInfo getFormInstanceModelByKey(String formDefinitionKey, String taskId, String processInstanceId,
                    Map<String, Object> variables, String tenantId, boolean fallbackToDefaultTenant);

    FormInstanceInfo getFormInstanceModelByKeyAndParentDeploymentId(String formDefinitionKey, String parentDeploymentId,
                    String taskId, String processInstanceId, Map<String, Object> variables);

    FormInstanceInfo getFormInstanceModelByKeyAndParentDeploymentId(String formDefinitionKey, String parentDeploymentId,
                    String taskId, String processInstanceId, Map<String, Object> variables, String tenantId, boolean fallbackToDefaultTenant);
    
    FormInstanceInfo getFormInstanceModelByKeyAndScopeId(String formDefinitionKey,
                    String scopeId, String scopeType, Map<String, Object> variables);

    FormInstanceInfo getFormInstanceModelByKeyAndScopeId(String formDefinitionKey,
                    String scopeId, String scopeType, Map<String, Object> variables, String tenantId, boolean fallbackToDefaultTenant);
    
    FormInstanceInfo getFormInstanceModelByKeyAndParentDeploymentIdAndScopeId(String formDefinitionKey, String parentDeploymentId,
                    String scopeId, String scopeType, Map<String, Object> variables);

    FormInstanceInfo getFormInstanceModelByKeyAndParentDeploymentIdAndScopeId(String formDefinitionKey, String parentDeploymentId,
                    String scopeId, String scopeType, Map<String, Object> variables, String tenantId, boolean fallbackToDefaultTenant);

    FormInstanceQuery createFormInstanceQuery();
    
    byte[] getFormInstanceValues(String formInstanceId);
    
    void deleteFormInstance(String formInstanceId);
    
    void deleteFormInstancesByFormDefinition(String formDefinitionId);
    
    void deleteFormInstancesByProcessDefinition(String processDefinitionId);
    
    void deleteFormInstancesByScopeDefinition(String scopeDefinitionId);
}
