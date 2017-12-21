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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.junit.Test;

public class BoundaryEventConverterTest extends AbstractConverterTest {

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
        return "test.boundaryeventmodel.json";
    }

    private void validateModel(BpmnModel model) {

        BoundaryEvent errorElement = (BoundaryEvent) model.getMainProcess().getFlowElement("errorEvent", true);
        ErrorEventDefinition errorEvent = (ErrorEventDefinition) extractEventDefinition(errorElement);
        assertTrue(errorElement.isCancelActivity()); // always true
        assertEquals("errorRef", errorEvent.getErrorCode());
        assertEquals("sid-F21E9F4D-EA19-44DF-B1D3-14663A809CAE", errorElement.getAttachedToRefId());

        BoundaryEvent signalElement = (BoundaryEvent) model.getMainProcess().getFlowElement("signalEvent", true);
        SignalEventDefinition signalEvent = (SignalEventDefinition) extractEventDefinition(signalElement);
        assertFalse(signalElement.isCancelActivity());
        assertEquals("signalRef", signalEvent.getSignalRef());
        assertEquals("sid-F21E9F4D-EA19-44DF-B1D3-14663A809CAE", errorElement.getAttachedToRefId());

        BoundaryEvent messageElement = (BoundaryEvent) model.getMainProcess().getFlowElement("messageEvent", true);
        MessageEventDefinition messageEvent = (MessageEventDefinition) extractEventDefinition(messageElement);
        assertFalse(messageElement.isCancelActivity());
        assertEquals("messageRef", messageEvent.getMessageRef());
        assertEquals("sid-F21E9F4D-EA19-44DF-B1D3-14663A809CAE", errorElement.getAttachedToRefId());

        BoundaryEvent timerElement = (BoundaryEvent) model.getMainProcess().getFlowElement("timerEvent", true);
        TimerEventDefinition timerEvent = (TimerEventDefinition) extractEventDefinition(timerElement);
        assertFalse(timerElement.isCancelActivity());
        assertEquals("PT5M", timerEvent.getTimeDuration());
        assertEquals("sid-F21E9F4D-EA19-44DF-B1D3-14663A809CAE", errorElement.getAttachedToRefId());

    }

}
