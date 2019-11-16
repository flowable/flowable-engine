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
package org.flowable.form.engine.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.DefaultTenantProvider;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormInstance;
import org.flowable.form.api.FormInstanceInfo;
import org.flowable.form.model.FormField;
import org.flowable.form.model.SimpleFormModel;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class FormInstanceTest extends AbstractFlowableFormTest {

    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form")
    public void submitSimpleForm() throws Exception {
        FormInfo formInfo = repositoryService.getFormModelByKey("form1");

        Map<String, Object> valuesMap = new HashMap<>();
        valuesMap.put("input1", "test");
        Map<String, Object> formValues = formService.getVariablesFromFormSubmission(formInfo, valuesMap, "default");
        assertThat(formValues)
            .containsOnly(
                entry("input1", "test"),
                entry("form_form1_outcome", "default")
            );

        FormInstance formInstance = formService.createFormInstance(formValues, formInfo, null, null, null, null, "default");
        assertEquals(formInfo.getId(), formInstance.getFormDefinitionId());
        JsonNode formNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
        assertEquals("test", formNode.get("values").get("input1").asText());
        assertEquals("default", formNode.get("flowable_form_outcome").asText());

        FormInstanceInfo formInstanceModel = formService.getFormInstanceModelById(formInstance.getId(), null);
        assertEquals("form1", formInstanceModel.getKey());
        
        SimpleFormModel formModel = (SimpleFormModel) formInstanceModel.getFormModel();
        assertEquals(1, formModel.getFields().size());
        FormField formField = formModel.getFields().get(0);
        assertEquals("input1", formField.getId());
        assertEquals("test", formField.getValue());
        
        assertEquals(1, formService.createFormInstanceQuery().id(formInstance.getId()).count());
        formService.deleteFormInstance(formInstance.getId());
        assertEquals(0, formService.createFormInstanceQuery().id(formInstance.getId()).count());
    }
    
    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form", tenantId="flowable")
    public void submitSimpleFormWithTenant() throws Exception {
        FormInfo formInfo = repositoryService.getFormModelByKey("form1", "flowable", false);

        Map<String, Object> valuesMap = new HashMap<>();
        valuesMap.put("input1", "test");
        Map<String, Object> formValues = formService.getVariablesFromFormSubmission(formInfo, valuesMap, "default");
        assertThat(formValues)
            .containsOnly(
                entry("input1", "test"),
                entry("form_form1_outcome", "default")
            );

        FormInstance formInstance = formService.createFormInstance(valuesMap, formInfo, "aTaskId", null, null, "flowable", "default");
        assertEquals(formInfo.getId(), formInstance.getFormDefinitionId());
        JsonNode formNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
        assertEquals("test", formNode.get("values").get("input1").asText());
        assertEquals("default", formNode.get("flowable_form_outcome").asText());

        FormInstanceInfo formInstanceModel = formService.getFormInstanceModelByKey("form1", "aTaskId", null, null, "flowable", false);
        assertEquals("form1", formInstanceModel.getKey());
        
        SimpleFormModel formModel = (SimpleFormModel) formInstanceModel.getFormModel();
        assertEquals(1, formModel.getFields().size());
        FormField formField = formModel.getFields().get(0);
        assertEquals("input1", formField.getId());
        assertEquals("test", formField.getValue());
        
        formInstance = formService.createFormInstanceQuery().formDefinitionId(formInfo.getId()).tenantId("flowable").singleResult();
        assertNotNull(formInstance);
        assertEquals("flowable", formInstance.getTenantId());
        
        formService.deleteFormInstance(formInstance.getId());
        assertEquals(0, formService.createFormInstanceQuery().formDefinitionId(formInfo.getId()).tenantId("flowable").count());
    }
    
    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form", tenantId="")
    public void submitSimpleFormWithFallbackTenant() throws Exception {
        FormInfo formInfo = repositoryService.getFormModelByKey("form1", "flowable", true);

        Map<String, Object> valuesMap = new HashMap<>();
        valuesMap.put("input1", "test");
        Map<String, Object> formValues = formService.getVariablesFromFormSubmission(formInfo, valuesMap, "default");
        assertEquals("test", formValues.get("input1"));
        assertThat(formValues)
            .containsOnly(
                entry("input1", "test"),
                entry("form_form1_outcome", "default")
            );

        FormInstance formInstance = formService.createFormInstance(formValues, formInfo, "aTaskId", null, null, "flowable", "default");
        assertEquals(formInfo.getId(), formInstance.getFormDefinitionId());
        JsonNode formNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
        assertEquals("test", formNode.get("values").get("input1").asText());
        assertEquals("default", formNode.get("flowable_form_outcome").asText());

        FormInstanceInfo formInstanceModel = formService.getFormInstanceModelByKey("form1", "aTaskId", null, null, "flowable", true);
        assertEquals("form1", formInstanceModel.getKey());
        
        SimpleFormModel formModel = (SimpleFormModel) formInstanceModel.getFormModel();
        assertEquals(1, formModel.getFields().size());
        FormField formField = formModel.getFields().get(0);
        assertEquals("input1", formField.getId());
        assertEquals("test", formField.getValue());
        
        formInstance = formService.createFormInstanceQuery().formDefinitionId(formInfo.getId()).tenantId("flowable").singleResult();
        assertNotNull(formInstance);
        assertEquals("flowable", formInstance.getTenantId());
        
        formService.deleteFormInstance(formInstance.getId());
        assertEquals(0, formService.createFormInstanceQuery().formDefinitionId(formInfo.getId()).tenantId("flowable").count());
    }
    
    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form", tenantId="defaultFlowable")
    public void submitSimpleFormWithGlobalFallbackTenant() throws Exception {
        DefaultTenantProvider originalDefaultTenantProvider = formEngineConfiguration.getDefaultTenantProvider();
        formEngineConfiguration.setFallbackToDefaultTenant(true);
        formEngineConfiguration.setDefaultTenantValue("defaultFlowable");
        try {
            FormInfo formInfo = repositoryService.getFormModelByKey("form1", "flowable", false);
    
            Map<String, Object> valuesMap = new HashMap<>();
            valuesMap.put("input1", "test");
            Map<String, Object> formValues = formService.getVariablesFromFormSubmission(formInfo, valuesMap, "default");
            assertThat(formValues)
                .containsOnly(
                    entry("input1", "test"),
                    entry("form_form1_outcome", "default")
                );
    
            FormInstance formInstance = formService.createFormInstance(formValues, formInfo, "aTaskId", null, null, "flowable", "default");
            assertEquals(formInfo.getId(), formInstance.getFormDefinitionId());
            JsonNode formNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
            assertEquals("test", formNode.get("values").get("input1").asText());
            assertEquals("default", formNode.get("flowable_form_outcome").asText());
    
            FormInstanceInfo formInstanceModel = formService.getFormInstanceModelByKey("form1", "aTaskId", null, null, "flowable", false);
            assertEquals("form1", formInstanceModel.getKey());
            
            SimpleFormModel formModel = (SimpleFormModel) formInstanceModel.getFormModel();
            assertEquals(1, formModel.getFields().size());
            FormField formField = formModel.getFields().get(0);
            assertEquals("input1", formField.getId());
            assertEquals("test", formField.getValue());
            
            formInstance = formService.createFormInstanceQuery().formDefinitionId(formInfo.getId()).tenantId("flowable").singleResult();
            assertNotNull(formInstance);
            assertEquals("flowable", formInstance.getTenantId());
            
            formService.deleteFormInstance(formInstance.getId());
            assertEquals(0, formService.createFormInstanceQuery().formDefinitionId(formInfo.getId()).tenantId("flowable").count());
            
        } finally {
            formEngineConfiguration.setFallbackToDefaultTenant(false);
            formEngineConfiguration.setDefaultTenantProvider(originalDefaultTenantProvider);
        }
    }

    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/form_with_dates.form")
    public void submitDateForm() throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        FormInfo formInfo = repositoryService.getFormModelByKey("dateform");

        Map<String, Object> valuesMap = new HashMap<>();
        valuesMap.put("input1", "test");
        valuesMap.put("date1", "2016-01-01");
        valuesMap.put("date2", "2017-01-01");
        valuesMap.put("date3", format.parse("2018-01-01"));
        Map<String, Object> formValues = formService.getVariablesFromFormSubmission(formInfo, valuesMap, "date");
        assertThat(formValues)
            .containsOnly(
                entry("input1", "test"),
                entry("date1", new LocalDate(2016, 1, 1)),
                entry("date2", new LocalDate(2017, 1, 1)),
                entry("date3", new LocalDate(2018, 1, 1)),
                entry("form_dateform_outcome", "date")
            );

        FormInstance formInstance = formService.createFormInstance(formValues, formInfo, null, null, null, null, "date");
        assertEquals(formInfo.getId(), formInstance.getFormDefinitionId());
        JsonNode formNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
        JsonNode valuesNode = formNode.get("values");
        assertEquals(4, valuesNode.size());
        assertEquals("test", valuesNode.get("input1").asText());
        assertEquals("2016-01-01", valuesNode.get("date1").asText());
        assertEquals("2017-01-01", valuesNode.get("date2").asText());
        assertEquals("2018-01-01", valuesNode.get("date3").asText());
        assertEquals("date", formNode.get("flowable_form_outcome").asText());
        
        assertEquals(1, formService.createFormInstanceQuery().id(formInstance.getId()).count());
        formService.deleteFormInstancesByFormDefinition(formInstance.getFormDefinitionId());
        assertEquals(0, formService.createFormInstanceQuery().id(formInstance.getId()).count());
    }

    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form")
    public void saveSimpleForm() throws Exception {
        String taskId = "123456";
        Authentication.setAuthenticatedUserId("User");
        FormInfo formInfo = repositoryService.getFormModelByKey("form1");

        Map<String, Object> valuesMap = new HashMap<>();
        valuesMap.put("input1", "test");
        Map<String, Object> formValues = formService.getVariablesFromFormSubmission(formInfo, valuesMap, "default");
        assertThat(formValues)
            .containsOnly(
                entry("input1", "test"),
                entry("form_form1_outcome", "default")
            );

        FormInstance formInstance = formService.saveFormInstance(formValues, formInfo, taskId, "someId", "testDefId", null, "default");
        assertEquals(formInfo.getId(), formInstance.getFormDefinitionId());
        JsonNode formNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
        assertEquals("test", formNode.get("values").get("input1").asText());
        assertEquals("default", formNode.get("flowable_form_outcome").asText());

        FormInstanceInfo formInstanceModel = formService.getFormInstanceModelById(formInstance.getId(), null);
        assertEquals("form1", formInstanceModel.getKey());
        SimpleFormModel formModel = (SimpleFormModel) formInstanceModel.getFormModel();
        assertEquals(1, formModel.getFields().size());
        FormField formField = formModel.getFields().get(0);
        assertEquals("input1", formField.getId());
        assertEquals("test", formField.getValue());

        valuesMap = new HashMap<>();
        valuesMap.put("input1", "updatedValue");
        formValues = formService.getVariablesFromFormSubmission(formInfo, valuesMap, "updatedOutcome");
        assertThat(formValues)
            .containsOnly(
                entry("input1", "updatedValue"),
                entry("form_form1_outcome", "updatedOutcome")
            );

        formInstance = formService.saveFormInstance(formValues, formInfo, taskId, "someId", "testDefId", null, "updatedOutcome");
        assertEquals(formInfo.getId(), formInstance.getFormDefinitionId());
        formNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
        assertEquals("updatedValue", formNode.get("values").get("input1").asText());
        assertEquals("updatedOutcome", formNode.get("flowable_form_outcome").asText());

        formInstanceModel = formService.getFormInstanceModelById(formInstance.getId(), null);
        assertEquals("form1", formInstanceModel.getKey());
        assertEquals("User", formInstanceModel.getSubmittedBy());
        formModel = (SimpleFormModel) formInstanceModel.getFormModel();
        assertEquals(1, formModel.getFields().size());
        formField = formModel.getFields().get(0);
        assertEquals("input1", formField.getId());
        assertEquals("updatedValue", formField.getValue());

        assertEquals(1, formService.createFormInstanceQuery().formDefinitionId(formInfo.getId()).count());
        
        assertEquals(1, formService.createFormInstanceQuery().id(formInstance.getId()).count());
        formService.deleteFormInstancesByProcessDefinition("testDefId");
        assertEquals(0, formService.createFormInstanceQuery().id(formInstance.getId()).count());
    }
    
    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/hyperlink.form")
    public void hyperlinkForm() throws Exception {
        FormInfo formInfo = repositoryService.getFormModelByKey("hyperlink");

        // test setting hyperlink from variable
        Map<String, Object> variables = new HashMap<>();
        variables.put("plainLink", "http://notmylink.com");
        variables.put("page", "downloads.html");
        Map<String, Object> formValues = formService.getVariablesFromFormSubmission(formInfo, variables, "default");
        // Should be not contain expressionLink as this is not an input element
        assertThat(formValues)
            .containsOnly(
                entry("plainLink", "http://notmylink.com"),
                entry("form_hyperlink_outcome", "default")
            )
            .doesNotContainKeys("expressionLink");

        // test setting hyperlink from variable
        FormInstance formInstance = formService.createFormInstanceWithScopeId(formValues, formInfo, "123456", "someId", "cmmn", "testDefId", null, "default");
        assertEquals(formInfo.getId(), formInstance.getFormDefinitionId());
        JsonNode formNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
        assertEquals("http://notmylink.com", formNode.get("values").get("plainLink").asText());
        // no variable provided for expressionLink and anotherPlainLink
        assertNull(formNode.get("values").get("expressionLink"));
        assertNull(formNode.get("values").get("anotherPlainLink"));

        // test hyperlink expression parsing in GetFormInstanceModelCmd
        FormInstanceInfo formInstanceModel = formService.getFormInstanceModelById(formInstance.getId(), variables);
        assertEquals("hyperlink", formInstanceModel.getKey());

        SimpleFormModel formModel = (SimpleFormModel) formInstanceModel.getFormModel();
        assertEquals(3, formModel.getFields().size());
        FormField plainLinkField = formModel.getFields().get(0);
        assertEquals("plainLink", plainLinkField.getId());
        assertEquals("http://notmylink.com", plainLinkField.getValue());

        FormField expressionLinkField = formModel.getFields().get(1);
        assertEquals("expressionLink", expressionLinkField.getId());
        assertEquals("http://www.flowable.org/downloads.html", expressionLinkField.getValue());

        FormField anotherPlainLinkField = formModel.getFields().get(2);
        assertEquals("anotherPlainLink", anotherPlainLinkField.getId());
        assertEquals("http://blog.flowable.org", anotherPlainLinkField.getValue());

        // test hyperlink expression parsing in GetFormModelWithVariablesCmd
        FormInfo formInfoWithVars = formService.getFormModelWithVariablesById(formInfo.getId(), null, variables);
        SimpleFormModel model = (SimpleFormModel) formInfoWithVars.getFormModel();
        assertEquals(3, model.getFields().size());
        plainLinkField = model.getFields().get(0);
        assertEquals("plainLink", plainLinkField.getId());
        assertEquals("http://notmylink.com", plainLinkField.getValue());

        expressionLinkField = model.getFields().get(1);
        assertEquals("expressionLink", expressionLinkField.getId());
        assertEquals("http://www.flowable.org/downloads.html", expressionLinkField.getValue());

        anotherPlainLinkField = model.getFields().get(2);
        assertEquals("anotherPlainLink", anotherPlainLinkField.getId());
        assertEquals("http://blog.flowable.org", anotherPlainLinkField.getValue());
        
        assertEquals(1, formService.createFormInstanceQuery().id(formInstance.getId()).count());
        formService.deleteFormInstancesByScopeDefinition("testDefId");
        assertEquals(0, formService.createFormInstanceQuery().id(formInstance.getId()).count());
    }

}
