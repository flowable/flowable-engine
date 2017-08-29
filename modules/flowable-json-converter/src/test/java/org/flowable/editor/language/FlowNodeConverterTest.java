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
import org.flowable.bpmn.model.ExclusiveGateway;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SequenceFlow;
import org.junit.Test;

public class FlowNodeConverterTest extends AbstractConverterTest {

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
        FlowElement flowElement = model.getMainProcess().getFlowElement("sid-B074A0DD-934A-4053-A537-20ADF0781023", true);
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof ExclusiveGateway);
        ExclusiveGateway gateway = (ExclusiveGateway) flowElement;
        List<SequenceFlow> sequenceFlows = gateway.getOutgoingFlows();
        assertEquals(2, sequenceFlows.size());
        assertTrue(sequenceFlows.get(0).getId().equals("sid-07A7E174-8857-4DE9-A7CD-A041706D79C3") || sequenceFlows.get(0).getId().equals("sid-C2068B1E-9A82-41C9-B876-C58E2736C186"));
        assertTrue(sequenceFlows.get(1).getId().equals("sid-07A7E174-8857-4DE9-A7CD-A041706D79C3") || sequenceFlows.get(1).getId().equals("sid-C2068B1E-9A82-41C9-B876-C58E2736C186"));
        assertEquals("sid-B074A0DD-934A-4053-A537-20ADF0781023", sequenceFlows.get(0).getSourceRef());
        assertEquals("sid-B074A0DD-934A-4053-A537-20ADF0781023", sequenceFlows.get(1).getSourceRef());
        assertEquals("sid-07A7E174-8857-4DE9-A7CD-A041706D79C3", gateway.getDefaultFlow());
    }

    @Override
    protected String getResource() {
        return "test.flownodemodel.json";
    }

}
