package org.activiti.engine.test.api.repository;

import java.util.List;
import java.util.Map;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.Lane;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * Created by P3700487 on 2/19/2015.
 */
public class LaneExtensionTest {

    @Rule
    public FlowableRule activitiRule = new FlowableRule();

    @Test
    @Deployment
    public void testLaneExtensionElement() {
        ProcessDefinition processDefinition = activitiRule.getRepositoryService().createProcessDefinitionQuery()
                .processDefinitionKey("swimlane-extension").singleResult();
        BpmnModel bpmnModel = activitiRule.getRepositoryService().getBpmnModel(processDefinition.getId());
        byte[] xml = new BpmnXMLConverter().convertToXML(bpmnModel);
        System.out.println(new String(xml));
        Process bpmnProcess = bpmnModel.getMainProcess();
        for (Lane l : bpmnProcess.getLanes()) {
            Map<String, List<ExtensionElement>> extensions = l.getExtensionElements();
            Assert.assertTrue(extensions.size() > 0);
        }
    }

}
