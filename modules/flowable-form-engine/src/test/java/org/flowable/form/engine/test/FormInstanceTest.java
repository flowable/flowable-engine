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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.form.api.FormInstance;
import org.flowable.form.model.FormField;
import org.flowable.form.model.FormInstanceModel;
import org.flowable.form.model.FormModel;
import org.flowable.form.model.Option;
import org.flowable.form.model.OptionFormField;
import org.joda.time.LocalDate;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class FormInstanceTest extends AbstractFlowableFormTest {

    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form")
    public void submitSimpleForm() throws Exception {
        FormModel formModel = repositoryService.getFormModelByKey("form1");

        Map<String, Object> valuesMap = new HashMap<>();
        valuesMap.put("input1", "test");
        Map<String, Object> formValues = formService.getVariablesFromFormSubmission(formModel, valuesMap, "default");
        assertEquals("test", formValues.get("input1"));

        FormInstance formInstance = formService.createFormInstance(formValues, formModel, null, null, null);
        assertEquals(formModel.getId(), formInstance.getFormDefinitionId());
        JsonNode formNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
        assertEquals("test", formNode.get("values").get("input1").asText());
        assertEquals("default", formNode.get("flowable_form_outcome").asText());

        FormInstanceModel formInstanceModel = formService.getFormInstanceModelById(formInstance.getId(), null);
        assertEquals("form1", formInstanceModel.getKey());
        assertEquals(1, formInstanceModel.getFields().size());
        FormField formField = formInstanceModel.getFields().get(0);
        assertEquals("input1", formField.getId());
        assertEquals("test", formField.getValue());
    }

    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/form_with_dates.form")
    public void submitDateForm() throws Exception {
        FormModel formDefinition = repositoryService.getFormModelByKey("dateform");

        Map<String, Object> valuesMap = new HashMap<>();
        valuesMap.put("input1", "test");
        valuesMap.put("date1", "2016-01-01");
        valuesMap.put("date2", "2017-01-01");
        Map<String, Object> formValues = formService.getVariablesFromFormSubmission(formDefinition, valuesMap, "date");
        assertEquals("test", formValues.get("input1"));
        assertEquals(new LocalDate(2016, 1, 1), formValues.get("date1"));
        assertEquals(new LocalDate(2017, 1, 1), formValues.get("date2"));

        FormInstance formInstance = formService.createFormInstance(formValues, formDefinition, null, null, null);
        assertEquals(formDefinition.getId(), formInstance.getFormDefinitionId());
        JsonNode formNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
        JsonNode valuesNode = formNode.get("values");
        assertEquals(3, valuesNode.size());
        assertEquals("test", valuesNode.get("input1").asText());
        assertEquals("2016-01-01", valuesNode.get("date1").asText());
        assertEquals("2017-01-01", valuesNode.get("date2").asText());
        assertEquals("date", formNode.get("flowable_form_outcome").asText());
    }

    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form")
    public void saveSimpleForm() throws Exception {
        String taskId = "123456";
        FormModel formModel = repositoryService.getFormModelByKey("form1");

        Map<String, Object> valuesMap = new HashMap<>();
        valuesMap.put("input1", "test");
        Map<String, Object> formValues = formService.getVariablesFromFormSubmission(formModel, valuesMap, "default");

        FormInstance formInstance = formService.saveFormInstance(formValues, formModel, taskId, null, null);
        assertEquals(formModel.getId(), formInstance.getFormDefinitionId());
        JsonNode formNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
        assertEquals("test", formNode.get("values").get("input1").asText());
        assertEquals("default", formNode.get("flowable_form_outcome").asText());

        FormInstanceModel formInstanceModel = formService.getFormInstanceModelById(formInstance.getId(), null);
        assertEquals("form1", formInstanceModel.getKey());
        assertEquals(1, formInstanceModel.getFields().size());
        FormField formField = formInstanceModel.getFields().get(0);
        assertEquals("input1", formField.getId());
        assertEquals("test", formField.getValue());

        valuesMap = new HashMap<>();
        valuesMap.put("input1", "updatedValue");
        formValues = formService.getVariablesFromFormSubmission(formModel, valuesMap, "updatedOutcome");

        formInstance = formService.saveFormInstance(formValues, formModel, taskId, null, null);
        assertEquals(formModel.getId(), formInstance.getFormDefinitionId());
        formNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
        assertEquals("updatedValue", formNode.get("values").get("input1").asText());
        assertEquals("updatedOutcome", formNode.get("flowable_form_outcome").asText());

        formInstanceModel = formService.getFormInstanceModelById(formInstance.getId(), null);
        assertEquals("form1", formInstanceModel.getKey());
        assertEquals(1, formInstanceModel.getFields().size());
        formField = formInstanceModel.getFields().get(0);
        assertEquals("input1", formField.getId());
        assertEquals("updatedValue", formField.getValue());

        assertEquals(1, formService.createFormInstanceQuery().formDefinitionId(formModel.getId()).count());
    }
    
    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/options.form")
    public void optionsExpression() throws Exception {
        String taskId = "123456";
        Map<String, Object> variables = new HashMap<>();

        // test form model has expression and no options
        FormModel formModel = repositoryService.getFormModelByKey("optionsForm");
        OptionFormField optionFormField = (OptionFormField) formModel.getFields().get(0);
        assertEquals("${optionsVariable}", optionFormField.getOptionsExpression());
        assertNull(optionFormField.getOptions());
        
        String expectedJson = "[{\"id\":\"opt0\",\"name\":\"Opt0\"},{\"id\":\"opt1\",\"name\":\"Opt1\"},{\"id\":\"opt2\",\"name\":\"Opt2\"}]";
        List<Option> expectedOptions = new ArrayList<Option>();
        for (int i = 0; i < 3; i++) {
            Option option = new Option("Opt" + i);
            option.setId("opt" + i);
            expectedOptions.add(option);
        }
        

        // test form model options from List<Option> variable
        variables.put("optionsVariable", expectedOptions);
        formModel = formService.getFormModelWithVariablesByKey("optionsForm", taskId, variables);
        optionFormField = (OptionFormField) formModel.getFields().get(0);
        assertOptions(optionFormField);
        
        // test form model options from json variable
        variables.clear();
        variables.put("optionsVariable", expectedJson);
        formModel = formService.getFormModelWithVariablesByKey("optionsForm", taskId, variables);
        optionFormField = (OptionFormField) formModel.getFields().get(0);
        assertOptions(optionFormField);

        
        // test form instance from List<Option> variable 
        variables.clear();
        variables.put("dynamicDropDown", "Opt2");
        variables.put("optionsVariable", expectedOptions);
        // get clean form model
        formModel = repositoryService.getFormModelByKey("optionsForm");
        FormInstance formInstance = formService.saveFormInstance(variables, formModel, taskId, null, null);
        FormInstanceModel formInstanceModel = formService.getFormInstanceModelById(formInstance.getId(), variables);
        optionFormField = (OptionFormField) formInstanceModel.getFields().get(0);
        assertOptions(optionFormField);
        assertEquals("Opt2", optionFormField.getValue());
        
        
        // test form instance from json variable
        variables.clear();
        variables.put("dynamicDropDown", "Opt2");
        variables.put("optionsVariable", expectedJson);
        // get clean form model
        formModel = repositoryService.getFormModelByKey("optionsForm");
        formInstance = formService.saveFormInstance(variables, formModel, taskId, null, null);
        formInstanceModel = formService.getFormInstanceModelById(formInstance.getId(), variables);
        optionFormField = (OptionFormField) formInstanceModel.getFields().get(0);
        assertOptions(optionFormField);
        assertEquals("Opt2", optionFormField.getValue());

        // test expression failure on model
        variables.clear();
        try {
            formModel = formService.getFormModelWithVariablesByKey("optionsForm", taskId, variables);
            fail("Expression failure should result in a FlowableException");
        } catch (FlowableException e) {
        }

        // test expression failure on instance
        variables.clear();
        try {
            formModel = repositoryService.getFormModelByKey("optionsForm");
            formInstance = formService.saveFormInstance(variables, formModel, taskId, null, null);
        } catch (FlowableException e) {
        }

    }
    
    private void assertOptions(OptionFormField optionFormField) {
        List<Option> actualOptions = optionFormField.getOptions();
        for (int i = 0; i < actualOptions.size(); i++) {
            Option option = actualOptions.get(i);
            assertEquals(option.getId(), "opt" + i);    
            assertEquals(option.getName(), "Opt" + i);
        }
    }

}
