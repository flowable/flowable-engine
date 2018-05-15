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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormInstance;
import org.flowable.form.api.FormInstanceInfo;
import org.flowable.form.model.Option;
import org.flowable.form.model.OptionFormField;
import org.flowable.form.model.SimpleFormModel;
import org.junit.Test;

public class OptionFormFieldTest extends AbstractFlowableFormTest {

    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/options.form")
    public void optionsExpression() throws Exception {
        String taskId = "123456";
        Map<String, Object> variables = new HashMap<>();

        // test form model has expression and no options
        FormInfo formInfo = repositoryService.getFormModelByKey("optionsForm");
        SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
        
        OptionFormField optionFormField = (OptionFormField) formModel.getFields().get(0);
        assertEquals("${optionsVariable}", optionFormField.getOptionsExpression());
        assertNull(optionFormField.getOptions());
        
        String expectedJson = "[{\"id\":\"opt0\",\"name\":\"Opt0\"},{\"id\":\"opt1\",\"name\":\"Opt1\"},{\"id\":\"opt2\",\"name\":\"Opt2\"}]";
        List<Option> expectedOptions = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Option option = new Option("Opt" + i);
            option.setId("opt" + i);
            expectedOptions.add(option);
        }
        

        // test form model options from List<Option> variable
        variables.put("optionsVariable", expectedOptions);
        formModel = getOptionsFormModelWithVariablesByKey(variables);
        optionFormField = (OptionFormField) formModel.getFields().get(0);
        assertOptions(optionFormField);
        
        // test form model options from json variable
        variables.clear();
        variables.put("optionsVariable", expectedJson);
        formModel = getOptionsFormModelWithVariablesByKey(variables);
        optionFormField = (OptionFormField) formModel.getFields().get(0);
        assertOptions(optionFormField);

        
        // test form instance from List<Option> variable 
        variables.clear();
        variables.put("dynamicDropDown", "Opt2");
        variables.put("optionsVariable", expectedOptions);
        // get clean form model
        formInfo = repositoryService.getFormModelByKey("optionsForm");
        formModel = (SimpleFormModel) formInfo.getFormModel();
        FormInstance formInstance = formService.saveFormInstance(variables, formInfo, taskId, null, null);
        FormInstanceInfo formInstanceInfo = formService.getFormInstanceModelById(formInstance.getId(), variables);
        SimpleFormModel formInstanceModel = (SimpleFormModel) formInstanceInfo.getFormModel(); 
        optionFormField = (OptionFormField) formInstanceModel.getFields().get(0);
        assertOptions(optionFormField);
        assertEquals("Opt2", optionFormField.getValue());
        
        
        // test form instance from json variable
        variables.clear();
        variables.put("dynamicDropDown", "Opt2");
        variables.put("optionsVariable", expectedJson);
        
        // get clean form model
        formInfo = repositoryService.getFormModelByKey("optionsForm");
        formModel = (SimpleFormModel) formInfo.getFormModel();
        formInstance = formService.saveFormInstance(variables, formInfo, taskId, null, null);
        formInstanceInfo = formService.getFormInstanceModelById(formInstance.getId(), variables);
        formInstanceModel = (SimpleFormModel) formInstanceInfo.getFormModel(); 
        
        optionFormField = (OptionFormField) formInstanceModel.getFields().get(0);
        assertOptions(optionFormField);
        assertEquals("Opt2", optionFormField.getValue());

        // test expression failure on model
        variables.clear();
        try {
            formModel = getOptionsFormModelWithVariablesByKey(variables);
            fail("Expression failure should result in a FlowableException");
        } catch (FlowableException e) {
        }

        // test expression failure on instance
        variables.clear();
        try {
            formInfo = repositoryService.getFormModelByKey("optionsForm");
            formInstance = formService.saveFormInstance(variables, formInfo, taskId, null, null);
        } catch (FlowableException e) {
        }

    }

    protected SimpleFormModel getOptionsFormModelByKey() {
        FormInfo formInfo = repositoryService.getFormModelByKey("optionsForm");
        SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
        return formModel;
    }

    protected SimpleFormModel getOptionsFormModelWithVariablesByKey(Map<String, Object> variables) {
        String taskId = null;
        FormInfo formInfo = formService.getFormModelWithVariablesByKey("optionsForm", taskId, variables);
        SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
        return formModel;
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
