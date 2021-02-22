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
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.FormProperty;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.junit.jupiter.api.Test;

public class UserTaskConverterTest extends AbstractConverterTest {

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
        return "test.usertaskmodel.json";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("usertask", true);
        assertThat(flowElement).isInstanceOf(UserTask.class);
        assertThat(flowElement.getId()).isEqualTo("usertask");
        UserTask userTask = (UserTask) flowElement;
        assertThat(userTask.getId()).isEqualTo("usertask");
        assertThat(userTask.getName()).isEqualTo("User task");
        assertThat(userTask.getFormKey()).isEqualTo("testKey");
        assertThat(userTask.getValidateFormFields()).isEqualTo("formFieldValidationValue");
        assertThat(userTask.getPriority()).isEqualTo("40");
        assertThat(userTask.getDueDate()).isEqualTo("2012-11-01");
        assertThat(userTask.getBusinessCalendarName()).isEqualTo("myCalendarName");
        assertThat(userTask.getCategory()).isEqualTo("defaultCategory");
        assertThat(userTask.getSkipExpression()).isEqualTo("${skipExpression}");

        assertThat(userTask.getAssignee()).isEqualTo("kermit");

        assertThat(userTask.getCandidateUsers())
                .containsOnly("kermit", "fozzie");
        assertThat(userTask.getCandidateGroups())
                .containsOnly("management", "sales");

        List<FormProperty> formProperties = userTask.getFormProperties();
        assertThat(formProperties)
                .extracting(FormProperty::getId, FormProperty::getName, FormProperty::getType)
                .containsExactly(
                        tuple("formId", "formName", "string"),
                        tuple("formId2", "anotherName", "long")
                );
        FormProperty formProperty = formProperties.get(0);
        assertThat(formProperty.getVariable()).isEqualTo("variable");
        assertThat(formProperty.getExpression()).isEqualTo("${expression}");
        formProperty = formProperties.get(1);
        assertThat(formProperty.getVariable()).isBlank();   // one time null, next time ""
        assertThat(formProperty.getExpression()).isBlank();

        List<FlowableListener> listeners = userTask.getTaskListeners();
        assertThat(listeners)
                .extracting(FlowableListener::getImplementationType, FlowableListener::getImplementation, FlowableListener::getEvent)
                .containsExactly(
                        tuple(ImplementationType.IMPLEMENTATION_TYPE_CLASS, "org.test.TestClass", "create"),
                        tuple(ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION, "${someExpression}", "assignment"),
                        tuple(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION, "${someDelegateExpression}", "complete")
                );
        FlowableListener listener = listeners.get(0);
        assertThat(listener.getFieldExtensions())
                .extracting(FieldExtension::getFieldName, FieldExtension::getStringValue)
                .containsExactly(
                        tuple("testField", "test"),
                        tuple("testField2", null)
                );

        flowElement = model.getMainProcess().getFlowElement("start", true);
        assertThat(flowElement).isInstanceOf(StartEvent.class);

        StartEvent startEvent = (StartEvent) flowElement;
        assertThat(startEvent.getOutgoingFlows()).hasSize(1);

        flowElement = model.getMainProcess().getFlowElement("flow1", true);
        assertThat(flowElement).isInstanceOf(SequenceFlow.class);

        SequenceFlow flow = (SequenceFlow) flowElement;
        assertThat(flow.getId()).isEqualTo("flow1");
        assertThat(flow.getSourceRef()).isNotNull();
        assertThat(flow.getTargetRef()).isNotNull();
    }
}
