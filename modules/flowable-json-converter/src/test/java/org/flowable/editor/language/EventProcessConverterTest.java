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

import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.StartEvent;
import org.junit.jupiter.api.Test;

public class EventProcessConverterTest extends AbstractConverterTest {

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
        return "test.eventprocessmodel.json";
    }

    private void validateModel(BpmnModel model) {
        List<StartEvent> startEvents = model.getMainProcess().findFlowElementsOfType(StartEvent.class);
        assertThat(startEvents).hasSize(1);
        assertThat(startEvents.get(0).getEventDefinitions()).isEmpty();
        Map<String, List<ExtensionElement>> extensionMap = startEvents.get(0).getExtensionElements();
        assertThat(extensionMap.get("eventType").get(0).getElementText()).isEqualTo("myEvent");
        assertThat(extensionMap.get("eventName").get(0).getElementText()).isEqualTo("My event name");
        assertThat(extensionMap.get("channelKey").get(0).getElementText()).isEqualTo("testChannel");
        assertThat(extensionMap.get("channelName").get(0).getElementText()).isEqualTo("My test channel");
        assertThat(extensionMap.get("channelType").get(0).getElementText()).isEqualTo("jms");
        assertThat(extensionMap.get("channelDestination").get(0).getElementText()).isEqualTo("testQueue");

        List<BoundaryEvent> boundaryEvents = model.getMainProcess().findFlowElementsOfType(BoundaryEvent.class);
        assertThat(boundaryEvents).hasSize(1);
        assertThat(boundaryEvents.get(0).getEventDefinitions()).isEmpty();
        extensionMap = boundaryEvents.get(0).getExtensionElements();
        assertThat(extensionMap.get("eventType").get(0).getElementText()).isEqualTo("boundaryEvent");
        assertThat(extensionMap.get("eventName").get(0).getElementText()).isEqualTo("Boundary event");
        assertThat(extensionMap.get("channelKey").get(0).getElementText()).isEqualTo("boundaryChannel");
        assertThat(extensionMap.get("channelName").get(0).getElementText()).isEqualTo("Boundary channel");
        assertThat(extensionMap.get("channelType").get(0).getElementText()).isEqualTo("jms");
        assertThat(extensionMap.get("channelDestination").get(0).getElementText()).isEqualTo("boundaryQueue");
    }
}
