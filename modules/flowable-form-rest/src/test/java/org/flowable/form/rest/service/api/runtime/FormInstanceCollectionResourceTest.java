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

import org.flowable.form.api.FormInfo;
import org.flowable.form.engine.test.FormDeploymentAnnotation;
import org.flowable.form.rest.service.BaseSpringRestTestCase;
import org.flowable.form.rest.service.api.form.FormRequest;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FormInstanceCollectionResourceTest extends BaseSpringRestTestCase {

    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/rest/service/api/form/simple.form")
    public void testGetFormInstances() throws IOException {

        FormInfo formInfo = repositoryService.getFormModelByKey("form1");
        Map<String, Object> valuesMap = new HashMap<>();
        valuesMap.put("user", "test");
        valuesMap.put("number", "1234");


        Map<String, Object> formValues = formService.getVariablesFromFormSubmission(formInfo, valuesMap, "default");
        assertEquals("test", formValues.get("user"));
        assertEquals("default", formValues.get("form_form1_outcome"));

        FormRequest formRequest = new FormRequest();
        formRequest.setFormDefinitionKey(formInfo.getKey());
        formRequest.setVariables(formValues);


        String json = objectMapper.writeValueAsString(formRequest);

        String url = "form/form-instances";
        storeFormInstance(url, json);
        assertResultsPresentInDataResponse(url, USER);

    }

}
