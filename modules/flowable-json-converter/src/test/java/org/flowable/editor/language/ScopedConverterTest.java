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

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.junit.Test;

public class ScopedConverterTest extends AbstractConverterTest {

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
        return "test.scopedmodel.json";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("outerSubProcess", true);
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof SubProcess);
        assertEquals("outerSubProcess", flowElement.getId());
        SubProcess outerSubProcess = (SubProcess) flowElement;
        List<BoundaryEvent> eventList = outerSubProcess.getBoundaryEvents();
        assertEquals(1, eventList.size());
        BoundaryEvent boundaryEvent = eventList.get(0);
        assertEquals("outerBoundaryEvent", boundaryEvent.getId());

        FlowElement subElement = outerSubProcess.getFlowElement("innerSubProcess");
        assertNotNull(subElement);
        assertTrue(subElement instanceof SubProcess);
        assertEquals("innerSubProcess", subElement.getId());
        SubProcess innerSubProcess = (SubProcess) subElement;
        eventList = innerSubProcess.getBoundaryEvents();
        assertEquals(1, eventList.size());
        boundaryEvent = eventList.get(0);
        assertEquals("innerBoundaryEvent", boundaryEvent.getId());

        FlowElement taskElement = innerSubProcess.getFlowElement("usertask");
        assertNotNull(taskElement);
        assertTrue(taskElement instanceof UserTask);
        UserTask userTask = (UserTask) taskElement;
        assertEquals("usertask", userTask.getId());
        eventList = userTask.getBoundaryEvents();
        assertEquals(1, eventList.size());
        boundaryEvent = eventList.get(0);
        assertEquals("taskBoundaryEvent", boundaryEvent.getId());
    }
}
