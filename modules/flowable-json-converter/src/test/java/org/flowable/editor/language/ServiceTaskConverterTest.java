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
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.bpmn.model.ServiceTask;
import org.junit.jupiter.api.Test;

public class ServiceTaskConverterTest extends AbstractConverterTest {

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
        return "test.servicetaskmodel.json";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("servicetask", true);
        assertThat(flowElement).isInstanceOf(ServiceTask.class);
        assertThat(flowElement.getId()).isEqualTo("servicetask");
        ServiceTask serviceTask = (ServiceTask) flowElement;
        assertThat(serviceTask.getId()).isEqualTo("servicetask");
        assertThat(serviceTask.getName()).isEqualTo("Service task");
        assertThat(serviceTask.getSkipExpression()).isEqualTo("${skipExpression}");

        List<FieldExtension> fields = serviceTask.getFieldExtensions();
        assertThat(fields)
                .extracting(FieldExtension::getFieldName, FieldExtension::getStringValue, FieldExtension::getExpression)
                .containsExactly(
                        tuple("testField", "test", null),
                        tuple("testField2", null, "${test}")
                );

        List<MapExceptionEntry> exceptions = serviceTask.getMapExceptions();
        assertThat(exceptions)
                .extracting(MapExceptionEntry::getErrorCode, MapExceptionEntry::getClassName, MapExceptionEntry::isAndChildren)
                .containsExactly(
                        tuple("java", null, false),
                        tuple("java", "MyClass", true)
                );

        List<FlowableListener> listeners = serviceTask.getExecutionListeners();
        assertThat(listeners)
                .extracting(FlowableListener::getImplementationType, FlowableListener::getImplementation, FlowableListener::getEvent)
                .containsExactly(
                        tuple(ImplementationType.IMPLEMENTATION_TYPE_CLASS, "org.test.TestClass", "start"),
                        tuple(ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION, "${testExpression}", "end"),
                        tuple(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION, "${delegateExpression}", "start")
                );
    }
}
