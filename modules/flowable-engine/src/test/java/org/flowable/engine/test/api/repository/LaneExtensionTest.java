package org.flowable.engine.test.api.repository;

import java.util.List;
import java.util.Map;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.Lane;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.test.Deployment;
import org.junit.Assert;

/**
 * Created by P3700487 on 2/19/2015.
 */
public class LaneExtensionTest extends PluggableFlowableTestCase {

  @Deployment
  public void testLaneExtensionElement() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("swimlane-extension").singleResult();
    BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
    byte[] xml = new BpmnXMLConverter().convertToXML(bpmnModel);
    System.out.println(new String(xml));
    Process bpmnProcess = bpmnModel.getMainProcess();
    for (Lane l : bpmnProcess.getLanes()) {
      Map<String, List<ExtensionElement>> extensions = l.getExtensionElements();
      Assert.assertTrue(extensions.size() > 0);
    }
  }

}
