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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.junit.Test;

public class EventNonInterruptingSubProcessConverterTest extends AbstractConverterTest {

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
        return "test.eventnoninterruptingsubprocessmodel.json";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("task1");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof UserTask);
        assertEquals("task1", flowElement.getId());

        FlowElement eventSubProcessElement = model.getMainProcess().getFlowElement("eventSubProcess");
        assertNotNull(eventSubProcessElement);
        assertTrue(eventSubProcessElement instanceof EventSubProcess);
        EventSubProcess eventSubProcess = (EventSubProcess) eventSubProcessElement;
        assertEquals("eventSubProcess", eventSubProcess.getId());

        FlowElement signalStartEvent = eventSubProcess.getFlowElement("eventSignalStart");
        assertNotNull(signalStartEvent);
        assertTrue(signalStartEvent instanceof StartEvent);
        StartEvent startEvent = (StartEvent) signalStartEvent;
        assertEquals("eventSignalStart", startEvent.getId());
        assertTrue(!startEvent.isInterrupting());
        assertEquals(eventSubProcess.getId(), startEvent.getSubProcess().getId());
    }
}
