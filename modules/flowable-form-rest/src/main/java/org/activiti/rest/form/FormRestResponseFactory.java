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
package org.activiti.rest.form;

import org.activiti.form.api.Form;
import org.activiti.form.api.FormDeployment;
import org.activiti.form.model.CompletedFormDefinition;
import org.activiti.form.model.FormDefinition;
import org.activiti.rest.form.service.api.form.CompletedFormDefinitionResponse;
import org.activiti.rest.form.service.api.form.RuntimeFormDefinitionResponse;
import org.activiti.rest.form.service.api.repository.FormDeploymentResponse;
import org.activiti.rest.form.service.api.repository.FormResponse;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Default implementation of a {@link FormRestResponseFactory}.
 *
 * @author Yvo Swillens
 */
public class FormRestResponseFactory {

  public RuntimeFormDefinitionResponse createRuntimeFormDefinitionResponse(FormDefinition runtimeFormDefinition) {
    return createRuntimeFormDefinitionResponse(runtimeFormDefinition, createUrlBuilder());
  }

  public RuntimeFormDefinitionResponse createRuntimeFormDefinitionResponse(FormDefinition runtimeFormDefinition, FormRestUrlBuilder urlBuilder) {
    RuntimeFormDefinitionResponse response = new RuntimeFormDefinitionResponse(runtimeFormDefinition);
    response.setUrl(urlBuilder.buildUrl(FormRestUrls.URL_RUNTIME_TASK_FORM));

    return response;
  }

  public CompletedFormDefinitionResponse createCompletedFormDefinitionResponse(CompletedFormDefinition completedFormDefinition) {
    return createCompletedFormDefinitionResponse(completedFormDefinition, createUrlBuilder());
  }

  public CompletedFormDefinitionResponse createCompletedFormDefinitionResponse(CompletedFormDefinition completedFormDefinition, FormRestUrlBuilder urlBuilder) {
    CompletedFormDefinitionResponse response = new CompletedFormDefinitionResponse(completedFormDefinition);
    response.setUrl(urlBuilder.buildUrl(FormRestUrls.URL_COMPLETED_TASK_FORM));

    return response;
  }

  public FormResponse createFormResponse(Form form) {
    return createFormResponse(form, createUrlBuilder());
  }

  public FormResponse createFormResponse(Form form, FormRestUrlBuilder urlBuilder) {
    FormResponse response = new FormResponse(form);
    response.setUrl(urlBuilder.buildUrl(FormRestUrls.URL_FORM, form.getId()));

    return response;
  }

  public List<FormResponse> createFormResponseList(List<Form> forms) {
    FormRestUrlBuilder urlBuilder = createUrlBuilder();
    List<FormResponse> responseList = new ArrayList<>();
    for (Form instance : forms) {
      responseList.add(createFormResponse(instance, urlBuilder));
    }
    return responseList;
  }

  public List<FormDeploymentResponse> createFormDeploymentResponseList(List<FormDeployment> deployments) {
    FormRestUrlBuilder urlBuilder = createUrlBuilder();
    List<FormDeploymentResponse> responseList = new ArrayList<>();
    for (FormDeployment instance : deployments) {
      responseList.add(createFormDeploymentResponse(instance, urlBuilder));
    }
    return responseList;
  }

  public FormDeploymentResponse createFormDeploymentResponse(FormDeployment deployment) {
    return createFormDeploymentResponse(deployment, createUrlBuilder());
  }

  public FormDeploymentResponse createFormDeploymentResponse(FormDeployment deployment, FormRestUrlBuilder urlBuilder) {
    return new FormDeploymentResponse(deployment, urlBuilder.buildUrl(FormRestUrls.URL_DEPLOYMENT, deployment.getId()));
  }

  protected FormRestUrlBuilder createUrlBuilder() {
    return FormRestUrlBuilder.fromCurrentRequest();
  }
}
