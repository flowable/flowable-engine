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

import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
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
public class AppDefinitionResourceDataResource {

    @Autowired
    protected AppRepositoryService appRepositoryService;

    @ApiOperation(value = "Get an app definition resource content", nickname = "getAppDefinitionContent", tags = { "App Definitions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates both app definition and resource have been found and the resource data has been returned."),
            @ApiResponse(code = 404, message = "Indicates the requested app definition was not found or there is no resource with the given id present in the app definition. The status-description contains additional information.")
    })
    @GetMapping(value = "/app-repository/app-definitions/{appDefinitionId}/resourcedata", produces = "application/json")
    @ResponseBody
    public byte[] getAppDefinitionResource(@ApiParam(name = "appDefinitionId") @PathVariable String appDefinitionId, HttpServletResponse response) {
        AppDefinition appDefinition = appRepositoryService.getAppDefinition(appDefinitionId);

        if (appDefinition == null) {
            throw new FlowableObjectNotFoundException("Could not find an app definition with id '" + appDefinitionId);
        }
        if (appDefinition.getDeploymentId() == null) {
            throw new FlowableException("No deployment id available");
        }
        if (appDefinition.getResourceName() == null) {
            throw new FlowableException("No resource name available");
        }

        // Check if deployment exists
        AppDeployment deployment = appRepositoryService.createDeploymentQuery().deploymentId(appDefinition.getDeploymentId()).singleResult();
        if (deployment == null) {
            throw new FlowableObjectNotFoundException("Could not find a deployment with id '" + appDefinition.getDeploymentId());
        }

        List<String> resourceList = appRepositoryService.getDeploymentResourceNames(appDefinition.getDeploymentId());

        if (resourceList.contains(appDefinition.getResourceName())) {
            final InputStream resourceStream = appRepositoryService.getResourceAsStream(
                    appDefinition.getDeploymentId(), appDefinition.getResourceName());

            response.setContentType("application/json");
            try {
                return IOUtils.toByteArray(resourceStream);
            } catch (Exception e) {
                throw new FlowableException("Error converting resource stream", e);
            }
        } else {
            // Resource not found in deployment
            throw new FlowableObjectNotFoundException("Could not find a resource with id '" +
                    appDefinition.getResourceName() + "' in deployment '" + appDefinition.getDeploymentId());
        }
    }
}
