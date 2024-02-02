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
package org.flowable.eventregistry.rest.service.api;

import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.api.ChannelDefinitionQuery;
import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.api.EventDefinitionQuery;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventDeploymentBuilder;
import org.flowable.eventregistry.api.EventDeploymentQuery;
import org.flowable.eventregistry.rest.service.api.runtime.EventInstanceCreateRequest;

public interface EventRegistryRestApiInterceptor {

    void accessEventDefinitionById(EventDefinition eventDefinition);
    
    void accessEventDefinitionsWithQuery(EventDefinitionQuery eventDefinitionQuery);
    
    void accessChannelDefinitionById(ChannelDefinition channelDefinition);
    
    void accessChannelDefinitionsWithQuery(ChannelDefinitionQuery channelDefinitionQuery);
    
    void accessDeploymentById(EventDeployment deployment);
    
    void accessDeploymentsWithQuery(EventDeploymentQuery deploymentQuery);
    
    void executeNewDeploymentForTenantId(String tenantId);

    void enhanceDeployment(EventDeploymentBuilder eventDeploymentBuilder);
    
    void deleteDeployment(EventDeployment deployment);
    
    void createEventInstance(EventInstanceCreateRequest request);
    
    void accessManagementInfo();
    
    void accessTableInfo();
}
