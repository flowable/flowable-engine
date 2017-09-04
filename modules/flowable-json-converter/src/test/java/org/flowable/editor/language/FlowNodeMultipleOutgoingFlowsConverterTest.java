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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.SequenceFlow;
import org.junit.Test;

public class FlowNodeMultipleOutgoingFlowsConverterTest extends AbstractConverterTest {

    @Test
    public void doubleConversionValidation() throws Exception {
        BpmnModel bpmnModel = readJsonFile();
        validateModel(bpmnModel);
        bpmnModel = convertToJsonAndBack(bpmnModel);
        // System.out.println("xml " + new String(new
        // BpmnXMLConverter().convertToXML(bpmnModel), "utf-8"));
        validateModel(bpmnModel);
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("parallel1", true);
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof ParallelGateway);
        ParallelGateway gateway = (ParallelGateway) flowElement;
        List<SequenceFlow> sequenceFlows = gateway.getOutgoingFlows();
        assertEquals(3, sequenceFlows.size());
        assertTrue(sequenceFlows.get(0).getId().equals("sid-B9EE4ECE-BF72-4C25-B768-8295906E5CF8") || sequenceFlows.get(0).getId().equals("sid-D2491B73-0382-4EC2-AAAC-C8FD129E4CBE")
                || sequenceFlows.get(0).getId().equals("sid-7036D56C-E8EF-493B-ADEC-57EED4C6CE1F"));
        assertTrue(sequenceFlows.get(1).getId().equals("sid-B9EE4ECE-BF72-4C25-B768-8295906E5CF8") || sequenceFlows.get(1).getId().equals("sid-D2491B73-0382-4EC2-AAAC-C8FD129E4CBE")
                || sequenceFlows.get(1).getId().equals("sid-7036D56C-E8EF-493B-ADEC-57EED4C6CE1F"));
        assertTrue(sequenceFlows.get(2).getId().equals("sid-B9EE4ECE-BF72-4C25-B768-8295906E5CF8") || sequenceFlows.get(2).getId().equals("sid-D2491B73-0382-4EC2-AAAC-C8FD129E4CBE")
                || sequenceFlows.get(2).getId().equals("sid-7036D56C-E8EF-493B-ADEC-57EED4C6CE1F"));
        assertEquals("parallel1", sequenceFlows.get(0).getSourceRef());
        assertEquals("parallel1", sequenceFlows.get(1).getSourceRef());
        assertEquals("parallel1", sequenceFlows.get(2).getSourceRef());
        flowElement = model.getMainProcess().getFlowElement("parallel2", true);
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof ParallelGateway);
        gateway = (ParallelGateway) flowElement;
        sequenceFlows = gateway.getIncomingFlows();
        assertEquals(3, sequenceFlows.size());
        assertTrue(sequenceFlows.get(0).getId().equals("sid-4C19E041-42FA-485D-9D09-D47CCD9DB270") || sequenceFlows.get(0).getId().equals("sid-05A991A6-0296-4867-ACBA-EF9EEC68FB8A")
                || sequenceFlows.get(0).getId().equals("sid-C546AC84-379D-4094-9DC3-548593F2EA0D"));
        assertTrue(sequenceFlows.get(1).getId().equals("sid-4C19E041-42FA-485D-9D09-D47CCD9DB270") || sequenceFlows.get(1).getId().equals("sid-05A991A6-0296-4867-ACBA-EF9EEC68FB8A")
                || sequenceFlows.get(1).getId().equals("sid-C546AC84-379D-4094-9DC3-548593F2EA0D"));
        assertTrue(sequenceFlows.get(2).getId().equals("sid-4C19E041-42FA-485D-9D09-D47CCD9DB270") || sequenceFlows.get(2).getId().equals("sid-05A991A6-0296-4867-ACBA-EF9EEC68FB8A")
                || sequenceFlows.get(2).getId().equals("sid-C546AC84-379D-4094-9DC3-548593F2EA0D"));
        assertEquals("parallel2", sequenceFlows.get(0).getTargetRef());
        assertEquals("parallel2", sequenceFlows.get(1).getTargetRef());
        assertEquals("parallel2", sequenceFlows.get(2).getTargetRef());
    }

    @Override
    protected String getResource() {
        return "test.flownodemultipleoutgoingflowsmodel.json";
    }

}
