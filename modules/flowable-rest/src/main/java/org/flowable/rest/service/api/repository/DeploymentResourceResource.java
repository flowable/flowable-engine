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

package org.flowable.rest.service.api.repository;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.engine.repository.Deployment;
import org.flowable.rest.application.ContentTypeResolver;
import org.flowable.rest.service.api.RestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Deployment" }, description = "Manage Deployment", authorizations = { @Authorization(value = "basicAuth") })
public class DeploymentResourceResource {

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected ContentTypeResolver contentTypeResolver;

    @Autowired
    protected RepositoryService repositoryService;

    @ApiOperation(value = "Get a deployment resource", tags = { "Deployment" }, notes = "Replace ** by ResourceId")
    /*
     * @ApiImplicitParams({
     * 
     * @ApiImplicitParam(name = "resourceId", dataType = "string", value =
     * "The id of the resource to get. Make sure you URL-encode the resourceId in case it contains forward slashes. Eg: use diagrams%2Fmy-process.bpmn20.xml instead of diagrams/Fmy-process.bpmn20.xml."
     * , paramType = "path") })
     */
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates both deployment and resource have been found and the resource has been returned."),
            @ApiResponse(code = 404, message = "Indicates the requested deployment was not found or there is no resource with the given id present in the deployment. The status-description contains additional information.")
    })
    // FIXME Why ** ?
    @RequestMapping(value = "/repository/deployments/{deploymentId}/resources/**", method = RequestMethod.GET, produces = "application/json")
    public DeploymentResourceResponse getDeploymentResource(@ApiParam(name = "deploymentId") @PathVariable("deploymentId") String deploymentId, HttpServletRequest request) {

        // Check if deployment exists
        Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();
        if (deployment == null) {
            throw new FlowableObjectNotFoundException("Could not find a deployment with id '" + deploymentId + "'.");
        }

        String pathInfo = request.getPathInfo();
        String resourceName = pathInfo.replace("/repository/deployments/" + deploymentId + "/resources/", "");

        List<String> resourceList = repositoryService.getDeploymentResourceNames(deploymentId);

        if (resourceList.contains(resourceName)) {
            // Build resource representation
            DeploymentResourceResponse response = restResponseFactory.createDeploymentResourceResponse(deploymentId, resourceName, contentTypeResolver.resolveContentType(resourceName));
            return response;

        } else {
            // Resource not found in deployment
            throw new FlowableObjectNotFoundException("Could not find a resource with id '" + resourceName + "' in deployment '" + deploymentId + "'.");
        }
    }
}
