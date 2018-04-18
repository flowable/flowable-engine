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
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.task.Comment;

/**
 * Test case for all {@link FlowableEvent}s related to comments.
 * 
 * @author Frederik Heremans
 */
public class StandaloneCommentEventsTest extends PluggableFlowableTestCase {

    private TestFlowable6EntityEventListener listener;

    public void testCommentEntityEventsStandaloneTask() throws Exception {
        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            org.flowable.task.api.Task task = null;
            try {
                task = taskService.newTask();
                taskService.saveTask(task);
                assertNotNull(task);

                // Create link-comment
                Comment comment = taskService.addComment(task.getId(), null, "comment");
                assertEquals(2, listener.getEventsReceived().size());
                FlowableEngineEntityEvent event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
                assertEquals(FlowableEngineEventType.ENTITY_CREATED, event.getType());
                assertNull(event.getProcessInstanceId());
                assertNull(event.getExecutionId());
                assertNull(event.getProcessDefinitionId());
                Comment commentFromEvent = (Comment) event.getEntity();
                assertEquals(comment.getId(), commentFromEvent.getId());

                event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
                assertEquals(FlowableEngineEventType.ENTITY_INITIALIZED, event.getType());
                listener.clearEventsReceived();

                // Finally, delete comment
                taskService.deleteComment(comment.getId());
                assertEquals(1, listener.getEventsReceived().size());
                event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
                assertEquals(FlowableEngineEventType.ENTITY_DELETED, event.getType());
                assertNull(event.getProcessInstanceId());
                assertNull(event.getExecutionId());
                assertNull(event.getProcessDefinitionId());
                commentFromEvent = (Comment) event.getEntity();
                assertEquals(comment.getId(), commentFromEvent.getId());

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

        listener = new TestFlowable6EntityEventListener(Comment.class);
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
