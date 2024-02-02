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
import java.util.List;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.delegate.event.FlowableActivityCancelledEvent;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.flowable.engine.delegate.event.FlowableCancelledEvent;
import org.flowable.engine.delegate.event.FlowableProcessStartedEvent;
import org.flowable.engine.event.EventLogEntry;
import org.flowable.engine.impl.event.logger.EventLogger;
import org.flowable.engine.impl.jobexecutor.AsyncCompleteCallActivityJobHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

public class CallActivityTest extends PluggableFlowableTestCase {

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
            "org/flowable/engine/test/api/event/CallActivityTest.testCallActivity.bpmn20.xml",
            "org/flowable/engine/test/api/event/CallActivityTest.testCalledActivity.bpmn20.xml" })
    public void testCallActivityCalledHasNoneEndEvent() throws Exception {

        CallActivityEventListener mylistener = new CallActivityEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(mylistener);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callActivity");
        assertThat(processInstance).isNotNull();

        // no task should be active in parent process
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNull();

        // only active task should be the one defined in the external subprocess
        task = taskService.createTaskQuery().active().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("User Task2 in External");

        ExecutionEntity subprocessInstance = (ExecutionEntity) runtimeService.createExecutionQuery()
                .rootProcessInstanceId(processInstance.getId())
                .onlySubProcessExecutions()
                .singleResult();
        assertThat(subprocessInstance).isNotNull();

        assertThat(runtimeService.getVariable(processInstance.getId(), "Name")).isEqualTo("Default name");
        assertThat(runtimeService.getVariable(subprocessInstance.getId(), "FullName")).isEqualTo("Default name");

        // set the variable in the subprocess to validate that the new value is returned from callActivity
        runtimeService.setVariable(subprocessInstance.getId(), "FullName", "Mary Smith");
        assertThat(runtimeService.getVariable(processInstance.getId(), "Name")).isEqualTo("Default name");
        assertThat(runtimeService.getVariable(subprocessInstance.getId(), "FullName")).isEqualTo("Mary Smith");

        // complete user task so that external subprocess will flow to terminate end
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("User Task1");

        // validate that the variable was copied back when Call Activity finished
        assertThat(runtimeService.getVariable(processInstance.getId(), "Name")).isEqualTo("Mary Smith");

        // complete user task so that parent process will terminate normally
        taskService.complete(task.getId());

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(0);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        ExecutionEntity executionEntity = (ExecutionEntity) entityEvent.getEntity();

        // this is the root process so parent null
        assertThat(executionEntity.getParentId()).isNull();
        String processExecutionId = executionEntity.getId();

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(1);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertThat(executionEntity.getParentId()).isEqualTo(processExecutionId);

        FlowableEvent flowableEvent = mylistener.getEventsReceived().get(2);
        assertThat(flowableEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_STARTED);

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(3);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(4);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(5);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("callActivity1");

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(6);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertThat(executionEntity.getParentId()).isNull();
        assertThat(executionEntity.getProcessInstanceId()).isEqualTo(executionEntity.getId());

        // user task within the external subprocess
        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(7);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertThat(executionEntity.getActivityId()).isEqualTo("calledtask1");

        // external subprocess
        flowableEvent = mylistener.getEventsReceived().get(8);
        assertThat(flowableEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_STARTED);

        // start event in external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(9);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");
        assertThat(activityEvent.getActivityId()).isEqualTo("startevent2");

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(10);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");
        assertThat(activityEvent.getActivityId()).isEqualTo("startevent2");

        // user task within external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(11);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("calledtask1");
        assertThat(activityEvent.getActivityType()).isEqualTo("userTask");

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(12);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("User Task2 in External");

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(13);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_COMPLETED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("User Task2 in External");

        // user task within external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(14);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("calledtask1");
        assertThat(activityEvent.getActivityType()).isEqualTo("userTask");

        // None event in external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(15);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("noneevent2");
        assertThat(activityEvent.getActivityType()).isEqualTo("endEvent");

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(16);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("noneevent2");
        assertThat(activityEvent.getActivityType()).isEqualTo("endEvent");

        // the external subprocess
        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(17);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_COMPLETED);

        // callActivity
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(18);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityType()).isEqualTo("callActivity");

        // user task within parent process
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(19);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task1");
        assertThat(activityEvent.getActivityType()).isEqualTo("userTask");

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(20);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("User Task1");

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(21);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_COMPLETED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("User Task1");

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(22);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task1");
        assertThat(activityEvent.getActivityType()).isEqualTo("userTask");

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(23);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("noneevent1");
        assertThat(activityEvent.getActivityType()).isEqualTo("endEvent");

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(24);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("noneevent1");
        assertThat(activityEvent.getActivityType()).isEqualTo("endEvent");

        // the parent process
        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(25);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_COMPLETED);

        assertThat(mylistener.getEventsReceived()).hasSize(26);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/event/CallActivityTest.testCallActivity.bpmn20.xml",
            "org/flowable/engine/test/api/event/CallActivityTest.testCalledActivity.bpmn20.xml" })
    public void testDeleteParentWhenCallActivityCalledHasNoneEndEvent() throws Exception {

        CallActivityEventListener mylistener = new CallActivityEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(mylistener);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callActivity");
        assertThat(processInstance).isNotNull();

        // no task should be active in parent process
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNull();

        // only active task should be the one defined in the external subprocess
        task = taskService.createTaskQuery().active().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("User Task2 in External");

        ExecutionEntity subprocessInstance = (ExecutionEntity) runtimeService.createExecutionQuery()
                .rootProcessInstanceId(processInstance.getId())
                .onlySubProcessExecutions()
                .singleResult();
        assertThat(subprocessInstance).isNotNull();

        assertThat(runtimeService.getVariable(processInstance.getId(), "Name")).isEqualTo("Default name");
        assertThat(runtimeService.getVariable(subprocessInstance.getId(), "FullName")).isEqualTo("Default name");

        runtimeService.deleteProcessInstance(processInstance.getId(), null);

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(0);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        ExecutionEntity executionEntity = (ExecutionEntity) entityEvent.getEntity();

        // this is the root process so parent null
        assertThat(executionEntity.getParentId()).isNull();
        String processExecutionId = executionEntity.getId();

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(1);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertThat(executionEntity.getParentId()).isEqualTo(processExecutionId);

        FlowableEvent flowableEvent = mylistener.getEventsReceived().get(2);
        assertThat(flowableEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_STARTED);

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(3);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(4);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(5);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("callActivity1");

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(6);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertThat(executionEntity.getParentId()).isNull();
        assertThat(executionEntity.getProcessInstanceId()).isEqualTo(executionEntity.getId());

        // user task within the external subprocess
        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(7);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertThat(executionEntity.getActivityId()).isEqualTo("calledtask1");

        // external subprocess
        flowableEvent = mylistener.getEventsReceived().get(8);
        assertThat(flowableEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_STARTED);

        // start event in external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(9);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");
        assertThat(activityEvent.getActivityId()).isEqualTo("startevent2");

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(10);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");
        assertThat(activityEvent.getActivityId()).isEqualTo("startevent2");

        // user task within external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(11);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("calledtask1");
        assertThat(activityEvent.getActivityType()).isEqualTo("userTask");

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(12);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("User Task2 in External");

        // user task within external subprocess cancelled
        FlowableActivityCancelledEvent activityCancelledEvent = (FlowableActivityCancelledEvent) mylistener.getEventsReceived().get(13);
        assertThat(activityCancelledEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(activityCancelledEvent.getActivityName()).isEqualTo("User Task2 in External");
        assertThat(activityCancelledEvent.getActivityType()).isEqualTo("userTask");

        // external subprocess cancelled
        FlowableCancelledEvent processCancelledEvent = (FlowableCancelledEvent) mylistener.getEventsReceived().get(14);
        assertThat(processCancelledEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_CANCELLED);
        assertThat(processCancelledEvent.getProcessInstanceId()).isEqualTo(subprocessInstance.getId());

        // expecting cancelled event for Call Activity
        activityCancelledEvent = (FlowableActivityCancelledEvent) mylistener.getEventsReceived().get(15);
        assertThat(activityCancelledEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(activityCancelledEvent.getActivityType()).isEqualTo("callActivity");

        // parent process cancelled
        processCancelledEvent = (FlowableCancelledEvent) mylistener.getEventsReceived().get(16);
        assertThat(processCancelledEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_CANCELLED);
        assertThat(processCancelledEvent.getProcessInstanceId()).isEqualTo(processInstance.getId());

        assertThat(mylistener.getEventsReceived()).hasSize(17);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/event/CallActivityTest.testCallActivityTerminateEnd.bpmn20.xml",
            "org/flowable/engine/test/api/event/CallActivityTest.testCalledActivityTerminateEnd.bpmn20.xml" })
    public void testCallActivityCalledHasTerminateEndEvent() throws Exception {

        CallActivityEventListener mylistener = new CallActivityEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(mylistener);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callActivityTerminateEnd");
        assertThat(processInstance).isNotNull();

        // no task should be active in parent process
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNull();

        // only active task should be the one defined in the external subprocess
        task = taskService.createTaskQuery().active().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("User Task2 in External with Terminate End Event");

        ExecutionEntity subprocessInstance = (ExecutionEntity) runtimeService.createExecutionQuery()
                .rootProcessInstanceId(processInstance.getId())
                .onlySubProcessExecutions()
                .singleResult();
        assertThat(subprocessInstance).isNotNull();

        assertThat(runtimeService.getVariable(processInstance.getId(), "Name")).isEqualTo("Default name");
        assertThat(runtimeService.getVariable(subprocessInstance.getId(), "FullName")).isEqualTo("Default name");

        // set the variable in the subprocess to validate that the new value is returned from callActivity
        runtimeService.setVariable(subprocessInstance.getId(), "FullName", "Mary Smith");
        assertThat(runtimeService.getVariable(processInstance.getId(), "Name")).isEqualTo("Default name");
        assertThat(runtimeService.getVariable(subprocessInstance.getId(), "FullName")).isEqualTo("Mary Smith");

        // complete user task so that external subprocess will flow to terminate end
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("User Task1 in Parent");

        // validate that the variable was copied back when Call Activity finished
        assertThat(runtimeService.getVariable(processInstance.getId(), "Name")).isEqualTo("Mary Smith");

        // complete user task so that parent process will terminate normally
        taskService.complete(task.getId());

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(0);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        ExecutionEntity executionEntity = (ExecutionEntity) entityEvent.getEntity();

        // this is the root process so parent null
        assertThat(executionEntity.getParentId()).isNull();
        String processExecutionId = executionEntity.getId();

        int idx = 1;
        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertThat(executionEntity.getParentId()).isEqualTo(processExecutionId);

        FlowableEvent flowableEvent = mylistener.getEventsReceived().get(idx++);
        assertThat(flowableEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_STARTED);

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("callActivityId1");

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertThat(executionEntity.getParentId()).isNull();
        assertThat(executionEntity.getProcessInstanceId()).isEqualTo(executionEntity.getId());

        // user task within the external subprocess
        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertThat(executionEntity.getActivityId()).isEqualTo("calledtask1");

        // external subprocess
        flowableEvent = mylistener.getEventsReceived().get(idx++);
        assertThat(flowableEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_STARTED);
        executionEntity = (ExecutionEntity) ((FlowableProcessStartedEvent) flowableEvent).getEntity();
        assertThat(executionEntity.getParentId()).isEqualTo(subprocessInstance.getId());

        // start event in external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");
        assertThat(activityEvent.getActivityId()).isEqualTo("startevent2");

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityType()).isEqualTo("startEvent");
        assertThat(activityEvent.getActivityId()).isEqualTo("startevent2");

        // user task within external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("calledtask1");
        assertThat(activityEvent.getActivityType()).isEqualTo("userTask");

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("User Task2 in External with Terminate End Event");

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_COMPLETED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("User Task2 in External with Terminate End Event");

        // user task within external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("calledtask1");
        assertThat(activityEvent.getActivityType()).isEqualTo("userTask");

        // None event in external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("terminateEnd2");
        assertThat(activityEvent.getActivityType()).isEqualTo("endEvent");

        // PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT instead of PROCESS_COMPLETED
        // because external subprocess defined with terminate end event
        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(idx++);
        assertThat(((FlowableEngineEntityEvent) entityEvent).getExecutionId()).isEqualTo(subprocessInstance.getId());
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT);

        //the external subprocess (callActivity)
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityType()).isEqualTo("callActivity");

        // user task within parent process
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task1");
        assertThat(activityEvent.getActivityType()).isEqualTo("userTask");

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("User Task1 in Parent");

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.TASK_COMPLETED);
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertThat(taskEntity.getName()).isEqualTo("User Task1 in Parent");

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("task1");
        assertThat(activityEvent.getActivityType()).isEqualTo("userTask");

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent.getActivityId()).isEqualTo("noneevent1");
        assertThat(activityEvent.getActivityType()).isEqualTo("endEvent");

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertThat(activityEvent.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED);
        assertThat(activityEvent.getActivityId()).isEqualTo("noneevent1");
        assertThat(activityEvent.getActivityType()).isEqualTo("endEvent");

        // the parent process
        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(idx++);
        assertThat(entityEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_COMPLETED);

        assertThat(mylistener.getEventsReceived()).hasSize(idx);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/event/CallActivityTest.testCallActivity.bpmn20.xml",
            "org/flowable/engine/test/api/event/CallActivityTest.testCalledActivity.bpmn20.xml" })
    public void testDeleteParentProcessWithCallActivity() throws Exception {

        CallActivityEventListener mylistener = new CallActivityEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(mylistener);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callActivity");
        assertThat(processInstance).isNotNull();

        // no task should be active in parent process
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNull();

        // only active task should be the one defined in the external subprocess
        task = taskService.createTaskQuery().active().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("User Task2 in External");

        ExecutionEntity subprocessInstance = (ExecutionEntity) runtimeService.createExecutionQuery()
                .rootProcessInstanceId(processInstance.getId())
                .onlySubProcessExecutions()
                .singleResult();
        assertThat(subprocessInstance).isNotNull();

        runtimeService.deleteProcessInstance(processInstance.getId(), null);

        List<FlowableEvent> entityEvents = mylistener.getEventsReceived();
        int lastIndex = entityEvents.size() - 1;
        assertThat(entityEvents.get(lastIndex - 3).getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(entityEvents.get(lastIndex - 2).getType()).isEqualTo(FlowableEngineEventType.PROCESS_CANCELLED);
        FlowableCancelledEvent subProcessCancelledEvent = (FlowableCancelledEvent) entityEvents.get(lastIndex - 2);
        assertThat(subProcessCancelledEvent.getProcessInstanceId()).isEqualTo(subprocessInstance.getId());
        assertThat(entityEvents.get(lastIndex - 1).getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(entityEvents.get(lastIndex).getType()).isEqualTo(FlowableEngineEventType.PROCESS_CANCELLED);
        FlowableCancelledEvent processCancelledEvent = (FlowableCancelledEvent) entityEvents.get(lastIndex);
        assertThat(processCancelledEvent.getProcessInstanceId()).isEqualTo(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/event/CallActivityTest.testCallActivityProcessInstanceName.bpmn20.xml",
            "org/flowable/engine/test/api/event/CallActivityTest.testCalledActivity.bpmn20.xml"
    })
    public void testCallActivityProcessInstanceName() {
        runtimeService.startProcessInstanceByKey("testCallActivityProcessInstanceName",
                CollectionUtil.singletonMap("theCollection", Arrays.asList("A", "B", "C", "D")));

        List<ProcessInstance> childProcessInstances = runtimeService.createProcessInstanceQuery().list().stream()
                .filter(processInstance -> (processInstance.getSuperExecutionId() != null))
                .sorted((p1, p2) -> p1.getName().compareTo(p2.getName()))
                .collect(Collectors.toList());

        assertThat(childProcessInstances)
                .extracting(ProcessInstance::getName)
                .containsExactly(
                        "Process instance A",
                        "Process instance B",
                        "Process instance C",
                        "Process instance D")
        ;
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/event/CallActivityTest.testCallActivityAsyncComplete.bpmn20.xml",
            "org/flowable/engine/test/api/event/CallActivityTest.testCallActivityAsyncComplete_subprocess.bpmn20.xml"
    })
    public void testCallActivityAsyncComplete() {
        runtimeService.startProcessInstanceByKey("testAsyncComplete");

        // 1 async service task
        List<Job> jobs = managementService.createJobQuery().list();
        assertThat(jobs).hasSize(1);
        jobs.forEach(job -> managementService.executeJob(job.getId()));

        // 5 async multi instance call activities after start
        jobs = managementService.createJobQuery().list();
        assertThat(jobs).hasSize(5);
        for (Job job : jobs) {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(job.getProcessDefinitionId())
                    .singleResult();
            assertThat(processDefinition.getKey()).isEqualTo("testAsyncComplete");
        }
        jobs.forEach(job -> managementService.executeJob(job.getId()));

        // 1 job for each step1 in subprocess, so 5 in total
        jobs = managementService.createJobQuery().list();
        assertThat(jobs).hasSize(5);
        for (Job job : jobs) {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(job.getProcessDefinitionId())
                    .singleResult();
            assertThat(processDefinition.getKey()).isEqualTo("subProcess");
        }
        jobs.forEach(job -> managementService.executeJob(job.getId()));

        // Step 2
        jobs = managementService.createJobQuery().list();
        assertThat(jobs).hasSize(5);
        jobs.forEach(job -> managementService.executeJob(job.getId()));

        // Step 3 will trigger the async complete
        jobs = managementService.createJobQuery().list();
        assertThat(jobs).hasSize(5);
        jobs.forEach(job -> managementService.executeJob(job.getId()));

        // Async complete
        jobs = managementService.createJobQuery().list();
        assertThat(jobs).hasSize(5);
        for (Job job : jobs) {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(job.getProcessDefinitionId())
                    .singleResult();
            assertThat(processDefinition.getKey())
                    .isEqualTo("subProcess"); // context is the subprocess, as the EndExecution will be scheduled for that process definition

            assertThat(job.getJobHandlerType()).isEqualTo(AsyncCompleteCallActivityJobHandler.TYPE);
        }

        // Completing ends the process instance
        jobs.forEach(job -> managementService.executeJob(job.getId()));
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    }

    // Same as testCallActivityAsyncComplete, but now using the real job executor instead of fetching and executing jobs manually
    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/event/CallActivityTest.testCallActivityAsyncComplete.bpmn20.xml",
            "org/flowable/engine/test/api/event/CallActivityTest.testCallActivityAsyncComplete_subprocess.bpmn20.xml"
    })
    @DisabledIfSystemProperty(named = "disableWhen", matches = "cockroachdb")
    public void testCallActivityAsyncCompleteRealExecutor() {
        runtimeService.startProcessInstanceByKey("testAsyncComplete");
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(20000L, 200L);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/event/CallActivityTest.testCallActivityWithEventSubprocessParent.bpmn20.xml",
            "org/flowable/engine/test/api/event/CallActivityTest.testCallActivityWithEventSubprocess.bpmn20.xml"
    })
    public void testCallActivityWithEventSubprocess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testWithEventSubprocessParent");
        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("One");

        // Completing the task should trigger the event subprocess
        taskService.complete(task.getId());
        Task subOneTask = taskService.createTaskQuery().taskName("sub one").singleResult();
        assertThat(subOneTask).isNotNull();
        taskService.complete(subOneTask.getId());

        // Complete the last task
        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Two");
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/event/CallActivityTest.testCallActivityWithEventSubprocessParent.bpmn20.xml",
            "org/flowable/engine/test/api/event/CallActivityTest.testCallActivityWithEventSubprocessInterrupting.bpmn20.xml"
    })
    public void testCallActivityWithEventSubprocessInterrupting() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testWithEventSubprocessParent");
        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("One");

        // Completing the task should trigger the event subprocess. This interrupts the main flow.
        taskService.complete(task.getId());
        Task subOneTask = taskService.createTaskQuery().taskName("sub one").singleResult();
        assertThat(subOneTask).isNotNull();
        taskService.complete(subOneTask.getId());

        assertProcessEnded(processInstance.getId());
    }

    class CallActivityEventListener extends AbstractFlowableEngineEventListener {

        private List<FlowableEvent> eventsReceived;

        public CallActivityEventListener() {
            eventsReceived = new ArrayList<>();
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
                case ENTITY_CREATED:
                    FlowableEntityEvent entityEvent = (FlowableEntityEvent) event;
                    if (entityEvent.getEntity() instanceof ExecutionEntity) {
                        eventsReceived.add(event);
                    }
                    break;
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
