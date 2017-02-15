package org.flowable.editor.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.bpmn.model.UserTask;
import org.junit.Test;

public class SubProcessConverterTest extends AbstractConverterTest {

    @Test
    public void connvertJsonToModel() throws Exception {
        BpmnModel bpmnModel = readJsonFile();
        validateModel(bpmnModel);
    }

    @Test
    public void doubleConversionValidation() throws Exception {
        BpmnModel bpmnModel = readJsonFile();
        bpmnModel = convertToJsonAndBack(bpmnModel);
        validateModel(bpmnModel);
    }

    protected String getResource() {
        return "test.subprocessmodel.json";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("start1", true);
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof StartEvent);
        assertEquals("start1", flowElement.getId());

        flowElement = model.getMainProcess().getFlowElement("userTask1", true);
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof UserTask);
        assertEquals("userTask1", flowElement.getId());
        UserTask userTask = (UserTask) flowElement;
        assertEquals(1, userTask.getCandidateUsers().size());
        assertEquals(1, userTask.getCandidateGroups().size());
        assertEquals(2, userTask.getFormProperties().size());

        flowElement = model.getMainProcess().getFlowElement("subprocess1", true);
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof SubProcess);
        assertEquals("subprocess1", flowElement.getId());
        SubProcess subProcess = (SubProcess) flowElement;
        assertEquals(5, subProcess.getFlowElements().size());

        flowElement = model.getMainProcess().getFlowElement("boundaryEvent1", true);
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof BoundaryEvent);
        assertEquals("boundaryEvent1", flowElement.getId());
        BoundaryEvent boundaryEvent = (BoundaryEvent) flowElement;
        assertNotNull(boundaryEvent.getAttachedToRef());
        assertEquals("subprocess1", boundaryEvent.getAttachedToRef().getId());
        assertEquals(1, boundaryEvent.getEventDefinitions().size());
        assertTrue(boundaryEvent.getEventDefinitions().get(0) instanceof TimerEventDefinition);
    }
}
