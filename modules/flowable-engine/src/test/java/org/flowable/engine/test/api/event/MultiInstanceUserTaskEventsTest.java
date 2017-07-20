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
import java.util.List;

import org.flowable.engine.common.api.delegate.event.FlowableEntityEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEventListener;
import org.flowable.engine.delegate.event.FlowableActivityCancelledEvent;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.FlowableProcessStartedEvent;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.TaskEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Task;
import org.flowable.engine.test.Deployment;

public class MultiInstanceUserTaskEventsTest extends PluggableFlowableTestCase {

    private MultiInstanceUserActivityEventListener testListener;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {

        if (testListener != null) {
            testListener.clearEventsReceived();
            processEngineConfiguration.getEventDispatcher().removeEventListener(testListener);
        }

        super.tearDown();
    }

    @Override
    protected void initializeServices() {
        super.initializeServices();
        testListener = new MultiInstanceUserActivityEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(testListener);
    }

    /**
     * Multi-instance user task cancelled by terminate end event.
     */
    @Deployment(resources = { "org/flowable/engine/test/api/event/MultiInstanceUserTaskEventsTest.bpmn20.xml" })
    public void testMultiInstanceCancelledWhenFlowToTerminateEnd() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("multiInstanceUserTaskEvents");
        assertNotNull(processInstance);

        int idx = 0;
        FlowableEvent activitiEvent = testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_STARTED, activitiEvent.getType());
        ExecutionEntity executionEntity = (ExecutionEntity) ((FlowableProcessStartedEvent) activitiEvent).getEntity();
        assertEquals(processInstance.getId(), executionEntity.getProcessInstanceId());

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("task1", activityEvent.getActivityId());

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("User Task1", taskEntity.getName());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("task2", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("task2", activityEvent.getActivityId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("Multi User Task-0", taskEntity.getName());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("task2", activityEvent.getActivityId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("Multi User Task-1", taskEntity.getName());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());
        Task userTask1 = null;
        for (Task task : tasks) {
            if ("User Task1".equals(task.getName())) {
                userTask1 = task;
                break;
            }
        }
        assertNotNull(userTask1);

        // complete task1 so we flow to terminate end
        taskService.complete(userTask1.getId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_COMPLETED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("User Task1", taskEntity.getName());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("task1", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("endEvent", activityEvent.getActivityType());
        assertEquals("endEvent1", activityEvent.getActivityId());

        // cancelled event for one of the multi-instance user task instances
        for (int i=0; i<4; i++) {
            activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
            assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
            FlowableActivityCancelledEvent cancelledEvent = (FlowableActivityCancelledEvent) activityEvent;
            
            if ("task2".equals(cancelledEvent.getActivityId())) {
                assertEquals("task2", cancelledEvent.getActivityId());
                assertEquals("userTask", cancelledEvent.getActivityType());
                assertEquals("Multi User Task-${loopCounter}", cancelledEvent.getActivityName());
                
            } else if ("cancelBoundaryEvent1".equals(cancelledEvent.getActivityId())) {
                assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
                cancelledEvent = (FlowableActivityCancelledEvent) activityEvent;
                assertEquals("cancelBoundaryEvent1", cancelledEvent.getActivityId());
                
            } else {
                // cancelled event for the root of the multi-instance user task
                activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
                assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
                cancelledEvent = (FlowableActivityCancelledEvent) activityEvent;
                assertEquals("task2", cancelledEvent.getActivityId());
                assertEquals("userTask", cancelledEvent.getActivityType());
                assertEquals("Multi User Task-${loopCounter}", cancelledEvent.getActivityName());
                
            }
        }

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);

        assertEquals(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT, entityEvent.getType());

        assertEquals(18, idx);
        assertEquals(18, testListener.getEventsReceived().size());
    }

    /**
     * Multi-instance user task cancelled by message boundary event defined on
     * multi-instance user task.
     */
    @Deployment(resources = { "org/flowable/engine/test/api/event/MultiInstanceUserTaskEventsTest.bpmn20.xml" })
    public void testMultiInstanceCancelledByMessageBoundaryEvent() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("multiInstanceUserTaskEvents");
        assertNotNull(processInstance);

        int idx = 0;
        FlowableEvent activitiEvent = testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_STARTED, activitiEvent.getType());
        ExecutionEntity executionEntity = (ExecutionEntity) ((FlowableProcessStartedEvent) activitiEvent).getEntity();
        assertEquals(processInstance.getId(), executionEntity.getProcessInstanceId());

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("task1", activityEvent.getActivityId());

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("User Task1", taskEntity.getName());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("task2", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("task2", activityEvent.getActivityId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("Multi User Task-0", taskEntity.getName());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("task2", activityEvent.getActivityId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("Multi User Task-1", taskEntity.getName());

        Execution cancelMessageExecution = runtimeService.createExecutionQuery().messageEventSubscriptionName("cancel")
                .singleResult();
        assertNotNull(cancelMessageExecution);
        assertEquals("cancelBoundaryEvent1", cancelMessageExecution.getActivityId());

        // cancel the multi-instance user task
        runtimeService.messageEventReceived("cancel", cancelMessageExecution.getId());

        // cancelled event for one of the multi-instance user task instances
        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
        FlowableActivityCancelledEvent cancelledEvent = (FlowableActivityCancelledEvent) activityEvent;
        assertEquals("task2", cancelledEvent.getActivityId());
        assertEquals("userTask", cancelledEvent.getActivityType());
        assertEquals("Multi User Task-${loopCounter}", cancelledEvent.getActivityName());

        // cancelled event for one of the multi-instance user task instances
        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
        assertEquals("task2", activityEvent.getActivityId());

        // cancelled event for the root of the multi-instance user task
        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
        assertEquals("task2", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("cancelBoundaryEvent1", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("endEvent1", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
        assertEquals("task1", activityEvent.getActivityId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT, entityEvent.getType());

        assertEquals(17, idx);
        assertEquals(17, testListener.getEventsReceived().size());
    }

    class MultiInstanceUserActivityEventListener implements FlowableEventListener {

        private List<FlowableEvent> eventsReceived;

        public MultiInstanceUserActivityEventListener() {
            eventsReceived = new ArrayList<FlowableEvent>();
        }

        public List<FlowableEvent> getEventsReceived() {
            return eventsReceived;
        }

        public void clearEventsReceived() {
            eventsReceived.clear();
        }

        @Override
        public void onEvent(FlowableEvent event) {

            FlowableEngineEventType engineEventType = (FlowableEngineEventType) event.getType();
            switch (engineEventType) {
            case ACTIVITY_STARTED:
            case ACTIVITY_COMPLETED:
            case ACTIVITY_CANCELLED:
            case TASK_CREATED:
            case TASK_COMPLETED:
            case PROCESS_STARTED:
            case PROCESS_COMPLETED:
            case PROCESS_CANCELLED:
            case PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT:
                eventsReceived.add(event);
                break;
            default:
                break;
            }
        }

        @Override
        public boolean isFailOnException() {
            return false;
        }
    }
}
