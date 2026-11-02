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
import static org.flowable.bpmn.constants.BpmnXMLConstants.ELEMENT_EVENT_VARIABLELISTENERDEFINITION;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class VariableListenerEventDefinitionParserTest {

    @BpmnXmlConverterTest("missingVariableListenerVariableName.bpmn")
    void validateModel(BpmnModel model) {

        FlowElement flowElement = model.getMainProcess().getFlowElement("eventSubProcess");
        assertThat(flowElement).isInstanceOf(EventSubProcess.class);
        EventSubProcess subProcess = (EventSubProcess) flowElement;
        assertThat(subProcess.getId()).isEqualTo("eventSubProcess");
        flowElement = subProcess.getFlowElement("variableListenerStartEvent");
        assertThat(flowElement).isInstanceOf(StartEvent.class);
        StartEvent startEvent = (StartEvent) flowElement;
        assertThat(startEvent.getId()).isEqualTo("variableListenerStartEvent");
        assertThat(startEvent.getExtensionElements().get(ELEMENT_EVENT_VARIABLELISTENERDEFINITION)).isNull();
    }
}
