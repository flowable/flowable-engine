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

package org.flowable.eventregistry.rest.service.api.runtime;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.api.EventDefinitionQuery;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.flowable.eventregistry.rest.service.api.EventRegistryRestApiInterceptor;
import org.flowable.eventregistry.rest.service.api.EventRegistryRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Event Instances" }, authorizations = { @Authorization(value = "basicAuth") })
public class EventInstanceCollectionResource {
    
    @Autowired
    protected EventRegistryRestResponseFactory restResponseFactory;

    @Autowired
    protected EventRegistry eventRegistry;

    @Autowired
    protected EventRepositoryService repositoryService;
    
    @Autowired
    protected EventRegistryEngineConfiguration eventRegistryEngineConfiguration;

    @Autowired(required=false)
    protected EventRegistryRestApiInterceptor restApiInterceptor;

    @ApiOperation(value = "Send an event instance", tags = { "Event Instances" },
            notes = "Only one of *eventDefinitionId* or *eventDefinitionKey* an be used in the request body. \n\n",
            code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the event instance was created."),
            @ApiResponse(code = 400, message = "Indicates either the event definition was not found (based on id or key), no event was send. Status description contains additional information about the error.")
    })
    @PostMapping(value = "/event-registry-runtime/event-instances")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void createEventInstance(@RequestBody EventInstanceCreateRequest request) {

        if (restApiInterceptor != null) {
            restApiInterceptor.createEventInstance(request);
        }
        validateRequestParameters(request);

        EventDefinition eventDefinition = null;
        if (request.getEventDefinitionId() != null) {
            eventDefinition = repositoryService.getEventDefinition(request.getEventDefinitionId());
            
        } else {
            EventDefinitionQuery eventDefinitionQuery = repositoryService.createEventDefinitionQuery().eventDefinitionKey(request.getEventDefinitionKey());
            if (StringUtils.isNotEmpty(request.getTenantId())) {
                eventDefinitionQuery.tenantId(request.getTenantId());
            }
            
            eventDefinition = eventDefinitionQuery.latestVersion().singleResult();
            
            if (eventDefinition == null && eventRegistryEngineConfiguration.isFallbackToDefaultTenant()) {
                String defaultTenant = eventRegistryEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(request.getTenantId(), ScopeTypes.EVENT_REGISTRY, request.getEventDefinitionKey());
                if (StringUtils.isNotEmpty(defaultTenant)) {
                    eventDefinitionQuery = repositoryService.createEventDefinitionQuery().eventDefinitionKey(request.getEventDefinitionKey()).tenantId(defaultTenant);
                } else {
                    eventDefinitionQuery = repositoryService.createEventDefinitionQuery().eventDefinitionKey(request.getEventDefinitionKey());
                }
                
                eventDefinition = eventDefinitionQuery.latestVersion().singleResult();
            }
        }
        
        if (eventDefinition == null) {
            throw new FlowableObjectNotFoundException("No event definition found");
        }

        ChannelModel channelModel = null;
        if (StringUtils.isNotEmpty(request.getChannelDefinitionId())) {
            channelModel = repositoryService.getChannelModelById(request.getChannelDefinitionId());
        } else if (StringUtils.isNotEmpty(request.getTenantId())) {
            channelModel = repositoryService.getChannelModelByKey(request.getChannelDefinitionKey(), request.getTenantId());
        } else {
            channelModel = repositoryService.getChannelModelByKey(request.getChannelDefinitionKey());
        }

        eventRegistry.eventReceived((InboundChannelModel) channelModel, request.getEventPayload().toString());
    }

    protected void validateRequestParameters(@RequestBody EventInstanceCreateRequest request) {
        if (request.getEventDefinitionId() == null && request.getEventDefinitionKey() == null) {
            throw new FlowableIllegalArgumentException("Either eventDefinitionId or eventDefinitionKey is required.");
        }

        int paramsSet = ((request.getEventDefinitionId() != null) ? 1 : 0) + ((request.getEventDefinitionKey() != null) ? 1 : 0);
        if (paramsSet > 1) {
            throw new FlowableIllegalArgumentException("Only one of eventDefinitionId or eventDefinitionKey should be set.");
        }

        if (request.getChannelDefinitionId() == null && request.getChannelDefinitionKey() == null) {
            throw new FlowableIllegalArgumentException("Either channelDefinitionId or channelDefinitionKey is required.");
        }

        paramsSet = ((request.getChannelDefinitionId() != null) ? 1 : 0) + ((request.getChannelDefinitionKey() != null) ? 1 : 0);
        if (paramsSet > 1) {
            throw new FlowableIllegalArgumentException("Only one of eventDefinitionId or eventDefinitionKey should be set.");
        }
    }
}
