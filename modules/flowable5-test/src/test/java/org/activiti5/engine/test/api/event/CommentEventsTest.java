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

import org.activiti.engine.delegate.event.ActivitiEngineEventType;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.impl.delegate.event.ActivitiEngineEntityEvent;
import org.activiti.engine.impl.history.HistoryLevel;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.activiti.engine.test.Deployment;
import org.activiti5.engine.impl.test.PluggableActivitiTestCase;

/**
 * Test case for all {@link ActivitiEvent}s related to comments.
 * 
 * @author Frederik Heremans
 */
public class CommentEventsTest extends PluggableActivitiTestCase {

	private TestActivitiEntityEventListener listener;

	/**
	 * Test create, update and delete events of comments on a task/process.
	 */
	@Deployment(resources = { "org/activiti5/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
	public void testCommentEntityEvents() throws Exception {
		if(processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
			ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
			
			Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
			assertNotNull(task);
			
			// Create link-comment
			Comment comment = taskService.addComment(task.getId(), task.getProcessInstanceId(), "comment");
			assertEquals(2, listener.getEventsReceived().size());
			ActivitiEngineEntityEvent event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(0);
			assertEquals(ActivitiEngineEventType.ENTITY_CREATED, event.getType());
			assertEquals(processInstance.getId(), event.getProcessInstanceId());
			assertEquals(processInstance.getId(), event.getExecutionId());
			assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
			org.activiti5.engine.task.Comment commentFromEvent = (org.activiti5.engine.task.Comment) event.getEntity();
			assertEquals(comment.getId(), commentFromEvent.getId());
			
			event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(1);
			assertEquals(ActivitiEngineEventType.ENTITY_INITIALIZED, event.getType());
			listener.clearEventsReceived();
			
			// Finally, delete comment
			taskService.deleteComment(comment.getId());
			assertEquals(1, listener.getEventsReceived().size());
			event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(0);
			assertEquals(ActivitiEngineEventType.ENTITY_DELETED, event.getType());
			assertEquals(processInstance.getId(), event.getProcessInstanceId());
			assertEquals(processInstance.getId(), event.getExecutionId());
			assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
			commentFromEvent = (org.activiti5.engine.task.Comment) event.getEntity();
			assertEquals(comment.getId(), commentFromEvent.getId());
		}
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		listener = new TestActivitiEntityEventListener(org.activiti5.engine.task.Comment.class);
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
