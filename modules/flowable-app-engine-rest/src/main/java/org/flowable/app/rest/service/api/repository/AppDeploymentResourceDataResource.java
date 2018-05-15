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
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
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
@Api(tags = { "App Deployments" }, description = "Manage App Deployments", authorizations = { @Authorization(value = "basicAuth") })
public class AppDeploymentResourceDataResource {

    @Autowired
    protected AppRepositoryService appRepositoryService;

    @ApiOperation(value = "Get an app deployment resource content", tags = {"App Deployments" }, nickname = "getAppDeploymentResource",
            notes = "The response body will contain the binary resource-content for the requested resource. The response content-type will be the same as the type returned in the resources mimeType property. Also, a content-disposition header is set, allowing browsers to download the file instead of displaying it.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates both app deployment and resource have been found and the resource data has been returned."),
            @ApiResponse(code = 404, message = "Indicates the requested app deployment was not found or there is no resource with the given id present in the app deployment. The status-description contains additional information.") })
    @GetMapping(value = "/app-repository/deployments/{deploymentId}/resourcedata/{resourceName}")
    @ResponseBody
    public byte[] getAppDeploymentResource(@ApiParam(name = "deploymentId") @PathVariable("deploymentId") String deploymentId,
            @ApiParam(name = "resourceName") @PathVariable("resourceName") String resourceName,
            HttpServletResponse response) {
        
        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("No deployment id provided");
        }
        if (resourceName == null) {
            throw new FlowableIllegalArgumentException("No resource name provided");
        }

        // Check if deployment exists
        AppDeployment deployment = appRepositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();
        if (deployment == null) {
            throw new FlowableObjectNotFoundException("Could not find an app deployment with id '" + deploymentId);
        }

        List<String> resourceList = appRepositoryService.getDeploymentResourceNames(deploymentId);

        if (resourceList.contains(resourceName)) {
            final InputStream resourceStream = appRepositoryService.getResourceAsStream(deploymentId, resourceName);

            response.setContentType("application/json");
            try {
                return IOUtils.toByteArray(resourceStream);
            } catch (Exception e) {
                throw new FlowableException("Error converting resource stream", e);
            }
        } else {
            // Resource not found in deployment
            throw new FlowableObjectNotFoundException("Could not find a resource with name '" + resourceName + "' in deployment '" + deploymentId);
        }
    }
}
