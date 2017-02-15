package org.flowable.editor.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.flowable.bpmn.model.BpmnModel;
import org.junit.Test;

public class NotExecutableConverterTest extends AbstractConverterTest {

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
        return "test.notexecutablemodel.json";
    }

    private void validateModel(BpmnModel model) {
        assertEquals("simpleProcess", model.getMainProcess().getId());
        assertEquals("Simple process", model.getMainProcess().getName());
        assertFalse(model.getMainProcess().isExecutable());
    }
}
