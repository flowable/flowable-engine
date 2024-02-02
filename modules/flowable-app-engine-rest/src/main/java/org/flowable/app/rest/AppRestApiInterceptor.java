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
package org.flowable.app.rest;

import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.api.repository.AppDefinitionQuery;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.api.repository.AppDeploymentBuilder;
import org.flowable.app.api.repository.AppDeploymentQuery;

public interface AppRestApiInterceptor {
    
    void accessAppDefinitionInfoById(AppDefinition appDefinition);
    
    void accessAppDefinitionInfoWithQuery(AppDefinitionQuery appDefinitionQuery);
    
    void accessDeploymentById(AppDeployment deployment);
    
    void accessDeploymentsWithQuery(AppDeploymentQuery deploymentQuery);
    
    void executeNewDeploymentForTenantId(String tenantId);

    void enhanceDeployment(AppDeploymentBuilder deploymentBuilder);
    
    void deleteDeployment(AppDeployment deployment);
    
    void accessAppManagementInfo();
}
