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
import static org.assertj.core.api.Assertions.tuple;
import static org.flowable.bpmn.constants.BpmnXMLConstants.ELEMENT_TRIGGER_EVENT_CORRELATION_PARAMETER;

import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.IOParameter;
import org.flowable.bpmn.model.SendEventServiceTask;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class SendEventServiceTaskConverterTest {

    @BpmnXmlConverterTest("sendeventservicetask.bpmn")
    void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("sendEventServiceTask");
        assertThat(flowElement)
                .isInstanceOfSatisfying(SendEventServiceTask.class, sendEventServiceTask -> {
                    assertThat(sendEventServiceTask.getId()).isEqualTo("sendEventServiceTask");
                    assertThat(sendEventServiceTask.getName()).isEqualTo("Send event task");
                    assertThat(sendEventServiceTask.getEventType()).isEqualTo("myEvent");
                    assertThat(sendEventServiceTask.isTriggerable()).isTrue();
                    assertThat(sendEventServiceTask.getTriggerEventType()).isEqualTo("triggerMyEvent");
                    assertThat(sendEventServiceTask.isSendSynchronously()).isFalse();
                    assertThat(sendEventServiceTask.getEventInParameters())
                            .extracting(IOParameter::getSource, IOParameter::getTarget)
                            .containsExactly(
                                    tuple("${myVariable}", "customerId"),
                                    tuple("anotherProperty", "anotherCustomerId")
                            );
                    assertThat(sendEventServiceTask.getEventInParameters().get(1).getAttributeValue(null, "targetType")).isEqualTo("string");
                    assertThat(sendEventServiceTask.getEventOutParameters())
                            .extracting(IOParameter::getSource, IOParameter::getTarget)
                            .containsExactly(tuple("eventProperty", "newVariable"));
                    assertThat(sendEventServiceTask.getEventOutParameters().get(0).getAttributeValue(null, "sourceType")).isEqualTo("integer");

                    List<ExtensionElement> correlationParameters = flowElement.getExtensionElements()
                            .get(ELEMENT_TRIGGER_EVENT_CORRELATION_PARAMETER);
                    assertThat(correlationParameters).hasSize(2);
                    ExtensionElement correlationElement = correlationParameters.get(0);
                    assertThat(correlationElement.getAttributeValue(null, "name")).isEqualTo("customerId");
                    assertThat(correlationElement.getAttributeValue(null, "value")).isEqualTo("${customerIdVar}");
                    correlationElement = correlationParameters.get(1);
                    assertThat(correlationElement.getAttributeValue(null, "name")).isEqualTo("orderId");
                    assertThat(correlationElement.getAttributeValue(null, "value")).isEqualTo("${orderIdVar}");
                });
    }
}
