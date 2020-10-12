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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.delegate.event.FlowableActivityCancelledEvent;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.flowable.engine.delegate.event.FlowableCancelledEvent;
import org.flowable.engine.delegate.event.FlowableMultiInstanceActivityCancelledEvent;
import org.flowable.engine.delegate.event.FlowableMultiInstanceActivityCompletedEvent;
import org.flowable.engine.delegate.event.FlowableMultiInstanceActivityEvent;
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

public class MultiInstanceUserTaskEventsTest extends PluggableFlowableTestCase {

    private MultiInstanceUserActivityEventListener testListener;

    @AfterEach
    protected void tearDown() throws Exception {

        if (testListener != null) {
            testListener.clearEventsReceived();
            processEngineConfiguration.getEventDispatcher().removeEventListener(testListener);
        }

    }

    @BeforeEach
    protected void setUp() {
        testListener = new MultiInstanceUserActivityEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(testListener);
    }

    /**
     * Multi-instance user task cancelled by terminate end event.
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/event/MultiInstanceUserTaskEventsTest.bpmn20.xml" })
    public void testMultiInstanceCancelledWhenFlowToTerminateEnd() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("multiInstanceUserTaskEvents");
        assertThat(processInstance).isNotNull();

        Execution task1Execution = runtimeService.createExecutionQuery().activityId("task1").singleResult();
        Execution boundaryExecution = runtimeService.createExecutionQuery().activityId("cancelBoundaryEvent1").singleResult();
        List<Execution> multiExecutions = runtimeService.createExecutionQuery().activityId("task2").list();
        assertThat(multiExecutions).hasSize(2);
        String multiExecutionId1 = multiExecutions.get(0).getId();
        String multiExecutionId2 = multiExecutions.get(1).getId();
        String rootMultiExecutionId = null;

        List<Execution> executions = runtimeService.createExecutionQuery().parentId(processInstance.getId()).list();
        for (Execution execution : executions) {
            if (((ExecutionEntity) execution).isMultiInstanceRoot()) {
                rootMultiExecutionId = execution.getId();
                break;
            }
        }
        assertThat(rootMultiExecutionId).isNotNull();
        assertThat(multiExecutionId1).isNotSameAs(rootMultiExecutionId);
        assertThat(multiExecutionId2).isNotSameAs(rootMultiExecutionId);
        assertThat(boundaryExecution).isNotSameAs(rootMultiExecutionId);

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
        assertThat(activityEvent.getExecutionId()).isEqualTo(task1Execution.getId());

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("User Task1");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task2");
        assertThat(activityEvent.getExecutionId()).isEqualTo(rootMultiExecutionId);

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task2");

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("Multi User Task-0");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task2");

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("Multi User Task-1");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(3);
        org.flowable.task.api.Task userTask1 = null;
        for (org.flowable.task.api.Task task : tasks) {
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

        boolean foundMultiExec1 = false;
        boolean foundMultiExec2 = false;
        boolean foundRootExec = false;
        boolean foundBoundaryExec = false;

        // cancelled event for multi-instance user task instances and boundary event
        for (int i = 0; i < 4; i++) {
            activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
            FlowableActivityCancelledEvent cancelledEvent = (FlowableActivityCancelledEvent) activityEvent;

            if ("task2".equals(cancelledEvent.getActivityId())) {
                assertThat(cancelledEvent.getActivityId()).isEqualTo("task2");
                assertThat(cancelledEvent.getActivityType()).isEqualTo("userTask");
                assertThat(cancelledEvent.getActivityName()).isEqualTo("Multi User Task-${loopCounter}");
                String eventExecutionId = activityEvent.getExecutionId();
                if (multiExecutionId1.equals(eventExecutionId)) {
                    assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
                    foundMultiExec1 = true;
                } else if (multiExecutionId2.equals(eventExecutionId)) {
                    assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
                    foundMultiExec2 = true;
                } else if (rootMultiExecutionId.equals(eventExecutionId)) {
                    assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_CANCELLED);
                    foundRootExec = true;
                }
            } else if ("cancelBoundaryEvent1".equals(cancelledEvent.getActivityId())) {
                assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
                cancelledEvent = (FlowableActivityCancelledEvent) activityEvent;
                assertThat(cancelledEvent.getActivityId()).isEqualTo("cancelBoundaryEvent1");
                assertThat(activityEvent.getExecutionId()).isEqualTo(boundaryExecution.getId());
                foundBoundaryExec = true;
            }
        }

        assertThat(foundMultiExec1).isTrue();
        assertThat(foundMultiExec2).isTrue();
        assertThat(foundRootExec).isTrue();
        assertThat(foundBoundaryExec).isTrue();

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);

        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT);

        assertThat(idx).isEqualTo(18);
        assertThat(testListener.getEventsReceived()).hasSize(18);
    }

    /**
     * Multi-instance user task cancelled by terminate end event.
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/event/MultiInstanceUserTaskEventsTest.testMultiInstanceCompleteCondition.bpmn20.xml" })
    public void testMultiInstanceCompleteCondition() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("percentageCompleted", .5f);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("multiInstanceUserTaskEvents", variables);
        assertThat(processInstance).isNotNull();

        //Execution task1Execution = runtimeService.createExecutionQuery().activityId("task1").singleResult();

        List<Execution> multiExecutions = runtimeService.createExecutionQuery().activityId("task").list();
        assertThat(multiExecutions).hasSize(2);
        String multiExecutionId1 = multiExecutions.get(0).getId();
        String multiExecutionId2 = multiExecutions.get(1).getId();
        String rootMultiExecutionId = null;

        List<Execution> executions = runtimeService.createExecutionQuery().parentId(processInstance.getId()).list();
        for (Execution execution : executions) {
            if (((ExecutionEntity) execution).isMultiInstanceRoot()) {
                rootMultiExecutionId = execution.getId();
                break;
            }
        }
        assertThat(rootMultiExecutionId).isNotNull();
        assertThat(multiExecutionId1).isNotSameAs(rootMultiExecutionId);
        assertThat(multiExecutionId2).isNotSameAs(rootMultiExecutionId);

        int idx = 0;
        FlowableEvent flowableEvent = testListener.getEventsReceived().get(idx++);
        assertThat(flowableEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_STARTED);
        ExecutionEntity executionEntity = (ExecutionEntity) ((FlowableProcessStartedEvent) flowableEvent).getEntity();
        assertThat(executionEntity.getProcessInstanceId()).isEqualTo(processInstance.getId());

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityName()).isEqualTo("start");
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityName()).isEqualTo("start");
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityName()).isEqualTo("Multi User Task-${loopCounter}");
        assertThat(activityEvent.getActivityId()).isEqualTo("task");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityName()).isEqualTo("Multi User Task-${loopCounter}");
        assertThat(activityEvent.getActivityId()).isEqualTo("task");

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("Multi User Task-0");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task");

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("Multi User Task-1");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        org.flowable.task.api.Task task0 = tasks.get(0);

        taskService.complete(task0.getId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_COMPLETED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getId()).isEqualTo(task0.getId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_COMPLETED_WITH_CONDITION);

        assertThat(activityEvent.getActivityId()).isEqualTo("task");
        assertThat(((FlowableMultiInstanceActivityCompletedEvent) activityEvent).getNumberOfInstances()).isEqualTo(2);
        assertThat(((FlowableMultiInstanceActivityCompletedEvent) activityEvent).getNumberOfActiveInstances()).isEqualTo(1);
        assertThat(((FlowableMultiInstanceActivityCompletedEvent) activityEvent).getNumberOfCompletedInstances()).isEqualTo(1);
        assertThat(((FlowableMultiInstanceActivityCompletedEvent) activityEvent).isSequential()).isFalse();

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityType()).isEqualTo("endEvent");
        assertThat(activityEvent.getActivityId()).isEqualTo("endEvent1");

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT);
    }

    /**
     * Multi-instance user task cancelled by terminate end event.
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/event/MultiInstanceUserTaskEventsTest.testMultiInstanceCompleteCondition.bpmn20.xml" })
    public void testMultiInstanceComplete() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("percentageCompleted", 2f);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("multiInstanceUserTaskEvents", variables);
        assertThat(processInstance).isNotNull();

        //Execution task1Execution = runtimeService.createExecutionQuery().activityId("task1").singleResult();

        List<Execution> multiExecutions = runtimeService.createExecutionQuery().activityId("task").list();
        assertThat(multiExecutions).hasSize(2);
        String multiExecutionId1 = multiExecutions.get(0).getId();
        String multiExecutionId2 = multiExecutions.get(1).getId();
        String rootMultiExecutionId = null;

        List<Execution> executions = runtimeService.createExecutionQuery().parentId(processInstance.getId()).list();
        for (Execution execution : executions) {
            if (((ExecutionEntity) execution).isMultiInstanceRoot()) {
                rootMultiExecutionId = execution.getId();
                break;
            }
        }
        assertThat(rootMultiExecutionId).isNotNull();
        assertThat(multiExecutionId1).isNotSameAs(rootMultiExecutionId);
        assertThat(multiExecutionId2).isNotSameAs(rootMultiExecutionId);

        int idx = 0;
        FlowableEvent flowableEvent = testListener.getEventsReceived().get(idx++);
        assertThat(flowableEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_STARTED);
        ExecutionEntity executionEntity = (ExecutionEntity) ((FlowableProcessStartedEvent) flowableEvent).getEntity();
        assertThat(executionEntity.getProcessInstanceId()).isEqualTo(processInstance.getId());

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityName()).isEqualTo("start");
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityName()).isEqualTo("start");
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityName()).isEqualTo("Multi User Task-${loopCounter}");
        assertThat(activityEvent.getActivityId()).isEqualTo("task");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityName()).isEqualTo("Multi User Task-${loopCounter}");
        assertThat(activityEvent.getActivityId()).isEqualTo("task");

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("Multi User Task-0");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task");

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("Multi User Task-1");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        org.flowable.task.api.Task task0 = tasks.get(0);
        org.flowable.task.api.Task task1 = tasks.get(1);

        multiExecutions = runtimeService.createExecutionQuery().activityId("task").list();
        assertThat(multiExecutions).hasSize(2);

        taskService.complete(task0.getId());

        multiExecutions = runtimeService.createExecutionQuery().activityId("task").list();
        assertThat(multiExecutions).hasSize(1);

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(1);

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_COMPLETED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getId()).isEqualTo(task0.getId());

        taskService.complete(task1.getId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_COMPLETED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getId()).isEqualTo(task1.getId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_COMPLETED);

        assertThat(activityEvent.getActivityId()).isEqualTo("task");
        assertThat(((FlowableMultiInstanceActivityCompletedEvent) activityEvent).getNumberOfInstances()).isEqualTo(2);
        assertThat(((FlowableMultiInstanceActivityCompletedEvent) activityEvent).getNumberOfActiveInstances()).isZero();
        assertThat(((FlowableMultiInstanceActivityCompletedEvent) activityEvent).getNumberOfCompletedInstances()).isEqualTo(2);
        assertThat(((FlowableMultiInstanceActivityCompletedEvent) activityEvent).isSequential()).isFalse();

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityType()).isEqualTo("endEvent");
        assertThat(activityEvent.getActivityId()).isEqualTo("endEvent1");

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT);
    }

    /**
     * Multi-instance user task cancelled by message boundary event defined on
     * multi-instance user task.
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/event/MultiInstanceUserTaskEventsTest.bpmn20.xml" })
    public void testMultiInstanceCancelledByMessageBoundaryEvent() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("multiInstanceUserTaskEvents");
        assertThat(processInstance).isNotNull();

        Execution task1Execution = runtimeService.createExecutionQuery().activityId("task1").singleResult();
        Execution boundaryExecution = runtimeService.createExecutionQuery().activityId("cancelBoundaryEvent1").singleResult();
        List<Execution> multiExecutions = runtimeService.createExecutionQuery().activityId("task2").list();
        assertThat(multiExecutions).hasSize(2);
        String multiExecutionId1 = multiExecutions.get(0).getId();
        String multiExecutionId2 = multiExecutions.get(1).getId();
        String rootMultiExecutionId = null;

        List<Execution> executions = runtimeService.createExecutionQuery().parentId(processInstance.getId()).list();
        for (Execution execution : executions) {
            if (((ExecutionEntity) execution).isMultiInstanceRoot()) {
                rootMultiExecutionId = execution.getId();
                break;
            }
        }
        assertThat(rootMultiExecutionId).isNotNull();
        assertThat(multiExecutionId1).isNotSameAs(rootMultiExecutionId);
        assertThat(multiExecutionId2).isNotSameAs(rootMultiExecutionId);
        assertThat(boundaryExecution).isNotSameAs(rootMultiExecutionId);

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
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task2");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task2");

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("Multi User Task-0");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task2");

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("Multi User Task-1");

        Execution cancelMessageExecution = runtimeService.createExecutionQuery().messageEventSubscriptionName("cancel")
                .singleResult();
        assertThat(cancelMessageExecution).isNotNull();
        assertThat(cancelMessageExecution.getActivityId()).isEqualTo("cancelBoundaryEvent1");

        // cancel the multi-instance user task
        runtimeService.messageEventReceived("cancel", cancelMessageExecution.getId());

        boolean foundMultiExec1 = false;
        boolean foundMultiExec2 = false;

        // cancelled event for one of the multi-instance user task instances
        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        FlowableActivityCancelledEvent cancelledEvent = (FlowableActivityCancelledEvent) activityEvent;
        assertThat(cancelledEvent.getActivityId()).isEqualTo("task2");
        assertThat(cancelledEvent.getActivityType()).isEqualTo("userTask");
        assertThat(cancelledEvent.getActivityName()).isEqualTo("Multi User Task-${loopCounter}");
        if (multiExecutionId2.equals(activityEvent.getExecutionId())) {
            foundMultiExec2 = true;
        } else if (multiExecutionId1.equals(activityEvent.getExecutionId())) {
            foundMultiExec1 = true;
        } else {
            fail("Unexpected execution id " + activityEvent.getExecutionId());
        }

        // cancelled event for one of the multi-instance user task instances
        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task2");
        if (multiExecutionId2.equals(activityEvent.getExecutionId())) {
            foundMultiExec2 = true;
        } else if (multiExecutionId1.equals(activityEvent.getExecutionId())) {
            foundMultiExec1 = true;
        } else {
            fail("Unexpected execution id " + activityEvent.getExecutionId());
        }

        assertThat(foundMultiExec1).isTrue();
        assertThat(foundMultiExec2).isTrue();

        // cancelled event for the root of the multi-instance user task
        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task2");
        assertThat(activityEvent.getExecutionId()).isEqualTo(rootMultiExecutionId);

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("cancelBoundaryEvent1");
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

        assertThat(idx).isEqualTo(17);
        assertThat(testListener.getEventsReceived()).hasSize(17);
    }

    /**
     * Multi-instance user task defined in external subprocess. The multi-instance user tasks
     * are cancelled by message boundary event defined on multi-instance user task.
     */
    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/event/MultiInstanceUserTaskEventsTest.testCallActivityTerminateEnd.bpmn20.xml",
            "org/flowable/engine/test/api/event/MultiInstanceUserTaskEventsTest.testCalledActivityTerminateEnd.bpmn20.xml" })
    public void testMultiInstanceInCallActivityCancelledByMessageBoundaryEvent() throws Exception {
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("multiInstanceCallActivityTerminateEnd");
        assertThat(processInstance).isNotNull();

        ExecutionEntity subprocessInstance = (ExecutionEntity) runtimeService.createExecutionQuery()
                .rootProcessInstanceId(processInstance.getId()).onlySubProcessExecutions().singleResult();
        assertThat(subprocessInstance).isNotNull();

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
        assertThat(activityEvent.getActivityId()).isEqualTo("callActivityId1");

        // external subprocess
        flowableEvent = testListener.getEventsReceived().get(idx++);
        assertThat(flowableEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_STARTED);
        executionEntity = (ExecutionEntity) ((FlowableProcessStartedEvent) flowableEvent).getEntity();
        assertThat(executionEntity.getParentId()).isEqualTo(subprocessInstance.getId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task1");

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("User Task1 in Parent");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("startevent2");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("startevent2");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("calledtask1");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("calledtask1");

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("Multi User Task-0");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("calledtask1");

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("Multi User Task-1");

        assertThat(testListener.getEventsReceived()).hasSize(idx);
        testListener.clearEventsReceived();

        idx = 0;
        Execution cancelMessageExecution = runtimeService.createExecutionQuery().messageEventSubscriptionName("cancel")
                .singleResult();
        assertThat(cancelMessageExecution).isNotNull();
        assertThat(cancelMessageExecution.getActivityId()).isEqualTo("cancelBoundaryEvent1");

        // cancel the multi-instance user task
        runtimeService.messageEventReceived("cancel", cancelMessageExecution.getId());

        // cancelled event for one of the multi-instance user task instances
        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        FlowableActivityCancelledEvent cancelledEvent = (FlowableActivityCancelledEvent) activityEvent;
        assertThat(cancelledEvent.getActivityId()).isEqualTo("calledtask1");
        assertThat(cancelledEvent.getActivityType()).isEqualTo("userTask");
        assertThat(cancelledEvent.getActivityName()).isEqualTo("Multi User Task-${loopCounter}");

        // cancelled event for one of the multi-instance user task instances
        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(activityEvent.getActivityId()).isEqualTo("calledtask1");

        // cancelled event for the root of the multi-instance user task
        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(activityEvent.getActivityId()).isEqualTo("calledtask1");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("cancelBoundaryEvent1");

        // end event in external call activity
        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("terminateEnd2");

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT);
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertThat(executionEntity.getId()).isEqualTo(subprocessInstance.getId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("callActivityId1");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("endevent1");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task1");

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT);
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertThat(executionEntity.getId()).isEqualTo(processInstance.getId());

        assertThat(idx).isEqualTo(10);
        assertThat(testListener.getEventsReceived()).hasSize(10);
    }

    /**
     * Multi-instance user task defined in external subprocess. The external subprocess and
     * the multi-instance user tasks are cancelled when parent flows to terminate end event.
     */
    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/event/MultiInstanceUserTaskEventsTest.testCallActivityTerminateEnd.bpmn20.xml",
            "org/flowable/engine/test/api/event/MultiInstanceUserTaskEventsTest.testCalledActivityTerminateEnd.bpmn20.xml" })
    public void testMultiInstanceInCallActivityCancelledWhenFlowToTerminateEnd() throws Exception {
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("multiInstanceCallActivityTerminateEnd");
        assertThat(processInstance).isNotNull();

        ExecutionEntity subprocessInstance = (ExecutionEntity) runtimeService.createExecutionQuery()
                .rootProcessInstanceId(processInstance.getId()).onlySubProcessExecutions().singleResult();
        assertThat(subprocessInstance).isNotNull();

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
        assertThat(activityEvent.getActivityId()).isEqualTo("callActivityId1");
        assertThat(activityEvent.getActivityType()).isEqualTo("callActivity");

        // external subprocess
        flowableEvent = testListener.getEventsReceived().get(idx++);
        assertThat(flowableEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_STARTED);
        executionEntity = (ExecutionEntity) ((FlowableProcessStartedEvent) flowableEvent).getEntity();
        assertThat(executionEntity.getParentId()).isEqualTo(subprocessInstance.getId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task1");

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("User Task1 in Parent");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("startevent2");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("startevent2");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("calledtask1");
        assertThat(activityEvent.getActivityType()).isEqualTo("userTask");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("calledtask1");
        assertThat(activityEvent.getActivityType()).isEqualTo("userTask");

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("Multi User Task-0");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("calledtask1");
        assertThat(activityEvent.getActivityType()).isEqualTo("userTask");

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("Multi User Task-1");

        assertThat(testListener.getEventsReceived()).hasSize(idx);
        testListener.clearEventsReceived();

        testListener.getEventsReceived().clear();
        idx = 0;

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        org.flowable.task.api.Task userTask1 = tasks.get(0);
        assertThat(userTask1.getName()).isEqualTo("User Task1 in Parent");

        // complete task1 in parent so we flow to terminate end
        taskService.complete(userTask1.getId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_COMPLETED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("User Task1 in Parent");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task1");
        assertThat(activityEvent.getActivityType()).isEqualTo("userTask");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("endevent1");
        assertThat(activityEvent.getActivityType()).isEqualTo("endEvent");

        // we now should see cancelled event for the root of the multi-instance,
        // for each instance, and for the boundary event.  They have the same creation
        // time so the ordering of these events can fluctuate
        int multiCount = 0;
        boolean foundBoundary = false;
        for (int i = 0; i < 4; i++) {
            activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
            if ("cancelBoundaryEvent1".equals(activityEvent.getActivityId())) {
                assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
                foundBoundary = true;
                assertThat(activityEvent.getActivityId()).isEqualTo("cancelBoundaryEvent1");
                assertThat(activityEvent.getActivityType()).isEqualTo("boundaryEvent");
            } else if ("calledtask1".equals(activityEvent.getActivityId())) {
                // cancelled event for one of the multi-instance user task instances or the root
                if (FlowableEngineEventType.ACTIVITY_CANCELLED.equals(activityEvent.getType())) {
                    FlowableActivityCancelledEvent cancelledEvent = (FlowableActivityCancelledEvent) activityEvent;
                    assertThat(cancelledEvent.getActivityId()).isEqualTo("calledtask1");
                    assertThat(cancelledEvent.getActivityType()).isEqualTo("userTask");
                    assertThat(cancelledEvent.getActivityName()).isEqualTo("Multi User Task-${loopCounter}");
                } else {
                    assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_CANCELLED);
                    FlowableMultiInstanceActivityCancelledEvent cancelledEvent = (FlowableMultiInstanceActivityCancelledEvent) activityEvent;
                    assertThat(cancelledEvent.getActivityId()).isEqualTo("calledtask1");
                    assertThat(cancelledEvent.getActivityType()).isEqualTo("userTask");
                    assertThat(cancelledEvent.getActivityName()).isEqualTo("Multi User Task-${loopCounter}");
                }

                multiCount++;
            } else {
                fail("Unknown activity id " + activityEvent.getActivityId());
            }
        }
        assertThat(foundBoundary).isTrue();
        assertThat(multiCount).isEqualTo(3);

        // external subprocess cancelled
        FlowableCancelledEvent processCancelledEvent = (FlowableCancelledEvent) testListener.getEventsReceived().get(idx++);
        assertThat(processCancelledEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_CANCELLED);
        assertThat(processCancelledEvent.getExecutionId()).isEqualTo(subprocessInstance.getId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(activityEvent.getActivityId()).isEqualTo("callActivityId1");
        assertThat(activityEvent.getActivityType()).isEqualTo("callActivity");

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT);
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertThat(executionEntity.getId()).isEqualTo(processInstance.getId());

        assertThat(idx).isEqualTo(10);
        assertThat(testListener.getEventsReceived()).hasSize(idx);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/event/MultiInstanceUserTaskEventsTest.testEmbeddedSubprocess.bpmn20.xml" })
    public void testMultiInstanceInSubprocessCancelledWhenFlowToTerminateEnd() throws Exception {
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("multiInstanceEmbeddedSubprocess");
        assertThat(processInstance).isNotNull();

        ExecutionEntity subprocessInstance = (ExecutionEntity) runtimeService.createExecutionQuery()
                .rootProcessInstanceId(processInstance.getId()).onlySubProcessExecutions().singleResult();
        assertThat(subprocessInstance).isNotNull();

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

        // embedded subprocess
        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("subprocess1");
        assertThat(activityEvent.getActivityType()).isEqualTo("subProcess");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task1");

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("User Task1 in Parent");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("startevent2");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("startevent2");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task2");
        assertThat(activityEvent.getActivityType()).isEqualTo("userTask");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task2");
        assertThat(activityEvent.getActivityType()).isEqualTo("userTask");

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("Multi User Task-0");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task2");
        assertThat(activityEvent.getActivityType()).isEqualTo("userTask");

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("Multi User Task-1");

        assertThat(testListener.getEventsReceived()).hasSize(idx);
        testListener.clearEventsReceived();

        testListener.getEventsReceived().clear();
        idx = 0;

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(3);
        org.flowable.task.api.Task userTask1 = null;
        for (org.flowable.task.api.Task t : tasks) {
            if ("User Task1 in Parent".equals(t.getName())) {
                userTask1 = t;
                break;
            }
        }
        assertThat(userTask1).isNotNull();

        // complete task1 so we flow to terminate end
        taskService.complete(userTask1.getId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_COMPLETED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("User Task1 in Parent");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task1");
        assertThat(activityEvent.getActivityType()).isEqualTo("userTask");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("endevent1");
        assertThat(activityEvent.getActivityType()).isEqualTo("endEvent");

        int miEventCount = 0;
        for (int i = 0; i < 4; i++) {

            activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
            if ("cancelBoundaryEvent1".equals(activityEvent.getActivityId())) {
                assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
                assertThat(activityEvent.getActivityId()).isEqualTo("cancelBoundaryEvent1");
                assertThat(activityEvent.getActivityType()).isEqualTo("boundaryEvent");

            } else if ("task2".equals(activityEvent.getActivityId())) {
                // cancelled event for one of the multi-instance user task instances
                if (FlowableEngineEventType.ACTIVITY_CANCELLED.equals(activityEvent.getType())) {
                    FlowableActivityCancelledEvent cancelledEvent = (FlowableActivityCancelledEvent) activityEvent;
                    assertThat(cancelledEvent.getActivityId()).isEqualTo("task2");
                    assertThat(cancelledEvent.getActivityType()).isEqualTo("userTask");
                    assertThat(cancelledEvent.getActivityName()).isEqualTo("Multi User Task-${loopCounter}");
                } else {
                    assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_CANCELLED);
                    FlowableMultiInstanceActivityCancelledEvent cancelledEvent = (FlowableMultiInstanceActivityCancelledEvent) activityEvent;
                    assertThat(cancelledEvent.getActivityId()).isEqualTo("task2");
                    assertThat(cancelledEvent.getActivityType()).isEqualTo("userTask");
                    assertThat(cancelledEvent.getActivityName()).isEqualTo("Multi User Task-${loopCounter}");
                }
                miEventCount++;

            } else if ("subprocess1".equals(activityEvent.getActivityId())) {
                assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
                assertThat(activityEvent.getActivityType()).isEqualTo("subProcess");

            } else {
                fail("Unknown activity id " + activityEvent.getActivityId());

            }

        }
        assertThat(miEventCount).isEqualTo(3);

        // embedded subprocess cancelled
        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        FlowableCancelledEvent processCancelledEvent = (FlowableCancelledEvent) activityEvent;
        assertThat(processCancelledEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(processCancelledEvent.getExecutionId()).isEqualTo(subprocessInstance.getId());
        assertThat(activityEvent.getActivityType()).isEqualTo("subProcess");

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT);
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertThat(executionEntity.getId()).isEqualTo(processInstance.getId());

        assertThat(idx).isEqualTo(9);
        assertThat(testListener.getEventsReceived()).hasSize(idx);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/event/MultiInstanceUserTaskEventsTest.testMultiInstanceSequentialUserTaskEventsWithNormalCompletion.bpmn20.xml" })
    public void testMultiInstanceSequentialUserTaskEventsWithNormalCompletion() {
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("ID_b8a6875f-6c4b-4864-b369-af6e69c612a5");
        assertThat(processInstance).isNotNull();
        String processId = processInstance.getProcessInstanceId();
        int idx = 0;

        FlowableProcessStartedEvent startEvent = (FlowableProcessStartedEvent) testListener.getEventsReceived().get(idx++);
        assertThat(startEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_STARTED);

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("startevent1");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("startevent1");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("usertask1");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("usertask1");

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processId).includeIdentityLinks().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task A-0");

        taskService.complete(tasks.get(0).getId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_COMPLETED);

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("usertask1");

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);

        tasks = taskService.createTaskQuery().processInstanceId(processId).includeIdentityLinks().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task A-1");

        taskService.complete(tasks.get(0).getId());

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processId).list()).isEmpty();

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_COMPLETED);

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("usertask1");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(activityEvent.getActivityId()).isEqualTo("cancelBoundaryEvent");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("endevent1");

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("endevent1");

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_COMPLETED);
    }

    class MultiInstanceUserActivityEventListener extends AbstractFlowableEngineEventListener {

        private List<FlowableEvent> eventsReceived;

        public MultiInstanceUserActivityEventListener() {
            super(new HashSet<>(Arrays.asList(
                    FlowableEngineEventType.ACTIVITY_STARTED,
                    FlowableEngineEventType.ACTIVITY_COMPLETED,
                    FlowableEngineEventType.ACTIVITY_CANCELLED,
                    FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED,
                    FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_COMPLETED,
                    FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_COMPLETED_WITH_CONDITION,
                    FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_CANCELLED,
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
        protected void multiInstanceActivityStarted(FlowableMultiInstanceActivityEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void multiInstanceActivityCompleted(FlowableMultiInstanceActivityCompletedEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void multiInstanceActivityCompletedWithCondition(FlowableMultiInstanceActivityCompletedEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void multiInstanceActivityCancelled(FlowableMultiInstanceActivityCancelledEvent event) {
            eventsReceived.add(event);
        }

        @Override
        public boolean isFailOnException() {
            return false;
        }
    }
}
