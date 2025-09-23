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

import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.rest.service.api.EventRegistryRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
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
@Api(tags = { "Event Deployment" }, authorizations = { @Authorization(value = "basicAuth") })
public class DeploymentResource extends BaseDeploymentResource {

    @Autowired
    protected EventRegistryRestResponseFactory restResponseFactory;

    @ApiOperation(value = "Get a deployment", tags = { "Deployment" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the deployment was found and returned."),
            @ApiResponse(code = 404, message = "Indicates the requested deployment was not found.")
    })
    @GetMapping(value = "/event-registry-repository/deployments/{deploymentId}", produces = "application/json")
    public EventDeploymentResponse getDeployment(@ApiParam(name = "deploymentId", value ="The id of the deployment to get.") @PathVariable String deploymentId) {
        EventDeployment deployment = getEventDeployment(deploymentId);
        return restResponseFactory.createDeploymentResponse(deployment);
    }

    @ApiOperation(value = "Delete a deployment", tags = { "Deployment" }, code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the deployment was found and has been deleted. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested deployment was not found.")
    })
    @DeleteMapping(value = "/event-registry-repository/deployments/{deploymentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeployment(@ApiParam(name = "deploymentId") @PathVariable String deploymentId) {
        
        EventDeployment deployment = getEventDeployment(deploymentId);
        if (restApiInterceptor != null) {
            restApiInterceptor.deleteDeployment(deployment);
        }

        repositoryService.deleteDeployment(deploymentId);
    }
}
