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
import static org.flowable.editor.language.xml.util.XmlTestUtils.exportAndReadXMLFile;
import static org.flowable.editor.language.xml.util.XmlTestUtils.readXMLFile;

import org.flowable.bpmn.BpmnAutoLayout;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.flowable.bpmn.model.ValuedDataObject;
import org.junit.jupiter.api.Test;

class SubProcessConverterAutoLayoutTest {

    @Test
    public void convertXMLToModel() throws Exception {
        BpmnModel bpmnModel = readXMLFile("subprocessmodel_autolayout.bpmn");
        validateModel(bpmnModel);
    }

    @Test
    public void convertModelToXML() throws Exception {
        BpmnModel bpmnModel = readXMLFile("subprocessmodel_autolayout.bpmn");

        // Add DI information to bpmn model
        BpmnAutoLayout bpmnAutoLayout = new BpmnAutoLayout(bpmnModel);
        bpmnAutoLayout.execute();

        BpmnModel parsedModel = exportAndReadXMLFile(bpmnModel);
        validateModel(parsedModel);
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("start1");
        assertThat(flowElement)
                .isInstanceOfSatisfying(StartEvent.class, startEvent -> {
                    assertThat(startEvent.getId()).isEqualTo("start1");
                });

        flowElement = model.getMainProcess().getFlowElement("userTask1");
        assertThat(flowElement)
                .isInstanceOfSatisfying(UserTask.class, userTask -> {
                    assertThat(userTask.getId()).isEqualTo("userTask1");
                    assertThat(userTask.getCandidateUsers()).hasSize(1);
                    assertThat(userTask.getCandidateGroups()).hasSize(1);
                });

        flowElement = model.getMainProcess().getFlowElement("subprocess1");
        assertThat(flowElement)
                .isInstanceOfSatisfying(SubProcess.class, subProcess -> {
                    assertThat(subProcess.getId()).isEqualTo("subprocess1");
                    assertThat(subProcess.getFlowElements()).hasSize(6);
                    assertThat(subProcess.getDataObjects())
                            .extracting(ValuedDataObject::getName, ValuedDataObject::getValue)
                            .containsExactly(tuple("SubTest", "Testing"));
                    assertThat(subProcess.getDataObjects().get(0).getItemSubjectRef().getStructureRef()).isEqualTo("xsd:string");
                });
    }
}
