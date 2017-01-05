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
package org.flowable.rest.dmn.service.api.repository;

import javax.servlet.http.HttpServletResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.rest.dmn.service.api.DmnRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Yvo Swillens
 */
@RestController
@Api(tags = { "Deployment" }, description = "Manage Decision Table Deployments")
public class DmnDeploymentResource {

  @Autowired
  protected DmnRestResponseFactory dmnRestResponseFactory;

  @Autowired
  protected DmnRepositoryService dmnRepositoryService;

  @ApiOperation(value = "Get a decision table deployment", tags = {"Deployment"})
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Indicates the deployment was found and returned."),
      @ApiResponse(code = 404, message = "Indicates the requested deployment was not found.")
  })
  @RequestMapping(value = "/dmn-repository/deployments/{deploymentId}", method = RequestMethod.GET, produces = "application/json")
  public DmnDeploymentResponse getDmnDeployment(@ApiParam(name = "deploymentId") @PathVariable String deploymentId) {
    DmnDeployment deployment = dmnRepositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();

    if (deployment == null) {
      throw new FlowableObjectNotFoundException("Could not find a DMN deployment with id '" + deploymentId);
    }

    return dmnRestResponseFactory.createDmnDeploymentResponse(deployment);
  }

  @ApiOperation(value = "Delete a decision table deployment", tags = {"Deployment"})
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Indicates the deployment was found and has been deleted. Response-body is intentionally empty."),
      @ApiResponse(code = 404, message = "Indicates the requested deployment was not found.")
  })
  @RequestMapping(value = "/dmn-repository/deployments/{deploymentId}", method = RequestMethod.DELETE, produces = "application/json")
  public void deleteDmnDeployment(@ApiParam(name = "deploymentId") @PathVariable String deploymentId, HttpServletResponse response) {

    dmnRepositoryService.deleteDeployment(deploymentId);

    response.setStatus(HttpStatus.NO_CONTENT.value());
  }
}