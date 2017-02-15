package org.flowable.editor.language;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class AbstractConverterTest {

    protected BpmnModel readJsonFile() throws Exception {
        InputStream jsonStream = this.getClass().getClassLoader().getResourceAsStream(getResource());
        JsonNode modelNode = new ObjectMapper().readTree(jsonStream);
        BpmnModel bpmnModel = new BpmnJsonConverter().convertToBpmnModel(modelNode);
        return bpmnModel;
    }

    protected BpmnModel convertToJsonAndBack(BpmnModel bpmnModel) {
        ObjectNode modelNode = new BpmnJsonConverter().convertToJson(bpmnModel);
        bpmnModel = new BpmnJsonConverter().convertToBpmnModel(modelNode);
        return bpmnModel;
    }

    protected EventDefinition extractEventDefinition(FlowElement flowElement) {
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof Event);
        Event event = (Event) flowElement;
        assertFalse(event.getEventDefinitions().isEmpty());
        return event.getEventDefinitions().get(0);
    }

    protected abstract String getResource();
}
