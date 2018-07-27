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

        FormRequest formRequest = new FormRequest();
        formRequest.setFormDefinitionKey(formInfo.getKey());
        formRequest.setVariables(formValues);


        String json = objectMapper.writeValueAsString(formRequest);

        String url = "form/form-instances";
        storeFormInstance(url, json);
        assertResultsPresentInDataResponse(url, USER);

    }

}