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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.flowable.common.rest.resolver.ContentTypeResolver;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.rest.service.api.EventRegistryRestResponseFactory;
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
@Api(tags = { "Event Registry Deployment" }, description = "Manage Event Registry Deployment", authorizations = { @Authorization(value = "basicAuth") })
public class DeploymentResourceCollectionResource extends BaseDeploymentResource {

    @Autowired
    protected EventRegistryRestResponseFactory restResponseFactory;

    @Autowired
    protected ContentTypeResolver contentTypeResolver;

    @Autowired
    protected EventRepositoryService repositoryService;

    @ApiOperation(value = "List resources in a deployment", tags = { "Deployment" }, nickname="listDeploymentResources",
            notes = "The dataUrl property in the resulting JSON for a single resource contains the actual URL to use for retrieving the binary resource.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the deployment was found and the resource list has been returned."),
            @ApiResponse(code = 404, message = "Indicates the requested deployment was not found.")
    })
    @GetMapping(value = "/event-registry-repository/deployments/{deploymentId}/resources", produces = "application/json")
    public List<DeploymentResourceResponse> getDeploymentResources(@ApiParam(name = "deploymentId") @PathVariable String deploymentId, HttpServletRequest request) {
        // Check if deployment exists
        EventDeployment deployment = getEventDeployment(deploymentId);

        List<String> resourceList = repositoryService.getDeploymentResourceNames(deployment.getId());

        return restResponseFactory.createDeploymentResourceResponseList(deploymentId, resourceList, contentTypeResolver);
    }
}
