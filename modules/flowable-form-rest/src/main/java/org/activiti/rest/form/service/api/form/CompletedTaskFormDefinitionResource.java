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
package org.activiti.rest.form.service.api.form;

import javax.servlet.http.HttpServletRequest;

import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.form.api.FormService;
import org.activiti.form.model.CompletedFormDefinition;
import org.activiti.rest.form.FormRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author Yvo Swillens
 */
public class CompletedTaskFormDefinitionResource {

  @Autowired
  protected FormService formService;

  @Autowired
  protected FormRestResponseFactory formRestResponseFactory;

  @RequestMapping(value = "/form/completed-task-form", method = RequestMethod.POST, produces = "application/json")
  public CompletedTaskFormDefinitionResponse getCompletedTaskFormDefinition(@RequestBody FormDefinitionRequest formDefinitionRequest, HttpServletRequest request) {

    CompletedFormDefinition formDefinition;

    if (formDefinitionRequest.getParentDeploymentId() != null) {
      formDefinition = formService.getCompletedTaskFormDefinitionByKeyAndParentDeploymentId(
          formDefinitionRequest.getParentDeploymentId(),
          formDefinitionRequest.getFormDefinitionKey(),
          formDefinitionRequest.getTaskId(),
          formDefinitionRequest.getProcessInstanceId(),
          formDefinitionRequest.getVariables(),
          formDefinitionRequest.getTenantId()
      );
    } else if (formDefinitionRequest.getFormDefinitionKey() != null) {
      formDefinition = formService.getCompletedTaskFormDefinitionByKey(
          formDefinitionRequest.getFormDefinitionKey(),
          formDefinitionRequest.getTaskId(),
          formDefinitionRequest.getProcessInstanceId(),
          formDefinitionRequest.getVariables(),
          formDefinitionRequest.getTenantId()
      );
    } else if (formDefinitionRequest.getFormId() != null) {
      formDefinition = formService.getCompletedTaskFormDefinitionById(
          formDefinitionRequest.getFormId(),
          formDefinitionRequest.getTaskId(),
          formDefinitionRequest.getProcessInstanceId(),
          formDefinitionRequest.getVariables(),
          formDefinitionRequest.getTenantId()
      );
    } else {
      throw new ActivitiIllegalArgumentException("Either parent deployment key, form definition key or form id must be provided in the request");
    }

    if (formDefinition == null) {
      throw new ActivitiObjectNotFoundException("Could not find a form definition");
    }

    return formRestResponseFactory.createCompletedTaskFormDefinitionResponse(formDefinition);
  }
}
