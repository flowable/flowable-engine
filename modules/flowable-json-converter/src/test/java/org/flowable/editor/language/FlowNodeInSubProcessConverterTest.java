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

import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.SubProcess;
import org.junit.jupiter.api.Test;

public class FlowNodeInSubProcessConverterTest extends AbstractConverterTest {

    @Test
    public void doubleConversionValidation() throws Exception {
        BpmnModel bpmnModel = readJsonFile();
        validateModel(bpmnModel);
        bpmnModel = convertToJsonAndBack(bpmnModel);
        validateModel(bpmnModel);
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("subprocess1", true);
        assertThat(flowElement).isInstanceOf(SubProcess.class);
        SubProcess subProcess = (SubProcess) flowElement;
        ParallelGateway gateway = (ParallelGateway) subProcess.getFlowElement("sid-A0E0B174-36DF-4C4F-A952-311CC3C031FC");
        assertThat(gateway).isNotNull();
        List<SequenceFlow> sequenceFlows = gateway.getOutgoingFlows();
        assertThat(sequenceFlows)
                .extracting(SequenceFlow::getId)
                .containsExactlyInAnyOrder("sid-9C669980-C274-4A48-BF7F-B9C5CA577DD2", "sid-A299B987-396F-46CA-8D63-85991FBFCE6E");
        assertThat(sequenceFlows)
                .extracting(SequenceFlow::getSourceRef)
                .containsExactly("sid-A0E0B174-36DF-4C4F-A952-311CC3C031FC", "sid-A0E0B174-36DF-4C4F-A952-311CC3C031FC");
    }

    @Override
    protected String getResource() {
        return "test.flownodeinsubprocessmodel.json";
    }

}
