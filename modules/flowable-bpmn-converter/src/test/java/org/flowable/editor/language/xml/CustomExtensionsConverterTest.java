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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionAttribute;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class CustomExtensionsConverterTest {

    @BpmnXmlConverterTest("customextensionsmodel.bpmn")
    void validateModel(BpmnModel model) {
        Process process = model.getMainProcess();
        assertThat(process.getAttributes()).hasSize(1);
        List<ExtensionAttribute> attributes = process.getAttributes().get("version");
        assertThat(attributes).isNotNull();
        // custom:version = "9"
        assertThat(attributes)
                .extracting(ExtensionAttribute::getNamespace, ExtensionAttribute::getNamespacePrefix, ExtensionAttribute::getName, ExtensionAttribute::getValue)
                .containsExactly(tuple("http://custom.org/bpmn", "custom", "version", "9"));

        List<FlowableListener> listeners = model.getMainProcess().getExecutionListeners();
        validateExecutionListeners(listeners);
        Map<String, List<ExtensionElement>> extensionElementMap = model.getMainProcess().getExtensionElements();
        validateExtensionElements(extensionElementMap);

        FlowElement flowElement = model.getMainProcess().getFlowElement("servicetask");
        assertThat(flowElement).isInstanceOf(ServiceTask.class);
        assertThat(flowElement.getId()).isEqualTo("servicetask");
        ServiceTask serviceTask = (ServiceTask) flowElement;
        assertThat(serviceTask.getId()).isEqualTo("servicetask");
        assertThat(serviceTask.getName()).isEqualTo("Service task");

        List<FieldExtension> fields = serviceTask.getFieldExtensions();
        assertThat(fields)
                .extracting(FieldExtension::getFieldName, FieldExtension::getStringValue, FieldExtension::getExpression)
                .containsExactly(
                        tuple("testField", "test", null),
                        tuple("testField2", null, "${test}")
                );

        listeners = serviceTask.getExecutionListeners();
        validateExecutionListeners(listeners);

        extensionElementMap = serviceTask.getExtensionElements();
        validateExtensionElements(extensionElementMap);

        assertThat(serviceTask.getBoundaryEvents()).hasSize(1);
        BoundaryEvent boundaryEvent = serviceTask.getBoundaryEvents().get(0);
        assertThat(boundaryEvent.getId()).isEqualTo("timerEvent");
        assertThat(boundaryEvent.getEventDefinitions()).hasSize(1);
        assertThat(boundaryEvent.getEventDefinitions().get(0)).isInstanceOf(TimerEventDefinition.class);
        extensionElementMap = boundaryEvent.getEventDefinitions().get(0).getExtensionElements();
        validateExtensionElements(extensionElementMap);
    }

    protected void validateExecutionListeners(List<FlowableListener> listeners) {
        assertThat(listeners)
                .extracting(FlowableListener::getImplementationType, FlowableListener::getImplementation, FlowableListener::getEvent,
                        FlowableListener::getOnTransaction, FlowableListener::getCustomPropertiesResolverImplementation)
                .containsExactly(
                        tuple(ImplementationType.IMPLEMENTATION_TYPE_CLASS, "org.test.TestClass", "start", "before-commit", "org.test.TestResolverClass"),
                        tuple(ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION, "${testExpression}", "end", "committed", "${testResolverExpression}"),
                        tuple(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION, "${delegateExpression}", "start", "rolled-back",
                                "${delegateResolverExpression}")
                );
    }

    protected void validateExtensionElements(Map<String, List<ExtensionElement>> extensionElementMap) {
        assertThat(extensionElementMap).hasSize(1);

        List<ExtensionElement> extensionElements = extensionElementMap.get("test");
        assertThat(extensionElements)
                .extracting(ExtensionElement::getName, ExtensionElement::getNamespacePrefix, ExtensionElement::getNamespace)
                .containsExactly(
                        tuple("test", "custom", "http://custom.org/bpmn"),
                        tuple("test", "custom", "http://custom.org/bpmn")
                );
        ExtensionElement extensionElement = extensionElements.get(0);
        assertThat(extensionElement.getAttributes()).hasSize(2);

        List<ExtensionAttribute> attributes = extensionElement.getAttributes().get("id");
        assertThat(attributes)
                .extracting(ExtensionAttribute::getName, ExtensionAttribute::getValue, ExtensionAttribute::getNamespace, ExtensionAttribute::getNamespacePrefix)
                .containsExactly(tuple("id", "test", null, null));

        attributes = extensionElement.getAttributes().get("name");
        assertThat(attributes)
                .extracting(ExtensionAttribute::getName, ExtensionAttribute::getValue, ExtensionAttribute::getNamespace, ExtensionAttribute::getNamespacePrefix)
                .containsExactly(tuple("name", "test", null, null));

        assertThat(extensionElement.getChildElements()).hasSize(2);
        List<ExtensionElement> childExtensions = extensionElement.getChildElements().get("name");
        assertThat(childExtensions).hasSize(2);

        ExtensionElement childExtension = childExtensions.get(0);
        assertThat(childExtension).isNotNull();
        assertThat(childExtension.getName()).isEqualTo("name");
        assertThat(childExtension.getNamespacePrefix()).isEqualTo("custom");
        assertThat(childExtension.getNamespace()).isEqualTo("http://custom.org/bpmn");
        assertThat(childExtension.getAttributes()).isEmpty();
        assertThat(childExtension.getChildElements()).hasSize(1);

        List<ExtensionElement> subChildExtensions = childExtension.getChildElements().get("test");
        Map<String, List<ExtensionAttribute>> emptyMap = new HashMap<>();
        assertThat(subChildExtensions)
                .extracting(ExtensionElement::getName, ExtensionElement::getNamespacePrefix, ExtensionElement::getNamespace, ExtensionElement::getAttributes,
                        ExtensionElement::getChildElements, ExtensionElement::getElementText)
                .containsExactly(tuple("test", "custom", "http://custom.org/bpmn", emptyMap, emptyMap, "test"));

        childExtensions = extensionElement.getChildElements().get("description");
        assertThat(childExtensions).hasSize(1);
        childExtension = childExtensions.get(0);
        assertThat(childExtension).isNotNull();
        assertThat(childExtension.getName()).isEqualTo("description");
        assertThat(childExtension.getAttributes()).hasSize(1);
        attributes = childExtension.getAttributes().get("id");
        assertThat(attributes)
                .extracting(ExtensionAttribute::getName, ExtensionAttribute::getValue, ExtensionAttribute::getNamespace, ExtensionAttribute::getNamespacePrefix)
                .containsExactly(tuple("id", "test", "http://custom2.org/bpmn", "custom2"));

        extensionElement = extensionElements.get(1);
        assertThat(extensionElement.getAttributes()).hasSize(2);

        attributes = extensionElement.getAttributes().get("id");
        assertThat(attributes)
                .extracting(ExtensionAttribute::getName, ExtensionAttribute::getValue, ExtensionAttribute::getNamespace, ExtensionAttribute::getNamespacePrefix)
                .containsExactly(tuple("id", "test2", null, null));

        attributes = extensionElement.getAttributes().get("name");
        assertThat(attributes)
                .extracting(ExtensionAttribute::getName, ExtensionAttribute::getValue, ExtensionAttribute::getNamespace, ExtensionAttribute::getNamespacePrefix)
                .containsExactly(tuple("name", "test2", null, null));
    }
}
