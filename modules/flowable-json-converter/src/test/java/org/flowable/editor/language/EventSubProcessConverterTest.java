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

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.junit.jupiter.api.Test;

public class EventSubProcessConverterTest extends AbstractConverterTest {

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
        return "test.eventsubprocessmodel.json";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("task1");
        assertThat(flowElement).isInstanceOf(UserTask.class);
        assertThat(flowElement.getId()).isEqualTo("task1");

        FlowElement eventSubProcessElement = model.getMainProcess().getFlowElement("eventSubProcess");
        assertThat(eventSubProcessElement).isInstanceOf(EventSubProcess.class);
        EventSubProcess eventSubProcess = (EventSubProcess) eventSubProcessElement;
        assertThat(eventSubProcess.getId()).isEqualTo("eventSubProcess");

        FlowElement signalStartEvent = eventSubProcess.getFlowElement("eventSignalStart");
        assertThat(signalStartEvent).isInstanceOf(StartEvent.class);
        StartEvent startEvent = (StartEvent) signalStartEvent;
        assertThat(startEvent.getId()).isEqualTo("eventSignalStart");
        assertThat(startEvent.isInterrupting()).isTrue();
        assertThat(startEvent.getSubProcess().getId()).isEqualTo(eventSubProcess.getId());
    }
}
