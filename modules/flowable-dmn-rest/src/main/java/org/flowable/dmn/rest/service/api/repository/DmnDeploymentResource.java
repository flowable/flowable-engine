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
package org.flowable.dmn.rest.service.api.repository;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.rest.service.api.DmnRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

/**
 * @author Yvo Swillens
 */
@RestController
@Api(tags = { "Deployment" }, description = "Manage Decision Table Deployments", authorizations = { @Authorization(value = "basicAuth") })
public class DmnDeploymentResource {

    @Autowired
    protected DmnRestResponseFactory dmnRestResponseFactory;

    @Autowired
    protected DmnRepositoryService dmnRepositoryService;

    @ApiOperation(value = "Get a decision table deployment", tags = { "Deployment" }, nickname = "getDecisionTableDeployment")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the deployment was found and returned."),
            @ApiResponse(code = 404, message = "Indicates the requested deployment was not found.")
    })
    @GetMapping(value = "/dmn-repository/deployments/{deploymentId}", produces = "application/json")
    public DmnDeploymentResponse getDmnDeployment(@ApiParam(name = "deploymentId") @PathVariable String deploymentId) {
        DmnDeployment deployment = dmnRepositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();

        if (deployment == null) {
            throw new FlowableObjectNotFoundException("Could not find a DMN deployment with id '" + deploymentId);
        }

        return dmnRestResponseFactory.createDmnDeploymentResponse(deployment);
    }

    @ApiOperation(value = "Delete a decision table deployment", tags = { "Deployment" }, nickname = "deleteDecisionTableDeployment")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the deployment was found and has been deleted. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested deployment was not found.")
    })
    @DeleteMapping(value = "/dmn-repository/deployments/{deploymentId}", produces = "application/json")
    public void deleteDmnDeployment(@ApiParam(name = "deploymentId") @PathVariable String deploymentId, HttpServletResponse response) {

        dmnRepositoryService.deleteDeployment(deploymentId);

        response.setStatus(HttpStatus.NO_CONTENT.value());
    }
}