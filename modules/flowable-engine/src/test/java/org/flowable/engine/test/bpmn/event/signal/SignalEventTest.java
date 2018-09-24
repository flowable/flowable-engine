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

package org.flowable.engine.test.bpmn.event.signal;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.EventSubscriptionQueryImpl;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.validation.validator.Problems;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class SignalEventTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignal.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignal.bpmn20.xml" })
    public void testSignalCatchIntermediate() {
        runtimeService.startProcessInstanceByKey("catchSignal");

        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());

        runtimeService.startProcessInstanceByKey("throwSignal");

        assertEquals(0, createEventSubscriptionQuery().count());
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignalExpression.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignalExpression.bpmn20.xml" })
    public void testSignalCatchIntermediateExpression() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("mySignalName", "testSignal");
        runtimeService.startProcessInstanceByKey("catchSignal", variableMap);

        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());

        runtimeService.startProcessInstanceByKey("throwSignal", variableMap);

        assertEquals(0, createEventSubscriptionQuery().count());
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignalBoundary.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignal.bpmn20.xml" })
    public void testSignalCatchBoundary() {
        runtimeService.startProcessInstanceByKey("catchSignal");

        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());

        runtimeService.startProcessInstanceByKey("throwSignal");

        assertEquals(0, createEventSubscriptionQuery().count());
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignalBoundaryWithReceiveTask.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignal.bpmn20.xml" })
    public void testSignalCatchBoundaryWithVariables() {
        HashMap<String, Object> variables1 = new HashMap<>();
        variables1.put("processName", "catchSignal");
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("catchSignal", variables1);

        HashMap<String, Object> variables2 = new HashMap<>();
        variables2.put("processName", "throwSignal");
        runtimeService.startProcessInstanceByKey("throwSignal", variables2);

        assertEquals("catchSignal", runtimeService.getVariable(pi.getId(), "processName"));
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignal.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignalAsynch.bpmn20.xml" })
    public void testSignalCatchIntermediateAsynch() {

        runtimeService.startProcessInstanceByKey("catchSignal");

        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());

        runtimeService.startProcessInstanceByKey("throwSignal");

        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());

        // there is a job:
        assertEquals(1, managementService.createJobQuery().count());

        try {
            processEngineConfiguration.getClock().setCurrentTime(new Date(System.currentTimeMillis() + 1000));
            waitForJobExecutorToProcessAllJobs(10000, 100l);

            assertEquals(0, createEventSubscriptionQuery().count());
            assertEquals(0, runtimeService.createProcessInstanceQuery().count());
            assertEquals(0, managementService.createJobQuery().count());
        } finally {
            processEngineConfiguration.getClock().setCurrentTime(new Date());
        }

    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.catchMultipleSignals.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignal.bpmn20.xml", "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAbortSignal.bpmn20.xml" })
    public void testSignalCatchDifferentSignals() {

        runtimeService.startProcessInstanceByKey("catchSignal");

        assertEquals(2, createEventSubscriptionQuery().count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());

        runtimeService.startProcessInstanceByKey("throwAbort");

        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());

        org.flowable.task.api.Task taskAfterAbort = taskService.createTaskQuery().taskAssignee("gonzo").singleResult();
        assertNotNull(taskAfterAbort);
        taskService.complete(taskAfterAbort.getId());

        runtimeService.startProcessInstanceByKey("throwSignal");

        assertEquals(0, createEventSubscriptionQuery().count());
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
    }

    /**
     * Verifies the solution of https://jira.codehaus.org/browse/ACT-1309
     */
    @Test
    @Deployment
    public void testSignalBoundaryOnSubProcess() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("signalEventOnSubprocess");
        runtimeService.signalEventReceived("stopSignal");
        assertProcessEnded(pi.getProcessInstanceId());
    }

    @Test
    public void testDuplicateSignalNames() {
        try {
            repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTests.duplicateSignalNames.bpmn20.xml").deploy();
            fail("exception expected");
        } catch (Exception e) {
            if (!e.getMessage().contains(Problems.SIGNAL_DUPLICATE_NAME)) {
                fail("different exception expected");
            }
        }
    }

    @Test
    public void testNoSignalName() {
        try {
            repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTests.noSignalName.bpmn20.xml").deploy();
            fail("exception expected");
        } catch (Exception e) {
            if (!e.getMessage().contains(Problems.SIGNAL_MISSING_NAME)) {
                fail("different exception expected, was " + e.getMessage());
            }
        }
    }

    @Test
    public void testSignalNoId() {
        try {
            repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTests.signalNoId.bpmn20.xml").deploy();
            fail("exception expected");
        } catch (Exception e) {
            if (!e.getMessage().contains(Problems.SIGNAL_MISSING_ID)) {
                fail("different exception expected");
            }
        }
    }

    @Test
    public void testSignalNoRef() {
        try {
            repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTests.signalNoRef.bpmn20.xml").deploy();
            fail("exception expected");
        } catch (Exception e) {
            if (!e.getMessage().contains(Problems.SIGNAL_EVENT_MISSING_SIGNAL_REF)) {
                fail("different exception expected");
            }
        }
    }

    private EventSubscriptionQueryImpl createEventSubscriptionQuery() {
        return new EventSubscriptionQueryImpl(processEngineConfiguration.getCommandExecutor());
    }

    /**
     * TestCase to reproduce Issue ACT-1344
     */
    @Test
    @Deployment
    public void testNonInterruptingSignal() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("nonInterruptingSignalEvent");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).list();
        assertEquals(1, tasks.size());
        org.flowable.task.api.Task currentTask = tasks.get(0);
        assertEquals("My User Task", currentTask.getName());

        runtimeService.signalEventReceived("alert");

        tasks = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).list();
        assertEquals(2, tasks.size());

        for (org.flowable.task.api.Task task : tasks) {
            if (!task.getName().equals("My User Task") && !task.getName().equals("My Second User Task")) {
                fail("Expected: <My User Task> or <My Second User Task> but was <" + task.getName() + ">.");
            }
        }

        taskService.complete(taskService.createTaskQuery().taskName("My User Task").singleResult().getId());

        tasks = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).list();
        assertEquals(1, tasks.size());
        currentTask = tasks.get(0);
        assertEquals("My Second User Task", currentTask.getName());
    }

    /**
     * TestCase to reproduce Issue ACT-1344
     */
    @Test
    @Deployment
    public void testNonInterruptingSignalWithSubProcess() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("nonInterruptingSignalWithSubProcess");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).list();
        assertEquals(1, tasks.size());

        org.flowable.task.api.Task currentTask = tasks.get(0);
        assertEquals("Approve", currentTask.getName());

        runtimeService.signalEventReceived("alert");

        tasks = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).list();
        assertEquals(2, tasks.size());

        for (org.flowable.task.api.Task task : tasks) {
            if (!task.getName().equals("Approve") && !task.getName().equals("Review")) {
                fail("Expected: <Approve> or <Review> but was <" + task.getName() + ">.");
            }
        }

        taskService.complete(taskService.createTaskQuery().taskName("Approve").singleResult().getId());

        tasks = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).list();
        assertEquals(1, tasks.size());

        currentTask = tasks.get(0);
        assertEquals("Review", currentTask.getName());

        taskService.complete(taskService.createTaskQuery().taskName("Review").singleResult().getId());

        tasks = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).list();
        assertEquals(1, tasks.size());
    }

    @Test
    @Deployment
    public void testUseSignalForExceptionsBetweenParallelPaths() {
        runtimeService.startProcessInstanceByKey("processWithSignal");

        // First task should be to select the developers
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("Enter developers", task.getName());
        taskService.complete(task.getId(), CollectionUtil.singletonMap("developers", Arrays.asList("developerOne", "developerTwo", "developerThree")));

        // Should be three distinct tasks for each developer
        assertEquals("Develop specifications", taskService.createTaskQuery().taskAssignee("developerOne").singleResult().getName());
        assertEquals("Develop specifications", taskService.createTaskQuery().taskAssignee("developerTwo").singleResult().getName());
        assertEquals("Develop specifications", taskService.createTaskQuery().taskAssignee("developerThree").singleResult().getName());

        // Negotiate with client is a task for kermit
        task = taskService.createTaskQuery().taskAssignee("kermit").singleResult();
        assertEquals("Negotiate with client", task.getName());

        // When the kermit task is completed, it throws a signal which should
        // cancel the multi instance
        taskService.complete(task.getId(), CollectionUtil.singletonMap("negotationFailed", true));

        // No tasks should be open then and process should have ended
        assertEquals(0, taskService.createTaskQuery().count());
        assertEquals(0, runtimeService.createExecutionQuery().count());
    }

    @Test
    @Deployment
    public void testSignalWithProcessInstanceScope() {
        // Start the process that catches the signal
        ProcessInstance processInstanceCatch = runtimeService.startProcessInstanceByKey("processWithSignalCatch");
        assertEquals("userTaskWithSignalCatch", taskService.createTaskQuery().processInstanceId(processInstanceCatch.getId()).singleResult().getName());

        // Then start the process that will throw the signal
        runtimeService.startProcessInstanceByKey("processWithSignalThrow");

        // Since the signal is process instance scoped, the second process
        // shouldn't have proceeded in any way
        assertEquals("userTaskWithSignalCatch", taskService.createTaskQuery().processInstanceId(processInstanceCatch.getId()).singleResult().getName());

        // Let's try to trigger the catch using the API, that should also fail
        runtimeService.signalEventReceived("The Signal");
        assertEquals("userTaskWithSignalCatch", taskService.createTaskQuery().processInstanceId(processInstanceCatch.getId()).singleResult().getName());
    }

    @Test
    @Deployment
    public void testSignalWithGlobalScope() {
        // Start the process that catches the signal
        ProcessInstance processInstanceCatch = runtimeService.startProcessInstanceByKey("processWithSignalCatch");
        assertEquals("userTaskWithSignalCatch", taskService.createTaskQuery().processInstanceId(processInstanceCatch.getId()).singleResult().getName());

        // Then start the process that will throw thee signal
        runtimeService.startProcessInstanceByKey("processWithSignalThrow");

        // Since the signal is process instance scoped, the second process
        // shouldn't have proceeded in any way
        assertEquals("userTaskAfterSignalCatch", taskService.createTaskQuery().processInstanceId(processInstanceCatch.getId()).singleResult().getName());
    }

    @Test
    @Deployment
    public void testAsyncTriggeredSignalEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processWithSignalCatch");

        assertNotNull(processInstance);
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).signalEventSubscriptionName("The Signal").singleResult();
        assertNotNull(execution);
        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(2, runtimeService.createExecutionQuery().count());

        runtimeService.signalEventReceivedAsync("The Signal", execution.getId());

        assertEquals(1, managementService.createJobQuery().messages().count());

        waitForJobExecutorToProcessAllJobs(8000L, 200L);
        assertEquals(0, createEventSubscriptionQuery().count());
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
        assertEquals(0, managementService.createJobQuery().count());
    }

    @Test
    @Deployment
    public void testSignalUserTask() {
        runtimeService.startProcessInstanceByKey("catchSignal");
        Execution execution = runtimeService.createExecutionQuery().onlyChildExecutions().activityId("waitState").singleResult();

        assertNotNull(execution);

        try {
            runtimeService.trigger(execution.getId());
            fail("ActivitiException expected");
        } catch (FlowableException ae) {
            // Exception expected
        }

    }

    @Test
    public void testSignalStartEventFromProcess() {

        // Deploy test processes
        repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTest.testSignalStartEvent.bpmn20.xml").deploy();

        // Starting the process that fires the signal should start three process
        // instances that are listening on that signal
        runtimeService.startProcessInstanceByKey("processWithSignalThrow");

        // Verify
        assertEquals(3, runtimeService.createProcessInstanceQuery().count());
        assertEquals(3, taskService.createTaskQuery().count());

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        List<String> names = Arrays.asList("A", "B", "C");
        for (int i = 0; i < tasks.size(); i++) {
            assertEquals("Task in process " + names.get(i), tasks.get(i).getName());
        }

        // Start a process with a signal boundary event
        runtimeService.startProcessInstanceByKey("processWithSignalCatch");
        assertEquals(4, runtimeService.createProcessInstanceQuery().count());
        assertEquals(4, taskService.createTaskQuery().count());
        assertEquals(1, taskService.createTaskQuery().taskName("Task in process D").count());

        // Firing the signal should now trigger the one with the boundary event
        // too
        runtimeService.startProcessInstanceByKey("processWithSignalThrow");
        assertEquals(7, runtimeService.createProcessInstanceQuery().count());
        assertEquals(7, taskService.createTaskQuery().count());
        assertEquals(1, taskService.createTaskQuery().taskName("Task after signal").count());

        // Cleanup
        for (org.flowable.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }

    }

    @Test
    public void testSignalStartEventFromProcesAsync() {

        // Deploy test processes
        repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTest.testSignalStartEventAsync.bpmn20.xml").deploy();

        // Starting the process that fires the signal should start 1 process
        // instance that are listening on that signal, the others are done async
        runtimeService.startProcessInstanceByKey("processWithSignalThrow");

        // Verify
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
        assertEquals(0, taskService.createTaskQuery().count());

        assertEquals(3, managementService.createJobQuery().count());
        for (Job job : managementService.createJobQuery().list()) {
            managementService.executeJob(job.getId());
        }
        assertEquals(3, runtimeService.createProcessInstanceQuery().count());
        assertEquals(3, taskService.createTaskQuery().count());

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        List<String> names = Arrays.asList("A", "B", "C");
        for (int i = 0; i < tasks.size(); i++) {
            assertEquals("Task in process " + names.get(i), tasks.get(i).getName());
        }

        // Start a process with a signal boundary event
        runtimeService.startProcessInstanceByKey("processWithSignalCatch");
        assertEquals(4, runtimeService.createProcessInstanceQuery().count());
        assertEquals(4, taskService.createTaskQuery().count());
        assertEquals(1, taskService.createTaskQuery().taskName("Task in process D").count());

        // Firing again
        runtimeService.startProcessInstanceByKey("processWithSignalThrow");

        assertEquals(4, managementService.createJobQuery().count());
        for (Job job : managementService.createJobQuery().list()) {
            managementService.executeJob(job.getId());
        }
        assertEquals(7, runtimeService.createProcessInstanceQuery().count());
        assertEquals(7, taskService.createTaskQuery().count());
        assertEquals(1, taskService.createTaskQuery().taskName("Task after signal").count());

        // Cleanup
        for (org.flowable.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }

    }

    @Test
    public void testSignalStartEventFromAPI() {

        // Deploy test processes
        repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTest.testSignalStartEvent.bpmn20.xml").deploy();

        runtimeService.signalEventReceived("The Signal");

        // Verify
        assertEquals(3, runtimeService.createProcessInstanceQuery().count());
        assertEquals(3, taskService.createTaskQuery().count());

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        List<String> names = Arrays.asList("A", "B", "C");
        for (int i = 0; i < tasks.size(); i++) {
            assertEquals("Task in process " + names.get(i), tasks.get(i).getName());
        }

        // Start a process with a signal boundary event
        runtimeService.startProcessInstanceByKey("processWithSignalCatch");
        assertEquals(4, runtimeService.createProcessInstanceQuery().count());
        assertEquals(4, taskService.createTaskQuery().count());
        assertEquals(1, taskService.createTaskQuery().taskName("Task in process D").count());

        // Firing the signal should now trigger the one with the boundary event
        // too
        runtimeService.signalEventReceived("The Signal");
        assertEquals(7, runtimeService.createProcessInstanceQuery().count());
        assertEquals(7, taskService.createTaskQuery().count());
        assertEquals(1, taskService.createTaskQuery().taskName("Task after signal").count());

        // Cleanup
        for (org.flowable.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }

    }

    @Test
    public void testSignalStartEventFromAPIAsync() {

        // Deploy test processes
        repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTest.testSignalStartEventAsync.bpmn20.xml").deploy();

        runtimeService.signalEventReceivedAsync("The Signal");

        assertEquals(3, managementService.createJobQuery().count());
        for (Job job : managementService.createJobQuery().list()) {
            managementService.executeJob(job.getId());
        }
        assertEquals(3, runtimeService.createProcessInstanceQuery().count());
        assertEquals(3, taskService.createTaskQuery().count());

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        List<String> names = Arrays.asList("A", "B", "C");
        for (int i = 0; i < tasks.size(); i++) {
            assertEquals("Task in process " + names.get(i), tasks.get(i).getName());
        }

        // Start a process with a signal boundary event
        runtimeService.startProcessInstanceByKey("processWithSignalCatch");
        assertEquals(4, runtimeService.createProcessInstanceQuery().count());
        assertEquals(4, taskService.createTaskQuery().count());
        assertEquals(1, taskService.createTaskQuery().taskName("Task in process D").count());

        // Firing again
        runtimeService.signalEventReceivedAsync("The Signal");

        assertEquals(4, managementService.createJobQuery().count());
        for (Job job : managementService.createJobQuery().list()) {
            managementService.executeJob(job.getId());
        }
        assertEquals(7, runtimeService.createProcessInstanceQuery().count());
        assertEquals(7, taskService.createTaskQuery().count());
        assertEquals(1, taskService.createTaskQuery().taskName("Task after signal").count());

        // Cleanup
        for (org.flowable.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }

    }

    @Test
    @Deployment
    public void testEarlyFinishedProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callerProcess");
        assertNotNull(processInstance.getId());
    }

    @Test
    @Deployment
    public void testNoneEndEventAfterSignalInConcurrentProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("my-process");
        assertNotNull(processInstance);

        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("usertask1").singleResult();
        taskService.claim(task.getId(), "user");
        taskService.complete(task.getId());
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // trigger history comment handling when necessary
        }

        task = taskService.createTaskQuery().singleResult();

        assertEquals("usertask2", task.getTaskDefinitionKey());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignal.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignal.bpmn20.xml" })
    public void testSignalCatchSuspendedDefinition() {
        runtimeService.startProcessInstanceByKey("catchSignal");

        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());

        repositoryService.suspendProcessDefinitionByKey("catchSignal");

        runtimeService.startProcessInstanceByKey("throwSignal");

        assertEquals(0, createEventSubscriptionQuery().count());
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignal.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignal.bpmn20.xml" })
    public void testSignalCatchSuspendedDefinitionAndInstances() {
        runtimeService.startProcessInstanceByKey("catchSignal");

        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());

        repositoryService.suspendProcessDefinitionByKey("catchSignal", true, null);

        runtimeService.startProcessInstanceByKey("throwSignal");

        // signal catch event is still there
        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());

        repositoryService.activateProcessDefinitionByKey("catchSignal", true, null);

        runtimeService.startProcessInstanceByKey("throwSignal");

        // now the signal catch event is gone
        assertEquals(0, createEventSubscriptionQuery().count());
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignal.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignal.bpmn20.xml" })
    public void testSignalCatchSuspendedInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("catchSignal");

        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());

        runtimeService.suspendProcessInstanceById(processInstance.getId());

        runtimeService.startProcessInstanceByKey("throwSignal");

        // signal catch event is still there
        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());

        runtimeService.activateProcessInstanceById(processInstance.getId());

        runtimeService.startProcessInstanceByKey("throwSignal");

        // now the signal catch event is gone
        assertEquals(0, createEventSubscriptionQuery().count());
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
    }

    @Test
    public void testSignalStartEventWithSuspendedDefinition() {

        repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTest.testSignalStartEvent.bpmn20.xml").deploy();

        repositoryService.suspendProcessDefinitionByKey("processWithSignalStart1");

        try {
            runtimeService.startProcessInstanceByKey("processWithSignalThrow");
            fail("Suspended process definition should fail");
        } catch (FlowableException e) {
            // expected
        }

        // Verify
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());

        repositoryService.activateProcessDefinitionByKey("processWithSignalStart1");

        // Starting the process that fires the signal should start three process
        // instances that are listening on that signal
        runtimeService.startProcessInstanceByKey("processWithSignalThrow");

        // Verify
        assertEquals(3, runtimeService.createProcessInstanceQuery().count());
        assertEquals(3, taskService.createTaskQuery().count());

        // Cleanup
        for (org.flowable.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }

    }

    /**
     * Test case for https://activiti.atlassian.net/browse/ACT-1978
     */
    @Test
    public void testSignalDeleteOnRedeploy() {

        // Deploy test processes
        repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTest.testSignalStartEvent.bpmn20.xml").deploy();

        // Deploy new versions
        repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTest.testSignalStartEvent.bpmn20.xml").deploy();
        repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTest.testSignalStartEvent.bpmn20.xml").deploy();

        // Firing a signal start event should only start ONE process instance
        // This used to be two, due to subscriptions not being cleaned up
        runtimeService.signalEventReceived("The Signal");
        assertEquals(3, runtimeService.createProcessInstanceQuery().count());

        // Cleanup
        for (org.flowable.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @Deployment
    public void testSignalWaitOnUserTaskBoundaryEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("signal-wait");
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).signalEventSubscriptionName("waitsig").singleResult();
        assertNotNull(execution);
        runtimeService.signalEventReceived("waitsig", execution.getId());
        execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).signalEventSubscriptionName("waitsig").singleResult();
        assertNull(execution);
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);
        assertEquals("Wait2", task.getName());
    }

    /**
     * From https://forums.activiti.org/content/boundary-signal-causes-already-taking-transition
     */
    @Test
    @Deployment
    public void testSignalThrowAndCatchInSameTransaction() {

        String fileExistsVar = "fileexists";

        // remove mock file
        FileExistsMock.getInstance().removeFile();

        // create first instance
        ProcessInstance firstProcessInstance = runtimeService.startProcessInstanceByKey("signalBoundaryProcess");
        assertNotNull(firstProcessInstance);

        // task should be "add a file"
        org.flowable.task.api.Task firstTask = taskService.createTaskQuery().singleResult();
        assertEquals("Add a file", firstTask.getName());

        Map<String, Object> vars = runtimeService.getVariables(firstTask.getExecutionId());
        // file does not exists
        assertEquals(false, vars.get(fileExistsVar));

        // create second instance
        ProcessInstance secondProcessInstance = runtimeService.startProcessInstanceByKey("signalBoundaryProcess");
        assertNotNull(secondProcessInstance);

        // there should be two open tasks
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(2, tasks.size());

        // get current second task
        org.flowable.task.api.Task secondTask = taskService.createTaskQuery().processInstanceId(secondProcessInstance.getProcessInstanceId()).singleResult();
        // must be also in "add a file"
        assertEquals("Add a file", secondTask.getName());

        // file does not exists yet
        vars = runtimeService.getVariables(secondTask.getExecutionId());
        assertEquals(false, vars.get(fileExistsVar));

        // now, we "add a file"
        taskService.claim(firstTask.getId(), "user");
        // create the file
        FileExistsMock.getInstance().touchFile();
        
        taskService.complete(firstTask.getId());
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // trigger history comment handling when necessary
        }

        List<org.flowable.task.api.Task> usingTask = taskService.createTaskQuery().taskName("Use the file").list();
        assertEquals(1, usingTask.size());
    }

    @Test
    @Deployment
    public void testMultipleSignalStartEvents() {
        runtimeService.signalEventReceived("signal1");
        validateTaskCounts(1, 0, 0);

        runtimeService.signalEventReceived("signal2");
        validateTaskCounts(1, 1, 0);

        runtimeService.signalEventReceived("signal3");
        validateTaskCounts(1, 1, 1);

        runtimeService.signalEventReceived("signal1");
        validateTaskCounts(2, 1, 1);

        runtimeService.signalEventReceived("signal1");
        validateTaskCounts(3, 1, 1);

        runtimeService.signalEventReceived("signal3");
        validateTaskCounts(3, 1, 2);
    }
    
    @Test
    @Deployment
    public void testSingleSignalCatchAfterEventGateway() {
        String processInstanceId = runtimeService.startProcessInstanceByKey("testSignalAfterEventGateway").getId();
        assertEquals(1, runtimeService.createEventSubscriptionQuery().processInstanceId(processInstanceId).count());
        runtimeService.signalEventReceived("mySignal");
        
        assertEquals(1, taskService.createTaskQuery().processInstanceId(processInstanceId).count());
    }

    private void validateTaskCounts(long taskACount, long taskBCount, long taskCCount) {
        assertEquals(taskACount, taskService.createTaskQuery().taskName("Task A").count());
        assertEquals(taskBCount, taskService.createTaskQuery().taskName("Task B").count());
        assertEquals(taskCCount, taskService.createTaskQuery().taskName("Task C").count());
    }

}
