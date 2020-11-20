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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.ExternalWorkerServiceTask;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class ExternalWorkerServiceTaskWithExtensionElementsConverterTest {

    @BpmnXmlConverterTest("externalWorkerServiceTaskWithExtensionElements.bpmn")
    void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("externalWorkerServiceTask");
        assertThat(flowElement).isInstanceOf(ExternalWorkerServiceTask.class);
        ExternalWorkerServiceTask externalWorkerServiceTask = (ExternalWorkerServiceTask) flowElement;
        assertThat(externalWorkerServiceTask.getId()).isEqualTo("externalWorkerServiceTask");
        assertThat(externalWorkerServiceTask.getName()).isEqualTo("External worker task");

        assertThat(externalWorkerServiceTask.getTopic()).isEqualTo("topic");
        assertThat(externalWorkerServiceTask.getSkipExpression()).isEqualTo("skipExpression");
        assertThat(externalWorkerServiceTask.isExclusive()).isTrue();

        assertThat(externalWorkerServiceTask.getExtensionElements())
                .containsOnlyKeys("customValue");

        assertThat(externalWorkerServiceTask.getExtensionElements().get("customValue"))
                .extracting(ExtensionElement::getNamespacePrefix, ExtensionElement::getName, ExtensionElement::getElementText)
                .containsOnly(
                        tuple("flowable", "customValue", "test")
                );
    }
}
