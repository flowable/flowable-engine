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

import javax.servlet.http.HttpServletResponse;

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
 * @author Yvo Swillens
 */
@RestController
@Api(tags = { "Deployment" }, description = "Manage Decision Deployments", authorizations = { @Authorization(value = "basicAuth") })
public class DmnDeploymentResourceDataResource extends BaseDmnDeploymentResourceDataResource {

    @ApiOperation(value = "Get a decision deployment resource content", tags = {"Deployment" }, nickname = "getDecisionTableDeploymentResource",
            notes = "The response body will contain the binary resource-content for the requested resource. The response content-type will be the same as the type returned in the resources mimeType property. Also, a content-disposition header is set, allowing browsers to download the file instead of displaying it.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates both deployment and resource have been found and the resource data has been returned."),
            @ApiResponse(code = 404, message = "Indicates the requested deployment was not found or there is no resource with the given id present in the deployment. The status-description contains additional information.") })
    @GetMapping(value = "/dmn-repository/deployments/{deploymentId}/resourcedata/{resourceName}")
    @ResponseBody
    public byte[] getDmnDeploymentResource(@ApiParam(name = "deploymentId") @PathVariable("deploymentId") String deploymentId, @PathVariable("resourceName") String resourceName, HttpServletResponse response) {
        return getDmnDeploymentResourceData(deploymentId, resourceName, response);
    }
}
