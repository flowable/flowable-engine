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

import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;
import org.flowable.editor.language.xml.util.ConversionDirection;

/**
 * @author Joram Barrez
 */
class ExtensionsConverterTest {

    @BpmnXmlConverterTest("extensions.bpmn20.xml")
    void validateModel(BpmnModel bpmnModel) {
        FlowElement flowElement = bpmnModel.getMainProcess().getFlowElement("theTask");
        List<ExtensionElement> extensionElements = flowElement.getExtensionElements().get("test");
        assertThat(extensionElements).hasSize(1);
    }

    // We are only converting one direction since the XML location changes when we do it both ways
    @BpmnXmlConverterTest(value = "extensionsXmlLocation.bpmn20.xml", directions = ConversionDirection.xmlToModel)
    void validateXmlLocations(BpmnModel bpmnModel) {
        FlowElement flowElement = bpmnModel.getMainProcess().getFlowElement("theTask");
        List<ExtensionElement> extensionElements = flowElement.getExtensionElements().get("test");
        assertThat(extensionElements).hasSize(1);
        ExtensionElement element = extensionElements.get(0);
        assertThat(element.getXmlRowNumber()).isEqualTo(9);
        assertThat(element.getXmlColumnNumber()).isEqualTo(43);

        extensionElements = flowElement.getExtensionElements().get("testValue");
        assertThat(extensionElements).hasSize(1);
        element = extensionElements.get(0);
        assertThat(element.getXmlRowNumber()).isEqualTo(10);
        assertThat(element.getXmlColumnNumber()).isEqualTo(50);
    }
}
