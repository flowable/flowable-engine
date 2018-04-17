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
package org.flowable.form.rest.service.api.repository;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.rest.FormRestResponseFactory;
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
@Api(tags = { "Form Deployments" }, description = "Manage Form Deployments", authorizations = { @Authorization(value = "basicAuth") })
public class FormDeploymentResource {

    @Autowired
    protected FormRestResponseFactory formRestResponseFactory;

    @Autowired
    protected FormRepositoryService formRepositoryService;

    @ApiOperation(value = "Get a form deployment", tags = { "Form Deployments" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the form deployment was found and returned."),
            @ApiResponse(code = 404, message = "Indicates the requested form deployment was not found.")
    })
    @GetMapping(value = "/form-repository/deployments/{deploymentId}", produces = "application/json")
    public FormDeploymentResponse getFormDeployment(@ApiParam(name = "deploymentId") @PathVariable String deploymentId) {
        FormDeployment deployment = formRepositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();

        if (deployment == null) {
            throw new FlowableObjectNotFoundException("Could not find a form deployment with id '" + deploymentId);
        }

        return formRestResponseFactory.createFormDeploymentResponse(deployment);
    }

    @ApiOperation(value = "Delete a form deployment", tags = { "Form Deployments" })
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the form deployment was found and has been deleted. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested form deployment was not found.")
    })
    @DeleteMapping(value = "/form-repository/deployments/{deploymentId}", produces = "application/json")
    public void deleteFormDeployment(@ApiParam(name = "deploymentId") @PathVariable String deploymentId, HttpServletResponse response) {
        formRepositoryService.deleteDeployment(deploymentId);
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }
}
