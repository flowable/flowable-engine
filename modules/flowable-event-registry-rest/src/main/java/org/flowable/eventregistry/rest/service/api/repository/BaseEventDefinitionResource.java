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

package org.flowable.eventregistry.rest.service.api.repository;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.rest.service.api.EventRegistryRestApiInterceptor;
import org.flowable.eventregistry.rest.service.api.EventRegistryRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Tijs Rademakers
 */
public class BaseEventDefinitionResource {

    @Autowired
    protected EventRegistryRestResponseFactory restResponseFactory;

    @Autowired
    protected EventRepositoryService repositoryService;
    
    @Autowired(required=false)
    protected EventRegistryRestApiInterceptor restApiInterceptor;

    /**
     * Returns the {@link EventDefinition} that is requested. Throws the right exceptions when bad request was made or definition was not found.
     */
    protected EventDefinition getEventDefinitionFromRequest(String eventDefinitionId) {
        EventDefinition eventDefinition = repositoryService.getEventDefinition(eventDefinitionId);

        if (eventDefinition == null) {
            throw new FlowableObjectNotFoundException("Could not find an event definition with id '" + eventDefinitionId + "'.", EventDefinition.class);
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessEventDefinitionById(eventDefinition);
        }
        
        return eventDefinition;
    }
    
    /**
     * Returns the {@link ChannelDefinition} that is requested. Throws the right exceptions when bad request was made or definition was not found.
     */
    protected ChannelDefinition getChannelDefinitionFromRequest(String channelDefinitionId) {
        ChannelDefinition channelDefinition = repositoryService.getChannelDefinition(channelDefinitionId);

        if (channelDefinition == null) {
            throw new FlowableObjectNotFoundException("Could not find a channel definition with id '" + channelDefinitionId + "'.", ChannelDefinition.class);
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessChannelDefinitionById(channelDefinition);
        }
        
        return channelDefinition;
    }
}
