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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

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

import net.javacrumbs.jsonunit.core.Option;

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
        assertThat(formInstance.getFormDefinitionId()).isEqualTo(formInfo.getId());
        JsonNode formNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
        assertThatJson(formNode)
                .isEqualTo("{"
                        + "   values: {"
                        + "     input1: 'test'"
                        + " },"
                        + " flowable_form_outcome: 'default'"
                        + "}");

        FormInstanceInfo formInstanceModel = formService.getFormInstanceModelById(formInstance.getId(), null);
        assertThat(formInstanceModel.getKey()).isEqualTo("form1");
        
        SimpleFormModel formModel = (SimpleFormModel) formInstanceModel.getFormModel();
        assertThat(formModel.getFields().size()).isOne();
        assertThat(formModel.getFields())
            .extracting(FormField::getId, FormField::getValue)
            .containsExactly(tuple("input1", "test"));

        assertThat(formService.createFormInstanceQuery().id(formInstance.getId()).count()).isOne();
        formService.deleteFormInstance(formInstance.getId());
        assertThat(formService.createFormInstanceQuery().id(formInstance.getId()).count()).isZero();
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
        assertThat(formInstance.getFormDefinitionId()).isEqualTo(formInfo.getId());
        JsonNode formNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
        assertThatJson(formNode)
                .isEqualTo("{"
                        + "   values: {"
                        + "     input1: 'test'"
                        + " },"
                        + " flowable_form_outcome: 'default'"
                        + "}");

        FormInstanceInfo formInstanceModel = formService.getFormInstanceModelByKey("form1", "aTaskId", null, null, "flowable", false);
        assertThat(formInstanceModel.getKey()).isEqualTo("form1");
        
        SimpleFormModel formModel = (SimpleFormModel) formInstanceModel.getFormModel();
        assertThat(formModel.getFields())
                .extracting(FormField::getId, FormField::getValue)
                .containsExactly(tuple("input1", "test"));
        
        formInstance = formService.createFormInstanceQuery().formDefinitionId(formInfo.getId()).tenantId("flowable").singleResult();
        assertThat(formInstance).isNotNull();
        assertThat(formInstance.getTenantId()).isEqualTo("flowable");
        
        formService.deleteFormInstance(formInstance.getId());
        assertThat(formService.createFormInstanceQuery().formDefinitionId(formInfo.getId()).tenantId("flowable").count()).isZero();
    }
    
    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form", tenantId="")
    public void submitSimpleFormWithFallbackTenant() throws Exception {
        FormInfo formInfo = repositoryService.getFormModelByKey("form1", "flowable", true);

        Map<String, Object> valuesMap = new HashMap<>();
        valuesMap.put("input1", "test");
        Map<String, Object> formValues = formService.getVariablesFromFormSubmission(formInfo, valuesMap, "default");
        assertThat(formValues)
            .containsOnly(
                entry("input1", "test"),
                entry("form_form1_outcome", "default")
            );

        FormInstance formInstance = formService.createFormInstance(formValues, formInfo, "aTaskId", null, null, "flowable", "default");
        assertThat(formInstance.getFormDefinitionId()).isEqualTo(formInfo.getId());
        JsonNode formNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
        assertThatJson(formNode)
                .isEqualTo("{"
                        + "   values: {"
                        + "     input1: 'test'"
                        + " },"
                        + " flowable_form_outcome: 'default'"
                        + "}");

        FormInstanceInfo formInstanceModel = formService.getFormInstanceModelByKey("form1", "aTaskId", null, null, "flowable", true);
        assertThat(formInstanceModel.getKey()).isEqualTo("form1");
        
        SimpleFormModel formModel = (SimpleFormModel) formInstanceModel.getFormModel();
        assertThat(formModel.getFields())
                .extracting(FormField::getId, FormField::getValue)
                .containsExactly(tuple("input1", "test"));
        
        formInstance = formService.createFormInstanceQuery().formDefinitionId(formInfo.getId()).tenantId("flowable").singleResult();
        assertThat(formInstance).isNotNull();
        assertThat(formInstance.getTenantId()).isEqualTo("flowable");
        
        formService.deleteFormInstance(formInstance.getId());
        assertThat(formService.createFormInstanceQuery().formDefinitionId(formInfo.getId()).tenantId("flowable").count()).isZero();
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
            assertThat(formInstance.getFormDefinitionId()).isEqualTo(formInfo.getId());
            JsonNode formNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
            assertThatJson(formNode)
                    .isEqualTo("{"
                            + "   values: {"
                            + "     input1: 'test'"
                            + " },"
                            + " flowable_form_outcome: 'default'"
                            + "}");

            FormInstanceInfo formInstanceModel = formService.getFormInstanceModelByKey("form1", "aTaskId", null, null, "flowable", false);
            assertThat(formInstanceModel.getKey()).isEqualTo("form1");
            
            SimpleFormModel formModel = (SimpleFormModel) formInstanceModel.getFormModel();
            assertThat(formModel.getFields())
                    .extracting(FormField::getId, FormField::getValue)
                    .containsExactly(tuple("input1", "test"));
            
            formInstance = formService.createFormInstanceQuery().formDefinitionId(formInfo.getId()).tenantId("flowable").singleResult();
            assertThat(formInstance).isNotNull();
            assertThat(formInstance.getTenantId()).isEqualTo("flowable");
            
            formService.deleteFormInstance(formInstance.getId());
            assertThat(formService.createFormInstanceQuery().formDefinitionId(formInfo.getId()).tenantId("flowable").count()).isZero();
            
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
        assertThat(formInstance.getFormDefinitionId()).isEqualTo(formInfo.getId());
        JsonNode formNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
        assertThatJson(formNode)
                .when(Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{"
                        + "   values: {"
                        + "     input1: 'test',"
                        + "     date1: '2016-01-01',"
                        + "     date2: '2017-01-01',"
                        + "     date3: '2018-01-01'"
                        + " },"
                        + " flowable_form_outcome: 'date'"
                        + "}");

        assertThat(formService.createFormInstanceQuery().id(formInstance.getId()).count()).isOne();
        formService.deleteFormInstancesByFormDefinition(formInstance.getFormDefinitionId());
        assertThat(formService.createFormInstanceQuery().id(formInstance.getId()).count()).isZero();
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
        assertThat(formInstance.getFormDefinitionId()).isEqualTo(formInfo.getId());
        JsonNode formNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
        assertThatJson(formNode)
                .isEqualTo("{"
                        + "   values: {"
                        + "     input1: 'test'"
                        + " },"
                        + " flowable_form_outcome: 'default'"
                        + "}");

        FormInstanceInfo formInstanceModel = formService.getFormInstanceModelById(formInstance.getId(), null);
        assertThat(formInstanceModel.getKey()).isEqualTo("form1");
        SimpleFormModel formModel = (SimpleFormModel) formInstanceModel.getFormModel();
        assertThat(formModel.getFields())
                .extracting(FormField::getId, FormField::getValue)
                .containsExactly(tuple("input1", "test"));

        valuesMap = new HashMap<>();
        valuesMap.put("input1", "updatedValue");
        formValues = formService.getVariablesFromFormSubmission(formInfo, valuesMap, "updatedOutcome");
        assertThat(formValues)
            .containsOnly(
                entry("input1", "updatedValue"),
                entry("form_form1_outcome", "updatedOutcome")
            );

        formInstance = formService.saveFormInstance(formValues, formInfo, taskId, "someId", "testDefId", null, "updatedOutcome");
        assertThat(formInstance.getFormDefinitionId()).isEqualTo(formInfo.getId());
        formNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
        assertThatJson(formNode)
                .isEqualTo("{"
                        + "   values: {"
                        + "     input1: 'updatedValue'"
                        + " },"
                        + " flowable_form_outcome: 'updatedOutcome'"
                        + "}");

        formInstanceModel = formService.getFormInstanceModelById(formInstance.getId(), null);
        assertThat(formInstanceModel.getKey()).isEqualTo("form1");
        assertThat(formInstanceModel.getSubmittedBy()).isEqualTo("User");
        formModel = (SimpleFormModel) formInstanceModel.getFormModel();
        assertThat(formModel.getFields())
                .extracting(FormField::getId, FormField::getValue)
                .containsExactly(tuple("input1", "updatedValue"));

        assertThat(formService.createFormInstanceQuery().formDefinitionId(formInfo.getId()).count()).isOne();
        
        assertThat(formService.createFormInstanceQuery().id(formInstance.getId()).count()).isOne();
        formService.deleteFormInstancesByProcessDefinition("testDefId");
        assertThat(formService.createFormInstanceQuery().id(formInstance.getId()).count()).isZero();
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
        assertThat(formInstance.getFormDefinitionId()).isEqualTo(formInfo.getId());
        JsonNode formNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
        assertThatJson(formNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "   values: {"
                        + "     plainLink: 'http://notmylink.com'"
                        + " }"
                        + "}");
        // no variable provided for expressionLink and anotherPlainLink
        assertThat(formNode.get("values").get("expressionLink")).isNull();
        assertThat(formNode.get("values").get("anotherPlainLink")).isNull();

        // test hyperlink expression parsing in GetFormInstanceModelCmd
        FormInstanceInfo formInstanceModel = formService.getFormInstanceModelById(formInstance.getId(), variables);
        assertThat(formInstanceModel.getKey()).isEqualTo("hyperlink");

        SimpleFormModel formModel = (SimpleFormModel) formInstanceModel.getFormModel();
        assertThat(formModel.getFields())
                .extracting(FormField::getId, FormField::getValue)
                .containsExactly(
                        tuple("plainLink", "http://notmylink.com"),
                        tuple("expressionLink", "http://www.flowable.org/downloads.html"),
                        tuple("anotherPlainLink", "http://blog.flowable.org")
                );

        // test hyperlink expression parsing in GetFormModelWithVariablesCmd
        FormInfo formInfoWithVars = formService.getFormModelWithVariablesById(formInfo.getId(), null, variables);
        SimpleFormModel model = (SimpleFormModel) formInfoWithVars.getFormModel();
        assertThat(model.getFields())
                .extracting(FormField::getId, FormField::getValue)
                .containsExactly(
                        tuple("plainLink", "http://notmylink.com"),
                        tuple("expressionLink", "http://www.flowable.org/downloads.html"),
                        tuple("anotherPlainLink", "http://blog.flowable.org")
                );
        
        assertThat(formService.createFormInstanceQuery().id(formInstance.getId()).count()).isOne();
        formService.deleteFormInstancesByScopeDefinition("testDefId");
        assertThat(formService.createFormInstanceQuery().id(formInstance.getId()).count()).isZero();
    }

}
