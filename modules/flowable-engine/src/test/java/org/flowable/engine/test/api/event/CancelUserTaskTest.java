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
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CancelUserTaskTest extends PluggableFlowableTestCase {

    private UserActivityEventListener testListener;

    @AfterEach
    public void tearDown() throws Exception {

        if (testListener != null) {
            testListener.clearEventsReceived();
            processEngineConfiguration.getEventDispatcher().removeEventListener(testListener);
        }
    }

    @BeforeEach
    protected void setUp() {
        testListener = new UserActivityEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(testListener);
    }

    /**
     * User task cancelled by terminate end event.
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/event/CancelUserTaskEventsTest.bpmn20.xml" })
    public void testUserTaskCancelledWhenFlowToTerminateEnd() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("cancelUserTaskEvents");
        assertThat(processInstance).isNotNull();

        Execution task1Execution = runtimeService.createExecutionQuery().activityId("task1").singleResult();
        Execution task2Execution = runtimeService.createExecutionQuery().activityId("task2").singleResult();
        Execution boundaryExecution = runtimeService.createExecutionQuery().activityId("cancelBoundaryEvent1").singleResult();

        int idx = 0;
        FlowableEvent flowableEvent = testListener.getEventsReceived().get(idx++);
        assertThat(flowableEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_STARTED);
        ExecutionEntity executionEntity = (ExecutionEntity) ((FlowableProcessStartedEvent) flowableEvent).getEntity();
        assertThat(executionEntity.getProcessInstanceId()).isEqualTo(processInstance.getId());

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task1");

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("User Task1");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task2");

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("User Task2");

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        Task userTask1 = null;
        for (Task task : tasks) {
            if ("User Task1".equals(task.getName())) {
                userTask1 = task;
                break;
            }
        }
        assertThat(userTask1).isNotNull();

        // complete task1 so we flow to terminate end
        taskService.complete(userTask1.getId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_COMPLETED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("User Task1");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task1");
        assertThat(activityEvent.getExecutionId()).isEqualTo(task1Execution.getId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityType()).isEqualTo("endEvent");
        assertThat(activityEvent.getActivityId()).isEqualTo("endEvent1");

        for (int i = 0; i < 2; i++) {
            activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
            assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
            FlowableActivityCancelledEvent cancelledEvent = (FlowableActivityCancelledEvent) activityEvent;

            if ("task2".equals(cancelledEvent.getActivityId())) {
                assertThat(cancelledEvent.getActivityId()).isEqualTo("task2");
                assertThat(cancelledEvent.getActivityType()).isEqualTo("userTask");
                assertThat(cancelledEvent.getActivityName()).isEqualTo("User Task2");
                assertThat(cancelledEvent.getExecutionId()).isEqualTo(task2Execution.getId());

            } else if ("cancelBoundaryEvent1".equals(cancelledEvent.getActivityId())) {
                assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
                cancelledEvent = (FlowableActivityCancelledEvent) activityEvent;
                assertThat(cancelledEvent.getActivityId()).isEqualTo("cancelBoundaryEvent1");
                assertThat(cancelledEvent.getExecutionId()).isEqualTo(boundaryExecution.getId());
            }
        }

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);

        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT);

        assertThat(idx).isEqualTo(13);
        assertThat(testListener.getEventsReceived()).hasSize(13);
    }

    /**
     * User task cancelled by message boundary event.
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/event/CancelUserTaskEventsTest.bpmn20.xml" })
    public void testUserTaskCancelledByMessageBoundaryEvent() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("cancelUserTaskEvents");
        assertThat(processInstance).isNotNull();

        Execution task1Execution = runtimeService.createExecutionQuery().activityId("task1").singleResult();
        Execution task2Execution = runtimeService.createExecutionQuery().activityId("task2").singleResult();
        Execution boundaryExecution = runtimeService.createExecutionQuery().activityId("cancelBoundaryEvent1").singleResult();

        int idx = 0;
        FlowableEvent flowableEvent = testListener.getEventsReceived().get(idx++);
        assertThat(flowableEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_STARTED);
        ExecutionEntity executionEntity = (ExecutionEntity) ((FlowableProcessStartedEvent) flowableEvent).getEntity();
        assertThat(executionEntity.getProcessInstanceId()).isEqualTo(processInstance.getId());

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task1");

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("User Task1");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task2");

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("User Task2");

        Execution cancelMessageExecution = runtimeService.createExecutionQuery().messageEventSubscriptionName("cancel").singleResult();
        assertThat(cancelMessageExecution).isNotNull();
        assertThat(cancelMessageExecution.getActivityId()).isEqualTo("cancelBoundaryEvent1");

        // cancel the user task (task2)
        runtimeService.messageEventReceived("cancel", cancelMessageExecution.getId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        FlowableActivityCancelledEvent cancelledEvent = (FlowableActivityCancelledEvent) activityEvent;
        assertThat(cancelledEvent.getActivityId()).isEqualTo("task2");
        assertThat(cancelledEvent.getActivityType()).isEqualTo("userTask");
        assertThat(cancelledEvent.getActivityName()).isEqualTo("User Task2");
        assertThat(cancelledEvent.getExecutionId()).isEqualTo(task2Execution.getId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("cancelBoundaryEvent1");
        assertThat(activityEvent.getActivityType()).isEqualTo("boundaryEvent");
        assertThat(activityEvent.getExecutionId()).isEqualTo(boundaryExecution.getId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("endEvent1");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task1");
        assertThat(activityEvent.getExecutionId()).isEqualTo(task1Execution.getId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT);

        assertThat(idx).isEqualTo(12);
        assertThat(testListener.getEventsReceived()).hasSize(12);
    }

    class UserActivityEventListener extends AbstractFlowableEngineEventListener {

        private List<FlowableEvent> eventsReceived;

        public UserActivityEventListener() {
            super(new HashSet<>(Arrays.asList(
                    FlowableEngineEventType.ACTIVITY_STARTED,
                    FlowableEngineEventType.ACTIVITY_COMPLETED,
                    FlowableEngineEventType.ACTIVITY_CANCELLED,
                    FlowableEngineEventType.TASK_CREATED,
                    FlowableEngineEventType.TASK_COMPLETED,
                    FlowableEngineEventType.PROCESS_STARTED,
                    FlowableEngineEventType.PROCESS_COMPLETED,
                    FlowableEngineEventType.PROCESS_CANCELLED,
                    FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT
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
        protected void activityStarted(FlowableActivityEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void activityCompleted(FlowableActivityEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void activityCancelled(FlowableActivityCancelledEvent event) {
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
        protected void processCompletedWithTerminateEnd(FlowableEngineEntityEvent event) {
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
