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
package org.activiti5.engine.test.api.event;

import java.io.ByteArrayInputStream;

import org.activiti.engine.common.api.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEngineEventType;
import org.activiti.engine.impl.delegate.event.ActivitiEngineEntityEvent;
import org.activiti.engine.impl.history.HistoryLevel;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Attachment;
import org.activiti.engine.task.Task;
import org.activiti.engine.test.Deployment;
import org.activiti5.engine.impl.test.PluggableActivitiTestCase;

/**
 * Test case for all {@link ActivitiEvent}s related to attachments.
 * 
 * @author Frederik Heremans
 */
public class AttachmentEventsTest extends PluggableActivitiTestCase {

	private TestActivitiEntityEventListener listener;

	/**
	 * Test create, update and delete events of attachments on a task/process.
	 */
	@Deployment(resources = { "org/activiti5/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
	public void testAttachmentEntityEvents() throws Exception {
		
		if(processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
			ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
			
			Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
			assertNotNull(task);
			
			// Create link-attachment
			Attachment attachment = taskService.createAttachment("test", task.getId(), processInstance.getId(), "attachment name", "description", "http://activiti.org");
			assertNull(attachment.getUserId());
			assertEquals(2, listener.getEventsReceived().size());
			ActivitiEngineEntityEvent event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(0);
			assertEquals(ActivitiEngineEventType.ENTITY_CREATED, event.getType());
			assertEquals(processInstance.getId(), event.getProcessInstanceId());
			assertEquals(processInstance.getId(), event.getExecutionId());
			assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
			org.activiti5.engine.task.Attachment attachmentFromEvent = (org.activiti5.engine.task.Attachment) event.getEntity();
			assertEquals(attachment.getId(), attachmentFromEvent.getId());
			event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(1);
			assertEquals(ActivitiEngineEventType.ENTITY_INITIALIZED, event.getType());
			assertEquals(processInstance.getId(), event.getProcessInstanceId());
			assertEquals(processInstance.getId(), event.getExecutionId());
			assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
			attachmentFromEvent = (org.activiti5.engine.task.Attachment) event.getEntity();
			assertEquals(attachment.getId(), attachmentFromEvent.getId());
			listener.clearEventsReceived();
			
			// Create binary attachment
			Authentication.setAuthenticatedUserId("testuser");
			attachment = taskService.createAttachment("test", task.getId(), processInstance.getId(), "attachment name", "description", new ByteArrayInputStream("test".getBytes()));
			assertNotNull(attachment.getUserId());
			assertEquals("testuser", attachment.getUserId());
			assertEquals(2, listener.getEventsReceived().size());
			event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(0);
			assertEquals(ActivitiEngineEventType.ENTITY_CREATED, event.getType());
			assertEquals(processInstance.getId(), event.getProcessInstanceId());
			assertEquals(processInstance.getId(), event.getExecutionId());
			assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
			attachmentFromEvent = (org.activiti5.engine.task.Attachment) event.getEntity();
			assertEquals(attachment.getId(), attachmentFromEvent.getId());
			
			event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(1);
			assertEquals(ActivitiEngineEventType.ENTITY_INITIALIZED, event.getType());
			listener.clearEventsReceived();
			
			// Update attachment
			attachment = taskService.getAttachment(attachment.getId());
			attachment.setDescription("Description");
			taskService.saveAttachment(attachment);
			
			assertEquals(1, listener.getEventsReceived().size());
			event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(0);
			assertEquals(ActivitiEngineEventType.ENTITY_UPDATED, event.getType());
			assertEquals(processInstance.getId(), event.getProcessInstanceId());
			assertEquals(processInstance.getId(), event.getExecutionId());
			assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
			attachmentFromEvent = (org.activiti5.engine.task.Attachment) event.getEntity();
			assertEquals(attachment.getId(), attachmentFromEvent.getId());
			assertEquals("Description", attachmentFromEvent.getDescription());
			listener.clearEventsReceived();
			
			// Finally, delete attachment
			taskService.deleteAttachment(attachment.getId());
			assertEquals(1, listener.getEventsReceived().size());
			event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(0);
			assertEquals(ActivitiEngineEventType.ENTITY_DELETED, event.getType());
			assertEquals(processInstance.getId(), event.getProcessInstanceId());
			assertEquals(processInstance.getId(), event.getExecutionId());
			assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
			attachmentFromEvent = (org.activiti5.engine.task.Attachment) event.getEntity();
			assertEquals(attachment.getId(), attachmentFromEvent.getId());
		}
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		listener = new TestActivitiEntityEventListener(org.activiti5.engine.task.Attachment.class);
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
