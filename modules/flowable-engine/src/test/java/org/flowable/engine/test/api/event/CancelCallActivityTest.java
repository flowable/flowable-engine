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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CancelCallActivityTest extends PluggableFlowableTestCase {

    private CallActivityEventListener listener;

    protected EventLogger databaseEventLogger;

    @BeforeEach
    protected void setUp() throws Exception {
        listener = new CallActivityEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);

        // Database event logger setup
        databaseEventLogger = new EventLogger(processEngineConfiguration.getClock(),
                processEngineConfiguration.getObjectMapper());
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

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/event/CancelCallActivityTest.testActivityMessageBoundaryEventsOnCallActivity.bpmn20.xml",
            "org/flowable/engine/test/api/event/CancelCallActivityTest.testActivityMessageBoundaryEventsCalledActivity.bpmn20.xml" })
    public void testCancelCallActivity() throws Exception {

        CallActivityEventListener mylistener = new CallActivityEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(mylistener);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("messageOnCallActivity");
        assertThat(processInstance).isNotNull();

        Execution executionWithMessage = runtimeService.createExecutionQuery().activityId("cancelBoundaryEvent").singleResult();
        assertThat(executionWithMessage).isNotNull();

        runtimeService.messageEventReceived("cancel", executionWithMessage.getId());

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(0);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        ExecutionEntity executionEntity = (ExecutionEntity) entityEvent.getEntity();

        // this is the root process so parent null
        assertThat(executionEntity.getParentId()).isNull();
        String processExecutionId = executionEntity.getId();

        // this is callActivity
        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(1);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertThat(executionEntity.getParentId()).isEqualTo(processExecutionId);

        FlowableEvent activitiEvent = mylistener.getEventsReceived().get(2);
        assertThat(activitiEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_STARTED);

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(3);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(4);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(5);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertThat(executionEntity.getActivityId()).isEqualTo("cancelBoundaryEvent");

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(6);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("callActivity1");

        // this is external subprocess. Workflow uses the ENTITY_CREATED event to determine when to send our event.
        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(7);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertThat(executionEntity.getParentId()).isNull();
        assertThat(executionEntity.getProcessInstanceId()).isEqualTo(executionEntity.getId());

        // this is the task within the external subprocess
        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(8);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertThat(executionEntity.getActivityId()).isEqualTo("calledtask1");

        // start event in external subprocess
        activitiEvent = mylistener.getEventsReceived().get(9);
        assertThat(activitiEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_STARTED);

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(10);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");
        assertThat(activityEvent.getActivityId()).isEqualTo("startevent2");

        // start event in external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(11);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");
        assertThat(activityEvent.getActivityId()).isEqualTo("startevent2");

        // this is user task within external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(12);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("calledtask1");
        assertThat(activityEvent.getActivityType()).isEqualTo("userTask");

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(13);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("User Task2 in External");

        FlowableActivityCancelledEvent taskCancelledEvent = (FlowableActivityCancelledEvent) mylistener.getEventsReceived().get(14);
        assertThat(taskCancelledEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(taskCancelledEvent.getActivityName()).isEqualTo(taskEntity.getName());
        assertThat(taskCancelledEvent.getActivityType()).isEqualTo("userTask");

        FlowableCancelledEvent processCancelledEvent = (FlowableCancelledEvent) mylistener.getEventsReceived().get(15);
        assertThat(processCancelledEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_CANCELLED);
        assertThat(processCancelledEvent.getExecutionId()).isEqualTo(processCancelledEvent.getProcessInstanceId());

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(16);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(activityEvent.getActivityType()).isEqualTo("callActivity");

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(17);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityType()).isEqualTo("boundaryEvent");
        assertThat(activityEvent.getActivityId()).isEqualTo("cancelBoundaryEvent");
        assertThat(activityEvent.getExecutionId()).isEqualTo(executionWithMessage.getId());

        // task in the main definition
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(18);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task1");
        assertThat(activityEvent.getActivityType()).isEqualTo("userTask");

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(19);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("User Task1");

        assertThat(mylistener.getEventsReceived()).hasSize(20);
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
