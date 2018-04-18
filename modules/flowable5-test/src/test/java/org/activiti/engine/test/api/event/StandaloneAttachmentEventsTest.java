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

import java.io.ByteArrayInputStream;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.task.Attachment;

/**
 * Test case for all {@link FlowableEvent}s related to attachments.
 * 
 * @author Frederik Heremans
 */
public class StandaloneAttachmentEventsTest extends PluggableFlowableTestCase {

    private TestFlowable6EntityEventListener listener;

    /**
     * Test create, update and delete events of users.
     */
    public void testAttachmentEntityEventsStandaloneTask() throws Exception {
        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            org.flowable.task.api.Task task = null;
            try {
                task = taskService.newTask();
                taskService.saveTask(task);
                assertNotNull(task);

                // Create link-attachment
                Attachment attachment = taskService.createAttachment("test", task.getId(), null, "attachment name", "description", "http://activiti.org");
                assertEquals(2, listener.getEventsReceived().size());
                FlowableEngineEntityEvent event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
                assertEquals(FlowableEngineEventType.ENTITY_CREATED, event.getType());
                assertNull(event.getProcessInstanceId());
                assertNull(event.getExecutionId());
                assertNull(event.getProcessDefinitionId());
                Attachment attachmentFromEvent = (Attachment) event.getEntity();
                assertEquals(attachment.getId(), attachmentFromEvent.getId());
                event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
                assertEquals(FlowableEngineEventType.ENTITY_INITIALIZED, event.getType());
                listener.clearEventsReceived();

                // Create binary attachment
                attachment = taskService.createAttachment("test", task.getId(), null, "attachment name", "description", new ByteArrayInputStream("test".getBytes()));
                assertEquals(2, listener.getEventsReceived().size());
                event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
                assertEquals(FlowableEngineEventType.ENTITY_CREATED, event.getType());
                assertNull(event.getProcessInstanceId());
                assertNull(event.getExecutionId());
                assertNull(event.getProcessDefinitionId());
                attachmentFromEvent = (Attachment) event.getEntity();
                assertEquals(attachment.getId(), attachmentFromEvent.getId());

                event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
                assertEquals(FlowableEngineEventType.ENTITY_INITIALIZED, event.getType());
                listener.clearEventsReceived();

                // Update attachment
                attachment = taskService.getAttachment(attachment.getId());
                attachment.setDescription("Description");
                taskService.saveAttachment(attachment);

                assertEquals(1, listener.getEventsReceived().size());
                event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
                assertEquals(FlowableEngineEventType.ENTITY_UPDATED, event.getType());
                assertNull(event.getProcessInstanceId());
                assertNull(event.getExecutionId());
                assertNull(event.getProcessDefinitionId());
                attachmentFromEvent = (Attachment) event.getEntity();
                assertEquals(attachment.getId(), attachmentFromEvent.getId());
                assertEquals("Description", attachmentFromEvent.getDescription());
                listener.clearEventsReceived();

                // Finally, delete attachment
                taskService.deleteAttachment(attachment.getId());
                assertEquals(1, listener.getEventsReceived().size());
                event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
                assertEquals(FlowableEngineEventType.ENTITY_DELETED, event.getType());
                assertNull(event.getProcessInstanceId());
                assertNull(event.getExecutionId());
                assertNull(event.getProcessDefinitionId());
                attachmentFromEvent = (Attachment) event.getEntity();
                assertEquals(attachment.getId(), attachmentFromEvent.getId());

            } finally {
                if (task != null && task.getId() != null) {
                    taskService.deleteTask(task.getId());
                    historyService.deleteHistoricTaskInstance(task.getId());
                }
            }
        }
    }

    public void testAttachmentEntityEventsOnHistoricTaskDelete() throws Exception {
        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            org.flowable.task.api.Task task = null;
            try {
                task = taskService.newTask();
                taskService.saveTask(task);
                assertNotNull(task);

                // Create link-attachment
                Attachment attachment = taskService.createAttachment("test", task.getId(), null, "attachment name", "description", "http://activiti.org");
                listener.clearEventsReceived();

                // Delete task and historic task
                taskService.deleteTask(task.getId());
                historyService.deleteHistoricTaskInstance(task.getId());

                assertEquals(1, listener.getEventsReceived().size());
                FlowableEngineEntityEvent event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
                assertEquals(FlowableEngineEventType.ENTITY_DELETED, event.getType());
                assertNull(event.getProcessInstanceId());
                assertNull(event.getExecutionId());
                assertNull(event.getProcessDefinitionId());
                Attachment attachmentFromEvent = (Attachment) event.getEntity();
                assertEquals(attachment.getId(), attachmentFromEvent.getId());

            } finally {
                if (task != null && task.getId() != null) {
                    taskService.deleteTask(task.getId());
                    historyService.deleteHistoricTaskInstance(task.getId());
                }
            }
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        listener = new TestFlowable6EntityEventListener(Attachment.class);
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
