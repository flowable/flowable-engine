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
package org.activiti.engine.test.api.event;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.runtime.ProcessInstance;

/**
 * Test case for all {@link FlowableEvent}s related to tasks.
 * 
 * @author Frederik Heremans
 */
public class TaskStandaloneEventsTest extends PluggableFlowableTestCase {

    private TestFlowable6EntityEventListener listener;

    /**
     * Check all events for tasks not related to a process-instance
     */
    public void testStandaloneTaskEvents() throws Exception {

        org.flowable.task.api.Task task = null;
        try {
            task = taskService.newTask();
            task.setCategory("123");
            task.setDescription("Description");
            taskService.saveTask(task);

            assertEquals(3, listener.getEventsReceived().size());

            FlowableEngineEntityEvent event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
            assertEquals(FlowableEngineEventType.ENTITY_CREATED, event.getType());
            assertTrue(event.getEntity() instanceof org.flowable.task.api.Task);
            org.flowable.task.api.Task taskFromEvent = (org.flowable.task.api.Task) event.getEntity();
            assertEquals(task.getId(), taskFromEvent.getId());
            assertNull(event.getProcessDefinitionId());
            assertNull(event.getProcessInstanceId());
            assertNull(event.getExecutionId());

            event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
            assertEquals(FlowableEngineEventType.ENTITY_INITIALIZED, event.getType());

            event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(2);
            assertEquals(FlowableEngineEventType.TASK_CREATED, event.getType());
            listener.clearEventsReceived();

            // Update task
            taskService.setOwner(task.getId(), "owner");
            assertEquals(1, listener.getEventsReceived().size());
            event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
            assertEquals(FlowableEngineEventType.ENTITY_UPDATED, event.getType());
            assertTrue(event.getEntity() instanceof org.flowable.task.api.Task);
            taskFromEvent = (org.flowable.task.api.Task) event.getEntity();
            assertEquals(task.getId(), taskFromEvent.getId());
            assertEquals("owner", taskFromEvent.getOwner());
            assertNull(event.getProcessDefinitionId());
            assertNull(event.getProcessInstanceId());
            assertNull(event.getExecutionId());
            listener.clearEventsReceived();

            // Assign task
            taskService.setAssignee(task.getId(), "kermit");
            assertEquals(2, listener.getEventsReceived().size());
            event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
            assertEquals(FlowableEngineEventType.ENTITY_UPDATED, event.getType());
            assertTrue(event.getEntity() instanceof org.flowable.task.api.Task);
            taskFromEvent = (org.flowable.task.api.Task) event.getEntity();
            assertEquals(task.getId(), taskFromEvent.getId());
            assertNull(event.getProcessDefinitionId());
            assertNull(event.getProcessInstanceId());
            assertNull(event.getExecutionId());
            
            event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
            assertEquals(FlowableEngineEventType.TASK_ASSIGNED, event.getType());
            assertTrue(event.getEntity() instanceof org.flowable.task.api.Task);
            taskFromEvent = (org.flowable.task.api.Task) event.getEntity();
            assertEquals(task.getId(), taskFromEvent.getId());
            assertEquals("kermit", taskFromEvent.getAssignee());
            assertNull(event.getProcessDefinitionId());
            assertNull(event.getProcessInstanceId());
            assertNull(event.getExecutionId());
            
            listener.clearEventsReceived();

            // Complete task
            taskService.complete(task.getId());
            assertEquals(2, listener.getEventsReceived().size());
            event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
            assertEquals(FlowableEngineEventType.TASK_COMPLETED, event.getType());
            assertTrue(event.getEntity() instanceof org.flowable.task.api.Task);
            taskFromEvent = (org.flowable.task.api.Task) event.getEntity();
            assertEquals(task.getId(), taskFromEvent.getId());
            assertNull(event.getProcessDefinitionId());
            assertNull(event.getProcessInstanceId());
            assertNull(event.getExecutionId());

            event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
            assertEquals(FlowableEngineEventType.ENTITY_DELETED, event.getType());
            assertTrue(event.getEntity() instanceof org.flowable.task.api.Task);
            taskFromEvent = (org.flowable.task.api.Task) event.getEntity();
            assertEquals(task.getId(), taskFromEvent.getId());
            assertNull(event.getProcessDefinitionId());
            assertNull(event.getProcessInstanceId());
            assertNull(event.getExecutionId());

        } finally {
            if (task != null) {
                String taskId = task.getId();
                task = taskService.createTaskQuery().taskId(taskId).singleResult();
                if (task != null) {
                    // If task still exists, delete it to have a clean DB after test
                    taskService.deleteTask(taskId);
                }
                historyService.deleteHistoricTaskInstance(taskId);
            }
        }
    }

    protected void assertExecutionDetails(FlowableEngineEvent event, ProcessInstance processInstance) {
        assertEquals(processInstance.getId(), event.getProcessInstanceId());
        assertEquals(processInstance.getId(), event.getExecutionId());
        assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        listener = new TestFlowable6EntityEventListener(org.flowable.task.api.Task.class);
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        if (listener != null) {
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }
}
