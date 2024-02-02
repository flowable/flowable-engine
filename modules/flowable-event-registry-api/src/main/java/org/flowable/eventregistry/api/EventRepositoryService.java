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
package org.flowable.eventregistry.api;

import java.io.InputStream;
import java.util.List;

import org.flowable.eventregistry.api.model.EventModelBuilder;
import org.flowable.eventregistry.api.model.InboundChannelModelBuilder;
import org.flowable.eventregistry.api.model.OutboundChannelModelBuilder;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.EventModel;

/**
 * Service providing access to the repository of forms.
 *
 * @author Tijs Rademakers
 */
public interface EventRepositoryService {

    EventDeploymentBuilder createDeployment();

    void deleteDeployment(String deploymentId);

    EventDefinitionQuery createEventDefinitionQuery();
    
    ChannelDefinitionQuery createChannelDefinitionQuery();

    /**
     * Changes the category of a deployment.
     * 
     * @param deploymentId
     *              The id of the deployment of which the category will be changed.
     * @param category
     *              The new category.
     */
    void setDeploymentCategory(String deploymentId, String category);

    /**
     * Changes the tenant id of a deployment.
     * 
     * @param deploymentId
     *              The id of the deployment of which the tenant identifier will be changed.
     * @param newTenantId
     *              The new tenant identifier.
     */
    void setDeploymentTenantId(String deploymentId, String newTenantId);
    
    /**
     * Changes the parent deployment id of a deployment. This is used to move deployments to a different app deployment parent.
     * 
     * @param deploymentId
     *              The id of the deployment of which the parent deployment identifier will be changed.
     * @param newParentDeploymentId
     *              The new parent deployment identifier.
     */
    void changeDeploymentParentDeploymentId(String deploymentId, String newParentDeploymentId);

    List<String> getDeploymentResourceNames(String deploymentId);

    InputStream getResourceAsStream(String deploymentId, String resourceName);

    EventDeploymentQuery createDeploymentQuery();

    EventDefinition getEventDefinition(String eventDefinitionId);

    InputStream getEventDefinitionResource(String eventDefinitionId);

    void setEventDefinitionCategory(String eventDefinitionId, String category);
    
    ChannelDefinition getChannelDefinition(String channelDefinitionId);

    InputStream getChannelDefinitionResource(String channelDefinitionId);

    void setChannelDefinitionCategory(String channelDefinitionId, String category);
    
    EventModel getEventModelById(String eventDefinitionId);

    EventModel getEventModelByKey(String eventDefinitionKey);
    
    EventModel getEventModelByKey(String eventDefinitionKey, String tenantId);

    EventModel getEventModelByKeyAndParentDeploymentId(String eventDefinitionKey, String parentDeploymentId);

    EventModel getEventModelByKeyAndParentDeploymentId(String eventDefinitionKey, String parentDeploymentId, String tenantId);
    
    ChannelModel getChannelModelById(String channelDefinitionId);

    ChannelModel getChannelModelByKey(String channelDefinitionKey);
    
    ChannelModel getChannelModelByKey(String channelDefinitionKey, String tenantId);

    ChannelModel getChannelModelByKeyAndParentDeploymentId(String channelDefinitionKey, String parentDeploymentId);

    ChannelModel getChannelModelByKeyAndParentDeploymentId(String channelDefinitionKey, String parentDeploymentId, String tenantId);
    
    /**
     * Programmatically build and register a new {@link EventModel}.
     */
    EventModelBuilder createEventModelBuilder();
    
    InboundChannelModelBuilder createInboundChannelModelBuilder();
    
    OutboundChannelModelBuilder createOutboundChannelModelBuilder();
}
