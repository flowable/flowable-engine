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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.Signal;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.StartEvent;
import org.junit.Test;

public class SignalExpressionConverterTest extends AbstractConverterTest {

    @Test
    public void convertXMLToModel() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        validateModel(bpmnModel);
    }

    @Test
    public void convertModelToXML() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        BpmnModel parsedModel = exportAndReadXMLFile(bpmnModel);
        validateModel(parsedModel);
    }

    private void validateModel(BpmnModel model) {
        Collection<Signal> signals = model.getSignals();
        assertEquals(1, signals.size());

        Signal signal = signals.iterator().next();
        assertEquals("signal1", signal.getId());

        List<StartEvent> startEvents = model.getMainProcess().findFlowElementsOfType(StartEvent.class);
        assertEquals(1, startEvents.size());
        StartEvent startEvent = startEvents.get(0);
        assertEquals(1, startEvent.getEventDefinitions().size());

        EventDefinition eventDefinition = startEvent.getEventDefinitions().get(0);
        assertTrue(eventDefinition instanceof SignalEventDefinition);
        SignalEventDefinition signalEventDefinition = (SignalEventDefinition) eventDefinition;
        assertEquals("${someExpressionThatReturnsSignalId}", signalEventDefinition.getSignalExpression());
    }

    @Override
    protected String getResource() {
        return "signalExpressionTest.bpmn";
    }
}
