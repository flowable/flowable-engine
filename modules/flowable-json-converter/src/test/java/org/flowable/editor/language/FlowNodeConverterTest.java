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
import org.flowable.bpmn.model.InclusiveGateway;
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
        ExclusiveGateway exclusiveGateway = (ExclusiveGateway) flowElement;
        List<SequenceFlow> sequenceFlows = exclusiveGateway.getOutgoingFlows();
        assertEquals(2, sequenceFlows.size());
        assertTrue(sequenceFlows.get(0).getId().equals("sid-07A7E174-8857-4DE9-A7CD-A041706D79C3") || sequenceFlows.get(0).getId().equals("sid-C2068B1E-9A82-41C9-B876-C58E2736C186"));
        assertTrue(sequenceFlows.get(1).getId().equals("sid-07A7E174-8857-4DE9-A7CD-A041706D79C3") || sequenceFlows.get(1).getId().equals("sid-C2068B1E-9A82-41C9-B876-C58E2736C186"));
        assertEquals("sid-B074A0DD-934A-4053-A537-20ADF0781023", sequenceFlows.get(0).getSourceRef());
        assertEquals("sid-B074A0DD-934A-4053-A537-20ADF0781023", sequenceFlows.get(1).getSourceRef());
        assertEquals("sid-07A7E174-8857-4DE9-A7CD-A041706D79C3", exclusiveGateway.getDefaultFlow());

        flowElement = model.getMainProcess().getFlowElement("sid-F9AABCC8-8E36-428F-A4C3-32DC991E64F5", true);
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof InclusiveGateway);
        InclusiveGateway inclusiveGateway = (InclusiveGateway) flowElement;
        sequenceFlows = inclusiveGateway.getOutgoingFlows();
        assertEquals(2, sequenceFlows.size());
        assertTrue(sequenceFlows.get(0).getId().equals("sid-849DE573-3063-4DFB-A729-39DFBF3DFB35") || sequenceFlows.get(0).getId().equals("sid-10913B58-DFB0-462B-B0EE-0EC4118D70A5"));
        assertTrue(sequenceFlows.get(1).getId().equals("sid-849DE573-3063-4DFB-A729-39DFBF3DFB35") || sequenceFlows.get(1).getId().equals("sid-10913B58-DFB0-462B-B0EE-0EC4118D70A5"));
        assertEquals("sid-F9AABCC8-8E36-428F-A4C3-32DC991E64F5", sequenceFlows.get(0).getSourceRef());
        assertEquals("sid-F9AABCC8-8E36-428F-A4C3-32DC991E64F5", sequenceFlows.get(1).getSourceRef());
        assertEquals("sid-849DE573-3063-4DFB-A729-39DFBF3DFB35", inclusiveGateway.getDefaultFlow());
    }

    @Override
    protected String getResource() {
        return "test.flownodemodel.json";
    }

}
