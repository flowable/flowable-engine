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
package org.activiti.form.api;

import java.io.InputStream;
import java.util.List;

import org.activiti.form.model.FormModel;

/**
 * Service providing access to the repository of forms.
 *
 * @author Tijs Rademakers
 */
public interface FormRepositoryService {

    FormDeploymentBuilder createDeployment();

    void deleteDeployment(String deploymentId);
    
    FormDefinitionQuery createFormDefinitionQuery();
    
    NativeFormDefinitionQuery createNativeFormDefinitionQuery();

    void setDeploymentCategory(String deploymentId, String category);
    
    void setDeploymentTenantId(String deploymentId, String newTenantId);

    List<String> getDeploymentResourceNames(String deploymentId);

    InputStream getResourceAsStream(String deploymentId, String resourceName);
    
    FormDeploymentQuery createDeploymentQuery();
    
    NativeFormDeploymentQuery createNativeDeploymentQuery();
    
    FormDefinition getFormDefinition(String formDefinitionId);

    FormModel getFormModelById(String formDefinitionId);
    
    FormModel getFormModelByKey(String formDefinitionKey);
    
    FormModel getFormModelByKey(String formDefinitionKey, String tenantId);
    
    FormModel getFormModelByKeyAndParentDeploymentId(String formDefinitionKey, String parentDeploymentId);
    
    FormModel getFormModelByKeyAndParentDeploymentId(String formDefinitionKey, String parentDeploymentId, String tenantId);
    
    InputStream getFormDefinitionResource(String formDefinitionId);
    
    void setFormDefinitionCategory(String formDefinitionId, String category);
}
