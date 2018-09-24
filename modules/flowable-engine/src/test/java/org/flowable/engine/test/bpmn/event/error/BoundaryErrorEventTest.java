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
package org.flowable.engine.test.bpmn.event.error;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class BoundaryErrorEventTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testCatchErrorOnEmbeddedSubprocess() {
        runtimeService.startProcessInstanceByKey("boundaryErrorOnEmbeddedSubprocess");

        // After process start, usertask in subprocess should exist
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("subprocessTask", task.getName());

        // After task completion, error end event is reached and caught
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().singleResult();
        assertEquals("task after catching the error", task.getName());
    }

    @Test
    public void testThrowErrorWithEmptyErrorCode() {
        try {
            repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testThrowErrorWithEmptyErrorCode.bpmn20.xml").deploy();
            fail("ActivitiException expected");
        } catch (FlowableException re) {
        }
    }

    @Test
    @Deployment
    public void testCatchErrorOnEmbeddedSubprocessWithEmptyErrorCode() {
        testCatchErrorOnEmbeddedSubprocess();
    }

    @Test
    @Deployment
    public void testCatchErrorOnEmbeddedSubprocessWithoutErrorCode() {
        testCatchErrorOnEmbeddedSubprocess();
    }

    @Test
    @Deployment
    public void testCatchErrorOfInnerSubprocessOnOuterSubprocess() {
        runtimeService.startProcessInstanceByKey("boundaryErrorTest");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("Inner subprocess task 1", tasks.get(0).getName());
        assertEquals("Inner subprocess task 2", tasks.get(1).getName());

        // Completing task 2, will cause the end error event to throw error with code 123
        taskService.complete(tasks.get(1).getId());
        taskService.createTaskQuery().list();
        org.flowable.task.api.Task taskAfterError = taskService.createTaskQuery().singleResult();
        assertEquals("task outside subprocess", taskAfterError.getName());
    }

    @Test
    @Deployment
    public void testCatchErrorInConcurrentEmbeddedSubprocesses() {
        assertErrorCaughtInConcurrentEmbeddedSubprocesses("boundaryEventTestConcurrentSubprocesses");
    }

    @Test
    @Deployment
    public void testCatchErrorInConcurrentEmbeddedSubprocessesThrownByScriptTask() {
        assertErrorCaughtInConcurrentEmbeddedSubprocesses("catchErrorInConcurrentEmbeddedSubprocessesThrownByScriptTask");
    }

    private void assertErrorCaughtInConcurrentEmbeddedSubprocesses(String processDefinitionKey) {
        // Completing task A will lead to task D
        String procId = runtimeService.startProcessInstanceByKey(processDefinitionKey).getId();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("task A", tasks.get(0).getName());
        assertEquals("task B", tasks.get(1).getName());
        taskService.complete(tasks.get(0).getId());
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("task D", task.getName());
        taskService.complete(task.getId());
        assertProcessEnded(procId);

        // Completing task B will lead to task C
        runtimeService.startProcessInstanceByKey(processDefinitionKey).getId();
        tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("task A", tasks.get(0).getName());
        assertEquals("task B", tasks.get(1).getName());
        taskService.complete(tasks.get(1).getId());

        tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("task A", tasks.get(0).getName());
        assertEquals("task C", tasks.get(1).getName());
        taskService.complete(tasks.get(1).getId());
        task = taskService.createTaskQuery().singleResult();
        assertEquals("task A", task.getName());

        taskService.complete(task.getId());
        task = taskService.createTaskQuery().singleResult();
        assertEquals("task D", task.getName());
    }

    @Test
    @Deployment
    public void testDeeplyNestedErrorThrown() {

        // Input = 1 -> error1 will be thrown, which will destroy ALL BUT ONE
        // subprocess, which leads to an end event, which ultimately leads to
        // ending the process instance
        String procId = runtimeService.startProcessInstanceByKey("deeplyNestedErrorThrown").getId();
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("Nested task", task.getName());
        taskService.complete(task.getId(), CollectionUtil.singletonMap("input", 1));
        assertProcessEnded(procId);

        // Input == 2 -> error2 will be thrown, leading to a userTask outside
        // all subprocesses
        procId = runtimeService.startProcessInstanceByKey("deeplyNestedErrorThrown").getId();
        task = taskService.createTaskQuery().singleResult();
        assertEquals("Nested task", task.getName());
        taskService.complete(task.getId(), CollectionUtil.singletonMap("input", 2));
        task = taskService.createTaskQuery().singleResult();
        assertEquals("task after catch", task.getName());
        taskService.complete(task.getId());
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testDeeplyNestedErrorThrownOnlyAutomaticSteps() {
        // input == 1 -> error2 is thrown -> caught on subprocess2 -> end event
        // in subprocess -> proc inst end 1
        String procId = runtimeService.startProcessInstanceByKey("deeplyNestedErrorThrown", CollectionUtil.singletonMap("input", 1)).getId();
        assertProcessEnded(procId);

        HistoricProcessInstance hip;
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            hip = historyService.createHistoricProcessInstanceQuery().processInstanceId(procId).singleResult();
            assertEquals("processEnd1", hip.getEndActivityId());
        }
        // input == 2 -> error2 is thrown -> caught on subprocess1 -> proc inst
        // end 2
        procId = runtimeService.startProcessInstanceByKey("deeplyNestedErrorThrown", CollectionUtil.singletonMap("input", 1)).getId();
        assertProcessEnded(procId);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            hip = historyService.createHistoricProcessInstanceQuery().processInstanceId(procId).singleResult();
            assertEquals("processEnd1", hip.getEndActivityId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorOnCallActivity-parent.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.subprocess.bpmn20.xml" })
    public void testCatchErrorOnCallActivity() {
        String procId = runtimeService.startProcessInstanceByKey("catchErrorOnCallActivity").getId();
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("Task in subprocess", task.getName());

        // Completing the task will reach the end error event,
        // which is caught on the call activity boundary
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().singleResult();
        assertEquals("Escalated Task", task.getName());

        // Completing the task will end the process instance
        taskService.complete(task.getId());
        assertProcessEnded(procId);
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.multipleErrorsCatch.bpmn20.xml",
        "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.multipleErrorsThrow.bpmn20.xml" })
    public void testCatchMultipleErrorsOnCallActivity() {
        String procId = runtimeService.startProcessInstanceByKey("catchError").getId();
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("specificErrorTask", task.getTaskDefinitionKey());
        
        taskService.complete(task.getId());
        
        assertProcessEnded(procId);
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.multipleErrorsCatch.bpmn20.xml",
        "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.multipleErrorsThrow2.bpmn20.xml" })
    public void testCatchMultipleErrorsOnCallActivityNoSpecificError() {
        String procId = runtimeService.startProcessInstanceByKey("catchError").getId();
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("emptyErrorTask", task.getTaskDefinitionKey());
        
        taskService.complete(task.getId());
        
        assertProcessEnded(procId);
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.callActivityWithErrorEndEventCatch.bpmn20.xml",
        "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.callActivityWithErrorEndEventThrow.bpmn20.xml" })
    public void testCatchErrorEndEventOnCallActivity() {
        String procId = runtimeService.startProcessInstanceByKey("catchError").getId();
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("specificErrorTask", task.getTaskDefinitionKey());
        
        taskService.complete(task.getId());
        
        assertProcessEnded(procId);
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.callActivityWithErrorEndEventCatch.bpmn20.xml",
        "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.callActivityWithErrorEndEventThrow2.bpmn20.xml" })
    public void testCatchErrorEndEventOnCallActivityNoSpecificError() {
        String procId = runtimeService.startProcessInstanceByKey("catchError").getId();
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("emptyErrorTask", task.getTaskDefinitionKey());
        
        taskService.complete(task.getId());
        
        assertProcessEnded(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.subprocess.bpmn20.xml" })
    public void testUncaughtError() {
        runtimeService.startProcessInstanceByKey("simpleSubProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("Task in subprocess", task.getName());

        try {
            // Completing the task will reach the end error event, which is never caught in the process
            taskService.complete(task.getId());
            fail("No catching boundary event found for error with errorCode 'myError', neither in same process nor in parent process but no Exception is thrown");
        } catch (BpmnError e) {
            assertTextPresent("No catching boundary event found for error with errorCode 'myError', neither in same process nor in parent process", e.getMessage());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testUncaughtErrorOnCallActivity-parent.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.subprocess.bpmn20.xml" })
    public void testUncaughtErrorOnCallActivity() {
        runtimeService.startProcessInstanceByKey("uncaughtErrorOnCallActivity");
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("Task in subprocess", task.getName());

        try {
            // Completing the task will reach the end error event,
            // which is never caught in the process
            taskService.complete(task.getId());
        } catch (BpmnError e) {
            assertTextPresent("No catching boundary event found for error with errorCode 'myError', neither in same process nor in parent process", e.getMessage());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByCallActivityOnSubprocess.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.subprocess.bpmn20.xml" })
    public void testCatchErrorThrownByCallActivityOnSubprocess() {
        String procId = runtimeService.startProcessInstanceByKey("catchErrorOnSubprocess").getId();
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("Task in subprocess", task.getName());

        // Completing the task will reach the end error event,
        // which is caught on the call activity boundary
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().singleResult();
        assertEquals("Escalated Task", task.getName());

        // Completing the task will end the process instance
        taskService.complete(task.getId());
        assertProcessEnded(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByCallActivityOnCallActivity.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.subprocess2ndLevel.bpmn20.xml", "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.subprocess.bpmn20.xml" })
    public void testCatchErrorThrownByCallActivityOnCallActivity() throws InterruptedException {
        String procId = runtimeService.startProcessInstanceByKey("catchErrorOnCallActivity2ndLevel").getId();

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("Task in subprocess", task.getName());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().singleResult();
        assertEquals("Escalated Task", task.getName());

        // Completing the task will end the process instance
        taskService.complete(task.getId());
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testCatchErrorOnParallelMultiInstance() {
        String procId = runtimeService.startProcessInstanceByKey("catchErrorOnParallelMi").getId();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(5, tasks.size());

        // Complete two subprocesses, just to make it a bit more complex
        Map<String, Object> vars = new HashMap<>();
        vars.put("throwError", false);
        taskService.complete(tasks.get(2).getId(), vars);
        taskService.complete(tasks.get(3).getId(), vars);

        // Reach the error event
        vars.put("throwError", true);
        taskService.complete(tasks.get(1).getId(), vars);

        assertEquals(0, taskService.createTaskQuery().count());
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testCatchErrorOnSequentialMultiInstance() {
        String procId = runtimeService.startProcessInstanceByKey("catchErrorOnSequentialMi").getId();

        // complete one task
        Map<String, Object> vars = new HashMap<>();
        vars.put("throwError", false);
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId(), vars);

        // complete second task and throw error
        vars.put("throwError", true);
        task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId(), vars);

        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testCatchErrorThrownByJavaDelegateOnServiceTask() {
        String procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnServiceTask").getId();
        assertThatErrorHasBeenCaught(procId);
    }

    @Test
    @Deployment
    public void testCatchErrorThrownByJavaDelegateOnServiceTaskNotCancelActivity() {
        String procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnServiceTaskNotCancelActiviti").getId();
        assertThatErrorHasBeenCaught(procId);
    }

    @Test
    @Deployment
    public void testCatchErrorThrownByJavaDelegateOnServiceTaskWithErrorCode() {
        String procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnServiceTaskWithErrorCode").getId();
        assertThatErrorHasBeenCaught(procId);
    }

    @Test
    @Deployment
    public void testCatchErrorThrownByJavaDelegateOnEmbeddedSubProcess() {
        String procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnEmbeddedSubProcess").getId();
        assertThatErrorHasBeenCaught(procId);
    }

    @Test
    @Deployment
    public void testCatchErrorThrownByJavaDelegateOnEmbeddedSubProcessInduction() {
        String procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnEmbeddedSubProcessInduction").getId();
        assertThatErrorHasBeenCaught(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByJavaDelegateOnCallActivity-parent.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByJavaDelegateOnCallActivity-child.bpmn20.xml" })
    public void testCatchErrorThrownByJavaDelegateOnCallActivity() {
        String procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnCallActivity-parent").getId();
        assertThatErrorHasBeenCaught(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByJavaDelegateOnCallActivity-child.bpmn20.xml" })
    public void testUncaughtErrorThrownByJavaDelegateOnServiceTask() {
        try {
            runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnCallActivity-child");
        } catch (BpmnError e) {
            assertTextPresent("No catching boundary event found for error with errorCode '23', neither in same process nor in parent process", e.getMessage());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testUncaughtErrorThrownByJavaDelegateOnCallActivity-parent.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByJavaDelegateOnCallActivity-child.bpmn20.xml" })
    public void testUncaughtErrorThrownByJavaDelegateOnCallActivity() {
        try {
            runtimeService.startProcessInstanceByKey("uncaughtErrorThrownByJavaDelegateOnCallActivity-parent");
        } catch (BpmnError e) {
            assertTextPresent("No catching boundary event found for error with errorCode '23', neither in same process nor in parent process", e.getMessage());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorOnGroovyScriptTask.bpmn20.xml" })
    public void testCatchErrorOnGroovyScriptTask() {
        String procId = runtimeService.startProcessInstanceByKey("catchErrorOnScriptTask").getId();
        assertProcessEnded(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorOnJavaScriptScriptTask.bpmn20.xml" })
    public void testCatchErrorOnJavaScriptScriptTask() {
        String procId = runtimeService.startProcessInstanceByKey("catchErrorOnScriptTask").getId();
        assertProcessEnded(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testUncaughtErrorOnScriptTaskWithEmptyErrorEventDefinition.bpmn20.xml" })
    public void testUncaughtErrorOnScriptTaskWithEmptyErrorEventDefinition() {
        String procId = runtimeService.startProcessInstanceByKey("uncaughtErrorOnScriptTaskWithEmptyErrorEventDefinition").getId();
        assertProcessEnded(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testUncaughtErrorOnScriptTask.bpmn20.xml" })
    public void testUncaughtErrorOnScriptTask() {
        try {
            String procId = runtimeService.startProcessInstanceByKey("uncaughtErrorOnScriptTask").getId();
            fail("The script throws error event with errorCode 'errorUncaught', but no catching boundary event was defined. An exception is expected which did not occur");
            assertProcessEnded(procId);
        } catch (BpmnError e) {
            assertTextPresent("No catching boundary event found for error with errorCode 'errorUncaught', neither in same process nor in parent process", e.getMessage());
        }
    }

    @Test
    @Deployment
    public void testCatchErrorThrownByJavaDelegateOnMultiInstanceServiceTaskSequential() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("executionsBeforeError", 2);
        String procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnMultiInstanceServiceTaskSequential", variables).getId();
        assertThatErrorHasBeenCaught(procId);
    }

    @Test
    @Deployment
    public void testCatchErrorThrownByJavaDelegateOnMultiInstanceServiceTaskParallel() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("executionsBeforeError", 2);
        String procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnMultiInstanceServiceTaskParallel", variables).getId();
        assertThatErrorHasBeenCaught(procId);
    }

    @Test
    @Deployment
    public void testErrorThrownByJavaDelegateNotCaughtByOtherEventType() {
        String procId = runtimeService.startProcessInstanceByKey("testErrorThrownByJavaDelegateNotCaughtByOtherEventType").getId();
        assertThatErrorHasBeenCaught(procId);
    }

    private void assertThatErrorHasBeenCaught(String procId) {
        // The service task will throw an error event,
        // which is caught on the service task boundary
        assertEquals("No tasks found in task list.", 1, taskService.createTaskQuery().count());
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("Escalated Task", task.getName());

        // Completing the task will end the process instance
        taskService.complete(task.getId());
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testConcurrentExecutionsInterruptedOnDestroyScope() {

        // this test makes sure that if the first concurrent execution destroys
        // the scope (due to the interrupting boundary catch), the second concurrent
        // execution does not move forward.

        // if the test fails, it produces a constraint violation in db.

        runtimeService.startProcessInstanceByKey("process");
    }

    @Test
    @Deployment
    public void testCatchErrorThrownByExpressionOnServiceTask() {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("bpmnErrorBean", new BpmnErrorBean());
        String procId = runtimeService.startProcessInstanceByKey("testCatchErrorThrownByExpressionOnServiceTask", variables).getId();
        assertThatErrorHasBeenCaught(procId);
    }

    @Test
    @Deployment
    public void testCatchErrorThrownByDelegateExpressionOnServiceTask() {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("bpmnErrorBean", new BpmnErrorBean());
        String procId = runtimeService.startProcessInstanceByKey("testCatchErrorThrownByDelegateExpressionOnServiceTask", variables).getId();
        assertThatErrorHasBeenCaught(procId);
    }

    @Test
    @Deployment
    public void testCatchErrorThrownByJavaDelegateProvidedByDelegateExpressionOnServiceTask() {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("bpmnErrorBean", new BpmnErrorBean());
        String procId = runtimeService.startProcessInstanceByKey("testCatchErrorThrownByJavaDelegateProvidedByDelegateExpressionOnServiceTask", variables).getId();
        assertThatErrorHasBeenCaught(procId);
    }

}
