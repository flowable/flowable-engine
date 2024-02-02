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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExclusiveGateway;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.IntermediateCatchEvent;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.bpmn.model.UserTask;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class SimpleConverterTest {

    @BpmnXmlConverterTest("simplemodel.bpmn")
    void validateModel(BpmnModel model) {
        assertThat(model.getDefinitionsAttributes()).hasSize(2);
        assertThat(model.getDefinitionsAttributeValue("http://flowable.org/modeler", "version")).isEqualTo("2.2A");
        assertThat(model.getDefinitionsAttributeValue("http://flowable.org/modeler", "exportDate")).isEqualTo("20140312T10:45:23");

        assertThat(model.getMainProcess().getId()).isEqualTo("simpleProcess");
        assertThat(model.getMainProcess().getName()).isEqualTo("Simple process");
        assertThat(model.getMainProcess().getDocumentation()).isEqualTo("simple doc");
        assertThat(model.getMainProcess().isExecutable()).isTrue();

        FlowElement flowElement = model.getMainProcess().getFlowElement("flow1");
        assertThat(flowElement)
                .isInstanceOfSatisfying(SequenceFlow.class, sequenceFlow -> {
                    assertThat(sequenceFlow.getId()).isEqualTo("flow1");
                });

        flowElement = model.getMainProcess().getFlowElement("catchEvent");
        assertThat(flowElement)
                .isInstanceOfSatisfying(IntermediateCatchEvent.class, intermediateCatchEvent -> {
                    assertThat(intermediateCatchEvent.getId()).isEqualTo("catchEvent");
                    assertThat(intermediateCatchEvent.getEventDefinitions()).hasSize(1);
                    assertThat(intermediateCatchEvent.getEventDefinitions().get(0))
                            .isInstanceOfSatisfying(TimerEventDefinition.class, timerEventDefinition -> {
                                assertThat(timerEventDefinition.getTimeDuration()).isEqualTo("PT5M");
                            });
                });

        flowElement = model.getMainProcess().getFlowElement("userTask1");
        assertThat(flowElement)
                .isInstanceOfSatisfying(UserTask.class, userTask -> {
                    assertThat(userTask.getDocumentation()).isEqualTo("task doc");
                });

        flowElement = model.getMainProcess().getFlowElement("flow1Condition");
        assertThat(flowElement)
                .isInstanceOfSatisfying(SequenceFlow.class, sequenceFlow -> {
                    assertThat(sequenceFlow.getId()).isEqualTo("flow1Condition");
                    assertThat(sequenceFlow.getConditionExpression()).isEqualTo("${number <= 1}");
                });

        flowElement = model.getMainProcess().getFlowElement("gateway1");
        assertThat(flowElement).isInstanceOf(ExclusiveGateway.class);
    }
}
