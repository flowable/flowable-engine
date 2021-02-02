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

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.bpmn.model.UserTask;
import org.junit.jupiter.api.Test;

public class SubProcessConverterTest extends AbstractConverterTest {

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
        return "test.subprocessmodel.json";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("start1", true);
        assertThat(flowElement).isInstanceOf(StartEvent.class);
        assertThat(flowElement.getId()).isEqualTo("start1");

        flowElement = model.getMainProcess().getFlowElement("userTask1", true);
        assertThat(flowElement).isInstanceOf(UserTask.class);
        assertThat(flowElement.getId()).isEqualTo("userTask1");
        UserTask userTask = (UserTask) flowElement;
        assertThat(userTask.getCandidateUsers()).hasSize(1);
        assertThat(userTask.getCandidateGroups()).hasSize(1);
        assertThat(userTask.getFormProperties()).hasSize(2);

        flowElement = model.getMainProcess().getFlowElement("subprocess1", true);
        assertThat(flowElement).isInstanceOf(SubProcess.class);
        assertThat(flowElement.getId()).isEqualTo("subprocess1");
        SubProcess subProcess = (SubProcess) flowElement;
        assertThat(subProcess.getFlowElements()).hasSize(5);

        flowElement = model.getMainProcess().getFlowElement("boundaryEvent1", true);
        assertThat(flowElement).isInstanceOf(BoundaryEvent.class);
        assertThat(flowElement.getId()).isEqualTo("boundaryEvent1");
        BoundaryEvent boundaryEvent = (BoundaryEvent) flowElement;
        assertThat(boundaryEvent.getAttachedToRef()).isNotNull();
        assertThat(boundaryEvent.getAttachedToRef().getId()).isEqualTo("subprocess1");
        assertThat(boundaryEvent.getEventDefinitions()).hasSize(1);
        assertThat(boundaryEvent.getEventDefinitions().get(0)).isInstanceOf(TimerEventDefinition.class);
    }
}
