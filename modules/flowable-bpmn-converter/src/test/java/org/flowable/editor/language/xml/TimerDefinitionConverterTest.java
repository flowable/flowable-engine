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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.IntermediateCatchEvent;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.junit.Test;

public class TimerDefinitionConverterTest extends AbstractConverterTest {

    @Test
    public void convertXMLToModel() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        validateModel(bpmnModel);
    }

    @Test
    public void convertModelToXML() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        BpmnModel parsedModel = exportAndReadXMLFile(bpmnModel);
        validateModel(parsedModel);
    }

    @Override
    protected String getResource() {
        return "timerCalendarDefinition.bpmn";
    }

    private void validateModel(BpmnModel model) {
        IntermediateCatchEvent timer = (IntermediateCatchEvent) model.getMainProcess().getFlowElement("timer");
        assertNotNull(timer);
        TimerEventDefinition timerEvent = (TimerEventDefinition) timer.getEventDefinitions().get(0);
        assertThat(timerEvent.getCalendarName(), is("custom"));
        assertEquals("PT5M", timerEvent.getTimeDuration());

        StartEvent start = (StartEvent) model.getMainProcess().getFlowElement("theStart");
        assertNotNull(start);
        TimerEventDefinition startTimerEvent = (TimerEventDefinition) start.getEventDefinitions().get(0);
        assertThat(startTimerEvent.getCalendarName(), is("custom"));
        assertEquals("R2/PT5S", startTimerEvent.getTimeCycle());
        assertEquals("${EndDate}", startTimerEvent.getEndDate());

        BoundaryEvent boundaryTimer = (BoundaryEvent) model.getMainProcess().getFlowElement("boundaryTimer");
        assertNotNull(boundaryTimer);
        TimerEventDefinition boundaryTimerEvent = (TimerEventDefinition) boundaryTimer.getEventDefinitions().get(0);
        assertThat(boundaryTimerEvent.getCalendarName(), is("custom"));
        assertEquals("PT10S", boundaryTimerEvent.getTimeDuration());
        assertNull(boundaryTimerEvent.getEndDate());
    }
}
