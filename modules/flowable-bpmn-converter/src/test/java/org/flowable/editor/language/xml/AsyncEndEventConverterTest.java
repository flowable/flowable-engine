package org.flowable.editor.language.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ImplementationType;
import org.junit.Test;

public class AsyncEndEventConverterTest extends AbstractConverterTest {

    @Test
    public void connvertXMLToModel() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        validateModel(bpmnModel);
    }

    @Test
    public void convertModelToXML() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        BpmnModel parsedModel = exportAndReadXMLFile(bpmnModel);
        validateModel(parsedModel);
        deployProcess(parsedModel);
    }

    protected String getResource() {
        return "asyncendeventmodel.bpmn";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("endEvent");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof EndEvent);
        assertEquals("endEvent", flowElement.getId());
        EndEvent endEvent = (EndEvent) flowElement;
        assertEquals("endEvent", endEvent.getId());
        assertTrue(endEvent.isAsynchronous());

        List<FlowableListener> listeners = endEvent.getExecutionListeners();
        assertEquals(1, listeners.size());
        FlowableListener listener = listeners.get(0);
        assertEquals(ImplementationType.IMPLEMENTATION_TYPE_CLASS, listener.getImplementationType());
        assertEquals("org.test.TestClass", listener.getImplementation());
        assertEquals("start", listener.getEvent());
    }
}
