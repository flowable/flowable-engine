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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.delegate.event.FlowableActivityCancelledEvent;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.flowable.engine.delegate.event.FlowableCancelledEvent;
import org.flowable.engine.delegate.event.FlowableProcessStartedEvent;
import org.flowable.engine.event.EventLogEntry;
import org.flowable.engine.impl.event.logger.EventLogger;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

public class CancelCallActivityTest extends PluggableFlowableTestCase {

    private CallActivityEventListener listener;

    protected EventLogger databaseEventLogger;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Database event logger setup
        databaseEventLogger = new EventLogger(processEngineConfiguration.getClock(),
                processEngineConfiguration.getObjectMapper());
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

        listener = new CallActivityEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @Deployment(resources = {
            "org/flowable/engine/test/api/event/CancelCallActivityTest.testActivityMessageBoundaryEventsOnCallActivity.bpmn20.xml",
            "org/flowable/engine/test/api/event/CancelCallActivityTest.testActivityMessageBoundaryEventsCalledActivity.bpmn20.xml" })
    public void testCancelCallActivity() throws Exception {

        CallActivityEventListener mylistener = new CallActivityEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(mylistener);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("messageOnCallActivity");
        assertNotNull(processInstance);

        Execution executionWithMessage = runtimeService.createExecutionQuery().activityId("cancelBoundaryEvent").singleResult();
        assertNotNull(executionWithMessage);

        runtimeService.messageEventReceived("cancel", executionWithMessage.getId());

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, entityEvent.getType());
        ExecutionEntity executionEntity = (ExecutionEntity) entityEvent.getEntity();

        // this is the root process so parent null
        assertNull(executionEntity.getParentId());
        String processExecutionId = executionEntity.getId();

        // this is callActivity
        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, entityEvent.getType());
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertNotNull(executionEntity.getParentId());
        assertEquals(processExecutionId, executionEntity.getParentId());

        FlowableEvent activitiEvent = mylistener.getEventsReceived().get(2);
        assertEquals(FlowableEngineEventType.PROCESS_STARTED, activitiEvent.getType());

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(3);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(4);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(5);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, entityEvent.getType());
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertEquals("cancelBoundaryEvent", executionEntity.getActivityId());

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(6);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("callActivity1", activityEvent.getActivityId());

        // this is external subprocess. Workflow uses the ENTITY_CREATED event to determine when to send our event.
        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(7);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, entityEvent.getType());
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertNull(executionEntity.getParentId());
        assertEquals(executionEntity.getId(), executionEntity.getProcessInstanceId());

        // this is the task within the external subprocess
        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(8);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, entityEvent.getType());
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertEquals("calledtask1", executionEntity.getActivityId());

        // start event in external subprocess
        activitiEvent = mylistener.getEventsReceived().get(9);
        assertEquals(FlowableEngineEventType.PROCESS_STARTED, activitiEvent.getType());

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(10);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());
        assertEquals("startevent2", activityEvent.getActivityId());

        // start event in external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(11);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());
        assertEquals("startevent2", activityEvent.getActivityId());

        // this is user task within external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(12);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("calledtask1", activityEvent.getActivityId());
        assertEquals("userTask", activityEvent.getActivityType());

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(13);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("User Task2 in External", taskEntity.getName());

        FlowableActivityCancelledEvent taskCancelledEvent = (FlowableActivityCancelledEvent) mylistener.getEventsReceived().get(14);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, taskCancelledEvent.getType());
        assertEquals(taskEntity.getName(), taskCancelledEvent.getActivityName());
        assertEquals("userTask", taskCancelledEvent.getActivityType());

        FlowableCancelledEvent processCancelledEvent = (FlowableCancelledEvent) mylistener.getEventsReceived().get(15);
        assertEquals(FlowableEngineEventType.PROCESS_CANCELLED, processCancelledEvent.getType());
        assertEquals(processCancelledEvent.getProcessInstanceId(), processCancelledEvent.getExecutionId());
        
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(16);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
        assertEquals("callActivity", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(17);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("boundaryEvent", activityEvent.getActivityType());
        assertEquals("cancelBoundaryEvent", activityEvent.getActivityId());
        assertEquals(executionWithMessage.getId(), activityEvent.getExecutionId());

        // task in the main definition
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(18);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("task1", activityEvent.getActivityId());
        assertEquals("userTask", activityEvent.getActivityType());

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(19);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("User Task1", taskEntity.getName());

        assertEquals(20, mylistener.getEventsReceived().size());
    }

    class CallActivityEventListener extends AbstractFlowableEngineEventListener {

        private List<FlowableEvent> eventsReceived;

        public CallActivityEventListener() {
            super(new HashSet<>(Arrays.asList(
                    FlowableEngineEventType.ENTITY_CREATED,
                    FlowableEngineEventType.ACTIVITY_STARTED,
                    FlowableEngineEventType.ACTIVITY_COMPLETED,
                    FlowableEngineEventType.ACTIVITY_CANCELLED,
                    FlowableEngineEventType.TASK_CREATED,
                    FlowableEngineEventType.TASK_COMPLETED,
                    FlowableEngineEventType.PROCESS_STARTED,
                    FlowableEngineEventType.PROCESS_COMPLETED,
                    FlowableEngineEventType.PROCESS_CANCELLED
            )));
            eventsReceived = new ArrayList<>();
        }

        public List<FlowableEvent> getEventsReceived() {
            return eventsReceived;
        }

        public void clearEventsReceived() {
            eventsReceived.clear();
        }

        @Override
        protected void entityCreated(FlowableEngineEntityEvent event) {
            if (event.getEntity() instanceof ExecutionEntity) {
                eventsReceived.add(event);
            }
        }

        @Override
        protected void activityStarted(FlowableActivityEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void activityCancelled(FlowableActivityCancelledEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void activityCompleted(FlowableActivityEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void taskCreated(FlowableEngineEntityEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void taskCompleted(FlowableEngineEntityEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void processStarted(FlowableProcessStartedEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void processCompleted(FlowableEngineEntityEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void processCancelled(FlowableCancelledEvent event) {
            eventsReceived.add(event);
        }

        @Override
        public boolean isFailOnException() {
            return false;
        }
    }

}
