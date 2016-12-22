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

/**
 * @author Yvo Swillens
 */
public class FormDeploymentResourceDataResource {

  @Autowired
  protected ContentTypeResolver contentTypeResolver;

  @Autowired
  protected FormRepositoryService formRepositoryService;

  @RequestMapping(value = "/form-repository/deployments/{deploymentId}/resourcedata/{resourceId}", method = RequestMethod.GET)
  @ResponseBody
  public byte[] getFormDeploymentResource(@PathVariable("deploymentId") String deploymentId, @PathVariable("resourceId") String resourceId, HttpServletResponse response) {
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
