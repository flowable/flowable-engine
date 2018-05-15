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
package org.flowable.app.rest.service.api.repository;

import javax.servlet.http.HttpServletRequest;

import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.rest.AppRestResponseFactory;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
@Api(tags = { "App Definitions" }, description = "Manage App Definitions", authorizations = { @Authorization(value = "basicAuth") })
public class AppDefinitionResource {

    @Autowired
    protected AppRestResponseFactory appRestResponseFactory;

    @Autowired
    protected AppRepositoryService appRepositoryService;

    @ApiOperation(value = "Get a app definition", tags = { "App Definitions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the app definition was found returned."),
            @ApiResponse(code = 404, message = "Indicates the app definition was not found.")
    })
    @GetMapping(value = "/app-repository/app-definitions/{appDefinitionId}", produces = "application/json")
    public AppDefinitionResponse getAppDefinition(@ApiParam(name = "appDefinitionId") @PathVariable String appDefinitionId, HttpServletRequest request) {
        AppDefinition appDefinition = appRepositoryService.getAppDefinition(appDefinitionId);

        if (appDefinition == null) {
            throw new FlowableObjectNotFoundException("Could not find an app definition with id '" + appDefinitionId);
        }

        return appRestResponseFactory.createAppDefinitionResponse(appDefinition);
    }
    
    @ApiOperation(value = "Execute actions for an app definition", tags = { "Case Definitions" },
            notes = "Execute actions for an app definition (Update category)")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates action has been executed for the specified app defintion. (category altered)"),
            @ApiResponse(code = 400, message = "Indicates no category was defined in the request body."),
            @ApiResponse(code = 404, message = "Indicates the requested app definition was not found.")
    })
    @PutMapping(value = "/app-repository/app-definitions/{appDefinitionId}", produces = "application/json")
    public AppDefinitionResponse executeAppDefinitionAction(
            @ApiParam(name = "appDefinitionId") @PathVariable String appDefinitionId,
            @ApiParam(required = true) @RequestBody AppDefinitionActionRequest actionRequest,
            HttpServletRequest request) {

        if (actionRequest == null) {
            throw new FlowableIllegalArgumentException("No action found in request body.");
        }

        AppDefinition appDefinition = appRepositoryService.getAppDefinition(appDefinitionId);

        if (appDefinition == null) {
            throw new FlowableObjectNotFoundException("Could not find an app definition with id '" + appDefinitionId + "'.", AppDefinition.class);
        }

        if (actionRequest.getCategory() != null) {
            // Update of category required
            appRepositoryService.setAppDefinitionCategory(appDefinition.getId(), actionRequest.getCategory());

            // No need to re-fetch the AppDefinition entity, just update category in response
            AppDefinitionResponse response = appRestResponseFactory.createAppDefinitionResponse(appDefinition);
            response.setCategory(actionRequest.getCategory());
            return response;
        }
        
        throw new FlowableIllegalArgumentException("Invalid action: '" + actionRequest.getAction() + "'.");
    }
}
