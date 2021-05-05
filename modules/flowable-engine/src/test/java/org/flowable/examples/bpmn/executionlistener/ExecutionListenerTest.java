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

}
