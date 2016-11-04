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
package org.activiti.rest.dmn.service.api.repository;

import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.activiti.dmn.api.DmnDeployment;
import org.activiti.dmn.api.DmnRepositoryService;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.rest.application.ContentTypeResolver;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Yvo Swillens
 */
public class BaseDmnDeploymentResourceDataResource {

  @Autowired
  protected ContentTypeResolver contentTypeResolver;

  @Autowired
  protected DmnRepositoryService dmnRepositoryService;

  protected byte[] getDmnDeploymentResourceData(String deploymentId, String resourceId, HttpServletResponse response) {

    if (deploymentId == null) {
      throw new ActivitiIllegalArgumentException("No deployment id provided");
    }
    if (resourceId == null) {
      throw new ActivitiIllegalArgumentException("No resource id provided");
    }

    // Check if deployment exists
    DmnDeployment deployment = dmnRepositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();
    if (deployment == null) {
      throw new ActivitiObjectNotFoundException("Could not find a DMN deployment with id '" + deploymentId);
    }

    List<String> resourceList = dmnRepositoryService.getDeploymentResourceNames(deploymentId);

    if (resourceList.contains(resourceId)) {
      final InputStream resourceStream = dmnRepositoryService.getResourceAsStream(deploymentId, resourceId);

      String contentType = contentTypeResolver.resolveContentType(resourceId);
      response.setContentType(contentType);
      try {
        return IOUtils.toByteArray(resourceStream);
      } catch (Exception e) {
        throw new ActivitiException("Error converting resource stream", e);
      }
    } else {
      // Resource not found in deployment
      throw new ActivitiObjectNotFoundException("Could not find a resource with id '" + resourceId + "' in deployment '" + deploymentId);
    }
  }
}
