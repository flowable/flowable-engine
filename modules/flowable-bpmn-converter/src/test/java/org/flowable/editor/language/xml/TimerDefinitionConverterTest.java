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

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.IntermediateCatchEvent;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class TimerDefinitionConverterTest {

    @BpmnXmlConverterTest("timerCalendarDefinition.bpmn")
    void validateModel(BpmnModel model) {
        IntermediateCatchEvent timer = (IntermediateCatchEvent) model.getMainProcess().getFlowElement("timer");
        assertThat(timer).isNotNull();
        TimerEventDefinition timerEvent = (TimerEventDefinition) timer.getEventDefinitions().get(0);
        assertThat(timerEvent.getCalendarName()).isEqualTo("custom");
        assertThat(timerEvent.getTimeDuration()).isEqualTo("PT5M");

        StartEvent start = (StartEvent) model.getMainProcess().getFlowElement("theStart");
        assertThat(start).isNotNull();
        TimerEventDefinition startTimerEvent = (TimerEventDefinition) start.getEventDefinitions().get(0);
        assertThat(startTimerEvent.getCalendarName()).isEqualTo("custom");
        assertThat(startTimerEvent.getTimeCycle()).isEqualTo("R2/PT5S");
        assertThat(startTimerEvent.getEndDate()).isEqualTo("${EndDate}");

        BoundaryEvent boundaryTimer = (BoundaryEvent) model.getMainProcess().getFlowElement("boundaryTimer");
        assertThat(boundaryTimer).isNotNull();
        TimerEventDefinition boundaryTimerEvent = (TimerEventDefinition) boundaryTimer.getEventDefinitions().get(0);
        assertThat(boundaryTimerEvent.getCalendarName()).isEqualTo("custom");
        assertThat(boundaryTimerEvent.getTimeDuration()).isEqualTo("PT10S");
        assertThat(boundaryTimerEvent.getEndDate()).isNull();
    }
}
