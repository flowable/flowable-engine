package org.flowable.editor.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FormProperty;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.StartEvent;
import org.junit.Test;

public class StartEventConverterTest extends AbstractConverterTest {

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

    @Override
    protected String getResource() {
        return "test.starteventmodel.json";
    }

    private void validateModel(BpmnModel model) {

        FlowElement flowElement = model.getMainProcess().getFlowElement("start", true);
        assertTrue(flowElement instanceof StartEvent);

        StartEvent startEvent = (StartEvent) flowElement;
        assertEquals("start", startEvent.getId());
        assertEquals("startName", startEvent.getName());
        assertEquals("startFormKey", startEvent.getFormKey());
        assertEquals("startInitiator", startEvent.getInitiator());
        assertEquals("startDoc", startEvent.getDocumentation());

        assertEquals(2, startEvent.getExecutionListeners().size());
        FlowableListener executionListener = startEvent.getExecutionListeners().get(0);
        assertEquals("start", executionListener.getEvent());
        assertEquals("org.test.TestClass", executionListener.getImplementation());
        assertEquals(ImplementationType.IMPLEMENTATION_TYPE_CLASS, executionListener.getImplementationType());

        executionListener = startEvent.getExecutionListeners().get(1);
        assertEquals("end", executionListener.getEvent());
        assertEquals("${someExpression}", executionListener.getImplementation());
        assertEquals(ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION, executionListener.getImplementationType());

        List<FormProperty> formProperties = startEvent.getFormProperties();
        assertEquals(2, formProperties.size());

        FormProperty formProperty = formProperties.get(0);
        assertEquals("startFormProp1", formProperty.getId());
        assertEquals("startFormProp1", formProperty.getName());
        assertEquals("string", formProperty.getType());

        formProperty = formProperties.get(1);
        assertEquals("startFormProp2", formProperty.getId());
        assertEquals("startFormProp2", formProperty.getName());
        assertEquals("boolean", formProperty.getType());

    }

}
