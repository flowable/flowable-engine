/** Licensed under the Apache License, Version 2.0 (the "License");
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
package org.flowable.rest.form.service.api.repository;

import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.io.IOUtils;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.rest.application.ContentTypeResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Yvo Swillens
 */
@RestController
@Api(tags = { "Form Deployments" }, description = "Manage Form Deployments")
public class FormDeploymentResourceDataResource {

  @Autowired
  protected ContentTypeResolver contentTypeResolver;

  @Autowired
  protected FormRepositoryService formRepositoryService;

  @ApiOperation(value = "Get a form deployment resource content", tags = {"Form Deployments"}, nickname = "getFormDeploymentResource",
      notes = "The response body will contain the binary resource-content for the requested resource. The response content-type will be the same as the type returned in the resources mimeType property. Also, a content-disposition header is set, allowing browsers to download the file instead of displaying it.")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Indicates both form deployment and resource have been found and the resource data has been returned."),
      @ApiResponse(code = 404, message = "Indicates the requested form deployment was not found or there is no resource with the given id present in the form deployment. The status-description contains additional information.")})
  @RequestMapping(value = "/form-repository/deployments/{deploymentId}/resourcedata/{resourceId}", method = RequestMethod.GET)
  @ResponseBody
  public byte[] getFormDeploymentResource(@ApiParam(name = "deploymentId") @PathVariable("deploymentId") String deploymentId,
                                          @ApiParam(name = "resourceId") @PathVariable("resourceId") String resourceId,
                                          HttpServletResponse response) {
    if (deploymentId == null) {
      throw new FlowableIllegalArgumentException("No deployment id provided");
    }
    if (resourceId == null) {
      throw new FlowableIllegalArgumentException("No resource id provided");
    }

    // Check if deployment exists
    FormDeployment deployment = formRepositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();
    if (deployment == null) {
      throw new FlowableObjectNotFoundException("Could not find a form deployment with id '" + deploymentId);
    }

    List<String> resourceList = formRepositoryService.getDeploymentResourceNames(deploymentId);

    if (resourceList.contains(resourceId)) {
      final InputStream resourceStream = formRepositoryService.getResourceAsStream(deploymentId, resourceId);

      String contentType = contentTypeResolver.resolveContentType(resourceId);
      response.setContentType(contentType);
      try {
        return IOUtils.toByteArray(resourceStream);
      } catch (Exception e) {
        throw new FlowableException("Error converting resource stream", e);
      }
    } else {
      // Resource not found in deployment
      throw new FlowableObjectNotFoundException("Could not find a resource with id '" + resourceId + "' in deployment '" + deploymentId);
    }
  }
}
