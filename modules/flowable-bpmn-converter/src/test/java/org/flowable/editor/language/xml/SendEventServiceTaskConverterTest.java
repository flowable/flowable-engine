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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.IOParameter;
import org.flowable.bpmn.model.SendEventServiceTask;
import org.junit.Test;

public class SendEventServiceTaskConverterTest extends AbstractConverterTest {

    @Test
    public void convertXMLToModel() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        validateModel(bpmnModel);
    }

    @Test
    public void convertModelToXML() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        
        FlowElement flowElement = bpmnModel.getMainProcess().getFlowElement("sendEventServiceTask");
        flowElement.getExtensionElements().remove("eventType");
        flowElement.getExtensionElements().remove("triggerEventType");
        flowElement.getExtensionElements().remove("sendSynchronously");
        flowElement.getExtensionElements().remove("eventInParameter");
        flowElement.getExtensionElements().remove("eventOutParameter");
        
        BpmnModel parsedModel = exportAndReadXMLFile(bpmnModel);
        validateModel(parsedModel);
    }

    @Override
    protected String getResource() {
        return "sendeventservicetask.bpmn";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("sendEventServiceTask");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof SendEventServiceTask);
        SendEventServiceTask sendEventServiceTask = (SendEventServiceTask) flowElement;
        assertEquals("sendEventServiceTask", sendEventServiceTask.getId());
        assertEquals("Send event task", sendEventServiceTask.getName());

        assertEquals("myEvent", sendEventServiceTask.getEventType());
        assertTrue(sendEventServiceTask.isTriggerable());
        assertEquals("triggerMyEvent", sendEventServiceTask.getTriggerEventType());
        assertFalse(sendEventServiceTask.isSendSynchronously());

        List<IOParameter> parameters = sendEventServiceTask.getEventInParameters();
        assertEquals(2, parameters.size());
        IOParameter parameter = parameters.get(0);
        assertEquals("${myVariable}", parameter.getSource());
        assertEquals("customerId", parameter.getTarget());
        parameter = parameters.get(1);
        assertEquals("anotherProperty", parameter.getSource());
        assertEquals("anotherCustomerId", parameter.getTarget());
        assertEquals("string", parameter.getAttributeValue(null, "targetType"));

        parameters = sendEventServiceTask.getEventOutParameters();
        assertEquals(1, parameters.size());
        parameter = parameters.get(0);
        assertEquals("eventProperty", parameter.getSource());
        assertEquals("integer", parameter.getAttributeValue(null, "sourceType"));
        assertEquals("newVariable", parameter.getTarget());
        
        List<ExtensionElement> correlationParameters = sendEventServiceTask.getExtensionElements().get(ELEMENT_TRIGGER_EVENT_CORRELATION_PARAMETER);
        assertEquals(2, correlationParameters.size());
        ExtensionElement correlationElement = correlationParameters.get(0);
        assertEquals("customerId", correlationElement.getAttributeValue(null, "name"));
        assertEquals("${customerIdVar}", correlationElement.getAttributeValue(null, "value"));
        correlationElement = correlationParameters.get(1);
        assertEquals("orderId", correlationElement.getAttributeValue(null, "name"));
        assertEquals("${orderIdVar}", correlationElement.getAttributeValue(null, "value"));
    }
}
