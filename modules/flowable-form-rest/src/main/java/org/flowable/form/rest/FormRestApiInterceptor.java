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
package org.flowable.form.rest;

import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormDefinitionQuery;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormDeploymentQuery;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormInstance;
import org.flowable.form.api.FormInstanceQuery;
import org.flowable.form.rest.service.api.form.FormInstanceQueryRequest;
import org.flowable.form.rest.service.api.form.FormRequest;

public interface FormRestApiInterceptor {
    
    void accessFormInfoById(FormInfo formInfo, FormRequest formRequest);

    void accessFormInstanceById(FormInstance formInstance);
    
    void accessFormInstanceInfoWithQuery(FormInstanceQuery formInstanceQuery, FormInstanceQueryRequest request);
    
    void storeFormInstance(FormRequest formRequest);
    
    void accessFormDefinitionInfoById(FormDefinition formDefinition);
    
    void accessFormDefinitionInfoWithQuery(FormDefinitionQuery formDefinitionQuery);
    
    void accessDeploymentById(FormDeployment deployment);
    
    void accessDeploymentsWithQuery(FormDeploymentQuery deploymentQuery);
    
    void executeNewDeploymentForTenantId(String tenantId);
    
    void deleteDeployment(FormDeployment deployment);
    
    void accessFormManagementInfo();
}
