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

import javax.servlet.http.HttpServletResponse;

import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.rest.AppRestResponseFactory;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@Api(tags = { "App Deployments" }, description = "Manage App Deployments", authorizations = { @Authorization(value = "basicAuth") })
public class AppDeploymentResource {

    @Autowired
    protected AppRestResponseFactory appRestResponseFactory;

    @Autowired
    protected AppRepositoryService appRepositoryService;

    @ApiOperation(value = "Get an app deployment", tags = { "App Deployments" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the app deployment was found and returned."),
            @ApiResponse(code = 404, message = "Indicates the requested app deployment was not found.")
    })
    @GetMapping(value = "/app-repository/deployments/{deploymentId}", produces = "application/json")
    public AppDeploymentResponse getAppDeployment(@ApiParam(name = "deploymentId") @PathVariable String deploymentId) {
        AppDeployment deployment = appRepositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();

        if (deployment == null) {
            throw new FlowableObjectNotFoundException("Could not find an app deployment with id '" + deploymentId);
        }

        return appRestResponseFactory.createAppDeploymentResponse(deployment);
    }

    @ApiOperation(value = "Delete an app deployment", tags = { "App Deployments" })
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the App deployment was found and has been deleted. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested app deployment was not found.")
    })
    @DeleteMapping(value = "/app-repository/deployments/{deploymentId}", produces = "application/json")
    public void deleteAppDeployment(@ApiParam(name = "deploymentId") @PathVariable String deploymentId, HttpServletResponse response) {
        appRepositoryService.deleteDeployment(deploymentId, true);
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }
}
