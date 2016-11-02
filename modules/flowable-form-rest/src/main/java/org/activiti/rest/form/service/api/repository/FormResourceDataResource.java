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
package org.activiti.rest.form.service.api.repository;

import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.form.api.Form;
import org.activiti.form.api.FormDeployment;
import org.activiti.form.api.FormRepositoryService;
import org.activiti.rest.form.common.ContentTypeResolver;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Yvo Swillens
 */
@RestController
public class FormResourceDataResource  {

  @Autowired
  protected FormRepositoryService formRepositoryService;

  @Autowired
  protected ContentTypeResolver contentTypeResolver;

  @RequestMapping(value = "/form-repository/forms/{formId}/resourcedata", method = RequestMethod.GET, produces = "application/json")
  public byte[] getDecisionTableResource(@PathVariable String formId, HttpServletResponse response) {
    Form form = formRepositoryService.getForm(formId);

    if (form == null) {
      throw new ActivitiObjectNotFoundException("Could not find a form with id '" + formId);
    }
    if (form.getDeploymentId() == null) {
      throw new ActivitiException("No deployment id available");
    }
    if (form.getResourceName() == null) {
      throw new ActivitiException("No resource name available");
    }

    // Check if deployment exists
    FormDeployment deployment = formRepositoryService.createDeploymentQuery().deploymentId(form.getDeploymentId()).singleResult();
    if (deployment == null) {
      throw new ActivitiObjectNotFoundException("Could not find a deployment with id '" + form.getDeploymentId());
    }

    List<String> resourceList = formRepositoryService.getDeploymentResourceNames(form.getDeploymentId());

    if (resourceList.contains(form.getResourceName())) {
      final InputStream resourceStream = formRepositoryService.getResourceAsStream(form.getDeploymentId(), form.getResourceName());

      String contentType = contentTypeResolver.resolveContentType(form.getResourceName());
      response.setContentType(contentType);
      try {
        return IOUtils.toByteArray(resourceStream);
      } catch (Exception e) {
        throw new ActivitiException("Error converting resource stream", e);
      }
    } else {
      // Resource not found in deployment
      throw new ActivitiObjectNotFoundException("Could not find a resource with id '" + form.getResourceName() + "' in deployment '" + form.getDeploymentId());
    }
  }
}
