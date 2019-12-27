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
package org.flowable.eventregistry.impl;

import java.io.InputStream;
import java.util.List;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;
import org.flowable.eventregistry.api.ChannelDefinitionQuery;
import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.api.EventDefinitionQuery;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventDeploymentBuilder;
import org.flowable.eventregistry.api.EventDeploymentQuery;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.model.EventModelBuilder;
import org.flowable.eventregistry.impl.cmd.DeleteDeploymentCmd;
import org.flowable.eventregistry.impl.cmd.DeployCmd;
import org.flowable.eventregistry.impl.cmd.GetChannelModelCmd;
import org.flowable.eventregistry.impl.cmd.GetDeploymentResourceCmd;
import org.flowable.eventregistry.impl.cmd.GetDeploymentResourceNamesCmd;
import org.flowable.eventregistry.impl.cmd.GetEventDefinitionCmd;
import org.flowable.eventregistry.impl.cmd.GetEventDefinitionResourceCmd;
import org.flowable.eventregistry.impl.cmd.GetEventModelCmd;
import org.flowable.eventregistry.impl.cmd.SetDeploymentCategoryCmd;
import org.flowable.eventregistry.impl.cmd.SetDeploymentParentDeploymentIdCmd;
import org.flowable.eventregistry.impl.cmd.SetDeploymentTenantIdCmd;
import org.flowable.eventregistry.impl.cmd.SetEventDefinitionCategoryCmd;
import org.flowable.eventregistry.impl.model.EventModelBuilderImpl;
import org.flowable.eventregistry.impl.repository.EventDeploymentBuilderImpl;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.EventModel;

/**
 * @author Tijs Rademakers
 */
public class EventRepositoryServiceImpl extends CommonEngineServiceImpl<EventRegistryEngineConfiguration> implements EventRepositoryService {

    public EventRepositoryServiceImpl(EventRegistryEngineConfiguration engineConfiguration) {
        super(engineConfiguration);
    }

    @Override
    public EventDeploymentBuilder createDeployment() {
        return commandExecutor.execute(new Command<EventDeploymentBuilder>() {
            @Override
            public EventDeploymentBuilder execute(CommandContext commandContext) {
                return new EventDeploymentBuilderImpl();
            }
        });
    }

    public EventDeployment deploy(EventDeploymentBuilderImpl deploymentBuilder) {
        return commandExecutor.execute(new DeployCmd<EventDeployment>(deploymentBuilder));
    }

    @Override
    public void deleteDeployment(String deploymentId) {
        commandExecutor.execute(new DeleteDeploymentCmd(deploymentId));
    }

    @Override
    public EventDefinitionQuery createEventDefinitionQuery() {
        return new EventDefinitionQueryImpl(commandExecutor);
    }
    
    @Override
    public ChannelDefinitionQuery createChannelDefinitionQuery() {
        return new ChannelDefinitionQueryImpl(commandExecutor);
    }

    @Override
    public List<String> getDeploymentResourceNames(String deploymentId) {
        return commandExecutor.execute(new GetDeploymentResourceNamesCmd(deploymentId));
    }

    @Override
    public InputStream getResourceAsStream(String deploymentId, String resourceName) {
        return commandExecutor.execute(new GetDeploymentResourceCmd(deploymentId, resourceName));
    }

    @Override
    public void setDeploymentCategory(String deploymentId, String category) {
        commandExecutor.execute(new SetDeploymentCategoryCmd(deploymentId, category));
    }

    @Override
    public void setDeploymentTenantId(String deploymentId, String newTenantId) {
        commandExecutor.execute(new SetDeploymentTenantIdCmd(deploymentId, newTenantId));
    }
    
    @Override
    public void changeDeploymentParentDeploymentId(String deploymentId, String newParentDeploymentId) {
        commandExecutor.execute(new SetDeploymentParentDeploymentIdCmd(deploymentId, newParentDeploymentId));
    }

    @Override
    public EventDeploymentQuery createDeploymentQuery() {
        return new EventDeploymentQueryImpl(commandExecutor);
    }

    @Override
    public EventDefinition getEventDefinition(String eventDefinitionId) {
        return commandExecutor.execute(new GetEventDefinitionCmd(eventDefinitionId));
    }

    @Override
    public InputStream getEventDefinitionResource(String formId) {
        return commandExecutor.execute(new GetEventDefinitionResourceCmd(formId));
    }

    @Override
    public void setEventDefinitionCategory(String eventDefinitionId, String category) {
        commandExecutor.execute(new SetEventDefinitionCategoryCmd(eventDefinitionId, category));
    }
    
    @Override
    public EventModel getEventModelById(String eventDefinitionId) {
        return commandExecutor.execute(new GetEventModelCmd(null, eventDefinitionId));
    }

    @Override
    public EventModel getEventModelByKey(String eventDefinitionKey) {
        return commandExecutor.execute(new GetEventModelCmd(eventDefinitionKey, null));
    }
    
    @Override
    public EventModel getEventModelByKey(String eventDefinitionKey, String tenantId, boolean fallbackToDefaultTenant) {
        return commandExecutor.execute(new GetEventModelCmd(eventDefinitionKey, null, tenantId, fallbackToDefaultTenant));
    }

    @Override
    public EventModel getEventModelByKeyAndParentDeploymentId(String eventDefinitionKey, String parentDeploymentId) {
        return commandExecutor.execute(new GetEventModelCmd(eventDefinitionKey, null, null, parentDeploymentId, false));
    }

    @Override
    public EventModel getEventModelByKeyAndParentDeploymentId(String eventDefinitionKey, String parentDeploymentId, String tenantId, boolean fallbackToDefaultTenant) {
        return commandExecutor.execute(new GetEventModelCmd(eventDefinitionKey, null, tenantId, parentDeploymentId, fallbackToDefaultTenant));
    }
    
    @Override
    public ChannelModel getChannelModelById(String channelDefinitionId) {
        return commandExecutor.execute(new GetChannelModelCmd(null, channelDefinitionId));
    }

    @Override
    public ChannelModel getChannelModelByKey(String channelDefinitionKey) {
        return commandExecutor.execute(new GetChannelModelCmd(channelDefinitionKey, null));
    }

    @Override
    public ChannelModel getChannelModelByKey(String channelDefinitionKey, String tenantId, boolean fallbackToDefaultTenant) {
        return commandExecutor.execute(new GetChannelModelCmd(channelDefinitionKey, null, tenantId, fallbackToDefaultTenant));
    }

    @Override
    public ChannelModel getChannelModelByKeyAndParentDeploymentId(String channelDefinitionKey, String parentDeploymentId) {
        return commandExecutor.execute(new GetChannelModelCmd(channelDefinitionKey, null, null, parentDeploymentId, false));
    }

    @Override
    public ChannelModel getChannelModelByKeyAndParentDeploymentId(String channelDefinitionKey, String parentDeploymentId, String tenantId, boolean fallbackToDefaultTenant) {
        return commandExecutor.execute(new GetChannelModelCmd(channelDefinitionKey, null, tenantId, parentDeploymentId, fallbackToDefaultTenant));
    }

    @Override
    public EventModelBuilder createEventModelBuilder() {
        return new EventModelBuilderImpl(this);
    }
    
    public void registerEventModel(EventModel eventModel) {
        
    }
}
