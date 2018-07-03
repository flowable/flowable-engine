package org.flowable.editor.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.junit.Test;

/**
 * Tests json - model conversions for multi instance type property
 */
public class MultiInstanceTypeExpressionConverterTest extends AbstractConverterTest {

    String resource;

    @Override
    protected String getResource() {
        return resource;
    }

    @Test
    public void convertJsonToModel() throws Exception {
        resource = "test.multiinstanceTypeExpression.json";
        BpmnModel bpmnModel = readJsonFile();
        validateModel(bpmnModel, "${someExpression}");
    }

    @Test
    public void doubleConversionValidation() throws Exception {
        resource = "test.multiinstanceTypeExpression.json";
        BpmnModel bpmnModel = readJsonFile();
        bpmnModel = convertToJsonAndBack(bpmnModel);
        validateModel(bpmnModel, "${someExpression}");
    }

    @Test
    public void convertJsonToModelWithoutExpression() throws Exception {
        resource = "test.multiinstanceTypeWithoutExpression.json";
        BpmnModel bpmnModel = readJsonFile();
        validateModel(bpmnModel, "Sequential");
    }

    @Test
    public void doubleConversionValidationWithoutExpression() throws Exception {
        resource = "test.multiinstanceTypeWithoutExpression.json";
        BpmnModel bpmnModel = readJsonFile();
        bpmnModel = convertToJsonAndBack(bpmnModel);
        validateModel(bpmnModel, "Sequential");
    }

    protected void validateModel(BpmnModel model, String isSequential) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("multiInstanceUserTask", true);
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof UserTask);
        assertEquals("multiInstanceUserTask", flowElement.getId());
        assertEquals(isSequential, ((UserTask) flowElement).getLoopCharacteristics().getSequential());
    }
}
