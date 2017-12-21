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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.IntermediateCatchEvent;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.junit.Test;

public class SimpleConverterTest extends AbstractConverterTest {

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
        return "test.simplemodel.json";
    }

    private void validateModel(BpmnModel model) {
        assertEquals("simpleProcess", model.getMainProcess().getId());
        assertEquals("Simple process", model.getMainProcess().getName());
        assertTrue(model.getMainProcess().isExecutable());

        FlowElement flowElement = model.getMainProcess().getFlowElement("flow1", true);
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof SequenceFlow);
        assertEquals("flow1", flowElement.getId());

        flowElement = model.getMainProcess().getFlowElement("catchEvent", true);
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof IntermediateCatchEvent);
        assertEquals("catchEvent", flowElement.getId());
        IntermediateCatchEvent catchEvent = (IntermediateCatchEvent) flowElement;
        assertEquals(1, catchEvent.getEventDefinitions().size());
        EventDefinition eventDefinition = catchEvent.getEventDefinitions().get(0);
        assertTrue(eventDefinition instanceof TimerEventDefinition);
        TimerEventDefinition timerDefinition = (TimerEventDefinition) eventDefinition;
        assertEquals("PT5M", timerDefinition.getTimeDuration());

        flowElement = model.getMainProcess().getFlowElement("flow1Condition", true);
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof SequenceFlow);
        assertEquals("flow1Condition", flowElement.getId());
        SequenceFlow flow = (SequenceFlow) flowElement;
        assertEquals("${number <= 1}", flow.getConditionExpression());
    }
}
