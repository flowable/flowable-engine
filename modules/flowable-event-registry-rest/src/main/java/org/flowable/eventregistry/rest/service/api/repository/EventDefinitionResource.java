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

import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Event Definitions" }, authorizations = { @Authorization(value = "basicAuth") })
public class EventDefinitionResource extends BaseEventDefinitionResource {
    
    @Autowired
    protected EventRegistryEngineConfiguration eventRegistryEngineConfiguration;
    
    @Autowired(required=false)
    protected EventRepositoryService eventRepositoryService;

    @ApiOperation(value = "Get an event definition", tags = { "Event Definitions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the event definitions are returned"),
            @ApiResponse(code = 404, message = "Indicates the requested event definition was not found.")
    })
    @GetMapping(value = "/event-registry-repository/event-definitions/{eventDefinitionId}", produces = "application/json")
    public EventDefinitionResponse getEventDefinition(@ApiParam(name = "eventDefinitionId") @PathVariable String eventDefinitionId) {
        EventDefinition eventDefinition = getEventDefinitionFromRequest(eventDefinitionId);

        return restResponseFactory.createEventDefinitionResponse(eventDefinition);
    }
}
