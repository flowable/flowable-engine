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

/**
 * Test case for all {@link FlowableEvent}s related to activities.
 * 
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class ActivityEventsTest extends PluggableFlowableTestCase {

    private TestFlowableActivityEventListener listener;

    protected EventLogger databaseEventLogger;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Database event logger setup
        databaseEventLogger = new EventLogger(processEngineConfiguration.getClock(), processEngineConfiguration.getObjectMapper());
        runtimeService.addEventListener(databaseEventLogger);
    }

    @Override
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

        super.tearDown();
    }

    @Override
    protected void initializeServices() {
        super.initializeServices();

        listener = new TestFlowableActivityEventListener(true);
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    /**
     * Test starting and completed events for activity. Since these events are dispatched in the core of the PVM, not all individual activity-type is tested. Rather, we test the main types (tasks,
     * gateways, events, subprocesses).
     */
    @Deployment
    public void testActivityEvents() throws Exception {
        // We're interested in the raw events, alter the listener to keep those as well
        listener.setIgnoreRawActivityEvents(false);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("activityProcess");
        assertNotNull(processInstance);

        assertEquals(3, listener.getEventsReceived().size());

        // Start-event activity started
        FlowableActivityEvent activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("theStart", activityEvent.getActivityId());
        assertFalse(processInstance.getId().equals(activityEvent.getExecutionId()));
        assertEquals(processInstance.getProcessInstanceId(), activityEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), activityEvent.getProcessDefinitionId());

        // Start-event finished
        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("theStart", activityEvent.getActivityId());
        assertFalse(processInstance.getId().equals(activityEvent.getExecutionId()));
        assertEquals(processInstance.getProcessInstanceId(), activityEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), activityEvent.getProcessDefinitionId());

        // Usertask started
        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(2);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("shipOrder", activityEvent.getActivityId());
        assertFalse(processInstance.getId().equals(activityEvent.getExecutionId()));
        assertEquals(processInstance.getProcessInstanceId(), activityEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), activityEvent.getProcessDefinitionId());

        // Complete usertask
        listener.clearEventsReceived();
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);
        taskService.complete(task.getId());

        // Subprocess execution is created
        Execution execution = runtimeService.createExecutionQuery().parentId(processInstance.getId()).singleResult();
        assertNotNull(execution);
        assertEquals(5, listener.getEventsReceived().size());
        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("shipOrder", activityEvent.getActivityId());
        assertFalse(processInstance.getId().equals(activityEvent.getExecutionId()));
        assertEquals(processInstance.getProcessInstanceId(), activityEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), activityEvent.getProcessDefinitionId());

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("subProcess", activityEvent.getActivityId());
        assertEquals(execution.getId(), activityEvent.getExecutionId());
        assertEquals(processInstance.getProcessInstanceId(), activityEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), activityEvent.getProcessDefinitionId());

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(2);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("subProcessStart", activityEvent.getActivityId());
        assertFalse(execution.getId().equals(activityEvent.getExecutionId()));
        assertEquals(processInstance.getProcessInstanceId(), activityEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), activityEvent.getProcessDefinitionId());

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(3);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("subProcessStart", activityEvent.getActivityId());
        assertFalse(execution.getId().equals(activityEvent.getExecutionId()));
        assertEquals(processInstance.getProcessInstanceId(), activityEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), activityEvent.getProcessDefinitionId());

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(4);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("subTask", activityEvent.getActivityId());
        assertFalse(execution.getId().equals(activityEvent.getExecutionId()));
        assertEquals(processInstance.getProcessInstanceId(), activityEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), activityEvent.getProcessDefinitionId());
        listener.clearEventsReceived();

        // Check gateway and intermediate throw event
        org.flowable.task.api.Task subTask = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).singleResult();
        assertNotNull(subTask);

        taskService.complete(subTask.getId());

        assertEquals(10, listener.getEventsReceived().size());

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("subTask", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("gateway", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(2);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("gateway", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(3);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("throwMessageEvent", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(4);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("throwMessageEvent", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(5);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("endSubProcess", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(6);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("endSubProcess", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(7);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("subProcess", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(8);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("theEnd", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(9);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("theEnd", activityEvent.getActivityId());
    }

    /**
     * Test events related to signalling
     */
    @Deployment
    public void testActivitySignalEvents() throws Exception {
        // Two paths are active in the process, one receive-task and one intermediate catching signal-event
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("signalProcess");
        assertNotNull(processInstance);

        // Check regular signal through API
        Execution executionWithSignal = runtimeService.createExecutionQuery().activityId("receivePayment").singleResult();
        assertNotNull(executionWithSignal);

        Execution signalExecution = runtimeService.createExecutionQuery().signalEventSubscriptionName("alert").singleResult();
        assertNotNull(signalExecution);

        runtimeService.trigger(executionWithSignal.getId());
        assertEquals(2, listener.getEventsReceived().size());

        assertTrue(listener.getEventsReceived().get(0) instanceof FlowableSignalEvent);
        FlowableSignalEvent signalEvent = (FlowableSignalEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING, signalEvent.getType());
        assertEquals("shipOrder", signalEvent.getActivityId());
        assertEquals(signalExecution.getId(), signalEvent.getExecutionId());
        assertEquals(signalExecution.getProcessInstanceId(), signalEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), signalEvent.getProcessDefinitionId());
        assertEquals("shipOrderSignal", signalEvent.getSignalName());
        assertNull(signalEvent.getSignalData());

        assertTrue(listener.getEventsReceived().get(1) instanceof FlowableSignalEvent);
        signalEvent = (FlowableSignalEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ACTIVITY_SIGNALED, signalEvent.getType());
        assertEquals("receivePayment", signalEvent.getActivityId());
        assertEquals(executionWithSignal.getId(), signalEvent.getExecutionId());
        assertEquals(executionWithSignal.getProcessInstanceId(), signalEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), signalEvent.getProcessDefinitionId());
        assertNull(signalEvent.getSignalName());
        assertNull(signalEvent.getSignalData());
        listener.clearEventsReceived();

        // Check signal using event, and pass in additional payload
        Execution executionWithSignalEvent = runtimeService.createExecutionQuery().activityId("shipOrder").singleResult();
        runtimeService.signalEventReceived("alert", executionWithSignalEvent.getId(), Collections.singletonMap("test", (Object) "test"));
        assertEquals(1, listener.getEventsReceived().size());
        assertTrue(listener.getEventsReceived().get(0) instanceof FlowableSignalEvent);
        signalEvent = (FlowableSignalEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ACTIVITY_SIGNALED, signalEvent.getType());
        assertEquals("shipOrder", signalEvent.getActivityId());
        assertEquals(executionWithSignalEvent.getId(), signalEvent.getExecutionId());
        assertEquals(executionWithSignalEvent.getProcessInstanceId(), signalEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), signalEvent.getProcessDefinitionId());
        assertEquals("alert", signalEvent.getSignalName());
        assertNotNull(signalEvent.getSignalData());
        listener.clearEventsReceived();

        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_SIGNALED);
    }

    /**
     * Test to verify if signals coming from an intermediate throw-event trigger the right events to be dispatched.
     */
    @Deployment
    public void testActivitySignalEventsWithinProcess() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("signalProcess");
        assertNotNull(processInstance);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);

        Execution executionWithSignalEvent = runtimeService.createExecutionQuery().activityId("shipOrder").singleResult();

        taskService.complete(task.getId());
        assertEquals(2, listener.getEventsReceived().size());

        assertTrue(listener.getEventsReceived().get(0) instanceof FlowableSignalEvent);
        FlowableSignalEvent signalEvent = (FlowableSignalEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING, signalEvent.getType());
        assertEquals("shipOrder", signalEvent.getActivityId());
        assertEquals(executionWithSignalEvent.getId(), signalEvent.getExecutionId());
        assertEquals(executionWithSignalEvent.getProcessInstanceId(), signalEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), signalEvent.getProcessDefinitionId());
        assertEquals("shipOrderSignal", signalEvent.getSignalName());
        assertNull(signalEvent.getSignalData());

        assertTrue(listener.getEventsReceived().get(1) instanceof FlowableSignalEvent);
        signalEvent = (FlowableSignalEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ACTIVITY_SIGNALED, signalEvent.getType());
        assertEquals("shipOrder", signalEvent.getActivityId());
        assertEquals(executionWithSignalEvent.getId(), signalEvent.getExecutionId());
        assertEquals(executionWithSignalEvent.getProcessInstanceId(), signalEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), signalEvent.getProcessDefinitionId());
        assertEquals("alert", signalEvent.getSignalName());
        assertNull(signalEvent.getSignalData());
    }

    /**
     * Test events related to message events, called from the API.
     */
    @Deployment
    public void testActivityMessageEvents() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("messageProcess");
        assertNotNull(processInstance);

        Execution executionWithMessage = runtimeService.createExecutionQuery().activityId("shipOrder").singleResult();
        assertNotNull(executionWithMessage);

        runtimeService.messageEventReceived("messageName", executionWithMessage.getId());
        assertEquals(2, listener.getEventsReceived().size());

        // First, a message waiting event is expected
        assertTrue(listener.getEventsReceived().get(0) instanceof FlowableMessageEvent);
        FlowableMessageEvent messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING, messageEvent.getType());
        assertEquals("shipOrder", messageEvent.getActivityId());
        assertEquals(executionWithMessage.getId(), messageEvent.getExecutionId());
        assertEquals(executionWithMessage.getProcessInstanceId(), messageEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), messageEvent.getProcessDefinitionId());
        assertEquals("messageName", messageEvent.getMessageName());
        assertNull(messageEvent.getMessageData());

        // Second, a message received event is expected
        assertTrue(listener.getEventsReceived().get(1) instanceof FlowableMessageEvent);
        messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ACTIVITY_MESSAGE_RECEIVED, messageEvent.getType());
        assertEquals("shipOrder", messageEvent.getActivityId());
        assertEquals(executionWithMessage.getId(), messageEvent.getExecutionId());
        assertEquals(executionWithMessage.getProcessInstanceId(), messageEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), messageEvent.getProcessDefinitionId());
        assertEquals("messageName", messageEvent.getMessageName());
        assertNull(messageEvent.getMessageData());

        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_MESSAGE_RECEIVED);
    }

    /**
     * Test events related to message events, called from the API, targeting an event-subprocess.
     */
    @Deployment
    public void testActivityMessageEventsInEventSubprocess() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("messageProcess");
        assertNotNull(processInstance);

        Execution executionWithMessage = runtimeService.createExecutionQuery().messageEventSubscriptionName("messageName").singleResult();
        assertNotNull(executionWithMessage);

        runtimeService.messageEventReceived("messageName", executionWithMessage.getId());

        // Only a message events should be present, no signal-event, since the event-subprocess is
        // not signaled, but executed instead
        assertEquals(2, listener.getEventsReceived().size());

        // A message waiting event is expected
        assertTrue(listener.getEventsReceived().get(0) instanceof FlowableMessageEvent);
        FlowableMessageEvent messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING, messageEvent.getType());
        assertEquals("catchMessage", messageEvent.getActivityId());
        assertEquals(executionWithMessage.getId(), messageEvent.getExecutionId());
        assertEquals(executionWithMessage.getProcessInstanceId(), messageEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), messageEvent.getProcessDefinitionId());
        assertEquals("messageName", messageEvent.getMessageName());
        assertNull(messageEvent.getMessageData());

        // A message received event is expected
        assertTrue(listener.getEventsReceived().get(1) instanceof FlowableMessageEvent);
        messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ACTIVITY_MESSAGE_RECEIVED, messageEvent.getType());
        assertEquals("catchMessage", messageEvent.getActivityId());
        assertEquals(executionWithMessage.getId(), messageEvent.getExecutionId());
        assertEquals(executionWithMessage.getProcessInstanceId(), messageEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), messageEvent.getProcessDefinitionId());
        assertEquals("messageName", messageEvent.getMessageName());
        assertNull(messageEvent.getMessageData());

        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);
        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_MESSAGE_RECEIVED);
    }

    /**
     * Test events related to message events, called from the API, targeting an event-subprocess.
     */
    @Deployment(resources = "org/flowable/engine/test/api/event/ActivityEventsTest.testActivityMessageEventsInEventSubprocess.bpmn20.xml")
    public void testActivityMessageEventsInEventSubprocessForCancel() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("messageProcess");
        assertNotNull(processInstance);

        Execution executionWithMessage = runtimeService.createExecutionQuery().messageEventSubscriptionName("messageName").singleResult();
        assertNotNull(executionWithMessage);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("Wait", task.getName());

        taskService.complete(task.getId());

        // Only a message events should be present, no signal-event, since the event-subprocess is
        // not signaled, but executed instead
        assertEquals(2, listener.getEventsReceived().size());

        // A message waiting event is expected
        assertTrue(listener.getEventsReceived().get(0) instanceof FlowableMessageEvent);
        FlowableMessageEvent messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING, messageEvent.getType());
        assertEquals("catchMessage", messageEvent.getActivityId());
        assertEquals(executionWithMessage.getId(), messageEvent.getExecutionId());
        assertEquals(executionWithMessage.getProcessInstanceId(), messageEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), messageEvent.getProcessDefinitionId());
        assertEquals("messageName", messageEvent.getMessageName());
        assertNull(messageEvent.getMessageData());

        // A message received event is expected
        assertTrue(listener.getEventsReceived().get(1) instanceof FlowableMessageEvent);
        messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ACTIVITY_MESSAGE_CANCELLED, messageEvent.getType());
        assertEquals("catchMessage", messageEvent.getActivityId());
        assertEquals(executionWithMessage.getId(), messageEvent.getExecutionId());
        assertEquals(executionWithMessage.getProcessInstanceId(), messageEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), messageEvent.getProcessDefinitionId());
        assertEquals("messageName", messageEvent.getMessageName());
        assertNull(messageEvent.getMessageData());

        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);
    }

    /**
     * Test events related to compensation events.
     */
    @Deployment
    public void testActivityCompensationEvents() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensationProcess");
        assertNotNull(processInstance);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);

        // Complete task, next a compensation event will be thrown
        taskService.complete(task.getId());

        assertEquals(1, listener.getEventsReceived().size());

        // A compensate-event is expected
        assertTrue(listener.getEventsReceived().get(0) instanceof FlowableActivityEvent);
        FlowableActivityEvent activityEvent = (FlowableActivityEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPENSATE, activityEvent.getType());
        assertEquals("compensate", activityEvent.getActivityId());
        // A new execution is created for the compensation-event, this should be
        // visible in the event
        assertFalse(processInstance.getId().equals(activityEvent.getExecutionId()));
        assertEquals(processInstance.getProcessInstanceId(), activityEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), activityEvent.getProcessDefinitionId());

        // Check if the process is still alive
        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();

        assertNotNull(processInstance);

        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_COMPENSATE);
    }

    /**
     * Test events related to error-events
     */
    @Deployment
    public void testActivityErrorEvents() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("errorProcess");
        assertNotNull(processInstance);

        // Error-handling should have ended the process
        ProcessInstance afterErrorInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(afterErrorInstance);

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

        assertNotNull(errorEvent);
        assertEquals(FlowableEngineEventType.ACTIVITY_ERROR_RECEIVED, errorEvent.getType());
        assertEquals("catchError", errorEvent.getActivityId());
        assertEquals("myError", errorEvent.getErrorId());
        assertEquals("123", errorEvent.getErrorCode());
        assertEquals(processInstance.getId(), errorEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), errorEvent.getProcessDefinitionId());
        assertFalse(processInstance.getId().equals(errorEvent.getExecutionId()));
    }

    /**
     * Test events related to error-events, thrown from within process-execution (eg. service-task).
     */
    @Deployment
    public void testActivityErrorEventsFromBPMNError() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("errorProcess");
        assertNotNull(processInstance);

        // Error-handling should have ended the process
        ProcessInstance afterErrorInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(afterErrorInstance);

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

        assertNotNull(errorEvent);
        assertEquals(FlowableEngineEventType.ACTIVITY_ERROR_RECEIVED, errorEvent.getType());
        assertEquals("catchError", errorEvent.getActivityId());
        assertEquals("23", errorEvent.getErrorId());
        assertEquals("23", errorEvent.getErrorCode());
        assertEquals(processInstance.getId(), errorEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), errorEvent.getProcessDefinitionId());
        assertFalse(processInstance.getId().equals(errorEvent.getExecutionId()));
    }

    @Deployment(resources = "org/flowable/engine/test/api/event/JobEventsTest.testJobEntityEvents.bpmn20.xml")
    public void testActivityTimeOutEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testJobEvents");
        Job theJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(theJob);

        // Force timer to fire
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        processEngineConfiguration.getClock().setCurrentTime(tomorrow.getTime());
        waitForJobExecutorToProcessAllJobs(2000, 1000);

        // Check timeout has been dispatched
        assertEquals(1, listener.getEventsReceived().size());
        FlowableEvent activitiEvent = listener.getEventsReceived().get(0);
        assertEquals("ACTIVITY_CANCELLED event expected", FlowableEngineEventType.ACTIVITY_CANCELLED, activitiEvent.getType());
        FlowableActivityCancelledEvent cancelledEvent = (FlowableActivityCancelledEvent) activitiEvent;
        assertTrue("Boundary timer is the cause of the cancellation", cancelledEvent.getCause() instanceof BoundaryEvent);
        BoundaryEvent boundaryEvent = (BoundaryEvent) cancelledEvent.getCause();
        assertTrue("Boundary timer is the cause of the cancellation", boundaryEvent.getEventDefinitions().get(0) instanceof TimerEventDefinition);
    }

    @Deployment(resources = "org/flowable/engine/test/bpmn/event/timer/BoundaryTimerEventTest.testTimerOnNestingOfSubprocesses.bpmn20.xml")
    public void testActivityTimeOutEventInSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerOnNestedSubprocesses");
        Job theJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(theJob);

        // Force timer to fire
        Calendar timeToFire = Calendar.getInstance();
        timeToFire.add(Calendar.HOUR, 2);
        timeToFire.add(Calendar.SECOND, 5);
        processEngineConfiguration.getClock().setCurrentTime(timeToFire.getTime());
        waitForJobExecutorToProcessAllJobs(2000, 200);

        // Check timeout-events have been dispatched
        assertEquals(4, listener.getEventsReceived().size());
        List<String> eventIdList = new ArrayList<>();
        for (FlowableEvent event : listener.getEventsReceived()) {
            assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
            assertTrue("Boundary timer is the cause of the cancellation", ((FlowableActivityCancelledEvent) event).getCause() instanceof BoundaryEvent);
            assertTrue("Boundary timer is the cause of the cancellation", ( (BoundaryEvent) ((FlowableActivityCancelledEvent) event).getCause()).getEventDefinitions().get(0) instanceof TimerEventDefinition);
            eventIdList.add(((FlowableActivityEventImpl) event).getActivityId());
        }
        assertTrue(eventIdList.indexOf("innerTask1") >= 0);
        assertTrue(eventIdList.indexOf("innerTask2") >= 0);
        assertTrue(eventIdList.indexOf("subprocess") >= 0);
        assertTrue(eventIdList.indexOf("innerSubprocess") >= 0);
    }

    @Deployment
    public void testActivityTimeOutEventInCallActivity() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerOnCallActivity");
        Job theJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(theJob);

        // Force timer to fire
        Calendar timeToFire = Calendar.getInstance();
        timeToFire.add(Calendar.HOUR, 2);
        timeToFire.add(Calendar.MINUTE, 5);
        processEngineConfiguration.getClock().setCurrentTime(timeToFire.getTime());
        waitForJobExecutorToProcessAllJobs(5000, 500);

        // Check timeout-events have been dispatched
        assertEquals(4, listener.getEventsReceived().size());
        List<String> eventIdList = new ArrayList<>();
        for (FlowableEvent event : listener.getEventsReceived()) {
            assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
            assertTrue("Boundary timer is the cause of the cancellation", ((FlowableCancelledEvent)event).getCause() instanceof BoundaryEvent);
            BoundaryEvent boundaryEvent = (BoundaryEvent) ((FlowableCancelledEvent)event).getCause();
            assertTrue("Boundary timer is the cause of the cancellation", boundaryEvent.getEventDefinitions().get(0) instanceof TimerEventDefinition);
            eventIdList.add(((FlowableActivityEventImpl) event).getActivityId());
        }
        assertTrue(eventIdList.contains("innerTask1"));
        assertTrue(eventIdList.contains("innerTask2"));
        assertTrue(eventIdList.contains("callActivity"));
        assertTrue(eventIdList.contains("innerSubprocess"));
    }

    /**
     * Test events related to message events, called from the API.
     */
    @Deployment
    public void testActivityMessageBoundaryEventsOnUserTask() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("messageOnUserTaskProcess");
        assertNotNull(processInstance);

        Execution executionWithMessage = runtimeService.createExecutionQuery().messageEventSubscriptionName("message_1").singleResult();
        assertNotNull(executionWithMessage);

        runtimeService.messageEventReceived("message_1", executionWithMessage.getId());
        assertEquals(3, listener.getEventsReceived().size());

        // First, a message waiting event is expected
        assertTrue(listener.getEventsReceived().get(0) instanceof FlowableMessageEvent);
        FlowableMessageEvent messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING, messageEvent.getType());
        assertEquals("boundaryMessageEventCatching", messageEvent.getActivityId());
        assertEquals(executionWithMessage.getId(), messageEvent.getExecutionId());
        assertEquals(executionWithMessage.getProcessInstanceId(), messageEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), messageEvent.getProcessDefinitionId());
        assertEquals("message_1", messageEvent.getMessageName());
        assertNull(messageEvent.getMessageData());

        // Second, a message received event is expected
        assertTrue(listener.getEventsReceived().get(1) instanceof FlowableMessageEvent);
        messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ACTIVITY_MESSAGE_RECEIVED, messageEvent.getType());
        assertEquals("boundaryMessageEventCatching", messageEvent.getActivityId());
        assertEquals(executionWithMessage.getId(), messageEvent.getExecutionId());
        assertEquals(executionWithMessage.getProcessInstanceId(), messageEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), messageEvent.getProcessDefinitionId());
        assertEquals("message_1", messageEvent.getMessageName());
        assertNull(messageEvent.getMessageData());

        // Next, an signal-event is expected, as a result of the message
        assertTrue(listener.getEventsReceived().get(2) instanceof FlowableActivityCancelledEvent);
        FlowableActivityCancelledEvent signalEvent = (FlowableActivityCancelledEvent) listener.getEventsReceived().get(2);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, signalEvent.getType());
        assertEquals("cloudformtask1", signalEvent.getActivityId());
        assertEquals(executionWithMessage.getProcessInstanceId(), signalEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), signalEvent.getProcessDefinitionId());
        assertNotNull(signalEvent.getCause());
        assertTrue(signalEvent.getCause() instanceof BoundaryEvent);
        BoundaryEvent cause = (BoundaryEvent) signalEvent.getCause();
        assertEquals("message_1", ((MessageEventDefinition) cause.getEventDefinitions().get(0)).getMessageRef());

        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);
        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_MESSAGE_RECEIVED);
    }

    /**
     * Test events related to message events, called from the API.
     */
    @Deployment(resources = "org/flowable/engine/test/api/event/ActivityEventsTest.testActivityMessageBoundaryEventsOnUserTask.bpmn20.xml")
    public void testActivityMessageBoundaryEventsOnUserTaskForCancel() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("messageOnUserTaskProcess");
        assertNotNull(processInstance);

        Execution executionWithMessage = runtimeService.createExecutionQuery().messageEventSubscriptionName("message_1").singleResult();
        assertNotNull(executionWithMessage);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("User Task", task.getName());
        taskService.complete(task.getId());

        assertEquals(2, listener.getEventsReceived().size());

        // First, a message waiting event is expected
        assertTrue(listener.getEventsReceived().get(0) instanceof FlowableMessageEvent);
        FlowableMessageEvent messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING, messageEvent.getType());
        assertEquals("boundaryMessageEventCatching", messageEvent.getActivityId());
        assertEquals(executionWithMessage.getId(), messageEvent.getExecutionId());
        assertEquals(executionWithMessage.getProcessInstanceId(), messageEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), messageEvent.getProcessDefinitionId());
        assertEquals("message_1", messageEvent.getMessageName());
        assertNull(messageEvent.getMessageData());

        // Second, a message received event is expected
        assertTrue(listener.getEventsReceived().get(1) instanceof FlowableMessageEvent);
        messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ACTIVITY_MESSAGE_CANCELLED, messageEvent.getType());
        assertEquals("boundaryMessageEventCatching", messageEvent.getActivityId());
        assertEquals(executionWithMessage.getId(), messageEvent.getExecutionId());
        assertEquals(executionWithMessage.getProcessInstanceId(), messageEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), messageEvent.getProcessDefinitionId());
        assertEquals("message_1", messageEvent.getMessageName());
        assertNull(messageEvent.getMessageData());

        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);
    }

    /**
     * Test events related to message events, called from the API.
     */
    @Deployment
    public void testActivityMessageBoundaryEventsOnSubProcess() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("messageOnSubProcess");
        assertNotNull(processInstance);

        Execution executionWithMessage = runtimeService.createExecutionQuery().activityId("boundaryMessageEventCatching").singleResult();
        assertNotNull(executionWithMessage);

        runtimeService.messageEventReceived("message_1", executionWithMessage.getId());
        assertEquals(4, listener.getEventsReceived().size());

        // First, a message waiting event is expected
        assertTrue(listener.getEventsReceived().get(0) instanceof FlowableMessageEvent);
        FlowableMessageEvent messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING, messageEvent.getType());
        assertEquals("boundaryMessageEventCatching", messageEvent.getActivityId());
        assertEquals(executionWithMessage.getId(), messageEvent.getExecutionId());
        assertEquals(executionWithMessage.getProcessInstanceId(), messageEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), messageEvent.getProcessDefinitionId());
        assertEquals("message_1", messageEvent.getMessageName());
        assertNull(messageEvent.getMessageData());

        // Second, a message received event is expected
        assertTrue(listener.getEventsReceived().get(1) instanceof FlowableMessageEvent);
        messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ACTIVITY_MESSAGE_RECEIVED, messageEvent.getType());
        assertEquals("boundaryMessageEventCatching", messageEvent.getActivityId());
        assertEquals(executionWithMessage.getId(), messageEvent.getExecutionId());
        assertEquals(executionWithMessage.getProcessInstanceId(), messageEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), messageEvent.getProcessDefinitionId());
        assertEquals("message_1", messageEvent.getMessageName());
        assertNull(messageEvent.getMessageData());

        // Next, an signal-event is expected, as a result of the message

        assertTrue(listener.getEventsReceived().get(2) instanceof FlowableActivityCancelledEvent);
        FlowableActivityCancelledEvent cancelEvent = (FlowableActivityCancelledEvent) listener.getEventsReceived().get(2);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, cancelEvent.getType());
        assertEquals("cloudformtask1", cancelEvent.getActivityId());
        assertEquals(executionWithMessage.getProcessInstanceId(), cancelEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), cancelEvent.getProcessDefinitionId());
        assertNotNull(cancelEvent.getCause());
        assertTrue(cancelEvent.getCause() instanceof BoundaryEvent);
        BoundaryEvent cause = (BoundaryEvent) cancelEvent.getCause();
        assertEquals("message_1", ((MessageEventDefinition) cause.getEventDefinitions().get(0)).getMessageRef());
        
        assertTrue(listener.getEventsReceived().get(3) instanceof FlowableActivityCancelledEvent);
        cancelEvent = (FlowableActivityCancelledEvent) listener.getEventsReceived().get(3);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, cancelEvent.getType());
        assertEquals("subProcess", cancelEvent.getActivityId());
        assertEquals(executionWithMessage.getProcessInstanceId(), cancelEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), cancelEvent.getProcessDefinitionId());
        assertNotNull(cancelEvent.getCause());
        assertTrue(cancelEvent.getCause() instanceof BoundaryEvent);
        cause = (BoundaryEvent) cancelEvent.getCause();
        assertEquals("message_1", ((MessageEventDefinition) cause.getEventDefinitions().get(0)).getMessageRef());

        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);
        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_MESSAGE_RECEIVED);
    }

    /** 
     * Test events related to message events, called from the API.
     */
    @Deployment(resources = "org/flowable/engine/test/api/event/ActivityEventsTest.testActivityMessageBoundaryEventsOnSubProcess.bpmn20.xml")
    public void testActivityMessageBoundaryEventsOnSubProcessForCancel() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("messageOnSubProcess");
        assertNotNull(processInstance);

        Execution executionWithMessage = runtimeService.createExecutionQuery().activityId("boundaryMessageEventCatching").singleResult();
        assertNotNull(executionWithMessage);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        assertEquals(2, listener.getEventsReceived().size());

        // First, a message waiting event is expected
        assertTrue(listener.getEventsReceived().get(0) instanceof FlowableMessageEvent);
        FlowableMessageEvent messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING, messageEvent.getType());
        assertEquals("boundaryMessageEventCatching", messageEvent.getActivityId());
        assertEquals(executionWithMessage.getId(), messageEvent.getExecutionId());
        assertEquals(executionWithMessage.getProcessInstanceId(), messageEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), messageEvent.getProcessDefinitionId());
        assertEquals("message_1", messageEvent.getMessageName());
        assertNull(messageEvent.getMessageData());

        // Second, a message received event is expected
        assertTrue(listener.getEventsReceived().get(1) instanceof FlowableMessageEvent);
        messageEvent = (FlowableMessageEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ACTIVITY_MESSAGE_CANCELLED, messageEvent.getType());
        assertEquals("boundaryMessageEventCatching", messageEvent.getActivityId());
        assertEquals(executionWithMessage.getId(), messageEvent.getExecutionId());
        assertEquals(executionWithMessage.getProcessInstanceId(), messageEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), messageEvent.getProcessDefinitionId());
        assertEquals("message_1", messageEvent.getMessageName());
        assertNull(messageEvent.getMessageData());

        assertDatabaseEventPresent(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);
    }

    @Deployment
    public void testActivitySignalBoundaryEventsOnSubProcess() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("signalOnSubProcess");
        assertNotNull(processInstance);

        Execution executionWithSignal = runtimeService.createExecutionQuery().activityId("userTaskInsideProcess").singleResult();
        assertNotNull(executionWithSignal);

        runtimeService.signalEventReceived("signalName");
        assertEquals(4, listener.getEventsReceived().size());

        assertTrue(listener.getEventsReceived().get(0) instanceof FlowableSignalEventImpl);
        FlowableSignalEventImpl signalEvent = (FlowableSignalEventImpl) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING, signalEvent.getType());
        assertEquals("boundarySignalEventCatching", signalEvent.getActivityId());
        assertEquals(executionWithSignal.getProcessInstanceId(), signalEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), signalEvent.getProcessDefinitionId());

        assertTrue(listener.getEventsReceived().get(1) instanceof FlowableSignalEventImpl);
        signalEvent = (FlowableSignalEventImpl) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ACTIVITY_SIGNALED, signalEvent.getType());
        assertEquals("boundarySignalEventCatching", signalEvent.getActivityId());
        assertEquals(executionWithSignal.getProcessInstanceId(), signalEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), signalEvent.getProcessDefinitionId());

        assertTrue(listener.getEventsReceived().get(2) instanceof FlowableActivityCancelledEvent);
        FlowableActivityCancelledEvent cancelEvent = (FlowableActivityCancelledEvent) listener.getEventsReceived().get(2);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, cancelEvent.getType());
        assertEquals("userTaskInsideProcess", cancelEvent.getActivityId());
        assertEquals(executionWithSignal.getId(), cancelEvent.getExecutionId());
        assertEquals(executionWithSignal.getProcessInstanceId(), cancelEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), cancelEvent.getProcessDefinitionId());
        assertNotNull(cancelEvent.getCause());
        assertTrue(cancelEvent.getCause() instanceof BoundaryEvent);
        BoundaryEvent cause = (BoundaryEvent) cancelEvent.getCause();
        assertTrue(cause.getEventDefinitions().get(0) instanceof SignalEventDefinition);
        SignalEventDefinition signalEventDefinition = ((SignalEventDefinition) cause.getEventDefinitions().get(0));
        assertEquals("signal", signalEventDefinition.getSignalRef());
        assertEquals("signalName", repositoryService.getBpmnModel(cancelEvent.getProcessDefinitionId()).getSignal(signalEventDefinition.getSignalRef()).getName());
        
        assertTrue(listener.getEventsReceived().get(3) instanceof FlowableActivityCancelledEvent);
        cancelEvent = (FlowableActivityCancelledEvent) listener.getEventsReceived().get(3);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, cancelEvent.getType());
        assertEquals("subProcess", cancelEvent.getActivityId());
        assertEquals(executionWithSignal.getProcessInstanceId(), cancelEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), cancelEvent.getProcessDefinitionId());
        assertNotNull(cancelEvent.getCause());
        assertTrue(cancelEvent.getCause() instanceof BoundaryEvent);
        cause = (BoundaryEvent) cancelEvent.getCause();
        assertTrue(cause.getEventDefinitions().get(0) instanceof SignalEventDefinition);
        signalEventDefinition = ((SignalEventDefinition) cause.getEventDefinitions().get(0));
        assertEquals("signal", signalEventDefinition.getSignalRef());
        assertEquals("signalName", repositoryService.getBpmnModel(cancelEvent.getProcessDefinitionId()).getSignal(signalEventDefinition.getSignalRef()).getName());
    }

    @Deployment
    public void testActivitySignalBoundaryEventsOnUserTask() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("signalOnUserTask");
        assertNotNull(processInstance);

        Execution executionWithSignal = runtimeService.createExecutionQuery().activityId("userTask").singleResult();
        assertNotNull(executionWithSignal);

        runtimeService.signalEventReceived("signalName");

        // Next, an signal-event is expected, as a result of the message
        assertEquals(3, listener.getEventsReceived().size());

        assertTrue(listener.getEventsReceived().get(0) instanceof FlowableSignalEventImpl);
        FlowableSignalEventImpl signalEvent = (FlowableSignalEventImpl) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING, signalEvent.getType());
        assertEquals("boundarySignalEventCatching", signalEvent.getActivityId());
        assertEquals(executionWithSignal.getProcessInstanceId(), signalEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), signalEvent.getProcessDefinitionId());

        assertTrue(listener.getEventsReceived().get(1) instanceof FlowableSignalEventImpl);
        signalEvent = (FlowableSignalEventImpl) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ACTIVITY_SIGNALED, signalEvent.getType());
        assertEquals("boundarySignalEventCatching", signalEvent.getActivityId());
        assertEquals(executionWithSignal.getProcessInstanceId(), signalEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), signalEvent.getProcessDefinitionId());

        assertTrue(listener.getEventsReceived().get(2) instanceof FlowableActivityCancelledEvent);
        FlowableActivityCancelledEvent cancelEvent = (FlowableActivityCancelledEvent) listener.getEventsReceived().get(2);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, cancelEvent.getType());
        assertEquals("userTask", cancelEvent.getActivityId());
        assertEquals(executionWithSignal.getProcessInstanceId(), cancelEvent.getProcessInstanceId());
        assertEquals(processInstance.getProcessDefinitionId(), cancelEvent.getProcessDefinitionId());
        assertNotNull(cancelEvent.getCause());
        assertTrue(cancelEvent.getCause() instanceof BoundaryEvent);
        BoundaryEvent cause = (BoundaryEvent) cancelEvent.getCause();
        assertTrue(cause.getEventDefinitions().get(0) instanceof SignalEventDefinition);
        SignalEventDefinition signalEventDefinition = ((SignalEventDefinition) cause.getEventDefinitions().get(0));
        assertEquals("signal", signalEventDefinition.getSignalRef());
        assertEquals("signalName", repositoryService.getBpmnModel(cancelEvent.getProcessDefinitionId()).getSignal(signalEventDefinition.getSignalRef()).getName());
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
        assertTrue(found);
    }

}
