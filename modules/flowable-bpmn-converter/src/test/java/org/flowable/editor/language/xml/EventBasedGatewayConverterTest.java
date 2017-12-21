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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventGateway;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ImplementationType;
import org.junit.Test;

/**
 * Test for ACT-1657
 * 
 * @author Frederik Heremans
 */
public class EventBasedGatewayConverterTest extends AbstractConverterTest {

    @Test
    public void convertXMLToModel() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        validateModel(bpmnModel);
    }

    @Override
    protected String getResource() {
        return "eventgatewaymodel.bpmn";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("eventBasedGateway");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof EventGateway);

        EventGateway gateway = (EventGateway) flowElement;
        List<FlowableListener> listeners = gateway.getExecutionListeners();
        assertEquals(1, listeners.size());
        FlowableListener listener = listeners.get(0);
        assertEquals(ImplementationType.IMPLEMENTATION_TYPE_CLASS, listener.getImplementationType());
        assertEquals("org.test.TestClass", listener.getImplementation());
        assertEquals("start", listener.getEvent());
    }
}
