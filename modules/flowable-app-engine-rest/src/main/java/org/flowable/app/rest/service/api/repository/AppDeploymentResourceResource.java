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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.entity.ContentType;
import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.rest.AppRestResponseFactory;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.rest.resolver.ContentTypeResolver;
import org.springframework.beans.factory.annotation.Autowired;
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
@Api(tags = { "App Deployment" }, description = "Manage App Deployment", authorizations = { @Authorization(value = "basicAuth") })
public class AppDeploymentResourceResource {

    @Autowired
    protected AppRestResponseFactory restResponseFactory;

    @Autowired
    protected ContentTypeResolver contentTypeResolver;

    @Autowired
    protected AppRepositoryService repositoryService;

    @ApiOperation(value = "Get a deployment resource", tags = { "Deployment" }, notes = "Replace ** by ResourceId")
    /*
     * @ApiImplicitParams({
     * 
     * @ApiImplicitParam(name = "resourceId", dataType = "string", value =
     * "The id of the resource to get. Make sure you URL-encode the resourceId in case it contains forward slashes. Eg: use folder%2FoneApp.app instead of folder/oneApp.app."
     * , paramType = "path") })
     */
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates both deployment and resource have been found and the resource has been returned."),
            @ApiResponse(code = 404, message = "Indicates the requested deployment was not found or there is no resource with the given id present in the deployment. The status-description contains additional information.")
    })
    @GetMapping(value = "/app-repository/deployments/{deploymentId}/resources/**", produces = "application/json")
    public AppDeploymentResourceResponse getDeploymentResource(@ApiParam(name = "deploymentId") @PathVariable("deploymentId") String deploymentId, HttpServletRequest request) {
        // The ** is needed because the name of the resource can actually contain forward slashes.
        // For example org/flowable/oneApp.app. The number of forward slashes is unknown.
        // Using ** means that everything should get matched.
        // See also https://stackoverflow.com/questions/31421061/how-to-handle-requests-that-includes-forward-slashes/42403361#42403361

        // Check if deployment exists
        AppDeployment deployment = repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();
        if (deployment == null) {
            throw new FlowableObjectNotFoundException("Could not find a deployment with id '" + deploymentId + "'.");
        }

        String pathInfo = request.getPathInfo();
        String resourceName = pathInfo.replace("/app-repository/deployments/" + deploymentId + "/resources/", "");

        List<String> resourceList = repositoryService.getDeploymentResourceNames(deploymentId);

        if (resourceList.contains(resourceName)) {
            // Build resource representation
            String contentType = null;
            if (resourceName.toLowerCase().endsWith(".app")) {
                contentType = ContentType.APPLICATION_JSON.getMimeType();
            } else {
                contentType = contentTypeResolver.resolveContentType(resourceName);
            }
            
            AppDeploymentResourceResponse response = restResponseFactory.createDeploymentResourceResponse(deploymentId, resourceName, contentType);
            return response;

        } else {
            // Resource not found in deployment
            throw new FlowableObjectNotFoundException("Could not find a resource with id '" + resourceName + "' in deployment '" + deploymentId + "'.");
        }
    }
}
