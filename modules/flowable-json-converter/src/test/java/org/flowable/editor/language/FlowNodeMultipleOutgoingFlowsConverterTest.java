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
import org.junit.jupiter.api.Test;

public class FlowNodeMultipleOutgoingFlowsConverterTest extends AbstractConverterTest {

    @Test
    public void doubleConversionValidation() throws Exception {
        BpmnModel bpmnModel = readJsonFile();
        validateModel(bpmnModel);
        bpmnModel = convertToJsonAndBack(bpmnModel);
        validateModel(bpmnModel);
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("parallel1", true);
        assertThat(flowElement).isInstanceOf(ParallelGateway.class);
        ParallelGateway gateway = (ParallelGateway) flowElement;
        List<SequenceFlow> sequenceFlows = gateway.getOutgoingFlows();
        assertThat(sequenceFlows)
                .extracting(SequenceFlow::getId)
                .containsExactlyInAnyOrder(
                        "sid-B9EE4ECE-BF72-4C25-B768-8295906E5CF8",
                        "sid-D2491B73-0382-4EC2-AAAC-C8FD129E4CBE",
                        "sid-7036D56C-E8EF-493B-ADEC-57EED4C6CE1F"
                );
        assertThat(sequenceFlows)
                .extracting(SequenceFlow::getSourceRef)
                .containsExactly("parallel1", "parallel1", "parallel1");
        flowElement = model.getMainProcess().getFlowElement("parallel2", true);
        assertThat(flowElement).isInstanceOf(ParallelGateway.class);
        gateway = (ParallelGateway) flowElement;
        sequenceFlows = gateway.getIncomingFlows();
        assertThat(sequenceFlows)
                .extracting(SequenceFlow::getId)
                .containsExactlyInAnyOrder(
                        "sid-4C19E041-42FA-485D-9D09-D47CCD9DB270",
                        "sid-05A991A6-0296-4867-ACBA-EF9EEC68FB8A",
                        "sid-C546AC84-379D-4094-9DC3-548593F2EA0D"
                );
        assertThat(sequenceFlows)
                .extracting(SequenceFlow::getTargetRef)
                .containsExactly("parallel2", "parallel2", "parallel2");
    }

    @Override
    protected String getResource() {
        return "test.flownodemultipleoutgoingflowsmodel.json";
    }

}
