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

import org.flowable.form.model.FormInstanceModel;
import org.flowable.form.model.FormModel;

/**
 * @author Tijs Rademakers
 */
public interface FormService {

    /**
     * @param formModel form definition to use for type-conversion and validation
     * @param values values submitted by the user
     * @param outcome outcome selected by the user. If null, no outcome is used and any outcome definitions are ignored.
     * 
     * @return raw variables that can be used in the activiti engine, based on the filled in values and selected outcome.
     * @throws FormValidationException when a submitted value is not valid or a required value is missing.
     */
    Map<String, Object> getVariablesFromFormSubmission(FormModel formModel, Map<String, Object> values, String outcome);
    
    /**
     * Store the submitted form values.
     * 
       * @param formModel form instance of the submitted form
       * @param taskId task instance id of the completed task
       * @param processInstanceId process instance id of the completed task
       * @param values json node with the values of the
       */
    FormInstance createFormInstance(Map<String, Object> values, FormModel formModel, String taskId, String processInstanceId);
    
    FormModel getFormModelWithVariablesById(String formDefinitionId, String processInstanceId, Map<String, Object> variables);
    
    FormModel getFormModelWithVariablesById(String formDefinitionId, String processInstanceId, Map<String, Object> variables, String tenantId);
    
    FormModel getFormModelWithVariablesByKey(String formDefinitionKey, String processInstanceId, Map<String, Object> variables);
    
    FormModel getFormModelWithVariablesByKey(String formDefinitionKey, String processInstanceId, 
        Map<String, Object> variables, String tenantId);
    
    FormModel getFormModelWithVariablesByKeyAndParentDeploymentId(String formDefinitionKey, String parentDeploymentId, 
        String processInstanceId, Map<String, Object> variables);
    
    FormModel getFormModelWithVariablesByKeyAndParentDeploymentId(String formDefinitionKey, String parentDeploymentId, String processInstanceId, 
        Map<String, Object> variables, String tenantId);

    FormInstanceModel getFormInstanceModelById(String formInstanceId, Map<String, Object> variables);

    FormInstanceModel getFormInstanceModelById(String formDefinitionId, String taskId, String processInstanceId, Map<String, Object> variables);
    
    FormInstanceModel getFormInstanceModelById(String formDefinitionId, String taskId, String processInstanceId,
        Map<String, Object> variables, String tenantId);
    
    FormInstanceModel getFormInstanceModelByKey(String formDefinitionKey, String taskId, String processInstanceId, Map<String, Object> variables);
    
    FormInstanceModel getFormInstanceModelByKey(String formDefinitionKey, String taskId, String processInstanceId, 
        Map<String, Object> variables, String tenantId);
    
    FormInstanceModel getFormInstanceModelByKeyAndParentDeploymentId(String formDefinitionKey, String parentDeploymentId,
        String taskId, String processInstanceId, Map<String, Object> variables);
    
    FormInstanceModel getFormInstanceModelByKeyAndParentDeploymentId(String formDefinitionKey, String parentDeploymentId,
        String taskId, String processInstanceId, Map<String, Object> variables, String tenantId);
    
    FormInstanceQuery createFormInstanceQuery();
}
