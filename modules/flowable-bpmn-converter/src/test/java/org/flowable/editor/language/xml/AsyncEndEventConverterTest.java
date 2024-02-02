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

import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class AsyncEndEventConverterTest {

    @BpmnXmlConverterTest("asyncendeventmodel.bpmn")
    void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("endEvent");
        assertThat(flowElement).isInstanceOf(EndEvent.class);
        assertThat(flowElement.getId()).isEqualTo("endEvent");
        EndEvent endEvent = (EndEvent) flowElement;
        assertThat(endEvent.getId()).isEqualTo("endEvent");
        assertThat(endEvent.isAsynchronous()).isTrue();

        List<FlowableListener> listeners = endEvent.getExecutionListeners();
        assertThat(listeners)
                .extracting(FlowableListener::getImplementationType, FlowableListener::getImplementation, FlowableListener::getEvent)
                .containsExactly(tuple(ImplementationType.IMPLEMENTATION_TYPE_CLASS, "org.test.TestClass", "start"));
    }
}
