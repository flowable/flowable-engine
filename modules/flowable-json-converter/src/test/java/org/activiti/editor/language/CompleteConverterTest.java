package org.activiti.editor.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.IntermediateCatchEvent;
import org.flowable.bpmn.model.ReceiveTask;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.junit.Test;

public class CompleteConverterTest extends AbstractConverterTest {

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
    return "test.completemodel.json";
  }

  private void validateModel(BpmnModel model) {
    FlowElement flowElement = model.getMainProcess().getFlowElement("userTask1", true);
    assertNotNull(flowElement);
    assertTrue(flowElement instanceof UserTask);
    assertEquals("userTask1", flowElement.getId());

    flowElement = model.getMainProcess().getFlowElement("catchsignal", true);
    assertNotNull(flowElement);
    assertTrue(flowElement instanceof IntermediateCatchEvent);
    assertEquals("catchsignal", flowElement.getId());
    IntermediateCatchEvent catchEvent = (IntermediateCatchEvent) flowElement;
    assertEquals(1, catchEvent.getEventDefinitions().size());
    assertTrue(catchEvent.getEventDefinitions().get(0) instanceof SignalEventDefinition);
    SignalEventDefinition signalEvent = (SignalEventDefinition) catchEvent.getEventDefinitions().get(0);
    assertEquals("testSignal", signalEvent.getSignalRef());

    flowElement = model.getMainProcess().getFlowElement("subprocess", true);
    assertNotNull(flowElement);
    assertTrue(flowElement instanceof SubProcess);
    assertEquals("subprocess", flowElement.getId());
    SubProcess subProcess = (SubProcess) flowElement;

    flowElement = subProcess.getFlowElement("receiveTask");
    assertNotNull(flowElement);
    assertTrue(flowElement instanceof ReceiveTask);
    assertEquals("receiveTask", flowElement.getId());
  }
}
