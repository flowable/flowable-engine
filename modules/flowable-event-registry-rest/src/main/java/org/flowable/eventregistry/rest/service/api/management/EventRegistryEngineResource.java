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

package org.flowable.eventregistry.rest.service.api.management;

import org.flowable.common.rest.api.EngineInfoResponse;
import org.flowable.eventregistry.impl.EventRegistryEngine;
import org.flowable.eventregistry.impl.EventRegistryEngines;
import org.flowable.eventregistry.rest.service.api.EventRegistryRestApiInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
@Api(tags = { "Event Registry Engine" }, authorizations = { @Authorization(value = "basicAuth") })
public class EventRegistryEngineResource {
    
    @Autowired(required=false)
    protected EventRegistryRestApiInterceptor restApiInterceptor;

    @ApiOperation(value = "Get engine info", tags = { "Event Registry Engine" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the engine info is returned."),
    })
    @GetMapping(value = "/event-registry-management/engine", produces = "application/json")
    public EngineInfoResponse getEngineInfo() {
        if (restApiInterceptor != null) {
            restApiInterceptor.accessManagementInfo();
        }
        
        EventRegistryEngine eventRegistryEngine = EventRegistryEngines.getDefaultEventRegistryEngine();
        EngineInfoResponse response = new EngineInfoResponse();
        response.setName(eventRegistryEngine.getName());
        response.setVersion(EventRegistryEngine.class.getPackage().getImplementationVersion());
        return response;
    }
}