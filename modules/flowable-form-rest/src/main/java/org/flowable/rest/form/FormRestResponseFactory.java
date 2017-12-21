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
package org.flowable.rest.form;

import java.util.ArrayList;
import java.util.List;

import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormInstance;
import org.flowable.form.model.FormInstanceModel;
import org.flowable.form.model.FormModel;
import org.flowable.rest.form.service.api.form.FormInstanceModelResponse;
import org.flowable.rest.form.service.api.form.FormInstanceResponse;
import org.flowable.rest.form.service.api.form.FormModelResponse;
import org.flowable.rest.form.service.api.repository.FormDefinitionResponse;
import org.flowable.rest.form.service.api.repository.FormDeploymentResponse;

/**
 *
 * Default implementation of a {@link FormRestResponseFactory}.
 *
 * @author Yvo Swillens
 */
public class FormRestResponseFactory {

    public FormModelResponse createFormModelResponse(FormModel formModel) {
        return createFormModelResponse(formModel, createUrlBuilder());
    }

    public FormModelResponse createFormModelResponse(FormModel formModel, FormRestUrlBuilder urlBuilder) {
        FormModelResponse response = new FormModelResponse(formModel);
        response.setUrl(urlBuilder.buildUrl(FormRestUrls.URL_FORM_MODEL));

        return response;
    }

    public List<FormInstanceResponse> createFormInstanceResponse(List<FormInstance> formInstances) {
        FormRestUrlBuilder urlBuilder = new FormRestUrlBuilder();
        List<FormInstanceResponse> responseList = new ArrayList<>();
        for (FormInstance formInstance : formInstances) {
            responseList.add(createFormInstanceResponse(formInstance, urlBuilder));
        }

        return responseList;
    }

    public FormInstanceResponse createFormInstanceResponse(FormInstance formInstance) {
        return createFormInstanceResponse(formInstance, createUrlBuilder());
    }

    public FormInstanceResponse createFormInstanceResponse(FormInstance formInstance, FormRestUrlBuilder urlBuilder) {
        FormInstanceResponse response = new FormInstanceResponse(formInstance);
        response.setUrl(urlBuilder.buildUrl(FormRestUrls.URL_FORM_INSTANCE_QUERY));

        return response;
    }

    public List<FormInstanceModelResponse> createFormInstanceModelResponse(List<FormInstanceModel> formInstanceModels) {
        FormRestUrlBuilder urlBuilder = new FormRestUrlBuilder();
        List<FormInstanceModelResponse> responseList = new ArrayList<>();
        for (FormInstanceModel formInstanceModel : formInstanceModels) {
            responseList.add(createFormInstanceModelResponse(formInstanceModel, urlBuilder));
        }

        return responseList;
    }

    public FormInstanceModelResponse createFormInstanceModelResponse(FormInstanceModel formInstanceModel) {
        return createFormInstanceModelResponse(formInstanceModel, createUrlBuilder());
    }

    public FormInstanceModelResponse createFormInstanceModelResponse(FormInstanceModel formInstanceModel, FormRestUrlBuilder urlBuilder) {
        FormInstanceModelResponse response = new FormInstanceModelResponse(formInstanceModel);
        response.setUrl(urlBuilder.buildUrl(FormRestUrls.URL_FORM_INSTANCE_MODEL));

        return response;
    }

    public FormDefinitionResponse createFormDefinitionResponse(FormDefinition formDefinition) {
        return createFormDefinitionResponse(formDefinition, createUrlBuilder());
    }

    public FormDefinitionResponse createFormDefinitionResponse(FormDefinition formDefinition, FormRestUrlBuilder urlBuilder) {
        FormDefinitionResponse response = new FormDefinitionResponse(formDefinition);
        response.setUrl(urlBuilder.buildUrl(FormRestUrls.URL_FORM_DEFINITION, formDefinition.getId()));

        return response;
    }

    public List<FormDefinitionResponse> createFormResponseList(List<FormDefinition> formDefinitions) {
        FormRestUrlBuilder urlBuilder = createUrlBuilder();
        List<FormDefinitionResponse> responseList = new ArrayList<>();
        for (FormDefinition formDefinition : formDefinitions) {
            responseList.add(createFormDefinitionResponse(formDefinition, urlBuilder));
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
