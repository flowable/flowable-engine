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
    @Deployment(resources = {"org/flowable/engine/test/api/event/MultiInstanceUserTaskEventsTest.bpmn20.xml"})
    public void testMultiInstanceCancelledWhenFlowToTerminateEnd() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("multiInstanceUserTaskEvents");
        assertNotNull(processInstance);

        Execution task1Execution = runtimeService.createExecutionQuery().activityId("task1").singleResult();
        Execution boundaryExecution = runtimeService.createExecutionQuery().activityId("cancelBoundaryEvent1").singleResult();
        List<Execution> multiExecutions = runtimeService.createExecutionQuery().activityId("task2").list();
        assertEquals(2, multiExecutions.size());
        String multiExecutionId1 = multiExecutions.get(0).getId();
        String multiExecutionId2 = multiExecutions.get(1).getId();
        String rootMultiExecutionId = null;

        List<Execution> executions = runtimeService.createExecutionQuery().parentId(processInstance.getId()).list();
        for (Execution execution: executions)
        {
            if (((ExecutionEntity)execution).isMultiInstanceRoot()) {
                rootMultiExecutionId = execution.getId();
                break;
            }
        }
        assertNotNull(rootMultiExecutionId);
        assertNotSame(rootMultiExecutionId, multiExecutionId1);
        assertNotSame(rootMultiExecutionId, multiExecutionId2);
        assertNotSame(rootMultiExecutionId, boundaryExecution);

        int idx = 0;
        FlowableEvent flowableEvent = testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_STARTED, flowableEvent.getType());
        ExecutionEntity executionEntity = (ExecutionEntity) ((FlowableProcessStartedEvent) flowableEvent).getEntity();
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
        assertEquals(task1Execution.getId(), activityEvent.getExecutionId());

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("User Task1", taskEntity.getName());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("task2", activityEvent.getActivityId());
        assertEquals(rootMultiExecutionId, activityEvent.getExecutionId());

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

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());
        org.flowable.task.api.Task userTask1 = null;
        for (org.flowable.task.api.Task task : tasks) {
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
        assertEquals(task1Execution.getId(), activityEvent.getExecutionId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("endEvent", activityEvent.getActivityType());
        assertEquals("endEvent1", activityEvent.getActivityId());

        boolean foundMultiExec1 = false;
        boolean foundMultiExec2 = false;
        boolean foundRootExec = false;
        boolean foundBoundaryExec = false;

        // cancelled event for multi-instance user task instances and boundary event
        for (int i = 0; i < 4; i++) {
            activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
            FlowableActivityCancelledEvent cancelledEvent = (FlowableActivityCancelledEvent) activityEvent;

            if ("task2".equals(cancelledEvent.getActivityId())) {
                assertEquals("task2", cancelledEvent.getActivityId());
                assertEquals("userTask", cancelledEvent.getActivityType());
                assertEquals("Multi User Task-${loopCounter}", cancelledEvent.getActivityName());
                String eventExecutionId = activityEvent.getExecutionId();
                if (multiExecutionId1.equals(eventExecutionId)) {
                    assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
                    foundMultiExec1 = true;
                }
                else if (multiExecutionId2.equals(eventExecutionId)) {
                    assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
                    foundMultiExec2 = true;
                }
                else if (rootMultiExecutionId.equals(eventExecutionId)) {
                    assertEquals(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_CANCELLED, activityEvent.getType());
                    foundRootExec = true;
                }
            } else if ("cancelBoundaryEvent1".equals(cancelledEvent.getActivityId())) {
                assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
                cancelledEvent = (FlowableActivityCancelledEvent) activityEvent;
                assertEquals("cancelBoundaryEvent1", cancelledEvent.getActivityId());
                assertEquals(boundaryExecution.getId(), activityEvent.getExecutionId());
                foundBoundaryExec = true;
            }
        }

        assert(foundMultiExec1);
        assert(foundMultiExec2);
        assert(foundRootExec);
        assert(foundBoundaryExec);

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);

        assertEquals(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT, entityEvent.getType());

        assertEquals(18, idx);
        assertEquals(18, testListener.getEventsReceived().size());
    }

    /**
     * Multi-instance user task cancelled by terminate end event.
     */
    @Deployment(resources = {"org/flowable/engine/test/api/event/MultiInstanceUserTaskEventsTest.testMultiInstanceCompleteCondition.bpmn20.xml"})
    public void testMultiInstanceCompleteCondition() throws Exception {
        Map<String,Object> variables = new HashMap<>();
        variables.put("percentageCompleted", Float.valueOf(.5f));
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("multiInstanceUserTaskEvents", variables);
        assertNotNull(processInstance);

        //Execution task1Execution = runtimeService.createExecutionQuery().activityId("task1").singleResult();

        List<Execution> multiExecutions = runtimeService.createExecutionQuery().activityId("task").list();
        assertEquals(2, multiExecutions.size());
        String multiExecutionId1 = multiExecutions.get(0).getId();
        String multiExecutionId2 = multiExecutions.get(1).getId();
        String rootMultiExecutionId = null;

        List<Execution> executions = runtimeService.createExecutionQuery().parentId(processInstance.getId()).list();
        for (Execution execution: executions)
        {
            if (((ExecutionEntity)execution).isMultiInstanceRoot()) {
                rootMultiExecutionId = execution.getId();
                break;
            }
        }
        assertNotNull(rootMultiExecutionId);
        assertNotSame(rootMultiExecutionId, multiExecutionId1);
        assertNotSame(rootMultiExecutionId, multiExecutionId2);

        int idx = 0;
        FlowableEvent flowableEvent = testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_STARTED, flowableEvent.getType());
        ExecutionEntity executionEntity = (ExecutionEntity) ((FlowableProcessStartedEvent) flowableEvent).getEntity();
        assertEquals(processInstance.getId(), executionEntity.getProcessInstanceId());

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("start", activityEvent.getActivityName());
        assertEquals("startEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("start", activityEvent.getActivityName());
        assertEquals("startEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("Multi User Task-${loopCounter}", activityEvent.getActivityName());
        assertEquals("task", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("Multi User Task-${loopCounter}", activityEvent.getActivityName());
        assertEquals("task", activityEvent.getActivityId());

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("Multi User Task-0", taskEntity.getName());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("task", activityEvent.getActivityId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("Multi User Task-1", taskEntity.getName());

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());
        org.flowable.task.api.Task task0 = tasks.get(0);
        org.flowable.task.api.Task task1 = tasks.get(1);

        taskService.complete(task0.getId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_COMPLETED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals(task0.getId(), taskEntity.getId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_COMPLETED_WITH_CONDITION, activityEvent.getType());

        assertEquals("task", activityEvent.getActivityId());
        assertEquals(2, ((FlowableMultiInstanceActivityCompletedEvent)activityEvent).getNumberOfInstances());
        assertEquals(1, ((FlowableMultiInstanceActivityCompletedEvent)activityEvent).getNumberOfActiveInstances());
        assertEquals(1, ((FlowableMultiInstanceActivityCompletedEvent)activityEvent).getNumberOfCompletedInstances());
        assertEquals(false, ((FlowableMultiInstanceActivityCompletedEvent)activityEvent).isSequential());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
        assertEquals("task", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("endEvent", activityEvent.getActivityType());
        assertEquals("endEvent1", activityEvent.getActivityId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT, entityEvent.getType());
    }

    /**
     * Multi-instance user task cancelled by terminate end event.
     */
    @Deployment(resources = {"org/flowable/engine/test/api/event/MultiInstanceUserTaskEventsTest.testMultiInstanceCompleteCondition.bpmn20.xml"})
    public void testMultiInstanceComplete() throws Exception {
        Map<String,Object> variables = new HashMap<>();
        variables.put("percentageCompleted", Float.valueOf(2));
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("multiInstanceUserTaskEvents", variables);
        assertNotNull(processInstance);

        //Execution task1Execution = runtimeService.createExecutionQuery().activityId("task1").singleResult();

        List<Execution> multiExecutions = runtimeService.createExecutionQuery().activityId("task").list();
        assertEquals(2, multiExecutions.size());
        String multiExecutionId1 = multiExecutions.get(0).getId();
        String multiExecutionId2 = multiExecutions.get(1).getId();
        String rootMultiExecutionId = null;

        List<Execution> executions = runtimeService.createExecutionQuery().parentId(processInstance.getId()).list();
        for (Execution execution: executions)
        {
            if (((ExecutionEntity)execution).isMultiInstanceRoot()) {
                rootMultiExecutionId = execution.getId();
                break;
            }
        }
        assertNotNull(rootMultiExecutionId);
        assertNotSame(rootMultiExecutionId, multiExecutionId1);
        assertNotSame(rootMultiExecutionId, multiExecutionId2);

        int idx = 0;
        FlowableEvent flowableEvent = testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_STARTED, flowableEvent.getType());
        ExecutionEntity executionEntity = (ExecutionEntity) ((FlowableProcessStartedEvent) flowableEvent).getEntity();
        assertEquals(processInstance.getId(), executionEntity.getProcessInstanceId());

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("start", activityEvent.getActivityName());
        assertEquals("startEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("start", activityEvent.getActivityName());
        assertEquals("startEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("Multi User Task-${loopCounter}", activityEvent.getActivityName());
        assertEquals("task", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("Multi User Task-${loopCounter}", activityEvent.getActivityName());
        assertEquals("task", activityEvent.getActivityId());

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("Multi User Task-0", taskEntity.getName());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("task", activityEvent.getActivityId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("Multi User Task-1", taskEntity.getName());

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());
        org.flowable.task.api.Task task0 = tasks.get(0);
        org.flowable.task.api.Task task1 = tasks.get(1);

        multiExecutions = runtimeService.createExecutionQuery().activityId("task").list() ;
        assertEquals(2, multiExecutions.size());

        taskService.complete(task0.getId());

        multiExecutions = runtimeService.createExecutionQuery().activityId("task").list() ;
        assertEquals(1, multiExecutions.size());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, tasks.size());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_COMPLETED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals(task0.getId(), taskEntity.getId());

        taskService.complete(task1.getId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_COMPLETED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals(task1.getId(), taskEntity.getId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_COMPLETED, activityEvent.getType());

        assertEquals("task", activityEvent.getActivityId());
        assertEquals(2, ((FlowableMultiInstanceActivityCompletedEvent)activityEvent).getNumberOfInstances());
        assertEquals(0, ((FlowableMultiInstanceActivityCompletedEvent)activityEvent).getNumberOfActiveInstances());
        assertEquals(2, ((FlowableMultiInstanceActivityCompletedEvent)activityEvent).getNumberOfCompletedInstances());
        assertEquals(false, ((FlowableMultiInstanceActivityCompletedEvent)activityEvent).isSequential());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("endEvent", activityEvent.getActivityType());
        assertEquals("endEvent1", activityEvent.getActivityId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT, entityEvent.getType());
    }

    /**
     * Multi-instance user task cancelled by message boundary event defined on
     * multi-instance user task.
     */
    @Deployment(resources = {"org/flowable/engine/test/api/event/MultiInstanceUserTaskEventsTest.bpmn20.xml"})
    public void testMultiInstanceCancelledByMessageBoundaryEvent() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("multiInstanceUserTaskEvents");
        assertNotNull(processInstance);

        Execution task1Execution = runtimeService.createExecutionQuery().activityId("task1").singleResult();
        Execution boundaryExecution = runtimeService.createExecutionQuery().activityId("cancelBoundaryEvent1").singleResult();
        List<Execution> multiExecutions = runtimeService.createExecutionQuery().activityId("task2").list();
        assertEquals(2, multiExecutions.size());
        String multiExecutionId1 = multiExecutions.get(0).getId();
        String multiExecutionId2 = multiExecutions.get(1).getId();
        String rootMultiExecutionId = null;

        List<Execution> executions = runtimeService.createExecutionQuery().parentId(processInstance.getId()).list();
        for (Execution execution: executions)
        {
            if (((ExecutionEntity)execution).isMultiInstanceRoot()) {
                rootMultiExecutionId = execution.getId();
                break;
            }
        }
        assertNotNull(rootMultiExecutionId);
        assertNotSame(rootMultiExecutionId, multiExecutionId1);
        assertNotSame(rootMultiExecutionId, multiExecutionId2);
        assertNotSame(rootMultiExecutionId, boundaryExecution);

        int idx = 0;
        FlowableEvent flowableEvent = testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_STARTED, flowableEvent.getType());
        ExecutionEntity executionEntity = (ExecutionEntity) ((FlowableProcessStartedEvent) flowableEvent).getEntity();
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
        assertEquals(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED, activityEvent.getType());
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

        boolean foundMultiExec1 = false;
        boolean foundMultiExec2 = false;

        // cancelled event for one of the multi-instance user task instances
        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
        FlowableActivityCancelledEvent cancelledEvent = (FlowableActivityCancelledEvent) activityEvent;
        assertEquals("task2", cancelledEvent.getActivityId());
        assertEquals("userTask", cancelledEvent.getActivityType());
        assertEquals("Multi User Task-${loopCounter}", cancelledEvent.getActivityName());
        if (multiExecutionId2.equals(activityEvent.getExecutionId())) {
            foundMultiExec2 = true;            
        }
        else if (multiExecutionId1.equals(activityEvent.getExecutionId())) {
            foundMultiExec1 = true;            
        }
        else {
            fail("Unexpected execution id " + activityEvent.getExecutionId());
        }       

        // cancelled event for one of the multi-instance user task instances
        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
        assertEquals("task2", activityEvent.getActivityId());
        if (multiExecutionId2.equals(activityEvent.getExecutionId())) {
            foundMultiExec2 = true;            
        }
        else if (multiExecutionId1.equals(activityEvent.getExecutionId())) {
            foundMultiExec1 = true;            
        }
        else {
            fail("Unexpected execution id " + activityEvent.getExecutionId());
        }

        assertTrue(foundMultiExec1);
        assertTrue(foundMultiExec2);

        // cancelled event for the root of the multi-instance user task
        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
        assertEquals("task2", activityEvent.getActivityId());
        assertEquals(rootMultiExecutionId, activityEvent.getExecutionId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("cancelBoundaryEvent1", activityEvent.getActivityId());
        assertEquals(boundaryExecution.getId(), activityEvent.getExecutionId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("endEvent1", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
        assertEquals("task1", activityEvent.getActivityId());
        assertEquals(task1Execution.getId(), activityEvent.getExecutionId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT, entityEvent.getType());

        assertEquals(17, idx);
        assertEquals(17, testListener.getEventsReceived().size());
    }

    /**
     * Multi-instance user task defined in external subprocess. The multi-instance user tasks
     * are cancelled by message boundary event defined on multi-instance user task.
     */
    @Deployment(resources = {
            "org/flowable/engine/test/api/event/MultiInstanceUserTaskEventsTest.testCallActivityTerminateEnd.bpmn20.xml",
            "org/flowable/engine/test/api/event/MultiInstanceUserTaskEventsTest.testCalledActivityTerminateEnd.bpmn20.xml"})
    public void testMultiInstanceInCallActivityCancelledByMessageBoundaryEvent() throws Exception {
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("multiInstanceCallActivityTerminateEnd");
        assertNotNull(processInstance);

        ExecutionEntity subprocessInstance = (ExecutionEntity) runtimeService.createExecutionQuery()
                .rootProcessInstanceId(processInstance.getId()).onlySubProcessExecutions().singleResult();
        assertNotNull(subprocessInstance);

        int idx = 0;
        FlowableEvent flowableEvent = testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_STARTED, flowableEvent.getType());
        ExecutionEntity executionEntity = (ExecutionEntity) ((FlowableProcessStartedEvent) flowableEvent).getEntity();
        assertEquals(processInstance.getId(), executionEntity.getProcessInstanceId());

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("callActivityId1", activityEvent.getActivityId());

        // external subprocess
        flowableEvent = testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_STARTED, flowableEvent.getType());
        executionEntity = (ExecutionEntity) ((FlowableProcessStartedEvent) flowableEvent).getEntity();
        assertEquals(subprocessInstance.getId(), executionEntity.getParentId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("task1", activityEvent.getActivityId());

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("User Task1 in Parent", taskEntity.getName());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("startevent2", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("startevent2", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("calledtask1", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("calledtask1", activityEvent.getActivityId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("Multi User Task-0", taskEntity.getName());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("calledtask1", activityEvent.getActivityId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("Multi User Task-1", taskEntity.getName());

        assertEquals(testListener.getEventsReceived().size(), idx);
        testListener.clearEventsReceived();

        idx = 0;
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
        assertEquals("calledtask1", cancelledEvent.getActivityId());
        assertEquals("userTask", cancelledEvent.getActivityType());
        assertEquals("Multi User Task-${loopCounter}", cancelledEvent.getActivityName());

        // cancelled event for one of the multi-instance user task instances
        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
        assertEquals("calledtask1", activityEvent.getActivityId());

        // cancelled event for the root of the multi-instance user task
        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
        assertEquals("calledtask1", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("cancelBoundaryEvent1", activityEvent.getActivityId());

        // end event in external call activity
        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("terminateEnd2", activityEvent.getActivityId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT, entityEvent.getType());
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertEquals(subprocessInstance.getId(), executionEntity.getId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("callActivityId1", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("endevent1", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
        assertEquals("task1", activityEvent.getActivityId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT, entityEvent.getType());
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertEquals(processInstance.getId(), executionEntity.getId());

        assertEquals(10, idx);
        assertEquals(10, testListener.getEventsReceived().size());
    }


    /**
     * Multi-instance user task defined in external subprocess. The external subprocess and
     * the multi-instance user tasks are cancelled when parent flows to terminate end event.
     */
    @Deployment(resources = {
            "org/flowable/engine/test/api/event/MultiInstanceUserTaskEventsTest.testCallActivityTerminateEnd.bpmn20.xml",
            "org/flowable/engine/test/api/event/MultiInstanceUserTaskEventsTest.testCalledActivityTerminateEnd.bpmn20.xml"})
    public void testMultiInstanceInCallActivityCancelledWhenFlowToTerminateEnd() throws Exception {
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("multiInstanceCallActivityTerminateEnd");
        assertNotNull(processInstance);

        ExecutionEntity subprocessInstance = (ExecutionEntity) runtimeService.createExecutionQuery()
                .rootProcessInstanceId(processInstance.getId()).onlySubProcessExecutions().singleResult();
        assertNotNull(subprocessInstance);

        int idx = 0;
        FlowableEvent flowableEvent = testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_STARTED, flowableEvent.getType());
        ExecutionEntity executionEntity = (ExecutionEntity) ((FlowableProcessStartedEvent) flowableEvent).getEntity();
        assertEquals(processInstance.getId(), executionEntity.getProcessInstanceId());

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("callActivityId1", activityEvent.getActivityId());
        assertEquals("callActivity", activityEvent.getActivityType());

        // external subprocess
        flowableEvent = testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_STARTED, flowableEvent.getType());
        executionEntity = (ExecutionEntity) ((FlowableProcessStartedEvent) flowableEvent).getEntity();
        assertEquals(subprocessInstance.getId(), executionEntity.getParentId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("task1", activityEvent.getActivityId());

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("User Task1 in Parent", taskEntity.getName());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("startevent2", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("startevent2", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("calledtask1", activityEvent.getActivityId());
        assertEquals("userTask", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("calledtask1", activityEvent.getActivityId());
        assertEquals("userTask", activityEvent.getActivityType());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("Multi User Task-0", taskEntity.getName());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("calledtask1", activityEvent.getActivityId());
        assertEquals("userTask", activityEvent.getActivityType());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("Multi User Task-1", taskEntity.getName());

        assertEquals(testListener.getEventsReceived().size(), idx);
        testListener.clearEventsReceived();

        testListener.getEventsReceived().clear();
        idx = 0;

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, tasks.size());
        org.flowable.task.api.Task userTask1 = tasks.get(0);
        assertEquals("User Task1 in Parent", userTask1.getName());

        // complete task1 in parent so we flow to terminate end
        taskService.complete(userTask1.getId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_COMPLETED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("User Task1 in Parent", taskEntity.getName());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("task1", activityEvent.getActivityId());
        assertEquals("userTask", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("endevent1", activityEvent.getActivityId());
        assertEquals("endEvent", activityEvent.getActivityType());

        // we now should see cancelled event for the root of the multi-instance,
        // for each instance, and for the boundary event.  They have the same creation
        // time so the ordering of these events can fluctuate
        int multiCount = 0;
        boolean foundBoundary = false;
        for (int i = 0; i < 4; i++) {
            activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
            if ("cancelBoundaryEvent1".equals(activityEvent.getActivityId())) {
                assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
                foundBoundary = true;
                assertEquals("cancelBoundaryEvent1", activityEvent.getActivityId());
                assertEquals("boundaryEvent", activityEvent.getActivityType());
            } else if ("calledtask1".equals(activityEvent.getActivityId())) {
                // cancelled event for one of the multi-instance user task instances or the root
                if(FlowableEngineEventType.ACTIVITY_CANCELLED.equals(activityEvent.getType())) {
                    FlowableActivityCancelledEvent cancelledEvent = (FlowableActivityCancelledEvent) activityEvent;
                    assertEquals("calledtask1", cancelledEvent.getActivityId());
                    assertEquals("userTask", cancelledEvent.getActivityType());
                    assertEquals("Multi User Task-${loopCounter}", cancelledEvent.getActivityName());
                }
                else {
                    assertEquals(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_CANCELLED, activityEvent.getType());
                    FlowableMultiInstanceActivityCancelledEvent cancelledEvent = (FlowableMultiInstanceActivityCancelledEvent) activityEvent;
                    assertEquals("calledtask1", cancelledEvent.getActivityId());
                    assertEquals("userTask", cancelledEvent.getActivityType());
                    assertEquals("Multi User Task-${loopCounter}", cancelledEvent.getActivityName());
                }

                multiCount++;
            } else {
                fail("Unknown activity id " + activityEvent.getActivityId());
            }
        }
        assertTrue(foundBoundary);
        assertEquals(3, multiCount);

        // external subprocess cancelled
        FlowableCancelledEvent processCancelledEvent = (FlowableCancelledEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_CANCELLED, processCancelledEvent.getType());
        assertEquals(subprocessInstance.getId(), processCancelledEvent.getExecutionId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
        assertEquals("callActivityId1", activityEvent.getActivityId());
        assertEquals("callActivity", activityEvent.getActivityType());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT, entityEvent.getType());
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertEquals(processInstance.getId(), executionEntity.getId());

        assertEquals(10, idx);
        assertEquals(idx, testListener.getEventsReceived().size());
    }

    @Deployment(resources = {
            "org/flowable/engine/test/api/event/MultiInstanceUserTaskEventsTest.testEmbeddedSubprocess.bpmn20.xml"})
    public void testMultiInstanceInSubprocessCancelledWhenFlowToTerminateEnd() throws Exception {
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("multiInstanceEmbeddedSubprocess");
        assertNotNull(processInstance);

        ExecutionEntity subprocessInstance = (ExecutionEntity) runtimeService.createExecutionQuery()
                .rootProcessInstanceId(processInstance.getId()).onlySubProcessExecutions().singleResult();
        assertNotNull(subprocessInstance);

        int idx = 0;
        FlowableEvent flowableEvent = testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_STARTED, flowableEvent.getType());
        ExecutionEntity executionEntity = (ExecutionEntity) ((FlowableProcessStartedEvent) flowableEvent).getEntity();
        assertEquals(processInstance.getId(), executionEntity.getProcessInstanceId());

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());

        // embedded subprocess
        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("subprocess1", activityEvent.getActivityId());
        assertEquals("subProcess", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("task1", activityEvent.getActivityId());

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("User Task1 in Parent", taskEntity.getName());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("startevent2", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("startevent2", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("task2", activityEvent.getActivityId());
        assertEquals("userTask", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("task2", activityEvent.getActivityId());
        assertEquals("userTask", activityEvent.getActivityType());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("Multi User Task-0", taskEntity.getName());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("task2", activityEvent.getActivityId());
        assertEquals("userTask", activityEvent.getActivityType());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("Multi User Task-1", taskEntity.getName());

        assertEquals(testListener.getEventsReceived().size(), idx);
        testListener.clearEventsReceived();

        testListener.getEventsReceived().clear();
        idx = 0;

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());
        org.flowable.task.api.Task userTask1 = null;
        for (org.flowable.task.api.Task t : tasks) {
            if ("User Task1 in Parent".equals(t.getName())) {
                userTask1 = t;
                break;
            }
        }
        assertNotNull(userTask1);

        // complete task1 so we flow to terminate end
        taskService.complete(userTask1.getId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_COMPLETED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("User Task1 in Parent", taskEntity.getName());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("task1", activityEvent.getActivityId());
        assertEquals("userTask", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("endevent1", activityEvent.getActivityId());
        assertEquals("endEvent", activityEvent.getActivityType());

        int miEventCount = 0;
        for (int i = 0; i < 4; i++) {

            activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
            if ("cancelBoundaryEvent1".equals(activityEvent.getActivityId())) {
                assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
                assertEquals("cancelBoundaryEvent1", activityEvent.getActivityId());
                assertEquals("boundaryEvent", activityEvent.getActivityType());

            } else if ("task2".equals(activityEvent.getActivityId())) {
                // cancelled event for one of the multi-instance user task instances
                if(FlowableEngineEventType.ACTIVITY_CANCELLED.equals(activityEvent.getType())) {
                    FlowableActivityCancelledEvent cancelledEvent = (FlowableActivityCancelledEvent) activityEvent;
                    assertEquals("task2", cancelledEvent.getActivityId());
                    assertEquals("userTask", cancelledEvent.getActivityType());
                    assertEquals("Multi User Task-${loopCounter}", cancelledEvent.getActivityName());
                }
                else {
                    assertEquals(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_CANCELLED, activityEvent.getType());
                    FlowableMultiInstanceActivityCancelledEvent cancelledEvent = (FlowableMultiInstanceActivityCancelledEvent) activityEvent;
                    assertEquals("task2", cancelledEvent.getActivityId());
                    assertEquals("userTask", cancelledEvent.getActivityType());
                    assertEquals("Multi User Task-${loopCounter}", cancelledEvent.getActivityName());
                }
                miEventCount++;

            } else if ("subprocess1".equals(activityEvent.getActivityId())) {
                assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
                assertEquals("subProcess", activityEvent.getActivityType());
                
            } else {
                fail("Unknown activity id " + activityEvent.getActivityId());
                
            }

        }
        assertEquals(3, miEventCount);

        // embedded subprocess cancelled
        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        FlowableCancelledEvent processCancelledEvent = (FlowableCancelledEvent) activityEvent;
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, processCancelledEvent.getType());
        assertEquals(subprocessInstance.getId(), processCancelledEvent.getExecutionId());
        assertEquals("subProcess", activityEvent.getActivityType());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT, entityEvent.getType());
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertEquals(processInstance.getId(), executionEntity.getId());

        assertEquals(9, idx);
        assertEquals(idx, testListener.getEventsReceived().size());
    }

    @Deployment(resources = {
            "org/flowable/engine/test/api/event/MultiInstanceUserTaskEventsTest.testMultiInstanceSequentialUserTaskEventsWithNormalCompletion.bpmn20.xml"})
    public void testMultiInstanceSequentialUserTaskEventsWithNormalCompletion() {
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("ID_b8a6875f-6c4b-4864-b369-af6e69c612a5");
        assertNotNull(processInstance);
        String processId = processInstance.getProcessInstanceId();
        int idx = 0;

        FlowableProcessStartedEvent startEvent = (FlowableProcessStartedEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_STARTED, startEvent.getType());

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("startevent1", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("startevent1", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("usertask1", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("usertask1", activityEvent.getActivityId());

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processId).includeIdentityLinks().list();
        assertEquals(1, tasks.size());
        Task task0  = tasks.get(0);
        assertEquals("Task A-0", task0.getName());

        taskService.complete(task0.getId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_COMPLETED, entityEvent.getType());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("usertask1", activityEvent.getActivityId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());

        tasks = taskService.createTaskQuery().processInstanceId(processId).includeIdentityLinks().list();
        assertEquals(1, tasks.size());
        Task task1 = tasks.get(0);
        assertEquals("Task A-1", task1.getName());

        taskService.complete(task1.getId());

        assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(processId).list().size());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_COMPLETED, entityEvent.getType());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("usertask1", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityEvent.getType());
        assertEquals("cancelBoundaryEvent", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("endevent1", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("endevent1", activityEvent.getActivityId());

        entityEvent = (FlowableEntityEvent) testListener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_COMPLETED, entityEvent.getType());
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
