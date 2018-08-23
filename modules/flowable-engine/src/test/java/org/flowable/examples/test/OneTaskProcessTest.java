/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.examples.test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.ScriptTask;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.engine.event.EventLogEntry;
import org.flowable.engine.impl.event.logger.EventLogger;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class provides examples how to test one task process
 */
public class OneTaskProcessTest extends PluggableFlowableTestCase {

    protected EventLogger databaseEventLogger;

    @BeforeEach
    protected void setUp() throws Exception {

        // Database event logger setup
        databaseEventLogger = new EventLogger(processEngineConfiguration.getClock(), processEngineConfiguration.getObjectMapper());
        runtimeService.addEventListener(databaseEventLogger);
    }

    @AfterEach
    protected void tearDown() throws Exception {
        // Cleanup
        for (EventLogEntry eventLogEntry : managementService.getEventLogEntries(null, null)) {
            managementService.deleteEventLogEntry(eventLogEntry.getLogNumber());
        }

        // Database event logger teardown
        runtimeService.removeEventListener(databaseEventLogger);

    }

    @Test
    @Deployment(resources = {"org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
    public void testStandardJUnitOneTaskProcess() {
        // Arrange -> start process
        ProcessInstance oneTaskProcess = this.runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess").start();

        // Act -> complete task
        this.taskService.complete(this.taskService.createTaskQuery().processInstanceId(oneTaskProcess.getProcessInstanceId()).singleResult().getId());

        // Assert -> process instance is finished
        assertThat(this.runtimeService.createProcessInstanceQuery().processInstanceId(oneTaskProcess.getId()).count(), is(0L));
    }

    @Test
    @Deployment(resources = {"org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
    public void testProcessModelByAnotherProcess() {
        testProcessModelByAnotherProcess(
                createTestProcessBpmnModel("oneTaskProcess")
        );
    }

    private void testProcessModelByAnotherProcess(BpmnModel model) {
        deployTestProcess(model);

        // start testing process instance
        ProcessInstance pUnitTestProcessInstance = this.runtimeService.startProcessInstanceByKey(model.getMainProcess().getId());

        waitForJobExecutorToProcessAllJobs(15000, 200);
        assertThat(this.runtimeService.createProcessInstanceQuery().processInstanceId(pUnitTestProcessInstance.getId()).count(), is(0L));
    }

    @Test
    @Deployment(resources = {"org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml"})
    public void testProcessModelFailure() {
        // deploy different process - test should fail
        try {
            testProcessModelByAnotherProcess(createTestProcessBpmnModel("twoTasksProcess"));
            fail("Expected exception was not thrown.");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), is("\nExpected: is <0L>\n     but: was <1L>"));
        }
    }

    @Test
    @Deployment(resources = {"org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml"})
    public void testGenerateProcessTestSemiAutomatically() {
        // Generate "user" events
        testStandardJUnitOneTaskProcess();
        List<EventLogEntry> eventLogEntries = managementService.getEventLogEntries(null, null);

        // automatic step to generate process test skeleton from already generated "user" events
        List<FlowNode> testFlowNodesSkeleton = createTestFlowNodesFromEventLogEntries(eventLogEntries);

        // add assertion manually
        ScriptTask assertTask = new ScriptTask();
        assertTask.setName("Assert task");
        assertTask.setId("assertTask");
        assertTask.setAsynchronous(true);
        assertTask.setScriptFormat("groovy");
        assertTask.setScript(
                "import org.flowable.engine.impl.context.Context;\n" +
                        "import static org.hamcrest.core.Is.is;\n" +
                        "import static org.flowable.examples.test.MatcherAssert.assertThat;\n" +
                        "\n" +
                        "assertThat(Context.getProcessEngineConfiguration().getRuntimeService().createProcessInstanceQuery().processInstanceId(processInstanceId).count(), is(0L));"
        );
        testFlowNodesSkeleton.add(assertTask);

        // generate BpmnModel from given skeleton
        BpmnModel bpmnModel = decorateTestSkeletonAsProcess(testFlowNodesSkeleton);

        // run process in the same way as ordinary process model test
        testProcessModelByAnotherProcess(bpmnModel);
    }

    private BpmnModel decorateTestSkeletonAsProcess(List<FlowNode> testFlowNodesSkeleton) {
        ArrayList<FlowNode> flowNodes = new ArrayList<>(testFlowNodesSkeleton);
        StartEvent startEvent = new StartEvent();
        startEvent.setId("start");
        flowNodes.add(0, startEvent);
        EndEvent endEvent = new EndEvent();
        endEvent.setId("theEnd");
        flowNodes.add(endEvent);

        BpmnModel model = new BpmnModel();
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        model.addProcess(process);
        model.setTargetNamespace("pUnit");
        process.setId("oneTaskProcessPUnitTest");
        process.setName("ProcessUnit test for oneTaskProcess");

        for (int i=0; i<flowNodes.size() -1; i++) {
            process.addFlowElement(flowNodes.get(i));
            process.addFlowElement(new SequenceFlow(flowNodes.get(i).getId(), flowNodes.get(i+1).getId()));
        }
        process.addFlowElement(flowNodes.get(flowNodes.size()-1));
        return model;
    }

    private List<FlowNode> createTestFlowNodesFromEventLogEntries(List<EventLogEntry> eventLogEntries) {
        List<FlowNode> flowNodes = new ArrayList<>();
        for (EventLogEntry eventLogEntry : eventLogEntries) {
            switch (eventLogEntry.getType()) {
                case "PROCESSINSTANCE_START" :
                    String processDefinitionId = eventLogEntry.getProcessDefinitionId();
                    ProcessDefinition processDefinition = this.repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
                    ScriptTask startProcess = new ScriptTask();
                    startProcess.setName("Start " + processDefinition.getKey() + " process");
                    startProcess.setId("startProcess-" + processDefinition.getKey());
                    startProcess.setScriptFormat("groovy");
                    startProcess.setAsynchronous(true);
                    startProcess.setScript(
                            "import org.flowable.engine.impl.context.Context;\n" +
                                    "\n" +
                                    "execution.setVariable('processInstanceId', Context.getProcessEngineConfiguration().getRuntimeService().createProcessInstanceBuilder().processDefinitionKey('" +
                                    processDefinition.getKey() + "').start().getId());"
                    );
                    flowNodes.add(startProcess);
                    break;
                case "TASK_COMPLETED" :
                    ScriptTask completeTask = new ScriptTask();
                    completeTask.setName("Complete task");
                    completeTask.setId("completeTask");
                    completeTask.setAsynchronous(true);
                    completeTask.setScriptFormat("groovy");
                    completeTask.setScript(
                            "import org.flowable.engine.impl.context.Context;\n" +
                                    "\n" +
                                    "taskId = Context.getProcessEngineConfiguration().getTaskService().createTaskQuery().processInstanceId(processInstanceId).singleResult().getId();\n" +
                                    "Context.getProcessEngineConfiguration().getTaskService().complete(taskId);"
                    );
                    flowNodes.add(completeTask);
                    break;
                default:
                    break;
            }


        }
        return flowNodes;
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
                "import org.flowable.engine.impl.context.Context;\n" +
                        "\n" +
                        "execution.setVariable('processInstanceId', Context.getProcessEngineConfiguration().getRuntimeService().createProcessInstanceBuilder().processDefinitionKey('" + processToTestDefinitionKey + "').start().getId());"
        );
        process.addFlowElement(startProcess);

        ScriptTask completeTask = new ScriptTask();
        completeTask.setName("Complete task");
        completeTask.setId("completeTask");
        completeTask.setAsynchronous(true);
        completeTask.setScriptFormat("groovy");
        completeTask.setScript(
                "import org.flowable.engine.impl.context.Context;\n" +
                        "\n" +
                        "taskId = Context.getProcessEngineConfiguration().getTaskService().createTaskQuery().processInstanceId(processInstanceId).singleResult().getId();\n" +
                        "Context.getProcessEngineConfiguration().getTaskService().complete(taskId);"
        );
        process.addFlowElement(completeTask);

        ScriptTask assertTask = new ScriptTask();
        assertTask.setName("Assert task");
        assertTask.setId("assertTask");
        assertTask.setAsynchronous(true);
        assertTask.setScriptFormat("groovy");
        assertTask.setScript(
                "import org.flowable.engine.impl.context.Context;\n" +
                        "import static org.hamcrest.core.Is.is;\n" +
                        "import static org.flowable.examples.test.MatcherAssert.assertThat;\n" +
                        "\n" +
                        "assertThat(Context.getProcessEngineConfiguration().getRuntimeService().createProcessInstanceQuery().processInstanceId(processInstanceId).count(), is(0L));"
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
