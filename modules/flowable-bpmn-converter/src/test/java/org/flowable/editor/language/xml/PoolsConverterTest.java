package org.flowable.editor.language.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Lane;
import org.flowable.bpmn.model.Pool;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.junit.Test;

public class PoolsConverterTest extends AbstractConverterTest {

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
        return "pools.bpmn";
    }

    private void validateModel(BpmnModel model) {
        assertEquals(1, model.getPools().size());
        Pool pool = model.getPools().get(0);
        assertEquals("pool1", pool.getId());
        assertEquals("Pool", pool.getName());
        Process process = model.getProcess(pool.getId());
        assertNotNull(process);
        assertEquals(2, process.getLanes().size());
        Lane lane = process.getLanes().get(0);
        assertEquals("lane1", lane.getId());
        assertEquals("Lane 1", lane.getName());
        assertEquals(2, lane.getFlowReferences().size());
        lane = process.getLanes().get(1);
        assertEquals("lane2", lane.getId());
        assertEquals("Lane 2", lane.getName());
        assertEquals(2, lane.getFlowReferences().size());
        FlowElement flowElement = process.getFlowElement("flow1");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof SequenceFlow);
    }
}
