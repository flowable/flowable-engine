package org.flowable.editor.language;

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BpmnModel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MultiInstanceConverterTest extends AbstractConverterTest {

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

    protected String getResource() {
        return "test.multiinstance.json";
    }

    private void validateModel(BpmnModel model) {
        Activity activity = (Activity) model.getFlowElement("multi-instance");
        assertEquals(activity.getLoopCharacteristics().isSequential(), true);
        assertEquals(activity.getLoopCharacteristics().getLoopCardinality(), "3");
        assertEquals(activity.getLoopCharacteristics().getElementVariable(), "instanceVar");
        assertEquals(activity.getLoopCharacteristics().getInputDataItem(), "collection");
        assertEquals(activity.getLoopCharacteristics().getElementIndexVariable(), "index");
        assertEquals(activity.getLoopCharacteristics().getCompletionCondition(), "completionCondition");
    }

}
