package org.flowable.form.rest.service.api.runtime;

import org.apache.commons.io.IOUtils;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.editor.form.converter.FormJsonConverter;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormInstance;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.test.FlowableFormRule;
import org.flowable.form.engine.test.FormDeploymentAnnotation;
import org.flowable.form.model.FormField;
import org.flowable.form.model.SimpleFormModel;
import org.flowable.form.rest.service.BaseSpringRestTestCase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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

        String submittedBy = "User";
        Authentication.setAuthenticatedUserId(submittedBy);
        FormInstance formInstance = formService.createFormInstance(formValues, formInfo, null, null, null);

        String id = formInstance.getId();
        String url = "form/form-instances?id=" + id;
        assertResultsPresentInDataResponse(url, submittedBy, id);

    }

}