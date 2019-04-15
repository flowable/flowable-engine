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

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.StartEvent;
import org.junit.Test;

/**
 * @author Zheng Ji
 */
public class ErrorConverterTest extends AbstractConverterTest {

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

    @Override
    protected String getResource() {
        return "errors.bpmn20.xml";
    }

    private void validateModel(BpmnModel model) {
        assertEquals(1, model.getProcesses().size());
        assertEquals(2, model.getErrors().size());

        // error start event
        FlowElement flowElement = model.getMainProcess().getFlowElement("startErrorEvent");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof StartEvent);

        StartEvent startEvent = (StartEvent) flowElement;
        List<EventDefinition> eventDefinitions = startEvent.getEventDefinitions();
        assertEquals(1, eventDefinitions.size());

        EventDefinition eventDefinition = eventDefinitions.get(0);
        assertTrue(eventDefinition instanceof ErrorEventDefinition);

        ErrorEventDefinition errorEventDefinition = (ErrorEventDefinition) eventDefinition;
        String errorCode = errorEventDefinition.getErrorCode();
        assertEquals("myerror1", errorCode);

        // error end event
        FlowElement endErrorFlowElement = model.getMainProcess().getFlowElement("endErrorEvent");
        assertNotNull(endErrorFlowElement);
        assertTrue(endErrorFlowElement instanceof EndEvent);

        EndEvent endEvent = (EndEvent) endErrorFlowElement;
        List<EventDefinition> endEventDefinitions = endEvent.getEventDefinitions();
        assertEquals(1, endEventDefinitions.size());

        EventDefinition endEventDefinition = endEventDefinitions.get(0);
        assertTrue(endEventDefinition instanceof ErrorEventDefinition);

        ErrorEventDefinition errorEndEventDefinition = (ErrorEventDefinition) endEventDefinition;
        String errorEndCode = errorEndEventDefinition.getErrorCode();
        assertEquals("myerror2", errorEndCode);

        // error boundary event
        FlowElement boundaryErrorFlowElement = model.getMainProcess().getFlowElement("errorBoundaryEvent");
        assertNotNull(boundaryErrorFlowElement);
        assertTrue(boundaryErrorFlowElement instanceof BoundaryEvent);

        BoundaryEvent boundaryEvent = (BoundaryEvent) boundaryErrorFlowElement;
        List<EventDefinition> boundaryEventDefinitions = boundaryEvent.getEventDefinitions();
        assertEquals(1, boundaryEventDefinitions.size());

        EventDefinition boundaryEventDefinition = boundaryEventDefinitions.get(0);
        assertTrue(boundaryEventDefinition instanceof ErrorEventDefinition);

        ErrorEventDefinition errorBoundaryEventDefinition = (ErrorEventDefinition) boundaryEventDefinition;
        String errorBoundaryCode = errorBoundaryEventDefinition.getErrorCode();
        assertEquals("myerror2", errorBoundaryCode);
    }
}
