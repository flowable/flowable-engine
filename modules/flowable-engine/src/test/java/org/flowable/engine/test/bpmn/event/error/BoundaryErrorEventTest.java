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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
        assertThat(task.getName()).isEqualTo("subprocessTask");

        // After task completion, error end event is reached and caught
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("task after catching the error");
    }

    @Test
    public void testThrowErrorWithEmptyErrorCode() {
        assertThatThrownBy(() -> repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testThrowErrorWithEmptyErrorCode.bpmn20.xml").deploy())
                .as("exception expected, as there is no matching exception map")
                .isInstanceOf(FlowableException.class);
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
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Inner subprocess task 1", "Inner subprocess task 2");

        // Completing task 2, will cause the end error event to throw error with code 123
        taskService.complete(tasks.get(1).getId());
        taskService.createTaskQuery().list();
        org.flowable.task.api.Task taskAfterError = taskService.createTaskQuery().singleResult();
        assertThat(taskAfterError.getName()).isEqualTo("task outside subprocess");
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
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("task A", "task B");
        taskService.complete(tasks.get(0).getId());
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("task D");
        taskService.complete(task.getId());
        assertProcessEnded(procId);

        // Completing task B will lead to task C
        runtimeService.startProcessInstanceByKey(processDefinitionKey).getId();
        tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("task A", "task B");
        taskService.complete(tasks.get(1).getId());

        tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("task A", "task C");
        taskService.complete(tasks.get(1).getId());
        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("task A");

        taskService.complete(task.getId());
        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("task D");
    }

    @Test
    @Deployment
    public void testDeeplyNestedErrorThrown() {

        // Input = 1 -> error1 will be thrown, which will destroy ALL BUT ONE
        // subprocess, which leads to an end event, which ultimately leads to
        // ending the process instance
        String procId = runtimeService.startProcessInstanceByKey("deeplyNestedErrorThrown").getId();
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Nested task");
        taskService.complete(task.getId(), CollectionUtil.singletonMap("input", 1));
        assertProcessEnded(procId);

        // Input == 2 -> error2 will be thrown, leading to a userTask outside
        // all subprocesses
        procId = runtimeService.startProcessInstanceByKey("deeplyNestedErrorThrown").getId();
        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Nested task");
        taskService.complete(task.getId(), CollectionUtil.singletonMap("input", 2));
        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("task after catch");
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
            assertThat(hip.getEndActivityId()).isEqualTo("processEnd1");
        }
        // input == 2 -> error2 is thrown -> caught on subprocess1 -> proc inst
        // end 2
        procId = runtimeService.startProcessInstanceByKey("deeplyNestedErrorThrown", CollectionUtil.singletonMap("input", 1)).getId();
        assertProcessEnded(procId);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            hip = historyService.createHistoricProcessInstanceQuery().processInstanceId(procId).singleResult();
            assertThat(hip.getEndActivityId()).isEqualTo("processEnd1");
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorOnCallActivity-parent.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.subprocess.bpmn20.xml" })
    public void testCatchErrorOnCallActivity() {
        String procId = runtimeService.startProcessInstanceByKey("catchErrorOnCallActivity").getId();
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Task in subprocess");

        // Completing the task will reach the end error event,
        // which is caught on the call activity boundary
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Escalated Task");

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
        assertThat(task.getTaskDefinitionKey()).isEqualTo("specificErrorTask");
        
        taskService.complete(task.getId());
        
        assertProcessEnded(procId);
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.multipleErrorsCatch.bpmn20.xml",
        "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.multipleErrorsThrow2.bpmn20.xml" })
    public void testCatchMultipleErrorsOnCallActivityNoSpecificError() {
        String procId = runtimeService.startProcessInstanceByKey("catchError").getId();
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("emptyErrorTask");
        
        taskService.complete(task.getId());
        
        assertProcessEnded(procId);
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.callActivityWithErrorEndEventCatch.bpmn20.xml",
        "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.callActivityWithErrorEndEventThrow.bpmn20.xml" })
    public void testCatchErrorEndEventOnCallActivity() {
        String procId = runtimeService.startProcessInstanceByKey("catchError").getId();
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("specificErrorTask");
        
        taskService.complete(task.getId());
        
        assertProcessEnded(procId);
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.callActivityWithErrorEndEventCatch.bpmn20.xml",
        "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.callActivityWithErrorEndEventThrow2.bpmn20.xml" })
    public void testCatchErrorEndEventOnCallActivityNoSpecificError() {
        String procId = runtimeService.startProcessInstanceByKey("catchError").getId();
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("emptyErrorTask");
        
        taskService.complete(task.getId());
        
        assertProcessEnded(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.subprocess.bpmn20.xml" })
    public void testUncaughtError() {
        runtimeService.startProcessInstanceByKey("simpleSubProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Task in subprocess");

        // Completing the task will reach the end error event, which is never caught in the process
        assertThatThrownBy(() -> taskService.complete(task.getId()))
                .isExactlyInstanceOf(BpmnError.class)
                .hasMessage("No catching boundary event found for error with errorCode 'myError', neither in same process nor in parent process");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testUncaughtErrorOnCallActivity-parent.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.subprocess.bpmn20.xml" })
    public void testUncaughtErrorOnCallActivity() {
        runtimeService.startProcessInstanceByKey("uncaughtErrorOnCallActivity");
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Task in subprocess");

        // Completing the task will reach the end error event, which is never caught in the process
        assertThatThrownBy(() -> taskService.complete(task.getId()))
                .isExactlyInstanceOf(BpmnError.class)
                .hasMessage("No catching boundary event found for error with errorCode 'myError', neither in same process nor in parent process");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByCallActivityOnSubprocess.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.subprocess.bpmn20.xml" })
    public void testCatchErrorThrownByCallActivityOnSubprocess() {
        String procId = runtimeService.startProcessInstanceByKey("catchErrorOnSubprocess").getId();
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Task in subprocess");

        // Completing the task will reach the end error event,
        // which is caught on the call activity boundary
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Escalated Task");

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
        assertThat(task.getName()).isEqualTo("Task in subprocess");

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Escalated Task");

        // Completing the task will end the process instance
        taskService.complete(task.getId());
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testCatchErrorOnParallelMultiInstance() {
        String procId = runtimeService.startProcessInstanceByKey("catchErrorOnParallelMi").getId();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(5);

        // Complete two subprocesses, just to make it a bit more complex
        Map<String, Object> vars = new HashMap<>();
        vars.put("throwError", false);
        taskService.complete(tasks.get(2).getId(), vars);
        taskService.complete(tasks.get(3).getId(), vars);

        // Reach the error event
        vars.put("throwError", true);
        taskService.complete(tasks.get(1).getId(), vars);

        assertThat(taskService.createTaskQuery().count()).isZero();
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
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnCallActivity-child"))
                .isExactlyInstanceOf(BpmnError.class)
                .hasMessage("No catching boundary event found for error with errorCode '23', neither in same process nor in parent process");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testUncaughtErrorThrownByJavaDelegateOnCallActivity-parent.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByJavaDelegateOnCallActivity-child.bpmn20.xml" })
    public void testUncaughtErrorThrownByJavaDelegateOnCallActivity() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("uncaughtErrorThrownByJavaDelegateOnCallActivity-parent"))
                .isExactlyInstanceOf(BpmnError.class)
                .hasMessage("No catching boundary event found for error with errorCode '23', neither in same process nor in parent process");
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
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("uncaughtErrorOnScriptTask").getId())
                .isExactlyInstanceOf(BpmnError.class)
                .hasMessage("No catching boundary event found for error with errorCode 'errorUncaught', neither in same process nor in parent process");
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
        assertThat(taskService.createTaskQuery().count()).as("No tasks found in task list.").isEqualTo(1);
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Escalated Task");

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
    public void testCatchErrorThrownByExpressionWithFutureOnServiceTask() {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("bpmnErrorBean", new BpmnErrorBean());
        String procId = runtimeService.startProcessInstanceByKey("testCatchErrorThrownByExpressionWithFutureOnServiceTask", variables).getId();
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

    @Deployment
    @ParameterizedTest(name = "JavaFutureDelegate via class throws error in {0} should escalate with {1}")
    @MethodSource("argumentsForCatchErrorThrownByFutureJavaDelegateOnServiceTaskWithErrorCode")
    public void testCatchErrorThrownByFutureJavaDelegateOnServiceTaskWithErrorCode(String throwErrorIn, String expectedEscalatedTaskName) {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("catchErrorThrownByFutureJavaDelegateOnServiceTaskWithErrorCode")
                .variable("throwErrorIn", throwErrorIn)
                .start()
                .getId();

        // The service task will throw an error event,
        // which is caught on the service task boundary
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo(expectedEscalatedTaskName);
    }

    @Deployment
    @ParameterizedTest(name = "JavaFutureDelegate via expression throws error in {0} should escalate with {1}")
    @MethodSource("argumentsForCatchErrorThrownByFutureJavaDelegateOnServiceTaskWithErrorCode")
    public void testCatchErrorThrownByFutureJavaDelegateProvidedByDelegateExpressionOnServiceTask(String throwErrorIn, String expectedEscalatedTaskName) {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testCatchErrorThrownByFutureJavaDelegateProvidedByDelegateExpressionOnServiceTask")
                .transientVariable("bpmnErrorBean", new ThrowBpmnErrorFutureDelegate())
                .variable("throwErrorIn", throwErrorIn)
                .start()
                .getId();

        // The service task will throw an error event,
        // which is caught on the service task boundary
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo(expectedEscalatedTaskName);
    }

    static Stream<Arguments> argumentsForCatchErrorThrownByFutureJavaDelegateOnServiceTaskWithErrorCode() {
        return Stream.of(
                Arguments.of("beforeExecution", "Escalated Task for before execution"),
                Arguments.of("execute", "Escalated Task for execute"),
                Arguments.of("afterExecution", "Escalated Task for after execution")
        );
    }

}
