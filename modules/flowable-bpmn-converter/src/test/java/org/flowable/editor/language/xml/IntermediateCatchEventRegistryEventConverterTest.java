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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventRegistryEventDefinition;
import org.flowable.bpmn.model.IntermediateCatchEvent;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class IntermediateCatchEventRegistryEventConverterTest {

    @BpmnXmlConverterTest("intermediateCatchEventRegistryEvent.bpmn")
    void validateModel(BpmnModel model) {
        IntermediateCatchEvent catchEvent = (IntermediateCatchEvent) model.getMainProcess().getFlowElement("catch");
        assertThat(catchEvent.getEventDefinitions())
                .singleElement()
                .isInstanceOfSatisfying(EventRegistryEventDefinition.class,
                        def -> assertThat(def.getEventDefinitionKey()).isEqualTo("orderShipped"));
    }

    @BpmnXmlConverterTest("intermediateCatchEventRegistryEvent.empty.bpmn")
    void emptyEventTypeProducesNoEventDefinition(BpmnModel model) {
        // An empty <flowable:eventType/> on an event host produces no EventDefinition (parser warns and
        // skips); the writer therefore emits nothing for it on round-trip either.
        IntermediateCatchEvent catchEvent = (IntermediateCatchEvent) model.getMainProcess().getFlowElement("catch");
        assertThat(catchEvent.getEventDefinitions()).isEmpty();
    }
}
