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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test case for all {@link FlowableEvent}s related to tasks.
 *
 * @author Frederik Heremans
 */
public class TaskEventsTest extends PluggableFlowableTestCase {

    private TestFlowableEntityEventListener listener;

    /**
     * Check create, update and delete events for a task.
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testTaskEventsInProcess() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        // Check create event
        assertThat(listener.getEventsReceived()).hasSize(3);
        FlowableEngineEntityEvent event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        assertThat(event.getEntity()).isInstanceOf(org.flowable.task.api.Task.class);
        org.flowable.task.api.Task taskFromEvent = (org.flowable.task.api.Task) event.getEntity();
        assertThat(taskFromEvent.getId()).isEqualTo(task.getId());
        assertExecutionDetails(event, processInstance);

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(2);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
        assertThat(event.getEntity()).isInstanceOf(org.flowable.task.api.Task.class);
        taskFromEvent = (org.flowable.task.api.Task) event.getEntity();
        assertThat(taskFromEvent.getId()).isEqualTo(task.getId());
        assertExecutionDetails(event, processInstance);

        listener.clearEventsReceived();

        // Update duedate, owner and priority should trigger update-event
        taskService.setDueDate(task.getId(), new Date());
        assertThat(listener.getEventsReceived()).hasSize(2);
        assertThat(listener.getEventsReceived().get(0).getType()).isEqualTo(FlowableEngineEventType.TASK_DUEDATE_CHANGED);
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
        assertExecutionDetails(event, processInstance);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_UPDATED);
        listener.clearEventsReceived();

        // Update name, owner and priority should trigger update-event
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        listener.clearEventsReceived();
        task.setName("newName");
        taskService.saveTask(task);
        assertThat(listener.getEventsReceived()).hasSize(2);
        assertThat(listener.getEventsReceived().get(0).getType()).isEqualTo(FlowableEngineEventType.TASK_NAME_CHANGED);
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
        assertExecutionDetails(event, processInstance);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_UPDATED);
        listener.clearEventsReceived();

        taskService.setPriority(task.getId(), 12);
        assertThat(listener.getEventsReceived()).hasSize(2);
        assertThat(listener.getEventsReceived().get(0).getType()).isEqualTo(FlowableEngineEventType.TASK_PRIORITY_CHANGED);
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_UPDATED);
        assertExecutionDetails(event, processInstance);
        listener.clearEventsReceived();

        taskService.setOwner(task.getId(), "kermit");
        assertThat(listener.getEventsReceived()).hasSize(2);
        assertThat(listener.getEventsReceived().get(0).getType()).isEqualTo(FlowableEngineEventType.TASK_OWNER_CHANGED);
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_UPDATED);
        assertExecutionDetails(event, processInstance);
        listener.clearEventsReceived();

        // Updating detached task and calling saveTask should trigger a single update-event
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        task.setDueDate(new Date());
        task.setOwner("john");
        taskService.saveTask(task);

        assertThat(listener.getEventsReceived()).hasSize(3);
        assertThat(listener.getEventsReceived().get(0).getType()).isEqualTo(FlowableEngineEventType.TASK_OWNER_CHANGED);
        assertThat(listener.getEventsReceived().get(1).getType()).isEqualTo(FlowableEngineEventType.TASK_DUEDATE_CHANGED);
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(2);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_UPDATED);
        assertExecutionDetails(event, processInstance);
        listener.clearEventsReceived();

        // Check delete-event on complete
        taskService.complete(task.getId());
        assertThat(listener.getEventsReceived()).hasSize(2);
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.TASK_COMPLETED);
        assertExecutionDetails(event, processInstance);
        TaskEntity taskEntity = (TaskEntity) event.getEntity();
        assertThat(taskEntity.getDueDate()).isNotNull();
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);
        assertExecutionDetails(event, processInstance);

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testTaskAssignmentEventInProcess() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();
        listener.clearEventsReceived();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        // Set assignee through API
        taskService.setAssignee(task.getId(), "kermit");
        assertThat(listener.getEventsReceived()).hasSize(2);
        FlowableEngineEntityEvent event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_UPDATED);
        assertThat(event.getEntity()).isInstanceOf(org.flowable.task.api.Task.class);
        assertExecutionDetails(event, processInstance);

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.TASK_ASSIGNED);
        assertThat(event.getEntity()).isInstanceOf(org.flowable.task.api.Task.class);
        org.flowable.task.api.Task taskFromEvent = (org.flowable.task.api.Task) event.getEntity();
        assertThat(taskFromEvent.getId()).isEqualTo(task.getId());
        assertThat(taskFromEvent.getAssignee()).isEqualTo("kermit");
        assertExecutionDetails(event, processInstance);

        listener.clearEventsReceived();

        // Set assignee through updateTask
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        task.setAssignee("newAssignee");
        taskService.saveTask(task);

        assertThat(listener.getEventsReceived()).hasSize(2);
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_UPDATED);
        assertThat(event.getEntity()).isInstanceOf(org.flowable.task.api.Task.class);
        assertExecutionDetails(event, processInstance);

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.TASK_ASSIGNED);
        assertThat(event.getEntity()).isInstanceOf(org.flowable.task.api.Task.class);
        taskFromEvent = (org.flowable.task.api.Task) event.getEntity();
        assertThat(taskFromEvent.getId()).isEqualTo(task.getId());
        assertThat(taskFromEvent.getAssignee()).isEqualTo("newAssignee");
        assertExecutionDetails(event, processInstance);
        listener.clearEventsReceived();

        // Unclaim
        taskService.unclaim(task.getId());
        assertThat(listener.getEventsReceived()).hasSize(2);
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_UPDATED);
        assertThat(event.getEntity()).isInstanceOf(org.flowable.task.api.Task.class);
        assertExecutionDetails(event, processInstance);

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.TASK_ASSIGNED);
        assertThat(event.getEntity()).isInstanceOf(org.flowable.task.api.Task.class);
        taskFromEvent = (org.flowable.task.api.Task) event.getEntity();
        assertThat(taskFromEvent.getId()).isEqualTo(task.getId());
        assertThat(taskFromEvent.getAssignee()).isNull();
        assertExecutionDetails(event, processInstance);

        listener.clearEventsReceived();
    }

    /**
     * Check events related to process instance delete and standalone task delete.
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testDeleteEventDoesNotDispathComplete() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();
        listener.clearEventsReceived();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        // Delete process, should delete task as well, but not complete
        runtimeService.deleteProcessInstance(processInstance.getId(), "testing task delete events");

        assertThat(listener.getEventsReceived()).hasSize(1);
        FlowableEngineEntityEvent event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);
        assertThat(event.getEntity()).isInstanceOf(org.flowable.task.api.Task.class);
        org.flowable.task.api.Task taskFromEvent = (org.flowable.task.api.Task) event.getEntity();
        assertThat(taskFromEvent.getId()).isEqualTo(task.getId());
        assertExecutionDetails(event, processInstance);

        try {
            task = taskService.newTask();
            task.setCategory("123");
            task.setDescription("Description");
            taskService.saveTask(task);
            listener.clearEventsReceived();

            // Delete standalone task, only a delete-event should be dispatched
            taskService.deleteTask(task.getId());

            assertThat(listener.getEventsReceived()).hasSize(1);
            event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);
            assertThat(event.getEntity()).isInstanceOf(org.flowable.task.api.Task.class);
            taskFromEvent = (org.flowable.task.api.Task) event.getEntity();
            assertThat(taskFromEvent.getId()).isEqualTo(task.getId());
            assertThat(event.getProcessDefinitionId()).isNull();
            assertThat(event.getProcessInstanceId()).isNull();
            assertThat(event.getExecutionId()).isNull();

        } finally {
            if (task != null) {
                String taskId = task.getId();
                task = taskService.createTaskQuery().taskId(taskId).singleResult();
                if (task != null) {
                    // If task still exists, delete it to have a clean DB after test
                    taskService.deleteTask(taskId);
                }
                historyService.deleteHistoricTaskInstance(taskId);
                managementService.executeCommand(commandContext -> {
                    processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().deleteHistoricTaskLogEntriesForTaskId(taskId);
                    return null;
                });
            }
        }
    }

    /**
     * This method checks to ensure that the task.fireEvent(TaskListener.EVENTNAME_CREATE), fires before the dispatchEvent FlowableEventType.TASK_CREATED. A ScriptTaskListener updates the priority and
     * assignee before the dispatchEvent() takes place.
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/event/TaskEventsTest.testEventFiring.bpmn20.xml" })
    public void testEventFiringOrdering() {
        // We need to add a special listener that copies the org.flowable.task.service.Task values - to record its state when the event fires,
        // otherwise the in-memory task instances is changed after the event fires.
        TestFlowableEntityEventTaskListener tlistener = new TestFlowableEntityEventTaskListener(org.flowable.task.api.Task.class);
        processEngineConfiguration.getEventDispatcher().addEventListener(tlistener);

        try {

            runtimeService.startProcessInstanceByKey("testTaskLocalVars");

            // Fetch first task
            org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();

            // Complete first task
            Map<String, Object> taskParams = new HashMap<>();
            taskService.complete(task.getId(), taskParams, true);

            FlowableEntityEvent event = (FlowableEntityEvent) tlistener.getEventsReceived().get(0);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
            assertThat(event.getEntity()).isInstanceOf(org.flowable.task.api.Task.class);

            event = (FlowableEntityEvent) tlistener.getEventsReceived().get(1);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
            assertThat(event.getEntity()).isInstanceOf(org.flowable.task.api.Task.class);

            event = (FlowableEntityEvent) tlistener.getEventsReceived().get(2);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
            assertThat(event.getEntity()).isInstanceOf(org.flowable.task.api.Task.class);
            org.flowable.task.api.Task taskFromEvent = tlistener.getTasks().get(2);
            assertThat(taskFromEvent.getId()).isEqualTo(task.getId());

            // verify script listener has done its job, on create before FlowableEntityEvent was fired
            assertThat(taskFromEvent.getAssignee()).as("The ScriptTaskListener must set this value before the dispatchEvent fires.")
                    .isEqualTo("scriptedAssignee");
            assertThat(taskFromEvent.getPriority()).as("The ScriptTaskListener must set this value before the dispatchEvent fires.").isEqualTo(877);

            // Fetch second task
            taskService.createTaskQuery().singleResult();

        } finally {
            processEngineConfiguration.getEventDispatcher().removeEventListener(tlistener);
        }
    }

    /**
     * Check all events for tasks not related to a process-instance
     */
    @Test
    public void testStandaloneTaskEvents() throws Exception {

        org.flowable.task.api.Task task = null;
        try {
            task = taskService.newTask();
            task.setCategory("123");
            task.setDescription("Description");
            taskService.saveTask(task);

            assertThat(listener.getEventsReceived()).hasSize(3);

            FlowableEngineEntityEvent event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
            assertThat(event.getEntity()).isInstanceOf(org.flowable.task.api.Task.class);
            org.flowable.task.api.Task taskFromEvent = (org.flowable.task.api.Task) event.getEntity();
            assertThat(taskFromEvent.getId()).isEqualTo(task.getId());
            assertThat(event.getProcessDefinitionId()).isNull();
            assertThat(event.getProcessInstanceId()).isNull();
            assertThat(event.getExecutionId()).isNull();

            event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);

            event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(2);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED);
            listener.clearEventsReceived();

            // Update task
            taskService.setOwner(task.getId(), "owner");
            assertThat(listener.getEventsReceived()).hasSize(2);
            assertThat(listener.getEventsReceived().get(0).getType()).isEqualTo(FlowableEngineEventType.TASK_OWNER_CHANGED);
            event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_UPDATED);
            assertThat(event.getEntity()).isInstanceOf(org.flowable.task.api.Task.class);
            taskFromEvent = (org.flowable.task.api.Task) event.getEntity();
            assertThat(taskFromEvent.getId()).isEqualTo(task.getId());
            assertThat(taskFromEvent.getOwner()).isEqualTo("owner");
            assertThat(event.getProcessDefinitionId()).isNull();
            assertThat(event.getProcessInstanceId()).isNull();
            assertThat(event.getExecutionId()).isNull();
            listener.clearEventsReceived();

            // Assign task
            taskService.setAssignee(task.getId(), "kermit");
            assertThat(listener.getEventsReceived()).hasSize(2);
            event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_UPDATED);
            assertThat(event.getEntity()).isInstanceOf(org.flowable.task.api.Task.class);
            taskFromEvent = (org.flowable.task.api.Task) event.getEntity();
            assertThat(taskFromEvent.getId()).isEqualTo(task.getId());
            assertThat(event.getProcessDefinitionId()).isNull();
            assertThat(event.getProcessInstanceId()).isNull();
            assertThat(event.getExecutionId()).isNull();
            event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.TASK_ASSIGNED);
            assertThat(event.getEntity()).isInstanceOf(org.flowable.task.api.Task.class);
            taskFromEvent = (org.flowable.task.api.Task) event.getEntity();
            assertThat(taskFromEvent.getId()).isEqualTo(task.getId());
            assertThat(taskFromEvent.getAssignee()).isEqualTo("kermit");
            assertThat(event.getProcessDefinitionId()).isNull();
            assertThat(event.getProcessInstanceId()).isNull();
            assertThat(event.getExecutionId()).isNull();
            listener.clearEventsReceived();

            // Complete task
            taskService.complete(task.getId());
            assertThat(listener.getEventsReceived()).hasSize(2);
            event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.TASK_COMPLETED);
            assertThat(event.getEntity()).isInstanceOf(org.flowable.task.api.Task.class);
            taskFromEvent = (org.flowable.task.api.Task) event.getEntity();
            assertThat(taskFromEvent.getId()).isEqualTo(task.getId());
            assertThat(event.getProcessDefinitionId()).isNull();
            assertThat(event.getProcessInstanceId()).isNull();
            assertThat(event.getExecutionId()).isNull();

            event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);
            assertThat(event.getEntity()).isInstanceOf(org.flowable.task.api.Task.class);
            taskFromEvent = (org.flowable.task.api.Task) event.getEntity();
            assertThat(taskFromEvent.getId()).isEqualTo(task.getId());
            assertThat(event.getProcessDefinitionId()).isNull();
            assertThat(event.getProcessInstanceId()).isNull();
            assertThat(event.getExecutionId()).isNull();

        } finally {
            if (task != null) {
                String taskId = task.getId();
                task = taskService.createTaskQuery().taskId(taskId).singleResult();
                if (task != null) {
                    // If task still exists, delete it to have a clean DB after
                    // test
                    taskService.deleteTask(taskId);
                }
                historyService.deleteHistoricTaskInstance(taskId);
                managementService.executeCommand(commandContext -> {
                    processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().deleteHistoricTaskLogEntriesForTaskId(taskId);
                    return null;
                });
            }
        }
    }

    protected void assertExecutionDetails(FlowableEngineEntityEvent event, ProcessInstance processInstance) {
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getExecutionId()).isNotNull();
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
    }

    @BeforeEach
    protected void setUp() throws Exception {
        listener = new TestFlowableEntityEventListener(org.flowable.task.api.Task.class);
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @AfterEach
    protected void tearDown() throws Exception {

        if (listener != null) {
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }
}
