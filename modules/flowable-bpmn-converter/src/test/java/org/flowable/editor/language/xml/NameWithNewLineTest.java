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
import static org.flowable.bpmn.constants.BpmnXMLConstants.ATTRIBUTE_ELEMENT_NAME;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class NameWithNewLineTest {

    @BpmnXmlConverterTest("nameWithNewLineTestProcess.bpmn")
    void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("startnoneevent1");
        assertThat(flowElement.getName()).isEqualTo("start\nevent");
        assertThat(flowElement.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();

        flowElement = model.getMainProcess().getFlowElement("bpmnCatchEvent_12");
        assertThat(flowElement.getName()).isEqualTo("intermediate\nevent");
        assertThat(flowElement.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();

        flowElement = model.getMainProcess().getFlowElement("bpmnGateway_14");
        assertThat(flowElement.getName()).isEqualTo("gate\nway");
        assertThat(flowElement.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();

        flowElement = model.getMainProcess().getFlowElement("bpmnEndEvent_3");
        assertThat(flowElement.getName()).isEqualTo("end\nevent");
        assertThat(flowElement.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();

        SubProcess subProcess = (SubProcess) model.getMainProcess().getFlowElement("bpmnStructure_1");
        assertThat(subProcess.getName()).isEqualTo("sub\nprocess");
        assertThat(flowElement.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();

        flowElement = subProcess.getFlowElement("bpmnTask_5");
        assertThat(flowElement.getName()).isEqualTo("user\ntask");
        assertThat(flowElement.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();

        flowElement = subProcess.getFlowElement("bpmnBoundaryEvent_10");
        assertThat(flowElement.getName()).isEqualTo("boundary\nevent");
        assertThat(flowElement.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();
    }

    @BpmnXmlConverterTest("nameWithoutNewLineTestProcess.bpmn")
    void validateModelWithoutNewLines(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("startnoneevent1");
        assertThat(flowElement.getName()).isEqualTo("startevent");
        assertThat(flowElement.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();

        flowElement = model.getMainProcess().getFlowElement("bpmnCatchEvent_12");
        assertThat(flowElement.getName()).isEqualTo("intermediateevent");
        assertThat(flowElement.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();

        flowElement = model.getMainProcess().getFlowElement("bpmnGateway_14");
        assertThat(flowElement.getName()).isEqualTo("gateway");
        assertThat(flowElement.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();

        flowElement = model.getMainProcess().getFlowElement("bpmnEndEvent_3");
        assertThat(flowElement.getName()).isEqualTo("endevent");
        assertThat(flowElement.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();

        SubProcess subProcess = (SubProcess) model.getMainProcess().getFlowElement("bpmnStructure_1");
        assertThat(subProcess.getName()).isEqualTo("subprocess");
        assertThat(subProcess.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();

        flowElement = subProcess.getFlowElement("bpmnTask_5");
        assertThat(flowElement.getName()).isEqualTo("usertask");
        assertThat(flowElement.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();

        flowElement = subProcess.getFlowElement("bpmnBoundaryEvent_10");
        assertThat(flowElement.getName()).isEqualTo("boundaryevent");
        assertThat(flowElement.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();
    }
}
