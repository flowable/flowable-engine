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
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class UserTaskWithEventRegistryBoundaryEventConverterTest {

    @BpmnXmlConverterTest("usertaskeventregistry.bpmn")
    void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("usertask");
        assertThat(flowElement)
                .isInstanceOfSatisfying(UserTask.class, userTask -> {
                    assertThat(userTask.getId()).isEqualTo("usertask");
                    assertThat(userTask.getName()).isEqualTo("User task");
                    assertThat(userTask.getAssignee()).isEqualTo("kermit");
                });

        flowElement = model.getMainProcess().getFlowElement("eventRegistryEvent");
        assertThat(flowElement)
                .isInstanceOfSatisfying(BoundaryEvent.class, boundaryEvent -> {
                    assertThat(boundaryEvent.getId()).isEqualTo("eventRegistryEvent");
                    assertThat(boundaryEvent.getAttachedToRefId()).isEqualTo("usertask");
                    assertThat(boundaryEvent.getExtensionElements()).hasSize(2);
                    ExtensionElement extensionElement = boundaryEvent.getExtensionElements().get("eventType").get(0);
                    assertThat(extensionElement.getElementText()).isEqualTo("myEvent");
                    extensionElement = boundaryEvent.getExtensionElements().get("eventCorrelationParameter").get(0);
                    assertThat(extensionElement.getAttributeValue(null, "name")).isEqualTo("customerId");
                    assertThat(extensionElement.getAttributeValue(null, "value")).isEqualTo("${customerIdVar}");
                });
    }
}
