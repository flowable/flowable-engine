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

package org.flowable.examples.bpmn.executionlistener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.flowable.engine.test.Deployment;
import org.flowable.examples.bpmn.executionlistener.CurrentActivityExecutionListener.CurrentActivity;
import org.flowable.examples.bpmn.executionlistener.RecorderExecutionListener.RecordedEvent;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

/**
 * @author Frederik Heremans
 */
public class ExecutionListenerTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/examples/bpmn/executionlistener/ExecutionListenersProcess.bpmn20.xml" })
    public void testExecutionListenersOnAllPossibleElements() {
        RecorderExecutionListener.clear();

        // Process start executionListener will have executionListener class
        // that sets 2 variables
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("executionListenersProcess", "businessKey123");

        String varSetInExecutionListener = (String) runtimeService.getVariable(processInstance.getId(), "variableSetInExecutionListener");
        assertThat(varSetInExecutionListener).isEqualTo("firstValue");

        // Check if business key was available in execution listener
        String businessKey = (String) runtimeService.getVariable(processInstance.getId(), "businessKeyInExecution");
        assertThat(businessKey).isEqualTo("businessKey123");

        // Transition take executionListener will set 2 variables
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());

        varSetInExecutionListener = (String) runtimeService.getVariable(processInstance.getId(), "variableSetInExecutionListener");

        assertThat(varSetInExecutionListener).isEqualTo("secondValue");

        ExampleExecutionListenerPojo myPojo = new ExampleExecutionListenerPojo();
        runtimeService.setVariable(processInstance.getId(), "myPojo", myPojo);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());

        // First usertask uses a method-expression as executionListener:
        // ${myPojo.myMethod(execution.eventName)}
        ExampleExecutionListenerPojo pojoVariable = (ExampleExecutionListenerPojo) runtimeService.getVariable(processInstance.getId(), "myPojo");
        assertThat(pojoVariable.getReceivedEventName()).isEqualTo("end");

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());

        List<RecordedEvent> events = RecorderExecutionListener.getRecordedEvents();
        assertThat(events)
                .extracting(RecordedEvent::getParameter)
                .containsExactly("End Process Listener");
    }

    @Test
    @Deployment(resources = { "org/flowable/examples/bpmn/executionlistener/ExecutionListenersStartEndEvent.bpmn20.xml" })
    public void testExecutionListenersOnStartEndEvents() {
        RecorderExecutionListener.clear();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("executionListenersProcess");
        assertProcessEnded(processInstance.getId());

        List<RecordedEvent> recordedEvents = RecorderExecutionListener.getRecordedEvents();
        assertThat(recordedEvents)
                .extracting(RecordedEvent::getActivityId, RecordedEvent::getActivityName, RecordedEvent::getParameter, RecordedEvent::getEventName)
                .containsExactly(
                        tuple("theStart", "Start Event", "Start Event Listener", "end"),
                        tuple("noneEvent", "None Event", "Intermediate Catch Event Listener", "end"),
                        tuple("signalEvent", "Signal Event", "Intermediate Throw Event Listener", "start"),
                        tuple("theEnd", "End Event", "End Event Listener", "start")
                );
    }

    @Test
    @Deployment(resources = { "org/flowable/examples/bpmn/executionlistener/ExecutionListenersFieldInjectionProcess.bpmn20.xml" })
    public void testExecutionListenerFieldInjection() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("myVar", "listening!");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("executionListenersProcess", variables);

        Object varSetByListener = runtimeService.getVariable(processInstance.getId(), "var");
        assertThat(varSetByListener).isInstanceOf(String.class);

        // Result is a concatenation of fixed injected field and injected expression
        assertThat(varSetByListener).isEqualTo("Yes, I am listening!");
    }

    @Test
    @Deployment(resources = { "org/flowable/examples/bpmn/executionlistener/ExecutionListenersCurrentActivity.bpmn20.xml" })
    public void testExecutionListenerCurrentActivity() {

        CurrentActivityExecutionListener.clear();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("executionListenersProcess");
        assertProcessEnded(processInstance.getId());

        List<CurrentActivity> currentActivities = CurrentActivityExecutionListener.getCurrentActivities();
        assertThat(currentActivities)
                .extracting(CurrentActivity::getActivityId, CurrentActivity::getActivityName)
                .containsExactly(
                        tuple("theStart", "Start Event"),
                        tuple("noneEvent", "None Event"),
                        tuple("theEnd", "End Event")
                );
    }

    @Test
    @Deployment(resources = { "org/flowable/examples/bpmn/executionlistener/ExecutionListenersForSubprocessStartEndEvent.bpmn20.xml" })
    public void testExecutionListenersForSubprocessStartEndEvents() {
        RecorderExecutionListener.clear();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("executionListenersProcess");

        List<RecordedEvent> recordedEvents = RecorderExecutionListener.getRecordedEvents();
        assertThat(recordedEvents)
                .extracting(RecordedEvent::getParameter)
                .containsExactly("Process Start");

        RecorderExecutionListener.clear();

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());

        recordedEvents = RecorderExecutionListener.getRecordedEvents();
        assertThat(recordedEvents)
                .extracting(RecordedEvent::getParameter)
                .containsExactly("Subprocess Start", "Subprocess End", "Process End");
    }

    @Test
    @Deployment(resources = { "org/flowable/examples/bpmn/executionlistener/ExecutionListenersProcess.bpmn20.xml" })
    public void testExecutionListenersOnAsyncProcessStart() {
        RecorderExecutionListener.clear();

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("executionListenersProcess").
            businessKey("businessKey123").startAsync();
        String varSetInExecutionListener = (String) runtimeService.getVariable(processInstance.getId(), "variableSetInExecutionListener");
        // ProcessStartExecutionListeners are executed from the asynchronous job
        assertThat(varSetInExecutionListener).isNull();

        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 2000, 200);

        // Process start executionListener will have executionListener class
        // that sets 2 variables
        varSetInExecutionListener = (String) runtimeService.getVariable(processInstance.getId(), "variableSetInExecutionListener");
        assertThat(varSetInExecutionListener).isEqualTo("firstValue");

        // Check if business key was available in execution listener
        String businessKey = (String) runtimeService.getVariable(processInstance.getId(), "businessKeyInExecution");
        assertThat(businessKey).isEqualTo("businessKey123");

    }

    @Test
    @Deployment(resources = { "org/flowable/examples/bpmn/executionlistener/ExecutionListenerDelegateExpressionThrowsException.bpmn20.xml" })
    public void testExecutionListenerWithDelegateExpressionThrowsFlowableException() {
        ProcessInstanceBuilder builder = runtimeService
                .createProcessInstanceBuilder()
                .processDefinitionKey("executionListenersProcess")
                .transientVariable("bean", (ExecutionListener) delegateExecution -> {
                    throw new FlowableIllegalArgumentException("Message from listener");
                });
        assertThatThrownBy(builder::start)
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasNoCause()
                .hasMessage("Message from listener");
    }

    @Test
    @Deployment(resources = { "org/flowable/examples/bpmn/executionlistener/ExecutionListenerDelegateExpressionThrowsException.bpmn20.xml" })
    public void testExecutionListenerWithDelegateExpressionThrowsNonFlowableException() {
        ProcessInstanceBuilder builder = runtimeService
                .createProcessInstanceBuilder()
                .processDefinitionKey("executionListenersProcess")
                .transientVariable("bean", (ExecutionListener) delegateExecution -> {
                    throw new RuntimeException("Message from listener");
                });
        assertThatThrownBy(builder::start)
                .isExactlyInstanceOf(RuntimeException.class)
                .hasNoCause()
                .hasMessage("Message from listener");
    }

    /**
     * https://github.com/flowable/flowable-engine/issues/3327
     */
    @Test
    @Deployment(resources = { "org/flowable/examples/bpmn/executionlistener/ExecutionListenerJavaDelegate.bpmn20.xml" })
    public void testJavaDelegateAsExecutionListenerFollowsCorrectFlow() {
        ProcessInstance myFlow = runtimeService.startProcessInstanceByKey("MyFlow");
        String myActions = runtimeService.getVariable(myFlow.getId(), "myActions", String.class);
        Task task = assertDoesNotThrow(() -> taskService.createTaskQuery().singleResult(), "Only one task 'Production Manager' must be returned.");
        assertThat(task.getName()).isEqualTo("Production Manager");
        assertThat(myActions).isEqualTo("Start executionListener,ProductionManager taskListener");
    }

    @Test
    @Deployment(resources = "org/flowable/examples/bpmn/executionlistener/ExecutionListenerTest.testThrowBpmnErrorCatchOnSubprocess.bpmn20.xml")
    public void testThrowBpmnErrorCatchOnSubprocessStart() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("throwErrorStartListener", "MY_ERROR");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("errorHandlingSubProcess", vars);
        Map<String, Object> processVariables = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .includeProcessVariables().singleResult()
                .getProcessVariables();

        assertThat(processInstance.getProcessVariables())
                .containsEntry("handled_error", "MY_ERROR")
                .doesNotContainKey("_script_task");
        assertThat(processVariables)
                .containsEntry("handled_error", "MY_ERROR")
                .doesNotContainKey("error_code");
    }

    @Test
    @Deployment(resources = "org/flowable/examples/bpmn/executionlistener/ExecutionListenerTest.testThrowBpmnErrorCatchOnSubprocess.bpmn20.xml")
    public void testThrowBpmnErrorCatchOnSubprocessEnd() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("throwErrorEndListener", "MY_ERROR");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("errorHandlingSubProcess", vars);
        Map<String, Object> processVariables = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .includeProcessVariables().singleResult()
                .getProcessVariables();

        assertThat(processInstance.getProcessVariables())
                .containsEntry("handled_error", "MY_ERROR")
                .containsEntry("_script_task", "executed");
        assertThat(processVariables)
                .containsEntry("handled_error", "MY_ERROR")
                .containsEntry("_script_task", "executed");
    }

    @Test
    @Deployment
    public void testThrowBpmnErrorCatchBoundaryEventStart() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("throwErrorCode", "EXECUTION_LISTENER_BPMN_ERROR");
        vars.put("eventType", "start");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("errorHandlingSubProcess", vars);

        Map<String, Object> processVariables = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .includeProcessVariables().singleResult()
                .getProcessVariables();

        assertThat(processInstance.getProcessVariables())
                .containsEntry("error_handled", "true")
                .doesNotContainKey("_script_task");
        assertThat(processVariables)
                .containsEntry("error_handled", "true")
                .doesNotContainKey("_script_task");

        List<HistoricTaskInstance> allTasks = historyService.createHistoricTaskInstanceQuery().list();
        // Why is this empty? Shouldn't we have the sub-process task 'handleErrorOther' here? Same behavior on main.
        assertThat(allTasks).isEmpty();
    }

    @Test
    @Deployment
    public void testThrowBpmnErrorCatchBoundaryEventEnd() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("throwErrorCode", "EXECUTION_LISTENER_BPMN_ERROR");
        vars.put("eventType", "start");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("errorHandlingSubProcess", vars);

        Map<String, Object> processVariables = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .includeProcessVariables().singleResult()
                .getProcessVariables();

        assertThat(processInstance.getProcessVariables())
                .containsEntry("error_handled", "true")
                .containsEntry("_script_task", "executed");
        assertThat(processVariables)
                .containsEntry("error_handled", "true")
                .containsEntry("_script_task", "executed");
    }

    @Test
    @Deployment(resources = { "org/flowable/examples/bpmn/executionlistener/ExecutionListenerTest.testThrowBpmnErrorCatchBoundaryEventSubProcess.bpmn20.xml" })
    public void testThrowBpmnErrorCatchBoundaryEventSubProcessStart() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("throwErrorStartListener", "EXECUTION_LISTENER_BPMN_ERROR");
        vars.put("direction", "subprocess");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessErrorHandling", vars);

        Map<String, Object> processVariables = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .includeProcessVariables().singleResult()
                .getProcessVariables();

        assertThat(processInstance.getProcessVariables())
                .containsEntry("direction", "subprocess")
                .containsEntry("throwErrorStartListener", "EXECUTION_LISTENER_BPMN_ERROR")
                .containsEntry("error_handled", "true")
                .doesNotContainKey("endListener");

        assertThat(processVariables)
                .containsEntry("direction", "subprocess")
                .containsEntry("throwErrorStartListener", "EXECUTION_LISTENER_BPMN_ERROR")
                .containsEntry("error_handled", "true")
                .doesNotContainKey("endListener");
    }

    @Test
    @Deployment(resources = { "org/flowable/examples/bpmn/executionlistener/ExecutionListenerTest.testThrowBpmnErrorCatchBoundaryEventSubProcess.bpmn20.xml" })
    public void testThrowBpmnErrorCatchBoundaryEventSubProcessEnd() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("throwErrorEndListener", "EXECUTION_LISTENER_BPMN_ERROR");
        vars.put("direction", "subprocess");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessErrorHandling", vars);

        Map<String, Object> processVariables = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .includeProcessVariables().singleResult()
                .getProcessVariables();

        assertThat(processInstance.getProcessVariables())
                .containsEntry("direction", "subprocess")
                .containsEntry("error_handled", "true")
                .containsEntry("startListener", "executed");

        assertThat(processVariables)
                .containsEntry("direction", "subprocess")
                .containsEntry("error_handled", "true")
                .containsEntry("startListener", "executed");
    }

    @Test
    @Deployment(resources =
            "org/flowable/examples/bpmn/executionlistener/ExecutionListenerTest.testThrowBpmnErrorCatchBoundaryEventSubProcessMultiInstanceSequential.bpmn20.xml")
    public void testThrowBpmnErrorCatchBoundaryEventSubProcessMultiInstanceSequentialStart() {
        {
            // SubProcess ExecutionListener
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("subProcessErrorHandling")
                    .transientVariable("elements", Arrays.asList("1", "2", "3"))
                    .transientVariable("throwErrorSubProcessStartListener", "EXECUTION_LISTENER_BPMN_ERROR")
                    .transientVariable("direction", "subprocess")
                    .start();

            assertThat(processInstance.getProcessVariables())
                    .containsEntry("error_handled", "true")
                    .hasSize(1)
                    .doesNotContainKey("startListener_element_1")
                    .doesNotContainKey("element_1")
                    .doesNotContainKey("element_2")
                    .doesNotContainKey("element_3");
        }
        {
            // SubProcess Activity ExecutionListener throws BpmnError (element 2)
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("subProcessErrorHandling")
                    .transientVariable("elements", Arrays.asList("1", "2", "3"))
                    .transientVariable("throwErrorSubProcessTaskStartListener", "EXECUTION_LISTENER_BPMN_ERROR")
                    .transientVariable("direction", "subprocess")
                    .start();

            // element 2 throws the exception and interrupts execution of other items.
            assertThat(processInstance.getProcessVariables())
                    .containsEntry("error_handled", "true")
                    .containsEntry("startProcessListener_null", "executed")
                    .containsEntry("startProcessListener_1", "executed")
                    .containsEntry("startListener_element_1", "executed")
                    .containsEntry("element_1", "executed")
                    .containsEntry("endListener_element_1", "executed")
                    .containsEntry("endProcessListener_1", "executed")
                    .containsEntry("startProcessListener_2", "executed")
                    .hasSize(8)
                    .doesNotContainKeys("endProcessListener_null");
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/examples/bpmn/executionlistener/ExecutionListenerTest.testThrowBpmnErrorCatchBoundaryEventSubProcessMultiInstanceSequential.bpmn20.xml" })
    public void testThrowBpmnErrorCatchBoundaryEventSubProcessMultiInstanceSequentialEnd() {
        {
            // SubProcess ExecutionListener
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("subProcessErrorHandling")
                    .transientVariable("direction", "subprocess")
                    .transientVariable("elements", Arrays.asList("1", "2", "3"))
                    .transientVariable("throwErrorSubProcessEndListener", "EXECUTION_LISTENER_BPMN_ERROR")
                    .start();

            assertThat(processInstance.getProcessVariables())
                    .containsEntry("startProcessListener_null", "executed")
                    .containsEntry("startProcessListener_1", "executed")
                    .containsEntry("startListener_element_1", "executed")
                    .containsEntry("element_1", "executed")
                    .containsEntry("endListener_element_1", "executed")
                    .containsEntry("endProcessListener_1", "executed")
                    .containsEntry("startProcessListener_2", "executed")
                    .containsEntry("startListener_element_2", "executed")
                    .containsEntry("element_2", "executed")
                    .containsEntry("endListener_element_2", "executed")
                    .containsEntry("endProcessListener_2", "executed")
                    .containsEntry("startProcessListener_2", "executed")
                    .containsEntry("startListener_element_3", "executed")
                    .containsEntry("element_3", "executed")
                    .containsEntry("endListener_element_3", "executed")
                    .containsEntry("endProcessListener_3", "executed")
                    .containsEntry("error_handled", "true")
                    .hasSize(17)
                    .doesNotContainKey("endProcessListener_null");
        }
        {
            // SubProcess Activity ExecutionListener
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("subProcessErrorHandling")
                    .transientVariable("direction", "subprocess")
                    .transientVariable("elements", Arrays.asList("1", "2", "3"))
                    .transientVariable("throwErrorSubProcessTaskEndListener", "EXECUTION_LISTENER_BPMN_ERROR")
                    .start();

            // element 2 throws the exception and interrupts execution of other items.
            assertThat(processInstance.getProcessVariables())
                    .containsEntry("error_handled", "true")
                    .containsEntry("startProcessListener_null", "executed")
                    .containsEntry("startProcessListener_1", "executed")
                    .containsEntry("startListener_element_1", "executed")
                    .containsEntry("element_1", "executed")
                    .containsEntry("endListener_element_1", "executed")
                    .containsEntry("endProcessListener_1", "executed")
                    .containsEntry("startProcessListener_2", "executed")
                    .containsEntry("startListener_element_2", "executed")
                    .containsEntry("element_2", "executed")
                    .hasSize(10)
                    .doesNotContainKey("endProcessListener_null");
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/examples/bpmn/executionlistener/ExecutionListenerTest.testThrowBpmnErrorCatchBoundaryEventSubProcessMultiInstanceParallel.bpmn20.xml" })
    public void testThrowBpmnErrorCatchBoundaryEventSubProcessMultiInstanceParallelStart() {
        {
            // SubProcess ExecutionListener
            Map<String, Object> vars = new HashMap<>();
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("subProcessErrorHandling")
                    .transientVariable("throwErrorSubProcessStartListener", "EXECUTION_LISTENER_BPMN_ERROR")
                    .transientVariable("elements", Arrays.asList("1", "2", "3"))
                    .transientVariable("direction", "subprocess")
                    .start();

            assertThat(processInstance.getProcessVariables())
                    .containsEntry("error_handled", "true")
                    .hasSize(1);
        }
        {
            // SubProcess Activity ExecutionListener
            Map<String, Object> vars = new HashMap<>();
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("subProcessErrorHandling")
                    .transientVariable("throwErrorSubProcessTaskStartListener", "EXECUTION_LISTENER_BPMN_ERROR")
                    .transientVariable("elements", Arrays.asList("1", "2", "3"))
                    .transientVariable("direction", "subprocess")
                    .start();

            assertThat(processInstance.getProcessVariables())
                    .containsEntry("error_handled", "true")
                    .containsEntry("startProcessListener_null", "executed")
                    /* This is different to sequential: Process executionListeners are executed, for all instances*/
                    .containsEntry("startProcessListener_1", "executed")
                    .containsEntry("startProcessListener_2", "executed")
                    .containsEntry("startProcessListener_3", "executed")
                    .containsEntry("startListener_element_1", "executed")
                    .containsEntry("element_1", "executed")
                    .hasSize(7)
                    /* this is different to 'sequential'. End listeners of element_1 are NOT called, because element_2 throws the error in
                        the start listener before the node of element_1 is left. */
                    .doesNotContainKey("endProcessListener_null");
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/examples/bpmn/executionlistener/ExecutionListenerTest.testThrowBpmnErrorCatchBoundaryEventSubProcessMultiInstanceParallel.bpmn20.xml" })
    public void testThrowBpmnErrorCatchBoundaryEventSubProcessMultiInstanceParallelEnd() {
        {
            // SubProcess ExecutionListener
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("subProcessErrorHandling")
                    .transientVariable("throwErrorSubProcessEndListener", "EXECUTION_LISTENER_BPMN_ERROR")
                    .transientVariable("direction", "subprocess")
                    .transientVariable("elements", Arrays.asList("1", "2", "3"))
                    .start();

            assertThat(processInstance.getProcessVariables())
                    .containsEntry("error_handled", "true")
                    .containsEntry("startProcessListener_null", "executed")
                    .containsEntry("startProcessListener_1", "executed")
                    .containsEntry("startProcessListener_2", "executed")
                    .containsEntry("startProcessListener_3", "executed")
                    .containsEntry("startListener_element_1", "executed")
                    .containsEntry("element_1", "executed")
                    .containsEntry("startListener_element_2", "executed")
                    .containsEntry("element_2", "executed")
                    .containsEntry("startListener_element_3", "executed")
                    .containsEntry("element_3", "executed")
                    .containsEntry("endListener_element_1", "executed")
                    .containsEntry("endListener_element_2", "executed")
                    .containsEntry("endListener_element_3", "executed")
                    .hasSize(14);
        }
        {
                // SubProcess Activity ExecutionListener
                ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("subProcessErrorHandling")
                        .transientVariable("elements", Arrays.asList("1", "2", "3"))
                        .transientVariable("throwErrorSubProcessTaskEndListener", "EXECUTION_LISTENER_BPMN_ERROR")
                        .transientVariable("direction", "subprocess")
                        .start();

                // element 2 throws the exception and interrupts execution of other items.
                assertThat(processInstance.getProcessVariables())
                        .containsEntry("error_handled", "true")
                        .containsEntry("startProcessListener_null", "executed")
                        .containsEntry("startProcessListener_1", "executed")
                        .containsEntry("startListener_element_1", "executed")
                        .containsEntry("element_1", "executed")
                        .containsEntry("endListener_element_1", "executed")
                        // Those look a bit unexpected at first. But we throw the error in the endListener.
                        // Therefore, for parallel multi instance execution the actual tasks have already been executed.
                        .containsEntry("startProcessListener_2", "executed")
                        .containsEntry("startListener_element_2", "executed")
                        .containsEntry("element_2", "executed")
                        .containsEntry("startListener_element_3", "executed")
                        .containsEntry("startProcessListener_3", "executed")
                        .containsEntry("element_3", "executed")
                        .hasSize(12);
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/examples/bpmn/executionlistener/ExecutionListenerTest.testThrowBpmnErrorCatchBoundaryEventMultiInstanceParallelStart.bpmn20.xml" })
    @Ignore("FIXME Must be fixed")
    // FIXME does not yet work
    public void testThrowBpmnErrorCatchBoundaryEventMultiInstanceParallelStart() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("throwErrorStartListener", "EXECUTION_LISTENER_BPMN_ERROR");
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("multiInstanceTest")
                .variables(vars)
                .transientVariable("direction", "")
                .transientVariable("elements", Arrays.asList("1", "2", "3")).start();

        assertThat(processInstance.getProcessVariables())
                .containsEntry("error_handled", "true")
                .containsEntry("startListener_element_1", "executed")
                .containsEntry("element_1", "executed")
                .doesNotContainKey("endListener_element_1");
        System.out.println("PROCESS INSTANCE VARS: " + processInstance.getProcessVariables());
    }


    // FIXME continue adding more test cases
    /*
     * Add sequenceFlow executionListener tests
     * Add asynchronous multi instance tests
     * Add test scenario with multi instance error boundary event on activity
     * Add test scenario with two matching error handlers
     */
}
