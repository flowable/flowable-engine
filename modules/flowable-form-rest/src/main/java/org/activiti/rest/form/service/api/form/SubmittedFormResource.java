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

import org.activiti.form.api.FormService;
import org.activiti.form.engine.ActivitiFormIllegalArgumentException;
import org.activiti.form.engine.ActivitiFormObjectNotFoundException;
import org.activiti.form.model.FormDefinition;
import org.activiti.rest.form.FormRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Yvo Swillens
 */
public class SubmittedFormResource {

  @Autowired
  protected FormService formService;

  @Autowired
  protected FormRestResponseFactory formRestResponseFactory;

  @RequestMapping(value = "/form/submitted-form", method = RequestMethod.POST, produces = "application/json")
  public void storeSubmittedForm(@RequestBody FormDefinitionRequest formDefinitionRequest, HttpServletRequest request) {

    FormDefinition formDefinition;

    if (formDefinitionRequest.getFormDefinitionKey() != null) {
      formDefinition = formService.getTaskFormDefinitionByKey(
          formDefinitionRequest.getFormDefinitionKey(),
          formDefinitionRequest.getProcessInstanceId(),
          formDefinitionRequest.getVariables(),
          formDefinitionRequest.getTenantId()
      );
    } else if (formDefinitionRequest.getFormId() != null) {
      formDefinition = formService.getTaskFormDefinitionById(
          formDefinitionRequest.getFormId(),
          formDefinitionRequest.getProcessInstanceId(),
          formDefinitionRequest.getVariables(),
          formDefinitionRequest.getTenantId()
      );
    } else {
      throw new ActivitiFormIllegalArgumentException("Either form definition key or form id must be provided in the request");
    }

    if (formDefinition == null) {
      throw new ActivitiFormObjectNotFoundException("Could not find a form definition");
    }

    formService.storeSubmittedForm(formDefinitionRequest.getVariables(), formDefinition, formDefinitionRequest.getTaskId(),
        formDefinitionRequest.getProcessInstanceId());
  }
}
