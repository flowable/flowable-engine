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

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.StartEvent;
import org.junit.Test;

import java.util.List;

/**
 * @author Zheng Ji
 */
public class ErrorConverterTest extends AbstractConverterTest {

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
        return "test.errormodel.json";
    }

    private void validateModel(BpmnModel model) {
        // error start event
        FlowElement startFlowElement = model.getMainProcess().getFlowElement("startErrorEvent");
        assertNotNull(startFlowElement);
        assertTrue(startFlowElement instanceof StartEvent);

        StartEvent startEvent = (StartEvent) startFlowElement;
        List<EventDefinition> eventDefinitions = startEvent.getEventDefinitions();
        assertEquals(1, eventDefinitions.size());

        EventDefinition eventDefinition = eventDefinitions.get(0);
        assertTrue(eventDefinition instanceof ErrorEventDefinition);

        ErrorEventDefinition errorEventDefinition= (ErrorEventDefinition) eventDefinition;
        String errorCode = errorEventDefinition.getErrorCode();
        assertEquals("myerror1",errorCode);

        // error end event
        FlowElement endFlowElement = model.getMainProcess().getFlowElement("endErrorEvent");
        assertNotNull(endFlowElement);
        assertTrue(endFlowElement instanceof EndEvent);

        EndEvent endEvent = (EndEvent) endFlowElement;
        List<EventDefinition> endEventDefinitions = endEvent.getEventDefinitions();
        assertEquals(1, endEventDefinitions.size());
        EventDefinition endEventDefinition = endEventDefinitions.get(0);
        assertTrue(endEventDefinition instanceof ErrorEventDefinition);

        ErrorEventDefinition errorEndEventDefinition = (ErrorEventDefinition) endEventDefinition;
        String endErrorCode = errorEndEventDefinition.getErrorCode();
        assertEquals("myerror2", endErrorCode);

        // error boundary event
        FlowElement boundaryFlowElement = model.getMainProcess().getFlowElement("errorBoundaryEvent");
        assertNotNull(boundaryFlowElement);
        assertTrue(boundaryFlowElement instanceof BoundaryEvent);

        BoundaryEvent boundaryEvent = (BoundaryEvent) boundaryFlowElement;
        List<EventDefinition> boundaryEventDefinitions = boundaryEvent.getEventDefinitions();
        assertEquals(1, boundaryEventDefinitions.size());
        EventDefinition boundaryEventDefinition = boundaryEventDefinitions.get(0);
        assertTrue(boundaryEventDefinition instanceof ErrorEventDefinition);

        ErrorEventDefinition errorBoundaryEventDefinition = (ErrorEventDefinition) endEventDefinition;
        String boundaryErrorCode = errorBoundaryEventDefinition.getErrorCode();
        assertEquals("myerror2", boundaryErrorCode);
    }
}
