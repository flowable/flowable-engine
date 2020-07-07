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
package org.flowable.form.rest.service.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormInstance;
import org.flowable.form.engine.test.FormDeploymentAnnotation;
import org.flowable.form.rest.service.BaseSpringRestTestCase;
import org.flowable.form.rest.service.api.form.FormRequest;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class FormInstanceCollectionResourceTest extends BaseSpringRestTestCase {

    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/rest/service/api/form/simple.form")
    public void testGetFormInstances() throws IOException {
        FormInfo formInfo = repositoryService.getFormModelByKey("form1");
        Map<String, Object> valuesMap = new HashMap<>();
        valuesMap.put("user", "test");
        valuesMap.put("number", "1234");

        Map<String, Object> formValues = formService.getVariablesFromFormSubmission(formInfo, valuesMap, "default");
        assertThat(formValues)
                .contains(
                        entry("user", "test"),
                        entry("form_form1_outcome", "default")
                );

        FormRequest formRequest = new FormRequest();
        formRequest.setFormDefinitionKey(formInfo.getKey());
        formRequest.setVariables(formValues);

        String json = objectMapper.writeValueAsString(formRequest);

        String url = "form/form-instances";
        storeFormInstance(url, json);
        assertResultsPresentInDataResponse(url, USER);
        
        List<FormInstance> formInstances = formService.createFormInstanceQuery().list();
        for (FormInstance formInstance : formInstances) {
            formService.deleteFormInstance(formInstance.getId());
        }
    }

    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/rest/service/api/form/simple.form")
    public void testGetFormInstanceById() throws IOException {
        FormInfo formInfo = repositoryService.getFormModelByKey("form1");
        Map<String, Object> valuesMap = new HashMap<>();
        valuesMap.put("user", "test");
        valuesMap.put("number", "1234");

        FormRequest formRequest = new FormRequest();
        formRequest.setFormDefinitionKey(formInfo.getKey());
        formRequest.setVariables(valuesMap);

        String json = objectMapper.writeValueAsString(formRequest);

        String url = "form/form-instances";
        storeFormInstance(url, json);
        
        // create another form instance
        valuesMap.put("user", "test2");
        valuesMap.put("number", "5678");
        json = objectMapper.writeValueAsString(formRequest);
        storeFormInstance(url, json);
        
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        // Check status and size
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        
        assertThat(dataNode).hasSize(2);
        
        String formInstanceId = dataNode.get(0).get("id").asText();
        response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url + "?id=" + formInstanceId), HttpStatus.SC_OK);

        // Check status and size
        dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        
        assertThat(dataNode).hasSize(1);
        assertThat(dataNode.get(0).get("id").asText()).isEqualTo(formInstanceId);
        
        List<FormInstance> formInstances = formService.createFormInstanceQuery().list();
        for (FormInstance formInstance : formInstances) {
            formService.deleteFormInstance(formInstance.getId());
        }
    }
}
