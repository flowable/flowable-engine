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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.data.MapEntry.entry;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.groups.Tuple;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryImpl;
import org.flowable.eventsubscription.service.impl.persistence.entity.SignalEventSubscriptionEntity;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.flowable.validation.validator.Problems;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class SignalEventTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignal.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignal.bpmn20.xml" })
    public void testSignalCatchIntermediate() {
        runtimeService.startProcessInstanceByKey("catchSignal");

        assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        runtimeService.startProcessInstanceByKey("throwSignal");

        assertThat(createEventSubscriptionQuery().count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignalExpression.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignalExpression.bpmn20.xml" })
    public void testSignalCatchIntermediateExpression() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("mySignalName", "testSignal");
        runtimeService.startProcessInstanceByKey("catchSignal", variableMap);

        assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        runtimeService.startProcessInstanceByKey("throwSignal", variableMap);

        assertThat(createEventSubscriptionQuery().count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignalWithInParameters.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignalWithOutParameters.bpmn20.xml" })
    public void testSignalCatchIntermediateWithParameters() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("catchSignal");

        assertThat(createEventSubscriptionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        Map<String, Object> signalVariableMap = new HashMap<>();
        signalVariableMap.put("textVar", "John Doe");
        signalVariableMap.put("numberVar", 1);
        runtimeService.startProcessInstanceByKey("throwSignal", signalVariableMap);

        assertThat(createEventSubscriptionQuery().count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        
        Map<String, Object> variableMap = runtimeService.getVariables(processInstance.getId());
        assertThat(variableMap)
                .containsOnly(
                        entry("myNewTextVar", "John Doe"),
                        entry("myNewNumberVar", 1));
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignalBoundary.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignal.bpmn20.xml" })
    public void testSignalCatchBoundary() {
        runtimeService.startProcessInstanceByKey("catchSignal");

        assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        runtimeService.startProcessInstanceByKey("throwSignal");

        assertThat(createEventSubscriptionQuery().count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
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

        assertThat(runtimeService.getVariable(pi.getId(), "processName")).isEqualTo("catchSignal");
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignalBoundaryWithInParameters.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignalWithOutParameters.bpmn20.xml" })
    public void testSignalCatchBoundaryWithParameters() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("catchSignal");

        assertThat(createEventSubscriptionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        assertThat(createEventSubscriptionQuery().withoutProcessInstanceId().count()).isZero();
        assertThat(createEventSubscriptionQuery().withoutScopeId().count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        Map<String, Object> signalVariableMap = new HashMap<>();
        signalVariableMap.put("textVar", "John Doe");
        signalVariableMap.put("numberVar", 1);
        runtimeService.startProcessInstanceByKey("throwSignal", signalVariableMap);

        assertThat(createEventSubscriptionQuery().count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        
        Map<String, Object> variableMap = runtimeService.getVariables(processInstance.getId());
        assertThat(variableMap)
                .containsOnly(
                        entry("myNewTextVar", "John Doe"),
                        entry("myNewNumberVar", 1));
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignalBoundaryWithInParameters.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignalWithOutParameterExpressions.bpmn20.xml" })
    public void testSignalCatchBoundaryWithParameterExpressions() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("catchSignal");

        assertThat(createEventSubscriptionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        Map<String, Object> signalVariableMap = new HashMap<>();
        signalVariableMap.put("firstNameVar", "John");
        signalVariableMap.put("lastNameVar", "Doe");
        runtimeService.startProcessInstanceByKey("throwSignal", signalVariableMap);

        assertThat(createEventSubscriptionQuery().count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        
        Map<String, Object> variableMap = runtimeService.getVariables(processInstance.getId());
        assertThat(variableMap)
                .containsOnly(
                        entry("myNewTextVar", "John Doe"),
                        entry("myNewNumberVar", 2L));
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignal.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignalAsynch.bpmn20.xml" })
    public void testSignalCatchIntermediateAsync() {

        runtimeService.startProcessInstanceByKey("catchSignal");

        assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        runtimeService.startProcessInstanceByKey("throwSignal");

        assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        // there is a job:
        assertThat(managementService.createJobQuery().count()).isEqualTo(1);

        try {
            processEngineConfiguration.getClock().setCurrentTime(new Date(System.currentTimeMillis() + 1000));
            waitForJobExecutorToProcessAllJobs(10000, 100L);

            assertThat(createEventSubscriptionQuery().count()).isZero();
            assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
            assertThat(managementService.createJobQuery().count()).isZero();
        } finally {
            processEngineConfiguration.getClock().setCurrentTime(new Date());
        }
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignalBoundaryWithInParameters.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignalAsynchWithOutParameters.bpmn20.xml" })
    public void testSignalCatchBoundaryAsyncWithParameters() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("catchSignal");

        assertThat(createEventSubscriptionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        Map<String, Object> signalVariableMap = new HashMap<>();
        signalVariableMap.put("textVar", "John Doe");
        signalVariableMap.put("numberVar", 1);
        runtimeService.startProcessInstanceByKey("throwSignal", signalVariableMap);

        assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
        
        assertThat(managementService.createJobQuery().count()).isEqualTo(1);
        
        try {
            processEngineConfiguration.getClock().setCurrentTime(new Date(System.currentTimeMillis() + 1000));
            waitForJobExecutorToProcessAllJobs(10000, 100L);

            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();
            
            Map<String, Object> variableMap = runtimeService.getVariables(processInstance.getId());
            assertThat(variableMap)
                    .containsOnly(
                            entry("myNewTextVar", "John Doe"),
                            entry("myNewNumberVar", 1));
            
        } finally {
            processEngineConfiguration.getClock().setCurrentTime(new Date());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.catchMultipleSignals.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignal.bpmn20.xml", "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAbortSignal.bpmn20.xml" })
    public void testSignalCatchDifferentSignals() {

        runtimeService.startProcessInstanceByKey("catchSignal");

        assertThat(createEventSubscriptionQuery().count()).isEqualTo(2);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        runtimeService.startProcessInstanceByKey("throwAbort");

        assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        org.flowable.task.api.Task taskAfterAbort = taskService.createTaskQuery().taskAssignee("gonzo").singleResult();
        assertThat(taskAfterAbort).isNotNull();
        taskService.complete(taskAfterAbort.getId());

        runtimeService.startProcessInstanceByKey("throwSignal");

        assertThat(createEventSubscriptionQuery().count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
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
        assertThatThrownBy(() -> repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTests.duplicateSignalNames.bpmn20.xml").deploy())
                .isInstanceOf(Exception.class)
                .hasMessageContaining(Problems.SIGNAL_DUPLICATE_NAME);
    }

    @Test
    public void testNoSignalName() {
        assertThatThrownBy(() ->  repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTests.noSignalName.bpmn20.xml").deploy())
                .isInstanceOf(Exception.class)
                .hasMessageContaining(Problems.SIGNAL_MISSING_NAME);
    }

    @Test
    public void testSignalNoId() {
        assertThatThrownBy(() ->  repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTests.signalNoId.bpmn20.xml").deploy())
                .isInstanceOf(Exception.class)
                .hasMessageContaining(Problems.SIGNAL_MISSING_ID);
    }

    @Test
    public void testSignalNoRef() {
        assertThatThrownBy(() ->  repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTests.signalNoRef.bpmn20.xml").deploy())
                .isInstanceOf(Exception.class)
                .hasMessageContaining(Problems.SIGNAL_EVENT_MISSING_SIGNAL_REF);
    }

    private EventSubscriptionQueryImpl createEventSubscriptionQuery() {
        return new EventSubscriptionQueryImpl(processEngineConfiguration.getCommandExecutor(), processEngineConfiguration.getEventSubscriptionServiceConfiguration());
    }

    /**
     * TestCase to reproduce Issue ACT-1344
     */
    @Test
    @Deployment
    public void testNonInterruptingSignal() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("nonInterruptingSignalEvent");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsOnly("My User Task");

        runtimeService.signalEventReceived("alert");

        tasks = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsOnly("My User Task", "My Second User Task");

        taskService.complete(taskService.createTaskQuery().taskName("My User Task").singleResult().getId());

        tasks = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsOnly("My Second User Task");
    }
    
    @Test
    @Deployment
    public void testNonInterruptingSignalWithInParameters() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("nonInterruptingSignalEvent");

        Task task = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).singleResult();

        Map<String, Object> payload = new HashMap<>();
        payload.put("testVar", "test");
        payload.put("anotherVar", "anotherTest");
        payload.put("nameVar", "John Doe");
        runtimeService.signalEventReceived("alert", payload);

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsOnly("My User Task", "My Second User Task");

        Map<String, Object> processVariableMap = runtimeService.getVariables(pi.getProcessInstanceId());
        assertThat(processVariableMap)
                .containsOnly(
                        entry("myTestVar", "test"),
                        entry("myAnotherVar", "anotherTest")
                );

        taskService.complete(taskService.createTaskQuery().taskName("My User Task").singleResult().getId());

        task = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).singleResult();
        assertThat(task.getName()).isEqualTo("My Second User Task");
    }

    /**
     * TestCase to reproduce Issue ACT-1344
     */
    @Test
    @Deployment
    public void testNonInterruptingSignalWithSubProcess() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("nonInterruptingSignalWithSubProcess");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsOnly("Approve");

        runtimeService.signalEventReceived("alert");

        tasks = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsOnly("Approve", "Review");

        taskService.complete(taskService.createTaskQuery().taskName("Approve").singleResult().getId());

        tasks = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsOnly("Review");

        taskService.complete(taskService.createTaskQuery().taskName("Review").singleResult().getId());

        tasks = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).list();
        assertThat(tasks).hasSize(1);
    }

    @Test
    @Deployment
    public void testUseSignalForExceptionsBetweenParallelPaths() {
        runtimeService.startProcessInstanceByKey("processWithSignal");

        // First task should be to select the developers
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Enter developers");
        taskService.complete(task.getId(), CollectionUtil.singletonMap("developers", Arrays.asList("developerOne", "developerTwo", "developerThree")));

        // Should be three distinct tasks for each developer
        assertThat(taskService.createTaskQuery().taskAssignee("developerOne").singleResult().getName()).isEqualTo("Develop specifications");
        assertThat(taskService.createTaskQuery().taskAssignee("developerTwo").singleResult().getName()).isEqualTo("Develop specifications");
        assertThat(taskService.createTaskQuery().taskAssignee("developerThree").singleResult().getName()).isEqualTo("Develop specifications");

        // Negotiate with client is a task for kermit
        task = taskService.createTaskQuery().taskAssignee("kermit").singleResult();
        assertThat(task.getName()).isEqualTo("Negotiate with client");

        // When the kermit task is completed, it throws a signal which should
        // cancel the multi instance
        taskService.complete(task.getId(), CollectionUtil.singletonMap("negotationFailed", true));

        // No tasks should be open then and process should have ended
        assertThat(taskService.createTaskQuery().count()).isZero();
        assertThat(runtimeService.createExecutionQuery().count()).isZero();
    }

    @Test
    @Deployment
    public void testSignalWithProcessInstanceScope() {
        // Start the process that catches the signal
        ProcessInstance processInstanceCatch = runtimeService.startProcessInstanceByKey("processWithSignalCatch");
        assertThat(taskService.createTaskQuery().processInstanceId(processInstanceCatch.getId()).singleResult().getName()).isEqualTo("userTaskWithSignalCatch");

        // Then start the process that will throw the signal
        runtimeService.startProcessInstanceByKey("processWithSignalThrow");

        // Since the signal is process instance scoped, the second process shouldn't have proceeded in any way
        assertThat(taskService.createTaskQuery().processInstanceId(processInstanceCatch.getId()).singleResult().getName()).isEqualTo("userTaskWithSignalCatch");

        // Let's try to trigger the catch using the API, that should also fail
        runtimeService.signalEventReceived("The Signal");
        assertThat(taskService.createTaskQuery().processInstanceId(processInstanceCatch.getId()).singleResult().getName()).isEqualTo("userTaskWithSignalCatch");
    }
    
    @Test
    @Deployment
    public void testCallActivityWithInstanceScopeSignal() {
        // start process with call activity and catching signal   
        ProcessInstance processInstanceCatch = runtimeService.startProcessInstanceByKey("processWithSignalCatch");
        assertThat(taskService.createTaskQuery().processInstanceId(processInstanceCatch.getId()).singleResult().getName()).isEqualTo("userTaskWithSignalCatch");
        
        ProcessInstance throwingProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstanceCatch.getId()).singleResult();
        assertThat(throwingProcessInstance).isNotNull();
        
        Task beforeThrowTask = taskService.createTaskQuery().processInstanceId(throwingProcessInstance.getId()).singleResult();
        assertThat(beforeThrowTask.getTaskDefinitionKey()).isEqualTo("beforeThrowTask");
        taskService.complete(beforeThrowTask.getId());
        
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(throwingProcessInstance.getId()).count()).isZero();
        
        Task afterSignalReceiveTask = taskService.createTaskQuery().processInstanceId(processInstanceCatch.getId()).singleResult();
        assertThat(afterSignalReceiveTask.getTaskDefinitionKey()).isEqualTo("userTaskAfterSignalCatch");
        taskService.complete(afterSignalReceiveTask.getId());
        
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceCatch.getId()).count()).isZero();
    }

    @Test
    @Deployment
    public void testSignalWithGlobalScope() {
        // Start the process that catches the signal
        ProcessInstance processInstanceCatch = runtimeService.startProcessInstanceByKey("processWithSignalCatch");
        assertThat(taskService.createTaskQuery().processInstanceId(processInstanceCatch.getId()).singleResult().getName()).isEqualTo("userTaskWithSignalCatch");

        // Then start the process that will throw thee signal
        runtimeService.startProcessInstanceByKey("processWithSignalThrow");

        // Since the signal is process instance scoped, the second process
        // shouldn't have proceeded in any way
        assertThat(taskService.createTaskQuery().processInstanceId(processInstanceCatch.getId()).singleResult().getName()).isEqualTo("userTaskAfterSignalCatch");
    }

    @Test
    @Deployment
    public void testAsyncTriggeredSignalEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processWithSignalCatch");

        assertThat(processInstance).isNotNull();
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).signalEventSubscriptionName("The Signal").singleResult();
        assertThat(execution).isNotNull();
        assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
        assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(2);

        runtimeService.signalEventReceivedAsync("The Signal", execution.getId());

        assertThat(managementService.createJobQuery().messages().count()).isEqualTo(1);

        waitForJobExecutorToProcessAllJobs(8000L, 200L);
        assertThat(createEventSubscriptionQuery().count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
        assertThat(managementService.createJobQuery().count()).isZero();
    }

    @Test
    @Deployment
    public void testSignalUserTask() {
        runtimeService.startProcessInstanceByKey("catchSignal");
        Execution execution = runtimeService.createExecutionQuery().onlyChildExecutions().activityId("waitState").singleResult();

        assertThat(execution).isNotNull();

        assertThatThrownBy(() -> runtimeService.trigger(execution.getId()))
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    public void testSignalStartEventFromProcess() {

        // Deploy test processes
        repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTest.testSignalStartEvent.bpmn20.xml").deploy();

        // Starting the process that fires the signal should start three process
        // instances that are listening on that signal
        runtimeService.startProcessInstanceByKey("processWithSignalThrow");

        // Verify
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(3);
        assertThat(taskService.createTaskQuery().count()).isEqualTo(3);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        List<String> names = Arrays.asList("A", "B", "C");
        for (int i = 0; i < tasks.size(); i++) {
            assertThat(tasks.get(i).getName()).isEqualTo("Task in process " + names.get(i));
        }

        // Start a process with a signal boundary event
        runtimeService.startProcessInstanceByKey("processWithSignalCatch");
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(4);
        assertThat(taskService.createTaskQuery().count()).isEqualTo(4);
        assertThat(taskService.createTaskQuery().taskName("Task in process D").count()).isEqualTo(1);

        // Firing the signal should now trigger the one with the boundary event
        // too
        runtimeService.startProcessInstanceByKey("processWithSignalThrow");
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(7);
        assertThat(taskService.createTaskQuery().count()).isEqualTo(7);
        assertThat(taskService.createTaskQuery().taskName("Task after signal").count()).isEqualTo(1);

        // Cleanup
        cleanup();

    }

    @Test
    public void testSignalStartEventFromProcesAsync() {

        // Deploy test processes
        repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTest.testSignalStartEventAsync.bpmn20.xml").deploy();

        // Starting the process that fires the signal should start 1 process
        // instance that are listening on that signal, the others are done async
        runtimeService.startProcessInstanceByKey("processWithSignalThrow");

        // Verify
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
        assertThat(taskService.createTaskQuery().count()).isZero();

        assertThat(managementService.createJobQuery().count()).isEqualTo(3);
        for (Job job : managementService.createJobQuery().list()) {
            managementService.executeJob(job.getId());
        }
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(3);
        assertThat(taskService.createTaskQuery().count()).isEqualTo(3);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        List<String> names = Arrays.asList("A", "B", "C");
        for (int i = 0; i < tasks.size(); i++) {
            assertThat(tasks.get(i).getName()).isEqualTo("Task in process " + names.get(i));
        }

        // Start a process with a signal boundary event
        runtimeService.startProcessInstanceByKey("processWithSignalCatch");
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(4);
        assertThat(taskService.createTaskQuery().count()).isEqualTo(4);
        assertThat(taskService.createTaskQuery().taskName("Task in process D").count()).isEqualTo(1);

        // Firing again
        runtimeService.startProcessInstanceByKey("processWithSignalThrow");

        assertThat(managementService.createJobQuery().count()).isEqualTo(4);
        for (Job job : managementService.createJobQuery().list()) {
            managementService.executeJob(job.getId());
        }
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(7);
        assertThat(taskService.createTaskQuery().count()).isEqualTo(7);
        assertThat(taskService.createTaskQuery().taskName("Task after signal").count()).isEqualTo(1);

        // Cleanup
        cleanup();

    }

    @Test
    public void testSignalStartEventFromAPI() {

        // Deploy test processes
        repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTest.testSignalStartEvent.bpmn20.xml").deploy();

        runtimeService.signalEventReceived("The Signal");

        // Verify
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(3);
        assertThat(taskService.createTaskQuery().count()).isEqualTo(3);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        List<String> names = Arrays.asList("A", "B", "C");
        for (int i = 0; i < tasks.size(); i++) {
            assertThat(tasks.get(i).getName()).isEqualTo("Task in process " + names.get(i));
        }

        // Start a process with a signal boundary event
        runtimeService.startProcessInstanceByKey("processWithSignalCatch");
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(4);
        assertThat(taskService.createTaskQuery().count()).isEqualTo(4);
        assertThat(taskService.createTaskQuery().taskName("Task in process D").count()).isEqualTo(1);

        // Firing the signal should now trigger the one with the boundary event
        // too
        runtimeService.signalEventReceived("The Signal");
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(7);
        assertThat(taskService.createTaskQuery().count()).isEqualTo(7);
        assertThat(taskService.createTaskQuery().taskName("Task after signal").count()).isEqualTo(1);

        // Cleanup
        cleanup();

    }

    @Test
    public void testSignalStartEventFromAPIAsync() {

        // Deploy test processes
        repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTest.testSignalStartEventAsync.bpmn20.xml").deploy();

        runtimeService.signalEventReceivedAsync("The Signal");

        assertThat(managementService.createJobQuery().count()).isEqualTo(3);
        for (Job job : managementService.createJobQuery().list()) {
            managementService.executeJob(job.getId());
        }
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(3);
        assertThat(taskService.createTaskQuery().count()).isEqualTo(3);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        List<String> names = Arrays.asList("A", "B", "C");
        for (int i = 0; i < tasks.size(); i++) {
            assertThat(tasks.get(i).getName()).isEqualTo("Task in process " + names.get(i));
        }

        // Start a process with a signal boundary event
        runtimeService.startProcessInstanceByKey("processWithSignalCatch");
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(4);
        assertThat(taskService.createTaskQuery().count()).isEqualTo(4);
        assertThat(taskService.createTaskQuery().taskName("Task in process D").count()).isEqualTo(1);

        // Firing again
        runtimeService.signalEventReceivedAsync("The Signal");

        assertThat(managementService.createJobQuery().count()).isEqualTo(4);
        for (Job job : managementService.createJobQuery().list()) {
            managementService.executeJob(job.getId());
        }
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(7);
        assertThat(taskService.createTaskQuery().count()).isEqualTo(7);
        assertThat(taskService.createTaskQuery().taskName("Task after signal").count()).isEqualTo(1);

        // Cleanup
        cleanup();

    }

    @Test
    @Deployment
    public void testEarlyFinishedProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callerProcess");
        assertThat(processInstance.getId()).isNotNull();
    }

    @Test
    @Deployment
    public void testNoneEndEventAfterSignalInConcurrentProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("my-process");
        assertThat(processInstance).isNotNull();

        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("usertask1").singleResult();
        taskService.claim(task.getId(), "user");
        taskService.complete(task.getId());
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // trigger history comment handling when necessary
        }

        task = taskService.createTaskQuery().singleResult();

        assertThat(task.getTaskDefinitionKey()).isEqualTo("usertask2");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignal.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignal.bpmn20.xml" })
    public void testSignalCatchSuspendedDefinition() {
        runtimeService.startProcessInstanceByKey("catchSignal");

        assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        repositoryService.suspendProcessDefinitionByKey("catchSignal");

        runtimeService.startProcessInstanceByKey("throwSignal");

        assertThat(createEventSubscriptionQuery().count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignal.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignal.bpmn20.xml" })
    public void testSignalCatchSuspendedDefinitionAndInstances() {
        runtimeService.startProcessInstanceByKey("catchSignal");

        assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        repositoryService.suspendProcessDefinitionByKey("catchSignal", true, null);

        runtimeService.startProcessInstanceByKey("throwSignal");

        // signal catch event is still there
        assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        repositoryService.activateProcessDefinitionByKey("catchSignal", true, null);

        runtimeService.startProcessInstanceByKey("throwSignal");

        // now the signal catch event is gone
        assertThat(createEventSubscriptionQuery().count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignal.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignal.bpmn20.xml" })
    public void testSignalCatchSuspendedInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("catchSignal");

        assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        runtimeService.suspendProcessInstanceById(processInstance.getId());

        runtimeService.startProcessInstanceByKey("throwSignal");

        // signal catch event is still there
        assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        runtimeService.activateProcessInstanceById(processInstance.getId());

        runtimeService.startProcessInstanceByKey("throwSignal");

        // now the signal catch event is gone
        assertThat(createEventSubscriptionQuery().count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    }

    @Test
    public void testSignalStartEventWithSuspendedDefinition() {

        repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTest.testSignalStartEvent.bpmn20.xml").deploy();

        repositoryService.suspendProcessDefinitionByKey("processWithSignalStart1");

        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("processWithSignalThrow"))
                .as("Suspended process definition should fail")
                .isExactlyInstanceOf(FlowableException.class);

        // Verify
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

        repositoryService.activateProcessDefinitionByKey("processWithSignalStart1");

        // Starting the process that fires the signal should start three process
        // instances that are listening on that signal
        runtimeService.startProcessInstanceByKey("processWithSignalThrow");

        // Verify
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(3);
        assertThat(taskService.createTaskQuery().count()).isEqualTo(3);

        // Cleanup
        cleanup();

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
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(3);
        cleanup();

    }

    @Test
    public void testRedeployWithRuntimeEventSubscription() {
        org.flowable.engine.repository.Deployment deployment1 = repositoryService.createDeployment()
            .addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTest.testSignalBoundaryOnSubProcess.bpmn20.xml").deploy();
        ProcessDefinition processDefinition1 = repositoryService.createProcessDefinitionQuery().deploymentId(deployment1.getId()).singleResult();

        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition1.getId());

        assertThat(runtimeService.createEventSubscriptionQuery().list())
            .extracting(EventSubscription::getEventType, EventSubscription::getProcessDefinitionId, EventSubscription::getProcessInstanceId)
            .containsOnly(tuple(SignalEventSubscriptionEntity.EVENT_TYPE, processDefinition1.getId(), processInstance.getId()));

        org.flowable.engine.repository.Deployment deployment2 = repositoryService.createDeployment()
            .addClasspathResource("org/flowable/engine/test/bpmn/event/signal/SignalEventTest.testSignalBoundaryOnSubProcess.bpmn20.xml").deploy();
        ProcessDefinition processDefinition2 = repositoryService.createProcessDefinitionQuery().deploymentId(deployment1.getId()).singleResult();

        assertThat(runtimeService.createEventSubscriptionQuery().list())
            .extracting(EventSubscription::getEventType, EventSubscription::getProcessDefinitionId, EventSubscription::getProcessInstanceId)
            .containsOnly(tuple(SignalEventSubscriptionEntity.EVENT_TYPE, processDefinition1.getId(), processInstance.getId())); // definition should have remained the same

        cleanup();
    }

    @Test
    @Deployment
    public void testSignalWaitOnUserTaskBoundaryEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("signal-wait");
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).signalEventSubscriptionName("waitsig").singleResult();
        assertThat(execution).isNotNull();
        runtimeService.signalEventReceived("waitsig", execution.getId());
        execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).signalEventSubscriptionName("waitsig").singleResult();
        assertThat(execution).isNull();
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("Wait2");
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
        assertThat(firstProcessInstance).isNotNull();

        // task should be "add a file"
        org.flowable.task.api.Task firstTask = taskService.createTaskQuery().singleResult();
        assertThat(firstTask.getName()).isEqualTo("Add a file");

        Map<String, Object> vars = runtimeService.getVariables(firstTask.getExecutionId());
        // file does not exists
        assertThat(vars)
                .containsEntry(fileExistsVar, false);

        // create second instance
        ProcessInstance secondProcessInstance = runtimeService.startProcessInstanceByKey("signalBoundaryProcess");
        assertThat(secondProcessInstance).isNotNull();

        // there should be two open tasks
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(2);

        // get current second task
        org.flowable.task.api.Task secondTask = taskService.createTaskQuery().processInstanceId(secondProcessInstance.getProcessInstanceId()).singleResult();
        // must be also in "add a file"
        assertThat(secondTask.getName()).isEqualTo("Add a file");

        // file does not exists yet
        vars = runtimeService.getVariables(secondTask.getExecutionId());
        assertThat(vars)
                .containsEntry(fileExistsVar, false);

        // now, we "add a file"
        taskService.claim(firstTask.getId(), "user");
        // create the file
        FileExistsMock.getInstance().touchFile();
        
        taskService.complete(firstTask.getId());
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // trigger history comment handling when necessary
        }

        List<org.flowable.task.api.Task> usingTask = taskService.createTaskQuery().taskName("Use the file").list();
        assertThat(usingTask).hasSize(1);
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
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstanceId).count()).isEqualTo(1);
        runtimeService.signalEventReceived("mySignal");
        
        assertThat(taskService.createTaskQuery().processInstanceId(processInstanceId).count()).isEqualTo(1);
    }

    @Test
    @Deployment
    public void testSignalExpression() {
        assertSignalEventSubscriptions("startSignal");

        runtimeService.signalEventReceived("startSignal", CollectionUtil.singletonMap("catchSignal", "actualCatchSignalValue"));
        assertSignalEventSubscriptions("actualCatchSignalValue", "eventSubprocessSignal", "startSignal");

        Map<String, Object> vars = new HashMap<>();
        vars.put("throwSignal", "eventSubprocessSignal");
        vars.put("boundarySignal", "actualBoundarySignalValue");
        runtimeService.signalEventReceived("actualCatchSignalValue", vars);

        List<Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("T1", "T3");

        assertSignalEventSubscriptions("actualBoundarySignalValue", "eventSubprocessSignal", "startSignal");
        runtimeService.signalEventReceived("actualBoundarySignalValue");
    }

    @Test
    @Deployment
    public void testSignalSubscriptionsRecreatedOnDeploymentDelete() {
        runtimeService.signalEventReceived("The Signal");
        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Task in process A");

        String deploymentId = repositoryService.createDeployment()
                .addClasspathResource(
                        "org/flowable/engine/test/bpmn/event/signal/SignalEventTest.testSignalSubscriptionsRecreatedOnDeploymentDeleteV2.bpmn20.xml")
                .deploy()
                .getId();

        runtimeService.signalEventReceived("The Signal");

        assertThat(taskService.createTaskQuery().list())
                .extracting(Task::getName)
                .containsExactlyInAnyOrder(
                        "Task in process A",
                        "Task in process A v2"
                );

        repositoryService.deleteDeployment(deploymentId, true);

        assertThat(taskService.createTaskQuery().list())
                .extracting(Task::getName)
                .containsExactlyInAnyOrder(
                        "Task in process A"
                );

        runtimeService.signalEventReceived("The Signal");

        assertThat(taskService.createTaskQuery().list())
                .extracting(Task::getName)
                .containsExactlyInAnyOrder(
                        "Task in process A",
                        "Task in process A"
                );
    }


    protected void assertSignalEventSubscriptions(String ... names) {
        Tuple[] tuples = new Tuple[names.length];
        for (int i = 0; i < names.length; i++) {
            tuples[i] = Tuple.tuple(SignalEventSubscriptionEntity.EVENT_TYPE, names[i]);
        }

        assertThat(runtimeService.createEventSubscriptionQuery().orderByEventName().asc().list())
            .extracting(EventSubscription::getEventType, EventSubscription::getEventName)
            .containsOnly(tuples);
    }

    private void validateTaskCounts(long taskACount, long taskBCount, long taskCCount) {
        assertThat(taskService.createTaskQuery().taskName("Task A").count()).isEqualTo(taskACount);
        assertThat(taskService.createTaskQuery().taskName("Task B").count()).isEqualTo(taskBCount);
        assertThat(taskService.createTaskQuery().taskName("Task C").count()).isEqualTo(taskCCount);
    }

    protected void cleanup() {
        for (org.flowable.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }


}
