package org.flowable.examples.test;

import org.flowable.bpmn.model.*;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;

import java.util.Date;
import java.util.concurrent.Callable;

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
        BpmnModel model = createTestProcessBpmnModel("oneTaskProcess");
        deployTestProcess(model);

        // start testing process instance
        this.runtimeService.startProcessInstanceByKey("oneTaskProcessPUnitTest");
    }

    @Deployment(resources = {"org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml"})
    public void testProcessModelFailure() {
        BpmnModel model = createTestProcessBpmnModel("twoTasksProcess");
        deployTestProcess(model);

        // start testing process instance
        final ProcessInstance oneTaskProcessPUnitTest = this.runtimeService.startProcessInstanceByKey("oneTaskProcessPUnitTest");

        Date currentTime = processEngine.getProcessEngineConfiguration().getClock().getCurrentTime();
        processEngine.getProcessEngineConfiguration().getClock().setCurrentTime(new Date(currentTime.getTime() + (15 * 1000L)));

        waitForJobExecutorOnCondition(10000L, 500L, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return runtimeService.createProcessInstanceQuery().processInstanceId(oneTaskProcessPUnitTest.getId()).count() == 0;
            }
        });
    }


    private BpmnModel createTestProcessBpmnModel(String processToTestDefinitionKey) {
        BpmnModel model = new BpmnModel();
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        model.addProcess(process);
        model.setTargetNamespace("pUnit");
        process.setId("oneTaskProcessPUnitTest");
        process.setName("ProcessUnit test for oneTaskProcess");

        StartEvent startEvent = new StartEvent();
        startEvent.setId("start");
        process.addFlowElement(startEvent);

        ScriptTask startProcess = new ScriptTask();
        startProcess.setName("Start oneTask process");
        startProcess.setId("startProcess");
        startProcess.setScriptFormat("groovy");
        startProcess.setAsynchronous(true);
        startProcess.setScript(
                "import org.flowable.engine.ProcessEngines;\n" +
                        "\n" +
                        "execution.setVariable('processInstanceId', ProcessEngines.getDefaultProcessEngine().getRuntimeService().createProcessInstanceBuilder().processDefinitionKey('"+ processToTestDefinitionKey +"').start().getId());"
        );
        process.addFlowElement(startProcess);

        ScriptTask completeTask = new ScriptTask();
        completeTask.setName("Complete task");
        completeTask.setId("completeTask");
        completeTask.setAsynchronous(true);
        completeTask.setScriptFormat("groovy");
        completeTask.setScript(
                "import org.flowable.engine.ProcessEngines;\n" +
                        "\n" +
                        "taskId = ProcessEngines.getDefaultProcessEngine().getTaskService().createTaskQuery().processInstanceId(processInstanceId).singleResult().getId()\n" +
                        "ProcessEngines.getDefaultProcessEngine().getTaskService().complete(taskId);"
        );
        process.addFlowElement(completeTask);

        ScriptTask assertTask = new ScriptTask();
        assertTask.setName("Assert task");
        assertTask.setId("assertTask");
        assertTask.setAsynchronous(true);
        assertTask.setScriptFormat("groovy");
        assertTask.setScript(
                "import org.flowable.engine.ProcessEngines;\n" +
                        "import static org.hamcrest.core.Is.is;\n" +
                        "import static org.junit.Assert.assertThat;\n" +
                        "\n" +
                        "assertThat(ProcessEngines.getDefaultProcessEngine().getRuntimeService().createProcessInstanceQuery().processInstanceId(processInstanceId).count(), is(0L));"
        );
        process.addFlowElement(assertTask);

        EndEvent endEvent = new EndEvent();
        endEvent.setId("theEnd");
        process.addFlowElement(endEvent);

        process.addFlowElement(new SequenceFlow("start", "startProcess"));
        process.addFlowElement(new SequenceFlow("startProcess", "completeTask"));
        process.addFlowElement(new SequenceFlow("completeTask", "assertTask"));
        process.addFlowElement(new SequenceFlow("assertTask", "theEnd"));
        return model;
    }

    public String deployTestProcess(BpmnModel bpmnModel) {
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeployment().addBpmnModel("oneTasktest.bpmn20.xml", bpmnModel).deploy();

        deploymentIdsForAutoCleanup.add(deployment.getId()); // For auto-cleanup

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        return processDefinition.getId();
    }


}
