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

import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.VariableListenerEventDefinition;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class VariableListenerConverterTest {

    @BpmnXmlConverterTest("variablelistenertest.bpmn")
    void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getFlowElement("variableListenerCatch");
        assertThat(flowElement).isInstanceOf(Event.class);
        Event event = (Event) flowElement;
        assertThat(event.getEventDefinitions()).hasSize(1);
        EventDefinition eventDefinition = (EventDefinition) event.getEventDefinitions().iterator().next();
        assertThat(eventDefinition).isInstanceOf(VariableListenerEventDefinition.class);
        
        VariableListenerEventDefinition variableListenerEventDefinition = (VariableListenerEventDefinition) eventDefinition;
        assertThat(variableListenerEventDefinition.getVariableName()).isEqualTo("var1");
        assertThat(variableListenerEventDefinition.getVariableChangeType()).isEqualTo(VariableListenerEventDefinition.CHANGE_TYPE_UPDATE_CREATE);
        
        List<String> activityIds = model.getActivityIdsForVariableListenerName("var1");
        assertThat(activityIds).isNotNull().hasSize(1);
        assertThat(activityIds.iterator().next()).isEqualTo("variableListenerCatch");
    }
}
