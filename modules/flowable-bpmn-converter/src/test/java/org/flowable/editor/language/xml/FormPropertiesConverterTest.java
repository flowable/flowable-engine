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
package org.flowable.editor.language.xml;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FormProperty;
import org.flowable.bpmn.model.FormValue;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class FormPropertiesConverterTest {

    @BpmnXmlConverterTest("formPropertiesProcess.bpmn")
    void validateModel(BpmnModel model) {
        assertThat(model.getMainProcess().getId()).isEqualTo("formPropertiesProcess");
        assertThat(model.getMainProcess().getName()).isEqualTo("User registration");
        assertThat(model.getMainProcess().isExecutable()).isTrue();

        FlowElement startFlowElement = model.getMainProcess().getFlowElement("startNode");
        assertThat(startFlowElement).isInstanceOf(StartEvent.class);
        StartEvent startEvent = (StartEvent) startFlowElement;

        for (FormProperty formProperty : startEvent.getFormProperties()) {
            assertThat(formProperty.isRequired()).isTrue();
        }

        FlowElement userFlowElement = model.getMainProcess().getFlowElement("userTask");
        assertThat(userFlowElement).isInstanceOf(UserTask.class);
        UserTask userTask = (UserTask) userFlowElement;

        List<FormProperty> formProperties = userTask.getFormProperties();

        assertThat(formProperties).as("Invalid form properties list: ").hasSize(8);

        for (FormProperty formProperty : formProperties) {
            if ("new_property_1".equals(formProperty.getId())) {
                checkFormProperty(formProperty, false, false, false);
            } else if ("new_property_2".equals(formProperty.getId())) {
                checkFormProperty(formProperty, false, false, true);
            } else if ("new_property_3".equals(formProperty.getId())) {
                checkFormProperty(formProperty, false, true, false);
            } else if ("new_property_4".equals(formProperty.getId())) {
                checkFormProperty(formProperty, false, true, true);
            } else if ("new_property_5".equals(formProperty.getId())) {
                checkFormProperty(formProperty, true, false, false);

                List<Map<String, Object>> formValues = new ArrayList<>();
                for (FormValue formValue : formProperty.getFormValues()) {
                    Map<String, Object> formValueMap = new HashMap<>();
                    formValueMap.put("id", formValue.getId());
                    formValueMap.put("name", formValue.getName());
                    formValues.add(formValueMap);
                }
                checkFormPropertyFormValues(formValues);

            } else if ("new_property_6".equals(formProperty.getId())) {
                checkFormProperty(formProperty, true, false, true);
            } else if ("new_property_7".equals(formProperty.getId())) {
                checkFormProperty(formProperty, true, true, false);
            } else if ("new_property_8".equals(formProperty.getId())) {
                checkFormProperty(formProperty, true, true, true);
            }
        }

    }

    private void checkFormProperty(FormProperty formProperty, boolean shouldBeRequired, boolean shouldBeReadable, boolean shouldBeWritable) {
        assertThat(formProperty.isRequired()).isEqualTo(shouldBeRequired);
        assertThat(formProperty.isReadable()).isEqualTo(shouldBeReadable);
        assertThat(formProperty.isWriteable()).isEqualTo(shouldBeWritable);
    }

    private void checkFormPropertyFormValues(List<Map<String, Object>> formValues) {
        List<Map<String, Object>> expectedFormValues = new ArrayList<>();
        Map<String, Object> formValue1 = new HashMap<>();
        formValue1.put("id", "value1");
        formValue1.put("name", "Value 1");
        Map<String, Object> formValue2 = new HashMap<>();
        formValue2.put("id", "value2");
        formValue2.put("name", "Value 2");

        Map<String, Object> formValue3 = new HashMap<>();
        formValue3.put("id", "value3");
        formValue3.put("name", "Value 3");

        Map<String, Object> formValue4 = new HashMap<>();
        formValue4.put("id", "value4");
        formValue4.put("name", "Value 4");

        expectedFormValues.add(formValue1);
        expectedFormValues.add(formValue2);
        expectedFormValues.add(formValue3);
        expectedFormValues.add(formValue4);

        assertThat(formValues).isEqualTo(expectedFormValues);
    }
}
