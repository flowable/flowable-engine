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

import java.io.ByteArrayInputStream;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Attachment;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test case for all {@link FlowableEvent}s related to attachments.
 *
 * @author Frederik Heremans
 */
public class AttachmentEventsTest extends PluggableFlowableTestCase {

    private TestFlowableEntityEventListener listener;

    /**
     * Test create, update and delete events of attachments on a task/process.
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testAttachmentEntityEvents() throws Exception {

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();

            // Create link-attachment
            Attachment attachment = taskService
                    .createAttachment("test", task.getId(), processInstance.getId(), "attachment name", "description", "http://flowable.org");
            assertThat(attachment.getUserId()).isNull();
            assertThat(listener.getEventsReceived()).hasSize(2);
            FlowableEngineEntityEvent event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
            assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
            assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
            assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
            Attachment attachmentFromEvent = (Attachment) event.getEntity();
            assertThat(attachmentFromEvent.getId()).isEqualTo(attachment.getId());
            event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
            assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
            assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
            assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
            attachmentFromEvent = (Attachment) event.getEntity();
            assertThat(attachmentFromEvent.getId()).isEqualTo(attachment.getId());
            listener.clearEventsReceived();

            // Create binary attachment
            Authentication.setAuthenticatedUserId("testuser");
            attachment = taskService.createAttachment("test", task.getId(), processInstance.getId(), "attachment name", "description",
                    new ByteArrayInputStream("test".getBytes()));
            assertThat(attachment.getUserId()).isEqualTo("testuser");
            assertThat(listener.getEventsReceived()).hasSize(2);
            event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
            assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
            assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
            assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
            attachmentFromEvent = (Attachment) event.getEntity();
            assertThat(attachmentFromEvent.getId()).isEqualTo(attachment.getId());

            event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
            listener.clearEventsReceived();

            // Update attachment
            attachment = taskService.getAttachment(attachment.getId());
            attachment.setDescription("Description");
            taskService.saveAttachment(attachment);

            assertThat(listener.getEventsReceived()).hasSize(1);
            event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_UPDATED);
            assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
            assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
            assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
            attachmentFromEvent = (Attachment) event.getEntity();
            assertThat(attachmentFromEvent.getId()).isEqualTo(attachment.getId());
            assertThat(attachmentFromEvent.getDescription()).isEqualTo("Description");
            listener.clearEventsReceived();

            // Finally, delete attachment
            taskService.deleteAttachment(attachment.getId());
            assertThat(listener.getEventsReceived()).hasSize(1);
            event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);
            assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
            assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
            assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
            attachmentFromEvent = (Attachment) event.getEntity();
            assertThat(attachmentFromEvent.getId()).isEqualTo(attachment.getId());
        }
    }

    /**
     * Test create, update and delete events of users.
     */
    @Test
    public void testAttachmentEntityEventsStandaloneTask() throws Exception {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            org.flowable.task.api.Task task = null;
            try {
                task = taskService.newTask();
                taskService.saveTask(task);
                assertThat(task).isNotNull();

                // Create link-attachment
                Attachment attachment = taskService.createAttachment("test", task.getId(), null, "attachment name", "description", "http://flowable.org");
                assertThat(listener.getEventsReceived()).hasSize(2);
                FlowableEngineEntityEvent event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
                assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
                assertThat(event.getProcessInstanceId()).isNull();
                assertThat(event.getExecutionId()).isNull();
                assertThat(event.getProcessDefinitionId()).isNull();
                Attachment attachmentFromEvent = (Attachment) event.getEntity();
                assertThat(attachmentFromEvent.getId()).isEqualTo(attachment.getId());
                event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
                assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
                listener.clearEventsReceived();

                // Create binary attachment
                attachment = taskService
                        .createAttachment("test", task.getId(), null, "attachment name", "description", new ByteArrayInputStream("test".getBytes()));
                assertThat(listener.getEventsReceived()).hasSize(2);
                event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
                assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
                assertThat(event.getProcessInstanceId()).isNull();
                assertThat(event.getExecutionId()).isNull();
                assertThat(event.getProcessDefinitionId()).isNull();
                attachmentFromEvent = (Attachment) event.getEntity();
                assertThat(attachmentFromEvent.getId()).isEqualTo(attachment.getId());

                event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
                assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
                listener.clearEventsReceived();

                // Update attachment
                attachment = taskService.getAttachment(attachment.getId());
                attachment.setDescription("Description");
                taskService.saveAttachment(attachment);

                assertThat(listener.getEventsReceived()).hasSize(1);
                event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
                assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_UPDATED);
                assertThat(event.getProcessInstanceId()).isNull();
                assertThat(event.getExecutionId()).isNull();
                assertThat(event.getProcessDefinitionId()).isNull();
                attachmentFromEvent = (Attachment) event.getEntity();
                assertThat(attachmentFromEvent.getId()).isEqualTo(attachment.getId());
                assertThat(attachmentFromEvent.getDescription()).isEqualTo("Description");
                listener.clearEventsReceived();

                // Finally, delete attachment
                taskService.deleteAttachment(attachment.getId());
                assertThat(listener.getEventsReceived()).hasSize(1);
                event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
                assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);
                assertThat(event.getProcessInstanceId()).isNull();
                assertThat(event.getExecutionId()).isNull();
                assertThat(event.getProcessDefinitionId()).isNull();
                attachmentFromEvent = (Attachment) event.getEntity();
                assertThat(attachmentFromEvent.getId()).isEqualTo(attachment.getId());

            } finally {
                if (task != null && task.getId() != null) {
                    taskService.deleteTask(task.getId(), true);
                }
            }
        }
    }

    @Test
    public void testAttachmentEntityEventsOnHistoricTaskDelete() throws Exception {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            org.flowable.task.api.Task task = null;
            try {
                task = taskService.newTask();
                taskService.saveTask(task);
                assertThat(task).isNotNull();

                // Create link-attachment
                Attachment attachment = taskService.createAttachment("test", task.getId(), null, "attachment name", "description", "http://flowable.org");
                listener.clearEventsReceived();

                // Delete task and historic task
                taskService.deleteTask(task.getId());
                historyService.deleteHistoricTaskInstance(task.getId());

                assertThat(listener.getEventsReceived()).hasSize(1);
                FlowableEngineEntityEvent event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
                assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);
                assertThat(event.getProcessInstanceId()).isNull();
                assertThat(event.getExecutionId()).isNull();
                assertThat(event.getProcessDefinitionId()).isNull();
                Attachment attachmentFromEvent = (Attachment) event.getEntity();
                assertThat(attachmentFromEvent.getId()).isEqualTo(attachment.getId());

            } finally {
                if (task != null && task.getId() != null) {
                    taskService.deleteTask(task.getId(), true);
                }
            }
        }
    }

    @BeforeEach
    protected void setUp() throws Exception {
        listener = new TestFlowableEntityEventListener(Attachment.class);
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @AfterEach
    protected void tearDown() throws Exception {

        if (listener != null) {
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }
}
