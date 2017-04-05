package org.flowable.examples.test;

import org.flowable.bpmn.model.*;
import org.flowable.engine.ProcessEngines;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * This class provides examples how to test one task process
 */
public class OneTaskProcessTest extends PluggableFlowableTestCase {

    @Deployment(resources={"org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
    public void testStandardJUnitOneTaskProcess() {
        // Arrange -> start process
        ProcessInstance oneTaskProcess = this.runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess").start();

        // Act -> complete task
        this.taskService.complete(this.taskService.createTaskQuery().processInstanceId(oneTaskProcess.getProcessInstanceId()).singleResult().getId());

        // Assert -> process instance is finished
        assertThat(this.runtimeService.createProcessInstanceQuery().processInstanceId(oneTaskProcess.getId()).count(), is(0L));
    }

    @Deployment(resources={"org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
    public void testProcessModelByAnotherProcess() {
        BpmnModel model = new BpmnModel();
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        model.addProcess(process);
        process.setId("oneTaskProcessPUnitTest");
        process.setName("ProcessUnit test for oneTaskProcess");

        StartEvent startEvent = new StartEvent();
        startEvent.setId("start");
        process.addFlowElement(startEvent);

        ScriptTask startProcess = new ScriptTask();
        startProcess.setName("Start oneTask process");
        startProcess.setId("startProcess");
        startProcess.setScriptFormat("groovy");
        startProcess.setScript(
                "import org.flowable.engine.ProcessEngines;\n" +
                        "\n" +
                        "ProcessEngines.getDefaultProcessEngine().getRuntimeService().createProcessInstanceBuilder().processDefinitionKey(\"oneTaskProcess\").start();"
        );
        process.addFlowElement(startProcess);

        EndEvent endEvent = new EndEvent();
        endEvent.setId("theEnd");
        process.addFlowElement(endEvent);

        process.addFlowElement(new SequenceFlow("start", "startProcess"));
        process.addFlowElement(new SequenceFlow("startProcess", "theEnd"));

        String deploymentId = deployTestProcess(model);

        // Arrange -> start process
        ProcessInstance oneTaskProcess = this.runtimeService.startProcessInstanceByKey("oneTaskProcessPUnitTest");

    }

    public String deployTestProcess(BpmnModel bpmnModel) {
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeployment().addBpmnModel("oneTasktest.bpmn20.xml", bpmnModel).deploy();

        deploymentIdsForAutoCleanup.add(deployment.getId()); // For auto-cleanup

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        return processDefinition.getId();
    }


}
