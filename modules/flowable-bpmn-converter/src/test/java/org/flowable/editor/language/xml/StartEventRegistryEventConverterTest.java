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
import static org.assertj.core.groups.Tuple.tuple;
import static org.flowable.bpmn.constants.BpmnXMLConstants.ELEMENT_EVENT_CORRELATION_PARAMETER;
import static org.flowable.bpmn.constants.BpmnXMLConstants.ELEMENT_EVENT_OUT_PARAMETER;
import static org.flowable.bpmn.constants.BpmnXMLConstants.ELEMENT_EVENT_TYPE;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

/**
 * @author Bas Claessen
 */
class StartEventRegistryEventConverterTest {

    @BpmnXmlConverterTest("startEventRegistryEvent.bpmn")
    void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("start");
        assertThat(flowElement).isInstanceOfSatisfying(StartEvent.class, start -> {
            assertThat(start.getId()).isEqualTo("start");
            assertThat(start.isInterrupting()).isEqualTo(true);
            assertThat(start.getExtensionElements().get(ELEMENT_EVENT_TYPE)).extracting(ExtensionElement::getElementText)
                    .containsExactly("eventType1");
            assertThat(start.getExtensionElements().get("eventName")).extracting(ExtensionElement::getElementText)
                    .containsExactly("eventName1");
            assertThat(start.getExtensionElements().get(ELEMENT_EVENT_OUT_PARAMETER)).extracting(
                            extensionElement -> extensionElement.getAttributes().size(),
                            extensionElement -> extensionElement.getAttributeValue(null, "source"),
                            extensionElement -> extensionElement.getAttributeValue(null, "sourceType"),
                            extensionElement -> extensionElement.getAttributeValue(null, "target")
                    )
                    .containsExactly(
                            tuple(3, "source1", "string", "target1"),
                            tuple(3, "source2", "string", "target2"));
            assertThat(start.getExtensionElements().get(ELEMENT_EVENT_CORRELATION_PARAMETER)).extracting(
                            extensionElement -> extensionElement.getAttributes().size(),
                            extensionElement -> extensionElement.getAttributeValue(null, "name"),
                            extensionElement -> extensionElement.getAttributeValue(null, "type"),
                            extensionElement -> extensionElement.getAttributeValue(null, "value")
                    )
                    .containsExactly(
                            tuple(3, "name1", "string", "value1"),
                            tuple(3, "name2", "string", "value2"));

        });

        flowElement = model.getMainProcess().getFlowElement("subProcessStart", true);
        assertThat(flowElement).isInstanceOfSatisfying(StartEvent.class, start -> {
            assertThat(start.getId()).isEqualTo("subProcessStart");
            assertThat(start.isInterrupting()).isEqualTo(false);
            assertThat(start.getExtensionElements()).containsOnlyKeys(ELEMENT_EVENT_TYPE);
            assertThat(start.getExtensionElements().get(ELEMENT_EVENT_TYPE)).extracting(ExtensionElement::getElementText)
                    .containsExactly("eventType2");
        });
    }
}