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

import java.util.Collection;
import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Signal;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class SignalExpressionConverterTest {

    @BpmnXmlConverterTest("signalExpressionTest.bpmn")
    void validateModel(BpmnModel model) {
        Collection<Signal> signals = model.getSignals();
        assertThat(signals).hasSize(1);

        Signal signal = signals.iterator().next();
        assertThat(signal.getId()).isEqualTo("signal1");

        List<StartEvent> startEvents = model.getMainProcess().findFlowElementsOfType(StartEvent.class);
        assertThat(startEvents).hasSize(1);
        StartEvent startEvent = startEvents.get(0);
        assertThat(startEvent.getEventDefinitions()).hasSize(1);
        assertThat(startEvent.getEventDefinitions().get(0))
                .isInstanceOfSatisfying(SignalEventDefinition.class, signalEventDefinition -> {
                    assertThat(signalEventDefinition.getSignalExpression()).isEqualTo("${someExpressionThatReturnsSignalId}");

                });
    }
}
