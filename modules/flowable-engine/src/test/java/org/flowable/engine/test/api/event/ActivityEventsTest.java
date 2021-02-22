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
package org.flowable.engine.test.api.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.delegate.event.FlowableActivityCancelledEvent;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.flowable.engine.delegate.event.FlowableCancelledEvent;
import org.flowable.engine.delegate.event.FlowableErrorEvent;
import org.flowable.engine.delegate.event.FlowableMessageEvent;
import org.flowable.engine.delegate.event.FlowableSignalEvent;
import org.flowable.engine.delegate.event.impl.FlowableActivityEventImpl;
import org.flowable.engine.delegate.event.impl.FlowableSignalEventImpl;
import org.flowable.engine.event.EventLogEntry;
import org.flowable.engine.impl.event.logger.EventLogger;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test case for all {@link FlowableEvent}s related to activities.
 *
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class ActivityEventsTest extends PluggableFlowableTestCase {

    private TestFlowableActivityEventListener listener;

    protected EventLogger databaseEventLogger;

    @BeforeEach
    protected void setUp() throws Exception {
        listener = new TestFlowableActivityEventListener(true);
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);

        // Database event logger setup
        databaseEventLogger = new EventLogger(processEngineConfiguration.getClock(), processEngineConfiguration.getObjectMapper());
        runtimeService.addEventListener(databaseEventLogger);
    }

    @AfterEach
    protected void tearDown() throws Exception {

        if (listener != null) {
            listener.clearEventsReceived();
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }

        // Remove entries
        for (EventLogEntry eventLogEntry : managementService.getEventLogEntries(null, null)) {
            managementService.deleteEventLogEntry(eventLogEntry.getLogNumber());
        }

        // Database event logger teardown
        runtimeService.removeEventListener(databaseEventLogger);

    }

    /**
     * Test starting and completed events for activity. Since these events are dispatched in the core of the PVM, not all individual activity-type is tested. Rather, we test the main types (tasks,
     * gateways, events, subprocesses).
     */
    @Test
    @Deployment
    public void testActivityEvents() throws Exception {
        // We're interested in the raw events, alter the listener to keep those as well
        listener.setIgnoreRawActivityEvents(false);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("activityProcess");
        assertThat(processInstance).isNotNull();

        assertThat(listener.getEventsReceived()).hasSize(3);

        // Start-event activity started
        FlowableActivityEvent activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(0);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("theStart");
        assertThat(processInstance.getId()).isNotEqualTo(activityEvent.getExecutionId());
        assertThat(activityEvent.getProcessInstanceId()).isEqualTo(processInstance.getProcessInstanceId());
        assertThat(activityEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());

        // Start-event finished
        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(1);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("theStart");
        assertThat(processInstance.getId()).isNotEqualTo(activityEvent.getExecutionId());
        assertThat(activityEvent.getProcessInstanceId()).isEqualTo(processInstance.getProcessInstanceId());
        assertThat(activityEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());

        // Usertask started
        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(2);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("shipOrder");
        assertThat(processInstance.getId()).isNotEqualTo(activityEvent.getExecutionId());
        assertThat(activityEvent.getProcessInstanceId()).isEqualTo(processInstance.getProcessInstanceId());
        assertThat(activityEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());

        // Complete usertask
        listener.clearEventsReceived();
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());

        // Subprocess execution is created
        Execution execution = runtimeService.createExecutionQuery().parentId(processInstance.getId()).singleResult();
        assertThat(execution).isNotNull();
        assertThat(listener.getEventsReceived()).hasSize(5);
        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(0);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("shipOrder");
        assertThat(processInstance.getId()).isNotEqualTo(activityEvent.getExecutionId());
        assertThat(activityEvent.getProcessInstanceId()).isEqualTo(processInstance.getProcessInstanceId());
        assertThat(activityEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(1);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("subProcess");
        assertThat(activityEvent.getExecutionId()).isEqualTo(execution.getId());
        assertThat(activityEvent.getProcessInstanceId()).isEqualTo(processInstance.getProcessInstanceId());
        assertThat(activityEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(2);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("subProcessStart");
        assertThat(execution.getId()).isNotEqualTo(activityEvent.getExecutionId());
        assertThat(activityEvent.getProcessInstanceId()).isEqualTo(processInstance.getProcessInstanceId());
        assertThat(activityEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(3);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("subProcessStart");
        assertThat(execution.getId()).isNotEqualTo(activityEvent.getExecutionId());
        assertThat(activityEvent.getProcessInstanceId()).isEqualTo(processInstance.getProcessInstanceId());
        assertThat(activityEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(4);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("subTask");
        assertThat(execution.getId()).isNotEqualTo(activityEvent.getExecutionId());
        assertThat(activityEvent.getProcessInstanceId()).isEqualTo(processInstance.getProcessInstanceId());
        assertThat(activityEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        listener.clearEventsReceived();

        // Check gateway and intermediate throw event
        org.flowable.task.api.Task subTask = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).singleResult();
        assertThat(subTask).isNotNull();

        taskService.complete(subTask.getId());

        assertThat(listener.getEventsReceived()).hasSize(10);

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(0);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("subTask");

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(1);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("gateway");

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(2);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("gateway");

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(3);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("throwMessageEvent");

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(4);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("throwMessageEvent");

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(5);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("endSubProcess");

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(6);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("endSubProcess");

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(7);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("subProcess");

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(8);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("theEnd");

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(9);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("theEnd");
    }

    /**
     * Test events related to signalling
     */
    @Test
    @Deployment
    public void testActivitySignalEvents() throws Exception {
        // Two paths are active in the process, one receive-task and one intermediate catching signal-event
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("signalProcess");
        assertThat(processInstance).isNotNull();

        // Check regular signal through API
        Execution executionWithSignal = runtimeService.createExecutionQuery().activityId("receivePayment").singleResult();
        assertThat(executionWithSignal).isNotNull();

        Execution signalExecution = runtimeService.createExecutionQuery().signalEventSubscriptionName("alert").singleResult();
        assertThat(signalExecution).isNotNull();

        runtimeService.trigger(executionWithSignal.getId());
        assertThat(listener.getEventsReceived()).hasSize(2);

        assertThat(listener.getEventsReceived().get(0)).isInstanceOf(FlowableSignalEvent.class);
        FlowableSignalEvent signalEvent = (FlowableSignalEvent) listener.getEventsReceived().get(0);
        assertThat(signalEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING);
        assertThat(signalEvent.getActivityId()).isEqualTo("shipOrder");
        assertThat(signalEvent.getExecutionId()).isEqualTo(signalExecution.getId());
        assertThat(signalEvent.getProcessInstanceId()).isEqualTo(signalExecution.getProcessInstanceId());
        assertThat(signalEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(signalEvent.getSignalName()).isEqualTo("alert");
        assertThat(signalEvent.getSignalData()).isNull();

        assertThat(listener.getEventsReceived().get(1)).isInstanceOf(FlowableSignalEvent.class);
        signalEvent = (FlowableSignalEvent) listener.getEventsReceived().get(1);
        assertThat(signalEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_SIGNALED);
        assertThat(signalEvent.getActivityId()).isEqualTo("receivePayment");
        assertThat(signalEvent.getExecutionId()).isEqualTo(executionWithSignal.getId());
        assertThat(signalEvent.getProcessInstanceId()).isEqualTo(executionWithSignal.getProcessInstanceId());
        assertThat(signalEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(signalEvent.getSignalName()).isNull();
        assertThat(signalEvent.getSignalData()).isNull();
        listener.clearEventsReceived();

        // Check signal using event, and pass in additional payload
        Execution executionWithSignalEvent = runtimeService.createExecutionQuery().activityId("shipOrder").singleResult();
        runtimeService.signalEventReceived("alert", executionWithSignalEvent.getId(), Collections.singletonMap("test", (Object) "test"));
        assertThat(listener.getEventsReceived()).hasSize(1);
        assertThat(listener.getEventsReceived().get(0)).isInstanceOf(FlowableSignalEvent.class);
        signalEvent = (FlowableSignalEvent) listener.getEventsReceived().get(0);
        assertThat(signalEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_SIGNALED);
        assertThat(signalEvent.getActivityId()).isEqualTo("shipOrder");
        assertThat(signalEvent.getExecutionId()).isEqualTo(executionWithSignalEvent.getId());
        assertThat(signalEvent.getProcessInstanceId()).isEqualTo(executionWithSignalEvent.getProcessInstanceId());
        assertThat(signalEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(signalEvent.getSignalName()).isEqualTo("alert");
        assertThat(signalEvent.getSignalData()).isNotNull();
        listener.clearEventsReceived();

        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_SIGNALED);
    }

    /**
     * Test to verify if signals coming from an intermediate throw-event trigger the right events to be dispatched.
     */
    @Test
    @Deployment
    public void testActivitySignalEventsWithinProcess() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("signalProcess");
        assertThat(processInstance).isNotNull();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        Execution executionWithSignalEvent = runtimeService.createExecutionQuery().activityId("shipOrder").singleResult();

        taskService.complete(task.getId());
        assertThat(listener.getEventsReceived()).hasSize(2);

        assertThat(listener.getEventsReceived().get(0)).isInstanceOf(FlowableSignalEvent.class);
        FlowableSignalEvent signalEvent = (FlowableSignalEvent) listener.getEventsReceived().get(0);
        assertThat(signalEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING);
        assertThat(signalEvent.getActivityId()).isEqualTo("shipOrder");
        assertThat(signalEvent.getExecutionId()).isEqualTo(executionWithSignalEvent.getId());
        assertThat(signalEvent.getProcessInstanceId()).isEqualTo(executionWithSignalEvent.getProcessInstanceId());
        assertThat(signalEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(signalEvent.getSignalName()).isEqualTo("alert");
        assertThat(signalEvent.getSignalData()).isNull();

        assertThat(listener.getEventsReceived().get(1)).isInstanceOf(FlowableSignalEvent.class);
        signalEvent = (FlowableSignalEvent) listener.getEventsReceived().get(1);
        assertThat(signalEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_SIGNALED);
        assertThat(signalEvent.getActivityId()).isEqualTo("shipOrder");
        assertThat(signalEvent.getExecutionId()).isEqualTo(executionWithSignalEvent.getId());
        assertThat(signalEvent.getProcessInstanceId()).isEqualTo(executionWithSignalEvent.getProcessInstanceId());
        assertThat(signalEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(signalEvent.getSignalName()).isEqualTo("alert");
        assertThat(signalEvent.getSignalData()).isNull();
    }

    /**
     * Test events related to message events, called from the API.
     */
    @Test
    @Deployment
    public void testActivityMessageEvents() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("messageProcess");
        assertThat(processInstance).isNotNull();

        Execution executionWithMessage = runtimeService.createExecutionQuery().activityId("shipOrder").singleResult();
        assertThat(executionWithMessage).isNotNull();

        runtimeService.messageEventReceived("messageName", executionWithMessage.getId());
        assertThat(listener.getEventsReceived()).hasSize(2);

        // First, a message waiting event is expected
        assertThat(listener.getEventsReceived().get(0)).isInstanceOf(FlowableMessageEvent.class);
        FlowableMessageEvent messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(0);
        assertThat(messageEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);
        assertThat(messageEvent.getActivityId()).isEqualTo("shipOrder");
        assertThat(messageEvent.getExecutionId()).isEqualTo(executionWithMessage.getId());
        assertThat(messageEvent.getProcessInstanceId()).isEqualTo(executionWithMessage.getProcessInstanceId());
        assertThat(messageEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(messageEvent.getMessageName()).isEqualTo("messageName");
        assertThat(messageEvent.getMessageData()).isNull();

        // Second, a message received event is expected
        assertThat(listener.getEventsReceived().get(1)).isInstanceOf(FlowableMessageEvent.class);
        messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(1);
        assertThat(messageEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_MESSAGE_RECEIVED);
        assertThat(messageEvent.getActivityId()).isEqualTo("shipOrder");
        assertThat(messageEvent.getExecutionId()).isEqualTo(executionWithMessage.getId());
        assertThat(messageEvent.getProcessInstanceId()).isEqualTo(executionWithMessage.getProcessInstanceId());
        assertThat(messageEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(messageEvent.getMessageName()).isEqualTo("messageName");
        assertThat(messageEvent.getMessageData()).isNull();

        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_MESSAGE_RECEIVED);
    }

    /**
     * Test events related to message events, called from the API, targeting an event-subprocess.
     */
    @Test
    @Deployment
    public void testActivityMessageEventsInEventSubprocess() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("messageProcess");
        assertThat(processInstance).isNotNull();

        Execution executionWithMessage = runtimeService.createExecutionQuery().messageEventSubscriptionName("messageName").singleResult();
        assertThat(executionWithMessage).isNotNull();

        runtimeService.messageEventReceived("messageName", executionWithMessage.getId());

        // Only a message events should be present, no signal-event, since the event-subprocess is
        // not signaled, but executed instead
        assertThat(listener.getEventsReceived()).hasSize(2);

        // A message waiting event is expected
        assertThat(listener.getEventsReceived().get(0)).isInstanceOf(FlowableMessageEvent.class);
        FlowableMessageEvent messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(0);
        assertThat(messageEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);
        assertThat(messageEvent.getActivityId()).isEqualTo("catchMessage");
        assertThat(messageEvent.getExecutionId()).isEqualTo(executionWithMessage.getId());
        assertThat(messageEvent.getProcessInstanceId()).isEqualTo(executionWithMessage.getProcessInstanceId());
        assertThat(messageEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(messageEvent.getMessageName()).isEqualTo("messageName");
        assertThat(messageEvent.getMessageData()).isNull();

        // A message received event is expected
        assertThat(listener.getEventsReceived().get(1)).isInstanceOf(FlowableMessageEvent.class);
        messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(1);
        assertThat(messageEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_MESSAGE_RECEIVED);
        assertThat(messageEvent.getActivityId()).isEqualTo("catchMessage");
        assertThat(messageEvent.getExecutionId()).isEqualTo(executionWithMessage.getId());
        assertThat(messageEvent.getProcessInstanceId()).isEqualTo(executionWithMessage.getProcessInstanceId());
        assertThat(messageEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(messageEvent.getMessageName()).isEqualTo("messageName");
        assertThat(messageEvent.getMessageData()).isNull();

        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);
        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_MESSAGE_RECEIVED);
    }

    /**
     * Test events related to message events, called from the API, targeting an event-subprocess.
     */
    @Test
    @Deployment(resources = "org/flowable/engine/test/api/event/ActivityEventsTest.testActivityMessageEventsInEventSubprocess.bpmn20.xml")
    public void testActivityMessageEventsInEventSubprocessForCancel() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("messageProcess");
        assertThat(processInstance).isNotNull();

        Execution executionWithMessage = runtimeService.createExecutionQuery().messageEventSubscriptionName("messageName").singleResult();
        assertThat(executionWithMessage).isNotNull();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Wait");

        taskService.complete(task.getId());

        // Only a message events should be present, no signal-event, since the event-subprocess is
        // not signaled, but executed instead
        assertThat(listener.getEventsReceived()).hasSize(2);

        // A message waiting event is expected
        assertThat(listener.getEventsReceived().get(0)).isInstanceOf(FlowableMessageEvent.class);
        FlowableMessageEvent messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(0);
        assertThat(messageEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);
        assertThat(messageEvent.getActivityId()).isEqualTo("catchMessage");
        assertThat(messageEvent.getExecutionId()).isEqualTo(executionWithMessage.getId());
        assertThat(messageEvent.getProcessInstanceId()).isEqualTo(executionWithMessage.getProcessInstanceId());
        assertThat(messageEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(messageEvent.getMessageName()).isEqualTo("messageName");
        assertThat(messageEvent.getMessageData()).isNull();

        // A message received event is expected
        assertThat(listener.getEventsReceived().get(1)).isInstanceOf(FlowableMessageEvent.class);
        messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(1);
        assertThat(messageEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_MESSAGE_CANCELLED);
        assertThat(messageEvent.getActivityId()).isEqualTo("catchMessage");
        assertThat(messageEvent.getExecutionId()).isEqualTo(executionWithMessage.getId());
        assertThat(messageEvent.getProcessInstanceId()).isEqualTo(executionWithMessage.getProcessInstanceId());
        assertThat(messageEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(messageEvent.getMessageName()).isEqualTo("messageName");
        assertThat(messageEvent.getMessageData()).isNull();

        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);
    }

    /**
     * Test events related to compensation events.
     */
    @Test
    @Deployment
    public void testActivityCompensationEvents() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensationProcess");
        assertThat(processInstance).isNotNull();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        // Complete task, next a compensation event will be thrown
        taskService.complete(task.getId());

        assertThat(listener.getEventsReceived()).hasSize(1);

        // A compensate-event is expected
        assertThat(listener.getEventsReceived().get(0)).isInstanceOf(FlowableActivityEvent.class);
        FlowableActivityEvent activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(0);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPENSATE);
        assertThat(activityEvent.getActivityId()).isEqualTo("usertask");
        // A new execution is created for the compensation-event, this should be visible in the event
        assertThat(processInstance.getId()).isNotEqualTo(activityEvent.getExecutionId());
        assertThat(activityEvent.getProcessInstanceId()).isEqualTo(processInstance.getProcessInstanceId());
        assertThat(activityEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());

        // Check if the process is still alive
        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();

        assertThat(processInstance).isNotNull();

        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_COMPENSATE);
    }

    /**
     * Test events related to error-events
     */
    @Test
    @Deployment
    public void testActivityErrorEvents() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("errorProcess");
        assertThat(processInstance).isNotNull();

        // Error-handling should have ended the process
        ProcessInstance afterErrorInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(afterErrorInstance).isNull();

        FlowableErrorEvent errorEvent = null;

        for (FlowableEvent event : listener.getEventsReceived()) {
            if (event instanceof FlowableErrorEvent) {
                if (errorEvent == null) {
                    errorEvent = (FlowableErrorEvent) event;
                } else {
                    fail("Only one ActivityErrorEvent expected");
                }
            }
        }

        assertThat(errorEvent).isNotNull();
        assertThat(errorEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_ERROR_RECEIVED);
        assertThat(errorEvent.getActivityId()).isEqualTo("catchError");
        assertThat(errorEvent.getErrorId()).isEqualTo("myError");
        assertThat(errorEvent.getErrorCode()).isEqualTo("123");
        assertThat(errorEvent.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(errorEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(processInstance.getId()).isNotEqualTo(errorEvent.getExecutionId());
    }

    /**
     * Test events related to error-events, thrown from within process-execution (eg. service-task).
     */
    @Test
    @Deployment
    public void testActivityErrorEventsFromBPMNError() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("errorProcess");
        assertThat(processInstance).isNotNull();

        // Error-handling should have ended the process
        ProcessInstance afterErrorInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(afterErrorInstance).isNull();

        FlowableErrorEvent errorEvent = null;

        for (FlowableEvent event : listener.getEventsReceived()) {
            if (event instanceof FlowableErrorEvent) {
                if (errorEvent == null) {
                    errorEvent = (FlowableErrorEvent) event;
                } else {
                    fail("Only one ActivityErrorEvent expected");
                }
            }
        }

        assertThat(errorEvent).isNotNull();
        assertThat(errorEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_ERROR_RECEIVED);
        assertThat(errorEvent.getActivityId()).isEqualTo("catchError");
        assertThat(errorEvent.getErrorId()).isEqualTo("23");
        assertThat(errorEvent.getErrorCode()).isEqualTo("23");
        assertThat(errorEvent.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(errorEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(processInstance.getId()).isNotEqualTo(errorEvent.getExecutionId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/event/JobEventsTest.testJobEntityEvents.bpmn20.xml")
    public void testActivityTimeOutEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testJobEvents");
        Job theJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(theJob).isNotNull();

        // Force timer to fire
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        processEngineConfiguration.getClock().setCurrentTime(tomorrow.getTime());
        waitForJobExecutorToProcessAllJobs(2000, 1000);

        // Check timeout has been dispatched
        assertThat(listener.getEventsReceived()).hasSize(1);
        FlowableEvent activitiEvent = listener.getEventsReceived().get(0);
        assertThat(activitiEvent.getType()).as("ACTIVITY_CANCELLED event expected")
                .isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        FlowableActivityCancelledEvent cancelledEvent = (FlowableActivityCancelledEvent) activitiEvent;
        assertThat(cancelledEvent.getCause()).as("Boundary timer is the cause of the cancellation")
                .isInstanceOf(BoundaryEvent.class);
        BoundaryEvent boundaryEvent = (BoundaryEvent) cancelledEvent.getCause();
        assertThat(boundaryEvent.getEventDefinitions().get(0)).as("Boundary timer is the cause of the cancellation")
                .isInstanceOf(TimerEventDefinition.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/timer/BoundaryTimerEventTest.testTimerOnNestingOfSubprocesses.bpmn20.xml")
    public void testActivityTimeOutEventInSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerOnNestedSubprocesses");
        Job theJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(theJob).isNotNull();

        // Force timer to fire
        Calendar timeToFire = Calendar.getInstance();
        timeToFire.add(Calendar.HOUR, 2);
        timeToFire.add(Calendar.SECOND, 5);
        processEngineConfiguration.getClock().setCurrentTime(timeToFire.getTime());
        waitForJobExecutorToProcessAllJobs(2000, 200);

        // Check timeout-events have been dispatched
        assertThat(listener.getEventsReceived()).hasSize(4);
        List<String> eventIdList = new ArrayList<>();
        for (FlowableEvent event : listener.getEventsReceived()) {
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
            assertThat(((FlowableActivityCancelledEvent) event).getCause()).as("Boundary timer is the cause of the cancellation")
                    .isInstanceOf(BoundaryEvent.class);
            assertThat(((BoundaryEvent) ((FlowableActivityCancelledEvent) event).getCause()).getEventDefinitions().get(0))
                    .as("Boundary timer is the cause of the cancellation")
                    .isInstanceOf(TimerEventDefinition.class);
            eventIdList.add(((FlowableActivityEventImpl) event).getActivityId());
        }
        assertThat(eventIdList.indexOf("innerTask1")).isGreaterThanOrEqualTo(0);
        assertThat(eventIdList.indexOf("innerTask2")).isGreaterThanOrEqualTo(0);
        assertThat(eventIdList.indexOf("subprocess")).isGreaterThanOrEqualTo(0);
        assertThat(eventIdList.indexOf("innerSubprocess")).isGreaterThanOrEqualTo(0);
    }

    @Test
    @Deployment
    public void testActivityTimeOutEventInCallActivity() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerOnCallActivity");
        Job theJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(theJob).isNotNull();

        // Force timer to fire
        Calendar timeToFire = Calendar.getInstance();
        timeToFire.add(Calendar.HOUR, 2);
        timeToFire.add(Calendar.MINUTE, 5);
        processEngineConfiguration.getClock().setCurrentTime(timeToFire.getTime());
        waitForJobExecutorToProcessAllJobs(7000, 500);

        // Check timeout-events have been dispatched
        assertThat(listener.getEventsReceived()).hasSize(4);
        List<String> eventIdList = new ArrayList<>();
        for (FlowableEvent event : listener.getEventsReceived()) {
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
            assertThat(((FlowableCancelledEvent) event).getCause()).as("Boundary timer is the cause of the cancellation")
                    .isInstanceOf(BoundaryEvent.class);
            BoundaryEvent boundaryEvent = (BoundaryEvent) ((FlowableCancelledEvent) event).getCause();
            assertThat(boundaryEvent.getEventDefinitions().get(0)).as("Boundary timer is the cause of the cancellation")
                    .isInstanceOf(TimerEventDefinition.class);
            eventIdList.add(((FlowableActivityEventImpl) event).getActivityId());
        }
        assertThat(eventIdList)
                .containsOnly("innerTask1", "innerTask2", "callActivity", "innerSubprocess");
    }

    /**
     * Test events related to message events, called from the API.
     */
    @Test
    @Deployment
    public void testActivityMessageBoundaryEventsOnUserTask() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("messageOnUserTaskProcess");
        assertThat(processInstance).isNotNull();

        Execution executionWithMessage = runtimeService.createExecutionQuery().messageEventSubscriptionName("message_1").singleResult();
        assertThat(executionWithMessage).isNotNull();

        runtimeService.messageEventReceived("message_1", executionWithMessage.getId());
        assertThat(listener.getEventsReceived()).hasSize(3);

        // First, a message waiting event is expected
        assertThat(listener.getEventsReceived().get(0)).isInstanceOf(FlowableMessageEvent.class);
        FlowableMessageEvent messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(0);
        assertThat(messageEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);
        assertThat(messageEvent.getActivityId()).isEqualTo("boundaryMessageEventCatching");
        assertThat(messageEvent.getExecutionId()).isEqualTo(executionWithMessage.getId());
        assertThat(messageEvent.getProcessInstanceId()).isEqualTo(executionWithMessage.getProcessInstanceId());
        assertThat(messageEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(messageEvent.getMessageName()).isEqualTo("message_1");
        assertThat(messageEvent.getMessageData()).isNull();

        // Second, a message received event is expected
        assertThat(listener.getEventsReceived().get(1)).isInstanceOf(FlowableMessageEvent.class);
        messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(1);
        assertThat(messageEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_MESSAGE_RECEIVED);
        assertThat(messageEvent.getActivityId()).isEqualTo("boundaryMessageEventCatching");
        assertThat(messageEvent.getExecutionId()).isEqualTo(executionWithMessage.getId());
        assertThat(messageEvent.getProcessInstanceId()).isEqualTo(executionWithMessage.getProcessInstanceId());
        assertThat(messageEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(messageEvent.getMessageName()).isEqualTo("message_1");
        assertThat(messageEvent.getMessageData()).isNull();

        // Next, an signal-event is expected, as a result of the message
        assertThat(listener.getEventsReceived().get(2)).isInstanceOf(FlowableActivityCancelledEvent.class);
        FlowableActivityCancelledEvent signalEvent = (FlowableActivityCancelledEvent) listener.getEventsReceived().get(2);
        assertThat(signalEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(signalEvent.getActivityId()).isEqualTo("cloudformtask1");
        assertThat(signalEvent.getProcessInstanceId()).isEqualTo(executionWithMessage.getProcessInstanceId());
        assertThat(signalEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(signalEvent.getCause()).isInstanceOf(BoundaryEvent.class);
        BoundaryEvent cause = (BoundaryEvent) signalEvent.getCause();
        assertThat(((MessageEventDefinition) cause.getEventDefinitions().get(0)).getMessageRef()).isEqualTo("message_1");

        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);
        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_MESSAGE_RECEIVED);
    }

    /**
     * Test events related to message events, called from the API.
     */
    @Test
    @Deployment(resources = "org/flowable/engine/test/api/event/ActivityEventsTest.testActivityMessageBoundaryEventsOnUserTask.bpmn20.xml")
    public void testActivityMessageBoundaryEventsOnUserTaskForCancel() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("messageOnUserTaskProcess");
        assertThat(processInstance).isNotNull();

        Execution executionWithMessage = runtimeService.createExecutionQuery().messageEventSubscriptionName("message_1").singleResult();
        assertThat(executionWithMessage).isNotNull();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("User Task");
        taskService.complete(task.getId());

        assertThat(listener.getEventsReceived()).hasSize(2);

        // First, a message waiting event is expected
        assertThat(listener.getEventsReceived().get(0)).isInstanceOf(FlowableMessageEvent.class);
        FlowableMessageEvent messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(0);
        assertThat(messageEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);
        assertThat(messageEvent.getActivityId()).isEqualTo("boundaryMessageEventCatching");
        assertThat(messageEvent.getExecutionId()).isEqualTo(executionWithMessage.getId());
        assertThat(messageEvent.getProcessInstanceId()).isEqualTo(executionWithMessage.getProcessInstanceId());
        assertThat(messageEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(messageEvent.getMessageName()).isEqualTo("message_1");
        assertThat(messageEvent.getMessageData()).isNull();

        // Second, a message received event is expected
        assertThat(listener.getEventsReceived().get(1)).isInstanceOf(FlowableMessageEvent.class);
        messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(1);
        assertThat(messageEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_MESSAGE_CANCELLED);
        assertThat(messageEvent.getActivityId()).isEqualTo("boundaryMessageEventCatching");
        assertThat(messageEvent.getExecutionId()).isEqualTo(executionWithMessage.getId());
        assertThat(messageEvent.getProcessInstanceId()).isEqualTo(executionWithMessage.getProcessInstanceId());
        assertThat(messageEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(messageEvent.getMessageName()).isEqualTo("message_1");
        assertThat(messageEvent.getMessageData()).isNull();

        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);
    }

    /**
     * Test events related to message events, called from the API.
     */
    @Test
    @Deployment
    public void testActivityMessageBoundaryEventsOnSubProcess() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("messageOnSubProcess");
        assertThat(processInstance).isNotNull();

        Execution executionWithMessage = runtimeService.createExecutionQuery().activityId("boundaryMessageEventCatching").singleResult();
        assertThat(executionWithMessage).isNotNull();

        runtimeService.messageEventReceived("message_1", executionWithMessage.getId());
        assertThat(listener.getEventsReceived()).hasSize(4);

        // First, a message waiting event is expected
        assertThat(listener.getEventsReceived().get(0)).isInstanceOf(FlowableMessageEvent.class);
        FlowableMessageEvent messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(0);
        assertThat(messageEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);
        assertThat(messageEvent.getActivityId()).isEqualTo("boundaryMessageEventCatching");
        assertThat(messageEvent.getExecutionId()).isEqualTo(executionWithMessage.getId());
        assertThat(messageEvent.getProcessInstanceId()).isEqualTo(executionWithMessage.getProcessInstanceId());
        assertThat(messageEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(messageEvent.getMessageName()).isEqualTo("message_1");
        assertThat(messageEvent.getMessageData()).isNull();

        // Second, a message received event is expected
        assertThat(listener.getEventsReceived().get(1)).isInstanceOf(FlowableMessageEvent.class);
        messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(1);
        assertThat(messageEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_MESSAGE_RECEIVED);
        assertThat(messageEvent.getActivityId()).isEqualTo("boundaryMessageEventCatching");
        assertThat(messageEvent.getExecutionId()).isEqualTo(executionWithMessage.getId());
        assertThat(messageEvent.getProcessInstanceId()).isEqualTo(executionWithMessage.getProcessInstanceId());
        assertThat(messageEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(messageEvent.getMessageName()).isEqualTo("message_1");
        assertThat(messageEvent.getMessageData()).isNull();

        // Next, an signal-event is expected, as a result of the message

        assertThat(listener.getEventsReceived().get(2)).isInstanceOf(FlowableActivityCancelledEvent.class);
        FlowableActivityCancelledEvent cancelEvent = (FlowableActivityCancelledEvent) listener.getEventsReceived().get(2);
        assertThat(cancelEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(cancelEvent.getActivityId()).isEqualTo("cloudformtask1");
        assertThat(cancelEvent.getProcessInstanceId()).isEqualTo(executionWithMessage.getProcessInstanceId());
        assertThat(cancelEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(cancelEvent.getCause()).isInstanceOf(BoundaryEvent.class);
        BoundaryEvent cause = (BoundaryEvent) cancelEvent.getCause();
        assertThat(((MessageEventDefinition) cause.getEventDefinitions().get(0)).getMessageRef()).isEqualTo("message_1");

        assertThat(listener.getEventsReceived().get(3)).isInstanceOf(FlowableActivityCancelledEvent.class);
        cancelEvent = (FlowableActivityCancelledEvent) listener.getEventsReceived().get(3);
        assertThat(cancelEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(cancelEvent.getActivityId()).isEqualTo("subProcess");
        assertThat(cancelEvent.getProcessInstanceId()).isEqualTo(executionWithMessage.getProcessInstanceId());
        assertThat(cancelEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(cancelEvent.getCause()).isInstanceOf(BoundaryEvent.class);
        cause = (BoundaryEvent) cancelEvent.getCause();
        assertThat(((MessageEventDefinition) cause.getEventDefinitions().get(0)).getMessageRef()).isEqualTo("message_1");

        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);
        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_MESSAGE_RECEIVED);
    }

    /**
     * Test events related to message events, called from the API.
     */
    @Test
    @Deployment(resources = "org/flowable/engine/test/api/event/ActivityEventsTest.testActivityMessageBoundaryEventsOnSubProcess.bpmn20.xml")
    public void testActivityMessageBoundaryEventsOnSubProcessForCancel() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("messageOnSubProcess");
        assertThat(processInstance).isNotNull();

        Execution executionWithMessage = runtimeService.createExecutionQuery().activityId("boundaryMessageEventCatching").singleResult();
        assertThat(executionWithMessage).isNotNull();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        assertThat(listener.getEventsReceived()).hasSize(2);

        // First, a message waiting event is expected
        assertThat(listener.getEventsReceived().get(0)).isInstanceOf(FlowableMessageEvent.class);
        FlowableMessageEvent messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(0);
        assertThat(messageEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);
        assertThat(messageEvent.getActivityId()).isEqualTo("boundaryMessageEventCatching");
        assertThat(messageEvent.getExecutionId()).isEqualTo(executionWithMessage.getId());
        assertThat(messageEvent.getProcessInstanceId()).isEqualTo(executionWithMessage.getProcessInstanceId());
        assertThat(messageEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(messageEvent.getMessageName()).isEqualTo("message_1");
        assertThat(messageEvent.getMessageData()).isNull();

        // Second, a message received event is expected
        assertThat(listener.getEventsReceived().get(1)).isInstanceOf(FlowableMessageEvent.class);
        messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(1);
        assertThat(messageEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_MESSAGE_CANCELLED);
        assertThat(messageEvent.getActivityId()).isEqualTo("boundaryMessageEventCatching");
        assertThat(messageEvent.getExecutionId()).isEqualTo(executionWithMessage.getId());
        assertThat(messageEvent.getProcessInstanceId()).isEqualTo(executionWithMessage.getProcessInstanceId());
        assertThat(messageEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(messageEvent.getMessageName()).isEqualTo("message_1");
        assertThat(messageEvent.getMessageData()).isNull();

        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);
    }

    @Test
    @Deployment
    public void testActivitySignalBoundaryEventsOnSubProcess() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("signalOnSubProcess");
        assertThat(processInstance).isNotNull();

        Execution executionWithSignal = runtimeService.createExecutionQuery().activityId("userTaskInsideProcess").singleResult();
        assertThat(executionWithSignal).isNotNull();

        runtimeService.signalEventReceived("signalName");
        assertThat(listener.getEventsReceived()).hasSize(4);

        assertThat(listener.getEventsReceived().get(0)).isInstanceOf(FlowableSignalEventImpl.class);
        FlowableSignalEventImpl signalEvent = (FlowableSignalEventImpl) listener.getEventsReceived().get(0);
        assertThat(signalEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING);
        assertThat(signalEvent.getActivityId()).isEqualTo("boundarySignalEventCatching");
        assertThat(signalEvent.getProcessInstanceId()).isEqualTo(executionWithSignal.getProcessInstanceId());
        assertThat(signalEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());

        assertThat(listener.getEventsReceived().get(1)).isInstanceOf(FlowableSignalEventImpl.class);
        signalEvent = (FlowableSignalEventImpl) listener.getEventsReceived().get(1);
        assertThat(signalEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_SIGNALED);
        assertThat(signalEvent.getActivityId()).isEqualTo("boundarySignalEventCatching");
        assertThat(signalEvent.getProcessInstanceId()).isEqualTo(executionWithSignal.getProcessInstanceId());
        assertThat(signalEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());

        assertThat(listener.getEventsReceived().get(2)).isInstanceOf(FlowableActivityCancelledEvent.class);
        FlowableActivityCancelledEvent cancelEvent = (FlowableActivityCancelledEvent) listener.getEventsReceived().get(2);
        assertThat(cancelEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(cancelEvent.getActivityId()).isEqualTo("userTaskInsideProcess");
        assertThat(cancelEvent.getExecutionId()).isEqualTo(executionWithSignal.getId());
        assertThat(cancelEvent.getProcessInstanceId()).isEqualTo(executionWithSignal.getProcessInstanceId());
        assertThat(cancelEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(cancelEvent.getCause()).isInstanceOf(BoundaryEvent.class);
        BoundaryEvent cause = (BoundaryEvent) cancelEvent.getCause();
        assertThat(cause.getEventDefinitions().get(0)).isInstanceOf(SignalEventDefinition.class);
        SignalEventDefinition signalEventDefinition = ((SignalEventDefinition) cause.getEventDefinitions().get(0));
        assertThat(signalEventDefinition.getSignalRef()).isEqualTo("signal");
        assertThat(repositoryService.getBpmnModel(cancelEvent.getProcessDefinitionId()).getSignal(signalEventDefinition.getSignalRef()).getName())
                .isEqualTo("signalName");

        assertThat(listener.getEventsReceived().get(3)).isInstanceOf(FlowableActivityCancelledEvent.class);
        cancelEvent = (FlowableActivityCancelledEvent) listener.getEventsReceived().get(3);
        assertThat(cancelEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(cancelEvent.getActivityId()).isEqualTo("subProcess");
        assertThat(cancelEvent.getProcessInstanceId()).isEqualTo(executionWithSignal.getProcessInstanceId());
        assertThat(cancelEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(cancelEvent.getCause()).isInstanceOf(BoundaryEvent.class);
        cause = (BoundaryEvent) cancelEvent.getCause();
        assertThat(cause.getEventDefinitions().get(0)).isInstanceOf(SignalEventDefinition.class);
        signalEventDefinition = ((SignalEventDefinition) cause.getEventDefinitions().get(0));
        assertThat(signalEventDefinition.getSignalRef()).isEqualTo("signal");
        assertThat(repositoryService.getBpmnModel(cancelEvent.getProcessDefinitionId()).getSignal(signalEventDefinition.getSignalRef()).getName())
                .isEqualTo("signalName");
    }

    @Test
    @Deployment
    public void testActivitySignalBoundaryEventsOnUserTask() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("signalOnUserTask");
        assertThat(processInstance).isNotNull();

        Execution executionWithSignal = runtimeService.createExecutionQuery().activityId("userTask").singleResult();
        assertThat(executionWithSignal).isNotNull();

        runtimeService.signalEventReceived("signalName");

        // Next, an signal-event is expected, as a result of the message
        assertThat(listener.getEventsReceived()).hasSize(3);

        assertThat(listener.getEventsReceived().get(0)).isInstanceOf(FlowableSignalEventImpl.class);
        FlowableSignalEventImpl signalEvent = (FlowableSignalEventImpl) listener.getEventsReceived().get(0);
        assertThat(signalEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING);
        assertThat(signalEvent.getActivityId()).isEqualTo("boundarySignalEventCatching");
        assertThat(signalEvent.getProcessInstanceId()).isEqualTo(executionWithSignal.getProcessInstanceId());
        assertThat(signalEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());

        assertThat(listener.getEventsReceived().get(1)).isInstanceOf(FlowableSignalEventImpl.class);
        signalEvent = (FlowableSignalEventImpl) listener.getEventsReceived().get(1);
        assertThat(signalEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_SIGNALED);
        assertThat(signalEvent.getActivityId()).isEqualTo("boundarySignalEventCatching");
        assertThat(signalEvent.getProcessInstanceId()).isEqualTo(executionWithSignal.getProcessInstanceId());
        assertThat(signalEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());

        assertThat(listener.getEventsReceived().get(2)).isInstanceOf(FlowableActivityCancelledEvent.class);
        FlowableActivityCancelledEvent cancelEvent = (FlowableActivityCancelledEvent) listener.getEventsReceived().get(2);
        assertThat(cancelEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(cancelEvent.getActivityId()).isEqualTo("userTask");
        assertThat(cancelEvent.getProcessInstanceId()).isEqualTo(executionWithSignal.getProcessInstanceId());
        assertThat(cancelEvent.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(cancelEvent.getCause()).isInstanceOf(BoundaryEvent.class);
        BoundaryEvent cause = (BoundaryEvent) cancelEvent.getCause();
        assertThat(cause.getEventDefinitions().get(0)).isInstanceOf(SignalEventDefinition.class);
        SignalEventDefinition signalEventDefinition = ((SignalEventDefinition) cause.getEventDefinitions().get(0));
        assertThat(signalEventDefinition.getSignalRef()).isEqualTo("signal");
        assertThat(repositoryService.getBpmnModel(cancelEvent.getProcessDefinitionId()).getSignal(signalEventDefinition.getSignalRef()).getName())
                .isEqualTo("signalName");
    }

    protected void assertDatabaseEventPresent(FlowableEngineEventType eventType) {
        String eventTypeString = eventType.name();
        List<EventLogEntry> eventLogEntries = managementService.getEventLogEntries(0L, 100000L);
        boolean found = false;
        for (EventLogEntry entry : eventLogEntries) {
            if (entry.getType().equals(eventTypeString)) {
                found = true;
            }
        }
        assertThat(found).isTrue();
    }

}
