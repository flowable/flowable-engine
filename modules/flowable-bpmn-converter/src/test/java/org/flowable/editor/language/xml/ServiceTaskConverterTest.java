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
import static org.assertj.core.api.Assertions.tuple;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class ServiceTaskConverterTest {

    @BpmnXmlConverterTest("servicetaskmodel.bpmn")
    void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("servicetask");
        assertThat(flowElement)
                .isInstanceOfSatisfying(ServiceTask.class, serviceTask -> {
                    assertThat(serviceTask.getId()).isEqualTo("servicetask");
                    assertThat(serviceTask.getName()).isEqualTo("Service task");
                    assertThat(serviceTask.getFieldExtensions())
                            .extracting(FieldExtension::getFieldName, FieldExtension::getStringValue, FieldExtension::getExpression)
                            .containsExactly(
                                    tuple("testField", "test", null),
                                    tuple("testField2", null, "${test}")
                            );
                    assertThat(serviceTask.getMapExceptions())
                            .extracting(MapExceptionEntry::getErrorCode, MapExceptionEntry::getClassName, MapExceptionEntry::isAndChildren)
                            .containsExactly(
                                    tuple("java", "", false),
                                    tuple("java", "MyClass", true)
                            );
                    assertThat(serviceTask.getExecutionListeners())
                            .extracting(FlowableListener::getImplementationType, FlowableListener::getImplementation, FlowableListener::getEvent)
                            .containsExactly(
                                    tuple(ImplementationType.IMPLEMENTATION_TYPE_CLASS, "org.test.TestClass", "start"),
                                    tuple(ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION, "${testExpression}", "end"),
                                    tuple(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION, "${delegateExpression}", "start")
                            );
                });
    }
}
