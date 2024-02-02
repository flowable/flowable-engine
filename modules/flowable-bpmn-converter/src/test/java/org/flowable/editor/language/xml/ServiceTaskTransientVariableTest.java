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
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class ServiceTaskTransientVariableTest {

    @BpmnXmlConverterTest("servicetaskstoreresulttransient.bpmn")
    void validateModel(BpmnModel model) {
        FlowElement flowElement1 = model.getMainProcess().getFlowElement("servicetask1");
        FlowElement flowElement2 = model.getMainProcess().getFlowElement("servicetask2");
        FlowElement flowElement3 = model.getMainProcess().getFlowElement("servicetask3");

        assertThat(flowElement1)
                .isInstanceOfSatisfying(ServiceTask.class, serviceTask -> {
                    assertThat(serviceTask.getId()).isEqualTo("servicetask1");
                    assertThat(serviceTask.isStoreResultVariableAsTransient()).isTrue();
                });
        assertThat(flowElement2)
                .isInstanceOfSatisfying(ServiceTask.class, serviceTask -> {
                    assertThat(serviceTask.getId()).isEqualTo("servicetask2");
                    assertThat(serviceTask.isStoreResultVariableAsTransient()).isFalse();
                });
        assertThat(flowElement3)
                .isInstanceOfSatisfying(ServiceTask.class, serviceTask -> {
                    assertThat(serviceTask.getId()).isEqualTo("servicetask3");
                    assertThat(serviceTask.isStoreResultVariableAsTransient()).isFalse();
                });
    }
}
