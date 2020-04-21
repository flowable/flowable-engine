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
package org.flowable.editor.language;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.FormProperty;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.StartEvent;
import org.junit.jupiter.api.Test;

public class StartEventConverterTest extends AbstractConverterTest {

    @Test
    public void convertJsonToModel() throws Exception {
        BpmnModel bpmnModel = readJsonFile();
        validateModel(bpmnModel);
    }

    @Test
    public void doubleConversionValidation() throws Exception {
        BpmnModel bpmnModel = readJsonFile();
        bpmnModel = convertToJsonAndBack(bpmnModel);
        validateModel(bpmnModel);
    }

    @Override
    protected String getResource() {
        return "test.starteventmodel.json";
    }

    private void validateModel(BpmnModel model) {

        FlowElement flowElement = model.getMainProcess().getFlowElement("start", true);
        assertThat(flowElement).isInstanceOf(StartEvent.class);

        StartEvent startEvent = (StartEvent) flowElement;
        assertThat(startEvent.getId()).isEqualTo("start");
        assertThat(startEvent.getName()).isEqualTo("startName");
        assertThat(startEvent.getFormKey()).isEqualTo("startFormKey");
        assertThat(startEvent.isSameDeployment()).isTrue();
        assertThat(startEvent.getValidateFormFields()).isEqualTo("formFieldValidationValue");
        assertThat(startEvent.getInitiator()).isEqualTo("startInitiator");
        assertThat(startEvent.getDocumentation()).isEqualTo("startDoc");

        assertThat(startEvent.getExecutionListeners())
                .extracting(FlowableListener::getEvent, FlowableListener::getImplementation, FlowableListener::getImplementationType)
                .containsExactly(
                        tuple("start", "org.test.TestClass", ImplementationType.IMPLEMENTATION_TYPE_CLASS),
                        tuple("end", "${someExpression}", ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION)
                );

        List<FormProperty> formProperties = startEvent.getFormProperties();
        assertThat(formProperties)
                .extracting(FormProperty::getId, FormProperty::getName, FormProperty::getType)
                .containsExactly(
                        tuple("startFormProp1", "startFormProp1", "string"),
                        tuple("startFormProp2", "startFormProp2", "boolean")
                );
    }

}
